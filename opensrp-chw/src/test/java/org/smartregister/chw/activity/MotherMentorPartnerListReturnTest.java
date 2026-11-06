package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import android.content.Intent;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.smartregister.chw.BaseUnitTest;

public class MotherMentorPartnerListReturnTest extends BaseUnitTest {
    @Test
    public void partnerListReturnIsConsumedOnceWithoutDiscardingTheNestedPartnerTab() {
        CapturingRegister activity = Robolectric.buildActivity(CapturingRegister.class, request()).get();
        activity.openRequestedFollowUpList();
        assertEquals(3, activity.selectedPage);
        assertEquals(1, activity.switches);
        assertFalse(activity.getIntent().hasExtra(MotherMentorRegisterActivity.EXTRA_OPEN_FOLLOW_UP_LIST));
        assertEquals(MotherMentorRegisterActivity.TAB_INDEX_PARTNER,
                activity.getIntent().getIntExtra(MotherMentorRegisterActivity.EXTRA_SECONDARY_ENROLLMENT_TAB_INDEX, -1));
        activity.openRequestedFollowUpList();
        assertEquals(1, activity.switches);
    }

    @Test
    public void reusedActivityReceivesTheNewIntentAndStillConsumesTheRequestOnlyOnce() {
        CapturingRegister activity = Robolectric.buildActivity(CapturingRegister.class, new Intent()).get();
        Intent incoming = request();
        activity.onNewIntent(incoming);
        assertSame(incoming, activity.getIntent());
        assertEquals(3, activity.selectedPage);
        assertEquals(1, activity.switches);
        activity.openRequestedFollowUpList();
        assertEquals(1, activity.switches);
    }

    @Test
    public void parentNavigationStillOpensAfterNestedFragmentHasConsumedTheTabIndex() {
        Intent intent = request();
        intent.removeExtra(MotherMentorRegisterActivity.EXTRA_SECONDARY_ENROLLMENT_TAB_INDEX);
        CapturingRegister activity = Robolectric.buildActivity(CapturingRegister.class, intent).get();
        activity.openRequestedFollowUpList();
        assertEquals(3, activity.selectedPage);
        assertEquals(1, activity.switches);
    }

    private Intent request() {
        return new Intent()
                .putExtra(MotherMentorRegisterActivity.EXTRA_OPEN_FOLLOW_UP_LIST, true)
                .putExtra(MotherMentorRegisterActivity.EXTRA_SECONDARY_ENROLLMENT_TAB_INDEX,
                        MotherMentorRegisterActivity.TAB_INDEX_PARTNER);
    }

    public static class CapturingRegister extends MotherMentorRegisterActivity {
        private int selectedPage = -1;
        private int switches;

        @Override
        public void switchToFragment(int position) {
            selectedPage = position;
            switches++;
        }
    }
}
