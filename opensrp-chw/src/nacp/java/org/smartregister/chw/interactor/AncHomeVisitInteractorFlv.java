package org.smartregister.chw.interactor;

import android.content.Context;

import com.sun.xml.bind.v2.TODO;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.actionhelper.HealthFacilityVisitAction;
import org.smartregister.chw.actionhelper.PNCVisitLocationActionHelper;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.contract.BaseAncHomeVisitContract;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.util.AppExecutors;
import org.smartregister.chw.anc.util.VisitUtils;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.util.ChwAncJsonFormUtils;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.ContactUtil;
import org.smartregister.chw.util.JsonFormUtils;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class AncHomeVisitInteractorFlv implements AncHomeVisitInteractor.Flavor {
    private final LinkedHashMap<String, BaseAncHomeVisitAction> actionList = new LinkedHashMap<>();
    private Map<String, List<VisitDetail>> details = null;
    private MemberObject memberObject;
    private Map<Integer, LocalDate> dateMap = new LinkedHashMap<>();
    private BaseAncHomeVisitContract.InteractorCallBack callBack;
    private String visit_title;
    protected Context context;

    @Override
    public LinkedHashMap<String, BaseAncHomeVisitAction> calculateActions(BaseAncHomeVisitContract.View view, MemberObject memberObject, BaseAncHomeVisitContract.InteractorCallBack callBack) throws BaseAncHomeVisitAction.ValidationException {
        context = view.getContext();
        this.memberObject = memberObject;
        this.callBack = callBack;
        // get the preloaded data
        if (view.getEditMode()) {
            Visit lastVisit = AncLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EventType.ANC_HOME_VISIT);
            if (lastVisit != null) {
                details = VisitUtils.getVisitGroups(AncLibrary.getInstance().visitDetailsRepository().getVisits(lastVisit.getVisitId()));
            }
        }

        // get contact
        LocalDate lastContact = new DateTime(memberObject.getDateCreated()).toLocalDate();
        boolean isFirst = (StringUtils.isBlank(memberObject.getLastContactVisit()));
        LocalDate lastMenstrualPeriod = new LocalDate();
        try {
            lastMenstrualPeriod = DateTimeFormat.forPattern("dd-MM-yyyy").parseLocalDate(memberObject.getLastMenstrualPeriod());
        } catch (Exception e) {
            Timber.e(e);
        }


        if (StringUtils.isNotBlank(memberObject.getLastContactVisit())) {
            lastContact = DateTimeFormat.forPattern("dd-MM-yyyy").parseLocalDate(memberObject.getLastContactVisit());
        }


        // today is the due date for the very first visit
        if (isFirst) {
            dateMap.put(0, LocalDate.now());
        }

        dateMap.putAll(ContactUtil.getContactWeeks(isFirst, lastContact, lastMenstrualPeriod));

        evaluateVisitLocation();
        evaluateDangerSigns(details, context);


        return actionList;
    }

    private void evaluateDangerSigns(Map<String, List<VisitDetail>> details,
                                     final Context context) throws BaseAncHomeVisitAction.ValidationException {
        JSONObject dangerSignsForm = FormUtils.getFormUtils().getFormJson(Constants.JSON_FORM.ANC_HOME_VISIT.getDangerSigns());
        if (details != null) {
            ChwAncJsonFormUtils.populateForm(dangerSignsForm, details);
        }
        BaseAncHomeVisitAction danger_signs = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_danger_signs))
                .withOptional(false)
                .withDetails(details)
                .withFormName(Constants.JSON_FORM.ANC_HOME_VISIT.getDangerSigns())
                .withJsonPayload(dangerSignsForm.toString())
                .withHelper(new DangerSignsAction())
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_danger_signs), danger_signs);
    }

    private void evaluateHealthFacilityVisit(Map<String, List<VisitDetail>> details,
                                             final MemberObject memberObject,
                                             Map<Integer, LocalDate> dateMap,
                                             final Context context) throws BaseAncHomeVisitAction.ValidationException {
        visit_title = MessageFormat.format(context.getString(R.string.anc_home_visit_facility_visit), memberObject.getConfirmedContacts() + 1);
        JSONObject healthFacilityVisitForm = FormUtils.getFormUtils().getFormJson(Constants.JSON_FORM.ANC_HOME_VISIT.getHealthFacilityVisit());
        if (details != null) {
            ChwAncJsonFormUtils.populateForm(healthFacilityVisitForm, details);
        }
        BaseAncHomeVisitAction facility_visit = new BaseAncHomeVisitAction.Builder(context, visit_title)
                .withOptional(false)
                .withDetails(details)
                .withHelper(new HealthFacilityAction(memberObject, dateMap))
                .withJsonPayload(healthFacilityVisitForm.toString())
                .withFormName(Constants.JSON_FORM.ANC_HOME_VISIT.getHealthFacilityVisit())
                .build();

        actionList.put(visit_title, facility_visit);
    }

    private void evaluateFamilyPlanning(Map<String, List<VisitDetail>> details,
                                        final Context context) throws BaseAncHomeVisitAction.ValidationException {
        JSONObject familyPlanningForm = FormUtils.getFormUtils().getFormJson(Constants.JSON_FORM.ANC_HOME_VISIT.getFamilyPlanning());
        if (details != null) {
            ChwAncJsonFormUtils.populateForm(familyPlanningForm, details);
        }
        BaseAncHomeVisitAction family_planning_ba = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_family_planning))
                .withOptional(false)
                .withDetails(details)
                .withFormName(Constants.JSON_FORM.ANC_HOME_VISIT.getFamilyPlanning())
                .withJsonPayload(familyPlanningForm.toString())
                .withHelper(new FamilyPlanningAction())
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_family_planning), family_planning_ba);
    }

