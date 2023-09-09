package org.smartregister.chw.model;

import static org.smartregister.AllConstants.TEAM_ROLE_IDENTIFIER;

import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.model.NavigationModel;
import org.smartregister.chw.core.model.NavigationOption;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.util.Constants;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NavigationModelFlv implements NavigationModel.Flavor {

    private static final List<NavigationOption> navigationOptions = new ArrayList<>();

    @Override
    public List<NavigationOption> getNavigationItems() {
        if (navigationOptions.size() == 0) {
            NavigationOption op1 = new NavigationOption(R.mipmap.sidemenu_families, R.mipmap.sidemenu_families_active, R.string.menu_all_families, Constants.DrawerMenu.ALL_FAMILIES, 0);
            NavigationOption op2 = new NavigationOption(R.mipmap.sidemenu_children, R.mipmap.sidemenu_children_active, R.string.menu_child_clients, Constants.DrawerMenu.CHILD_CLIENTS, 0);

            NavigationOption op3 = new NavigationOption(R.mipmap.sidemenu_anc, R.mipmap.sidemenu_anc_active, R.string.menu_anc, Constants.DrawerMenu.ANC, 0);
            NavigationOption op5 = new NavigationOption(R.mipmap.sidemenu_pnc, R.mipmap.sidemenu_pnc_active, R.string.menu_pnc, Constants.DrawerMenu.PNC, 0);
            NavigationOption op6 = new NavigationOption(R.mipmap.sidemenu_fp, R.mipmap.sidemenu_fp_active, R.string.menu_family_planing, Constants.DrawerMenu.FAMILY_PLANNING, 0);
            NavigationOption op7 = new NavigationOption(R.mipmap.sidemenu_malaria, R.mipmap.sidemenu_malaria_active, R.string.menu_malaria, Constants.DrawerMenu.MALARIA, 0);
            NavigationOption op8 = new NavigationOption(R.mipmap.sidemenu_referrals, R.mipmap.sidemenu_referrals_active, R.string.menu_referrals, Constants.DrawerMenu.REFERRALS, 0);
            NavigationOption op9 = new NavigationOption(R.mipmap.sidemenu_updates, R.mipmap.sidemenu_updates_active, R.string.updates, CoreConstants.DrawerMenu.UPDATES, 0);
            NavigationOption op10 = new NavigationOption(R.drawable.sidemenu_all_clients, R.drawable.sidemenu_all_clients_active, R.string.menu_all_clients, CoreConstants.DrawerMenu.ALL_CLIENTS, 0);
            NavigationOption op11 = new NavigationOption(R.mipmap.sidemenu_hiv, R.mipmap.sidemenu_hiv_active, R.string.menu_hiv, CoreConstants.DrawerMenu.CBHS_CLIENTS, 0);
            NavigationOption op12 = new NavigationOption(R.mipmap.sidemenu_hiv, R.mipmap.sidemenu_hiv_active, R.string.menu_hiv_index_contacts, CoreConstants.DrawerMenu.HIV_INDEX_CLIENTS, 0);
            NavigationOption op13 = new NavigationOption(R.drawable.sidemenu_pmtct, R.drawable.sidemenu_pmtct_active, R.string.mother_champion, CoreConstants.DrawerMenu.MOTHER_CHAMPION, 0);
            NavigationOption op14 = new NavigationOption(R.mipmap.sidemenu_tb, R.mipmap.sidemenu_tb_active, R.string.menu_tb, CoreConstants.DrawerMenu.TB_CLIENTS, 0);
            NavigationOption op15 = new NavigationOption(R.mipmap.sidemenu_referrals, R.mipmap.sidemenu_referrals_active, R.string.menu_ltfu, Constants.DrawerMenu.LTFU, 0);
            NavigationOption op16 = new NavigationOption(R.mipmap.sidemenu_hiv, R.mipmap.sidemenu_hiv_active, R.string.menu_hivst, CoreConstants.DrawerMenu.HIV_SELF_TESTING, 0);
            NavigationOption op17 = new NavigationOption(R.mipmap.sidemenu_hiv, R.mipmap.sidemenu_hiv_active, R.string.menu_cdp, CoreConstants.DrawerMenu.CDP, 0);
            NavigationOption op18 = new NavigationOption(R.mipmap.sidemenu_hiv, R.mipmap.sidemenu_hiv_active, R.string.menu_kvp, CoreConstants.DrawerMenu.KVP_PrEP, 0);
            NavigationOption op19 = new NavigationOption(R.mipmap.sidemenu_hiv, R.mipmap.sidemenu_hiv_active, R.string.menu_AGYW, CoreConstants.DrawerMenu.AGYW, 0);
            NavigationOption op20 = new NavigationOption(R.mipmap.sidemenu_updates, R.mipmap.sidemenu_updates_active, R.string.sbc, CoreConstants.DrawerMenu.SBC, 0);
            NavigationOption op21 = new NavigationOption(R.mipmap.sidemenu_updates, R.mipmap.sidemenu_updates_active, R.string.sbc_monthly_social_media_report, CoreConstants.DrawerMenu.SBC_MONTHLY_SOCIAL_MEDIA_REPORT, 0);

            if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH && BuildConfig.BUILD_FOR_BORESHA_AFYA_SOUTH) {
                AllSharedPreferences allSharedPreferences = Utils.getAllSharedPreferences();
                String teamRoleIdentifier = allSharedPreferences.getPreferences().getString(TEAM_ROLE_IDENTIFIER, "");
                if (teamRoleIdentifier != null && teamRoleIdentifier.equals("mother_champion")) {
                    navigationOptions.addAll(Arrays.asList(op10, op13, op8));
                } else if (teamRoleIdentifier != null && teamRoleIdentifier.equals("cbhs_provider")) {
                    navigationOptions.addAll(Arrays.asList(op10, op11, op12, op8, op15));
                } else {
                    navigationOptions.addAll(Arrays.asList(op10, op1, op11, op12, op3, op5, op2, op13, op8, op15));
                }
                if (ChwApplication.getApplicationFlavor().hasHIVST()) {
                    navigationOptions.add(3, op16);
                }
                if (ChwApplication.getApplicationFlavor().hasCdp()) {
                    navigationOptions.add(4, op17);
                }
                if (ChwApplication.getApplicationFlavor().hasKvp()) {
                    navigationOptions.add(5, op18);
                }
                if (ChwApplication.getApplicationFlavor().hasSbc()) {
                    navigationOptions.add(2, op21);
                    navigationOptions.add(2, op20);
                }
            } else {
                navigationOptions.addAll(Arrays.asList(op1, op3, op5, op2, op6, op7));
                if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH)
                    navigationOptions.add(op8);
                navigationOptions.add(op9);
            }
        }

        return navigationOptions;
    }
}
