package org.smartregister.chw.util;

import android.app.Activity;
import android.view.View;

import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.adapter.NavigationAdapter;
import org.smartregister.chw.core.listener.NavigationListener;
import org.smartregister.chw.core.model.NavigationOption;
import org.smartregister.chw.core.utils.CoreConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Resolves dashboard categories to the same role-available destinations as the drawer. */
public final class AllClientsDashboardNavigation {
    private static final String[][] MODULE_MENUS = {
            {"Mother Mentor", CoreConstants.DrawerMenu.MOTHER_MENTOR},
            {"ANC", CoreConstants.DrawerMenu.ANC},
            {"PNC", CoreConstants.DrawerMenu.PNC},
            {"Child", CoreConstants.DrawerMenu.CHILD_CLIENTS},
            {"CBHS", CoreConstants.DrawerMenu.CBHS_CLIENTS},
            {"iCCHW", CoreConstants.DrawerMenu.HPS},
            {"iCCM", CoreConstants.DrawerMenu.ICCM},
            {"Malaria", CoreConstants.DrawerMenu.MALARIA},
            {"TB", CoreConstants.DrawerMenu.TB_CLIENTS},
            {"Family Planning", CoreConstants.DrawerMenu.FAMILY_PLANNING},
            {"AGYW", CoreConstants.DrawerMenu.AGYW},
            {"KVP/PrEP", CoreConstants.DrawerMenu.KVP_PrEP},
            {"TB/Leprosy", CoreConstants.DrawerMenu.TBLEPROSY},
            {"HARM REDUCTION", CoreConstants.DrawerMenu.HARM_REDUCTION,
                    CoreConstants.DrawerMenu.HARM_REDUCTION_SOBER_HOUSE},
            {"AYP in school", CoreConstants.DrawerMenu.AYP_IN_SCHOOL},
            {"AYP out of school", CoreConstants.DrawerMenu.AYP_OUT_SCHOOL},
            {"AYP parental", CoreConstants.DrawerMenu.AYP_PARENTAL},
            {"Reproductive Cancers", CoreConstants.DrawerMenu.CECAP},
            {"AYSRH", CoreConstants.DrawerMenu.AYSRH},
            {"SBC", CoreConstants.DrawerMenu.SBC},
            {"NCD", CoreConstants.DrawerMenu.NCD},
            {"HIVST", CoreConstants.DrawerMenu.HIV_SELF_TESTING}
    };

    private AllClientsDashboardNavigation() {
    }

    public static List<NavigationOption> getOptions(String registerType,
                                                   List<NavigationOption> availableOptions) {
        List<NavigationOption> result = new ArrayList<>();
        List<String> menus = menuTitles(registerType);
        for (NavigationOption option : availableOptions) {
            if (menus.contains(option.getMenuTitle())) result.add(option);
        }
        return result;
    }

    public static boolean isAvailable(String registerType, Set<String> availableMenus) {
        return !Collections.disjoint(menuTitles(registerType), availableMenus);
    }

    private static List<String> menuTitles(String registerType) {
        for (String[] mapping : MODULE_MENUS) {
            if (mapping[0].equals(registerType)) {
                return Arrays.asList(mapping).subList(1, mapping.length);
            }
        }
        return Collections.emptyList();
    }

    public static void openRegister(Activity activity, View row, NavigationOption option) {
        NavigationAdapter adapter = new NavigationAdapter(Collections.singletonList(option), activity,
                ((ChwApplication) ChwApplication.getInstance()).getRegisteredActivities());
        row.setTag(option.getMenuTitle());
        new NavigationListener(activity, adapter).onClick(row);
    }
}
