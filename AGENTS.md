# OpenSRP CHW application guidance

## Role in the stack

This repository builds the concrete `org.smartregister.chw` Android
application. It is the final customization layer above
`opensrp-client-chw-core`, which in turn integrates feature libraries including
`opensrp-client-chw-mother-mentor`.

The main module is `opensrp-chw/`. The active product flavor is `nacp`; its
debug unit-test task is `:opensrp-chw:testNacpDebugUnitTest` and its debug APK
task is `:opensrp-chw:assembleNacpDebug`.

## Where behavior lives

- `application/ChwApplication.java` initializes Core, Family, ANC, PNC,
  reporting, Mother Mentor, and other feature libraries. Initialization order,
  app/database versions, repositories, and feature flags are significant.
- `activity/` and `fragment/` contain concrete screens, usually extending a
  `Core*` or feature-library `Base*` class.
- `presenter/`, `interactor/`, `model/`, and `contract/` implement the existing
  MVP conventions. Keep Android view code out of interactors/models.
- `configs/`, `*Flv` classes, navigation models, and application `Flavor`
  implementations are the preferred application customization points.
- `dao/`, `repository/`, `sync/`, `service/`, `job/`, `task/`, and `schedulers/`
  own persistence and background processing. Preserve offline-first behavior
  and idempotent sync/event processing.
- `src/main/assets/json.form*`, `assets/rule`, reporting YAML, and `res/values*`
  jointly define workflows and user-visible behavior.

## Dependency and build constraints

- The app currently consumes the published
  `org.smartregister:opensrp-client-chw-core:2.2.0-MOTHER-MENTOR-MOH-SNAPSHOT`
  AAR. The optional local project dependency in `settings.gradle` is commented
  out; do not assume edits in the sibling core repository automatically affect
  this app build.
- The app has intentional dependency exclusions, direct replacement
  dependencies, local AAR fallbacks, and a local plan-evaluator JAR fallback.
  Preserve these unless explicitly validating a migration.
- Before building, follow `README.md` to sync `legacy-opensrp-libs/` into the
  required Maven cache/mirror. Do not replace the checked-in AARs under
  `opensrp-chw/libs/` casually.
- Mapbox and GitHub Packages credentials are local secrets. Never hardcode,
  disclose, or commit them.
- Compile/target SDK are configured centrally; Java and Kotlin targets are 1.8.
  Avoid broad modernization while implementing an unrelated feature.

## Forms and localization

- A form change often requires coordinated edits to JSON, YAML relevance or
  calculation rules, constants, processors, SQL queries, tests, and strings.
- Preserve field `key`, `openmrs_entity`, `openmrs_entity_id`, event type, and
  encounter type values unless a migration is part of the task.
- Keep default, Swahili (`json.form-sw`, `values-sw`), and any corresponding
  French (`json.form-fr`, `values-fr`) assets structurally aligned.
- Prefer adding focused asset/config regression tests under
  `opensrp-chw/src/test` for form keys, options, translations, and rule wiring.

## Validation

- Focused unit test: `./gradlew :opensrp-chw:testNacpDebugUnitTest --tests '<fully.qualified.TestClass>'`
- Full app unit tests: `./gradlew :opensrp-chw:testNacpDebugUnitTest`
- Debug APK: `./gradlew :opensrp-chw:assembleNacpDebug`
- Coverage: `./gradlew :opensrp-chw:jacocoTestReport`

Instrumentation tests require a connected emulator/device. If a change is in
a consumed library, validate and publish/use that library version before
treating the app build as proof of the library change.

