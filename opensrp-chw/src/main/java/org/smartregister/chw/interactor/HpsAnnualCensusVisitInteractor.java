package org.smartregister.chw.interactor;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.R;
import org.smartregister.chw.actionhelper.HpsAnnualCensusStep10WorkplaceHealthReportsActionHelper;
import org.smartregister.chw.actionhelper.HpsAnnualCensusStep11SolidWasteActionHelper;
import org.smartregister.chw.actionhelper.HpsAnnualCensusStep12InsectBreedingControlActionHelper;
import org.smartregister.chw.actionhelper.HpsAnnualCensusStep1PopulationActionHelper;
import org.smartregister.chw.actionhelper.HpsAnnualCensusStep2NutritionSourcesActionHelper;
import org.smartregister.chw.actionhelper.HpsAnnualCensusStep3CentersActionHelper;
import org.smartregister.chw.actionhelper.HpsAnnualCensusStep4SocialEconomicActionHelper;
import org.smartregister.chw.actionhelper.HpsAnnualCensusStep5CommitteesTraditionalMedicineActionHelper;
import org.smartregister.chw.actionhelper.HpsAnnualCensusStep6EnvironmentSanitationActionHelper;
import org.smartregister.chw.actionhelper.HpsAnnualCensusStep7BuildingInspectionActionHelper;
import org.smartregister.chw.actionhelper.HpsAnnualCensusStep8WorkplaceInspectionActionHelper;
import org.smartregister.chw.actionhelper.HpsAnnualCensusStep9FoodBeverageInspectionActionHelper;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.chw.hps.HpsLibrary;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.hps.contract.BaseHpsVisitContract;
import org.smartregister.chw.hps.domain.Visit;
import org.smartregister.chw.hps.domain.VisitDetail;
import org.smartregister.chw.hps.interactor.BaseHpsServiceVisitInteractor;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;
import org.smartregister.chw.hps.util.Constants;
import org.smartregister.chw.hps.util.VisitUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Interactor that breaks HPS Annual Census into action-driven steps.
 */
public class HpsAnnualCensusVisitInteractor extends BaseHpsServiceVisitInteractor {
    private static boolean editMode;

    private BaseHpsVisitContract.InteractorCallBack callBack;
    private String householdCountValue;

    public HpsAnnualCensusVisitInteractor() {
        super(Constants.EVENT_TYPE.HPS_ANNUAL_CENSUS);
    }

