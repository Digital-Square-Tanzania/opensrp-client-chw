package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.core.activity.CoreFamilyRemoveMemberActivity;
import org.smartregister.chw.fragment.FamilyRemoveMemberFragment;
import org.smartregister.family.util.JsonFormUtils;

import timber.log.Timber;

public class FamilyRemoveMemberActivity extends CoreFamilyRemoveMemberActivity {
    @Override
    protected void setRemoveMemberFragment() {
        this.removeMemberFragment = FamilyRemoveMemberFragment.newInstance(getIntent().getExtras());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            try {
                String json = data.getStringExtra("json");
                if (!TextUtils.isEmpty(json)) {
                    JSONObject form = new JSONObject(json);
                    if ("Family Registration".equalsIgnoreCase(form.optString(org.smartregister.chw.util.JsonFormUtils.ENCOUNTER_TYPE, ""))) {
                        JSONObject stepTwo = form.optJSONObject(JsonFormUtils.STEP2);
                        if (stepTwo != null) {
                            JSONArray stepTwoFields = stepTwo.optJSONArray(JsonFormConstants.FIELDS);
                            if (stepTwoFields != null) {
                                JSONObject existingHead = JsonFormUtils.getFieldJSONObject(stepTwoFields, "existing_head");
                                String headId = existingHead != null ? existingHead.optString(JsonFormConstants.VALUE) : "";
                                if (!TextUtils.isEmpty(headId)) {
                                    // Ensure Step 1 has the family_head set
                                    JSONObject stepOne = form.optJSONObject(JsonFormUtils.STEP1);
                                    if (stepOne != null) {
                                        JSONArray stepOneFields = stepOne.optJSONArray(JsonFormConstants.FIELDS);
                                        if (stepOneFields != null) {
                                            JsonFormUtils.getFieldJSONObject(stepOneFields, "family_head");
                                            // set value if field exists (assets were updated to include it)
                                            try {
                                                JSONObject fh = JsonFormUtils.getFieldJSONObject(stepOneFields, "family_head");
                                                if (fh != null) fh.put(JsonFormConstants.VALUE, headId);
                                            } catch (Exception ignore) { }
                                        }
                                    }
                                    // Drop Step 2 to prevent head-person creation and set count to 1
                                    form.remove(JsonFormUtils.STEP2);
                                    form.put("count", "1");
                                    data.putExtra("json", form.toString());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Timber.w(e);
            }
        }

        if (removeMemberFragment.getReasonForRemove() != null && data != null) {
            data.putExtra("reasonForRemove", removeMemberFragment.getReasonForRemove());
        }

        if (data != null) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
