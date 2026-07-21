package org.smartregister.chw.interactor;

import android.content.Context;
import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.mothermentor.contract.BaseMotherMentorVisitContract;
import org.smartregister.chw.mothermentor.domain.VisitDetail;
import org.smartregister.chw.mothermentor.interactor.BaseMotherMentorServiceVisitInteractor;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MotherMentorHouseholdVisitInteractor extends BaseMotherMentorServiceVisitInteractor {
    private static final String ACTION_KUSUDI_LA_ZIARA = "Kusudi la Ziara";
    private static final String ACTION_WANAKAYA_WALIOPEWA_USHAURI = "Wanakaya waliopewa ushauri";
    private static final String ACTION_MADA_ZILIZOFUNDISHWA = "Mada zilizofundishwa";
    private static final String ACTION_MAONI = "Maoni";

    private static final String FORM_KUSUDI_LA_ZIARA = "mothermentor_household_visit_purpose";
    private static final String FORM_WANAKAYA_WALIOPEWA_USHAURI = "mother_mentor_household_members_who_received_counselling";
    private static final String FORM_MADA_ZILIZOFUNDISHWA = "mother_mentor_household_visit_topics_taught";
    private static final String FORM_MAONI = "mothermentor_household_visit_comment";

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
                addVisitAction(ACTION_KUSUDI_LA_ZIARA, FORM_KUSUDI_LA_ZIARA);
                addVisitAction(ACTION_WANAKAYA_WALIOPEWA_USHAURI, FORM_WANAKAYA_WALIOPEWA_USHAURI);
                addVisitAction(ACTION_MADA_ZILIZOFUNDISHWA, FORM_MADA_ZILIZOFUNDISHWA);
                addVisitAction(ACTION_MAONI, FORM_MAONI);
            } catch (BaseMotherMentorVisitAction.ValidationException e) {
                e.printStackTrace();
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(householdVisitActions));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void addVisitAction(String title, String formName) throws BaseMotherMentorVisitAction.ValidationException {
        BaseMotherMentorVisitAction action = getBuilder(title)
                .withOptional(false)
                .withDetails(details)
                .withFormName(formName)
                .build();
        householdVisitActions.put(title, action);
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
