package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.widget.TextView;
import org.robolectric.util.ReflectionHelpers;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.smartregister.chw.mothermentor.repository.MotherMentorPartnerClosureRepository;
import org.smartregister.chw.mothermentor.util.MotherMentorPartnerClosure;
import org.smartregister.chw.mothermentor.util.DBConstants;
import java.util.HashMap;
import java.util.Map;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MotherMentorVisitActivityTest extends BaseUnitTest {

    @Test
    public void closedPartnerCannotBeLaunchedDirectlyForANewOrEditedVisit() {
        Map<String, String> values = new HashMap<>();
        values.put(DBConstants.KEY.PARTNER_FOLLOW_UP_PHASE, "tracing");
        values.put(DBConstants.KEY.FOLLOW_UP_STATUS, "not_found");
        values.put(DBConstants.KEY.PARTNER_NOT_FOUND_REASON, "moved");
        MotherMentorPartnerClosure closure = MotherMentorPartnerClosure.active()
                .advance(values, "2026-11-04");
        Activity activity = Robolectric.buildActivity(Activity.class).get();
        try (MockedStatic<MotherMentorPartnerClosureRepository> repository =
                     Mockito.mockStatic(MotherMentorPartnerClosureRepository.class)) {
            repository.when(() -> MotherMentorPartnerClosureRepository.getState(
                    Mockito.any(), Mockito.eq("partner"), Mockito.isNull())).thenReturn(closure);
            for (boolean editMode : new boolean[] {false, true}) {
                MotherMentorVisitActivity.startMotherMentorFollowUpVisitActivity(
                        activity, "partner", editMode, MotherMentorFollowUpType.PARTNER);
                assertNull(Shadows.shadowOf(activity).getNextStartedActivityForResult());
            }
        }
    }

    @Test
    public void cancellingClosureConfirmationReleasesSubmitLatch() {
        MotherMentorVisitActivity activity = Robolectric.buildActivity(MotherMentorVisitActivity.class).get();
        TextView submit = new TextView(activity);
        submit.setEnabled(false);
        ReflectionHelpers.setField(activity, "tvSubmit", submit);
        ReflectionHelpers.setField(activity, "submitting", true);

        activity.onSubmissionCancelled();

        assertTrue(submit.isEnabled());
        assertFalse(ReflectionHelpers.getField(activity, "submitting"));
    }

    @Test
    public void visitWorkflowIsSelectedOnlyByExplicitFollowUpContext() {
        MotherMentorVisitActivity homeVisit = Robolectric
                .buildActivity(MotherMentorVisitActivity.class, new Intent())
                .get();
        assertFalse(homeVisit.isFollowUpVisit());

        Intent followUpIntent = new Intent()
                .putExtra(MotherMentorVisitActivity.EXTRA_FOLLOW_UP_VISIT, true);
        MotherMentorVisitActivity followUpVisit = Robolectric
                .buildActivity(MotherMentorVisitActivity.class, followUpIntent)
                .get();
        assertTrue(followUpVisit.isFollowUpVisit());
    }

    @Test
    public void followUpCategoryComesFromStableIntentExtra() {
        Intent partnerIntent = new Intent()
                .putExtra(MotherMentorVisitActivity.EXTRA_FOLLOW_UP_VISIT, true)
                .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                        MotherMentorFollowUpType.PARTNER.getValue());
        MotherMentorVisitActivity partnerVisit = Robolectric
                .buildActivity(MotherMentorVisitActivity.class, partnerIntent)
                .get();
        assertEquals(MotherMentorFollowUpType.PARTNER, partnerVisit.getFollowUpType());

        Intent childIntent = new Intent()
                .putExtra(MotherMentorVisitActivity.EXTRA_FOLLOW_UP_VISIT, true)
                .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                        MotherMentorFollowUpType.CHILD_EID.getValue());
        MotherMentorVisitActivity childVisit = Robolectric
                .buildActivity(MotherMentorVisitActivity.class, childIntent)
                .get();
        assertEquals(MotherMentorFollowUpType.CHILD_EID, childVisit.getFollowUpType());
    }
}
