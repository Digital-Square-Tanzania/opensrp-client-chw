package org.smartregister.chw.activity;

import static org.smartregister.util.JsonFormUtils.ENTITY_ID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONObject;
import org.smartregister.chw.ayp.activity.BaseAypGroupProfileActivity;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.util.AypVisitsUtil;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;

public class AypInSchoolGroupProfileActivity extends BaseAypGroupProfileActivity {

    public static void start(Context context, String groupId, String groupName) {
        Intent intent = new Intent(context, AypInSchoolGroupProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.GROUP_ID, groupId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.GROUP_NAME, groupName);
        context.startActivity(intent);
    }

    @Override
    public void openGroupDetailsForm() {
        try {
            JSONObject form = new FormUtils().getFormJsonFromRepositoryOrAssets(this, "ayp_in_school_group_creation");
            if (form != null) {
                // Use group id as entity id for this event
                String groupId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.GROUP_ID);
                if (groupId == null) groupId = java.util.UUID.randomUUID().toString();
                form.put(ENTITY_ID, groupId);

                // Configure form (non-wizard)
                Form cfg = new Form();
                cfg.setWizard(false);
                Intent intent = new Intent(this, org.smartregister.view.activity.FormActivity.class);
                intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, form.toString());
                intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, cfg);
                startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onAddMember() {
        // Optional: navigate to a member enrollment flow; left as no-op for now
    }

    @Override
    public void openMemberProfile(MemberObject member) {
        AypInSchoolMemberProfileActivity.startProfileActivity(this, member.getBaseEntityId());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE_GET_JSON && resultCode == Activity.RESULT_OK) {
            try {
                String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
                if (jsonString == null) jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                if (jsonString == null) return;

                // Convert form JSON to Event and process via AYP visit pipeline
                AllSharedPreferences prefs = Utils.getAllSharedPreferences();
                Event baseEvent = org.smartregister.chw.ayp.util.JsonFormUtils.processJsonForm(prefs, jsonString, org.smartregister.chw.ayp.util.Constants.TABLES.AYP_IN_SCHOOL_GROUP_DETAILS);
                if (baseEvent != null) {
                    org.smartregister.chw.ayp.domain.Visit visit = new org.smartregister.chw.ayp.domain.Visit();
                    visit.setVisitId(java.util.UUID.randomUUID().toString());
                    visit.setVisitType(org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_GROUP_DETAILS);
                    visit.setBaseEntityId(baseEvent.getBaseEntityId());
                    java.util.Date now = new java.util.Date();
                    visit.setDate(now);
                    visit.setUpdatedAt(now);
                    visit.setProcessed(false);
                    visit.setPreProcessedJson(new Gson().toJson(baseEvent));

                    org.smartregister.chw.ayp.AypLibrary.getInstance().visitRepository().addVisit(visit);
                    AypVisitsUtil.manualProcessVisit(visit);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
