package org.smartregister.chw.interactor;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.actionhelper.BirthPreparednessAction;
import org.smartregister.chw.actionhelper.BreastFeedingActionHelper;
import org.smartregister.chw.actionhelper.ClinicAttendanceAction;
import org.smartregister.chw.actionhelper.CommunityHealthWorkerObservationsAction;
import org.smartregister.chw.actionhelper.CounsellingStatusAction;
import org.smartregister.chw.actionhelper.FamilyPlanningAction;
import org.smartregister.chw.actionhelper.HealthFacilityAction;
import org.smartregister.chw.actionhelper.MalariaAction;
import org.smartregister.chw.actionhelper.NutritionCounsellingAction;
import org.smartregister.chw.actionhelper.PMTCTActionHelper;
import org.smartregister.chw.actionhelper.PNCVisitLocationActionHelper;
import org.smartregister.chw.actionhelper.PartnerEngagementAction;
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
import java.util.LinkedHashMap;
import java.util.List;
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
        if (org.smartregister.chw.util.VisitUtils.isFirstVisit(memberObject) ||
                org.smartregister.chw.util.VisitUtils.isSecondVisit(memberObject)) {
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
    private void evaluatePMTCT() throws BaseAncHomeVisitAction.ValidationException {
        if (org.smartregister.chw.util.VisitUtils.isFirstVisit(memberObject) ||
                org.smartregister.chw.util.VisitUtils.isSecondVisit(memberObject) ||
                org.smartregister.chw.util.VisitUtils.isThirdVisit(memberObject)) {
            String visitTitle = context.getString(R.string.anc_home_visit_pmtct);
            BaseAncHomeVisitAction pmtctAction = new BaseAncHomeVisitAction.Builder(context, visitTitle)
                    .withOptional(false)
                    .withDetails(details)
                    .withHelper(new PMTCTActionHelper())
                    .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                    .withFormName("anc_hv_pmctc")
                    .build();
            actionList.put(visitTitle, pmtctAction);
        }
    }

    private void evaluatePostpartumCareForMother() throws BaseAncHomeVisitAction.ValidationException {
        if (org.smartregister.chw.util.VisitUtils.isThirdVisit(memberObject)) {
            BaseAncHomeVisitAction earlyStimulation = new BaseAncHomeVisitAction.Builder(
                    context, context.getString(R.string.anc_home_visit_postpartum_care_for_mother))
                    .withOptional(false)
                    .withDetails(details)
                    .withFormName("anc_hv_postpartum_care_for_mother")
                    .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                    .build();
            actionList.put(context.getString(R.string.anc_home_visit_postpartum_care_for_mother), earlyStimulation);
        }
    }

    private void evaluateEarlyStimulation() throws BaseAncHomeVisitAction.ValidationException {
        if (org.smartregister.chw.util.VisitUtils.isFirstVisit(memberObject) ||
                org.smartregister.chw.util.VisitUtils.isSecondVisit(memberObject) ||
                org.smartregister.chw.util.VisitUtils.isThirdVisit(memberObject)) {
            BaseAncHomeVisitAction earlyStimulation = new BaseAncHomeVisitAction.Builder(
                    context, context.getString(R.string.anc_home_visit_early_stimulation))
                    .withOptional(false)
                    .withDetails(details)
                    .withFormName("anc_hv_early_stimulation")
                    .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                    .build();
            actionList.put(context.getString(R.string.anc_home_visit_early_stimulation), earlyStimulation);
        }
    }

    private void evaluatePostpartumDangerSigns() throws BaseAncHomeVisitAction.ValidationException {
        if (org.smartregister.chw.util.VisitUtils.isThirdVisit(memberObject)) {
            BaseAncHomeVisitAction earlyStimulation = new BaseAncHomeVisitAction.Builder(
                    context, context.getString(R.string.anc_home_visit_postpartum_danger_signs))
                    .withOptional(false)
                    .withDetails(details)
                    .withFormName("anc_hv_postpartum_danger_signs")
                    .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                    .build();
            actionList.put(context.getString(R.string.anc_home_visit_postpartum_danger_signs), earlyStimulation);
        }
    }

    private void evaluateCommunityHealthWorkerObservation(Map<String, List<VisitDetail>> details,
                                                          final Context context) throws BaseAncHomeVisitAction.ValidationException {
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
        if (org.smartregister.chw.util.VisitUtils.isFirstVisit(memberObject) ||
                org.smartregister.chw.util.VisitUtils.isSecondVisit(memberObject) ||
                org.smartregister.chw.util.VisitUtils.isThirdVisit(memberObject)) {
            BaseAncHomeVisitAction earlyStimulation = new BaseAncHomeVisitAction.Builder(
                    context, context.getString(R.string.anc_home_visit_immediate_newborn_care))
                    .withOptional(false)
                    .withDetails(details)
                    .withFormName("anc_hv_immediate_newborn_care")
                    .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                    .build();
            actionList.put(context.getString(R.string.anc_home_visit_immediate_newborn_care), earlyStimulation);
        }
    }

    private void evaluateAncClinicAttendance() throws BaseAncHomeVisitAction.ValidationException {
        if (org.smartregister.chw.util.VisitUtils.isFirstVisit(memberObject) ||
                org.smartregister.chw.util.VisitUtils.isSecondVisit(memberObject) ) {
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
    }

    private void evaluateLAM() throws BaseAncHomeVisitAction.ValidationException {
        if (org.smartregister.chw.util.VisitUtils.isThirdVisit(memberObject)) {
            BaseAncHomeVisitAction lam = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_lam))
                    .withOptional(false)
                    .withDetails(details)
                    .withFormName("anc_hv_lam")
                    .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                    .build();
            actionList.put(context.getString(R.string.anc_home_visit_lam), lam);
        }
    }

    private void evaluateBreastFeeding(Map<String, List<VisitDetail>> details, final MemberObject memberObject,
                                       final Context context) throws BaseAncHomeVisitAction.ValidationException {
        if (org.smartregister.chw.util.VisitUtils.isSecondVisit(memberObject) || org.smartregister.chw.util.VisitUtils.isThirdVisit(memberObject)) {
            BaseAncHomeVisitAction bread_feeding_action = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_breast_feeding))
                .withOptional(false)
                .withDetails(details)
                .withHelper(new BreastFeedingActionHelper())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withFormName("anc_hv_breastfeeding")
                .build();
            actionList.put(context.getString(R.string.anc_home_visit_breast_feeding), bread_feeding_action);
        }
    }

    private void evaluateGenderIssues() throws BaseAncHomeVisitAction.ValidationException {
        if (org.smartregister.chw.util.VisitUtils.isFirstVisit(memberObject)) {
            BaseAncHomeVisitAction earlyStimulation = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_home_visit_gender_issues))
                    .withOptional(false)
                    .withDetails(details)
                    .withFormName("anc_hv_gender_issues")
                    .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                    .build();
            actionList.put(context.getString(R.string.anc_home_visit_gender_issues), earlyStimulation);
        }
    }

    private void evaluateNewBornDangerSign() throws BaseAncHomeVisitAction.ValidationException {
        if (org.smartregister.chw.util.VisitUtils.isThirdVisit(memberObject)) {
            BaseAncHomeVisitAction earlyStimulation = new BaseAncHomeVisitAction.Builder(
                    context, context.getString(R.string.anc_home_visit_new_born_danger_signs))
                    .withOptional(false)
                    .withDetails(details)
                    .withFormName("anc_hv_new_born_danger_signs")
                    .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                    .build();
            actionList.put(context.getString(R.string.anc_home_visit_new_born_danger_signs), earlyStimulation);
        }
    }

    private void evaluateHIVExposedInfantFollowUp() throws BaseAncHomeVisitAction.ValidationException {
        if (org.smartregister.chw.util.VisitUtils.isThirdVisit(memberObject)) {
            BaseAncHomeVisitAction earlyStimulation = new BaseAncHomeVisitAction.Builder(
                    context, context.getString(R.string.anc_home_visit_hiv_exposed_infant_follow_up))
                    .withOptional(false)
                    .withDetails(details)
                    .withFormName("anc_hv_hiv_exposed_infant_follow_up")
                    .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                    .build();
            actionList.put(context.getString(R.string.anc_home_visit_hiv_exposed_infant_follow_up), earlyStimulation);
        }
    }

    private void evaluatePostpartumPhysiologicalChanges() throws BaseAncHomeVisitAction.ValidationException {
        if (org.smartregister.chw.util.VisitUtils.isThirdVisit(memberObject)) {
            BaseAncHomeVisitAction earlyStimulation = new BaseAncHomeVisitAction.Builder(
                    context, context.getString(R.string.anc_home_visit_postpartum_physiological_changes))
                    .withOptional(false)
                    .withDetails(details)
                    .withFormName("anc_hv_postpartum_physiological_changes")
                    .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                    .build();
            actionList.put(context.getString(R.string.anc_home_visit_postpartum_physiological_changes), earlyStimulation);
        }
    }

    private void evaluateHIVAIDSGeneralInformation() throws BaseAncHomeVisitAction.ValidationException {
        if (org.smartregister.chw.util.VisitUtils.isFirstVisit(memberObject)) {
            BaseAncHomeVisitAction earlyStimulation = new BaseAncHomeVisitAction.Builder(
                    context, context.getString(R.string.anc_home_visit_hiv_aids_general_information))
                    .withOptional(false)
                    .withDetails(details)
                    .withFormName("anc_hv_hiv_aids_general_information")
                    .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                    .build();
            actionList.put(context.getString(R.string.anc_home_visit_hiv_aids_general_information), earlyStimulation);
        }
    }

    private void evaluateBirthPreparedness(Map<String, List<VisitDetail>> details,
                                           final MemberObject memberObject) throws BaseAncHomeVisitAction.ValidationException {
        if (org.smartregister.chw.util.VisitUtils.isFirstVisit(memberObject) ||
                org.smartregister.chw.util.VisitUtils.isSecondVisit(memberObject) ||
                org.smartregister.chw.util.VisitUtils.isThirdVisit(memberObject)) {
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

    private void evaluatePartnerEngagement(Map<String, List<VisitDetail>> details,
                                           final Context context) throws BaseAncHomeVisitAction.ValidationException {
        if (org.smartregister.chw.util.VisitUtils.isFirstVisit(memberObject) ||
                org.smartregister.chw.util.VisitUtils.isSecondVisit(memberObject) ||
                org.smartregister.chw.util.VisitUtils.isThirdVisit(memberObject)) {
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
    }

    private void evaluateNutritionCounselling() throws BaseAncHomeVisitAction.ValidationException {
        if (org.smartregister.chw.util.VisitUtils.isFirstVisit(memberObject) ||
                org.smartregister.chw.util.VisitUtils.isSecondVisit(memberObject)) {
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
                    evaluateLAM();
                    evaluateHIVExposedInfantFollowUp();
                    evaluatePostpartumPhysiologicalChanges();
                    evaluateGenderIssues();
                    evaluatePMTCT();
                } else {
                    Timber.d(actionList.toString());
                    actionList.remove(context.getString(R.string.anc_home_visit_family_planning));
                    actionList.remove(context.getString(R.string.anc_home_visit_nutrition_status));
                    actionList.remove(context.getString(R.string.anc_home_visit_counselling_task));
                    actionList.remove(context.getString(R.string.anc_home_visit_malaria_prevention));
                    actionList.remove(context.getString(R.string.anc_home_visit_observations_n_illnes));
                    actionList.remove(context.getString(R.string.anc_home_visit_remarks_and_comments));
                    actionList.remove(context.getString(R.string.anc_home_visit_gender_issues));
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
                    actionList.remove(context.getString(R.string.anc_home_visit_lam));
                    actionList.remove(context.getString(R.string.anc_home_visit_hiv_exposed_infant_follow_up));
                    actionList.remove(context.getString(R.string.anc_home_visit_postpartum_physiological_changes));
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
}