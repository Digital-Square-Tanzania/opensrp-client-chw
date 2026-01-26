package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.chw.core.R;
import org.smartregister.chw.interactor.TbLeprosyVisitInteractor;
import org.smartregister.chw.tbleprosy.activity.BaseTbLeprosyVisitActivity;
import org.smartregister.chw.tbleprosy.dao.TbLeprosyDao;
import org.smartregister.chw.tbleprosy.domain.MemberObject;
import org.smartregister.chw.tbleprosy.interactor.BaseTbLeprosyServiceVisitInteractor;
import org.smartregister.chw.tbleprosy.presenter.BaseTbLeprosyVisitPresenter;
import org.smartregister.chw.tbleprosy.util.Constants;
import org.smartregister.chw.vmmc.interactor.BaseVmmcFollowUpInteractor;
import org.smartregister.chw.vmmc.model.BaseVmmcVisitAction;
import org.smartregister.chw.vmmc.presenter.BaseVmmcVisitPresenter;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.util.LangUtils;

import java.util.LinkedHashMap;
import java.util.Map;


public class TbLeprosyContactVisitActivity extends BaseTbLeprosyVisitActivity {
    public static void startTbLeprosyVisitActivity(Activity activity, String baseEntityId, Boolean editMode) {
        Intent intent = new Intent(activity, TbLeprosyContactVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, editMode);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.TBLEPROSY_PROFILE);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }



    @Override
    protected void registerPresenter() {
        presenter = new BaseTbLeprosyVisitPresenter(memberObject, this, new TbLeprosyVisitInteractor(org.smartregister.chw.tbleprosy.util.Constants.EVENT_TYPE.TB_LEPROSY_SERVICES));
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
    protected void attachBaseContext(Context base) {
        // get language from prefs
        String lang = LangUtils.getLanguage(base.getApplicationContext());
        super.attachBaseContext(LangUtils.setAppLocale(base, lang));
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        return TbLeprosyDao.getContact(baseEntityId);
    }
}