//    private void evaluateNutritionStatus(Map<String, List<VisitDetail>> details,
//                                         final Context context) throws BaseAncHomeVisitAction.ValidationException {
//        BaseAncHomeVisitAction nutrition_ba = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_nutrition_status))
//                .withOptional(true)
//                .withDetails(details)
//                .withFormName(Constants.JSON_FORM.ANC_HOME_VISIT.getNutritionStatus())
//                .withHelper(new NutritionAction())
//                .build();
//        actionList.put(context.getString(R.string.anc_home_visit_nutrition_status), nutrition_ba);
//    }

    private void evaluateCounsellingStatus(Map<String, List<VisitDetail>> details,
                                           final Context context) throws BaseAncHomeVisitAction.ValidationException {
        JSONObject counsellingForm = FormUtils.getFormUtils().getFormJson(Constants.JSON_FORM.ANC_HOME_VISIT.getCOUNSELLING());
        if (details != null) {
            ChwAncJsonFormUtils.populateForm(counsellingForm, details);
        }
        BaseAncHomeVisitAction counselling_ba = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_counselling_task))
                .withOptional(false)
                .withDetails(details)
                .withJsonPayload(counsellingForm.toString())
                .withFormName(Constants.JSON_FORM.ANC_HOME_VISIT.getCOUNSELLING())
                .withHelper(new CounsellingStatusAction())
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_counselling_task), counselling_ba);
    }

    private void evaluateMalaria(Map<String, List<VisitDetail>> details,
                                 final Context context) throws BaseAncHomeVisitAction.ValidationException {
        JSONObject malariaForm = FormUtils.getFormUtils().getFormJson(Constants.JSON_FORM.ANC_HOME_VISIT.getMALARIA());
        if (details != null) {
            ChwAncJsonFormUtils.populateForm(malariaForm, details);
        }
        BaseAncHomeVisitAction malaria_ba = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_malaria_prevention))
                .withOptional(false)
                .withDetails(details)
                .withJsonPayload(malariaForm.toString())
                .withFormName(Constants.JSON_FORM.ANC_HOME_VISIT.getMALARIA())
                .withHelper(new MalariaAction())
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_malaria_prevention), malaria_ba);
    }

    private void evaluateObservation(Map<String, List<VisitDetail>> details,
                                     final Context context) throws BaseAncHomeVisitAction.ValidationException {
        BaseAncHomeVisitAction remark_ba = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_observations_n_illnes))
                .withOptional(true)
                .withDetails(details)
                .withFormName(Constants.JSON_FORM.ANC_HOME_VISIT.getObservationAndIllness())
                .withHelper(new ObservationAction())
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_observations_n_illnes), remark_ba);
    }

    private void evaluateRemarks(Map<String, List<VisitDetail>> details,
                                 final Context context) throws BaseAncHomeVisitAction.ValidationException {
        JSONObject remarkForm = FormUtils.getFormUtils().getFormJson(Constants.JSON_FORM.ANC_HOME_VISIT.getRemarksAndComments());
        if (details != null) {
            ChwAncJsonFormUtils.populateForm(remarkForm, details);
        }
        BaseAncHomeVisitAction remark_ba = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_remarks_and_comments))
                .withOptional(true)
                .withDetails(details)
                .withFormName(Constants.JSON_FORM.ANC_HOME_VISIT.getRemarksAndComments())
                .withJsonPayload(remarkForm.toString())
                .withHelper(new RemarksAction())
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_remarks_and_comments), remark_ba);
    }

    private void evaluatePostpartumCareForMother() throws BaseAncHomeVisitAction.ValidationException {
        BaseAncHomeVisitAction earlyStimulation = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_postpartum_care_for_mother))
                .withOptional(false)
                .withDetails(details)
                .withFormName("anc_hv_postpartum_care_for_mother")
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_postpartum_care_for_mother), earlyStimulation);
    }

    private void evaluateEarlyStimulation() throws BaseAncHomeVisitAction.ValidationException {
        BaseAncHomeVisitAction earlyStimulation = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_early_stimulation))
                .withOptional(false)
                .withDetails(details)
                .withFormName("anc_hv_early_stimulation")
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_early_stimulation), earlyStimulation);
    }

    private void evaluatePostpartumDangerSigns() throws BaseAncHomeVisitAction.ValidationException {
        BaseAncHomeVisitAction earlyStimulation = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_postpartum_danger_signs))
                .withOptional(false)
                .withDetails(details)
                .withFormName("anc_hv_postpartum_danger_signs")
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_postpartum_danger_signs), earlyStimulation);
    }
    private void evaluateCommunityHealthWorkerObservation(Map<String, List<VisitDetail>> details, final Context context) throws BaseAncHomeVisitAction.ValidationException {
        BaseAncHomeVisitAction chw_observations = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_community_health_worker_observations))
                .withOptional(false)
                .withDetails(details)
                .withFormName("anc_hv_community_health_worker_observations")
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withHelper(new CommunityHealthWorkerObservationsAction())
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_community_health_worker_observations), chw_observations);
    }
    private void evaluateImmediateNewBornCare() throws BaseAncHomeVisitAction.ValidationException {
        BaseAncHomeVisitAction earlyStimulation = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_immediate_newborn_care))
                .withOptional(false)
                .withDetails(details)
                .withFormName("anc_hv_immediate_newborn_care")
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_immediate_newborn_care), earlyStimulation);
    }

    private void evaluateAncClinicAttendance() throws BaseAncHomeVisitAction.ValidationException {

        //Check if first and second visit had already been conducted
//        if (org.smartregister.chw.util.VisitUtils.isThirdVisit(memberObject))
//            return;



//        String visit_title = MessageFormat.format(context.getString(R.string.anc_hv_clinic_attendance), allVisits.size() + 1);
        String visit_title = context.getString(R.string.anc_hv_clinic_attendance);
        BaseAncHomeVisitAction anc_clinic_attendance = new BaseAncHomeVisitAction.Builder(context, visit_title)
                .withOptional(false)
                .withDetails(details)
                .withHelper(new ClinicAttendanceAction())
                .withFormName("anc_hv_clinic_attendance")
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .build();

        actionList.put(visit_title, anc_clinic_attendance);
    }

    private void evaluateBreastFeeding(Map<String, List<VisitDetail>> details, final MemberObject memberObject,
                                       final Context context) throws BaseAncHomeVisitAction.ValidationException {
//        if (org.smartregister.chw.util.VisitUtils.isSecondVisit(memberObject) || org.smartregister.chw.util.VisitUtils.isThirdVisit(memberObject)) {

        BaseAncHomeVisitAction bread_feeding_action = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_breast_feeding))
                .withOptional(false)
                .withDetails(details)
                .withHelper(new BreastFeedingActionHelper())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withFormName("anc_hv_breastfeeding")
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_breast_feeding), bread_feeding_action);
//        }

    }
    private void evaluateNewBornDangerSign() throws BaseAncHomeVisitAction.ValidationException {
        BaseAncHomeVisitAction earlyStimulation = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_new_born_danger_signs))
                .withOptional(false)
                .withDetails(details)
                .withFormName("anc_hv_new_born_danger_signs")
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_new_born_danger_signs), earlyStimulation);
    }

    private void evaluateHIVAIDSGeneralInformation() throws BaseAncHomeVisitAction.ValidationException {
        BaseAncHomeVisitAction earlyStimulation = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_hiv_aids_general_information))
                .withOptional(false)
                .withDetails(details)
                .withFormName("anc_hv_hiv_aids_general_information")
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_hiv_aids_general_information), earlyStimulation);
    }

    private void evaluateBirthPreparedness(Map<String, List<VisitDetail>> details, final MemberObject memberObject) throws BaseAncHomeVisitAction.ValidationException {
        String visit_title = MessageFormat.format(context.getString(R.string.anc_home_visit_birth_preparedness), memberObject.getConfirmedContacts() + 1);
        BaseAncHomeVisitAction birth_preparedness = new BaseAncHomeVisitAction.Builder(context, visit_title)
                .withOptional(false)
                .withDetails(details)
                .withHelper(new BirthPreparednessAction())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withFormName("anc_hv_birth_preparedness")
                .build();

        actionList.put(visit_title, birth_preparedness);
    }

    private void evaluateVisitLocation() throws BaseAncHomeVisitAction.ValidationException {
        BaseAncHomeVisitAction action = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.pnc_hv_location))
                .withOptional(false)
                .withDetails(details)
                .withFormName(Constants.JsonForm.getPncHvLocation())
                .withHelper(new PNCVisitLocationActionHelper())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .build();
        actionList.put(context.getString(R.string.pnc_hv_location), action);
    }

    // TODO: 7/16/25 Below Action Helepers Needs to be Moved to Individual files as other Action Helper
    private void evaluatePartnerEngagement(Map<String, List<VisitDetail>> details, final Context context) throws BaseAncHomeVisitAction.ValidationException {
        JSONObject partnerEngagementForm = FormUtils.getFormUtils().getFormJson(Constants.JsonForm.getAncHvPartnerEngagement());
        if (details != null) {
            ChwAncJsonFormUtils.populateForm(partnerEngagementForm, details);
        }
        BaseAncHomeVisitAction partner_engagement = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_partner_engagement))
                .withOptional(false)
                .withDetails(details)
                .withFormName("anc_hv_partner_engagement")
                .withHelper(new PartnerEngagementAction())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .build();
        actionList.put(context.getString(R.string.anc_home_visit_partner_engagement), partner_engagement);
    }
    private class DangerSignsAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {
        private String danger_signs_counseling;
        private String danger_signs_present;
        private Context context;

        @Override
        public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
            this.context = context;
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                danger_signs_counseling = JsonFormUtils.getValue(jsonObject, "danger_signs_counseling");
                danger_signs_present = JsonFormUtils.getCheckBoxValue(jsonObject, "danger_signs_present");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return null;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return null;
        }

        @Override
        public String postProcess(String s) {
            try {
                if (danger_signs_present.contains("None") || danger_signs_present.equals("Hakuna")) {
                    evaluateHealthFacilityVisit(details, memberObject, dateMap, context);
                    evaluateFamilyPlanning(details, context);
                    // evaluateNutritionStatus(details, context);
                    evaluateCounsellingStatus(details, context);
                    evaluateMalaria(details, context);
                    evaluateObservation(details, context);
                    evaluateRemarks(details, context);
                    evaluatePostpartumCareForMother();
                    evaluateEarlyStimulation();
                    evaluatePostpartumDangerSigns();
                    evaluateCommunityHealthWorkerObservation(details, context);
                    evaluateImmediateNewBornCare();
                    evaluateAncClinicAttendance();
                    evaluateNutritionCounselling();
                    evaluateBirthPreparedness(details, memberObject);
                    evaluateHIVAIDSGeneralInformation();
                    evaluateBreastFeeding(details, memberObject, context);
                    evaluateNewBornDangerSign();
                    evaluatePartnerEngagement(details, context);
                } else {
                    Timber.d(actionList.toString());
                    actionList.remove(context.getString(R.string.anc_home_visit_family_planning));
                    actionList.remove(context.getString(R.string.anc_home_visit_nutrition_status));
                    actionList.remove(context.getString(R.string.anc_home_visit_counselling_task));
                    actionList.remove(context.getString(R.string.anc_home_visit_malaria_prevention));
                    actionList.remove(context.getString(R.string.anc_home_visit_observations_n_illnes));
                    actionList.remove(context.getString(R.string.anc_home_visit_remarks_and_comments));
                    actionList.remove(context.getString(R.string.anc_home_visit_postpartum_care_for_mother));
                    actionList.remove(context.getString(R.string.anc_home_visit_early_stimulation));
                    actionList.remove(context.getString(R.string.anc_home_visit_postpartum_danger_signs));
                    actionList.remove(context.getString(R.string.anc_home_visit_community_health_worker_observations));
                    actionList.remove(context.getString(R.string.anc_home_visit_immediate_newborn_care));
                    actionList.remove(context.getString(R.string.anc_hv_clinic_attendance));
                    actionList.remove(context.getString(R.string.anc_home_visit_birth_preparedness));
                    actionList.remove(context.getString(R.string.anc_home_visit_hiv_aids_general_information));
                    actionList.remove(context.getString(R.string.anc_home_visit_breast_feeding));
                    actionList.remove(context.getString(R.string.anc_home_visit_new_born_danger_signs));
                    actionList.remove(context.getString(R.string.anc_home_visit_partner_engagement));
                    actionList.remove(visit_title);
                }
            } catch (BaseAncHomeVisitAction.ValidationException e) {
                Timber.e(e);
            }
            new AppExecutors().mainThread().execute(() -> callBack.preloadActions(actionList));
            return null;
        }

        @Override
        public String evaluateSubTitle() {
            if (danger_signs_present.contains("None") || danger_signs_present.equals("Hakuna")) {
                return MessageFormat.format(context.getString(R.string.anc_home_visit_danger_signs) + ": " + "{0}", danger_signs_present) +
                        "\n" +
                        MessageFormat.format(context.getString(R.string.anc_health_facility_counselling_subtitle) + " " + "{0}",
                                (danger_signs_counseling.equalsIgnoreCase("Yes") ? context.getString(R.string.done).toLowerCase() : context.getString(R.string.not_done).toLowerCase())
                        );
            } else {
                return MessageFormat.format(context.getString(R.string.anc_home_visit_danger_signs) + ": " + "{0}", danger_signs_present) +
                        "\n" + context.getString(R.string.refer_to_facility);
            }
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(danger_signs_present)) {
                return BaseAncHomeVisitAction.Status.PENDING;
            } else if (danger_signs_present.contains("None") || danger_signs_present.equals("Hakuna")) {
                if (danger_signs_counseling.equalsIgnoreCase("Yes")) {
                    return BaseAncHomeVisitAction.Status.COMPLETED;
                } else if (danger_signs_counseling.equalsIgnoreCase("No")) {
                    return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
                } else {
                    return BaseAncHomeVisitAction.Status.PENDING;
                }
            } else {
                return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
            }
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
            Timber.v("onPayloadReceived");
        }
    }

    private void evaluateNutritionCounselling() throws BaseAncHomeVisitAction.ValidationException {
        String visit_title = context.getString(R.string.anc_hv_nutrition_counselling);
        BaseAncHomeVisitAction nutrition_counselling = new BaseAncHomeVisitAction.Builder(context, visit_title)
                .withOptional(false)
                .withDetails(details)
                .withHelper(new NutritionCounsellingAction())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withFormName("anc_hv_nutrition_counselling")
                .build();

        actionList.put(visit_title, nutrition_counselling);
    }

    private class HealthFacilityAction extends HealthFacilityVisitAction {
        private Context context;

        private String anc_hf_visit;
        private String anc_hf_visit_date;
        private Date visitDate;


        public HealthFacilityAction(MemberObject memberObject, Map<Integer, LocalDate> dateMap) {
            super(memberObject, dateMap);
        }

        @Override
        public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
            super.onJsonFormLoaded(jsonPayload, context, map);
            this.context = context;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);

                anc_hf_visit = JsonFormUtils.getValue(jsonObject, "anc_hf_visit");
                anc_hf_visit_date = JsonFormUtils.getValue(jsonObject, "anc_hf_visit_date");
                visitDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(anc_hf_visit_date);

            } catch (Exception e) {
                Timber.e(e);
            }
        }

        @Override
        public String getPreProcessed() {
            return super.getPreProcessed();
        }

        @Override
        public String evaluateSubTitle() {
            StringBuilder stringBuilder = new StringBuilder();
            if (anc_hf_visit.equalsIgnoreCase("No")) {
                stringBuilder.append(context.getString(R.string.visit_not_done).replace("\n", ""));
            } else {
                stringBuilder.append(MessageFormat.format("{0}: {1}\n", context.getString(R.string.date), new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(visitDate)));
            }
            return stringBuilder.toString();
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(anc_hf_visit)) {
                return BaseAncHomeVisitAction.Status.PENDING;
            }

            if (anc_hf_visit.equalsIgnoreCase("Yes")) {
                return BaseAncHomeVisitAction.Status.COMPLETED;
            } else {
                return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
            }
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
            Timber.v("onPayloadReceived");
        }
    }

    private class FamilyPlanningAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {
        private Context context;
        private String fam_planning;

        @Override
        public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
            this.context = context;
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                fam_planning = JsonFormUtils.getValue(jsonObject, "fam_planning").toLowerCase();
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return null;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return null;
        }

        @Override
        public String postProcess(String s) {
            return null;
        }

        @Override
        public String evaluateSubTitle() {
            String subTitle = (fam_planning.equalsIgnoreCase("Yes") ? context.getString(R.string.family_planning_done).toLowerCase() : context.getString(R.string.family_planning_not_done).toLowerCase());
            return StringUtils.capitalize(subTitle);
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(fam_planning)) {
                return BaseAncHomeVisitAction.Status.PENDING;
            }

            if (fam_planning.equalsIgnoreCase("Yes")) {
                return BaseAncHomeVisitAction.Status.COMPLETED;
            } else {
                return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
            }
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
            Timber.v("onPayloadReceived");
        }
    }

    private class NutritionAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {
        private Context context;
        private String nutrition_status;

        @Override
        public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
            this.context = context;
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                nutrition_status = JsonFormUtils.getValue(jsonObject, "nutrition_status");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return null;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return null;
        }

        @Override
        public String postProcess(String s) {
            return null;
        }

        @Override
        public String evaluateSubTitle() {
            if (nutrition_status.equalsIgnoreCase("Normal"))
                return MessageFormat.format(context.getString(R.string.nutrition_status) + ": " + "{0}", context.getString(R.string.anc_nutrition_status_normal));
            else if (nutrition_status.equalsIgnoreCase("Moderate"))
                return MessageFormat.format(context.getString(R.string.nutrition_status) + ": " + "{0}", context.getString(R.string.anc_nutrition_status_normal));
            else
                return MessageFormat.format(context.getString(R.string.nutrition_status) + ": " + "{0}", context.getString(R.string.anc_nutrition_status_severe));
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(nutrition_status)) {
                return BaseAncHomeVisitAction.Status.PENDING;
            }
            return BaseAncHomeVisitAction.Status.COMPLETED;
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
            Timber.v("onPayloadReceived");
        }
    }

    private class CounsellingStatusAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {
        private Context context;
        private String counselling_given;

        @Override
        public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
            this.context = context;
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                counselling_given = JsonFormUtils.getValue(jsonObject, "counselling_given").toLowerCase();
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return null;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return null;
        }

        @Override
        public String postProcess(String s) {
            return null;
        }

        @Override
        public String evaluateSubTitle() {
            String subTitle = (!counselling_given.contains("chk_none") ? context.getString(R.string.done).toLowerCase() : context.getString(R.string.not_done).toLowerCase());
            return MessageFormat.format("{0} {1}", context.getString(R.string.counselling), subTitle);
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(counselling_given)) {
                return BaseAncHomeVisitAction.Status.PENDING;
            } else if (counselling_given.contains("chk_none")) {
                return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
            }

            return BaseAncHomeVisitAction.Status.COMPLETED;
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
            Timber.v("onPayloadReceived");
        }
    }

    private class MalariaAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {
        private String fam_llin;
        private String llin_2days;
        private String llin_condition;
        private Context context;

        @Override
        public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
            this.context = context;
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                fam_llin = JsonFormUtils.getValue(jsonObject, "fam_llin");
                llin_2days = JsonFormUtils.getValue(jsonObject, "llin_2days");
                llin_condition = JsonFormUtils.getValue(jsonObject, "llin_condition");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return null;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return null;
        }

        @Override
        public String postProcess(String s) {
            return null;
        }

        @Override
        public String evaluateSubTitle() {
            if (fam_llin.equalsIgnoreCase("No"))
                return MessageFormat.format(context.getString(R.string.uses_net) + ": " + "{0}", context.getString(R.string.anc_malaria_field_no));
            else
                return MessageFormat.format(context.getString(R.string.uses_net) + ": " + "{0}",
                        (fam_llin.equalsIgnoreCase("Yes") ? context.getString(R.string.anc_malaria_field_yes) : context.getString(R.string.anc_malaria_field_no))
                                + "\n" + MessageFormat.format(context.getString(R.string.slept_under_net) + ": " + "{0}",
                                (llin_2days.equalsIgnoreCase("Yes") ? context.getString(R.string.anc_malaria_field_yes) : context.getString(R.string.anc_malaria_field_no))
                                        + "\n" + MessageFormat.format(context.getString(R.string.net_condition) + ": " + "{0}",
                                        (llin_condition.equalsIgnoreCase("Good") ? context.getString(R.string.anc_malaria_net_condition_good) : context.getString(R.string.anc_malaria_net_condition_bad)))));
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(fam_llin)) {
                return BaseAncHomeVisitAction.Status.PENDING;
            }

            if (fam_llin.equalsIgnoreCase("Yes") && llin_2days.equalsIgnoreCase("Yes") && llin_condition.equalsIgnoreCase("Good")) {
                return BaseAncHomeVisitAction.Status.COMPLETED;
            } else {
                return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
            }
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
            Timber.v("onPayloadReceived");
        }
    }

    private class ObservationAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {
        private String date_of_illness;
        private String illness_description;
        private String action_taken;
        private Context context;
        private LocalDate illnessDate;

        @Override
        public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
            this.context = context;
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                date_of_illness = JsonFormUtils.getValue(jsonObject, "date_of_illness");
                illness_description = JsonFormUtils.getValue(jsonObject, "illness_description");
                action_taken = JsonFormUtils.getCheckBoxValue(jsonObject, "action_taken");
                illnessDate = DateTimeFormat.forPattern("dd-MM-yyyy").parseLocalDate(date_of_illness);
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return null;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return null;
        }

        @Override
        public String postProcess(String s) {
            return null;
        }

        @Override
        public String evaluateSubTitle() {
            if (illnessDate == null) {
                return "";
            }

            return MessageFormat.format("{0}: {1}\n {2}: {3}",
                    DateTimeFormat.forPattern("dd MMM yyyy").print(illnessDate),
                    illness_description, context.getString(R.string.action_taken), action_taken
            );
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(date_of_illness)) {
                return BaseAncHomeVisitAction.Status.PENDING;
            }

            return BaseAncHomeVisitAction.Status.COMPLETED;
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
            Timber.v("onPayloadReceived");
        }
    }

    private class RemarksAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {
        private String chw_comment_anc;
        private Context context;

        @Override
        public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
            this.context = context;
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                chw_comment_anc = JsonFormUtils.getValue(jsonObject, "chw_comment_anc");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return null;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return null;
        }

        @Override
        public String postProcess(String s) {
            return null;
        }

        @Override
        public String evaluateSubTitle() {
            return MessageFormat.format("{0}: {1}",
                    context.getString(R.string.remarks_and__comments), StringUtils.capitalize(chw_comment_anc));
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(chw_comment_anc)) {
                return BaseAncHomeVisitAction.Status.PENDING;
            }

            return BaseAncHomeVisitAction.Status.COMPLETED;
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
            Timber.v("onPayloadReceived");
        }
    }

    private class NutritionCounsellingAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {
        private Context context;
        private String available_foods = "";

        @Override
        public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
            this.context = context;
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                available_foods = JsonFormUtils.getCheckBoxValue(jsonObject, "foods_available");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return null;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return null;
        }

        @Override
        public String postProcess(String s) {
            return null;
        }

        @Override
        public String evaluateSubTitle() {
            return MessageFormat.format("{0}: {1}", context.getString(R.string.foods_available), available_foods);
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (available_foods.isEmpty()){
                return BaseAncHomeVisitAction.Status.PENDING;
            }else {
                return  BaseAncHomeVisitAction.Status.COMPLETED;
            }
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
            Timber.v("onPayloadReceived");
        }
    }

    private class CommunityHealthWorkerObservationsAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {
        private String value = "";
        private String anyone_presence = "";
        private Context context;

        @Override
        public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
            this.context = context;
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                value = JsonFormUtils.getCheckBoxValue(jsonObject, "anyone_else_present_during_visit");
                anyone_presence = JsonFormUtils.getValue(jsonObject, "anyone_else_present_during_visit");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return null;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return null;
        }

        @Override
        public String postProcess(String s) {
            return null;
        }

        @Override
        public String evaluateSubTitle() {
            if (!value.isEmpty()){
                return MessageFormat.format(context.getString(R.string.community_health_worker_observations_evaluate_sub_title), value);
            } else{
                return value;
            }
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (!anyone_presence.isEmpty()){
                if (anyone_presence.contains("anyone_present_yes")){
                    return BaseAncHomeVisitAction.Status.COMPLETED;
                } else{
                    return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
                }
            }
            else
                return BaseAncHomeVisitAction.Status.PENDING;
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
            Timber.v("onPayloadReceived");
        }
    }
    private class ClinicAttendanceAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {

        private Context context;
        private String clinic_attendance;

        @Override
        public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
            this.context = context;
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                clinic_attendance = JsonFormUtils.getValue(jsonObject, "attend_anc_clinic_visit");
            }catch (JSONException e){
                e.printStackTrace();
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return null;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return null;
        }

        @Override
        public String postProcess(String s) {
            return null;
        }

        @Override
        public String evaluateSubTitle() {
            if (clinic_attendance.equalsIgnoreCase("Yes")) {
                return MessageFormat.format("{0}: {1}", context.getString(R.string.anc_hv_clinic_attendance_sub_title), context.getString(R.string.yes));
            } else {
                return MessageFormat.format("{0}: {1}", context.getString(R.string.anc_hv_clinic_attendance_sub_title), context.getString(R.string.no));
            }
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(clinic_attendance)){
                return BaseAncHomeVisitAction.Status.PENDING;
            }else{
                return BaseAncHomeVisitAction.Status.COMPLETED;
            }
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {

        }
    }
    private class BirthPreparednessAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {

        private Context context;
        private String location_nearest_health_facility = "";
        private String savings_preparedness = "";
        private String birth_companion_preparedness = "";
        private String family_member_individual_stay_home_preparedness = "";
        private String transportation_preparedness = "";

        @Override
        public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
            this.context = context;
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try{
                JSONObject jsonObject = new JSONObject(jsonPayload);
                location_nearest_health_facility = JsonFormUtils.getValue(jsonObject, "location_nearest_health_facility");
                savings_preparedness = JsonFormUtils.getValue(jsonObject, "savings_preparedness");
                birth_companion_preparedness = JsonFormUtils.getValue(jsonObject, "birth_companion_preparedness");
                family_member_individual_stay_home_preparedness = JsonFormUtils.getValue(jsonObject, "family_member_individual_stay_home_preparedness");
                transportation_preparedness = JsonFormUtils.getValue(jsonObject, "transportation_preparedness");
            }catch (Exception e){
                Timber.e(e);
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return null;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return null;
        }

        @Override
        public String postProcess(String jsonPayload) {
            return null;
        }

        @Override
        public String evaluateSubTitle() {
            return MessageFormat.format(
                    context.getString(R.string.birth_preparedness_summary),
                    location_nearest_health_facility,
                    savings_preparedness,
                    birth_companion_preparedness,
                    family_member_individual_stay_home_preparedness,
                    transportation_preparedness
            );
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (location_nearest_health_facility.equalsIgnoreCase("yes") &&
                    savings_preparedness.equalsIgnoreCase("yes") &&
                    birth_companion_preparedness.equalsIgnoreCase("yes") &&
                    family_member_individual_stay_home_preparedness.equalsIgnoreCase("yes") &&
                    transportation_preparedness.equalsIgnoreCase("yes")){
                return BaseAncHomeVisitAction.Status.COMPLETED;
            }
            else if (location_nearest_health_facility.equalsIgnoreCase("yes") ||
                    savings_preparedness.equalsIgnoreCase("yes") ||
                    birth_companion_preparedness.equalsIgnoreCase("yes") ||
                    family_member_individual_stay_home_preparedness.equalsIgnoreCase("yes") ||
                    transportation_preparedness.equalsIgnoreCase("yes")){
                return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
            }
            else
                return BaseAncHomeVisitAction.Status.PENDING;
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction ancHomeVisitAction) {

        }
    }

