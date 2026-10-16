package org.smartregister.chw.activity;

import android.content.Intent;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.smartregister.chw.BaseUnitTest;

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
}
