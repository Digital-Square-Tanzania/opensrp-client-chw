# Design review: CHW / Mother Mentor

## Summary

The starting UI had **Critical accessibility issues**: low contrast, undersized controls, clipped identity text, and actions that became unreachable with larger text. The implemented direction is a quiet field-work utility: familiar Android navigation, clear client identity, readable secondary information, and one consistent blue for primary actions. Apple Design Skill foundations were translated to Android `sp`, 48dp targets, Android scrolling and existing navigation.

The original audit evidence below was captured before the target branch advanced. Base: `implement/mother-mentor-module` at `fd69011173bbc755f576fc658ab434d6f2a08f4c`. Work branch: `ilakoze/chw-apple-design-audit`. App: NACP debug, Mother Mentor Beta 7, published Core and Mother Mentor dependencies retained.

## PR integration

UI PR [#1123](https://github.com/Digital-Square-Tanzania/opensrp-client-chw/pull/1123) integrates target commit `6367bfefd8b9e5a25e7e1044b8dcf102ddb042b6`, including database version 60, Core ALPHA-15 and Mother Mentor 0.1.13. The detailed results below and `validation.json` remain the original audit-stage evidence; [integration validation](integration-validation.json) records checks after this target update.

The four retained Mother Mentor layouts are deliberate accessibility overrides of published 0.1.13 layouts. Their pre-audit versions are byte-identical to those in the new AAR. All other target removals stay removed, including household forms/rules, unchanged layouts/drawables and duplicate translations. Only the corrected visible EID tag, new UI labels and three contrast color overrides remain in the app.

Current integration checks: **1,081/1,084 unit tests pass**, with the same three recorded navigation failures; **14 instrumented tests pass at each width**. Both APKs build. Original-profile data remains byte-identical. The final emulator screen is All Clients at normal text size.

## Critical findings implemented

| Finding and evidence | Implementation | Design reference |
| --- | --- | --- |
| White text on the original `#218CC5` action blue measured 3.74:1; secondary `#999999` on the debug sky background measured 1.64:1. | Primary action blue `#0B6E9E` gives 5.62:1 against white. Secondary `#4F4F4F` gives 8.19:1 on white and 4.70:1 on sky. English, Swahili and French resource overrides agree. Clinical status colors retain their meaning. | `accessibility.md` › Vision: “Strive to meet color contrast minimum standards.” |
| Login, PIN, routine questions, WASH, ANC and child rows used fixed text units/heights; the footer could collide with the form. | `sp` text, flexible field/button heights, a sequential scroll layout, clearer hints, labeled password visibility and useful IME/autofill settings. | `accessibility.md` › Vision: “Support larger text sizes.”; `typography.md` › Supporting Dynamic Type |
| Search/filter controls, history editing, job-aid refresh and visit controls had small or unlabeled targets. Native JSON radio/checkbox rows could measure below 48dp. | Minimum 48dp targets and labels for clear, cancel and refresh. Search retains usable width next to filters. Native form layout overrides keep factory IDs and clinical values. | `accessibility.md` › Mobility: “Offer sufficiently sized controls.” |
| At 200% text the All Clients metadata competed for one line; Mother Mentor gender/address/client ID lost readable space. | Actual Core-configured client row override puts register type on its own row and sex/location on a shared first-line baseline; explicit start/end padding removes the inherited 10dp indentation. The action column stays reserved. Mother Mentor profile metadata and CTC ID stack and wrap. Visit/history action contrast is corrected. | `layout.md` › Adaptability; `lists-and-tables.md` › Content |
| Long report menus and partner/child choices could place their last action below the visible area. Headings competed with chevrons. | Scrollable bodies under fixed toolbars, wrapping headings constrained before icons, reachable last choices. HPS/iCCM repeated decorative IDs are unique; action IDs remain stable. | `layout.md` › Adaptability; `buttons.md` › Best practices |

## Further improvements implemented

- **Medium:** localized display labels for HIV/PrEP filters, separate from canonical query values; labels remain associated with their controls.
- **Medium:** vaccination date selection can scroll into view, permission rationale keeps its buttons reachable, and KVP report tabs scroll when needed.
- **Medium:** PIN-choice text and buttons flow in a scrollable column; job-aid tabs scroll; vaccination section headings wrap, and compact toolbar labels stay readable.
- **Low:** the visible English Mother Mentor Child EID tag spelling is corrected; serialized identifiers remain unchanged.
- **Low:** removed inherited sample patient text from the new overrides and excluded the invisible native-form focus sentinel from accessibility traversal.

## Craft notes and patterns retained

Roboto/custom Android controls remain appropriate for repeated field work. Existing grouped navigation, clinical status labels, list-to-profile routing, history access, offline storage and draft-discard confirmation are useful patterns to keep. The main visual change is stronger hierarchy and legibility; no decorative motion or platform imitation was introduced. The repeated institutional logos remain confined to sign-in rather than spreading into work screens.

## Coverage and emulator evidence

The machine-readable inventories are exhaustive for the compiled manifest and app-owned main/NACP activity/customization files. **An inventory or inflated layout is not proof that an activity's complete workflow was exercised.**

- [Screen inventory](screen-inventory.json): 231 activity/customization source files, 133 layout variants / 130 unique layout names.
- [Compiled activity coverage](activity-coverage.json): 276 manifest declarations / 272 distinct activity classes, with explicit live checkpoints or an untraversed status.
- [Live routes](live-routes.json): all 26 modules exposed to the supplied test account opened through the drawer.
- [Validation receipt](validation.json): build, unit, emulator, data-preservation and exact APK details.
- [Lint comparison](lint-comparison.json): baseline-aware findings; the lint task is configured not to abort on existing errors.

Emulator: `emulator-5556`, Android 17 / API 37. Native resource tests render all 130 owned layouts at **320dp and 640dp, 200% system text**. Focused tests verify target bounds, search width, last-control scrolling, metadata containment, localized labels and action contrast. The form asset tests verify English/Swahili structure and serialization in memory. After the final All Clients arrangement change, focused geometry checks also pass at both widths. Live radio, close and save controls measure 81px at 270dpi, equal to 48dp.

Runtime evidence covers 42 distinct activity classes, including the instrumented filter/resource host; live traversal captures 41 classes and covers the supplied account's register routes; household, child, NCD, CBHS, TB/leprosy, KVP, ANC and PNC profiles; Mother Mentor profile/history, all three follow-up types, household and mobilization tabs, visit-type validation, the draft action list and linkage form opening. Drafts are discarded; no completed visit is submitted. Additional final checkpoints are listed in activity coverage. Screenshots containing synced test-client data remain in the private local evidence directory rather than the repository. Included PNGs use blank login fields or resource fixtures and are labeled accordingly.

## Validation limits and existing defects

- The full app unit run reports **1,067 tests, 1,064 passing and three failing GroupedNavigationMenuTest cases**. The same three fail on an untouched worktree of the starting commit (19 tests, three failures): `restrictedRolesRetainOnlyTheirAvailableModules`, `refreshedRoleOptionsUpdateAnExistingAdapterWithoutExposingOldModules`, and `disabledMotherMentorDoesNotRemoveOtherRoleModulesOrExposeChampion`.
- Lint has an existing backlog: 6,915 baseline findings and 6,862 after this change, with 61 resolved and 8 added reports. Added reports include analysis of copied library overrides: the legacy `com.rey.material.widget.ImageView` resolves and inflates on the emulator despite a lint MissingClass report; two overdraw reports; API 28 heading metadata; an unused dimension, the replaced PIN background and the former verbose follow-up title resource; and the retained vaccination FrameLayout wrapper. These are recorded rather than calling lint clean.
- Complete live validation of all 272 distinct activity classes is **not established**. Activities needing other roles, enrollment states, specialized clients, remote report data, printing/hardware or external apps remain individually marked as untraversed. Resource fixtures do not validate those prerequisites or business interactions. French values are resource-checked; French clinical workflow parity is not asserted. TalkBack speech, switch access and dark appearance were not exercised.
- The original emulator profile contains a database at version 60; this branch expects version 59. Attempting that original profile exposed the existing downgrade incompatibility. Validation therefore uses an ephemeral Android user with its own version-59 database. No database version or migration was changed to force compatibility. Original client/event preservation is checked separately in the receipt: all 188 table digests agree and the database is byte-identical. At the original audit checkpoint, the emulator was left on ephemeral user 10 with the corrected All Clients list at 720×1606 / 270dpi and 100% text, with Wi-Fi and mobile data off. The database downgrade limitation described here belongs to that earlier version-59 APK; the integrated PR now retains the target's database version 60.

## Reproducing focused checks

Use JDK 17 and the README's existing local Maven mirror/AAR setup. No sibling library edits or dependency publication are assumed.

```sh
./gradlew :opensrp-chw:testNacpDebugUnitTest --tests org.smartregister.chw.provider.MotherMentorAllClientsTagTest
./gradlew :opensrp-chw:assembleNacpDebug :opensrp-chw:assembleNacpDebugAndroidTest
./gradlew :opensrp-chw:lintNacpDebug
adb -s emulator-5556 shell am instrument --user TEST_USER -w \
  -e class org.smartregister.chw.activity.UiAccessibilityInstrumentedTest,org.smartregister.chw.activity.MotherMentorFormAssetsInstrumentedTest \
  org.smartregister.chw.moh.testing.test/androidx.test.runner.AndroidJUnitRunner
```

To repeat the full layout sweep, pass `-e auditLayouts` with the comma-separated unique layout names from the screen inventory. Tests save fixtures/results under the target app's private `files/ui-audit` directory. Use a disposable compatible profile; do not clear an existing user's application data.

## Skill provenance and references

Project-local skill: [Apple Design Skill](../../.agents/skills/apple-design/SKILL.md), pinned to `cozej4/apple-design-skill` revision `914b6a377c22755597e4405e54911b996de822d7`; provenance in `SOURCE.json`. Read references: accessibility, layout, typography, color, designing-for-ios, cross-platform, buttons, entering-data, lists-and-tables, searching and code-audit. Foundation guidance applies here; Apple navigation conventions do not replace Android conventions. Reference documents are the pinned skill snapshot, not a claim about a newly fetched HIG revision.

## Final alignment correction and shared source review

The sex/location arrangement was reviewed again after user feedback. The previous weighted register-type field pushed sex to the right edge; `opensrp-client-core`'s `CustomFontTextViewStyle` also contributes `paddingStart=10dp`, which caused the location indent. The app override now gives register type its own row and uses a common first-line baseline for sex, separator and wrapping location. Focused emulator tests explicitly assert that location has zero start padding. Initial regression failure on inherited padding was corrected and both width checks pass.

Shared source reviewed at `opensrp-client-core` commit `7b2e4748889695ca8762d3d6f67b81a4a24bfe3f`: `BaseRegisterFragment`, `RecyclerViewFragment.setupSearchView`, `CustomFontTextView`, shared search/sort layouts, and text styles. That checkout remains unchanged; the compiled app consumes its configured published dependencies.

The template clipping scan still flags summary ellipses in prioritized-child and WASH rows, the inherited KVP sort/filter status strip at 320dp, and zero-size Mother Mentor separator labels. These candidates are recorded in the template JSON; they do not establish a clean TalkBack or all-state accessibility audit.

## Privacy-safe visual evidence

Blank-field live Swahili login: [before](evidence/login-before-sw.png), [after](evidence/login-after-sw.png). Native resource fixtures at 200% text, using no synced clients: [profile identity](evidence/mother-profile-fixture-200.png), [PIN choice](evidence/pin-choice-fixture-200.png), [filters](evidence/filters-fixture-200.png), [last report action](evidence/report-fixture-200.png). Populated All Clients before/after screenshots remain in the private local evidence directory.
