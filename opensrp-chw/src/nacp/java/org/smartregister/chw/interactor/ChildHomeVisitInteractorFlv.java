package org.smartregister.chw.interactor;

import static org.smartregister.chw.anc.model.BaseAncHomeVisitAction.Status.COMPLETED;
import static org.smartregister.chw.core.utils.CoreConstants.TASKS_FOCUS.SICK_CHILD;
import static org.smartregister.chw.util.JsonFormUtils.getCheckBoxValue;

import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.actionhelper.ChildHVProblemSolvingHelper;
import org.smartregister.chw.actionhelper.ChildMinorAilmentsActionHelper;
import org.smartregister.chw.actionhelper.ExclusiveBreastFeedingAction;
import org.smartregister.chw.actionhelper.MalnutritionScreeningActionHelper;
import org.smartregister.chw.actionhelper.ToddlerDangerSignsBabyHelper;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.contract.BaseAncHomeVisitContract;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.util.AppExecutors;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.chw.referral.util.LocationUtils;
import org.smartregister.chw.util.ChwAncJsonFormUtils;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.chw.util.JsonFormUtilsFlv;
import org.smartregister.chw.util.JsonQ;
import org.smartregister.domain.Alert;
import org.smartregister.immunization.domain.ServiceWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class ChildHomeVisitInteractorFlv extends DefaultChildHomeVisitInteractorFlv {

    private  static final String NONE="(?i)hakuna|none|chk_none";
    private Map<String, ServiceWrapper> serviceWrapperMap;
    private BaseAncHomeVisitContract.InteractorCallBack callBack;

    @Override
    protected void bindEvents(Map<String, ServiceWrapper> serviceWrapperMap) throws BaseAncHomeVisitAction.ValidationException {
        try {
            this.serviceWrapperMap = serviceWrapperMap;
            //isToddler function needs needs to be confirmed
            if( isToddler() )  evaluateToddlerDanger();
            else evaluateChildDangerSigns();
        }
        catch (BaseAncHomeVisitAction.ValidationException e) {throw (e);}
        catch (Exception e) {Timber.e(e);}
    }

    private void evaluateChildDangerSigns() throws BaseAncHomeVisitAction.ValidationException {

        String title = context.getString(R.string.child_danger_signs_baby);


        ToddlerDangerSignsBabyHelper helper = new ToddlerDangerSignsBabyHelper(context, null);
        helper.setDangerSignsResultsListener(this::onDangerSignFormResults);

        Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.CHILD_HOME_VISIT);

        JSONObject dangerSignsForm = FormUtils.getFormUtils().getFormJson(org.smartregister.chw.util.Constants.JsonForm.getChildHomeVisitDangerSignForm());
        JsonFormUtilsFlv.overwriteQuestionOptions("referral_facility", LocationUtils.INSTANCE.getFacilitiesKeyAndName(), dangerSignsForm);

        BaseAncHomeVisitAction action = new BaseAncHomeVisitAction.Builder(context,title)
                .withHelper(helper)
                .withDetails(details)
                .withOptional(false)
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withJsonPayload(dangerSignsForm.toString())
                .withFormName(org.smartregister.chw.util.Constants.JsonForm.getChildHomeVisitDangerSignForm())
                .build();
        actionList.put(context.getString(R.string.child_danger_signs_baby), action);
    }

    private int cleanInt(String input){
        return Integer.parseInt(input.replaceAll("\\D+",""));
    }
     private boolean isToddler(){
         int fiveYears = 5 * 12;
         int ageInMonths = Months.monthsBetween(new DateTime(memberObject.getDob()), DateTime.now()).getMonths();

         try( InputStream input = context.getAssets().open("recurring_service_types.json")){
             JsonQ services = JsonQ.fromIO(input).get("[(@.type~'(?i).*toddler.*danger.*sign.*')].services[0,-1]");

             String firstOffset = services.str("[0].schedule.due.offset");
             String lastExpiry = services.str("[-1].schedule.expiry.offset");

             int start = cleanInt(firstOffset);
             int end = cleanInt(lastExpiry);

             return start <= ageInMonths && ageInMonths <= end
                     && ageInMonths < fiveYears;
         }
         catch (IOException e){Timber.e(e);}
         return false;
     }

    @Override
    public LinkedHashMap<String, BaseAncHomeVisitAction> calculateActions(BaseAncHomeVisitContract.View view, MemberObject memberObject, BaseAncHomeVisitContract.InteractorCallBack callBack) throws BaseAncHomeVisitAction.ValidationException {
       this.callBack=callBack;
       return super.calculateActions(view,memberObject,callBack);
    }

    protected void evaluateImmunization () throws Exception {
        setVaccinesDefaultChecked(false);
        super.evaluateImmunization();
    }

    private void evaluateMalariaPrevention() throws Exception {
        HomeVisitActionHelper malariaPreventionHelper = new HomeVisitActionHelper() {
            private String famllin1m5yr;
            private String llin2days1m5yr;
            private String llinCondition1m5yr;

            @Override
            public void onPayloadReceived(String jsonPayload) {
                try {
                    JSONObject jsonObject = new JSONObject(jsonPayload);
                    famllin1m5yr = JsonFormUtils.getValue(jsonObject, "fam_llin_1m5yr");
                    llin2days1m5yr = JsonFormUtils.getValue(jsonObject, "llin_2days_1m5yr");
                    llinCondition1m5yr = JsonFormUtils.getValue(jsonObject, "llin_condition_1m5yr");
                } catch (JSONException e) {
                    Timber.e(e);
                }
            }

            @Override
            public String evaluateSubTitle() {

                // Handle translation of drop down values
                if (!TextUtils.isEmpty(famllin1m5yr) && !TextUtils.isEmpty(llin2days1m5yr)) {
                    famllin1m5yr = getYesNoTranslation(famllin1m5yr);
                    llin2days1m5yr = getYesNoTranslation(llin2days1m5yr);
                }

                if (!TextUtils.isEmpty(llinCondition1m5yr)) {
                    if ("Okay".equals(llinCondition1m5yr)) {
                        llinCondition1m5yr = context.getString(R.string.okay);
                    } else if ("Bad".equals(llinCondition1m5yr)) {
                        llinCondition1m5yr = context.getString(R.string.bad);
                    }
                }

                StringBuilder stringBuilder = new StringBuilder();
                if (famllin1m5yr.equalsIgnoreCase(context.getString(R.string.no))) {
                    stringBuilder.append(MessageFormat.format("{0}: {1}\n", context.getString(R.string.uses_net), StringUtils.capitalize(famllin1m5yr.trim().toLowerCase())));
                } else {
                    stringBuilder.append(MessageFormat.format("{0}: {1} · ", context.getString(R.string.uses_net), StringUtils.capitalize(famllin1m5yr.trim().toLowerCase())));
                    stringBuilder.append(MessageFormat.format("{0}: {1} · ", context.getString(R.string.slept_under_net), StringUtils.capitalize(llin2days1m5yr.trim().toLowerCase())));
                    stringBuilder.append(MessageFormat.format("{0}: {1}", context.getString(R.string.net_condition), StringUtils.capitalize(llinCondition1m5yr.trim().toLowerCase())));
                }
                return stringBuilder.toString();
            }

            public String getYesNoTranslation(String subtitleText) {
                if ("Yes".equals(subtitleText)) {
                    return context.getString(R.string.yes);
                } else if ("No".equals(subtitleText)) {
                    return context.getString(R.string.no);
                } else {
                    return subtitleText;
                }
            }

            @Override
            public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
                if (StringUtils.isBlank(famllin1m5yr)) {
                    return BaseAncHomeVisitAction.Status.PENDING;
                }

                if (famllin1m5yr.equalsIgnoreCase(context.getString(R.string.yes)) && llin2days1m5yr.equalsIgnoreCase(context.getString(R.string.yes)) && llinCondition1m5yr.equalsIgnoreCase(context.getString(R.string.okay))) {
                    return COMPLETED;
                } else {
                    return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
                }
            }
        };

        BaseAncHomeVisitAction action = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.pnc_malaria_prevention))
                .withOptional(false)
                .withDetails(details)
                .withFormName(Constants.JSON_FORM.CHILD_HOME_VISIT.getMalariaPrevention())
                .withHelper(malariaPreventionHelper)
                .build();
        actionList.put(context.getString(R.string.pnc_malaria_prevention), action);
    }

    private void evaluateCounselling() throws Exception {
        HomeVisitActionHelper counsellingHelper = new HomeVisitActionHelper() {
            private String couselling_child;

            @Override
            public void onPayloadReceived(String jsonPayload) {
                try {
                    JSONObject jsonObject = new JSONObject(jsonPayload);
                    couselling_child = getCheckBoxValue(jsonObject, "pnc_counselling");
                } catch (JSONException e) {
                    Timber.e(e);
                }
            }

            @Override
            public String evaluateSubTitle() {
                return MessageFormat.format("{0}: {1}", "Counselling", couselling_child);
            }

            @Override
            public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
                if (StringUtils.isNotBlank(couselling_child)) {
                    return COMPLETED;
                } else {
                    return BaseAncHomeVisitAction.Status.PENDING;
                }
            }
        };

        BaseAncHomeVisitAction action = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.pnc_counselling))
                .withOptional(false)
                .withDetails(details)
                .withFormName(Constants.JSON_FORM.PNC_HOME_VISIT.getCOUNSELLING())
                .withHelper(counsellingHelper)
                .build();
        actionList.put(context.getString(R.string.pnc_counselling), action);
    }

    @Override
    protected int immunizationCeiling() {
        return 60;
    }

    private void evaluateNutritionStatus() throws BaseAncHomeVisitAction.ValidationException {
        HomeVisitActionHelper nutritionStatusHelper = new HomeVisitActionHelper() {
            private String nutritionStatus;

            @Override
            public void onPayloadReceived(String jsonPayload) {
                try {
                    JSONObject jsonObject = new JSONObject(jsonPayload);
                    nutritionStatus = JsonFormUtils.getValue(jsonObject, "nutrition_status_1m5yr").toLowerCase();
                } catch (JSONException e) {
                    Timber.e(e);
                }
            }

            @Override
            public String evaluateSubTitle() {
                if (!TextUtils.isEmpty(nutritionStatus)) {
                    switch (nutritionStatus) {
                        case "normal":
                            nutritionStatus = context.getString(R.string.normal);
                            break;
                        case "moderate":
                            nutritionStatus = context.getString(R.string.moderate);
                            break;
                        case "severe":
                            nutritionStatus = context.getString(R.string.severe);
                            break;
                        default:
                            return nutritionStatus;
                    }
                }
                return MessageFormat.format("{0}: {1}", context.getString(R.string.nutrition_status), nutritionStatus);
            }

            @Override
            public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
                if (StringUtils.isBlank(nutritionStatus))
                    return BaseAncHomeVisitAction.Status.PENDING;

                return COMPLETED;
            }
        };

        BaseAncHomeVisitAction observation = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_nutrition_status))
                .withOptional(false)
                .withDetails(details)
                .withFormName(Constants.JSON_FORM.CHILD_HOME_VISIT.getNutritionStatus())
                .withHelper(nutritionStatusHelper)
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_nutrition_status), observation);
    }

    @Override
    protected void evaluateObsAndIllness() throws BaseAncHomeVisitAction.ValidationException {
        class ObsIllnessBabyHelper extends HomeVisitActionHelper {
            private String date_of_illness;
            private String illness_description;
            private String action_taken;
            private LocalDate illnessDate;

            @Override
            public void onPayloadReceived(String jsonPayload) {
                try {
                    JSONObject jsonObject = new JSONObject(jsonPayload);
                    date_of_illness = JsonFormUtils.getValue(jsonObject, "date_of_illness");
                    illness_description = JsonFormUtils.getValue(jsonObject, "illness_description");
                    action_taken = JsonFormUtils.getValue(jsonObject, "action_taken_1m5yr");
                    illnessDate = DateTimeFormat.forPattern("dd-MM-yyyy").parseLocalDate(date_of_illness);
                } catch (Exception e) {
                    Timber.e(e);
                }
            }

            @Override
            public String evaluateSubTitle() {
                if (illnessDate == null)
                    return "";

                return MessageFormat.format("{0}: {1}\n {2}: {3}",
                        DateTimeFormat.forPattern("dd MMM yyyy").print(illnessDate),
                        illness_description, context.getString(R.string.action_taken), action_taken
                );
            }

            @Override
            public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
                if (StringUtils.isBlank(date_of_illness))
                    return BaseAncHomeVisitAction.Status.PENDING;

                return COMPLETED;
            }
        }

        BaseAncHomeVisitAction observation = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_observations_n_illnes))
                .withOptional(true)
                .withDetails(details)
                .withFormName(Constants.JSON_FORM.getObsIllness())
                .withHelper(new ObsIllnessBabyHelper())
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_observations_n_illnes), observation);
    }

    private void evaluateMalnutritionScreening(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {
        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Malnutrition Screening");
        if (serviceWrapper == null) return;
        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        MalnutritionScreeningActionHelper malnutritionScreeningActionHelper = new MalnutritionScreeningActionHelper(serviceWrapper);
        Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.CHILD_HOME_VISIT);

        String title = context.getString(R.string.malnutrition_screening);

        BaseAncHomeVisitAction malnutritionScreeningAction = new BaseAncHomeVisitAction.Builder(context, title)
                .withOptional(false)
                .withDetails(details)
                .withFormName(Constants.JsonForm.getChildHvMalnutritionScreening())
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withSubtitle(MessageFormat.format("{0}{1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .withHelper(malnutritionScreeningActionHelper)
                .build();
        actionList.put(title, malnutritionScreeningAction);
    }

    private void evaluateProblemSolving() throws Exception {
        BaseAncHomeVisitAction action = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.child_problem_solving))
                .withOptional(false)
                .withDetails(details)
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.SEPARATE)
                .withFormName(Constants.JsonForm.getChildHvProblemSolvingForm())
                .withHelper(new ChildHVProblemSolvingHelper())
                .build();
        actionList.put(context.getString(R.string.child_problem_solving), action);
    }

    private void evaluateMinorAilments(MemberObject memberObject) throws BaseAncHomeVisitAction.ValidationException{
        ChildMinorAilmentsActionHelper minorAilmentHelper = new ChildMinorAilmentsActionHelper(context, memberObject);
        String title = MessageFormat.format(context.getString(R.string.child_minor_ailments), memberObject.getFullName());
        BaseAncHomeVisitAction childMinorAilmentAction = new BaseAncHomeVisitAction.Builder(context, title)
                .withOptional(false)
                .withDetails(details)
                .withFormName(Utils.getLocalForm("linkages/native/child_linkage_form", CoreConstants.JSON_FORM.locale, CoreConstants.JSON_FORM.assetManager))
                .withHelper(minorAilmentHelper)
                .build();

        actionList.put(title, childMinorAilmentAction);
    }

    private  synchronized  void clearActions(){
        //using iterator to avoid concurrent modification exception
        Iterator<String> keys=actionList.keySet().iterator();
        while(keys.hasNext()){
            String key=keys.next();
            String dangerSign=context.getString(R.string.child_danger_signs_baby);
            if(!key.equals(dangerSign)){
                keys.remove();
            }
        }
    }

    private void evaluateActions() throws Exception{
        evaluateMinorAilments(memberObject);
        evaluateImmunization();
        evaluateExclusiveBreastFeeding(serviceWrapperMap);
        evaluateVitaminA(serviceWrapperMap);
        evaluateDeworming(serviceWrapperMap);
        evaluateMalariaPrevention();
        evaluateCounselling();
        evaluateNutritionStatus();
        evaluateObsAndIllness();
        evaluateMalnutritionScreening(serviceWrapperMap);
        evaluateProblemSolving();
    }

    private void onDangerSignFormResults(JSONObject dangerSignForm, String dangerSigns, boolean goFacility){
        try{
            clearActions();
            if( goFacility ) {
                evaluateFacilityReferral(dangerSignForm);
                evaluateMalariaPrevention();
            }
            else if(dangerSigns.matches(NONE)) evaluateActions();
            new AppExecutors().mainThread().execute(() -> callBack.preloadActions(actionList));
        }
        catch (Exception e){Timber.e(e);}
    }

    private void evaluateToddlerDanger() throws Exception {
        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Toddler danger sign");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        String title = context.getString(R.string.child_danger_signs_baby);

        // alert if overdue after 14 days
        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        ToddlerDangerSignsBabyHelper helper = new ToddlerDangerSignsBabyHelper(context, alert);
        helper.setDangerSignsResultsListener(this::onDangerSignFormResults);

        Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.CHILD_HOME_VISIT);

        JSONObject dangerSignsForm = FormUtils.getFormUtils().getFormJson(org.smartregister.chw.util.Constants.JsonForm.getChildHomeVisitDangerSignForm());
        JsonFormUtilsFlv.overwriteQuestionOptions("referral_facility", LocationUtils.INSTANCE.getFacilitiesKeyAndName(), dangerSignsForm);

        BaseAncHomeVisitAction action = new BaseAncHomeVisitAction.Builder(context,title)
                .withHelper(helper)
                .withDetails(details)
                .withOptional(false)
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withJsonPayload(dangerSignsForm.toString())
                .withFormName(org.smartregister.chw.util.Constants.JsonForm.getChildHomeVisitDangerSignForm())
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .build();
        actionList.put(context.getString(R.string.child_danger_signs_baby), action);
    }

    private void evaluateFacilityReferral(JSONObject referralPayload) throws BaseAncHomeVisitAction.ValidationException {
        String formName="referral_facility_selection";
        JSONObject jsonForm = FormUtils.getFormUtils().getFormJson(formName);

        if (details != null) ChwAncJsonFormUtils.populateForm(jsonForm, details);

        FacilitySelectionActionHelper helper=new FacilitySelectionActionHelper(
                FacilitySelectionActionHelper.copyReferralProblem(referralPayload,"toddler_danger_signs_present"),
                SICK_CHILD,
                memberObject.getBaseEntityId());

        BaseAncHomeVisitAction action = new BaseAncHomeVisitAction.Builder(context,context.getString(R.string.home_visit_facility_referral) )
                .withOptional(false)
                .withDetails(details)
                .withFormName(formName)
                .withJsonPayload(jsonForm.toString())
                .withHelper(helper)
                .build();
        actionList.put(context.getString(R.string.home_visit_facility_referral), action);
    }

    @Override
    protected void evaluateExclusiveBreastFeeding(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {
        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Exclusive breastfeeding");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceIteration = serviceWrapper.getName().substring(serviceWrapper.getName().length() - 1);

        String title = context.getString(R.string.exclusive_breastfeeding_months, serviceIteration);

        // alert if overdue after 14 days
        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        ExclusiveBreastFeedingAction helper = new ExclusiveBreastFeedingAction(context, alert, serviceIteration);
        JSONObject jsonObject = getFormJson(org.smartregister.chw.util.Constants.JsonForm.getChildHvBreastfeedingForm(), memberObject.getBaseEntityId());

        Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.CHILD_HOME_VISIT);

        if (details != null && !details.isEmpty()) {
            org.smartregister.chw.anc.util.JsonFormUtils.populateForm(jsonObject, details);
        }

        BaseAncHomeVisitAction action = getBuilder(title)
                .withHelper(helper)
                .withDetails(details)
                .withOptional(false)
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withFormName(org.smartregister.chw.util.Constants.JsonForm.getChildHvBreastfeedingForm())
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withSubtitle(MessageFormat.format("{0}{1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .build();
        actionList.put(title, action);
    }
}
