package org.smartregister.chw.activity;

import static org.smartregister.util.JsonFormUtils.createEvent;
import static org.smartregister.util.JsonFormUtils.generateRandomUUIDString;

import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.Context;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.presenter.BaseAncHomeVisitPresenter;
import org.smartregister.chw.anc.util.AppExecutors;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.core.activity.CoreChildHomeVisitActivity;
import org.smartregister.chw.core.interactor.CoreChildHomeVisitInteractor;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.interactor.ChildHomeVisitInteractorFlv;
import org.smartregister.chw.referral.ReferralLibrary;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.chw.util.LinkageUtils;
import org.smartregister.chw.util.ReferralUtils;
import org.smartregister.clientandeventmodel.Event;

import java.util.LinkedHashMap;
import java.util.Map;

import timber.log.Timber;

public class ChildHomeVisitActivity extends CoreChildHomeVisitActivity {
    @Override
    protected void registerPresenter() {
        presenter = new BaseAncHomeVisitPresenter(memberObject, this, new CoreChildHomeVisitInteractor(new ChildHomeVisitInteractorFlv()));
    }

    @Override
    public void submittedAndClose() {
        super.submittedAndClose();
        ChildProfileActivity.startMe(this, memberObject, ChildProfileActivity.class);
    }

    @Override
    public void submitVisit() {
        super.submitVisit();

        Map<String, BaseAncHomeVisitAction> actions = this.getAncHomeVisitActions();
        if (actions !=  null) {

            BaseAncHomeVisitAction facilitySelectionAction = actions.get(this.getString(R.string.home_visit_facility_referral));
            if (facilitySelectionAction != null){
                String facilitySelectionForm = facilitySelectionAction.getJsonPayload();
                BaseAncHomeVisitAction dangerSignsActions = actions.get(this.getString(R.string.child_danger_signs_baby));
                try {

                    assert dangerSignsActions != null;
                    String dangerSignsForm = dangerSignsActions.getJsonPayload();
                    assert dangerSignsForm != null;
                    JSONObject dangerSignsJsonObject = new JSONObject(dangerSignsForm);

                    String referralProblems = JsonFormUtils.getCheckBoxValue(dangerSignsJsonObject, "toddler_danger_signs_present");
                    ReferralUtils.processReferral(facilitySelectionForm, this.memberObject.getBaseEntityId(), CoreConstants.TASKS_FOCUS.SICK_CHILD, referralProblems);
                    Toast.makeText(this, R.string.referral_submitted, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Timber.e(e);
                }
            }

            String childMinorAilmentString = this.getResources().getString(R.string.child_minor_ailments);
            String enChildAilments = "Child Minor Ailments";
            String swChildAilments = "Maradhi ya mtoto";

            BaseAncHomeVisitAction minorAilmentAction;

            //Check for Child Minor Ailment action
            for (Map.Entry<String, BaseAncHomeVisitAction> entry : actions.entrySet()){
                String key = entry.getKey();
                BaseAncHomeVisitAction value = entry.getValue();
                if (key.contains(enChildAilments) || key.contains(swChildAilments)){
                    minorAilmentAction = value;
                    String childMinorAilmentForm = minorAilmentAction.getJsonPayload();
                    if (childMinorAilmentForm != null){
                        try {
                            JSONObject minorAilmentObject = new JSONObject(childMinorAilmentForm);
                            String childAilments = JsonFormUtils.getCheckBoxValue(minorAilmentObject, "child_minor_ailment").toLowerCase();

                            //Get fields from json object
                            JSONArray fields = org.smartregister.util.JsonFormUtils.fields(minorAilmentObject);
                            JSONObject metadata = org.smartregister.util.JsonFormUtils.getJSONObject(minorAilmentObject, "metadata");
                            String bindType = org.smartregister.chw.referral.util.Constants.Tables.REFERRAL;
                            String enconterType = org.smartregister.chw.referral.util.Constants.EventType.REGISTRATION;
                            String baseEntityId = memberObject.getBaseEntityId();

                            ReferralLibrary referralLibrary = ReferralLibrary.getInstance();

                            //Create and process event
                            Event event = createEvent(fields, metadata, LinkageUtils.getFormTag(referralLibrary), baseEntityId, enconterType, bindType);
                            LinkageUtils.addLinkageDetails(event, Constants.AddoLinkage.CHILD_TASK_FOCUS, childAilments);
                            NCUtils.processEvent(event.getBaseEntityId(), new JSONObject(org.smartregister.chw.anc.util.JsonFormUtils.gson.toJson(event)));
                            //LinkageUtils.processEvent(ReferralLibrary.getInstance(), event);

                            //Create task
                            ReferralUtils.createLinkageTask(Context.getInstance().allSharedPreferences(),
                                    memberObject.getBaseEntityId(), event.getFormSubmissionId(), childAilments, Constants.AddoLinkage.CHILD_TASK_FOCUS);

                            Toast.makeText(getContext(), getContext().getString(R.string.linked_to_addo_message), Toast.LENGTH_LONG).show();
                        }catch (Exception e){
                            Timber.e(e);
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
