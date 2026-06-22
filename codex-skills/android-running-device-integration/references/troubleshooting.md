# Troubleshooting

## adb daemon startup fails inside Codex

Typical errors:

- `ADB server didn't ACK`
- `could not install *smartsocket* listener: Operation not permitted`
- `adb: failed to check server version: cannot connect to daemon`

Meaning:

- The session cannot start or bind the adb daemon inside the sandbox.

What to do:

1. Retry the adb command with escalated execution if the session allows it.
2. If `adb devices -l` succeeds only with escalation, keep all further adb work on the escalated path.
3. If the session does not allow escalation, require a daemon already started outside the sandbox.

## `adb devices -l` works once, later adb commands fail

Meaning:

- The successful command likely reused a daemon that is no longer reachable from the sandbox.

What to do:

1. Rerun the failing adb command with escalation.
2. Avoid parallel adb invocations when the daemon has been unstable.
3. Prefer one adb command per shell process.

## App opens on an unexpected activity

What to do:

1. Re-resolve the activity:
   - `adb -s <serial> shell cmd package resolve-activity --brief <package>`
2. Launch the resolved activity, not the previously cached one.
3. Expect first-run permission or onboarding screens to change the entry activity.

## Taps stop working or hit the wrong control

What to do:

1. Dump a fresh UI tree.
2. Recompute coordinates from the latest bounds.
3. If the target node is absent, swipe and dump again.
4. Prefer resource ids or exact visible text over screenshot guessing.

## Device date cannot be changed

What to do:

1. Check whether the target is a rooted emulator or debug build.
2. Try root-based date change commands.
3. If time change is blocked and the test depends on future schedule windows, report the device limitation clearly.

## DB pull fails

What to do:

1. Check whether the package is debuggable.
2. Try `run-as <package>`.
3. On an emulator, try rooted access if allowed.
4. Copy the DB to a pullable temp location before `adb pull`.

## Final rule

Always distinguish:

- environment blockers
- app failures
- workflow failures caused by incomplete permissions or device access
