package org.smartregister.chw.interactor;

import org.smartregister.chw.actionhelper.HpsAnnualCensusStep1PopulationActionHelper;
import org.smartregister.chw.actionhelper.HpsAnnualCensusStep2NutritionSourcesActionHelper;
import org.smartregister.chw.actionhelper.HpsSimpleFormActionHelper;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.chw.hps.contract.BaseHpsVisitContract;
import org.smartregister.chw.hps.domain.VisitDetail;
import org.smartregister.chw.hps.interactor.BaseHpsServiceVisitInteractor;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;
import org.smartregister.chw.hps.util.Constants;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Interactor that breaks HPS Annual Census into action-driven steps.
 */
public class HpsAnnualCensusVisitInteractor extends BaseHpsServiceVisitInteractor {

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
            // Recreate step 2 using the newly captured household value
            actionList.remove("Number of households with basic nutrition source");
            try {
                evaluateStep2Nutrition(details);
            } catch (BaseHpsVisitAction.ValidationException e) {
                throw new RuntimeException(e);
            }
            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        });

        String formName = Utils.getLocalForm("hps_annual_census_step1_population", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        BaseHpsVisitAction action = getBuilder("Population")
                .withOptional(false)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(formName)
                .build();

        actionList.put("Population", action);
    }

    private void evaluateStep2Nutrition(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        if (householdCountValue == null || householdCountValue.trim().isEmpty()) {
            // Defer adding step 2 until household count is known (after step 1)
            return;
        }
        HpsAnnualCensusStep2NutritionSourcesActionHelper actionHelper = new HpsAnnualCensusStep2NutritionSourcesActionHelper(householdCountValue);

        String formName = Utils.getLocalForm("hps_annual_census_step2_nutrition_sources", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);

        BaseHpsVisitAction action = getBuilder("Number of households with basic nutrition source")
                .withOptional(false)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(formName)
                .build();

        actionList.put("Number of households with basic nutrition source", action);
    }

    private void evaluateStep3Centers(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step3_centers", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        BaseHpsVisitAction action = getBuilder("Healthcare services, education, child and elder care centers")
                .withOptional(false)
                .withDetails(details)
                .withHelper(new HpsSimpleFormActionHelper())
                .withFormName(formName)
                .build();
        actionList.put("Healthcare services, education, child and elder care centers", action);
    }

    private void evaluateStep4SocialEconomic(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step4_social_economic", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        BaseHpsVisitAction action = getBuilder("Social services and economic activities")
                .withOptional(false)
                .withDetails(details)
                .withHelper(new HpsSimpleFormActionHelper())
                .withFormName(formName)
                .build();
        actionList.put("Social services and economic activities", action);
    }

    private void evaluateStep5CommitteesTraditionalMedicine(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step5_committees_traditional_medicine", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        BaseHpsVisitAction action = getBuilder("Committee meetings & Traditional medicine")
                .withOptional(false)
                .withDetails(details)
                .withHelper(new HpsSimpleFormActionHelper())
                .withFormName(formName)
                .build();
        actionList.put("Committee meetings & Traditional medicine", action);
    }

    private void evaluateStep6EnvironmentSanitation(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step6_environment_sanitation", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        BaseHpsVisitAction action = getBuilder("Environmental and sanitation Inspection report")
                .withOptional(false)
                .withDetails(details)
                .withHelper(new HpsSimpleFormActionHelper())
                .withFormName(formName)
                .build();
        actionList.put("Environmental and sanitation Inspection report", action);
    }

    private void evaluateStep7BuildingInspection(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step7_building_inspection", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        BaseHpsVisitAction action = getBuilder("Building Inspection Report")
                .withOptional(false)
                .withDetails(details)
                .withHelper(new HpsSimpleFormActionHelper())
                .withFormName(formName)
                .build();
        actionList.put("Building Inspection Report", action);
    }

    private void evaluateStep8WorkplaceInspection(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step8_workplace_inspection", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        BaseHpsVisitAction action = getBuilder("Workplace Inspection Report")
                .withOptional(false)
                .withDetails(details)
                .withHelper(new HpsSimpleFormActionHelper())
                .withFormName(formName)
                .build();
        actionList.put("Workplace Inspection Report", action);
    }

    private void evaluateStep9FoodBeverageInspection(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step9_food_beverage_inspection", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        BaseHpsVisitAction action = getBuilder("Food and Beverage Inspection Report")
                .withOptional(false)
                .withDetails(details)
                .withHelper(new HpsSimpleFormActionHelper())
                .withFormName(formName)
                .build();
        actionList.put("Food and Beverage Inspection Report", action);
    }

    private void evaluateStep10WorkplaceHealthReports(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step10_workplace_health_reports", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        BaseHpsVisitAction action = getBuilder("Health Reports Affecting People in Workplaces")
                .withOptional(false)
                .withDetails(details)
                .withHelper(new HpsSimpleFormActionHelper())
                .withFormName(formName)
                .build();
        actionList.put("Health Reports Affecting People in Workplaces", action);
    }

    private void evaluateStep11SolidWaste(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step11_solid_waste", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        BaseHpsVisitAction action = getBuilder("Solid waste & waste collection equipments")
                .withOptional(false)
                .withDetails(details)
                .withHelper(new HpsSimpleFormActionHelper())
                .withFormName(formName)
                .build();
        actionList.put("Solid waste & waste collection equipments", action);
    }

    private void evaluateStep12InsectBreedingControl(Map<String, List<VisitDetail>> details) throws BaseHpsVisitAction.ValidationException {
        String formName = Utils.getLocalForm("hps_annual_census_step12_insect_breeding_control", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager);
        BaseHpsVisitAction action = getBuilder("Identification and Control of Insect Breeding Sites")
                .withOptional(false)
                .withDetails(details)
                .withHelper(new HpsSimpleFormActionHelper())
                .withFormName(formName)
                .build();
        actionList.put("Identification and Control of Insect Breeding Sites", action);
    }
}
