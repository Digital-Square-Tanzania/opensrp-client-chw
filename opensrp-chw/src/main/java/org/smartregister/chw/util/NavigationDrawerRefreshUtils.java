package org.smartregister.chw.util;

import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.model.NavigationModelFlv;

import java.lang.reflect.Field;

import timber.log.Timber;

public final class NavigationDrawerRefreshUtils {

    private static final String NAVIGATION_MENU_INSTANCE_FIELD = "instance";

    private NavigationDrawerRefreshUtils() {
    }

    public static void refreshNavigationDrawer() {
        try {
            NavigationModelFlv.refreshNavigationOptions();

            NavigationMenu navigationMenu = getCurrentNavigationMenu();
            if (navigationMenu == null || navigationMenu.getNavigationAdapter() == null) {
                return;
            }

            navigationMenu.refreshCount();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    static NavigationMenu getCurrentNavigationMenu() throws NoSuchFieldException, IllegalAccessException {
        Field instanceField = NavigationMenu.class.getDeclaredField(NAVIGATION_MENU_INSTANCE_FIELD);
        instanceField.setAccessible(true);
        return (NavigationMenu) instanceField.get(null);
    }
}
