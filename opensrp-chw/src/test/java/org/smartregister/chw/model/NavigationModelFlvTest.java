package org.smartregister.chw.model;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.application.ChwApplicationFlv;
import org.smartregister.chw.core.model.NavigationOption;
import org.smartregister.chw.core.utils.CoreConstants;

import java.util.List;

public class NavigationModelFlvTest extends BaseUnitTest {

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(ChwApplication.class, "flavor", new ChwApplicationFlv());
        NavigationModelFlv.resetNavigationOptions();
    }

    @Test
    public void refreshNavigationOptionsRebuildsAddoItemVisibility() {
        ChwApplicationFlv flavor = Mockito.spy(new ChwApplicationFlv());
        ReflectionHelpers.setStaticField(ChwApplication.class, "flavor", flavor);

        Mockito.doReturn(false).when(flavor).hasADDO();
        List<NavigationOption> optionsWithoutAddo = NavigationModelFlv.refreshNavigationOptions();
        Assert.assertFalse(hasMenuOption(optionsWithoutAddo, CoreConstants.DrawerMenu.ADDO_LINKAGE));

        Mockito.doReturn(true).when(flavor).hasADDO();
        List<NavigationOption> optionsWithAddo = NavigationModelFlv.refreshNavigationOptions();
        Assert.assertTrue(hasMenuOption(optionsWithAddo, CoreConstants.DrawerMenu.ADDO_LINKAGE));
    }

    private boolean hasMenuOption(List<NavigationOption> navigationOptions, String menuTitle) {
        for (NavigationOption navigationOption : navigationOptions) {
            if (menuTitle.equals(navigationOption.getMenuTitle())) {
                return true;
            }
        }
        return false;
    }
}
