package org.smartregister.chw.interactor;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.mothermentor.contract.BaseMotherMentorVisitContract;
import org.smartregister.chw.mothermentor.domain.VisitDetail;
import org.smartregister.chw.mothermentor.interactor.BaseMotherMentorServiceVisitInteractor;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.JsonFormUtils;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import timber.log.Timber;

public class MotherMentorServiceVisitInteractor extends BaseMotherMentorServiceVisitInteractor {
    private static final String FORM_VISIT_TYPE = "mothermentor_visit_type";
    private static final String FORM_GROUP_LINKAGE = "mothermentor_visit_group_linkage";
    private static final String FORM_EDUCATION_PROVIDED = "mothermentor_visit_education_provided";
    private static final String FORM_REFERRAL_PROVIDED = "mothermentor_visit_referal";
    private static final String FORM_NEXT_VISIT_DATE = "mothermentor_next_visit_date";
    private static final String FORM_COMMENT = "mothermentor_visit_comment";

    private final LinkedHashMap<String, BaseMotherMentorVisitAction> motherMentorVisitActions = new LinkedHashMap<>();

    public MotherMentorServiceVisitInteractor(String visitType) {
        super(visitType);
    }

    @Override
    protected void populateActionList(BaseMotherMentorVisitContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            motherMentorVisitActions.clear();
            try {
                addVisitAction("Visit Type", FORM_VISIT_TYPE, new VisitTypeActionHelper());
                addVisitAction("Group Linkage", FORM_GROUP_LINKAGE, new GroupLinkageActionHelper());
                addVisitAction("Education Provided", FORM_EDUCATION_PROVIDED, new EducationProvidedActionHelper());
                addVisitAction("Referral Provided", FORM_REFERRAL_PROVIDED, new ReferralProvidedActionHelper());
                addVisitAction("Next Visit Date", FORM_NEXT_VISIT_DATE, new NextVisitDateActionHelper());
                addVisitAction("Comment", FORM_COMMENT, new CommentActionHelper());
            } catch (BaseMotherMentorVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(motherMentorVisitActions));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void addVisitAction(String title, String formName) throws BaseMotherMentorVisitAction.ValidationException {
        addVisitAction(title, formName, null);
    }

    private void addVisitAction(String title, String formName, BaseMotherMentorVisitAction.MotherMentorVisitActionHelper helper)
            throws BaseMotherMentorVisitAction.ValidationException {
        BaseMotherMentorVisitAction action = getBuilder(title)
                .withOptional(false)
                .withDetails(details)
                .withHelper(helper)
                .withFormName(formName)
                .build();
        motherMentorVisitActions.put(title, action);
    }

    @Override
    protected String getEncounterType() {
        return Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES;
    }

    private static class VisitTypeActionHelper implements BaseMotherMentorVisitAction.MotherMentorVisitActionHelper {
        private String attendanceType;

        @Override
        public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
            // No preprocessing needed.
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                attendanceType = JsonFormUtils.getValue(new org.json.JSONObject(jsonPayload), "attendance_type");
            } catch (Exception e) {
                Timber.e(e);
                attendanceType = null;
            }
        }

        @Override
        public BaseMotherMentorVisitAction.ScheduleStatus getPreProcessedStatus() {
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
            return null;
        }

        @Override
        public BaseMotherMentorVisitAction.Status evaluateStatusOnPayload() {
            return StringUtils.isNotBlank(attendanceType)
                    ? BaseMotherMentorVisitAction.Status.COMPLETED
                    : BaseMotherMentorVisitAction.Status.PENDING;
        }

