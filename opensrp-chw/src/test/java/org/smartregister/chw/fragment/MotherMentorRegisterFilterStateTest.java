package org.smartregister.chw.fragment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorRegisterFilterStateTest {
    @Test
    public void disabledFiltersClearSavedChoicesWhenApplied() {
        MotherMentorRegisterFilterState state =
                new MotherMentorRegisterFilterState(false, true, true, true).applied();

        assertFalse(state.isEnabled());
        assertFalse(state.isPregnant());
        assertFalse(state.isBreastfeeding());
        assertFalse(state.isUnderTwo());
        assertFalse(state.isActive());
    }

    @Test
    public void enabledFilterWithNoChoicesStillShowsAllClients() {
        MotherMentorRegisterFilterState state =
                new MotherMentorRegisterFilterState(true, false, false, false).applied();

        assertTrue(state.isEnabled());
        assertFalse(state.isActive());
    }

    @Test
    public void bundleRoundTripPreservesTheIndependentDialogDraft() {
        MotherMentorRegisterFilterState restored =
                MotherMentorRegisterFilterState.fromBundle(
                        new MotherMentorRegisterFilterState(false, true, false, true).toBundle());

        assertFalse(restored.isEnabled());
        assertTrue(restored.isPregnant());
        assertFalse(restored.isBreastfeeding());
        assertTrue(restored.isUnderTwo());
        assertFalse(restored.isActive());
    }

    @Test
    public void anyEnabledCategoryMakesTheAppliedFilterActive() {
        assertTrue(new MotherMentorRegisterFilterState(true, true, false, false).isActive());
        assertTrue(new MotherMentorRegisterFilterState(true, false, true, false).isActive());
        assertTrue(new MotherMentorRegisterFilterState(true, false, false, true).isActive());
        assertFalse(MotherMentorRegisterFilterState.fromBundle(null).isActive());
    }
}
