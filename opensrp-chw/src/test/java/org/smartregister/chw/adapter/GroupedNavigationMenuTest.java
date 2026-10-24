package org.smartregister.chw.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.smartregister.AllConstants.TEAM_ROLE_IDENTIFIER;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.adapter.NavigationAdapter;
import org.smartregister.chw.core.model.NavigationOption;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.custom_view.NavigationMenuFlv;
import org.smartregister.chw.model.NavigationModelFlv;
import org.smartregister.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class GroupedNavigationMenuTest {

    private ActivityController<Activity> controller;
    private Activity activity;
    private FrameLayout parent;
    private SharedPreferences preferences;
    private ChwApplication.Flavor flavor;
    private ChwApplication.Flavor originalFlavor;
    private CoreLibrary originalCoreLibrary;
    private Context originalContext;

    @Before
    public void setUp() {
        originalFlavor = ChwApplication.getApplicationFlavor();
        originalCoreLibrary = ReflectionHelpers.getStaticField(CoreLibrary.class, "instance");
        originalContext = ReflectionHelpers.getStaticField(Context.class, "context");
        ReflectionHelpers.setStaticField(CoreLibrary.class, "instance", null);
        ReflectionHelpers.setStaticField(Context.class, "context", null);
        Context context = Context.getInstance();
        context.updateApplicationContext(RuntimeEnvironment.getApplication());
        CoreLibrary.init(context);
        preferences = Utils.getAllSharedPreferences().getPreferences();
        preferences.edit().clear().commit();

        flavor = Mockito.mock(ChwApplication.Flavor.class);
        Mockito.when(flavor.hasHps()).thenReturn(true);
        Mockito.when(flavor.hasICCM()).thenReturn(true);
        Mockito.when(flavor.hasMotherMentor()).thenReturn(true);
        ReflectionHelpers.setStaticField(ChwApplication.class, "flavor", flavor);
        NavigationModelFlv.resetNavigationOptions();

        controller = Robolectric.buildActivity(Activity.class).setup();
        activity = controller.get();
        activity.setTheme(org.smartregister.chw.R.style.Theme_AppCompat);
        parent = new FrameLayout(activity);
    }

    @After
    public void tearDown() {
        NavigationModelFlv.resetNavigationOptions();
        ReflectionHelpers.setStaticField(ChwApplication.class, "flavor", originalFlavor);
        ReflectionHelpers.setStaticField(CoreLibrary.class, "instance", originalCoreLibrary);
        ReflectionHelpers.setStaticField(Context.class, "context", originalContext);
        if (controller != null) {
            controller.pause().stop().destroy();
        }
    }

    @Test
    public void buildFlagControlsAvailableDrawerEntries() {
        for (String role : Arrays.asList("icchw", "iccm_provider", "cbhs_provider",
                "mother_champion", "rmncah_provider", "AYP_OUT_OF_SCHOOL", "")) {
            List<NavigationOption> options = optionsForRole(role);
            List<String> originalOrder = menuTitles(options);
            NavigationAdapter adapter = adapterFor(options);

            if (BuildConfig.ENABLE_COLLAPSIBLE_NAVIGATION) {
                List<String> collapsedRows = new ArrayList<>();
                for (String primary : Arrays.asList(CoreConstants.DrawerMenu.ALL_CLIENTS,
                        CoreConstants.DrawerMenu.ALL_FAMILIES, CoreConstants.DrawerMenu.HPS)) {
                    if (originalOrder.contains(primary)) {
                        collapsedRows.add(primary);
                    }
                }
                collapsedRows.add(null);
                assertEquals(collapsedRows, visibleTags(adapter));
                toggleOtherModules(adapter);
                assertExpandedContainsExactly(adapter, originalOrder);
            } else {
                assertEquals(originalOrder, visibleTags(adapter));
                assertFalse(visibleTags(adapter).contains(null));
            }

            NavigationOption selected = options.get(options.size() - 1);
            selected.setRegisterCount(17);
            adapter.notifyDataSetChanged();
            adapter.setSelectedView(selected.getMenuTitle());
            int selectedPosition = visibleTags(adapter).indexOf(selected.getMenuTitle());
            NavigationAdapter.MyViewHolder selectedRow = bind(adapter, selectedPosition);
            assertEquals("17", selectedRow.tvCount.getText().toString());
            assertEquals(activity.getResources().getColor(
                            org.smartregister.chw.core.R.color.navigation_item_selected),
                    selectedRow.tvName.getCurrentTextColor());
            assertEquals(originalOrder, menuTitles(options));
            if (!BuildConfig.ENABLE_COLLAPSIBLE_NAVIGATION) {
                assertEquals(originalOrder, visibleTags(adapter));
            }
        }
    }

    @Test
    public void restrictedRolesRetainOnlyTheirAvailableModules() {
        assertFalse(menuTitles(optionsForRole("mother_champion"))
                .contains(CoreConstants.DrawerMenu.MOTHER_CHAMPION));
        assertRestrictedRole("mother_champion", Arrays.asList(
                CoreConstants.DrawerMenu.ALL_CLIENTS,
                CoreConstants.DrawerMenu.MOTHER_MENTOR,
                CoreConstants.DrawerMenu.NCD,
                CoreConstants.DrawerMenu.REFERRALS,
                CoreConstants.DrawerMenu.UPDATES));
        assertRestrictedRole("cbhs_provider", Arrays.asList(
                CoreConstants.DrawerMenu.ALL_CLIENTS,
                CoreConstants.DrawerMenu.CBHS_CLIENTS,
                CoreConstants.DrawerMenu.HIV_INDEX_CLIENTS,
                CoreConstants.DrawerMenu.REFERRALS,
                CoreConstants.DrawerMenu.UPDATES,
                CoreConstants.DrawerMenu.LTFU));
        assertRestrictedRole("iccm_provider", Arrays.asList(
                CoreConstants.DrawerMenu.ALL_CLIENTS,
                CoreConstants.DrawerMenu.ICCM,
                CoreConstants.DrawerMenu.REFERRALS,
                CoreConstants.DrawerMenu.UPDATES));
        assertRestrictedRole("AYP_OUT_OF_SCHOOL", Arrays.asList(
                CoreConstants.DrawerMenu.ALL_CLIENTS,
                CoreConstants.DrawerMenu.AYP_OUT_SCHOOL,
                CoreConstants.DrawerMenu.HIV_SELF_TESTING,
                CoreConstants.DrawerMenu.UPDATES,
                CoreConstants.DrawerMenu.REFERRALS));
    }

    @Test
    public void enabledMotherMentorIsAvailableOnceInOtherModulesForEligibleRoles() {
        for (String role : Arrays.asList("", "icchw", "mother_champion")) {
            List<NavigationOption> options = optionsForRole(role);
            List<String> originalOrder = menuTitles(options);
            NavigationOption motherMentor =
                    optionFor(options, CoreConstants.DrawerMenu.MOTHER_MENTOR);
            motherMentor.setRegisterCount(9);
            NavigationAdapter adapter = adapterFor(options);

            assertEquals(1, Collections.frequency(originalOrder,
                    CoreConstants.DrawerMenu.MOTHER_MENTOR));
            assertFalse(originalOrder.contains(CoreConstants.DrawerMenu.MOTHER_CHAMPION));
            if (BuildConfig.ENABLE_COLLAPSIBLE_NAVIGATION) {
                assertFalse(visibleTags(adapter).contains(CoreConstants.DrawerMenu.MOTHER_MENTOR));
                toggleOtherModules(adapter);
                assertExpandedContainsExactly(adapter, originalOrder);
            }
            List<String> visible = visibleTags(adapter);
            assertEquals(1, Collections.frequency(visible, CoreConstants.DrawerMenu.MOTHER_MENTOR));
            int position = visible.indexOf(CoreConstants.DrawerMenu.MOTHER_MENTOR);
            assertEquals(activity.getString(org.smartregister.chw.R.string.mother_mentor),
                    bind(adapter, position).tvName.getText().toString());
            assertEquals("9", bind(adapter, position).tvCount.getText().toString());
            assertEquals(originalOrder, menuTitles(options));
        }
    }

    @Test
    public void disabledMotherMentorDoesNotRemoveOtherRoleModulesOrExposeChampion() {
        for (String role : Arrays.asList("", "icchw", "mother_champion")) {
            Mockito.when(flavor.hasMotherMentor()).thenReturn(true);
            List<String> expected = menuTitles(optionsForRole(role));
            expected.remove(CoreConstants.DrawerMenu.MOTHER_MENTOR);
            Mockito.when(flavor.hasMotherMentor()).thenReturn(false);
            List<NavigationOption> options = optionsForRole(role);
            NavigationAdapter adapter = adapterFor(options);

            assertEquals(expected, menuTitles(options));
            assertFalse(menuTitles(options).contains(CoreConstants.DrawerMenu.MOTHER_MENTOR));
            assertFalse(menuTitles(options).contains(CoreConstants.DrawerMenu.MOTHER_CHAMPION));
            if (BuildConfig.ENABLE_COLLAPSIBLE_NAVIGATION) {
                toggleOtherModules(adapter);
                assertExpandedContainsExactly(adapter, expected);
            } else {
                assertEquals(expected, visibleTags(adapter));
            }
            assertFalse(visibleTags(adapter).contains(CoreConstants.DrawerMenu.MOTHER_MENTOR));
        }
        assertEquals(Arrays.asList(
                CoreConstants.DrawerMenu.ALL_CLIENTS,
                CoreConstants.DrawerMenu.NCD,
                CoreConstants.DrawerMenu.REFERRALS,
                CoreConstants.DrawerMenu.UPDATES), menuTitles(optionsForRole("mother_champion")));
    }

    @Test
    public void icchwRoleShowsPrimaryItemsAndKeepsIccmInOtherModules() {
        List<NavigationOption> options = optionsForRole("icchw");
        NavigationAdapter adapter = adapterFor(options);

        assertEquals(Arrays.asList(CoreConstants.DrawerMenu.ALL_CLIENTS,
                CoreConstants.DrawerMenu.ALL_FAMILIES, CoreConstants.DrawerMenu.HPS, null),
                visibleTags(adapter));
        assertEquals("All Clients", bind(adapter, 0).tvName.getText().toString());
        assertEquals("All Households", bind(adapter, 1).tvName.getText().toString());
        assertEquals("iCCHW", bind(adapter, 2).tvName.getText().toString());
        assertEquals("Other Modules", bind(adapter, 3).tvName.getText().toString());

        toggleOtherModules(adapter);

        assertTrue(visibleTags(adapter).indexOf(CoreConstants.DrawerMenu.ICCM) > 3);
        assertExpandedContainsExactly(adapter, menuTitles(options));
    }

    @Test
    public void rmncahRoleKeepsHouseholdsAndIcchwWithoutAddingIccm() {
        List<NavigationOption> options = optionsForRole("rmncah_provider");
        NavigationAdapter adapter = adapterFor(options);

        assertEquals(Arrays.asList(CoreConstants.DrawerMenu.ALL_CLIENTS,
                CoreConstants.DrawerMenu.ALL_FAMILIES, CoreConstants.DrawerMenu.HPS, null),
                visibleTags(adapter));
        assertFalse(menuTitles(options).contains(CoreConstants.DrawerMenu.ICCM));
        toggleOtherModules(adapter);
        assertExpandedContainsExactly(adapter, Arrays.asList(
                CoreConstants.DrawerMenu.ALL_CLIENTS,
                CoreConstants.DrawerMenu.ALL_FAMILIES,
                CoreConstants.DrawerMenu.ANC,
                CoreConstants.DrawerMenu.PNC,
                CoreConstants.DrawerMenu.CHILD_CLIENTS,
                CoreConstants.DrawerMenu.HPS,
                CoreConstants.DrawerMenu.REFERRALS,
                CoreConstants.DrawerMenu.UPDATES));
    }

    @Test
    public void missingRoleKeepsIcchwAndIccmUnavailable() {
        List<NavigationOption> options = optionsForRole("");
        NavigationAdapter adapter = adapterFor(options);

        assertEquals(Arrays.asList(CoreConstants.DrawerMenu.ALL_CLIENTS,
                CoreConstants.DrawerMenu.ALL_FAMILIES, null), visibleTags(adapter));
        assertFalse(menuTitles(options).contains(CoreConstants.DrawerMenu.HPS));
        assertFalse(menuTitles(options).contains(CoreConstants.DrawerMenu.ICCM));
        toggleOtherModules(adapter);
        assertExpandedContainsExactly(adapter, menuTitles(options));
    }

    @Test
    public void disabledIcchwFeaturesDoNotGainPrimaryOrGroupedEntries() {
        Mockito.when(flavor.hasHps()).thenReturn(false);
        Mockito.when(flavor.hasICCM()).thenReturn(false);
        List<NavigationOption> options = optionsForRole("icchw");
        NavigationAdapter adapter = adapterFor(options);

        assertEquals(Arrays.asList(CoreConstants.DrawerMenu.ALL_CLIENTS,
                CoreConstants.DrawerMenu.ALL_FAMILIES, null), visibleTags(adapter));
        toggleOtherModules(adapter);
        assertFalse(visibleTags(adapter).contains(CoreConstants.DrawerMenu.HPS));
        assertFalse(visibleTags(adapter).contains(CoreConstants.DrawerMenu.ICCM));
        assertExpandedContainsExactly(adapter, menuTitles(options));
    }

    @Test
    public void expandingAndCollapsingDoesNotInsertAHeaderIntoCountedModelOptions() {
        List<NavigationOption> options = optionsForRole("icchw");
        List<NavigationOption> originalOptions = new ArrayList<>(options);
        NavigationAdapter adapter = adapterFor(options);

        toggleOtherModules(adapter);
        assertExpandedContainsExactly(adapter, menuTitles(originalOptions));
        toggleOtherModules(adapter);

        assertEquals(originalOptions, options);
        assertSame(options, new NavigationModelFlv().getNavigationItems());
        assertEquals(4, adapter.getItemCount());
        for (NavigationOption option : options) {
            assertTrue(option.getMenuTitle() != null);
        }
    }

    @Test
    public void refreshedCountsRemainVisibleAfterExpandingOtherModules() {
        List<NavigationOption> options = optionsForRole("iccm_provider");
        NavigationAdapter adapter = adapterFor(options);
        optionFor(options, CoreConstants.DrawerMenu.ALL_CLIENTS).setRegisterCount(42);
        optionFor(options, CoreConstants.DrawerMenu.ICCM).setRegisterCount(7);
        adapter.notifyDataSetChanged();

        assertEquals("42", bind(adapter, 0).tvCount.getText().toString());
        assertEquals(2, adapter.getItemCount());
        toggleOtherModules(adapter);
        int iccmPosition = visibleTags(adapter).indexOf(CoreConstants.DrawerMenu.ICCM);
        assertEquals("7", bind(adapter, iccmPosition).tvCount.getText().toString());

        optionFor(options, CoreConstants.DrawerMenu.ICCM).setRegisterCount(11);
        adapter.notifyDataSetChanged();
        assertEquals("11", bind(adapter, iccmPosition).tvCount.getText().toString());
    }

    @Test
    public void refreshedRoleOptionsUpdateAnExistingAdapterWithoutExposingOldModules() {
        List<NavigationOption> options = optionsForRole("icchw");
        NavigationAdapter adapter = adapterFor(options);
        toggleOtherModules(adapter);

        List<NavigationOption> refreshedOptions = optionsForRole("mother_champion");
        assertSame(options, refreshedOptions);
        adapter.notifyDataSetChanged();

        assertExpandedContainsExactly(adapter, menuTitles(refreshedOptions));
        assertFalse(visibleTags(adapter).contains(CoreConstants.DrawerMenu.ALL_FAMILIES));
        assertFalse(visibleTags(adapter).contains(CoreConstants.DrawerMenu.HPS));
        assertFalse(visibleTags(adapter).contains(CoreConstants.DrawerMenu.ICCM));
    }

    @Test
    public void manualCollapseSurvivesRefreshUntilADifferentChildIsSelected() {
        List<NavigationOption> options = optionsForRole("iccm_provider");
        NavigationAdapter adapter = adapterFor(options);
        adapter.setSelectedView(CoreConstants.DrawerMenu.ICCM);
        assertExpandedContainsExactly(adapter, menuTitles(options));

        toggleOtherModules(adapter);
        adapter.notifyDataSetChanged();
        adapter.setSelectedView(CoreConstants.DrawerMenu.ICCM);
        assertEquals(Arrays.asList(CoreConstants.DrawerMenu.ALL_CLIENTS, null),
                visibleTags(adapter));
        assertEquals(CoreConstants.DrawerMenu.ICCM, adapter.getSelectedView());

        adapter.setSelectedView(CoreConstants.DrawerMenu.REFERRALS);
        assertExpandedContainsExactly(adapter, menuTitles(options));
        int selectedPosition = visibleTags(adapter).indexOf(CoreConstants.DrawerMenu.REFERRALS);
        assertEquals(activity.getResources().getColor(
                        org.smartregister.chw.core.R.color.navigation_item_selected),
                bind(adapter, selectedPosition).tvName.getCurrentTextColor());
    }

    @Test
    public void headerAccessibilityActionsExpandAndCollapseOtherModules() {
        List<NavigationOption> options = optionsForRole("iccm_provider");
        NavigationAdapter adapter = adapterFor(options);
        NavigationAdapter.MyViewHolder header = bind(adapter, 1);
        assertEquals("Other Modules, collapsed", header.getView().getContentDescription());
        assertHeaderAction(header, AccessibilityNodeInfoCompat.ACTION_EXPAND,
                "Expand Other Modules");
        assertTrue(ViewCompat.performAccessibilityAction(header.getView(),
                AccessibilityNodeInfoCompat.ACTION_EXPAND, null));
        assertExpandedContainsExactly(adapter, menuTitles(options));

        header = bind(adapter, 1);
        assertEquals("Other Modules, expanded", header.getView().getContentDescription());
        assertHeaderAction(header, AccessibilityNodeInfoCompat.ACTION_COLLAPSE,
                "Collapse Other Modules");
        assertTrue(ViewCompat.performAccessibilityAction(header.getView(),
                AccessibilityNodeInfoCompat.ACTION_COLLAPSE, null));
        assertEquals(2, adapter.getItemCount());
    }

    @Test
    @Config(qualifiers = "sw")
    public void swahiliHeaderHasTranslatedLabelsAndAccessibilityActions() {
        NavigationAdapter adapter = adapterFor(optionsForRole("icchw"));
        assertEquals("Wateja wote", bind(adapter, 0).tvName.getText().toString());
        assertEquals("Kaya Zote", bind(adapter, 1).tvName.getText().toString());
        assertEquals("iCCHW", bind(adapter, 2).tvName.getText().toString());
        NavigationAdapter.MyViewHolder header = bind(adapter, 3);
        assertEquals("Moduli Nyingine", header.tvName.getText().toString());
        assertEquals("Moduli Nyingine, zimekunjwa", header.getView().getContentDescription());
        assertHeaderAction(header, AccessibilityNodeInfoCompat.ACTION_EXPAND,
                "Panua Moduli Nyingine");

        assertTrue(header.getView().performClick());
        header = bind(adapter, 3);
        assertEquals("Moduli Nyingine, zimepanuliwa", header.getView().getContentDescription());
        assertHeaderAction(header, AccessibilityNodeInfoCompat.ACTION_COLLAPSE,
                "Kunja Moduli Nyingine");

        int motherMentorPosition = visibleTags(adapter)
                .indexOf(CoreConstants.DrawerMenu.MOTHER_MENTOR);
        NavigationAdapter.MyViewHolder motherMentor = bind(adapter, motherMentorPosition);
        assertEquals("Mama Kinara", motherMentor.tvName.getText().toString());
        assertEquals(CoreConstants.DrawerMenu.MOTHER_MENTOR, motherMentor.getView().getTag());
    }

    @Test
    @Config(qualifiers = "w1000dp-h1400dp")
    public void wideLayoutBindsPrimaryHeaderAndIndentedChildRows() {
        NavigationAdapter adapter = adapterFor(optionsForRole("icchw"));
        NavigationAdapter.MyViewHolder primary = bind(adapter, 0);
        NavigationAdapter.MyViewHolder header = bind(adapter, 3);
        assertTrue(primary.getView() instanceof RelativeLayout);
        assertEquals("All Clients", primary.tvName.getText().toString());
        assertEquals(24f * activity.getResources().getDisplayMetrics().scaledDensity,
                header.tvName.getTextSize(), 0.01f);

        toggleOtherModules(adapter);
        int iccmPosition = visibleTags(adapter).indexOf(CoreConstants.DrawerMenu.ICCM);
        NavigationAdapter.MyViewHolder child = bind(adapter, iccmPosition);
        assertTrue(child.getView() instanceof RelativeLayout);
        assertEquals(CoreConstants.DrawerMenu.ICCM, child.getView().getTag());
        assertEquals("0", child.tvCount.getText().toString());
        assertTrue(child.getView().getPaddingStart() > primary.getView().getPaddingStart());
    }

    private void assertHeaderAction(NavigationAdapter.MyViewHolder header, int actionId,
                                    String expectedLabel) {
        AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.wrap(
                header.getView().createAccessibilityNodeInfo());
        try {
            assertEquals("android.widget.Button", info.getClassName().toString());
            for (AccessibilityNodeInfoCompat.AccessibilityActionCompat action : info.getActionList()) {
                if (action.getId() == actionId) {
                    assertEquals(expectedLabel, action.getLabel().toString());
                    return;
                }
            }
            throw new AssertionError("Missing accessibility action: " + expectedLabel);
        } finally {
            info.recycle();
        }
    }

    private void assertRestrictedRole(String role, List<String> expectedModules) {
        List<NavigationOption> options = optionsForRole(role);
        NavigationAdapter adapter = adapterFor(options);
        assertEquals(expectedModules, menuTitles(options));
        assertEquals(Arrays.asList(CoreConstants.DrawerMenu.ALL_CLIENTS, null),
                visibleTags(adapter));
        toggleOtherModules(adapter);
        assertExpandedContainsExactly(adapter, expectedModules);
        assertFalse(visibleTags(adapter).contains(CoreConstants.DrawerMenu.ALL_FAMILIES));
        assertFalse(visibleTags(adapter).contains(CoreConstants.DrawerMenu.HPS));
    }

    private List<NavigationOption> optionsForRole(String role) {
        preferences.edit().putString(TEAM_ROLE_IDENTIFIER, role).commit();
        return NavigationModelFlv.refreshNavigationOptions();
    }

    private NavigationAdapter adapterFor(List<NavigationOption> options) {
        return new NavigationAdapter(options, activity, Collections.<String, Class>emptyMap(),
                new NavigationMenuFlv().getPrimaryNavigationItems());
    }

    private NavigationAdapter.MyViewHolder bind(NavigationAdapter adapter, int position) {
        NavigationAdapter.MyViewHolder holder =
                adapter.onCreateViewHolder(parent, adapter.getItemViewType(position));
        adapter.onBindViewHolder(holder, position);
        return holder;
    }

    private List<String> visibleTags(NavigationAdapter adapter) {
        List<String> tags = new ArrayList<>();
        for (int position = 0; position < adapter.getItemCount(); position++) {
            tags.add((String) bind(adapter, position).getView().getTag());
        }
        return tags;
    }

    private void toggleOtherModules(NavigationAdapter adapter) {
        int headerPosition = visibleTags(adapter).indexOf(null);
        assertTrue("Expected an Other Modules header", headerPosition >= 0);
        NavigationAdapter.MyViewHolder header = bind(adapter, headerPosition);
        assertNull(header.getView().getTag());
        assertTrue(header.getView().performClick());
    }

    private void assertExpandedContainsExactly(NavigationAdapter adapter, List<String> expected) {
        List<String> visibleModules = visibleTags(adapter);
        assertEquals(1, Collections.frequency(visibleModules, null));
        visibleModules.remove(null);
        assertEquals(expected.size(), visibleModules.size());
        assertEquals(new HashSet<>(expected), new HashSet<>(visibleModules));
    }

    private List<String> menuTitles(List<NavigationOption> options) {
        List<String> titles = new ArrayList<>();
        for (NavigationOption option : options) {
            titles.add(option.getMenuTitle());
        }
        return titles;
    }

    private NavigationOption optionFor(List<NavigationOption> options, String menuTitle) {
        for (NavigationOption option : options) {
            if (menuTitle.equals(option.getMenuTitle())) {
                return option;
            }
        }
        throw new AssertionError("Missing navigation option: " + menuTitle);
    }
}
