package org.smartregister.chw.activity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.tbleprosy.domain.MemberObject;

import java.util.Date;
import java.util.List;

public class TbLeprosyProfileActivityTest {

    @Test
    public void shouldRemoveTreatmentDecisionAlgorithmForClientsAgedTenOrAbove() throws Exception {
        JSONObject form = buildObservationResultsForm();

        TbLeprosyProfileActivity.maybeRemoveTreatmentDecisionAlgorithmOption(form, 10);

        JSONObject targetField = findField(form, "tb_preliminary_investigation_tests");
        JSONArray options = targetField.optJSONArray("options");
        Assert.assertNotNull(options);
        Assert.assertEquals(2, options.length());
        Assert.assertEquals("screening_questionnaire", options.optJSONObject(0).optString("key"));
        Assert.assertEquals("xray", options.optJSONObject(1).optString("key"));
        Assert.assertNull(findOption(options, "treatment_decision_algorithm"));
    }

    @Test
    public void shouldKeepTreatmentDecisionAlgorithmForClientsYoungerThanTen() throws Exception {
        JSONObject form = buildObservationResultsForm();

        TbLeprosyProfileActivity.maybeRemoveTreatmentDecisionAlgorithmOption(form, 9);

        JSONObject targetField = findField(form, "tb_preliminary_investigation_tests");
        JSONArray options = targetField.optJSONArray("options");
        Assert.assertNotNull(options);
        Assert.assertEquals(3, options.length());
        Assert.assertNotNull(findOption(options, "treatment_decision_algorithm"));
    }

    @Test
    public void shouldShowPendingReferralForTbPresumptiveWithoutObservationResults() {
        boolean shouldShow = TbLeprosyProfileActivity.shouldShowPendingReferralAction(
                true,
                false,
                true,
                false,
                false
        );

        Assert.assertTrue(shouldShow);
    }

    @Test
    public void shouldShowPendingReferralForLeprosyOnlyPresumptiveClientWithoutReferralTaskAfterVisit() {
        boolean shouldShow = TbLeprosyProfileActivity.shouldShowPendingReferralAction(
                false,
                true,
                true,
                true,
                false
        );

        Assert.assertTrue(shouldShow);
    }

    @Test
    public void shouldNotShowPendingReferralWhenReferralTaskExistsAfterVisit() {
        boolean shouldShow = TbLeprosyProfileActivity.shouldShowPendingReferralAction(
                true,
                false,
                true,
                false,
                true
        );

        Assert.assertFalse(shouldShow);
    }

    @Test
    public void shouldParseTbLeprosyVisitDate() {
        Date parsedDate = TbLeprosyProfileActivity.parseTbLeprosyVisitDate("2026-02-25 12:30:00");

        Assert.assertNotNull(parsedDate);
    }

    @Test
    public void shouldReturnNullWhenTbLeprosyVisitDateIsBlank() {
        Date parsedDate = TbLeprosyProfileActivity.parseTbLeprosyVisitDate("  ");

        Assert.assertNull(parsedDate);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void pendingReferralEditClickShouldLaunchTbLeprosyReferralForm() {
        TbLeprosyProfileActivity activity = Mockito.mock(TbLeprosyProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        MemberObject memberObject = Mockito.mock(MemberObject.class);

        Mockito.doReturn("base-id").when(memberObject).getBaseEntityId();
        Mockito.doReturn("TB/Leprosy Referral").when(activity).getString(R.string.tb_leprosy_referral);
        ReflectionHelpers.setField(activity, "memberObject", memberObject);

        ArgumentCaptor<List> referralTypesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> baseEntityIdCaptor = ArgumentCaptor.forClass(String.class);

        activity.getPendingReferralActionClickListener().onClick(null);

        Mockito.verify(activity).launchClientReferralActivity(referralTypesCaptor.capture(), baseEntityIdCaptor.capture());
        Assert.assertEquals("base-id", baseEntityIdCaptor.getValue());

        List<ReferralTypeModel> referralTypeModels = (List<ReferralTypeModel>) referralTypesCaptor.getValue();
        Assert.assertNotNull(referralTypeModels);
        Assert.assertEquals(1, referralTypeModels.size());
        Assert.assertEquals("TB/Leprosy Referral", referralTypeModels.get(0).getReferralType());
        Assert.assertEquals(CoreConstants.JSON_FORM.getTbLeprosyReferralForm(), referralTypeModels.get(0).getFormName());
        Assert.assertEquals(CoreConstants.TASKS_FOCUS.TBLEPROSY, referralTypeModels.get(0).getFocus());
    }

    private JSONObject buildObservationResultsForm() throws Exception {
        JSONObject form = new JSONObject();
        JSONObject stepOne = new JSONObject();
        JSONArray fields = new JSONArray();
        stepOne.put("fields", fields);
        form.put("step1", stepOne);

        JSONObject targetField = new JSONObject();
        targetField.put("key", "tb_preliminary_investigation_tests");

        JSONArray options = new JSONArray();
        options.put(buildOption("screening_questionnaire"));
        options.put(buildOption("xray"));
        options.put(buildOption("treatment_decision_algorithm"));
        targetField.put("options", options);

        fields.put(targetField);
        fields.put(new JSONObject().put("key", "another_field"));
        return form;
    }

    private JSONObject buildOption(String key) throws Exception {
        return new JSONObject().put("key", key).put("text", key + "_text");
    }

    private JSONObject findField(JSONObject form, String key) {
        JSONObject stepOne = form.optJSONObject("step1");
        if (stepOne == null) {
            return null;
        }

        JSONArray fields = stepOne.optJSONArray("fields");
        if (fields == null) {
            return null;
        }

        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field != null && key.equals(field.optString("key"))) {
                return field;
            }
        }
        return null;
    }

    private JSONObject findOption(JSONArray options, String key) {
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (option != null && key.equals(option.optString("key"))) {
                return option;
            }
        }
        return null;
    }
}
