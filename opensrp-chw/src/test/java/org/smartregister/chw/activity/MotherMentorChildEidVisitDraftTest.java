package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONObject;
import org.junit.Test;
import org.mockito.InOrder;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;

import java.util.LinkedHashMap;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Config(sdk = 28)
public class MotherMentorChildEidVisitDraftTest extends BaseUnitTest {
    private static final String STATUS = "mothermentor_child_eid_follow_up_status";
    private static final String HEI = "mothermentor_child_eid_hei";
    private static final String FOUND = "{\"follow_up_status\":\"found\",\"mama_child_status\":\"alive\"}";
    private static final String NOT_FOUND = "{\"follow_up_status\":\"not_found\"}";
    private static final String PENDING = "{\"mama_hei_test_date\":\"26-11-2026\",\"result\":\"pending\"}";

    @Test
    public void englishSaveDeliveredBeforeTheRecreatedVisitLoadsKeepsTracingAndSample() {
        assertRecreatedVisitKeepsDraft("Follow-up status", "HEI test", true);
    }

    @Test
    public void swahiliSaveDeliveredAfterTheRecreatedVisitLoadsKeepsTracingAndSample() {
        assertRecreatedVisitKeepsDraft("Hali ya ufuatiliaji", "Kipimo cha HEI", false);
    }

    @Test
    public void cancellingAnOpenSectionPreservesItsPreviouslySavedDraft() {
        ActivityController<DraftVisit> controller = openVisit();
        Rows rows = new Rows(controller.get(), "Follow-up status", "HEI test");
        rows.showStatus();
        completeTracingAndHei(rows);
        rows.visit.startForm(rows.hei);

        rows.visit.deliverSectionResult(Activity.RESULT_CANCELED, null);
        ActivityController<DraftVisit> recreated = recreateVisit(controller);
        Rows restored = new Rows(recreated.get(), "Follow-up status", "HEI test");
        restored.showStatus();
        verify(restored.status).setJsonPayload(FOUND);
        verify(restored.hei).setJsonPayload(PENDING);
        recreated.pause().stop().destroy();
    }

    @Test
    public void changingTracingToNotFoundDiscardsPreviouslySavedHeiAnswers() {
        ActivityController<DraftVisit> controller = openVisit();
        Rows rows = new Rows(controller.get(), "Follow-up status", "HEI test");
        rows.showStatus();
        completeTracingAndHei(rows);
        rows.visit.startForm(rows.status);
        save(rows.visit, NOT_FOUND);

        ActivityController<DraftVisit> recreated = recreateVisit(controller);
        Rows restored = new Rows(recreated.get(), "Follow-up status", "HEI test");
        restored.showStatus();
        verify(restored.status).setJsonPayload(NOT_FOUND);
        restored.visit.startForm(restored.status);
        save(restored.visit, FOUND);
        verify(restored.hei, never()).setJsonPayload(PENDING);
        recreated.pause().stop().destroy();
    }

    private void assertRecreatedVisitKeepsDraft(String statusLabel, String heiLabel, boolean earlyResult) {
        ActivityController<DraftVisit> controller = openVisit();
        Rows rows = new Rows(controller.get(), statusLabel, heiLabel);
        rows.showStatus();
        rows.visit.startForm(rows.status);
        save(rows.visit, FOUND);
        rows.visit.startForm(rows.hei);

        ActivityController<DraftVisit> recreated = recreateVisit(controller);
        Rows restored = new Rows(recreated.get(), statusLabel, heiLabel);
        if (earlyResult) save(restored.visit, PENDING);
        restored.showStatus();
        if (!earlyResult) save(restored.visit, PENDING);

        InOrder submitted = inOrder(restored.status, restored.hei);
        submitted.verify(restored.status).setJsonPayload(FOUND);
        submitted.verify(restored.hei).setJsonPayload(PENDING);
        recreated.pause().stop().destroy();
    }

    private static void completeTracingAndHei(Rows rows) {
        rows.visit.startForm(rows.status);
        save(rows.visit, FOUND);
        rows.visit.startForm(rows.hei);
        save(rows.visit, PENDING);
    }

    private static ActivityController<DraftVisit> recreateVisit(ActivityController<DraftVisit> controller) {
        Bundle state = new Bundle();
        controller.pause().saveInstanceState(state).stop().destroy();
        return Robolectric.buildActivity(DraftVisit.class, visitIntent()).setup(state);
    }

    private static Intent visitIntent() {
        return new Intent()
                .putExtra(MotherMentorVisitActivity.EXTRA_FOLLOW_UP_VISIT, true)
                .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                        MotherMentorFollowUpType.CHILD_EID.getValue());
    }

    private static ActivityController<DraftVisit> openVisit() {
        return Robolectric.buildActivity(DraftVisit.class, visitIntent()).setup();
    }

    private static void save(DraftVisit visit, String payload) {
        visit.deliverSectionResult(Activity.RESULT_OK,
                new Intent().putExtra(Constants.JSON_FORM_EXTRA.JSON, payload));
    }

    private static class Rows {
        final DraftVisit visit;
        final BaseMotherMentorVisitAction status;
        final BaseMotherMentorVisitAction hei;

        Rows(DraftVisit visit, String statusLabel, String heiLabel) {
            this.visit = visit;
            status = action(STATUS, statusLabel);
            hei = action(HEI, heiLabel);
            doAnswer(invocation -> {
                show(FOUND.equals(invocation.getArgument(0)));
                return null;
            }).when(status).setJsonPayload(anyString());
        }

        void showStatus() {
            show(false);
        }

        void show(boolean found) {
            LinkedHashMap<String, BaseMotherMentorVisitAction> actions = new LinkedHashMap<>();
            actions.put(status.getTitle(), status);
            if (found) actions.put(hei.getTitle(), hei);
            visit.initializeActions(actions);
        }

        private static BaseMotherMentorVisitAction action(String form, String label) {
            BaseMotherMentorVisitAction action = mock(BaseMotherMentorVisitAction.class);
            when(action.getFormName()).thenReturn(form);
            when(action.getTitle()).thenReturn(label);
            when(action.getJsonPayload()).thenReturn("{}");
            return action;
        }
    }

    public static class DraftVisit extends MotherMentorVisitActivity {
        void deliverSectionResult(int resultCode, Intent data) {
            super.onActivityResult(Constants.REQUEST_CODE_GET_JSON, resultCode, data);
        }

        @Override
        protected MemberObject getMemberObject(String baseEntityId) {
            return null;
        }

        @Override
        protected void registerPresenter() {
            // The test supplies the asynchronously loaded rows on its own schedule.
        }

        @Override
        public void startFormActivity(JSONObject form) {
            // The test delivers the native Save result to the visit directly.
        }
    }
}
