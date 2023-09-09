package org.smartregister.chw.custom_view;

import static org.smartregister.chw.core.utils.Utils.redrawWithOption;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.smartregister.chw.core.R;
import org.smartregister.chw.core.fragment.FamilyCallDialogFragment;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.pmtct.custom_views.BasePmtctFloatingMenu;
import org.smartregister.chw.pmtct.domain.MemberObject;

public class MotherChampionFloatingMenu extends BasePmtctFloatingMenu {
    public FloatingActionButton fab;
    protected View referLayout;
    protected View callLayout;
    protected RelativeLayout activityMain;
    protected boolean isFabMenuOpen = false;
    protected LinearLayout menuBar;
    protected OnClickFloatingMenu onClickFloatingMenu;
    private Animation fabOpen;
    private Animation fabClose;
    private Animation rotateForward;
    private Animation rotateBack;
    private MemberObject MEMBER_OBJECT;

    public MotherChampionFloatingMenu(Context context, MemberObject MEMBER_OBJECT) {
        super(context, MEMBER_OBJECT);
        this.MEMBER_OBJECT = MEMBER_OBJECT;
    }

    public void setFloatMenuClickListener(OnClickFloatingMenu onClickFloatingMenu) {
        this.onClickFloatingMenu = onClickFloatingMenu;
    }

    @Override
    protected void initUi() {
        inflate(getContext(), R.layout.view_anc_call_woma_floating_menu, this);
        fabOpen = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close);
        rotateForward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_forward);
        rotateBack = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_back);

        activityMain = findViewById(R.id.activity_main);
        menuBar = findViewById(R.id.menu_bar);

        fab = findViewById(R.id.anc_fab);
        fab.setOnClickListener(this);

        callLayout = findViewById(R.id.call_layout);
        callLayout.setOnClickListener(this);
        callLayout.setClickable(false);

        referLayout = findViewById(R.id.refer_to_facility_layout);
        referLayout.setOnClickListener(this);
        referLayout.setClickable(false);

        menuBar.setVisibility(GONE);
    }

    @Override
    public void onClick(View view) {
        onClickFloatingMenu.onClickMenu(view.getId());
    }

    public void animateFAB() {
        if (menuBar.getVisibility() == GONE) {
            menuBar.setVisibility(VISIBLE);
        }

        if (isFabMenuOpen) {
            activityMain.setBackgroundResource(R.color.transparent);
            fab.startAnimation(rotateBack);
            fab.setImageResource(R.drawable.ic_edit_white);

            callLayout.startAnimation(fabClose);
            callLayout.setClickable(false);

            referLayout.startAnimation(fabClose);
            referLayout.setClickable(false);
            isFabMenuOpen = false;
        } else {
            activityMain.setBackgroundResource(R.color.grey_tranparent_50);
            fab.startAnimation(rotateForward);
            fab.setImageResource(R.drawable.ic_input_add);

            callLayout.startAnimation(fabOpen);
            callLayout.setClickable(true);

            referLayout.startAnimation(fabOpen);
            referLayout.setClickable(true);
            isFabMenuOpen = true;
        }
    }

    public void launchCallWidget() {
        FamilyCallDialogFragment.launchDialog((Activity) this.getContext(), MEMBER_OBJECT.getFamilyBaseEntityId());
    }

    public void redraw(boolean hasPhoneNumber) {
        redrawWithOption(this, hasPhoneNumber);
    }

    public View getCallLayout() {
        return callLayout;
    }
}
