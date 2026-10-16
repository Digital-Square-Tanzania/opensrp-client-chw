package org.smartregister.chw.activity;

import android.content.Intent;
import android.view.View;

import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.domain.MemberObject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Guards against routing the localized follow-up button by its displayed text. */
public class MotherMentorProfileActivityTest extends BaseUnitTest {

    @Test
    public void contactButtonIsHiddenOnlyForFollowUpNavigationContext() {
        MotherMentorProfileActivity homeProfile = Robolectric
                .buildActivity(MotherMentorProfileActivity.class, new Intent())
                .get();
        assertFalse(homeProfile.isFollowUpProfile());

        Intent followUpIntent = new Intent()
                .putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true);
        MotherMentorProfileActivity followUpProfile = Robolectric
                .buildActivity(MotherMentorProfileActivity.class, followUpIntent)
                .get();
        assertTrue(followUpProfile.isFollowUpProfile());
    }

    @Test
    public void recordFollowUpClickLaunchesClientVisitRegardlessOfButtonLabel() {
        MotherMentorProfileActivity activity = Mockito.spy(
                Robolectric.buildActivity(MotherMentorProfileActivity.class).get());
        MemberObject member = new MemberObject();
        member.setBaseEntityId("test-client-id");
        ReflectionHelpers.setField(activity, "memberObject", member);
        Mockito.doNothing().when(activity).openRecordClientVisit();

        View view = Mockito.mock(View.class);
        Mockito.when(view.getId()).thenReturn(org.smartregister.chw.mothermentor.R.id.textview_record_mothermentor);
        activity.onClick(view);

        Mockito.verify(activity).openRecordClientVisit();
    }
}
