package org.smartregister.chw.interactor;

import android.content.Context;
import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.mothermentor.contract.BaseMotherMentorVisitContract;
import org.smartregister.chw.mothermentor.domain.VisitDetail;
import org.smartregister.chw.mothermentor.interactor.BaseMotherMentorServiceVisitInteractor;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.JsonFormUtils;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MotherMentorHouseholdVisitInteractor extends BaseMotherMentorServiceVisitInteractor {

    private static final String FORM_KUSUDI_LA_ZIARA = "mothermentor_household_visit_purpose";
    private static final String FORM_WANAKAYA_WALIOPEWA_USHAURI = "mother_mentor_household_members_who_received_counselling";
    private static final String FORM_MADA_ZILIZOFUNDISHWA = "mother_mentor_household_visit_topics_taught";
    private static final String FORM_MAONI = "mothermentor_household_visit_comment";
    private static final String FIELD_PURPOSE_OF_VISIT = "purpose_of_visit";
    private static final String FIELD_PARTICIPANTS = "participants";
    private static final String FIELD_TOPICS_TAUGHT = "topics_taught";
    private static final String FIELD_COMMENTS = "comments";

    private final LinkedHashMap<String, BaseMotherMentorVisitAction> householdVisitActions = new LinkedHashMap<>();

    public MotherMentorHouseholdVisitInteractor(String visitType) {
        super(visitType);
    }

    @Override
    public org.smartregister.chw.mothermentor.domain.MemberObject getMemberClient(String memberID, String profileType) {
        if (TextUtils.isEmpty(memberID)) {
            return null;
        }

        org.smartregister.chw.mothermentor.domain.MemberObject householdMember = getHouseholdMember(memberID);
        if (householdMember != null) {
            return householdMember;
        }

        return super.getMemberClient(memberID, profileType);
    }

    @Override
    protected void populateActionList(BaseMotherMentorVisitContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            householdVisitActions.clear();
            try {
                addVisitAction(context.getString(org.smartregister.chw.R.string.mothermentor_household_visit_kusudi_la_ziara), FORM_KUSUDI_LA_ZIARA, FIELD_PURPOSE_OF_VISIT);
                addVisitAction(context.getString(org.smartregister.chw.R.string.mothermentor_household_visit_wanakaya_waliopewa_ushauri), FORM_WANAKAYA_WALIOPEWA_USHAURI, FIELD_PARTICIPANTS);
                addVisitAction(context.getString(org.smartregister.chw.R.string.mothermentor_household_visit_mada_zilizofundishwa), FORM_MADA_ZILIZOFUNDISHWA, FIELD_TOPICS_TAUGHT);
                addVisitAction(context.getString(org.smartregister.chw.R.string.mothermentor_household_visit_maoni), FORM_MAONI, FIELD_COMMENTS);
            } catch (BaseMotherMentorVisitAction.ValidationException e) {
                e.printStackTrace();
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(householdVisitActions));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void addVisitAction(String title, String formName, String completionKey) throws BaseMotherMentorVisitAction.ValidationException {
        BaseMotherMentorVisitAction action = getBuilder(title)
                .withOptional(false)
                .withDetails(details)
                .withHelper(new FieldCompletionHelper(completionKey))
                .withFormName(formName)
                .build();
        householdVisitActions.put(title, action);
    }

    private static class FieldCompletionHelper implements BaseMotherMentorVisitAction.MotherMentorVisitActionHelper {
        private final String completionKey;
        private boolean completed;

        private FieldCompletionHelper(String completionKey) {
            this.completionKey = completionKey;
        }

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
            completed = StringUtils.isNotBlank(jsonPayload) && hasAnsweredKey(jsonPayload, completionKey);
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
            return completed ? BaseMotherMentorVisitAction.Status.COMPLETED : BaseMotherMentorVisitAction.Status.PENDING;
        }

        @Override
        public void onPayloadReceived(BaseMotherMentorVisitAction mothermentorVisitAction) {
            // No-op.
        }

        private boolean hasAnsweredKey(Object node, String key) {
            if (node == null || TextUtils.isEmpty(key)) {
                return false;
            }

            if (node instanceof org.json.JSONObject) {
                org.json.JSONObject jsonObject = (org.json.JSONObject) node;
                if (jsonObject.has(key) && !jsonObject.isNull(key)) {
                    Object value = jsonObject.opt(key);
                    if (value instanceof String) {
                        return StringUtils.isNotBlank((String) value);
                    }
                    return value != null;
                }

                java.util.Iterator<String> iterator = jsonObject.keys();
                while (iterator.hasNext()) {
                    String childKey = iterator.next();
                    if (hasAnsweredKey(jsonObject.opt(childKey), key)) {
                        return true;
                    }
                }
            } else if (node instanceof org.json.JSONArray) {
                org.json.JSONArray jsonArray = (org.json.JSONArray) node;
                for (int i = 0; i < jsonArray.length(); i++) {
                    if (hasAnsweredKey(jsonArray.opt(i), key)) {
                        return true;
                    }
                }
            }

            return false;
        }

        private boolean hasAnsweredKey(String jsonPayload, String key) {
            if (TextUtils.isEmpty(jsonPayload) || TextUtils.isEmpty(key)) {
                return false;
            }

            try {
                org.json.JSONObject jsonObject = new org.json.JSONObject(jsonPayload);
                String value = JsonFormUtils.getValue(jsonObject, key);
                if (StringUtils.isNotBlank(value)) {
                    return true;
                }

                if ("participants".equalsIgnoreCase(key) || "topics_taught".equalsIgnoreCase(key)) {
                    return StringUtils.isNotBlank(JsonFormUtils.getCheckBoxValue(jsonObject, key));
                }

                return hasAnsweredKey((Object) jsonObject, key);
            } catch (Exception e) {
                return false;
            }
        }
    }

    private org.smartregister.chw.mothermentor.domain.MemberObject getHouseholdMember(String baseEntityId) {
        try {
            CommonRepository repository = Utils.context().commonrepository(Utils.metadata().familyRegister.tableName);
            if (repository == null) {
                return null;
            }

            CommonPersonObject personObject = repository.findByBaseEntityId(baseEntityId);
            if (personObject != null) {
                org.smartregister.chw.mothermentor.domain.MemberObject memberObject = new org.smartregister.chw.mothermentor.domain.MemberObject();
                memberObject.setBaseEntityId(personObject.getCaseId());
                memberObject.setFamilyBaseEntityId(personObject.getCaseId());
                memberObject.setFirstName(Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.FIRST_NAME, false));
                memberObject.setLastName(Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.LAST_NAME, false));
                memberObject.setUniqueId(Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.UNIQUE_ID, false));
                memberObject.setAddress(Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.VILLAGE_TOWN, false));
                memberObject.setFamilyHead(Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.FAMILY_HEAD, false));
                memberObject.setPrimaryCareGiver(Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.PRIMARY_CAREGIVER, false));
                memberObject.setPhoneNumber(Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.PHONE_NUMBER, false));
                return memberObject;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void submitVisit(boolean editMode, String memberID, Map<String, BaseMotherMentorVisitAction> map,
                            BaseMotherMentorVisitContract.InteractorCallBack callBack) {
        super.submitVisit(editMode, memberID, getVisibleVisitActions(map), callBack);
    }

    @Override
    protected String getEncounterType() {
        return Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES;
    }

    private Map<String, BaseMotherMentorVisitAction> getVisibleVisitActions(Map<String, BaseMotherMentorVisitAction> map) {
        LinkedHashMap<String, BaseMotherMentorVisitAction> visibleActions = new LinkedHashMap<>();
        for (Map.Entry<String, BaseMotherMentorVisitAction> entry : map.entrySet()) {
            if (entry.getValue() != null && entry.getValue().isValid()) {
                visibleActions.put(entry.getKey(), entry.getValue());
            }
        }
        return visibleActions;
    }
}
