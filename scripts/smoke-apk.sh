#!/bin/sh
# emulator-smoke-apk.sh — throwaway NFC-tolerant smoke APK.
# The app finish()es on NFC absence, so emulators can't run it. This patches the two
# NFC guards to return, builds, copies the APK, asserts the artifact is fresh, and
# restores the tree. NEVER commit the patch or the APK.
set -eu
cd "$(dirname "$0")/.."
MAIN=app/src/main/java/li/power/app/mifareuidchanger/MainActivity.java

# git safe.directory for /work is configured globally; no per-call flag needed.
APK=app/build/outputs/apk/debug/app-debug.apk
START=$(date +%s)

n=$(grep -c "^            finish();$" "$MAIN")
[ "$n" -eq 2 ] || { echo "expected 2 NFC finish() guards, found $n — source changed?" >&2; exit 1; }

if ! git diff --quiet -- "$MAIN"; then
    echo "uncommitted changes in $MAIN — commit first (this script restores the tree on any exit)" >&2
    exit 1
fi
git checkout -- "$MAIN"
trap 'git checkout -- "$MAIN"' EXIT
sed -i 's|^            finish();$|            /* smoke */;|' "$MAIN"

rm -f "$APK" emulator-smoke.apk
ANDROID_HOME=${ANDROID_HOME:-/root/android-sdk} sh ./gradlew assembleDebug --no-daemon -q
MT=$(stat -c %Y "$APK" 2>/dev/null || echo 0)
[ "$MT" -gt "$START" ] || { echo "APK not rebuilt by this run (stale mtime)" >&2; exit 1; }
cp "$APK" emulator-smoke.apk
ls -la emulator-smoke.apk
echo "smoke APK ready (tree restored; do not commit)"
