package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;

import android.content.Intent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.repository.MotherMentorPartnerClosureRepository;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorPartnerClosure;
import org.smartregister.chw.util.MarkClientAsDeceasedHelper;
import org.smartregister.util.AppExecutors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Closed partner history retains the upstream standard shared death workflow. */
public class MotherMentorPartnerDeathIntegrationTest extends BaseUnitTest {
    @Test
    public void closedPartnerKeepsItsProfileAndRetainsQueuedDeathAfterLeaving() throws Exception {
        Intent intent = new Intent()
                .putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true)
                .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                        MotherMentorFollowUpType.PARTNER.getValue());
        PartnerDeathProfile activity = Robolectric.buildActivity(PartnerDeathProfile.class, intent).get();
        MemberObject member = new MemberObject();
        member.setBaseEntityId("synthetic-deceased-partner");
        ReflectionHelpers.setField(activity, "memberObject", member);
        TextView record = new TextView(activity);
        ReflectionHelpers.setField(activity, "textViewRecordMotherMentor", record);
        ReflectionHelpers.setField(activity, "manualProcessVisit", new TextView(activity));
        ReflectionHelpers.setField(activity, "mothermentorServiceInProgress", new RelativeLayout(activity));
        ReflectionHelpers.setField(activity, "rlLastVisit", new RelativeLayout(activity));
        List<Runnable> queued = new ArrayList<>();
        ReflectionHelpers.setField(activity, "deathRecordExecutors",
                new AppExecutors(queued::add, Runnable::run, Runnable::run));
        Date deathDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-10-29");
        Map<String, String> answers = new HashMap<>();
        answers.put(DBConstants.KEY.PARTNER_FOLLOW_UP_PHASE, "tracing");
        answers.put(DBConstants.KEY.FOLLOW_UP_STATUS, "not_found");
        answers.put(DBConstants.KEY.PARTNER_NOT_FOUND_REASON, "deceased");
        MotherMentorPartnerClosure closed = MotherMentorPartnerClosure.active()
                .advance(answers, "2026-10-29");
        try (MockedStatic<MotherMentorPartnerClosureRepository> state = mockStatic(MotherMentorPartnerClosureRepository.class);
                MockedStatic<MotherMentorDao> dao = mockStatic(MotherMentorDao.class);
                MockedStatic<MarkClientAsDeceasedHelper> removal = mockStatic(MarkClientAsDeceasedHelper.class)) {
            state.when(() -> MotherMentorPartnerClosureRepository.getState(
                    any(), eq("synthetic-deceased-partner"), isNull())).thenReturn(closed);
            dao.when(() -> MotherMentorDao.isClientDeceased("synthetic-deceased-partner")).thenReturn(true);
            dao.when(() -> MotherMentorDao.hasRecordedDeath("synthetic-deceased-partner")).thenReturn(false);
            dao.when(() -> MotherMentorDao.getClientDeathDate("synthetic-deceased-partner")).thenReturn(deathDate);

            // This is the normal profile setup hook; closing no longer redirects the profile.
            activity.applyMotherMentorDeceasedHandling();
            activity.openRecordClientVisit();

            assertFalse(activity.isFinishing());
            assertEquals(View.GONE, record.getVisibility());
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
            assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
            assertEquals(1, queued.size());
            removal.verifyNoInteractions();
            activity.finish();
            assertTrue(activity.isFinishing());
            queued.remove(0).run();
            removal.verify(() -> MarkClientAsDeceasedHelper.markAsDeceased(activity,
                    "synthetic-deceased-partner", null, null, null, null, "29-10-2026"));
            dao.verify(() -> MotherMentorDao.closeDeceasedClientEnrollments("synthetic-deceased-partner"));
            removal.verifyNoMoreInteractions();
        }
    }

    public static class PartnerDeathProfile extends MotherMentorProfileActivity {
        @Override
        public void refreshMedicalHistory(boolean hasHistory) {
            // Exercise closure/death dispatch without unrelated history view initialization.
        }

        @Override
        protected boolean isDeathRecordSessionActive() {
            return true;
        }

        @Override
        protected String getDeathRecordProviderId() {
            return "initiating-synthetic-provider";
        }
    }
}
