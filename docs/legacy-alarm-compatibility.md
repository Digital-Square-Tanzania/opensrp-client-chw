Legacy alarm compatibility
==========================

The app retains `android-job:1.4.3` and Mapbox telemetry `4.5.1` to preserve its
existing scheduling and map APIs. Both versions create alarm `PendingIntent`s
without mutability flags. Android rejects those calls when targeting API 31+.
Telemetry also registers an app-only receiver without an export flag, which
Android rejects when targeting API 34+.

`org.smartregister.legacy-alarm-compatibility` uses AGP's supported dependency
instrumentation API. It selects exactly `JobProxy14`, `AlarmSchedulerFlusher`
and `LocationEngineControllerImpl`. At build time, it redirects only their known
platform calls to the ordinary Java methods in `LegacyAlarmCompat`:

- Job alarm creation adds `FLAG_IMMUTABLE`, available at the app's minimum API
  23. Request codes, explicit receiver intents, payloads, `FLAG_NO_CREATE`,
  `FLAG_ONE_SHOT` and update flags remain unchanged.
- Each of the two legacy cancellation calls skips an absent token independently.
  Creating an immutable job token retires only its matching pre-upgrade mutable
  token, with the same explicit component, action, job ID and one-shot mode.
  A lookup-only request checks immutable then legacy tokens without creating,
  updating or cancelling either. Other job IDs and repeating modes remain
  untouched. Legacy lookup covers unflagged mutable tokens below API 31 and
  explicitly mutable tokens on API 31+. Retirement of unflagged tokens retained
  across an Android/target-SDK upgrade to API 31+ is not established: adding
  `FLAG_MUTABLE` does not reproduce the older token identity. The API 31 upgrade
  fixture uses an explicitly mutable token and must not be described as an exact
  reproduction of the old unflagged dependency.
- Telemetry copies its alarm intent for delivery within this app. Its receiver
  uses `RECEIVER_NOT_EXPORTED` on API 33+ and the legacy registration overload
  below that. Its location engine uses the same private receiver registration,
  with a package-scoped **mutable** token on API 31+ so location providers can
  attach result extras. Older Android retains its default mutability. Actions,
  callbacks, alarm timing and unregister behavior remain.

This does not disable jobs, telemetry, or mapping. No patched binary is stored
in this repository. Gradle constraints pin the reviewed dependency versions,
and the visitor fails the build if their expected call sites change. Each
redirection has an equivalent stack shape, so upstream frames and control flow
remain unchanged.

Validation:

- `./gradlew -p buildSrc test` loads both pinned upstream AARs. It checks every
  invocation in all three adapted classes, including both cancellation sites,
  checks the exact class filter and rejects changed upstream call sites.
- Run `./gradlew :opensrp-chw:testNacpDebugUnitTest` with the filter
  `--tests '*.LegacyAlarmCompatTest'`. The tests cover API 23/31 token identity,
  lookup flags, private alarm delivery, cancellation, pre-upgrade job tokens,
  mutable location-result delivery and receiver lifecycle. The API 33
  registration branch uses a recording Context in tests. Native API 36 testing
  covers the real platform call.
- Build and run the APK on API 31+. Inspect startup, background-job cleanup and
  telemetry initialization logs, and exercise a map screen.

The Mapbox bridge intentionally does not look up old unscoped mutable telemetry
alarms. Its native validation covers current private tokens and initialization.
Retirement of telemetry alarms created by a prior app on an older Android
version has not been established. The job-specific migration is not applied to
telemetry.

When the upstream dependencies are replaced by compatible versions, first
verify those paths on API 23 and current Android. Then remove the compatibility
plugin, constraints, helper and their tests together. Do not silently broaden
the class filter or skip its call-site checks after a dependency upgrade.

Platform references:

- [PendingIntent mutability](https://developer.android.com/guide/components/intents-filters#SpecifyMutability)
- [Context-registered receivers](https://developer.android.com/develop/background-work/background-tasks/broadcasts#context-registered-receivers)
- [AGP instrumentation API](https://developer.android.com/reference/tools/gradle-api/8.13/com/android/build/api/variant/Instrumentation)