        @Override
        public void onPayloadReceived(BaseMotherMentorVisitAction motherMentorVisitAction) {
            // No-op.
        }
    }

    private static class GroupLinkageActionHelper implements BaseMotherMentorVisitAction.MotherMentorVisitActionHelper {
        private String psychosocialSupportGroupLinkage;
        private String igaGroupLinkage;

        @Override
        public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
            // No preprocessing needed.
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                org.json.JSONObject jsonObject = new org.json.JSONObject(jsonPayload);
                psychosocialSupportGroupLinkage = JsonFormUtils.getValue(jsonObject, "has_been_linked_to_psychosocial_support_group");
                igaGroupLinkage = JsonFormUtils.getValue(jsonObject, "has_been_linked_to_iga_group");
            } catch (Exception e) {
                Timber.e(e);
                psychosocialSupportGroupLinkage = null;
                igaGroupLinkage = null;
            }
        }

        @Override
        public BaseMotherMentorVisitAction.ScheduleStatus getPreProcessedStatus() {
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
            return null;
        }

        @Override
        public BaseMotherMentorVisitAction.Status evaluateStatusOnPayload() {
            return StringUtils.isNotBlank(psychosocialSupportGroupLinkage)
                    && StringUtils.isNotBlank(igaGroupLinkage)
                    ? BaseMotherMentorVisitAction.Status.COMPLETED
                    : BaseMotherMentorVisitAction.Status.PENDING;
        }

        @Override
        public void onPayloadReceived(BaseMotherMentorVisitAction motherMentorVisitAction) {
            // No-op.
        }
    }

    private static class EducationProvidedActionHelper implements BaseMotherMentorVisitAction.MotherMentorVisitActionHelper {
        private String educationProvided;

        @Override
        public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
            // No preprocessing needed.
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                educationProvided = JsonFormUtils.getValue(new org.json.JSONObject(jsonPayload), "education_provided");
            } catch (Exception e) {
                Timber.e(e);
                educationProvided = null;
            }
        }

        @Override
        public BaseMotherMentorVisitAction.ScheduleStatus getPreProcessedStatus() {
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
            return null;
        }

        @Override
        public BaseMotherMentorVisitAction.Status evaluateStatusOnPayload() {
            return StringUtils.isNotBlank(educationProvided)
                    ? BaseMotherMentorVisitAction.Status.COMPLETED
                    : BaseMotherMentorVisitAction.Status.PENDING;
        }

        @Override
        public void onPayloadReceived(BaseMotherMentorVisitAction motherMentorVisitAction) {
            // No-op.
        }
    }

    private static class ReferralProvidedActionHelper implements BaseMotherMentorVisitAction.MotherMentorVisitActionHelper {
        private String referralGiven;

        @Override
        public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
            // No preprocessing needed.
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                referralGiven = JsonFormUtils.getValue(new org.json.JSONObject(jsonPayload), "referral_given");
            } catch (Exception e) {
                Timber.e(e);
                referralGiven = null;
            }
        }

        @Override
        public BaseMotherMentorVisitAction.ScheduleStatus getPreProcessedStatus() {
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
            return null;
        }

        @Override
        public BaseMotherMentorVisitAction.Status evaluateStatusOnPayload() {
            return StringUtils.isNotBlank(referralGiven)
                    ? BaseMotherMentorVisitAction.Status.COMPLETED
                    : BaseMotherMentorVisitAction.Status.PENDING;
        }

        @Override
        public void onPayloadReceived(BaseMotherMentorVisitAction motherMentorVisitAction) {
            // No-op.
        }
    }

    private static class NextVisitDateActionHelper implements BaseMotherMentorVisitAction.MotherMentorVisitActionHelper {
        private String nextAppointmentDate;

        @Override
        public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
            // No preprocessing needed.
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                nextAppointmentDate = JsonFormUtils.getValue(new org.json.JSONObject(jsonPayload), "next_appointment_date");
            } catch (Exception e) {
                Timber.e(e);
                nextAppointmentDate = null;
            }
        }

        @Override
        public BaseMotherMentorVisitAction.ScheduleStatus getPreProcessedStatus() {
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
            return null;
        }

        @Override
        public BaseMotherMentorVisitAction.Status evaluateStatusOnPayload() {
            return StringUtils.isNotBlank(nextAppointmentDate)
                    ? BaseMotherMentorVisitAction.Status.COMPLETED
                    : BaseMotherMentorVisitAction.Status.PENDING;
        }

        @Override
        public void onPayloadReceived(BaseMotherMentorVisitAction motherMentorVisitAction) {
            // No-op.
        }
    }

    private static class CommentActionHelper implements BaseMotherMentorVisitAction.MotherMentorVisitActionHelper {
        private String comments;

        @Override
        public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
            // No preprocessing needed.
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                comments = JsonFormUtils.getValue(new org.json.JSONObject(jsonPayload), "comments");
            } catch (Exception e) {
                Timber.e(e);
                comments = null;
            }
        }

        @Override
        public BaseMotherMentorVisitAction.ScheduleStatus getPreProcessedStatus() {
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
            return null;
        }

        @Override
        public BaseMotherMentorVisitAction.Status evaluateStatusOnPayload() {
            return StringUtils.isNotBlank(comments)
                    ? BaseMotherMentorVisitAction.Status.COMPLETED
                    : BaseMotherMentorVisitAction.Status.PENDING;
        }

        @Override
        public void onPayloadReceived(BaseMotherMentorVisitAction motherMentorVisitAction) {
            // No-op.
        }
    }
}
