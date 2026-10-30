package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;

import android.content.Intent;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.dao.MotherMentorPartnerFollowUpDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.util.MarkClientAsDeceasedHelper;
import org.smartregister.util.AppExecutors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Immediate partner-list navigation must preserve the upstream standard shared death workflow. */
public class MotherMentorPartnerDeathIntegrationTest extends BaseUnitTest {
    @Test
    public void endedPartnerQueuesStandardDeathBeforeNavigationAndRetainsItAfterFinishing()
            throws Exception {
        Intent intent = new Intent()
                .putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true)
                .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                        MotherMentorFollowUpType.PARTNER.getValue());
        PartnerDeathProfile activity = Robolectric.buildActivity(PartnerDeathProfile.class, intent).get();
        MemberObject member = new MemberObject();
        member.setBaseEntityId("synthetic-deceased-partner");
        ReflectionHelpers.setField(activity, "memberObject", member);
        List<Runnable> queued = new ArrayList<>();
        ReflectionHelpers.setField(activity, "deathRecordExecutors",
                new AppExecutors(queued::add, Runnable::run, Runnable::run));
        Date deathDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-10-29");
        try (MockedStatic<MotherMentorPartnerFollowUpDao> state = mockStatic(MotherMentorPartnerFollowUpDao.class);
                MockedStatic<MotherMentorDao> dao = mockStatic(MotherMentorDao.class);
                MockedStatic<MarkClientAsDeceasedHelper> removal = mockStatic(MarkClientAsDeceasedHelper.class)) {
            state.when(() -> MotherMentorPartnerFollowUpDao.getContext(
                    eq("synthetic-deceased-partner"), isNull(), any(Date.class)))
                    .thenReturn(new MotherMentorPartnerFollowUpDao.Context(
                            "link", "mother", "episode", "yes", false, false));
            dao.when(() -> MotherMentorDao.isClientDeceased("synthetic-deceased-partner")).thenReturn(true);
            dao.when(() -> MotherMentorDao.hasRecordedDeath("synthetic-deceased-partner")).thenReturn(false);
            dao.when(() -> MotherMentorDao.getClientDeathDate("synthetic-deceased-partner")).thenReturn(deathDate);

            activity.openRecordClientVisit();

            assertTrue(activity.isFinishing());
            assertEquals(1, queued.size());
            assertEquals(MotherMentorRegisterActivity.class.getName(),
                    Shadows.shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
            removal.verifyNoInteractions();
            queued.remove(0).run();
            removal.verify(() -> MarkClientAsDeceasedHelper.markAsDeceased(activity,
                    "synthetic-deceased-partner", null, null, null, null, "29-10-2026"));
            dao.verify(() -> MotherMentorDao.closeDeceasedClientEnrollments("synthetic-deceased-partner"));
            removal.verifyNoMoreInteractions();
        }
    }

    public static class PartnerDeathProfile extends MotherMentorProfileActivity {
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
