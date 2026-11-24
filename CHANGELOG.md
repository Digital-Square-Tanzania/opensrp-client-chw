# Changelog

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
