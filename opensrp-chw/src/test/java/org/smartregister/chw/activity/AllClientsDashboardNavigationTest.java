package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.fragment.AllClientsDashboardFragment;
import org.smartregister.chw.fragment.AllClientsRegisterFragment;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.provider.ChwAllClientsRegisterQueryProvider;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.opd.OpdLibrary;
import org.smartregister.opd.configuration.OpdConfiguration;
import org.smartregister.opd.presenter.BaseOpdRegisterActivityPresenter;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.view.fragment.BaseRegisterFragment;

import java.util.HashMap;

/** Exercises the real activity lifecycle, pager and bottom-menu callbacks without database work. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class AllClientsDashboardNavigationTest {
    private static Context testContext;
    private ActivityController<NavigationActivity> controller;
    private NavigationActivity activity;
    private boolean activityStarted;
    private MockedStatic<CoreLibrary> coreLibrary;
    private MockedStatic<OpdLibrary> opdLibrary;
    private MockedStatic<CoreChwApplication> application;
    private MockedStatic<NavigationMenu> navigationMenu;
    private MockedStatic<SyncStatusBroadcastReceiver> syncReceiver;

    @Before
    public void setUp() {
        assertEquals("These tests exercise the NACP dashboard", "nacp", BuildConfig.FLAVOR);
        testContext = mock(Context.class);
        coreLibrary = mockStatic(CoreLibrary.class);
        coreLibrary.when(CoreLibrary::getInstance).thenReturn(mock(CoreLibrary.class));
        opdLibrary = mockStatic(OpdLibrary.class);
        OpdLibrary opd = mock(OpdLibrary.class);
        when(opd.getOpdConfiguration()).thenReturn(
                new OpdConfiguration.Builder(ChwAllClientsRegisterQueryProvider.class).build());
        opdLibrary.when(OpdLibrary::getInstance).thenReturn(opd);
        application = mockStatic(CoreChwApplication.class);
        application.when(CoreChwApplication::getInstance)
                .thenReturn(mock(CoreChwApplication.class));
        navigationMenu = mockStatic(NavigationMenu.class);
        syncReceiver = mockStatic(SyncStatusBroadcastReceiver.class);
        syncReceiver.when(SyncStatusBroadcastReceiver::getInstance)
                .thenReturn(mock(SyncStatusBroadcastReceiver.class));
    }

    @After
    public void tearDown() {
        try {
            if (controller != null && activityStarted) {
                controller.pause().stop().destroy();
            }
        } finally {
            if (syncReceiver != null) syncReceiver.close();
            if (navigationMenu != null) navigationMenu.close();
            if (application != null) application.close();
            if (opdLibrary != null) opdLibrary.close();
            if (coreLibrary != null) coreLibrary.close();
            testContext = null;
        }
    }

    @Test
    public void normalEntryShowsHomeAndKeepsTheOriginalRosterAtPageZero() {
        launch(new Intent(), null);

        assertFalse(activity.isClientPickerMode());
        assertEquals(1, activity.requestedFragments.length);
        assertTrue(activity.requestedFragments[0] instanceof AllClientsDashboardFragment);
        assertTrue(activity.findFragmentByPosition(0) instanceof RosterFragment);
        assertSelected(1, R.id.action_family);

        controller.pause().resume();
        assertSelected(1, R.id.action_family);
    }

    @Test
    public void bottomTabsSwitchTheVisiblePageAndCheckedItemTogether() {
        launch(new Intent(), null);

        activity.navigation().setSelectedItemId(R.id.action_dashboard_clients);
        assertSelected(0, R.id.action_dashboard_clients);
        assertNotNull(activity.findFragmentByPosition(0).getView());

        activity.navigation().setSelectedItemId(R.id.action_family);
        assertSelected(1, R.id.action_family);
        assertNotNull(activity.findFragmentByPosition(1).getView());
        assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void addClientLaunchesRegistrationWithoutChangingTheCurrentTab() {
        launch(new Intent(), null);
        assertEquals(2, activity.navigation().getMenu().size());
        assertNull(activity.navigation().getMenu().findItem(R.id.action_register));
        View addClient = activity.findViewById(R.id.dashboard_register_client);
        assertTrue(addClient.isShown());
        addClient.performClick();
        assertEquals(1, activity.registrationsStarted);
        assertSelected(1, R.id.action_family);

        activity.navigation().setSelectedItemId(R.id.action_dashboard_clients);
        assertTrue(addClient.isShown());
        addClient.performClick();
        assertEquals(2, activity.registrationsStarted);
        assertSelected(0, R.id.action_dashboard_clients);
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    public void footerReservesSpaceBelowBothPagesOnANarrowScreen() {
        Resources resources = RuntimeEnvironment.getApplication().getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.fontScale = 1.3f;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        launch(new Intent(), null);
        View content = activity.findViewById(android.R.id.content);
        int width = (int) (320 * activity.getResources().getDisplayMetrics().density);
        int height = (int) (640 * activity.getResources().getDisplayMetrics().density);
        content.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        content.layout(0, 0, width, height);

        View pager = activity.findViewById(R.id.base_view_pager);
        View footer = activity.findViewById(R.id.dashboard_footer);
        TextView addClient = activity.findViewById(R.id.dashboard_register_client);
        assertTrue(pager.getHeight() > 0);
        assertTrue(footer.getHeight() > 0);
        assertTrue(pager.getBottom() <= footer.getTop());
        assertTrue(addClient.getWidth() > 0);
        assertTrue(addClient.getHeight() >= 48 * activity.getResources().getDisplayMetrics().density);
        assertTrue(addClient.getLayout().getHeight() <= addClient.getHeight()
                - addClient.getCompoundPaddingTop() - addClient.getCompoundPaddingBottom());
        for (int line = 0; line < addClient.getLineCount(); line++) {
            assertEquals(0, addClient.getLayout().getEllipsisCount(line));
        }
    }

    @Test
    public void searchActionOpensTheRosterAndFocusesItsSearchField() {
        launch(new Intent(), null);

        activity.showAllClients(true);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertSelected(0, R.id.action_dashboard_clients);
        View search = activity.findFragmentByPosition(0).getView().findViewById(R.id.edt_search);
        assertTrue(search.hasFocus());
    }

    @Test
    public void savedRosterSelectionSurvivesActivityRecreation() {
        launch(new Intent(), null);
        activity.showAllClients(false);
        Bundle state = new Bundle();
        controller.pause().saveInstanceState(state).stop().destroy();
        controller = null;
        activityStarted = false;

        launch(new Intent(), state);

        assertSelected(0, R.id.action_dashboard_clients);
    }

    @Test
    public void backFromRosterReturnsHomeWithoutStartingAnotherActivity() {
        launch(new Intent(), null);
        activity.showAllClients(false);

        activity.onBackPressed();

        assertSelected(1, R.id.action_family);
        assertFalse(activity.isFinishing());
        assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void pickerOpensRosterDirectlyAndReturnsTheSelectedClient() {
        launch(new Intent().putExtra(Constants.EXTRA_CLIENT_PICKER_MODE, true), null);

        assertTrue(activity.isClientPickerMode());
        assertEquals(0, activity.requestedFragments.length);
        assertEquals(0, activity.page());
        assertNull(activity.navigation().getMenu().findItem(R.id.action_dashboard_clients));
        assertNull(activity.findViewById(R.id.dashboard_footer));

        CommonPersonObjectClient client =
                new CommonPersonObjectClient("selected-client", new HashMap<>(), "Selected Client");
        ((RosterFragment) activity.findFragmentByPosition(0)).selectClient(client);

        assertTrue(activity.isFinishing());
        assertEquals(Activity.RESULT_OK, Shadows.shadowOf(activity).getResultCode());
        Intent result = Shadows.shadowOf(activity).getResultIntent();
        assertEquals("selected-client", result.getStringExtra(
                org.smartregister.family.util.Constants.INTENT_KEY.BASE_ENTITY_ID));
        assertSame(client, result.getSerializableExtra(Constants.EXTRA_EXISTING_HEAD_CLIENT));
        assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
    }

    private void launch(Intent intent, Bundle state) {
        controller = Robolectric.buildActivity(NavigationActivity.class, intent);
        activity = controller.get();
        activity.setTheme(R.style.Theme_AppCompat_NoActionBar);
        controller.create(state).start().resume().visible();
        activityStarted = true;
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private void assertSelected(int page, int menuId) {
        assertEquals(page, activity.page());
        assertEquals(menuId, activity.navigation().getSelectedItemId());
        assertTrue(activity.navigation().getMenu().findItem(menuId).isChecked());
    }

    public static class NavigationActivity extends AllClientsRegisterActivity {
        Fragment[] requestedFragments;
        int registrationsStarted;

        @Override
        protected Context context() {
            return testContext;
        }

        @Override
        protected void onCreation() {
            // The base register supplies its real pager layout after secured-activity creation.
        }

        @Override
        protected void onResumption() {
            // Avoid unrelated presenter/database refreshes while keeping onResume itself real.
        }

        @Override
        protected void initializePresenter() {
            presenter = mock(BaseOpdRegisterActivityPresenter.class);
        }

        @Override
        protected BaseRegisterFragment getRegisterFragment() {
            return new RosterFragment();
        }

        @Override
        protected Fragment[] getOtherFragments() {
            requestedFragments = super.getOtherFragments();
            return requestedFragments.length == 0
                    ? requestedFragments : new Fragment[]{new DashboardPage()};
        }

        @Override
        public void startRegistration() {
            registrationsStarted++;
        }

        int page() {
            return mPager.getCurrentItem();
        }

        BottomNavigationView navigation() {
            return bottomNavigationView;
        }
    }

    /** Retains the production picker-result method; replaces database-backed roster rendering. */
    public static class RosterFragment extends AllClientsRegisterFragment {
        @Override
        protected Context context() {
            return testContext;
        }

        @Override
        protected void onCreation() {
        }

        @Override
        protected void onResumption() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
            LinearLayout root = new LinearLayout(requireContext());
            root.setOrientation(LinearLayout.VERTICAL);
            EditText search = new EditText(requireContext());
            search.setId(R.id.edt_search);
            root.addView(search, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return root;
        }

        void selectClient(CommonPersonObjectClient client) {
            goToClientDetailActivity(client);
        }
    }

    public static class DashboardPage extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
            TextView home = new TextView(requireContext());
            home.setText(R.string.all_clients_dashboard_home);
            return home;
        }
    }
}
