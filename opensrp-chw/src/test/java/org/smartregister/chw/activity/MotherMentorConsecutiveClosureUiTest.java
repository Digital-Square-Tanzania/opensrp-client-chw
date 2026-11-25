package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.R;
import org.smartregister.chw.mothermentor.activity.BaseMotherMentorVisitActivity;
import org.smartregister.chw.mothermentor.contract.BaseMotherMentorVisitContract;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorChildEidExit;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorHouseholdClosure;
import org.smartregister.chw.mothermentor.util.MotherMentorIitState;
import org.smartregister.chw.mothermentor.util.MotherMentorPartnerClosure;
import org.smartregister.util.LangUtils;

import java.util.HashMap;
import java.util.Map;

public class MotherMentorConsecutiveClosureUiTest extends BaseUnitTest {
    private static final LocalDate DAY = new LocalDate(2026, 11, 24);

    @Test
    public void englishSaveFlows() {
        checkSaveFlows(false);
        assertFalse(ShadowAlertDialog.getLatestDialog().isShowing());
    }

    @Test
    public void swahiliSaveFlows() {
        checkSaveFlows(true);
        assertFalse(ShadowAlertDialog.getLatestDialog().isShowing());
    }

    private void checkSaveFlows(boolean swahili) {
        LangUtils.saveLanguage(RuntimeEnvironment.getApplication(), swahili ? "sw" : "en");

        checkHouseholdSaveFlows(swahili);
        checkChildEidSaveFlows(swahili);
        checkPartnerSaveFlows(swahili);
        checkIitSaveFlows(swahili);
    }

    private void checkHouseholdSaveFlows(boolean swahili) {
        MotherMentorHouseholdClosure household = MotherMentorHouseholdClosure.active();
        for (int strike = 1; strike <= 3; strike++) {
            household = household.advance("repeat", "not_found", "house_closed", DAY.toString());
            checkSave(householdVisit(household), strike, swahili,
                    swahili ? "Hakuna aliyepatikana (nyumba imefungwa)" : "No one found (house closed)");
        }
        MotherMentorHouseholdClosure other = MotherMentorHouseholdClosure.active();
        for (int strike = 1; strike <= 3; strike++) {
            other = other.advance("repeat", "not_found", "other", DAY.toString());
        }
        checkSave(householdVisit(other), 3, swahili,
                swahili ? "Haijapatikana — Nyingine" : "Not found — Other");
        MotherMentorHouseholdClosure mixed = MotherMentorHouseholdClosure.active()
                .advance("repeat", "not_found", "house_closed", DAY.toString())
                .advance("repeat", "not_found", "other", DAY.toString())
                .advance("repeat", "not_found", "house_closed", DAY.toString());
        checkSave(householdVisit(mixed), 3, swahili, lostContact(swahili));
    }

    private void checkChildEidSaveFlows(boolean swahili) {
        for (int strike = 1; strike <= 3; strike++) {
            for (MotherMentorChildEidExit exit : new MotherMentorChildEidExit[]{
                    MotherMentorChildEidExit.E8, MotherMentorChildEidExit.E9, MotherMentorChildEidExit.E10}) {
                FollowUpVisit visit = followUpVisit(MotherMentorFollowUpType.CHILD_EID);
                visit.childExit = strike == 3 ? exit : null;
                checkSave(visit, strike, swahili, childStrikeReason(exit, swahili));
            }
        }
    }

    private String childStrikeReason(MotherMentorChildEidExit exit, boolean swahili) {
        switch (exit) {
            case E8:
                return swahili ? "Mlezi amekataa." : "Caregiver refused.";
            case E9:
                return swahili ? "Hajafika baada ya kukubali" : "Agreed but did not attend";
            default:
                return swahili ? "Hajapatikana — Nyingine." : "Not found — Other.";
        }
    }