    @Override
    protected void populateActionList(BaseHpsVisitContract.InteractorCallBack callBack) {
        this.callBack = callBack;
        final Runnable runnable = () -> {
            try {
                evaluateStep1Population(details);
                evaluateStep2Nutrition(details);
                evaluateStep3Centers(details);
                evaluateStep4SocialEconomic(details);
                evaluateStep5CommitteesTraditionalMedicine(details);
                evaluateStep6EnvironmentSanitation(details);
                evaluateStep7BuildingInspection(details);
                evaluateStep8WorkplaceInspection(details);
                evaluateStep9FoodBeverageInspection(details);
                evaluateStep10WorkplaceHealthReports(details);
                evaluateStep11SolidWaste(details);
                evaluateStep12InsectBreedingControl(details);
            } catch (BaseHpsVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void evaluateStep1Population(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        HpsAnnualCensusStep1PopulationActionHelper actionHelper = new HpsAnnualCensusStep1PopulationActionHelper(households -> {
            householdCountValue = households;
            // Recreate steps dependent on household value
            String nutritionTitle = getString(R.string.hps_annual_census_basic_nutrition_action_title);
            String socialEconomicTitle = getString(R.string.hps_annual_census_social_economic_action_title);
            String committeesTitle = getString(R.string.hps_annual_census_committees_traditional_action_title);
            actionList.remove(nutritionTitle);
            actionList.remove(socialEconomicTitle);
            actionList.remove(committeesTitle);
            try {
                evaluateStep2Nutrition(details);
                evaluateStep4SocialEconomic(details);
                evaluateStep5CommitteesTraditionalMedicine(details);
            } catch (BaseHpsVisitAction.ValidationException e) {
                throw new RuntimeException(e);
            }
            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        });

        String formName = Utils.getLocalForm("hps_annual_census_step1_population", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        String title = getString(R.string.hps_annual_census_population_action_title);
        BaseHpsVisitAction action = getBuilder(title)
                .withOptional(false)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(formName)
                .build();

        actionList.put(title, action);
    }

    private void evaluateStep2Nutrition(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        if (householdCountValue == null || householdCountValue.trim().isEmpty()) {
            // Defer adding step 2 until household count is known (after step 1)
            return;
        }
        HpsAnnualCensusStep2NutritionSourcesActionHelper actionHelper = new HpsAnnualCensusStep2NutritionSourcesActionHelper(householdCountValue);

        String formName = Utils.getLocalForm("hps_annual_census_step2_nutrition_sources", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);

        String title = getString(R.string.hps_annual_census_basic_nutrition_action_title);
        BaseHpsVisitAction action = getBuilder(title)
                .withOptional(true)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(formName)
                .build();

        actionList.put(title, action);
    }

    private void evaluateStep3Centers(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step3_centers", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        String title = getString(R.string.hps_annual_census_centers_action_title);
        BaseHpsVisitAction action = getBuilder(title)
                .withOptional(true)
                .withDetails(details)
                .withHelper(new HpsAnnualCensusStep3CentersActionHelper())
                .withFormName(formName)
                .build();
        actionList.put(title, action);
    }

    private void evaluateStep4SocialEconomic(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        if (householdCountValue == null || householdCountValue.trim().isEmpty()) {
            return;
        }
        String formName = Utils.getLocalForm("hps_annual_census_step4_social_economic", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        String title = getString(R.string.hps_annual_census_social_economic_action_title);
        BaseHpsVisitAction action = getBuilder(title)
                .withOptional(true)
                .withDetails(details)
                .withHelper(new HpsAnnualCensusStep4SocialEconomicActionHelper(householdCountValue))
                .withFormName(formName)
                .build();
        actionList.put(title, action);
    }

    private void evaluateStep5CommitteesTraditionalMedicine(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        if (householdCountValue == null || householdCountValue.trim().isEmpty()) {
            return;
        }
        String formName = Utils.getLocalForm("hps_annual_census_step5_committees_traditional_medicine", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        String title = getString(R.string.hps_annual_census_committees_traditional_action_title);
        BaseHpsVisitAction action = getBuilder(title)
                .withOptional(true)
                .withDetails(details)
                .withHelper(new HpsAnnualCensusStep5CommitteesTraditionalMedicineActionHelper(householdCountValue))
                .withFormName(formName)
                .build();
        actionList.put(title, action);
    }

    private void evaluateStep6EnvironmentSanitation(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        if (householdCountValue == null || householdCountValue.trim().isEmpty()) {
            return;
        }
        String formName = Utils.getLocalForm("hps_annual_census_step6_environment_sanitation", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        String title = getString(R.string.hps_annual_census_environment_sanitation_action_title);
        BaseHpsVisitAction action = getBuilder(title)
                .withOptional(true)
                .withDetails(details)
                .withHelper(new HpsAnnualCensusStep6EnvironmentSanitationActionHelper(householdCountValue))
                .withFormName(formName)
                .build();
        actionList.put(title, action);
    }

    private void evaluateStep7BuildingInspection(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step7_building_inspection", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        String title = getString(R.string.hps_annual_census_building_inspection_action_title);
        BaseHpsVisitAction action = getBuilder(title)
                .withOptional(true)
                .withDetails(details)
                .withHelper(new HpsAnnualCensusStep7BuildingInspectionActionHelper())
                .withFormName(formName)
                .build();
        actionList.put(title, action);
    }

    private void evaluateStep8WorkplaceInspection(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step8_workplace_inspection", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        String title = getString(R.string.hps_annual_census_workplace_inspection_action_title);
        BaseHpsVisitAction action = getBuilder(title)
                .withOptional(true)
                .withDetails(details)
                .withHelper(new HpsAnnualCensusStep8WorkplaceInspectionActionHelper())
                .withFormName(formName)
                .build();
        actionList.put(title, action);
    }

    private void evaluateStep9FoodBeverageInspection(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step9_food_beverage_inspection", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        String title = getString(R.string.hps_annual_census_food_beverage_action_title);
        BaseHpsVisitAction action = getBuilder(title)
                .withOptional(true)
                .withDetails(details)
                .withHelper(new HpsAnnualCensusStep9FoodBeverageInspectionActionHelper())
                .withFormName(formName)
                .build();
        actionList.put(title, action);
    }

    private void evaluateStep10WorkplaceHealthReports(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step10_workplace_health_reports", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        String title = getString(R.string.hps_annual_census_workplace_health_reports_action_title);
        BaseHpsVisitAction action = getBuilder(title)
                .withOptional(true)
                .withDetails(details)
                .withHelper(new HpsAnnualCensusStep10WorkplaceHealthReportsActionHelper())
                .withFormName(formName)
                .build();
        actionList.put(title, action);
    }

    private void evaluateStep11SolidWaste(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step11_solid_waste", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        String title = getString(R.string.hps_annual_census_solid_waste_action_title);
        BaseHpsVisitAction action = getBuilder(title)
                .withOptional(true)
                .withDetails(details)
                .withHelper(new HpsAnnualCensusStep11SolidWasteActionHelper())
                .withFormName(formName)
                .build();
        actionList.put(title, action);
    }

    private void evaluateStep12InsectBreedingControl(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step12_insect_breeding_control", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        String title = getString(R.string.hps_annual_census_insect_breeding_control_action_title);
        BaseHpsVisitAction action = getBuilder(title)
                .withOptional(true)
                .withDetails(details)
                .withHelper(new HpsAnnualCensusStep12InsectBreedingControlActionHelper())
                .withFormName(formName)
                .build();
        actionList.put(title, action);
    }

    @Override
    protected String submitVisit(final boolean editMode,
                                 final String memberID,
                                 final Map<String,
                                         BaseHpsVisitAction> map,
                                 String parentEventType) throws Exception {
        this.editMode = editMode;
        // create a map of the different types
        Map<String, BaseHpsVisitAction> externalVisits = new HashMap<>();
        Map<String, String> combinedJsons = new HashMap<>();
        String payloadType = null;
        String payloadDetails = null;

        // aggregate forms to be processed
        for (Map.Entry<String, BaseHpsVisitAction> entry : map.entrySet()) {
            String json = entry.getValue().getJsonPayload();
            if (StringUtils.isNotBlank(json)) {
                // do not process events that are meant to be in detached mode
                // in a similar manner to the the aggregated events
                BaseHpsVisitAction action = entry.getValue();
                BaseHpsVisitAction.ProcessingMode mode = action.getProcessingMode();

                if (mode == BaseHpsVisitAction.ProcessingMode.SEPARATE && StringUtils.isBlank(parentEventType)) {
                    externalVisits.put(entry.getKey(), entry.getValue());
                } else {
                    combinedJsons.put(entry.getKey(), json);
                }

                payloadType = action.getPayloadType().name();
                payloadDetails = action.getPayloadDetails();
            }
        }

        String type = getEncounterType();

        // persist to database
        Visit visit = saveVisit(editMode, memberID, type, combinedJsons, parentEventType);
        if (visit != null) {
            saveVisitDetails(visit, payloadType, payloadDetails);
//            processExternalVisits(visit, externalVisits, memberID);
        }

        if (visit != null && allActionsFullyFilled(map, parentEventType)) {
            List<Visit> visits = new ArrayList<>(1);
            visits.add(visit);
            VisitUtils.processVisits(visits, HpsLibrary.getInstance().visitRepository(), HpsLibrary.getInstance().visitDetailsRepository());
        }
        return visit.getJson();
    }

    protected String getEncounterType() {
        return Constants.EVENT_TYPE.HPS_ANNUAL_CENSUS;
    }

    /**
     * Determines if all actions that are part of the aggregated (non-SEPARATE) visit are fully filled.
     * <p>
     * An action is considered fully filled when either:
     * - It reports an action status of COMPLETED (via getActionStatus when available), or
     * - It has a non-blank JSON payload (fallback for older implementations).
     * <p>
     * Actions configured with ProcessingMode.SEPARATE are excluded from this check
     * when this interactor is creating the parent aggregated event (i.e., parentEventType is blank),
     * since they are submitted independently.
     */
    private boolean allActionsFullyFilled(Map<String, BaseHpsVisitAction> actions, String parentEventType) {
        if (actions == null || actions.isEmpty()) return false;
        try {
            for (Map.Entry<String, BaseHpsVisitAction> entry : actions.entrySet()) {
                BaseHpsVisitAction action = entry.getValue();

                // Skip actions configured to be processed separately when we are at the parent level
                BaseHpsVisitAction.ProcessingMode mode = action.getProcessingMode();
                if (mode == BaseHpsVisitAction.ProcessingMode.SEPARATE && StringUtils.isBlank(parentEventType)) {
                    continue;
                }

                if (!isActionFullyFilled(action)) return false;
            }
            return true;
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    /**
     * Attempts to determine if a single action is fully filled.
     * Prefers a reflective call to getActionStatus() == COMPLETED when available, otherwise
     * falls back to checking for a non-blank JSON payload.
     */
    private boolean isActionFullyFilled(BaseHpsVisitAction action) {
        if (action == null) return false;
        try {
            // Prefer using status when the API is available
            Method m = action.getClass().getMethod("getActionStatus");
            Object status = m.invoke(action);
            if (status != null && "COMPLETED".equalsIgnoreCase(String.valueOf(status))) {
                return true;
            }
        } catch (Exception ignored) {
            // Method not present or invocation failed; fall back to payload check
        }
        return false;
    }

    /**
     * Partially saves the current visit state without processing it. Intended for auto-save after
     * each action completes. This will update (edit) the ongoing visit for the encounter type.
     */
    public void autoSavePartial(final String memberID, final Map<String, BaseHpsVisitAction> map) {
        final Runnable runnable = () -> {
            try {
                if (map == null || map.isEmpty()) return;

                Map<String, BaseHpsVisitAction> externalVisits = new HashMap<>();
                Map<String, String> combinedJsons = new HashMap<>();
                String payloadType = null;
                String payloadDetails = null;

                for (Map.Entry<String, BaseHpsVisitAction> entry : map.entrySet()) {
                    BaseHpsVisitAction action = entry.getValue();
                    String json = action.getJsonPayload();
                    if (StringUtils.isNotBlank(json)) {
                        BaseHpsVisitAction.ProcessingMode mode = action.getProcessingMode();
                        if (mode == BaseHpsVisitAction.ProcessingMode.SEPARATE) {
                            externalVisits.put(entry.getKey(), action);
                        } else {
                            combinedJsons.put(entry.getKey(), json);
                        }
                        payloadType = action.getPayloadType().name();
                        payloadDetails = action.getPayloadDetails();
                    }
                }

                if (combinedJsons.isEmpty() && externalVisits.isEmpty()) return; // nothing to save

                String type = getEncounterType();
                // Use editMode=true to update an ongoing/partial visit if present
                Visit visit = saveVisit(true, memberID, type, combinedJsons, null);
                if (visit != null) {
                    saveVisitDetails(visit, payloadType, payloadDetails);
                    // Do not process visits here; this is a partial auto-save only
//                    processExternalVisits(visit, externalVisits, memberID);
                }
//                List<Visit> visits = new ArrayList<>(1);
//                visits.add(visit);
//                VisitUtils.processVisits(visits, HpsLibrary.getInstance().visitRepository(), HpsLibrary.getInstance().visitDetailsRepository());

            } catch (Exception e) {
                Timber.e(e);
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    private String getString(int resId) {
        return context.getString(resId);
    }
}
