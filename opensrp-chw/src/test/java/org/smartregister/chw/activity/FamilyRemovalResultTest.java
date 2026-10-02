package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.R;
import org.smartregister.chw.core.fragment.CoreFamilyRemoveMemberFragment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.smartregister.chw.core.provider.CoreFamilyRemoveMemberProvider.REMOVAL_REASON_DEATH;
import static org.smartregister.chw.core.provider.CoreFamilyRemoveMemberProvider.REMOVAL_REASON_START_NEW_FAMILY;

@Config(sdk = 28)
public class FamilyRemovalResultTest extends BaseUnitTest {
    private FamilyRemoveMemberActivity activity;
    private CoreFamilyRemoveMemberFragment fragment;

    @Before
    public void setUp() {
        ShadowToast.reset();
        activity = Robolectric.buildActivity(FamilyRemoveMemberActivity.class).get();
        fragment = mock(CoreFamilyRemoveMemberFragment.class);
        ReflectionHelpers.setField(activity, "removeMemberFragment", fragment);
    }

    @Test
    public void missingReasonDoesNotCrashOrRemoveMember() {
        activity.onActivityResult(1, Activity.RESULT_OK, result());
        verify(fragment, never()).confirmRemove(any(JSONObject.class));
        verify(fragment, never()).startNewFamily(anyString(), anyString());
    }

    @Test
    public void missingFragmentDoesNotCrashOrRemoveMember() {
        ReflectionHelpers.setField(activity, "removeMemberFragment", null);
        activity.onActivityResult(1, Activity.RESULT_OK,
                result().putExtra("reasonForRemove", REMOVAL_REASON_DEATH));
        assertEquals(activity.getString(R.string.member_removal_reason_missing), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void canceledResultWithoutDataDoesNotRequireFragment() {
        ReflectionHelpers.setField(activity, "removeMemberFragment", null);
        activity.onActivityResult(1, Activity.RESULT_CANCELED, null);
        assertEquals(0, ShadowToast.shownToastCount());
    }

    @Test
    public void emptyJsonWithoutReasonDoesNotReachCoreHandler() {
        activity.onActivityResult(1, Activity.RESULT_OK, new Intent().putExtra("json", ""));
        assertEquals(activity.getString(R.string.member_removal_reason_missing), ShadowToast.getTextOfLatestToast());
        verify(fragment, never()).confirmRemove(any(JSONObject.class));
        verify(fragment, never()).startNewFamily(anyString(), anyString());
    }

    @Test
    public void selectedDeathReasonStillConfirmsRemoval() {
        when(fragment.getReasonForRemove()).thenReturn(REMOVAL_REASON_DEATH);
        activity.onActivityResult(1, Activity.RESULT_OK, result());
        verify(fragment).confirmRemove(any(JSONObject.class));
    }

    @Test
    public void reasonReturnedByFormIsPreservedWhenFragmentSelectionIsMissing() {
        activity.onActivityResult(1, Activity.RESULT_OK,
                result().putExtra("reasonForRemove", REMOVAL_REASON_START_NEW_FAMILY));
        verify(fragment).startNewFamily("{}", REMOVAL_REASON_START_NEW_FAMILY);
    }

    private Intent result() {
        return new Intent().putExtra("json", "{}");
    }
}
