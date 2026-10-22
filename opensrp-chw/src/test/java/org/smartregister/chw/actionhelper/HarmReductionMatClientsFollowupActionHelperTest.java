package org.smartregister.chw.actionhelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.smartregister.chw.harmreduction.model.BaseHarmReductionVisitAction;

public class HarmReductionMatClientsFollowupActionHelperTest {

    @Test
    public void unansweredTreatmentStatusRemainsPending() throws Exception {
        assertStatus(buildPayload(""), BaseHarmReductionVisitAction.Status.PENDING);
    }

    @Test
    public void eachMethadoneTreatmentAnswerCompletesTheAction() throws Exception {
        for (String answer : new String[]{"continuing_methadone_treatment",
                "stopped_using_methadone", "completed_methadone_treatment"}) {
            assertStatus(buildPayload(answer), BaseHarmReductionVisitAction.Status.COMPLETED);
        }
    }

    @Test
    public void legacyPayloadWithoutStepCountStillReadsTreatmentStatus() throws Exception {
        JSONObject payload = buildPayload("continuing_methadone_treatment");
        payload.remove("count");
        assertStatus(payload, BaseHarmReductionVisitAction.Status.COMPLETED);
    }

    @Test
    public void unrelatedHealthEducationCannotCompleteTreatmentStatus() throws Exception {
        JSONObject field = new JSONObject().put("key", "health_education_provided")
                .put("type", "check_box").put("value", "tuberculosis")
                .put("options", new JSONArray().put(new JSONObject()
                        .put("key", "tuberculosis").put("text", "Tuberculosis").put("value", true)));
        JSONObject payload = new JSONObject().put("count", "1").put("step1",
                new JSONObject().put("fields", new JSONArray().put(field)));
        assertStatus(payload, BaseHarmReductionVisitAction.Status.PENDING);
    }

    @Test
    public void invalidOrClearedPayloadDoesNotRetainPreviousCompletion() throws Exception {
        for (String cleared : new String[]{"", null, "invalid json", "{}", buildPayload("").toString()}) {
            HarmReductionMatClientsFollowupActionHelper helper = new HarmReductionMatClientsFollowupActionHelper();
            helper.onPayloadReceived(buildPayload("continuing_methadone_treatment").toString());
            Assert.assertEquals(BaseHarmReductionVisitAction.Status.COMPLETED, helper.evaluateStatusOnPayload());
            helper.onPayloadReceived(cleared);
            Assert.assertEquals(BaseHarmReductionVisitAction.Status.PENDING, helper.evaluateStatusOnPayload());
        }
    }

    private void assertStatus(JSONObject payload, BaseHarmReductionVisitAction.Status expected) {
        HarmReductionMatClientsFollowupActionHelper helper = new HarmReductionMatClientsFollowupActionHelper();
        helper.onPayloadReceived(payload.toString());
        Assert.assertEquals(expected, helper.evaluateStatusOnPayload());
    }

    private JSONObject buildPayload(String answer) throws Exception {
        JSONObject field = new JSONObject().put("key", "methadone_treatment_status")
                .put("type", "native_radio").put("value", answer);
        return new JSONObject().put("count", "1").put("step1",
                new JSONObject().put("fields", new JSONArray().put(field)));
    }
}
