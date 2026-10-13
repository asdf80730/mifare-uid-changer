# Build & Test

All commands run from the repo root. `gradlew` ships without the exec bit — invoke it as `sh ./gradlew …`, with `ANDROID_HOME=/root/android-sdk` (JDK 17 from apt; SDK platform/build-tools 34 preinstalled there).

## Build

```
ANDROID_HOME=/root/android-sdk sh ./gradlew assembleDebug --no-daemon
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Ship criterion: `ls -la` that path shows a timestamp from this build. A build that was not copied is still just a build.

## Tests (the only guardrail — upstream has no CI and no lint)

```
ANDROID_HOME=/root/android-sdk sh ./gradlew testDebugUnitTest --no-daemon
```

Suite `app/src/test/java/li/power/app/mifareuidchanger/MainActivitySmokeTest.java` runs the real `MainActivity`: launch, real dialog SAVE clicks, filtering, duplicate-UID rename. Run it green before touching UI or persistence code.

The only automated check is the tracked hook `scripts/hooks/pre-commit`, wired via
`git config core.hooksPath scripts/hooks`. A checkout without that config has no
guardrail — run the config command before trusting any commit.

Gotcha: `setOnShowListener` dialogs register their custom button listeners lazily. After showing one (e.g. `showAddTagDialog`), call `Shadows.shadowOf(Looper.getMainLooper()).idle();` before clicking — otherwise the click does nothing and the failure looks like a product bug.

## Emulator smoke APK

The app self-closes without NFC (`setupNfc` calls `finish()`); emulators have no NFC. Build the throwaway NFC-tolerant variant with `scripts/smoke-apk.sh` — it patches, builds, copies to `emulator-smoke.apk`, asserts the artifact, and restores the tree. Never commit the patch or the APK.

## Git plumbing

Let plumbing command stderr through (`git stash pop`, checkout, reset); a silenced failure becomes a ghost hunt. `safe.directory` for `/work` is configured globally — no per-command flag.

After any `git checkout --` restore or external edit, `read` the file before `edit`; tags captured before the restore are stale and anchored edits land on the wrong lines.

## Reading results

Never pass `-i` to gradle to chase output — daemon logs bury it. Read the verdict from `app/build/test-results/testDebugUnitTest/*.xml`: failures under the `failure` node, `println` probes under `system-out`.

To probe product code, drop a throwaway `Log.d("TAGX", "…")` there and dump it from a test with `ShadowLog.getLogsForTag("TAGX")` — proves which lines actually run, e.g. whether a dialog listener fired at all. Remove the probe before committing.

For the instrumented build on a real device: keep the `Log.d` lines, run the
`smoke-apk.sh` NFC-patch sed (`s|^            finish();$|            /* smoke */;|`),
then `ANDROID_HOME=/root/android-sdk sh ./gradlew assembleDebug --no-daemon`
(output `app/build/outputs/apk/debug/app-debug.apk`), then `git checkout --` the file.
`smoke-apk.sh` refuses to run on a dirty tree, so patch builds always go this way.

## Real device without KVM
Run `scripts/emulator-up.sh` — idempotent (installs the image, creates the AVD, boots
under TCG in software, waits for `boot_completed`, hides error dialogs). Boot ~4 min;
uiautomator/adb calls run 10x slower than normal, so sleep 20+ between steps.

Robolectric passes while the real UI is broken — trust logcat + window dumps. One tap
per dump, re-dump after each tap (an ANR dialog carries `Wait` and `Close app` in the
same tree; grabbing both kills the app). `adb logcat | grep AccessibilityNodeInfoDumper`
surfaces zero-bounds widgets the dumpfile omits.