    private void checkPartnerSaveFlows(boolean swahili) {
        checkPartner("found", "refused", null, null, swahili, swahili ? "Amekataa" : "Refused");
        checkPartner("not_found", null, "travelled", null, swahili, swahili ? "Amesafiri" : "Travelled");
        checkPartner("not_found", null, "other", null, swahili,
                swahili ? "Hajapatikana — Nyingine" : "Not found — Other");
        checkPartner(null, null, null, "no", swahili,
                swahili ? "Hajahudhuria baada ya kukubali" : "Agreed but did not attend");
        MotherMentorPartnerClosure partnerMixed = MotherMentorPartnerClosure.active()
                .advance(partnerAnswers("not_found", null, "travelled", null), DAY.toString())
                .advance(partnerAnswers("not_found", null, "other", null), DAY.toString())
                .advance(partnerAnswers("not_found", null, "travelled", null), DAY.toString());
        FollowUpVisit mixedPartner = followUpVisit(MotherMentorFollowUpType.PARTNER);
        mixedPartner.partner = partnerMixed;
        checkSave(mixedPartner, 3, swahili, lostContact(swahili));
    }

    private void checkIitSaveFlows(boolean swahili) {
        checkIit(MotherMentorIitState.Outcome.NOT_RETURNED, swahili,
                swahili ? "Hajarudi baada ya kukubali" : "Agreed but did not return");
        checkIit(MotherMentorIitState.Outcome.REFUSED, swahili,
                swahili ? "Amekataa kurudi" : "Refused to return");
        checkIit(MotherMentorIitState.Outcome.TRAVELLED, swahili, swahili ? "Amesafiri" : "Travelled");
        checkIit(MotherMentorIitState.Outcome.OTHER, swahili,
                swahili ? "Hajapatikana — Nyingine" : "Not found — Other");
        FollowUpVisit mixedIit = followUpVisit(MotherMentorFollowUpType.MOTHER_IIT);
        mixedIit.iit = MotherMentorIitState.active()
                .advance(MotherMentorIitState.Outcome.TRAVELLED, DAY, "", 0)
                .advance(MotherMentorIitState.Outcome.OTHER, DAY, "", 0)
                .advance(MotherMentorIitState.Outcome.TRAVELLED, DAY, "", 0);
        checkSave(mixedIit, 3, swahili, lostContact(swahili));
    }

    private void checkPartner(String status, String agreement, String reason, String attendance,
            boolean swahili, String label) {
        Map<String, String> answers = partnerAnswers(status, agreement, reason, attendance);
        MotherMentorPartnerClosure preview = MotherMentorPartnerClosure.active();
        for (int strike = 1; strike <= 3; strike++) {
            preview = preview.advance(answers, DAY.toString());
            FollowUpVisit visit = followUpVisit(MotherMentorFollowUpType.PARTNER);
            visit.partner = preview;
            checkSave(visit, strike, swahili, label);
        }
    }

    private Map<String, String> partnerAnswers(String status, String agreement, String reason, String attendance) {
        Map<String, String> answers = new HashMap<>();
        answers.put(DBConstants.KEY.PARTNER_FOLLOW_UP_PHASE, attendance == null ? "tracing" : "attendance");
        answers.put(DBConstants.KEY.FOLLOW_UP_STATUS, status);
        answers.put(DBConstants.KEY.PARTNER_FACILITY_AGREEMENT, agreement);
        answers.put(DBConstants.KEY.PARTNER_NOT_FOUND_REASON, reason);
        answers.put(DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY, attendance);
        return answers;
    }

    private void checkIit(MotherMentorIitState.Outcome outcome, boolean swahili, String label) {
        MotherMentorIitState preview = MotherMentorIitState.active();
        for (int strike = 1; strike <= 3; strike++) {
            preview = preview.advance(outcome, DAY.plusDays(strike), "", 0);
            FollowUpVisit visit = followUpVisit(MotherMentorFollowUpType.MOTHER_IIT);
            visit.iit = preview;
            checkSave(visit, strike, swahili, label);
        }
    }

