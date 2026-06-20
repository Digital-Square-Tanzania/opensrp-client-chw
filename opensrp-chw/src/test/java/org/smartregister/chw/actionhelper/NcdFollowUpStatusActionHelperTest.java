package org.smartregister.chw.actionhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.smartregister.chw.ncd.model.BaseNcdVisitAction;

public class NcdFollowUpStatusActionHelperTest {

    @Test
    public void activeStatusIsComplete() {
        NcdFollowUpStatusActionHelper helper = helperWith(
                NcdFollowUpStatusActionHelper.STATUS_ACTIVE, null, null);

        assertEquals(BaseNcdVisitAction.Status.COMPLETED, helper.evaluateStatusOnPayload());
        assertTrue(helper.isActive());
        assertFalse(helper.isDeceased());
    }

    @Test
    public void inactiveStatusRequiresReason() {
        NcdFollowUpStatusActionHelper helper = helperWith(
                NcdFollowUpStatusActionHelper.STATUS_INACTIVE, null, null);

        assertEquals(BaseNcdVisitAction.Status.PENDING, helper.evaluateStatusOnPayload());
    }

    @Test
    public void otherReasonRequiresSpecification() {
        NcdFollowUpStatusActionHelper missingSpecification = helperWith(
                NcdFollowUpStatusActionHelper.STATUS_INACTIVE,
                NcdFollowUpStatusActionHelper.REASON_OTHER,
                null);
        NcdFollowUpStatusActionHelper complete = helperWith(
                NcdFollowUpStatusActionHelper.STATUS_INACTIVE,
                NcdFollowUpStatusActionHelper.REASON_OTHER,
                "Receiving care elsewhere");

        assertEquals(BaseNcdVisitAction.Status.PENDING,
                missingSpecification.evaluateStatusOnPayload());
        assertEquals(BaseNcdVisitAction.Status.COMPLETED,
                complete.evaluateStatusOnPayload());
    }

    @Test
    public void deceasedReasonIsDetected() {
        NcdFollowUpStatusActionHelper helper = helperWith(
                NcdFollowUpStatusActionHelper.STATUS_INACTIVE,
                NcdFollowUpStatusActionHelper.REASON_DECEASED,
                null,
                "2026-06-01");

        assertEquals(BaseNcdVisitAction.Status.COMPLETED, helper.evaluateStatusOnPayload());
        assertTrue(helper.isDeceased());
    }

    @Test
    public void deceasedReasonRequiresDateOfDeath() {
        NcdFollowUpStatusActionHelper helper = helperWith(
                NcdFollowUpStatusActionHelper.STATUS_INACTIVE,
                NcdFollowUpStatusActionHelper.REASON_DECEASED,
                null);

        assertEquals(BaseNcdVisitAction.Status.PENDING, helper.evaluateStatusOnPayload());
    }

    @Test
    public void emptyPayloadRemainsPending() {
        NcdFollowUpStatusActionHelper helper = new NcdFollowUpStatusActionHelper();

        assertEquals(BaseNcdVisitAction.Status.PENDING, helper.evaluateStatusOnPayload());
        assertFalse(helper.isActive());
        assertFalse(helper.isDeceased());
    }

    @Test
    public void activeStatusClearsInactiveReasonValues() {
        String payload = "{\"step1\":{\"fields\":["
                + field(NcdFollowUpStatusActionHelper.KEY_STATUS,
                NcdFollowUpStatusActionHelper.STATUS_ACTIVE) + ","
                + field(NcdFollowUpStatusActionHelper.KEY_REASON,
                NcdFollowUpStatusActionHelper.REASON_OTHER) + ","
                + field(NcdFollowUpStatusActionHelper.KEY_OTHER_REASON, "Old reason") + ","
                + field(NcdFollowUpStatusActionHelper.KEY_DATE_OF_DEATH, "2026-06-01")
                + "]}}";

        String sanitized = NcdFollowUpStatusActionHelper.clearIrrelevantValues(payload);

        assertEquals("", NcdFollowUpStatusActionHelper.extractValue(
                sanitized, NcdFollowUpStatusActionHelper.KEY_REASON));
        assertEquals("", NcdFollowUpStatusActionHelper.extractValue(
                sanitized, NcdFollowUpStatusActionHelper.KEY_OTHER_REASON));
        assertEquals("", NcdFollowUpStatusActionHelper.extractValue(
                sanitized, NcdFollowUpStatusActionHelper.KEY_DATE_OF_DEATH));
    }

    private NcdFollowUpStatusActionHelper helperWith(String status, String reason,
                                                       String otherReason) {
        return helperWith(status, reason, otherReason, null);
    }

    private NcdFollowUpStatusActionHelper helperWith(String status, String reason,
                                                       String otherReason, String dateOfDeath) {
        StringBuilder fields = new StringBuilder();
        fields.append(field(NcdFollowUpStatusActionHelper.KEY_STATUS, status));
        if (reason != null) {
            fields.append(',').append(field(NcdFollowUpStatusActionHelper.KEY_REASON, reason));
        }
        if (otherReason != null) {
            fields.append(',').append(field(
                    NcdFollowUpStatusActionHelper.KEY_OTHER_REASON, otherReason));
        }
        if (dateOfDeath != null) {
            fields.append(',').append(field(
                    NcdFollowUpStatusActionHelper.KEY_DATE_OF_DEATH, dateOfDeath));
        }

        NcdFollowUpStatusActionHelper helper = new NcdFollowUpStatusActionHelper();
        helper.onPayloadReceived("{\"step1\":{\"fields\":[" + fields + "]}}");
        return helper;
    }

    private String field(String key, String value) {
        return "{\"key\":\"" + key + "\",\"value\":\"" + value + "\"}";
    }
}
