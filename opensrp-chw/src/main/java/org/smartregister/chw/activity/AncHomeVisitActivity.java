package org.smartregister.chw.activity;

import static org.smartregister.util.JsonFormUtils.createEvent;
import static org.smartregister.util.JsonFormUtils.generateRandomUUIDString;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.activity.BaseAncHomeVisitActivity;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.presenter.BaseAncHomeVisitPresenter;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.task.RunnableTask;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.interactor.AncHomeVisitInteractor;
import org.smartregister.chw.referral.ReferralLibrary;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.chw.util.LinkageUtils;
import org.smartregister.chw.util.ReferralUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.util.LangUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * @author rkodev
 */
public class AncHomeVisitActivity extends BaseAncHomeVisitActivity {

    public static void startMe(Activity activity, String baseEntityID, Boolean isEditMode) {
        Intent intent = new Intent(activity, AncHomeVisitActivity.class);
        intent.putExtra(org.smartregister.chw.anc.util.Constants.ANC_MEMBER_OBJECTS.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(org.smartregister.chw.anc.util.Constants.ANC_MEMBER_OBJECTS.EDIT_MODE, isEditMode);
        activity.startActivityForResult(intent, org.smartregister.chw.anc.util.Constants.REQUEST_CODE_HOME_VISIT);
    }

    @Override
    protected void registerPresenter() {
        presenter = new BaseAncHomeVisitPresenter(memberObject, this, new AncHomeVisitInteractor(this));
    }

    @Override
    public void submittedAndClose() {
        // recompute schedule
        Runnable runnable = () -> ChwScheduleTaskExecutor.getInstance().execute(memberObject.getBaseEntityId(), CoreConstants.EventType.ANC_HOME_VISIT, new Date());
        org.smartregister.chw.util.Utils.startAsyncTask(new RunnableTask(runnable), null);
        super.submittedAndClose();
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {

        Form form = new Form();
        form.setActionBarBackground(R.color.family_actionbar);
        form.setWizard(false);
        form.setHideSaveLabel(true);

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(Constants.WizardFormActivity.EnableOnCloseDialog, false);
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
    public void onDestroy() {
        try {
            super.onDestroy();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void submitVisit() {
        super.submitVisit();

        Map<String, BaseAncHomeVisitAction> actions = this.getAncHomeVisitActions();
        if (actions != null){
            BaseAncHomeVisitAction ancMinorAilmentAction = actions.get(this.getString(org.smartregister.chw.R.string.anc_home_visit_minor_ailment));
            if (ancMinorAilmentAction != null) {
                String minorAilmentForm = ancMinorAilmentAction.getJsonPayload();
                if (minorAilmentForm != null) {
                    try {
                        //Create and save linkage Task
                        JSONObject minorAilmentObject = new JSONObject(minorAilmentForm);
                        String minorAilments = org.smartregister.chw.util.JsonFormUtils.getCheckBoxValue(minorAilmentObject, "minor_ailment").toLowerCase();

                        //Get fields from json object
                        JSONArray fields = org.smartregister.util.JsonFormUtils.fields(minorAilmentObject);
                        JSONObject metadata = org.smartregister.util.JsonFormUtils.getJSONObject(minorAilmentObject, "metadata");
                        String bindType = org.smartregister.chw.referral.util.Constants.Tables.REFERRAL;
                        String enconterType = org.smartregister.chw.referral.util.Constants.EventType.REGISTRATION;
                        String baseEntityId = memberObject.getBaseEntityId();

                        ReferralLibrary referralLibrary = ReferralLibrary.getInstance();

                        //Create and process event
                        Event event = createEvent(fields, metadata, LinkageUtils.getFormTag(referralLibrary), baseEntityId, enconterType, bindType);
                        LinkageUtils.addLinkageDetails(event, org.smartregister.chw.util.Constants.AddoLinkage.ANC_TASK_FOCUS, minorAilments);
                        NCUtils.processEvent(event.getBaseEntityId(), new JSONObject(org.smartregister.chw.anc.util.JsonFormUtils.gson.toJson(event)));
                        //LinkageUtils.processEvent(ReferralLibrary.getInstance(), event);

                        //Create linkage task
                        ReferralUtils.createLinkageTask(org.smartregister.Context.getInstance().allSharedPreferences(),
                                memberObject.getBaseEntityId(), event.getFormSubmissionId(), minorAilments, org.smartregister.chw.util.Constants.AddoLinkage.ANC_TASK_FOCUS);

                        Toast.makeText(getContext(), getContext().getString(org.smartregister.chw.R.string.linked_to_addo_message), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

    }

    @Override
    public void initializeActions(LinkedHashMap<String, BaseAncHomeVisitAction> map) {
        actionList.clear();
        //Necessary evil to rearrange the actions according to a specific arrangement
        if (map.containsKey(getString(R.string.anc_home_visit_danger_signs))) {
            BaseAncHomeVisitAction dangerSignsAction = map.get(getString(R.string.anc_home_visit_danger_signs));
            actionList.put(getString(R.string.anc_home_visit_danger_signs), dangerSignsAction);
        }
        //====================End of Necessary evil ====================================

        for (Map.Entry<String, BaseAncHomeVisitAction> entry : map.entrySet()) {
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
        redrawVisitUI();
    }
}
