package org.smartregister.chw.actionhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.smartregister.chw.ncd.model.BaseNcdVisitAction;

public class NcdFollowUpStatusActionHelperTest {

    @Test
    public void continuingStatusIsComplete() {
        NcdFollowUpStatusActionHelper helper = helperWith(
                NcdFollowUpStatusActionHelper.STATUS_CONTINUING, null);

        assertEquals(BaseNcdVisitAction.Status.COMPLETED, helper.evaluateStatusOnPayload());
        assertTrue(helper.isActive());
        assertFalse(helper.isDeceased());
    }

    @Test
    public void transferredStatusIsComplete() {
        NcdFollowUpStatusActionHelper helper = helperWith(
                NcdFollowUpStatusActionHelper.STATUS_TRANSFERRED, null);

        assertEquals(BaseNcdVisitAction.Status.COMPLETED, helper.evaluateStatusOnPayload());
        assertFalse(helper.isActive());
        assertFalse(helper.isDeceased());
    }

    @Test
    public void deadStatusIsDetectedWhenDateIsPresent() {
        NcdFollowUpStatusActionHelper helper = helperWith(
                NcdFollowUpStatusActionHelper.STATUS_DEAD, "2026-06-01");

        assertEquals(BaseNcdVisitAction.Status.COMPLETED, helper.evaluateStatusOnPayload());
        assertTrue(helper.isDeceased());
    }

    @Test
    public void deadStatusRequiresDateOfDeath() {
        NcdFollowUpStatusActionHelper helper = helperWith(
                NcdFollowUpStatusActionHelper.STATUS_DEAD, null);

        assertEquals(BaseNcdVisitAction.Status.PENDING, helper.evaluateStatusOnPayload());
        assertTrue(helper.isDeceased());
    }

    @Test
    public void emptyPayloadRemainsPending() {
        NcdFollowUpStatusActionHelper helper = new NcdFollowUpStatusActionHelper();

        assertEquals(BaseNcdVisitAction.Status.PENDING, helper.evaluateStatusOnPayload());
        assertFalse(helper.isActive());
        assertFalse(helper.isDeceased());
    }

    @Test
    public void nonDeadStatusClearsStaleDateOfDeath() {
        String payload = "{\"step1\":{\"fields\":["
                + field(NcdFollowUpStatusActionHelper.KEY_STATUS,
                NcdFollowUpStatusActionHelper.STATUS_TRANSFERRED) + ","
                + field(NcdFollowUpStatusActionHelper.KEY_DATE_OF_DEATH, "2026-06-01")
                + "]}}";

        String sanitized = NcdFollowUpStatusActionHelper.clearIrrelevantValues(payload);

        assertEquals("", NcdFollowUpStatusActionHelper.extractValue(
                sanitized, NcdFollowUpStatusActionHelper.KEY_DATE_OF_DEATH));
    }

    @Test
    public void deadStatusPreservesDateOfDeath() {
        String payload = "{\"step1\":{\"fields\":["
                + field(NcdFollowUpStatusActionHelper.KEY_STATUS,
                NcdFollowUpStatusActionHelper.STATUS_DEAD) + ","
                + field(NcdFollowUpStatusActionHelper.KEY_DATE_OF_DEATH, "2026-06-01")
                + "]}}";

        String sanitized = NcdFollowUpStatusActionHelper.clearIrrelevantValues(payload);

        assertEquals("2026-06-01", NcdFollowUpStatusActionHelper.extractValue(
                sanitized, NcdFollowUpStatusActionHelper.KEY_DATE_OF_DEATH));
    }

    private NcdFollowUpStatusActionHelper helperWith(String status, String dateOfDeath) {
        StringBuilder fields = new StringBuilder();
        fields.append(field(NcdFollowUpStatusActionHelper.KEY_STATUS, status));
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
