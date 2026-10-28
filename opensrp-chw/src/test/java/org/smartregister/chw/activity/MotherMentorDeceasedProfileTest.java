package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.content.Context;
import android.view.View;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.R;
import org.smartregister.chw.util.MarkClientAsDeceasedHelper;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.util.AppExecutors;
import org.smartregister.util.LangUtils;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;

import java.text.SimpleDateFormat;
import java.util.Locale;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;

/** A stale profile or saved draft must never restart services after a finalized death. */
public class MotherMentorDeceasedProfileTest extends BaseUnitTest {

    @Test
    public void deceasedMotherCannotStartOrContinueServices() {
        assertDeceasedProfile(new Intent());
    }

    @Test
    public void deceasedFollowUpClientsCannotStartOrContinueServices() {
        for (MotherMentorFollowUpType type : MotherMentorFollowUpType.values()) {
            assertDeceasedProfile(new Intent()
                    .putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true)
                    .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE, type.getValue()));
        }
    }

    @Test
    public void finalizedDeathCreatesStandardRemovalWithRecordedDateOnce() throws Exception {
        MotherMentorProfileActivity activity = profile(new Intent());
        ReflectionHelpers.setField(activity, "deathRecordExecutors",
                new AppExecutors(Runnable::run, Runnable::run, Runnable::run));
        java.util.Date recorded = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2026-10-20");
        try (MockedStatic<MotherMentorDao> dao = Mockito.mockStatic(MotherMentorDao.class);
                MockedStatic<MarkClientAsDeceasedHelper> mortality =
                        Mockito.mockStatic(MarkClientAsDeceasedHelper.class)) {
            dao.when(() -> MotherMentorDao.isClientDeceased("deceased-test-client")).thenReturn(true);
            dao.when(() -> MotherMentorDao.hasRecordedDeath("deceased-test-client"))
                    .thenReturn(false, true);
            dao.when(() -> MotherMentorDao.getClientDeathDate("deceased-test-client"))
                    .thenReturn(recorded);
            activity.applyMotherMentorDeceasedHandling();
            activity.applyMotherMentorDeceasedHandling();

            mortality.verify(() -> MarkClientAsDeceasedHelper.markAsDeceased(activity,
                    "deceased-test-client", null, null, null, null, "20-10-2026"));
            mortality.verifyNoMoreInteractions();
            dao.verify(() -> MotherMentorDao.closeDeceasedClientEnrollments("deceased-test-client"),
                    Mockito.times(2));
        }
    }

    @Test
    public void alreadyRecordedDeathRepairsEnrollmentWithoutAnotherRemoval() {
        MotherMentorProfileActivity activity = profile(new Intent());
        ReflectionHelpers.setField(activity, "deathRecordExecutors",
                new AppExecutors(Runnable::run, Runnable::run, Runnable::run));
        try (MockedStatic<MotherMentorDao> dao = Mockito.mockStatic(MotherMentorDao.class);
                MockedStatic<MarkClientAsDeceasedHelper> mortality =
                        Mockito.mockStatic(MarkClientAsDeceasedHelper.class)) {
            dao.when(() -> MotherMentorDao.isClientDeceased("deceased-test-client")).thenReturn(true);
            dao.when(() -> MotherMentorDao.hasRecordedDeath("deceased-test-client")).thenReturn(true);
            activity.applyMotherMentorDeceasedHandling();
            mortality.verifyNoInteractions();
            dao.verify(() -> MotherMentorDao.closeDeceasedClientEnrollments("deceased-test-client"));
        }
    }

    @Test
    public void unknownLegacyDateDoesNotFabricateDateOfDeath() {
        MotherMentorProfileActivity activity = profile(new Intent());
        ReflectionHelpers.setField(activity, "deathRecordExecutors",
                new AppExecutors(Runnable::run, Runnable::run, Runnable::run));
        try (MockedStatic<MotherMentorDao> dao = Mockito.mockStatic(MotherMentorDao.class);
                MockedStatic<MarkClientAsDeceasedHelper> mortality =
                        Mockito.mockStatic(MarkClientAsDeceasedHelper.class)) {
            dao.when(() -> MotherMentorDao.isClientDeceased("deceased-test-client")).thenReturn(true);
            activity.applyMotherMentorDeceasedHandling();
            mortality.verifyNoInteractions();
            dao.verify(() -> MotherMentorDao.closeDeceasedClientEnrollments("deceased-test-client"),
                    Mockito.never());
        }
    }

    @Test
    @Config(qualifiers = "en")
    public void englishDeathStatusIsShownWithoutHidingHistory() {
        assertStatus("Deceased");
    }

    @Test
    @Config(qualifiers = "sw")
    public void swahiliDeathStatusIsShownWithoutHidingHistory() {
        assertStatus("Amefariki");
    }

    @Test
    public void menuUsesStandardRemovalScreenWithTheCurrentClientsIdentity() {
        MotherMentorProfileActivity activity = profile(new Intent());
        CommonPersonObjectClient client = new CommonPersonObjectClient("deceased-test-client", new java.util.HashMap<>(), "");
        MenuItem item = Mockito.mock(MenuItem.class);
        Mockito.when(item.getItemId()).thenReturn(R.id.action_remove_member);
        try (MockedStatic<MotherMentorDao> dao = Mockito.mockStatic(MotherMentorDao.class);
                MockedStatic<org.smartregister.chw.core.utils.Utils> clients =
                        Mockito.mockStatic(org.smartregister.chw.core.utils.Utils.class)) {
            clients.when(() -> org.smartregister.chw.core.utils.Utils
                    .getCommonPersonObjectClient("deceased-test-client")).thenReturn(client);
            assertTrue(activity.onOptionsItemSelected(item));
            Intent removal = Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
            assertEquals(IndividualProfileRemoveActivity.class.getName(), removal.getComponent().getClassName());
            assertEquals(client, removal.getSerializableExtra(org.smartregister.chw.util.Constants.INTENT_KEY.CHILD_COMMON_PERSON));
        }
    }

    @Test
    public void queuedDeathWriteDoesNotRunAfterLogoutOrUnderAnotherUser() {
        for (boolean logout : new boolean[] {true, false}) {
            DeceasedProfileActivity activity = (DeceasedProfileActivity) profile(new Intent());
            java.util.List<Runnable> queued = new java.util.ArrayList<>();
            ReflectionHelpers.setField(activity, "deathRecordExecutors",
                    new AppExecutors(queued::add, Runnable::run, Runnable::run));
            try (MockedStatic<MotherMentorDao> dao = Mockito.mockStatic(MotherMentorDao.class);
                    MockedStatic<MarkClientAsDeceasedHelper> mortality =
                            Mockito.mockStatic(MarkClientAsDeceasedHelper.class)) {
                dao.when(() -> MotherMentorDao.isClientDeceased("deceased-test-client"))
                        .thenReturn(true);
                activity.applyMotherMentorDeceasedHandling();
                assertEquals(1, queued.size());
                if (logout) activity.activeSession = false;
                else activity.providerId = "another-provider";
                queued.get(0).run();
                mortality.verifyNoInteractions();
                dao.verify(() -> MotherMentorDao.hasRecordedDeath("deceased-test-client"), Mockito.never());
                dao.verify(() -> MotherMentorDao.closeDeceasedClientEnrollments("deceased-test-client"), Mockito.never());
            }
        }
    }

    @Test
    public void queuedDeathWriteSurvivesFinishingTheProfile() throws Exception {
        assertQueuedDeathSurvivesProfileClosure(false);
    }

    @Test
    public void queuedDeathWriteSurvivesDestroyingTheProfile() throws Exception {
        assertQueuedDeathSurvivesProfileClosure(true);
    }

    private void assertQueuedDeathSurvivesProfileClosure(boolean destroy) throws Exception {
        ActivityController<QueuedDeathProfileActivity> controller =
                Robolectric.buildActivity(QueuedDeathProfileActivity.class);
        QueuedDeathProfileActivity activity = controller.get();
        activity.setTheme(R.style.ChwTheme);
        controller.create();
        MemberObject member = new MemberObject();
        member.setBaseEntityId("deceased-test-client");
        ReflectionHelpers.setField(activity, "memberObject", member);
        java.util.List<Runnable> queued = new java.util.ArrayList<>();
        ReflectionHelpers.setField(activity, "deathRecordExecutors",
                new AppExecutors(queued::add, Runnable::run, Runnable::run));
        java.util.Date recorded =
                new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2026-10-20");
        try (MockedStatic<MotherMentorDao> dao = Mockito.mockStatic(MotherMentorDao.class);
                MockedStatic<MarkClientAsDeceasedHelper> mortality =
                        Mockito.mockStatic(MarkClientAsDeceasedHelper.class)) {
            dao.when(() -> MotherMentorDao.isClientDeceased("deceased-test-client"))
                    .thenReturn(true);
            dao.when(() -> MotherMentorDao.getClientDeathDate("deceased-test-client"))
                    .thenReturn(recorded);
            activity.applyMotherMentorDeceasedHandling();
            assertEquals(1, queued.size());
            if (destroy) {
                controller.destroy();
                assertTrue(activity.isDestroyed());
            } else {
                activity.finish();
                assertTrue(activity.isFinishing());
            }
            assertFalse(activity.session.IsUserLoggedOut());
            queued.get(0).run();

            mortality.verify(() -> MarkClientAsDeceasedHelper.markAsDeceased(activity,
                    "deceased-test-client", null, null, null, null, "20-10-2026"));
            dao.verify(() -> MotherMentorDao.closeDeceasedClientEnrollments("deceased-test-client"));
        }
    }

    private void assertStatus(String expected) {
        Context context = RuntimeEnvironment.getApplication();
        String originalLanguage = LangUtils.getLanguage(context);
        try {
            LangUtils.saveLanguage(context, "Amefariki".equals(expected) ? "sw" : "en");
            MotherMentorProfileActivity activity = profile(new Intent());
            FrameLayout layout = new FrameLayout(activity);
            TextView status = new TextView(activity);
            status.setId(org.smartregister.chw.mothermentor.R.id.family_mothermentor_head);
            layout.addView(status);
            activity.setContentView(layout);
            activity.updateDeceasedClientStatusTag(true);
            assertEquals(expected, status.getText().toString());
            assertEquals(View.VISIBLE, status.getVisibility());
            activity.updateDeceasedClientStatusTag(false);
            assertEquals(View.GONE, status.getVisibility());
        } finally {
            LangUtils.saveLanguage(context, originalLanguage);
        }
    }

    private MotherMentorProfileActivity profile(Intent intent) {
        MotherMentorProfileActivity activity =
                Robolectric.buildActivity(DeceasedProfileActivity.class, intent).get();
        activity.setTheme(R.style.ChwTheme);
        MemberObject member = new MemberObject();
        member.setBaseEntityId("deceased-test-client");
        member.setFamilyBaseEntityId("test-household");
        ReflectionHelpers.setField(activity, "memberObject", member);
        return activity;
    }

    private void assertDeceasedProfile(Intent intent) {
        MotherMentorProfileActivity activity = profile(intent);
        TextView record = new TextView(activity);
        TextView process = new TextView(activity);
        RelativeLayout draft = new RelativeLayout(activity);
        ReflectionHelpers.setField(activity, "textViewRecordMotherMentor", record);
        ReflectionHelpers.setField(activity, "manualProcessVisit", process);
        ReflectionHelpers.setField(activity, "mothermentorServiceInProgress", draft);
        ReflectionHelpers.setField(activity, "rlLastVisit", new RelativeLayout(activity));

        try (MockedStatic<MotherMentorDao> dao = Mockito.mockStatic(MotherMentorDao.class)) {
            dao.when(() -> MotherMentorDao.isClientDeceased("deceased-test-client"))
                    .thenReturn(true);
            activity.setupButtons();

            assertEquals(View.GONE, record.getVisibility());
            assertEquals(View.GONE, process.getVisibility());
            assertEquals(View.GONE, draft.getVisibility());
            activity.openRecordClientVisit();
            activity.continueService();
            activity.continueContactVisit();
            assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
        }
    }

    public static class QueuedDeathProfileActivity extends MotherMentorProfileActivity {
        final org.smartregister.Context session = Mockito.mock(org.smartregister.Context.class);

        @Override
        protected void onCreation() {
            // Initialize the real Activity lifecycle without unrelated profile/database views.
        }

        @Override
        public org.smartregister.Context context() {
            return session;
        }

        @Override
        protected String getDeathRecordProviderId() {
            return "initiating-provider";
        }
    }

    public static class DeceasedProfileActivity extends MotherMentorProfileActivity {
        boolean activeSession = true;
        String providerId = "initiating-provider";

        @Override
        protected boolean isDeathRecordSessionActive() {
            return activeSession;
        }

        @Override
        protected String getDeathRecordProviderId() {
            return providerId;
        }

        @Override
        public void refreshMedicalHistory(boolean hasHistory) {
            // This test checks the service controls; history is covered independently.
        }
    }
}
