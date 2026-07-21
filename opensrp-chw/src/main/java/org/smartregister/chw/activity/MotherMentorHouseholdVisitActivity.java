package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.chw.core.R;
import org.smartregister.chw.interactor.MotherMentorHouseholdVisitInteractor;
import org.smartregister.chw.mothermentor.activity.BaseMotherMentorVisitActivity;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.presenter.BaseMotherMentorVisitPresenter;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.util.LangUtils;

public class MotherMentorHouseholdVisitActivity extends BaseMotherMentorVisitActivity {

    public static void startMotherMentorHouseholdVisitActivity(Activity activity, String baseEntityId, Boolean editMode) {
        Intent intent = new Intent(activity, MotherMentorHouseholdVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, editMode);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.MOTHERMENTOR_PROFILE);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void registerPresenter() {
        presenter = new BaseMotherMentorVisitPresenter(memberObject, this,
                new MotherMentorHouseholdVisitInteractor(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES));
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Form form = new Form();
        form.setActionBarBackground(R.color.family_actionbar);
        form.setWizard(false);

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(org.smartregister.family.util.Constants.WizardFormActivity.EnableOnCloseDialog, false);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @Override
    public void submittedAndClose(String results) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(Constants.JSON_FORM_EXTRA.JSON, results);
        setResult(Activity.RESULT_OK, returnIntent);
        close();
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        if (TextUtils.isEmpty(baseEntityId)) {
            return null;
        }

        MemberObject householdMember = getHouseholdMember(baseEntityId);
        if (householdMember != null) {
            return householdMember;
        }

        MemberObject memberObject = MotherMentorDao.getMember(baseEntityId);
        return memberObject != null ? memberObject : MotherMentorDao.getContact(baseEntityId);
    }

    private MemberObject getHouseholdMember(String baseEntityId) {
        try {
            CommonRepository repository = Utils.context().commonrepository(Utils.metadata().familyRegister.tableName);
            if (repository == null) {
                return null;
            }

            CommonPersonObject personObject = repository.findByBaseEntityId(baseEntityId);
            if (personObject != null) {
                MemberObject memberObject = new MemberObject();
                memberObject.setBaseEntityId(personObject.getCaseId());
                memberObject.setFamilyBaseEntityId(personObject.getCaseId());
                memberObject.setFirstName(Utils.getValue(personObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.FIRST_NAME, false));
                memberObject.setLastName(Utils.getValue(personObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.LAST_NAME, false));
                memberObject.setUniqueId(Utils.getValue(personObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.UNIQUE_ID, false));
                memberObject.setAddress(Utils.getValue(personObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.VILLAGE_TOWN, false));
                memberObject.setFamilyHead(Utils.getValue(personObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.FAMILY_HEAD, false));
                memberObject.setPrimaryCareGiver(Utils.getValue(personObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.PRIMARY_CAREGIVER, false));
                memberObject.setPhoneNumber(Utils.getValue(personObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.PHONE_NUMBER, false));
                return memberObject;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void attachBaseContext(Context base) {
        String lang = LangUtils.getLanguage(base.getApplicationContext());
        super.attachBaseContext(LangUtils.setAppLocale(base, lang));
    }
}
