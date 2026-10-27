package org.smartregister.chw.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.MockedConstruction;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.*;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.model.NavigationOption;
import org.smartregister.chw.core.utils.CoreConstants.DrawerMenu;
import org.smartregister.chw.fragment.AllClientsDashboardFragment;
import org.smartregister.chw.model.NavigationModelFlv;
import org.smartregister.chw.repository.AllClientsDashboardRepository;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/** Exercises dashboard destinations through the actual published Core navigation listener. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class AllClientsDashboardModuleNavigationTest {
    private MockedStatic<CoreChwApplication> application;

    @Before
    public void setUp() {
        ChwApplication app = mock(ChwApplication.class);
        when(app.getRegisteredActivities()).thenReturn(new ChwApplication().getRegisteredActivities());
        application = mockStatic(CoreChwApplication.class);
        application.when(CoreChwApplication::getInstance).thenReturn(app);
    }

    @After
    public void tearDown() {
        application.close();
    }

    @Test
    public void eachSingleRegisterModuleOpensTheExistingAppRegister() {
        Object[][] routes = {
                {"Mother Mentor", DrawerMenu.MOTHER_MENTOR, MotherMentorRegisterActivity.class},
                {"ANC", DrawerMenu.ANC, AncRegisterActivity.class},
                {"PNC", DrawerMenu.PNC, PncRegisterActivity.class},
                {"Child", DrawerMenu.CHILD_CLIENTS, ChildRegisterActivity.class},
                {"CBHS", DrawerMenu.CBHS_CLIENTS, HivRegisterActivity.class},
                {"iCCHW", DrawerMenu.HPS, HpsRegisterActivity.class},
                {"iCCM", DrawerMenu.ICCM, IccmRegisterActivity.class},
                {"Malaria", DrawerMenu.MALARIA, MalariaRegisterActivity.class},
                {"TB", DrawerMenu.TB_CLIENTS, TbRegisterActivity.class},
                {"Family Planning", DrawerMenu.FAMILY_PLANNING, FpRegisterActivity.class},
                {"AGYW", DrawerMenu.AGYW, AgywRegisterActivity.class},
                {"AYP in school", DrawerMenu.AYP_IN_SCHOOL, AypInSchoolRegisterActivity.class},
                {"AYP out of school", DrawerMenu.AYP_OUT_SCHOOL, AypOutSchoolRegisterActivity.class},
                {"AYP parental", DrawerMenu.AYP_PARENTAL, AypParentalRegisterActivity.class},
                {"KVP/PrEP", DrawerMenu.KVP_PrEP, KvpPrEPRegisterActivity.class},
                {"TB/Leprosy", DrawerMenu.TBLEPROSY, TbLeprosyRegisterActivity.class},
                {"Reproductive Cancers", DrawerMenu.CECAP, CecapRegisterActivity.class},
                {"AYSRH", DrawerMenu.AYSRH, AsrhRegisterActivity.class},
                {"SBC", DrawerMenu.SBC, SbcRegisterActivity.class},
                {"NCD", DrawerMenu.NCD, NcdRegisterActivity.class},
                {"HIVST", DrawerMenu.HIV_SELF_TESTING, HivstRegisterActivity.class}
        };
        List<NavigationOption> available = new ArrayList<>();
        for (Object[] route : routes) available.add(option((String) route[1]));
        for (Object[] route : routes) {
            List<NavigationOption> matches = AllClientsDashboardNavigation.getOptions((String) route[0], available);
            assertEquals((String) route[0], 1, matches.size());
            assertEquals(route[1], matches.get(0).getMenuTitle());
            assertOpens(matches.get(0), (Class<?>) route[2]);
        }
    }

    @Test
    public void separateAypRowsOpenTheirOwnRegisterWithoutAChooser() {
        List<NavigationOption> available = Arrays.asList(option(DrawerMenu.AYP_OUT_SCHOOL),
                option(DrawerMenu.HIV_SELF_TESTING), option(DrawerMenu.AYP_PARENTAL), option(DrawerMenu.AYP_IN_SCHOOL));
        String[] types = {"AYP in school", "AYP out of school", "AYP parental"};
        Class<?>[] registers = {AypInSchoolRegisterActivity.class, AypOutSchoolRegisterActivity.class,
                AypParentalRegisterActivity.class};
        for (int i = 0; i < types.length; i++) {
            Class<?> expected = registers[i];
            withDashboard(Collections.singletonList(types[i]), available, (activity, rows) -> {
                rows.getChildAt(0).performClick();

                assertNull(ShadowDialog.getLatestDialog());
                Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
                assertNotNull(intent);
                assertEquals(expected.getName(), intent.getComponent().getClassName());
            });
        }
        assertTrue(AllClientsDashboardNavigation.getOptions("AYP", available).isEmpty());
    }

    @Test
    public void restrictedAypRoleUsesItsOutOfSchoolRegisterWithoutAddingOtherRoutes() {
        NavigationOption out = option(DrawerMenu.AYP_OUT_SCHOOL);
        List<NavigationOption> available = Arrays.asList(option(DrawerMenu.ALL_CLIENTS), out,
                option(DrawerMenu.HIV_SELF_TESTING));
        List<NavigationOption> matches = AllClientsDashboardNavigation.getOptions("AYP out of school", available);

        assertEquals(1, matches.size());
        assertSame(out, matches.get(0));
        assertOpens(matches.get(0), AypOutSchoolRegisterActivity.class);
        assertTrue(AllClientsDashboardNavigation.getOptions("AYP in school", available).isEmpty());
        assertTrue(AllClientsDashboardNavigation.getOptions("AYP parental", available).isEmpty());
    }

    @Test
    public void harmReductionOffersBothRegistersWhenBothAreAvailable() {
        NavigationOption risk = option(DrawerMenu.HARM_REDUCTION);
        NavigationOption sober = option(DrawerMenu.HARM_REDUCTION_SOBER_HOUSE);
        List<NavigationOption> matches = AllClientsDashboardNavigation.getOptions("HARM REDUCTION",
                Arrays.asList(risk, sober, option(DrawerMenu.ANC)));

        assertEquals(Arrays.asList(risk, sober), matches);
        assertOpens(matches.get(0), HarmReductionRegisterActivity.class);
        assertOpens(matches.get(1), HarmReductionSoberHouseRegisterActivity.class);
    }

    @Test
    public void soberHouseOnlyAccessDoesNotFallBackToRiskAssessment() {
        NavigationOption sober = option(DrawerMenu.HARM_REDUCTION_SOBER_HOUSE);
        List<NavigationOption> matches = AllClientsDashboardNavigation.getOptions("HARM REDUCTION",
                Collections.singletonList(sober));

        assertEquals(Collections.singletonList(sober), matches);
        assertOpens(matches.get(0), HarmReductionSoberHouseRegisterActivity.class);
    }

    @Test
    public void unavailableModulesCannotBypassRoleOrFeatureRestrictions() {
        List<NavigationOption> restricted = Arrays.asList(option(DrawerMenu.ALL_CLIENTS),
                option(DrawerMenu.CBHS_CLIENTS));

        assertTrue(AllClientsDashboardNavigation.getOptions("Mother Mentor", restricted).isEmpty());
        assertTrue(AllClientsDashboardNavigation.getOptions("ANC", restricted).isEmpty());
        assertTrue(AllClientsDashboardNavigation.getOptions("NCD", Collections.emptyList()).isEmpty());
        assertEquals(1, AllClientsDashboardNavigation.getOptions("CBHS", restricted).size());
    }

    @Test
    public void unlinkedAndUnknownSummaryRowsHaveNoInventedDestination() {
        List<NavigationOption> available = Arrays.asList(option(DrawerMenu.ALL_CLIENTS), option(DrawerMenu.ANC));
        assertTrue(AllClientsDashboardNavigation.getOptions("Independent", available).isEmpty());
        assertTrue(AllClientsDashboardNavigation.getOptions("unknown module", available).isEmpty());
    }

    @Test
    public void tappingASingleDestinationRowOpensItsRegisterAndUnlinkedStaysNoninteractive() {
        withDashboard(Arrays.asList("NCD", "Independent"), Collections.singletonList(option(DrawerMenu.NCD)),
                (activity, rows) -> {
                    assertTrue(rows.getChildAt(0).isClickable());
                    assertTrue(rows.getChildAt(0).isFocusable());
                    assertFalse(rows.getChildAt(1).isClickable());
                    rows.getChildAt(0).performClick();

                    Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
                    assertNotNull(intent);
                    assertEquals(NcdRegisterActivity.class.getName(), intent.getComponent().getClassName());
                });
    }

    @Test
    public void groupedRowWaitsForTheUserToChooseTheDesiredRegister() {
        NavigationOption risk = option(DrawerMenu.HARM_REDUCTION);
        risk.setTitleID(R.string.menu_harm_reduction);
        NavigationOption sober = option(DrawerMenu.HARM_REDUCTION_SOBER_HOUSE);
        sober.setTitleID(R.string.harm_reduction_sober_house);
        withDashboard(Collections.singletonList("HARM REDUCTION"), Arrays.asList(risk, sober), (activity, rows) -> {
            rows.getChildAt(0).performClick();

            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
            AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
            assertNotNull(dialog);
            assertEquals(2, dialog.getListView().getCount());
            assertEquals(activity.getString(risk.getTitleID()), dialog.getListView().getAdapter().getItem(0));
            assertEquals(activity.getString(sober.getTitleID()), dialog.getListView().getAdapter().getItem(1));
            dialog.getListView().performItemClick(null, 1, 1);

            Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
            assertNotNull(intent);
            assertEquals(HarmReductionSoberHouseRegisterActivity.class.getName(), intent.getComponent().getClassName());
        });
    }

    @Test
    public void moduleClickRechecksCurrentRoleOptionsBeforeStartingARegister() {
        List<NavigationOption> available = new ArrayList<>(Collections.singletonList(option(DrawerMenu.NCD)));
        withDashboard(Collections.singletonList("NCD"), available, (activity, rows) -> {
            assertTrue(rows.getChildAt(0).isClickable());
            available.clear();
            rows.getChildAt(0).performClick();

            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
            assertFalse(activity.isFinishing());
        });
    }

    private void withDashboard(List<String> types, List<NavigationOption> available,
                               BiConsumer<FragmentActivity, ViewGroup> assertion) {
        try (MockedConstruction<NavigationModelFlv> model = mockConstruction(NavigationModelFlv.class,
                (mock, context) -> when(mock.getNavigationItems()).thenReturn(available));
             MockedStatic<SyncStatusBroadcastReceiver> receiver = mockStatic(SyncStatusBroadcastReceiver.class)) {
            receiver.when(SyncStatusBroadcastReceiver::getInstance).thenReturn(mock(SyncStatusBroadcastReceiver.class));
            ActivityController<FragmentActivity> controller = Robolectric.buildActivity(FragmentActivity.class);
            FragmentActivity activity = controller.get();
            activity.setTheme(R.style.Theme_AppCompat_NoActionBar);
            controller.setup();
            try {
                TestDashboard fragment = new TestDashboard();
                activity.getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment).commitNow();
                List<AllClientsDashboardRepository.ModuleCount> counts = new ArrayList<>();
                for (String type : types) counts.add(new AllClientsDashboardRepository.ModuleCount(type, 1));
                AllClientsDashboardRepository.Snapshot snapshot = new AllClientsDashboardRepository.Snapshot(
                        types.size(), 0, types.size(), 0, counts, Collections.emptyList());
                ReflectionHelpers.callInstanceMethod(fragment, "bindSnapshot",
                        ReflectionHelpers.ClassParameter.from(View.class, fragment.requireView()),
                        ReflectionHelpers.ClassParameter.from(AllClientsDashboardRepository.Snapshot.class, snapshot));
                assertion.accept(activity, fragment.requireView().findViewById(R.id.dashboard_breakdown));
            } finally {
                controller.pause().stop().destroy();
            }
        }
    }

    private NavigationOption option(String menu) {
        return new NavigationOption(0, 0, R.string.menu_all_clients, menu, 0);
    }

    private void assertOpens(NavigationOption option, Class<?> expectedActivity) {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup();
        try {
            Activity activity = controller.get();
            AllClientsDashboardNavigation.openRegister(activity, new View(activity), option);

            Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
            assertNotNull(option.getMenuTitle(), intent);
            assertNotNull(intent.getComponent());
            assertEquals(expectedActivity.getName(), intent.getComponent().getClassName());
            assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0);
            assertTrue(activity.isFinishing());
        } finally {
            controller.pause().stop().destroy();
        }
    }

    public static class TestDashboard extends AllClientsDashboardFragment {
        @Override
        public void onViewCreated(View view, Bundle state) {
            // This test host has no register toolbar; rendering and module clicks remain real.
        }

        @Override
        public void refreshDashboard() {
            // Bind a known snapshot without starting unrelated database work.
        }
    }
}
