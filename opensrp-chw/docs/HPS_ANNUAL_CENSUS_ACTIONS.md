HPS Annual Census: Action-Based Refactor

Overview
- Splits the original multi-step `hps_annual_census.json` (12 steps) into single-step, action-driven forms.
- Introduces `HpsAnnualCensusVisitActivity` and `HpsAnnualCensusVisitInteractor` to orchestrate actions.
- Provides helpers for steps 1–2 as references for pre-processing, payload handling, and status.

Forms
- Step 1: `json.form/hps_annual_census_step1_population.json`
- Step 2: `json.form/hps_annual_census_step2_nutrition_sources.json`
- Step 3: `json.form/hps_annual_census_step3_centers.json` / `json.form-sw/hps_annual_census_step3_centers.json`
  - Title: EN "Healthcare services, education, child and elder care centers" / SW "Huduma za Afya, Elimu, Vituo vya Kulelea Watoto na Wazee"
  - Extracted the entire `step3.fields` into a single-step layout (`step1`). Relevance preserved via rules-engine.
  - New rules file: `src/nacp/assets/rule/hps_annual_census_centers.yml` (extracted from `hps_annual_census.yml`, only step3 rules).
    - Normalized rule names and conditions from `step3_*`/`step3_select_centers_category` to `step1_*`/`step1_select_centers_category` to match the single-step context.
    - Both EN and SW forms now reference `hps_annual_census_centers.yml`.
  - Note: Step 3 has no cross-step constraints; only visibility rules based on `select_centers_category`.
- Step 4: `json.form/hps_annual_census_step4_social_economic.json` / `json.form-sw/hps_annual_census_step4_social_economic.json`
  - Title: EN "Social services and economic activities" / SW "Vyanzo vinavyotumiwa zaidi na kaya"
  - Extracted all step4 fields; added hidden `step1.household_max` fed via rules from `global.household_max` and injected by helper; normalized constraints:
    - `lessThanEqualTo(., step1:number_of_house_hold)` → `lessThanEqualTo(., step1:household_max)`
    - `lessThanEqualTo(., step4:...)` → `lessThanEqualTo(., step1:...)`
- Step 5: `json.form/hps_annual_census_step5_committees_traditional_medicine.json` / `json.form-sw/hps_annual_census_step5_committees_traditional_medicine.json`
  - Title: EN "Committee meetings & Traditional medicine" / SW "Ufanisi wa Vikao vya Kamati vya Afya & Huduma za Tiba Mbadala Zinazotolewa"
  - Extracted all step5 fields; normalized cross-field constraints:
    - `lessThanEqualTo(., step5:...)` → `lessThanEqualTo(., step1:...)`
  - Added hidden `step1.household_max` (fed from `global.household_max`) to align with dependency pattern; helper injects value from Step 1.
- Step 6: `json.form/hps_annual_census_step6_environment_sanitation.json` / `json.form-sw/hps_annual_census_step6_environment_sanitation.json`
  - Title: EN "Environmental and sanitation Inspection report" / SW "Taarifa ya Ukaguzi na Usafi wa Mazingira"
  - Extracted all step6 fields from aggregated forms.
  - Normalized constraints to single-step: `lessThanEqualTo(., step1:number_of_house_hold)` → `lessThanEqualTo(., step1:household_max)`; mirrored `global.number_of_house_hold` for completeness.
  - Added hidden `step1.household_max` and `step1.number_of_house_hold` fed via rules from `global` and injected by helper.
- Placeholders (4–12): `json.form/hps_annual_census_step<4..12>_*.json`
  - SW (localized) copies under `json.form-sw/`.

Code
- Activity: `src/main/java/org/smartregister/chw/activity/HpsAnnualCensusVisitActivity.java`
- Interactor: `src/main/java/org/smartregister/chw/interactor/HpsAnnualCensusVisitInteractor.java`
- Helpers:
  - `HpsAnnualCensusStep1PopulationActionHelper`
  - `HpsAnnualCensusStep2NutritionSourcesActionHelper`
  - `HpsAnnualCensusStep3CentersActionHelper`
  - `HpsAnnualCensusStep4SocialEconomicActionHelper`
  - `HpsAnnualCensusStep5CommitteesTraditionalMedicineActionHelper`
  - `HpsAnnualCensusStep6EnvironmentSanitationActionHelper`

Wiring
- Add button in `HpsAnnualCensusRegisterFragment` now launches the new visit activity with a generated entity ID.

Next Steps
- Implement actions + helpers for steps 3–12 using the Step 1–2 pattern.
- Migrate edit mode: route existing records to the action-based flow, pre-filling details per action.
- Rules: If cross-step constraints exist, inject needed values via the helper `getPreProcessed()` into the later form’s `global` block.
- Localization: Add or adjust string resources for action titles/subtitles where needed.
