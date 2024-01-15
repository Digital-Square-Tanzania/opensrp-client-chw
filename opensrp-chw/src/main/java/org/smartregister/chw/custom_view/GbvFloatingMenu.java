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
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.gbv.custom_views.BaseGbvFloatingMenu;
import org.smartregister.chw.gbv.domain.MemberObject;
import org.smartregister.chw.gbv.fragment.BaseGbvCallDialogFragment;

public class GbvFloatingMenu extends BaseGbvFloatingMenu {
    public FloatingActionButton fab;

    private Animation fabOpen;

    private Animation fabClose;

    private Animation rotateForward;

    private Animation rotateBack;

    private View callLayout;

    private View referLayout;

    private RelativeLayout activityMain;

    private boolean isFabMenuOpen = false;

    private LinearLayout menuBar;

    private OnClickFloatingMenu onClickFloatingMenu;

    private MemberObject MEMBER_OBJECT;


    public GbvFloatingMenu(Context context, MemberObject MEMBER_OBJECT) {
        super(context, MEMBER_OBJECT);
        this.MEMBER_OBJECT = MEMBER_OBJECT;
    }

    public void setFloatMenuClickListener(OnClickFloatingMenu onClickFloatingMenu) {
        this.onClickFloatingMenu = onClickFloatingMenu;
    }

    @Override
    protected void initUi() {
        inflate(getContext(), R.layout.view_gbv_floating_menu, this);

        fabOpen = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close);
        rotateForward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_forward);
        rotateBack = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_back);

        activityMain = findViewById(R.id.activity_main);
        menuBar = findViewById(R.id.menu_bar);

        fab = findViewById(R.id.gbv_fab);
        fab.setOnClickListener(this);

        callLayout = findViewById(R.id.gbv_call_layout);
        callLayout.setOnClickListener(this);
        callLayout.setClickable(false);

        referLayout = findViewById(R.id.gbv_refer_to_facility_layout);
        referLayout.setOnClickListener(this);
        referLayout.setClickable(false);


        menuBar.setVisibility(GONE);

    }

    @Override
    public void onClick(View view) {
        onClickFloatingMenu.onClickMenu(view.getId());
    }

    public void animateFAB() {
        menuBar.setVisibility(VISIBLE);
        fab.startAnimation(rotateForward);

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

            fab.setImageResource(R.drawable.ic_edit_white);

            callLayout.startAnimation(fabOpen);
            callLayout.setClickable(true);

            referLayout.startAnimation(fabOpen);
            referLayout.setClickable(true);
            isFabMenuOpen = true;
        }
    }


    public void launchCallWidget() {
        BaseGbvCallDialogFragment.launchDialog((Activity) this.getContext(), MEMBER_OBJECT);
    }

    public void redraw(boolean hasPhoneNumber) {
        redrawWithOption(this, hasPhoneNumber);
    }

    public View getCallLayout() {
        return callLayout;
    }
}