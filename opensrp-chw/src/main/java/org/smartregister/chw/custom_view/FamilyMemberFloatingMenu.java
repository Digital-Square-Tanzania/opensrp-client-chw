package org.smartregister.chw.custom_view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.custom_views.CoreFamilyMemberFloatingMenu;

public class FamilyMemberFloatingMenu extends CoreFamilyMemberFloatingMenu {
    private Flavor flavor = new FamilyMemberFloatingMenuFlv();

    public FamilyMemberFloatingMenu(Context context) {
        super(context);
        initUi();
    }

    @Override
    public void initUi() {
        flavor = new FamilyMemberFloatingMenuFlv();
        super.initUi();
        flavor.prepareFab(this, fab);

        if (ChwApplication.getApplicationFlavor().hasADDO()) {
            addoLayout.setVisibility(VISIBLE);
        } else {
            addoLayout.setVisibility(GONE);
        }

    }

    @Override
    public void onClick(View v) {
        onClickFloatingMenu.onClickMenu(v.getId());
        flavor.fabInteraction(this);
    }

    @Override
    public void reDraw(boolean has_phone) {
        flavor.reDraw(this, has_phone);
    }

    public FamilyMemberFloatingMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FamilyMemberFloatingMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initUi();
    }

    interface Flavor {
        void reDraw(FamilyMemberFloatingMenu menu, boolean has_phone);

        void prepareFab(FamilyMemberFloatingMenu menu, FloatingActionButton fab);

        void fabInteraction(FamilyMemberFloatingMenu menu);
    }
}
