package org.smartregister.chw.actionhelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.smartregister.chw.harmreduction.model.BaseHarmReductionVisitAction;

public class HarmReductionMatClientsFollowupActionHelperTest {

    @Test
    public void evaluateStatusOnPayloadReturnsPendingWhenNothingIsSelected() throws Exception {
        HarmReductionMatClientsFollowupActionHelper actionHelper = new HarmReductionMatClientsFollowupActionHelper();

        actionHelper.onPayloadReceived(buildPayload().toString());

        Assert.assertEquals(BaseHarmReductionVisitAction.Status.PENDING, actionHelper.evaluateStatusOnPayload());
    }

    @Test
    public void evaluateStatusOnPayloadReturnsCompletedWhenCombinedValueIsPresent() throws Exception {
        HarmReductionMatClientsFollowupActionHelper actionHelper = new HarmReductionMatClientsFollowupActionHelper();
        JSONObject payload = buildPayload();
        payload.getJSONObject("step1").getJSONArray("fields").getJSONObject(0).put("value", "tuberculosis");

        actionHelper.onPayloadReceived(payload.toString());

        Assert.assertEquals(BaseHarmReductionVisitAction.Status.COMPLETED, actionHelper.evaluateStatusOnPayload());
    }

    @Test
    public void evaluateStatusOnPayloadReturnsCompletedWhenCheckboxOptionIsSelected() throws Exception {
        HarmReductionMatClientsFollowupActionHelper actionHelper = new HarmReductionMatClientsFollowupActionHelper();
        JSONObject payload = buildPayload();
        payload.getJSONObject("step1")
                .getJSONArray("fields")
                .getJSONObject(0)
                .getJSONArray("options")
                .getJSONObject(0)
                .put("value", true);

        actionHelper.onPayloadReceived(payload.toString());

        Assert.assertEquals(BaseHarmReductionVisitAction.Status.COMPLETED, actionHelper.evaluateStatusOnPayload());
    }

    private JSONObject buildPayload() throws Exception {
        JSONObject option = new JSONObject()
                .put("key", "tuberculosis")
                .put("text", "Tuberculosis");

        JSONObject field = new JSONObject()
                .put("key", "health_education_provided")
                .put("type", "check_box")
                .put("options", new JSONArray().put(option));

        JSONObject stepOne = new JSONObject().put("fields", new JSONArray().put(field));

        return new JSONObject().put("step1", stepOne);
    }
}
