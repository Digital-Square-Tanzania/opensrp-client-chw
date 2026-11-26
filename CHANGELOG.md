# Changelog

## Mother Mentor Batch 2 publication preparation

- Read Mapbox download credentials from local Gradle properties. Remove the tracked assignment from this release lineage. [UCSW-201](https://multica.ai/ucs-workspace/issues/UCSW-201)

## Mother Mentor Closed partner register — 2026-11-26

- Use finalized partner visit evidence for the Closed register when the enrollment
  summary lags, and show the saved closure date with the localized status.
  [CHW #1244](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1244)

## Mother Mentor Child EID visit draft — 2026-11-25

- Preserve saved tracing and HEI section answers when Android recreates an open Child EID visit. Apply the returning section to the restored visit before the CHW saves it. [CHW #1248](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1248)

## Mother Mentor Alpha 69 integration candidate — 2026-11-25

- Use the same breastfeeding questions and answers on both Mtoto EID enrolment routes. Use Core `2.2.0-MOTHER-MENTOR-ALPHA-69-UCSW-149-HEI-BREASTFEEDING-MOH-SNAPSHOT`. [CHW #1252](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1252)

## Mother Mentor partner visit history — 2026-11-25

- Label Mwenzi visit sections and dated cards as partner visits in English
  and Kiswahili. [CHW #1251](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1251)

## Mother Mentor outcome dates in visit history — 2026-11-25

- Show the saved delivery, preterm delivery or loss date below the corresponding
  pregnancy outcome. [CHW #1250](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1250)

## Mother Mentor specify fields — 2026-11-25

- Limit text entered or pasted into Mother Mentor specify fields to 100 characters,
  with a live counter and Save validation. [CHW #1249](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1249)

## Mother Mentor Alpha 68 integration candidate — 2026-11-25

- Open documented pending confirmation samples on the confirmation result form
  using Core `2.2.0-MOTHER-MENTOR-ALPHA-68-UCSW-147-CONFIRMATION-RESULT-MOH-SNAPSHOT`. [CHW #1248](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1248)

## Mother Mentor Alpha 67 integration candidate — 2026-11-25

- Localize child-sex captions in HEI registration, child lists and visit
  History, including older saved answers. Use Core `2.2.0-MOTHER-MENTOR-ALPHA-67-UCSW-148-CHILD-SEX-CAPTIONS-MOH-SNAPSHOT`. [CHW #1247](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1247)

## Mother Mentor Alpha 66 integration candidate — 2026-11-25

- Use the same localized post-weaning test name in Mother Mentor forms, lists
  and History. Apply the after-nine-month eligibility rule in Core
  `2.2.0-MOTHER-MENTOR-ALPHA-66-UCSW-146-POST-WEANING-CUTOFF-MOH-SNAPSHOT`. [CHW #1246](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1246)

## Mother Mentor Alpha 65 integration candidate — 2026-11-25

- Leave the Mtoto EID due-test display blank until the weaning date is available
  when breastfeeding has stopped. Use Core `2.2.0-MOTHER-MENTOR-ALPHA-65-UCSW-140-MTOTO-EID-DUE-TEST-MOH-SNAPSHOT`. [CHW #1245](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1245)

## Mother Mentor Alpha 64 integration candidate — 2026-11-25

- Record not-found outcomes on open post-acceptance Mwenzi visits with reason-specific closure using
  Core `2.2.0-MOTHER-MENTOR-ALPHA-64-UCSW-139-MWENZI-EXIT-ALIGNMENT-MOH-SNAPSHOT`. [CHW #1242](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1242)
- Show the child's sex in the app language during HEI registration while preserving
  the stored sex value. [CHW #1243](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1243)
- Format the HEI birth date and closed Mwenzi date as DD-MM-YYYY, show the
  localized breastfeeding status in Mama Kadi history, and use the localized
  Kaya title with a location-only profile subtitle. [CHW #1244](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1244)

## Mother Mentor Alpha 62 integration candidate — 2026-11-25

- Open the selected Mtoto EID visit in History to correct its weaning date
  while preserving visit order. Use Core `2.2.0-MOTHER-MENTOR-ALPHA-62-UCSW-142-WEANING-DATE-RECONCILED-MOH-SNAPSHOT`. [CHW #1241](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1241)

## Mother Mentor Alpha 61 integration candidate — 2026-11-25

- Record a finalized Mwenzi tracing death on the linked adult household
  member and retain the partner's completed visits in Closed History. Use Core
  `2.2.0-MOTHER-MENTOR-ALPHA-61-UCSW-151-MWENZI-DEATH-MOH-SNAPSHOT`.
  [CHW #1238][m8]

## Mother Mentor Alpha 57 integration candidate — 2026-11-24

Core snapshot
2.2.0-MOTHER-MENTOR-ALPHA-57-UCSW-131-THIRD-EVENT-SAVE-CONFIRMATION-MOH-SNAPSHOT

- Exclude removed household members from the Members list when their death date
  is recorded. Keep the child's closed Mtoto EID record and visit history
  available. [CHW #1238][m8]
- Confirm before saving a third consecutive event that closes a Kaya, Mtoto EID,
  Mwenzi, or Mama IIT record. [CHW #1240][m0]
- Verify direct saves for successful Mtoto EID exits and reason-only
  confirmation for third-strike exits. [CHW #1240][m0]
- Require the separate Mama IIT death confirmation before saving a staged death
  when the closure preview is still active. [CHW #1239][m9] · [CHW #1240][m0]

## Mother Mentor Alpha 56 integration candidate — 2026-11-24

- Show count-free Mother Mentor follow-up statuses and use Core
  `2.2.0-MOTHER-MENTOR-ALPHA-56-UCSW-130-HIDE-DEVELOPER-NOTES-MOH-SNAPSHOT` for
  the form cleanup. [CHW #1239][m9]

## Mother Mentor Alpha 55 integration candidate — 2026-11-24

- Apply the profile death action to the correct household member after a Mother
  Mentor death visit. Hide the deceased row's due dot and service entry while
  preserving visit history. Use Core
  `2.2.0-MOTHER-MENTOR-ALPHA-55-UCSW-129-HOUSEHOLD-DEATH-STATUS-MOH-SNAPSHOT` .
  [CHW #1238][m8]

## Mother Mentor Alpha 54 integration candidate — 2026-11-24

- Apply the inclusive 19-month child age limit when saving a new Not pregnant
  Mama Kinara enrollment or linking a child in Mtoto EID. Use CHW Core
  `2.2.0-MOTHER-MENTOR-ALPHA-54-UCSW-127-19-MONTH-CUTOFF-MOH-SNAPSHOT` .
  [CHW #1236][m6]
- Offer Mama Kinara and Mama IIT enrollment from client profiles to female
  clients aged 10 or older, with no upper age limit. [CHW #1237][m7]

[m0]: https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1240
[m6]: https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1236
[m7]: https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1237
[m8]: https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1238
[m9]: https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1239

## Mother Mentor Alpha 53 integration candidate — 2026-11-22

- Resolve the authenticated repository when Mother Mentor opens a service visit. Use CHW Core `2.2.0-MOTHER-MENTOR-ALPHA-53-UCSW-92-ENTRY-INTEGRATION-MOH-SNAPSHOT` to retain the first-visit delivery assessment for both feeding choices in NACP Beta.10 (UCSW-91 and UCSW-92).

## Mother Mentor Alpha 51 local candidate — 2026-11-22

- Use CHW Core `2.2.0-MOTHER-MENTOR-ALPHA-51-UCSW-92-LOCAL-MOH-SNAPSHOT` for the first-visit delivery count after breastfeeding enrollment in NACP Beta.10 (UCSW-92).

## Mother Mentor Alpha 50 — 2026-11-22

- Use CHW Core `2.2.0-MOTHER-MENTOR-ALPHA-50-MOH-SNAPSHOT` to bring the first-visit delivery count and child sections to the Mother Mentor app (UCSW-92).

## Mother Mentor Alpha 49 — 2026-11-22

- Show results and receipt dates from Mama Kadi follow-up in visit history. CHW Core `2.2.0-MOTHER-MENTOR-ALPHA-49-MOH-SNAPSHOT` supplies Mother Mentor `0.1.49-SNAPSHOT` and the questions about awaited HEI results. [CHW #1209](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1209)
- Use the revised monthly report row order, numbering, and lettered subrows in the app and exported PDF. [CHW #1211](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1211)
- Add indicators 2, 6, and 7. Indicator 7 selects mothers with a finalized visit in the reporting month and counts each recorded live birth through month-end using the actual delivery date. [CHW #1212](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1212)
- Update the counts for indicators 1, 8–15, and 19. Indicator 15 shows the full JUMLA and marks sex subtotals unavailable when the breakdown is incomplete. [CHW #1213](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/issues/1213)
