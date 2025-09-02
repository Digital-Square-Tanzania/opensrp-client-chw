HPS Annual Census — Split Steps 7–12

- EN forms:
  - `opensrp-chw/src/nacp/assets/json.form/hps_annual_census_step7_building_inspection.json`
  - `opensrp-chw/src/nacp/assets/json.form/hps_annual_census_step8_workplace_inspection.json`
  - `opensrp-chw/src/nacp/assets/json.form/hps_annual_census_step9_food_beverage_inspection.json`
  - `opensrp-chw/src/nacp/assets/json.form/hps_annual_census_step10_workplace_health_reports.json`
  - `opensrp-chw/src/nacp/assets/json.form/hps_annual_census_step11_solid_waste.json`
  - `opensrp-chw/src/nacp/assets/json.form/hps_annual_census_step12_insect_breeding_control.json`

- SW forms:
  - `opensrp-chw/src/nacp/assets/json.form-sw/hps_annual_census_step7_building_inspection.json`
  - `opensrp-chw/src/nacp/assets/json.form-sw/hps_annual_census_step8_workplace_inspection.json`
  - `opensrp-chw/src/nacp/assets/json.form-sw/hps_annual_census_step9_food_beverage_inspection.json`
  - `opensrp-chw/src/nacp/assets/json.form-sw/hps_annual_census_step10_workplace_health_reports.json`
  - `opensrp-chw/src/nacp/assets/json.form-sw/hps_annual_census_step11_solid_waste.json`
  - `opensrp-chw/src/nacp/assets/json.form-sw/hps_annual_census_step12_insect_breeding_control.json`

- Helpers:
  - `org.smartregister.chw.actionhelper.HpsAnnualCensusStep7BuildingInspectionActionHelper`
  - `org.smartregister.chw.actionhelper.HpsAnnualCensusStep8WorkplaceInspectionActionHelper`
  - `org.smartregister.chw.actionhelper.HpsAnnualCensusStep9FoodBeverageInspectionActionHelper`
  - `org.smartregister.chw.actionhelper.HpsAnnualCensusStep10WorkplaceHealthReportsActionHelper`
  - `org.smartregister.chw.actionhelper.HpsAnnualCensusStep11SolidWasteActionHelper`
  - `org.smartregister.chw.actionhelper.HpsAnnualCensusStep12InsectBreedingControlActionHelper`

Notes

- Each form is single-step with `count: "1"`, `encounter_type: "HPS Annual Census"`, `entity_id: ""` and fields wrapped under `step1`.
- Titles: EN/SW titles match action labels for each step.
- Rules: All relevance and rules-engine `rules-file` references (e.g., `hps_annual_census.yml`) are preserved.
- Constraint normalization:
  - Step-internal references were localized to `step1:` context. Examples adjusted:
    - `lessThanEqualTo(., step7:number_of_food_shop_visited)` → `lessThanEqualTo(., step1:number_of_food_shop_visited)`
    - `lessThanEqualTo(., step8:number_of_inspected_agriculture_areas)` → `lessThanEqualTo(., step1:number_of_inspected_agriculture_areas)`
    - `lessThanEqualTo(., step9:number_of_inspected_grains)` → `lessThanEqualTo(., step1:number_of_inspected_grains)`

Interactor wiring

- Add actions in `HpsAnnualCensusVisitInteractor` following the existing Step 1–2 pattern, resolving the localized form name via `Utils.getLocalForm("hps_annual_census_step<STEP>_<slug>", context)` and using the matching helper:
  - Step 7 (Building Inspection Report) → `HpsAnnualCensusStep7BuildingInspectionActionHelper`
  - Step 8 (Workplace Inspection Report) → `HpsAnnualCensusStep8WorkplaceInspectionActionHelper`
  - Step 9 (Food and Beverage Inspection Report) → `HpsAnnualCensusStep9FoodBeverageInspectionActionHelper`
  - Step 10 (Health Reports Affecting People in Workplaces) → `HpsAnnualCensusStep10WorkplaceHealthReportsActionHelper`
  - Step 11 (Solid waste & waste collection equipments) → `HpsAnnualCensusStep11SolidWasteActionHelper`
  - Step 12 (Identification and Control of Insect Breeding Sites) → `HpsAnnualCensusStep12InsectBreedingControlActionHelper`

- None of these steps depend on prior inputs; no callback/injection is required beyond the default `onJsonFormLoaded`/`getPreProcessed` flow.

