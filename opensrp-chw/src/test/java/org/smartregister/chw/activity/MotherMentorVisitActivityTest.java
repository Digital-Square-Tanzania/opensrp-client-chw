package org.smartregister.chw.activity;

import android.content.Intent;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.util.MotherMentorFollowUpType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MotherMentorVisitActivityTest extends BaseUnitTest {

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
