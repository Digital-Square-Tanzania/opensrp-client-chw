package org.smartregister.chw.custom_view;

import android.content.Context;
import android.util.AttributeSet;

import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.custom_views.CoreAncFloatingMenu;

public class AncFloatingMenu extends CoreAncFloatingMenu {

    public AncFloatingMenu(Context context, String ancWomanName, String ancWomanPhone, String ancFamilyHeadName, String ancFamilyHeadPhone, String profileType) {
        super(context, ancWomanName, ancWomanPhone, ancFamilyHeadName, ancFamilyHeadPhone, profileType);
    }

    public AncFloatingMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void initUi() {
        super.initUi();
        this.referLayout.setVisibility(ChwApplication.getApplicationFlavor().hasReferrals() ? VISIBLE : GONE);

        if (ChwApplication.getApplicationFlavor().hasADDO()) {
            linkageLayout.setVisibility(VISIBLE);
        } else {
            linkageLayout.setVisibility(GONE);
        }
    }

}
