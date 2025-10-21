package org.smartregister.chw.activity;

import static org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_OUT_SCHOOL_FOLLOW_UP_VISIT;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.ayp.activity.BaseAypOutSchoolRecordServiceVisitActivity;
import org.smartregister.chw.ayp.activity.BaseAypVisitActivity;
import org.smartregister.chw.ayp.dao.AypDao;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.interactor.aypOutOfSchool.BaseAypOutSchoolClientVisitInteractor;
import org.smartregister.chw.ayp.model.BaseAypVisitAction;
import org.smartregister.chw.ayp.presenter.BaseAypVisitPresenter;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.vmmc.model.BaseVmmcVisitAction;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.util.LangUtils;

import java.util.LinkedHashMap;
import java.util.Map;


public class AypOutSchoolClientServiceVisitActivity extends BaseAypVisitActivity {
    public static void startAypVisitActivity(Activity activity, String baseEntityId, Boolean editMode) {
        Intent intent = new Intent(activity, AypOutSchoolClientServiceVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, editMode);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.ayp_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        return AypDao.getOutSchoolMember(baseEntityId);
    }

    @Override
    protected void registerPresenter() {
        presenter = new BaseAypVisitPresenter(memberObject, this, new BaseAypOutSchoolClientVisitInteractor(AYP_OUT_SCHOOL_FOLLOW_UP_VISIT));
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Form form = new Form();
        form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        form.setWizard(false);

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(org.smartregister.family.util.Constants.WizardFormActivity.EnableOnCloseDialog, false);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @Override
    public void initializeActions(LinkedHashMap<String, BaseAypVisitAction> map) {
        actionList.clear();

        //Necessary evil to rearrange the actions according to a specific arrangement

        if (map.containsKey(getString(R.string.ayp_out_school_service_status))) {
            BaseAypVisitAction visitTypeAction = map.get(getString(R.string.ayp_out_school_service_status));
            actionList.put(getString(R.string.ayp_out_school_service_status), visitTypeAction);
        }
        if (map.containsKey(getString(R.string.ayp_out_school_structural_services))) {
            BaseAypVisitAction visitTypeAction = map.get(getString(R.string.ayp_out_school_structural_services));
            actionList.put(getString(R.string.ayp_out_school_structural_services), visitTypeAction);
        }
        if (map.containsKey(getString(R.string.ayp_out_school_medical_services))) {
            BaseAypVisitAction visitTypeAction = map.get(getString(R.string.ayp_out_school_medical_services));
            actionList.put(getString(R.string.ayp_out_school_medical_services), visitTypeAction);
        }

        for (Map.Entry<String, BaseAypVisitAction> entry : map.entrySet()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                actionList.putIfAbsent(entry.getKey(), entry.getValue());
            } else {
                actionList.put(entry.getKey(), entry.getValue());
            }
        }
        //====================End of Necessary evil ====================================

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        displayProgressBar(false);
    }

    @Override
    protected void attachBaseContext(Context base) {
        // get language from prefs
        String lang = LangUtils.getLanguage(base.getApplicationContext());
        super.attachBaseContext(LangUtils.setAppLocale(base, lang));
    }

}

