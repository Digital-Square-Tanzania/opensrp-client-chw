package org.smartregister.chw.activity;

import static org.smartregister.util.JsonFormUtils.createEvent;
import static org.smartregister.util.JsonFormUtils.generateRandomUUIDString;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.presenter.BaseAncHomeVisitPresenter;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.core.task.RunnableTask;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.interactor.PncHomeVisitInteractor;
import org.smartregister.chw.pnc.activity.BasePncHomeVisitActivity;
import org.smartregister.chw.referral.ReferralLibrary;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.chw.util.LinkageUtils;
import org.smartregister.chw.util.ReferralUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.util.LangUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import timber.log.Timber;

public class PncHomeVisitActivity extends BasePncHomeVisitActivity {

    public static void startMe(Activity activity, MemberObject memberObject, Boolean isEditMode) {
        Intent intent = new Intent(activity, PncHomeVisitActivity.class);
        intent.putExtra(org.smartregister.chw.anc.util.Constants.ANC_MEMBER_OBJECTS.MEMBER_PROFILE_OBJECT, memberObject);
        intent.putExtra(org.smartregister.chw.anc.util.Constants.ANC_MEMBER_OBJECTS.EDIT_MODE, isEditMode);
        activity.startActivityForResult(intent, org.smartregister.chw.anc.util.Constants.REQUEST_CODE_HOME_VISIT);
    }

    @Override
    protected void registerPresenter() {
        presenter = new BaseAncHomeVisitPresenter(memberObject, this, new PncHomeVisitInteractor(this));
    }

    @Override
    public void submittedAndClose() {
        // recompute schedule
        Runnable runnable = () -> ChwScheduleTaskExecutor.getInstance().execute(memberObject.getBaseEntityId(), CoreConstants.EventType.PNC_HOME_VISIT, new Date());
        org.smartregister.chw.util.Utils.startAsyncTask(new RunnableTask(runnable), null);
        super.submittedAndClose();

    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Form form = new Form();
        form.setActionBarBackground(R.color.family_actionbar);
        form.setWizard(false);

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
    public void submitVisit() {
        super.submitVisit();

        String minorAilmentBabyString = this.getString(R.string.child_minor_illness);
        String minorAilmentMotherString = this.getString(R.string.pnc_minor_ailment_mama);

        BaseAncHomeVisitAction motherMEAction;
        BaseAncHomeVisitAction babyMEAction;

        Map<String, BaseAncHomeVisitAction> actions = this.getAncHomeVisitActions();
        if (actions != null){
            for (Map.Entry<String, BaseAncHomeVisitAction> entry : actions.entrySet()){
                String key = entry.getKey();
                BaseAncHomeVisitAction value = entry.getValue();

                if (key.contains(minorAilmentMotherString)){
                    motherMEAction = value;
                    if (motherMEAction != null){
                        String meForm = motherMEAction.getJsonPayload();
                        if (meForm != null){
                            try {
                                JSONObject minorAilmentObject = new JSONObject(meForm);
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
                                LinkageUtils.addLinkageDetails(event, org.smartregister.chw.util.Constants.AddoLinkage.PNC_TASK_FOCUS, minorAilments);
                                NCUtils.processEvent(event.getBaseEntityId(), new JSONObject(org.smartregister.chw.anc.util.JsonFormUtils.gson.toJson(event)));
                                //LinkageUtils.processEvent(ReferralLibrary.getInstance(), event);

                                //Create linkage task
                                ReferralUtils.createLinkageTask(org.smartregister.Context.getInstance().allSharedPreferences(),
                                        memberObject.getBaseEntityId(), event.getFormSubmissionId(), minorAilments, org.smartregister.chw.util.Constants.AddoLinkage.PNC_TASK_FOCUS);

                                Toast.makeText(getContext(), getContext().getString(R.string.linked_to_addo_message), Toast.LENGTH_LONG).show();
                            }catch (Exception e){
                                Timber.e(e);
                            }
                        }
                    }
                }

                if (key.contains(minorAilmentBabyString)){
                    babyMEAction = value;
                    if (babyMEAction != null){
                        String meForm = babyMEAction.getJsonPayload();
                        if (meForm != null){
                            try {
                                JSONObject minorAilmentObject = new JSONObject(meForm);
                                String minorAilments = org.smartregister.chw.util.JsonFormUtils.getCheckBoxValue(minorAilmentObject, "child_minor_ailment").toLowerCase();
                                ReferralUtils.createLinkageTask(org.smartregister.Context.getInstance().allSharedPreferences(),
                                        memberObject.getBaseEntityId(), generateRandomUUIDString(), minorAilments, org.smartregister.chw.util.Constants.AddoLinkage.CHILD_TASK_FOCUS);
                                Toast.makeText(getContext(), getContext().getString(org.smartregister.chw.R.string.linked_to_addo_message), Toast.LENGTH_LONG).show();
                            }
                            catch (Exception e){

                            }
                        }
                    }
                }
            }
        }

    }

    @Override
    public void initializeActions(LinkedHashMap<String, BaseAncHomeVisitAction> map) {
        actionList.clear();
        actionList.putAll(map);

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        displayProgressBar(false);
    }
}
