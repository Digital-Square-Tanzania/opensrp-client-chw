package org.smartregister.chw.activity;

import android.content.Intent;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.harmreduction.util.Constants;

public class HarmReductionRegisterActivityTest extends BaseUnitTest {

    @Test
    public void shouldLaunchPreMatSessionReturnsTrueForRiskAssessmentWithYesAnswer() throws Exception {
        JSONObject form = new JSONObject()
                .put(Constants.JSON_FORM_EXTRA.ENCOUNTER_TYPE, Constants.EVENT_TYPE.HARM_REDUCTION_RISK_ASSESSMENT)
                .put("step1", new JSONObject().put("fields", new org.json.JSONArray()
                        .put(new JSONObject()
                                .put("key", "roc_mat_pre_session")
                                .put("value", "yes")
                        )));

        Assert.assertTrue(HarmReductionRegisterActivity.shouldLaunchPreMatSession(form));
    }

    @Test
    public void shouldLaunchPreMatSessionReturnsFalseWhenPreMatAnswerIsNo() throws Exception {
        JSONObject form = new JSONObject()
                .put(Constants.JSON_FORM_EXTRA.ENCOUNTER_TYPE, Constants.EVENT_TYPE.HARM_REDUCTION_RISK_ASSESSMENT)
                .put("step1", new JSONObject().put("fields", new org.json.JSONArray()
                        .put(new JSONObject()
                                .put("key", "roc_mat_pre_session")
                                .put("value", "no")
                        )));

        Assert.assertFalse(HarmReductionRegisterActivity.shouldLaunchPreMatSession(form));
    }

    @Test
    public void resolveRegistrationFormNameReturnsExistingClientFormForExistingClientChoice() {
        Assert.assertEquals(HarmReductionRegisterActivity.HARM_REDUCTION_REGISTER_EXISTING_CLIENT_FORM,
                HarmReductionRegisterActivity.resolveRegistrationFormName(1));
    }

    @Test
    public void shouldShowRegistrationChooserReturnsTrueWhenLaunchFlagIsSet() {
        Intent intent = new Intent().putExtra(HarmReductionRegisterActivity.EXTRA_SHOW_REGISTRATION_CHOOSER, true);

        Assert.assertTrue(HarmReductionRegisterActivity.shouldShowRegistrationChooser(intent));
    }
}
