package org.smartregister.chw.model;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.smartregister.AllConstants.TEAM_ROLE_IDENTIFIER;

import android.app.Application;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.application.ChwApplication.Flavor;
import org.smartregister.chw.core.model.NavigationOption;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorNavigationTest {
    @After
    public void clearNavigationCache() {
        NavigationModelFlv.resetNavigationOptions();
    }

    @Test
    public void defaultMenuReplacesChampionAndReusesBothIcons() {
        assertMotherMentorMenu("", true, 1);
    }

    @Test
    public void generalProviderMenuContainsOnlyOneMotherMentorEntry() {
        assertMotherMentorMenu("icchw", true, 1);
    }

    @Test
    public void motherChampionRoleCanOpenMotherMentor() {
        assertMotherMentorMenu("mother_champion", true, 1);
    }

    @Test
    public void disabledModuleIsHiddenForDefaultAndMotherChampionRoles() {
        assertMotherMentorMenu("", false, 0);
        assertMotherMentorMenu("icchw", false, 0);
        assertMotherMentorMenu("mother_champion", false, 0);
    }

    @Test
    public void unrelatedRestrictedRolesKeepTheirExistingMenu() {
        assertMotherMentorMenu("cbhs_provider", true, 0);
    }

    private void assertMotherMentorMenu(String role, boolean enabled, int expectedCount) {
        SharedPreferences preferences = mock(SharedPreferences.class);
        when(preferences.getString(TEAM_ROLE_IDENTIFIER, "")).thenReturn(role);
        AllSharedPreferences sharedPreferences = mock(AllSharedPreferences.class);
        when(sharedPreferences.getPreferences()).thenReturn(preferences);
        Flavor flavor = mock(Flavor.class);
        when(flavor.hasMotherMentor()).thenReturn(enabled);
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
                MockedStatic<ChwApplication> application = mockStatic(ChwApplication.class)) {
            utils.when(Utils::getAllSharedPreferences).thenReturn(sharedPreferences);
            application.when(ChwApplication::getApplicationFlavor).thenReturn(flavor);
            List<NavigationOption> options = NavigationModelFlv.refreshNavigationOptions();
            int count = 0;
            for (NavigationOption option : options) {
                assertNotEquals(CoreConstants.DrawerMenu.MOTHER_CHAMPION, option.getMenuTitle());
                if (CoreConstants.DrawerMenu.MOTHER_MENTOR.equals(option.getMenuTitle())) {
                    count++;
                    assertEquals(R.string.mother_mentor, option.getTitleID());
                    assertEquals(R.drawable.sidemenu_pmtct, option.getResourceID());
                    assertEquals(R.drawable.sidemenu_pmtct_active, option.getResourceActiveID());
                }
            }
            assertEquals(expectedCount, count);
        }
    }
}
