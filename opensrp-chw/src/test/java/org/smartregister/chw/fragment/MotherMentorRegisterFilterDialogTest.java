package org.smartregister.chw.fragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.smartregister.chw.R;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorRegisterFilterDialogTest {
    private static final String OWNER_TAG = "filter_owner";
    private ActivityController<FragmentActivity> controller;

    @After
    public void closeActivity() {
        if (controller != null) controller.pause().stop().destroy();
    }

    @Test
    public void backDiscardsChangesWithoutUpdatingAppliedFilters() {
        show(new MotherMentorRegisterFilterState(true, true, false, false));
        option(R.id.mama_kinara_filter_pregnant).setChecked(false);
        option(R.id.mama_kinara_filter_breastfeeding).setChecked(true);

        click(R.id.mama_kinara_filter_back);

        assertNull(dialog());
        assertEquals(0, owner().saveCount);
        assertTrue(owner().applied.isPregnant());
        assertFalse(owner().applied.isBreastfeeding());
    }

    @Test
    public void saveAppliesTheSelectedCategories() {
        show(MotherMentorRegisterFilterState.allClients());
        toggle().setChecked(true);
        option(R.id.mama_kinara_filter_pregnant).setChecked(true);
        option(R.id.mama_kinara_filter_under_two).setChecked(true);

        click(R.id.mama_kinara_filter_save);

        assertNull(dialog());
        assertEquals(1, owner().saveCount);
        assertTrue(owner().applied.isActive());
        assertTrue(owner().applied.isPregnant());
        assertFalse(owner().applied.isBreastfeeding());
        assertTrue(owner().applied.isUnderTwo());
    }

    @Test
    public void savingOffDisablesTheControlsAndClearsAppliedChoices() {
        show(new MotherMentorRegisterFilterState(true, true, true, true));
        toggle().setChecked(false);
        assertFalse(option(R.id.mama_kinara_filter_pregnant).isEnabled());
        assertFalse(option(R.id.mama_kinara_filter_pregnant).isShown());
        assertFalse(option(R.id.mama_kinara_filter_breastfeeding).isEnabled());
        assertFalse(option(R.id.mama_kinara_filter_under_two).isEnabled());

        click(R.id.mama_kinara_filter_save);

        assertEquals(1, owner().saveCount);
        assertFalse(owner().applied.isEnabled());
        assertFalse(owner().applied.isPregnant());
        assertFalse(owner().applied.isBreastfeeding());
        assertFalse(owner().applied.isUnderTwo());
    }

    @Test
    public void savingEnabledWithoutChoicesKeepsAllClientsVisible() {
        show(MotherMentorRegisterFilterState.allClients());
        toggle().setChecked(true);

        click(R.id.mama_kinara_filter_save);

        assertEquals(1, owner().saveCount);
        assertTrue(owner().applied.isEnabled());
        assertFalse(owner().applied.isActive());
    }

    @Test
    public void rotationRestoresTheDraftWithoutApplyingItUntilSave() {
        show(MotherMentorRegisterFilterState.allClients());
        toggle().setChecked(true);
        option(R.id.mama_kinara_filter_pregnant).setChecked(true);
        option(R.id.mama_kinara_filter_breastfeeding).setChecked(true);

        recreate();

        assertNotNull(dialog());
        assertTrue(toggle().isChecked());
        assertTrue(option(R.id.mama_kinara_filter_pregnant).isChecked());
        assertTrue(option(R.id.mama_kinara_filter_breastfeeding).isChecked());
        assertFalse(option(R.id.mama_kinara_filter_under_two).isChecked());
        assertEquals(0, owner().saveCount);
        assertFalse(owner().applied.isEnabled());

        click(R.id.mama_kinara_filter_save);

        assertEquals(1, owner().saveCount);
        assertTrue(owner().applied.isPregnant());
        assertTrue(owner().applied.isBreastfeeding());
    }

    @Test
    public void rotationKeepsPreviouslyAppliedFiltersAfterDialogWasSaved() {
        show(MotherMentorRegisterFilterState.allClients());
        toggle().setChecked(true);
        option(R.id.mama_kinara_filter_under_two).setChecked(true);
        click(R.id.mama_kinara_filter_save);

        recreate();

        assertNull(dialog());
        assertEquals(1, owner().saveCount);
        assertTrue(owner().applied.isEnabled());
        assertTrue(owner().applied.isUnderTwo());
    }

    private void show(MotherMentorRegisterFilterState applied) {
        controller = createController(null);
        CallbackFragment owner = CallbackFragment.newInstance(applied);
        controller
                .get()
                .getSupportFragmentManager()
                .beginTransaction()
                .add(owner, OWNER_TAG)
                .commit();
        idle();
        MotherMentorRegisterFilterDialogFragment dialog =
                MotherMentorRegisterFilterDialogFragment.newInstance(applied);
        dialog.setTargetFragment(owner, 0);
        dialog.show(
                controller.get().getSupportFragmentManager(),
                MotherMentorRegisterFilterDialogFragment.TAG);
        idle();
        assertNotNull(dialog.getView());
    }

    private ActivityController<FragmentActivity> createController(Bundle savedState) {
        ActivityController<FragmentActivity> result =
                Robolectric.buildActivity(FragmentActivity.class);
        result.get().setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar);
        return result.create(savedState).start().resume().visible();
    }

    private void recreate() {
        Bundle savedState = new Bundle();
        controller.saveInstanceState(savedState).pause().stop().destroy();
        controller = createController(savedState);
        idle();
    }

    private void idle() {
        ShadowLooper.runUiThreadTasks();
        controller.get().getSupportFragmentManager().executePendingTransactions();
        ShadowLooper.runUiThreadTasks();
    }

    private void click(int id) {
        View button = dialog().getView().findViewById(id);
        assertTrue(button.performClick());
        idle();
    }

    private SwitchCompat toggle() {
        return dialog().getView().findViewById(R.id.mama_kinara_filter_enabled);
    }

    private CheckBox option(int id) {
        return dialog().getView().findViewById(id);
    }

    private MotherMentorRegisterFilterDialogFragment dialog() {
        return (MotherMentorRegisterFilterDialogFragment)
                controller
                        .get()
                        .getSupportFragmentManager()
                        .findFragmentByTag(MotherMentorRegisterFilterDialogFragment.TAG);
    }

    private CallbackFragment owner() {
        return (CallbackFragment)
                controller.get().getSupportFragmentManager().findFragmentByTag(OWNER_TAG);
    }

    /** A headless callback owner, restored by the normal FragmentManager during recreation. */
    public static class CallbackFragment extends Fragment
            implements MotherMentorRegisterFilterDialogFragment.Listener {
        private static final String APPLIED = "applied";
        private static final String SAVE_COUNT = "save_count";
        MotherMentorRegisterFilterState applied = MotherMentorRegisterFilterState.allClients();
        int saveCount;

        static CallbackFragment newInstance(MotherMentorRegisterFilterState state) {
            CallbackFragment fragment = new CallbackFragment();
            Bundle args = new Bundle();
            args.putBundle(APPLIED, state.toBundle());
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle state = savedInstanceState == null ? getArguments() : savedInstanceState;
            applied =
                    MotherMentorRegisterFilterState.fromBundle(
                            state == null ? null : state.getBundle(APPLIED));
            saveCount = state == null ? 0 : state.getInt(SAVE_COUNT);
        }

        @Override
        public void onFilterSaved(MotherMentorRegisterFilterState state) {
            applied = state;
            saveCount++;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBundle(APPLIED, applied.toBundle());
            outState.putInt(SAVE_COUNT, saveCount);
        }
    }
}
