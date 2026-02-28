package org.smartregister.chw.activity;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.tbleprosy.dao.TbLeprosyDao;
import org.smartregister.chw.tbleprosy.domain.MemberObject;
import org.smartregister.domain.Task;
import org.smartregister.family.util.JsonFormUtils;

import java.util.Date;

public class TbLeprosyProfileActivityTest extends BaseUnitTest {

    @Test
    public void onResumeShouldHandleDeceasedClient() {
        TbLeprosyProfileActivity activity = Mockito.mock(TbLeprosyProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        MemberObject memberObject = Mockito.mock(MemberObject.class);
        Mockito.doReturn("base-id").when(memberObject).getBaseEntityId();
        ReflectionHelpers.setField(activity, "memberObject", memberObject);

        TextView recordTbLeprosyButton = Mockito.mock(TextView.class);
        TextView recordLeprosyTreatmentStartDateButton = Mockito.mock(TextView.class);
        ReflectionHelpers.setField(activity, "textViewRecordTbLeprosy", recordTbLeprosyButton);
        ReflectionHelpers.setField(activity, "textViewRecordLeprosyTreatmentStartDate", recordLeprosyTreatmentStartDateButton);

        try (MockedStatic<TbLeprosyDao> tbLeprosyDaoStatic = Mockito.mockStatic(TbLeprosyDao.class)) {
            tbLeprosyDaoStatic.when(() -> TbLeprosyDao.isClientDeceased("base-id")).thenReturn(true);

            activity.applyTbLeprosyDeceasedHandling();

            tbLeprosyDaoStatic.verify(() -> TbLeprosyDao.isClientDeceased("base-id"));
            Mockito.verify(recordTbLeprosyButton).setVisibility(View.GONE);
            Mockito.verify(recordLeprosyTreatmentStartDateButton).setVisibility(View.GONE);
        }
    }

    @Test
    public void shouldHideRecordActionsWhenClientIsDeceased() {
        TbLeprosyProfileActivity activity = Mockito.mock(TbLeprosyProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        MemberObject memberObject = Mockito.mock(MemberObject.class);
        TextView recordTbLeprosyButton = Mockito.mock(TextView.class);
        TextView recordLeprosyTreatmentStartDateButton = Mockito.mock(TextView.class);
        Mockito.doReturn("base-id").when(memberObject).getBaseEntityId();

        ReflectionHelpers.setField(activity, "memberObject", memberObject);
        ReflectionHelpers.setField(activity, "textViewRecordTbLeprosy", recordTbLeprosyButton);
        ReflectionHelpers.setField(activity, "textViewRecordLeprosyTreatmentStartDate", recordLeprosyTreatmentStartDateButton);

        try (MockedStatic<TbLeprosyDao> tbLeprosyDaoStatic = Mockito.mockStatic(TbLeprosyDao.class)) {
            tbLeprosyDaoStatic.when(() -> TbLeprosyDao.isClientDeceased("base-id")).thenReturn(true);
            tbLeprosyDaoStatic.when(() -> TbLeprosyDao.getTbLeprosyClientStatus("base-id")).thenReturn("contact");

            activity.setupButtons();

            Mockito.verify(recordTbLeprosyButton).setVisibility(View.GONE);
            Mockito.verify(recordLeprosyTreatmentStartDateButton).setVisibility(View.GONE);
            tbLeprosyDaoStatic.verify(() -> TbLeprosyDao.isClientDeceased("base-id"));
        }
    }

    @Test
    public void shouldNotHideRecordActionsForNonDeceasedClient() {
        TbLeprosyProfileActivity activity = Mockito.mock(TbLeprosyProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        MemberObject memberObject = Mockito.mock(MemberObject.class);
        TextView recordTbLeprosyButton = Mockito.mock(TextView.class);
        TextView recordLeprosyTreatmentStartDateButton = Mockito.mock(TextView.class);
        Mockito.doReturn("base-id").when(memberObject).getBaseEntityId();

        ReflectionHelpers.setField(activity, "memberObject", memberObject);
        ReflectionHelpers.setField(activity, "textViewRecordTbLeprosy", recordTbLeprosyButton);
        ReflectionHelpers.setField(activity, "textViewRecordLeprosyTreatmentStartDate", recordLeprosyTreatmentStartDateButton);

        try (MockedStatic<TbLeprosyDao> tbLeprosyDaoStatic = Mockito.mockStatic(TbLeprosyDao.class)) {
            tbLeprosyDaoStatic.when(() -> TbLeprosyDao.isClientDeceased("base-id")).thenReturn(false);
            tbLeprosyDaoStatic.when(() -> TbLeprosyDao.getTbLeprosyClientStatus("base-id")).thenReturn("contact");
            tbLeprosyDaoStatic.when(() -> TbLeprosyDao.isClientTbOrLeprosyNegative("base-id")).thenReturn(false);
            tbLeprosyDaoStatic.when(() -> TbLeprosyDao.getTbLeprosyObservationResults("base-id")).thenReturn(null);

            activity.applyTbLeprosyDeceasedHandling();

            Mockito.verify(recordTbLeprosyButton, Mockito.never()).setVisibility(View.GONE);
            Mockito.verify(recordLeprosyTreatmentStartDateButton, Mockito.never()).setVisibility(View.GONE);
            tbLeprosyDaoStatic.verify(() -> TbLeprosyDao.isClientDeceased("base-id"));
        }
    }

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

    @Test
    public void hasReferralTaskAfterTbLeprosyVisitShouldPrioritizeTbDateForDualPresumptiveClient() {
        TbLeprosyProfileActivity activity = Mockito.mock(TbLeprosyProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        MemberObject memberObject = Mockito.mock(MemberObject.class);
        Task task = Mockito.mock(Task.class);
        Mockito.doReturn("base-id").when(memberObject).getBaseEntityId();
        Mockito.doReturn(CoreConstants.TASKS_FOCUS.TBLEPROSY).when(task).getFocus();
        Mockito.doReturn(new DateTime(2026, 2, 25, 10, 0)).when(task).getLastModified();
        ReflectionHelpers.setField(activity, "memberObject", memberObject);
        Mockito.doReturn(task).when(activity).getTaskByEntityId("base-id");

        try (MockedStatic<TbLeprosyDao> tbLeprosyDaoStatic = Mockito.mockStatic(TbLeprosyDao.class)) {
            tbLeprosyDaoStatic.when(() -> TbLeprosyDao.getLatestTbSampleCollectionDate("base-id"))
                    .thenReturn(new DateTime(2026, 2, 28, 8, 0).toDate());
            tbLeprosyDaoStatic.when(() -> TbLeprosyDao.getLatestTbLeprosyScreeningDate("base-id"))
                    .thenReturn(new DateTime(2026, 2, 20, 8, 0).toDate());

            boolean hasReferralTaskAfterVisit = ReflectionHelpers.callInstanceMethod(
                    activity,
                    "hasReferralTaskAfterTbLeprosyVisit",
                    ReflectionHelpers.ClassParameter.from(boolean.class, true),
                    ReflectionHelpers.ClassParameter.from(boolean.class, true)
            );

            Assert.assertFalse(hasReferralTaskAfterVisit);
        }
    }

    @Test
    public void hasReferralTaskAfterTbLeprosyVisitShouldUseLeprosyDateForLeprosyOnlyClient() {
        TbLeprosyProfileActivity activity = Mockito.mock(TbLeprosyProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        MemberObject memberObject = Mockito.mock(MemberObject.class);
        Task task = Mockito.mock(Task.class);
        Mockito.doReturn("base-id").when(memberObject).getBaseEntityId();
        Mockito.doReturn(CoreConstants.TASKS_FOCUS.TBLEPROSY).when(task).getFocus();
        Mockito.doReturn(new DateTime(2026, 2, 25, 10, 0)).when(task).getLastModified();
        ReflectionHelpers.setField(activity, "memberObject", memberObject);
        Mockito.doReturn(task).when(activity).getTaskByEntityId("base-id");

        try (MockedStatic<TbLeprosyDao> tbLeprosyDaoStatic = Mockito.mockStatic(TbLeprosyDao.class)) {
            tbLeprosyDaoStatic.when(() -> TbLeprosyDao.getLatestTbLeprosyScreeningDate("base-id"))
                    .thenReturn(new DateTime(2026, 2, 20, 8, 0).toDate());

            boolean hasReferralTaskAfterVisit = ReflectionHelpers.callInstanceMethod(
                    activity,
                    "hasReferralTaskAfterTbLeprosyVisit",
                    ReflectionHelpers.ClassParameter.from(boolean.class, false),
                    ReflectionHelpers.ClassParameter.from(boolean.class, true)
            );

            Assert.assertTrue(hasReferralTaskAfterVisit);
        }
    }

    @Test
    public void shouldReturnFalseForMissingReferralTaskAfterVisit() {
        boolean hasReferralTask = TbLeprosyProfileActivity.isTbLeprosyReferralTaskAfterVisit(
                null,
                new DateTime(2026, 2, 25, 10, 0).toDate()
        );

        Assert.assertFalse(hasReferralTask);
    }

    @Test
    public void shouldReturnFalseForReferralTaskWithDifferentFocus() {
        Task task = Mockito.mock(Task.class);
        Mockito.doReturn("HIV").when(task).getFocus();
        Mockito.doReturn(new DateTime(2026, 2, 26, 10, 0)).when(task).getLastModified();

        boolean hasReferralTask = TbLeprosyProfileActivity.isTbLeprosyReferralTaskAfterVisit(
                task,
                new DateTime(2026, 2, 25, 10, 0).toDate()
        );

        Assert.assertFalse(hasReferralTask);
    }

    @Test
    public void shouldReturnFalseWhenReferralTaskIsBeforeVisitDate() {
        Task task = Mockito.mock(Task.class);
        Mockito.doReturn(CoreConstants.TASKS_FOCUS.TBLEPROSY).when(task).getFocus();
        Mockito.doReturn(new DateTime(2026, 2, 24, 10, 0)).when(task).getLastModified();

        boolean hasReferralTask = TbLeprosyProfileActivity.isTbLeprosyReferralTaskAfterVisit(
                task,
                new DateTime(2026, 2, 25, 10, 0).toDate()
        );

        Assert.assertFalse(hasReferralTask);
    }

    @Test
    public void shouldReturnTrueWhenReferralTaskIsAfterVisitDate() {
        Task task = Mockito.mock(Task.class);
        Mockito.doReturn(CoreConstants.TASKS_FOCUS.TBLEPROSY).when(task).getFocus();
        Mockito.doReturn(new DateTime(2026, 2, 26, 10, 0)).when(task).getLastModified();

        boolean hasReferralTask = TbLeprosyProfileActivity.isTbLeprosyReferralTaskAfterVisit(
                task,
                new DateTime(2026, 2, 25, 10, 0).toDate()
        );

        Assert.assertTrue(hasReferralTask);
    }

    @Test
    public void shouldReturnTrueWhenReferralTaskIsOnSameDateAsVisitDate() {
        Task task = Mockito.mock(Task.class);
        Mockito.doReturn(CoreConstants.TASKS_FOCUS.TBLEPROSY).when(task).getFocus();
        Mockito.doReturn(new DateTime(2026, 2, 25, 1, 0)).when(task).getLastModified();

        boolean hasReferralTask = TbLeprosyProfileActivity.isTbLeprosyReferralTaskAfterVisit(
                task,
                new DateTime(2026, 2, 25, 23, 59).toDate()
        );

        Assert.assertTrue(hasReferralTask);
    }

    @Test
    public void pendingReferralEditClickShouldLaunchTbLeprosyReferralForm() throws Exception {
        TbLeprosyProfileActivity activity = Mockito.mock(TbLeprosyProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        MemberObject memberObject = Mockito.mock(MemberObject.class);
        org.smartregister.util.FormUtils formUtils = Mockito.mock(org.smartregister.util.FormUtils.class);

        Mockito.doReturn("base-id").when(memberObject).getBaseEntityId();
        ReflectionHelpers.setField(activity, "memberObject", memberObject);

        JSONObject referralFormJson = new JSONObject().put("encounter_type", "Referral Registration");
        Mockito.doReturn(referralFormJson).when(formUtils)
                .getFormJson(CoreConstants.JSON_FORM.getTbLeprosyReferralForm());

        ArgumentCaptor<JSONObject> referralFormJsonCaptor = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<String> baseEntityIdCaptor = ArgumentCaptor.forClass(String.class);

        try (MockedStatic<FormUtils> formUtilsStatic = Mockito.mockStatic(FormUtils.class)) {
            formUtilsStatic.when(FormUtils::getFormUtils).thenReturn(formUtils);
            activity.getPendingReferralActionClickListener().onClick(null);
        }

        Mockito.verify(activity).startPendingReferralFormActivity(baseEntityIdCaptor.capture(), referralFormJsonCaptor.capture());
        Assert.assertEquals("base-id", baseEntityIdCaptor.getValue());
        Assert.assertEquals(
                CoreConstants.TASKS_FOCUS.TBLEPROSY,
                referralFormJsonCaptor.getValue().optString(org.smartregister.chw.util.Constants.REFERRAL_TASK_FOCUS)
        );
        Mockito.verify(formUtils).getFormJson(CoreConstants.JSON_FORM.getTbLeprosyReferralForm());
    }

    @Test
    public void handlePendingReferralFormResultShouldRefreshAfterSuccess() {
        TbLeprosyProfileActivity activity = Mockito.mock(TbLeprosyProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doNothing().when(activity).refreshAfterReferralSubmission();
        ReflectionHelpers.setField(activity, "pendingTbLeprosyReferralLaunch", true);

        activity.handlePendingReferralFormResult(Activity.RESULT_OK);

        Mockito.verify(activity).refreshAfterReferralSubmission();
        Assert.assertFalse(ReflectionHelpers.getField(activity, "pendingTbLeprosyReferralLaunch"));
    }

    @Test
    public void handlePendingReferralFormResultShouldNotRefreshAfterCancel() {
        TbLeprosyProfileActivity activity = Mockito.mock(TbLeprosyProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doNothing().when(activity).refreshAfterReferralSubmission();
        ReflectionHelpers.setField(activity, "pendingTbLeprosyReferralLaunch", true);

        activity.handlePendingReferralFormResult(Activity.RESULT_CANCELED);

        Mockito.verify(activity, Mockito.never()).refreshAfterReferralSubmission();
        Assert.assertFalse(ReflectionHelpers.getField(activity, "pendingTbLeprosyReferralLaunch"));
    }

    @Test
    public void handleJsonFormActivityResultShouldRefreshForReferralRegistration() throws Exception {
        TbLeprosyProfileActivity activity = Mockito.mock(TbLeprosyProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doNothing().when(activity).refreshAfterReferralSubmission();

        JSONObject form = new JSONObject()
                .put(JsonFormUtils.ENCOUNTER_TYPE, org.smartregister.chw.referral.util.Constants.EventType.REGISTRATION);
        activity.handleJsonFormActivityResult(form.toString());

        Mockito.verify(activity).refreshAfterReferralSubmission();
    }

    @Test
    public void handleJsonFormActivityResultShouldNotRefreshForNonReferralEncounter() throws Exception {
        TbLeprosyProfileActivity activity = Mockito.mock(TbLeprosyProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doNothing().when(activity).refreshAfterReferralSubmission();

        JSONObject form = new JSONObject().put(JsonFormUtils.ENCOUNTER_TYPE, "TB Leprosy Record Visit");
        activity.handleJsonFormActivityResult(form.toString());

        Mockito.verify(activity, Mockito.never()).refreshAfterReferralSubmission();
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
