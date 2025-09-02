HPS Annual Census: Action-Based Refactor

Overview
- Splits the original multi-step `hps_annual_census.json` (12 steps) into single-step, action-driven forms.
- Introduces `HpsAnnualCensusVisitActivity` and `HpsAnnualCensusVisitInteractor` to orchestrate actions.
- Provides helpers for steps 1–2 as references for pre-processing, payload handling, and status.

Forms
- Step 1: `json.form/hps_annual_census_step1_population.json`
- Step 2: `json.form/hps_annual_census_step2_nutrition_sources.json`
- Placeholders (3–12): `json.form/hps_annual_census_step<3..12>_*.json`
  - SW (localized) copies under `json.form-sw/`.

Code
- Activity: `src/main/java/org/smartregister/chw/activity/HpsAnnualCensusVisitActivity.java`
- Interactor: `src/main/java/org/smartregister/chw/interactor/HpsAnnualCensusVisitInteractor.java`
- Helpers:
  - `HpsAnnualCensusStep1PopulationActionHelper`
  - `HpsAnnualCensusStep2NutritionSourcesActionHelper`

Wiring
- Add button in `HpsAnnualCensusRegisterFragment` now launches the new visit activity with a generated entity ID.

Next Steps
- Implement actions + helpers for steps 3–12 using the Step 1–2 pattern.
- Migrate edit mode: route existing records to the action-based flow, pre-filling details per action.
- Rules: If cross-step constraints exist, inject needed values via the helper `getPreProcessed()` into the later form’s `global` block.
- Localization: Add or adjust string resources for action titles/subtitles where needed.

