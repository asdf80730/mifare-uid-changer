#!/usr/bin/env bash
# Boot (or reuse) the API 34 x86_64 emulator for real-device verification.
# Idempotent: safe to run every session, whether or not the emulator is up.
# Boot under TCG takes ~4 min; call with a generous timeout.
set -euo pipefail
SDK=/root/android-sdk
export PATH="$SDK/platform-tools:$SDK/emulator:$PATH"

# already booted?
if adb start-server >/dev/null 2>&1 && adb get-state >/dev/null 2>&1; then
    if [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
        echo "emulator already booted"
        exit 0
    fi
fi

pkill -f "emulator -avd muc34" 2>/dev/null || true
sleep 2

if [ ! -d "$SDK/system-images/android-34/default/x86_64" ]; then
    echo "installing emulator image (~2 min)"
    yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" \
        "emulator" "system-images;android-34;default;x86_64" >/dev/null 2>&1
fi

if [ ! -d "$HOME/.android/avd/muc34.avd" ]; then
    echo no | "$SDK/cmdline-tools/latest/bin/avdmanager" create avd \
        -n muc34 -k "system-images;android-34;default;x86_64" --force >/dev/null
fi

# a killed instance leaves snapshot/lock files that make the next boot exit FATAL
rm -f "$HOME"/.android/avd/muc34.avd/*.snap* \
      "$HOME"/.android/avd/muc34.avd/*.lock 2>/dev/null || true

setsid nohup "$SDK/emulator/emulator" -avd muc34 \
    -no-window -no-audio -no-boot-anim \
    -gpu swiftshader_indirect -no-accel -no-snapshot -wipe-data \
    >/tmp/muc-emulator.log 2>&1 < /dev/null &
EMU=$!

# fail fast if the emulator itself dies instead of waiting on a dead device
alive() { kill -0 "$EMU" 2>/dev/null; }

for _ in $(seq 1 120); do
    alive || { echo "emulator exited:"; tail -3 /tmp/muc-emulator.log; exit 1; }
    [ "$(adb get-state 2>/dev/null | tr -d '\r')" = "device" ] && break
    sleep 5
done

for _ in $(seq 1 120); do
    alive || { echo "emulator exited:"; tail -3 /tmp/muc-emulator.log; exit 1; }
    [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && break
    sleep 5
done

# TCG runs slow enough to trip ANR dialogs; keep them off the screen.
adb shell settings put global hide_error_dialogs 1 >/dev/null
echo "emulator booted"
