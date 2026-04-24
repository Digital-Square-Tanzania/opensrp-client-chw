---
name: android-running-device-integration
description: Use when Codex must connect to a running Android device or emulator with adb, keep that connection stable across sandboxed sessions, drive the UI from UIAutomator dumps, capture evidence, and optionally pull app data or databases for verification.
---

# Android Running Device Integration

Use this skill when the user needs a real device or emulator test, not just code inspection.

This skill assumes the app is already installed or can be installed from the current repo.

## When to use

- A connected Android emulator or device must be driven with `adb`.
- The task requires launch, navigation, screenshots, logcat, or database pulls.
- The workflow depends on changing device state such as date, login state, or app process state.

## First step: prove device access

Run:

- `adb devices -l`

If this fails with errors such as:

- `ADB server didn't ACK`
- `could not install *smartsocket* listener: Operation not permitted`
- `adb: cannot connect to daemon`

then treat that as an environment blocker, not an app blocker.

Response rules:

1. If the session allows escalated execution, retry `adb` with escalation immediately.
2. If the first successful `adb` command required escalation, keep using escalated `adb` commands for the rest of the session.
3. If approval policy is `never` and `adb` is not already running outside the sandbox, stop and report that real device testing is blocked by environment access.
4. Do not pretend a device retest happened if `adb` never became usable.

For common failure modes, read [references/troubleshooting.md](references/troubleshooting.md).

## Stable adb policy

Once `adb devices -l` succeeds:

1. Record the serial, for example `emulator-5554`.
2. Use `adb -s <serial> ...` for all further commands.
3. Prefer one adb command per shell invocation when the daemon has been unstable.
4. If a later adb command fails with a daemon startup error, rerun it with the same escalation level that made `adb devices -l` work.

## Discover and launch the app

Confirm the package is installed:

- `adb -s <serial> shell pm list packages | rg <package-or-namespace>`

Resolve the current launchable activity:

- `adb -s <serial> shell cmd package resolve-activity --brief <package>`

Launch the app:

- `adb -s <serial> shell am start -n <package>/<activity>`

Force-stop before clean test loops:

- `adb -s <serial> shell am force-stop <package>`

## Drive the UI from the UI tree

Do not guess coordinates from screenshots.

Workflow:

1. Dump the current UI tree:
   - `adb -s <serial> exec-out uiautomator dump /dev/tty > /tmp/ui-step.xml`
2. Inspect the target node text, content description, resource id, and bounds.
3. Compute the tap point from the node bounds center.
4. Tap with:
   - `adb -s <serial> shell input tap <x> <y>`
5. Re-dump the UI tree after each meaningful transition.

Useful commands:

- Text:
  - `adb -s <serial> shell input text 'value'`
- Back:
  - `adb -s <serial> shell input keyevent 4`
- Enter:
  - `adb -s <serial> shell input keyevent 66`
- Swipe:
  - `adb -s <serial> shell input swipe <x1> <y1> <x2> <y2>`

If the installed Android emulator QA skill is available, use its helper scripts to summarize UI trees or pick bounds. Otherwise inspect the XML directly with `rg`.

## Capture evidence

Save screenshots at key checkpoints:

- `adb -s <serial> exec-out screencap -p > /tmp/<step>.png`

Save logs when debugging:

- Clear logs:
  - `adb -s <serial> logcat -c`
- Save logs:
  - `adb -s <serial> logcat -d > /tmp/<step>.log`
- Crash buffer:
  - `adb -s <serial> logcat -b crash`

Minimum evidence for a real retest:

1. Device serial
2. Package and resolved activity
3. UI dump or screenshot before the critical action
4. UI dump or screenshot after save or transition
5. Logs or DB output if validating persistence

## Control device state

Always record the active device date when schedule logic matters:

- `adb -s <serial> shell date`

If the test requires time travel:

1. Prefer an emulator or rooted build.
2. If supported, use root-enabled date changes such as:
   - `adb -s <serial> root`
   - `adb -s <serial> shell su 0 date -s YYYYMMDD.HHMMSS`
3. Verify immediately with:
   - `adb -s <serial> shell date`

If the device cannot change time programmatically and the test depends on time simulation, stop and report that the required test cannot be completed on that device configuration.

## Pull files and databases

For debug builds, prefer `run-as <package>`. For emulators, `adb root` may also work.

Typical flow:

1. Locate target directories:
   - `/data/data/<package>/databases/`
   - `/data/data/<package>/shared_prefs/`
2. Use `run-as` or rooted shell to copy files into a world-readable temp location on the device.
3. Pull the copied files:
   - `adb -s <serial> pull <device-path> <local-path>`

If the app uses SQLCipher, obtain the key from app configuration or shared preferences before querying the DB.

For OpenSRP CHW specifics in this repo, read [references/opensrp-chw.md](references/opensrp-chw.md).

## Recommended verification loop

For each simulated visit or scenario:

1. Set device state.
2. Force-stop the app.
3. Launch the app.
4. Navigate using UI-tree-driven taps.
5. Capture UI evidence before save.
6. Perform the action.
7. Capture UI evidence after save.
8. Pull DB or logs if persistence matters.
9. Validate against the expected schedule, records, or business rule.

## Report honestly

A valid final report should separate:

- what was executed on the device
- what was only verified by code or tests
- what was blocked by the environment

Never claim a device workflow passed if `adb` access, date control, or DB access was not actually achieved.