    private String lostContact(boolean swahili) {
        return swahili ? "Kutopatikana wakati wa ufuatiliaji" : "Lost to follow-up";
    }

    private void checkSave(BaseMotherMentorVisitActivity activity, int strike, boolean swahili, String reason) {
        BaseMotherMentorVisitContract.Presenter presenter = activity.getPresenter();
        AlertDialog previous = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        TextView submit = ReflectionHelpers.getField(activity, "tvSubmit");
        activity.submitVisit();
        activity.submitVisit();
        if (strike < 3) {
            assertSame(previous, ShadowAlertDialog.getLatestDialog());
            verify(presenter).submitVisit();
            return;
        }
        AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertNotSame(previous, dialog);
        assertTrue(dialog.isShowing());
        assertFalse(submit.isEnabled());
        verifyNoInteractions(presenter);
        assertEquals((swahili ? "Rekodi hii itafungwa baada ya kuhifadhi. Sababu: "
                : "This record will close after saving. Reason: ") + reason,
                ((TextView) dialog.findViewById(android.R.id.message)).getText().toString());
        assertEquals(swahili ? "Hifadhi na funga" : "Save and close",
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        assertEquals(swahili ? "Ghairi" : "Cancel",
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString());

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        verifyNoInteractions(presenter);
        assertFalse(dialog.isShowing());
        assertTrue(submit.isEnabled());
        assertEquals("fixture selections", activity.getMotherMentorVisitActions().get("answers").getJsonPayload());

        activity.submitVisit();
        AlertDialog retry = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertNotSame(dialog, retry);
        assertTrue(retry.isShowing());
        retry.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        retry.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        activity.submitVisit();
        verify(presenter).submitVisit();
        verifyNoMoreInteractions(presenter);
    }

    private HouseholdVisit householdVisit(MotherMentorHouseholdClosure closure) {
        HouseholdVisit visit = Robolectric.buildActivity(HouseholdVisit.class).get();
        visit.closure = closure;
        bind(visit);
        return visit;
    }

    private FollowUpVisit followUpVisit(MotherMentorFollowUpType type) {
        FollowUpVisit visit = Robolectric.buildActivity(FollowUpVisit.class).get();
        visit.type = type;
        bind(visit);
        return visit;
    }

    private void bind(BaseMotherMentorVisitActivity visit) {
        visit.setTheme(R.style.ChwTheme);
        ReflectionHelpers.setField(visit, "tvSubmit", new TextView(visit));
        ReflectionHelpers.setField(visit, "presenter", mock(BaseMotherMentorVisitContract.Presenter.class));
        BaseMotherMentorVisitAction answer = mock(BaseMotherMentorVisitAction.class);
        when(answer.getJsonPayload()).thenReturn("fixture selections");
        visit.getMotherMentorVisitActions().put("answers", answer);
    }

    public static class HouseholdVisit extends MotherMentorHouseholdVisitActivity {
        MotherMentorHouseholdClosure closure;

        @Override protected MotherMentorHouseholdClosure getClosurePreview() { return closure; }
    }

    public static class FollowUpVisit extends MotherMentorVisitActivity {
        MotherMentorFollowUpType type;
        MotherMentorChildEidExit childExit;
        MotherMentorIitState iit;
        MotherMentorPartnerClosure partner;

        @Override boolean isFollowUpVisit() { return true; }
        @Override MotherMentorFollowUpType getFollowUpType() { return type; }
        @Override protected boolean isFollowUpCardClosed() { return false; }
        @Override protected MotherMentorChildEidExit getChildEidClosurePreview() { return childExit; }
        @Override protected MotherMentorIitState getIitClosurePreview() { return iit; }
        @Override protected MotherMentorPartnerClosure getPartnerClosurePreview() { return partner; }
    }
}