private class BreastFeedingActionHelper implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {

    private String preg_woman_other_children;
    private String preg_woman_breastfeed;


    @Override
    public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {

    }

    @Override
    public String getPreProcessed() {
        return null;
    }

    @Override
    public void onPayloadReceived(String s) {

        try {
            JSONObject jsonObject = new JSONObject(s);
            preg_woman_other_children = JsonFormUtils.getValue(jsonObject, "preg_woman_other_children");
            preg_woman_breastfeed = JsonFormUtils.getValue(jsonObject, "preg_woman_breastfeed");
        } catch (JSONException e) {
            Timber.e(e);
        }

    }

    @Override
    public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
        return null;
    }

    @Override
    public String getPreProcessedSubTitle() {
        return null;
    }

    @Override
    public String postProcess(String s) {
        return null;
    }

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(preg_woman_other_children) || StringUtils.isBlank(preg_woman_breastfeed)) {
            return BaseAncHomeVisitAction.Status.PENDING;
        } else if (preg_woman_breastfeed.contains("chk_no")) {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        }
    }

    @Override
    public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {

    }
}
    private class PartnerEngagementAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {
        private String partner_presence;
        private Context context;

        @Override
        public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
            this.context = context;
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                partner_presence = JsonFormUtils.getCheckBoxValue(jsonObject, "partner_head_of_household");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return null;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return null;
        }

        @Override
        public String postProcess(String s) {
            return null;
        }

        @Override
        public String evaluateSubTitle() {
            return MessageFormat.format(context.getString(R.string.partner_engagement_evaluate_sub_title), partner_presence);
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (!StringUtils.isBlank(partner_presence))
                if(partner_presence.equalsIgnoreCase("yes") || partner_presence.equalsIgnoreCase("ndio")){
                    return BaseAncHomeVisitAction.Status.COMPLETED;
                }else{
                    return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
                }
            else
                return BaseAncHomeVisitAction.Status.PENDING;
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
            Timber.v("onPayloadReceived");
        }
    }
}

