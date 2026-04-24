# OpenSRP CHW Notes

Use this reference when the target repo is `opensrp-client-chw` or a close fork.

## App and credentials

- Expected NACP testing package:
  - `org.smartregister.chw.moh.testing`
- If login is required, ask the user for the NACP test username and password for the current session. Do not store those values in this reference, logs, screenshots, or Git history.

## Focused automated tests

Use Java 17 for Gradle commands.

Focused immunization verification command:

```sh
/bin/zsh -c 'JAVA_HOME="$(/usr/libexec/java_home -v 17)" PATH="$JAVA_HOME/bin:$PATH" ./gradlew :opensrp-chw:testNacpDebugUnitTest --tests org.smartregister.chw.sync.ChwClientProcessorTest --tests org.smartregister.chw.interactor.DefaultChildHomeVisitInteractorFlvNormalizationTest'
```

These tests cover:

- vaccine event persistence using `obs.formSubmissionField` instead of a date string for `vaccines.name`
- vaccine name normalization for schedule matching
- filtering previously recorded vaccines from pending vaccines
- merging medical-history and visit-detail vaccines without treating `VACCINE_NOT_GIVEN` as administered

## Vaccine schedule assets

Primary NACP assets:

- `opensrp-chw/src/nacp/assets/vaccines.json`
- `opensrp-chw/src/nacp/assets/ec_client_vaccine.json`

Inspect these when later-dose vaccines appear incorrectly or persisted names do not match scheduler expectations.

## Child home visit flow

For immunization actions to appear in a child home visit:

1. Complete `Visit Location`
2. Complete `Danger signs`
3. Select `None`

Only after that should the remaining actions, including immunization, become available.

## Relevant code paths

- `opensrp-chw/src/main/java/org/smartregister/chw/interactor/ChildHomeVisitInteractor.java`
- `opensrp-chw/src/main/java/org/smartregister/chw/interactor/DefaultChildHomeVisitInteractorFlv.java`
- `opensrp-chw/src/nacp/java/org/smartregister/chw/interactor/ChildHomeVisitInteractorFlv.java`
- `opensrp-chw/src/main/java/org/smartregister/chw/actionhelper/ImmunizationValidator.java`
- `opensrp-chw/src/main/java/org/smartregister/chw/actionhelper/ImmunizationActionHelper.java`
- `opensrp-chw/src/main/java/org/smartregister/chw/fragment/DefaultBaseHomeVisitImmunizationFragment.java`
- `opensrp-chw/src/main/java/org/smartregister/chw/sync/ChwClientProcessor.java`

## DB validation

The app may use SQLCipher. In this workspace, the SQLCipher key has been derived from shared preferences using:

- `CURRENT_LOCATION_ID`

Common validation targets:

1. Child identity in `ec_child`
2. Persisted vaccine rows in `vaccines`
3. Visit details in `visit_details`
4. Detection of broken vaccine names that look like dates instead of field ids

Representative queries:

```sql
SELECT base_entity_id, first_name, middle_name, last_name, dob
FROM ec_child
WHERE first_name LIKE '%Vaccine%'
   OR last_name LIKE '%Schedule%';
```

```sql
SELECT base_entity_id, name, date, calculation, event_id, form_submission_id
FROM vaccines
WHERE base_entity_id = '<child_base_entity_id>'
ORDER BY date, name;
```

```sql
SELECT name, date
FROM vaccines
WHERE base_entity_id = '<child_base_entity_id>'
  AND name GLOB '____-__-__';
```

## Expected vaccine schedule checkpoints

Typical NACP stages to validate:

1. Birth:
   - `BCG`
   - `OPV 0`
2. 6 Weeks:
   - `OPV 1`
   - `PCV 1`
   - `Penta 1`
   - `Rota 1`
3. 10 Weeks:
   - `OPV 2`
   - `PCV 2`
   - `Penta 2`
   - `Rota 2`
4. 14 Weeks:
   - `OPV 3`
   - `PCV 3`
   - `Penta 3`
   - `IPV`
   - `Rota 3`
5. 9 Months:
   - `MR 1`
6. 18 Months:
   - `MR 2`

## Bug-specific checks

When retesting the immunization fix, confirm:

1. Earlier vaccines do not reappear as pending at later visits.
2. `Rota 3` appears at 14 weeks, not around 1 year.
3. `MR 2` appears only after `MR 1` and at 18 months.
4. Persisted `vaccines.name` values are schedule ids such as `opv_1`, not administered-date strings.
