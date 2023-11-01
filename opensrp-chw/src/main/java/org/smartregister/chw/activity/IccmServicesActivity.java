package org.smartregister.chw.activity;

import static org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.FORM_SUBMISSION_ID;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.interactor.IccmServicesActivityInteractor;
import org.smartregister.chw.ld.util.Constants;
import org.smartregister.chw.malaria.activity.BaseIccmVisitActivity;
import org.smartregister.chw.malaria.domain.IccmMemberObject;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;
import org.smartregister.chw.malaria.presenter.BaseIccmVisitPresenter;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Ilakoze Jumanne on 2023-04-20
 */
public class IccmServicesActivity extends BaseIccmVisitActivity {

    public static void startIccmServicesActivity(Activity activity, String baseEntityId, Boolean editMode) {
        Intent intent = new Intent(activity, IccmServicesActivity.class);
        intent.putExtra(FORM_SUBMISSION_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, editMode);

        activity.startActivityForResult(intent, org.smartregister.chw.anc.util.Constants.REQUEST_CODE_HOME_VISIT);
    }

    @Override
    protected void registerPresenter() {
        presenter = new BaseIccmVisitPresenter(memberObject, this, new IccmServicesActivityInteractor());
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
    public void initializeActions(LinkedHashMap<String, BaseIccmVisitAction> map) {
        //Clearing the action List before recreation
        actionList.clear();

        //Rearranging the actions according to a specific arrangement
        if (map.containsKey(getString(R.string.iccm_medical_history))) {
            actionList.put(getString(R.string.iccm_medical_history), map.get(getString(R.string.iccm_medical_history)));
        }

        if (map.containsKey(getString(R.string.iccm_physical_examination))) {
            actionList.put(getString(R.string.iccm_physical_examination), map.get(getString(R.string.iccm_physical_examination)));
        }

        if (map.containsKey(getString(R.string.iccm_pneumonia))) {
            actionList.put(getString(R.string.iccm_pneumonia), map.get(getString(R.string.iccm_pneumonia)));
        }

        if (map.containsKey(getString(R.string.iccm_diarrhea))) {
            actionList.put(getString(R.string.iccm_diarrhea), map.get(getString(R.string.iccm_diarrhea)));
        }

        if (map.containsKey(getString(R.string.iccm_malaria))) {
            actionList.put(getString(R.string.iccm_malaria), map.get(getString(R.string.iccm_malaria)));
        }

        //====================End of Necessary evil ====================================


        for (Map.Entry<String, BaseIccmVisitAction> entry : map.entrySet()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                actionList.putIfAbsent(entry.getKey(), entry.getValue());
            } else {
                actionList.put(entry.getKey(), entry.getValue());
            }
        }

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        displayProgressBar(false);


        super.initializeActions(map);
    }

    @Override
    public void redrawHeader(IccmMemberObject memberObject) {
        String clientAge = (org.smartregister.chw.core.utils.Utils.getTranslatedDate(org.smartregister.chw.core.utils.Utils.getDuration(memberObject.getAge()), getBaseContext()));
        tvTitle.setText(MessageFormat.format("{0}, {1} \u00B7 {2}", memberObject.getFullName(), clientAge, getString(org.smartregister.malaria.R.string.iccm_visit)));
    }
}
