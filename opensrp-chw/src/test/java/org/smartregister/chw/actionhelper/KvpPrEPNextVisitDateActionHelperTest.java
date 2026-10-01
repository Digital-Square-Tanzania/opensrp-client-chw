package org.smartregister.chw.actionhelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;

import static org.junit.Assert.assertEquals;

public class KvpPrEPNextVisitDateActionHelperTest {

    @Test
    public void actionRemainsPendingUntilDateIsProvided() throws Exception {
        KvpPrEPNextVisitDateActionHelper helper = new KvpPrEPNextVisitDateActionHelper();

        helper.onPayloadReceived(payload(""));
        assertEquals(BaseKvpVisitAction.Status.PENDING, helper.evaluateStatusOnPayload());

        helper.onPayloadReceived(payload("2026-08-20"));
        assertEquals(BaseKvpVisitAction.Status.COMPLETED, helper.evaluateStatusOnPayload());
    }

    private String payload(String value) throws Exception {
        JSONObject field = new JSONObject()
                .put("key", "next_visit_date")
                .put("value", value);
        return new JSONObject()
                .put("count", "1")
                .put("step1", new JSONObject().put("fields", new JSONArray().put(field)))
                .toString();
    }
}
