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

import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.R;
import org.smartregister.chw.harmreduction.custom_views.BaseHarmReductionFloatingMenu;
import org.smartregister.chw.harmreduction.domain.MemberObject;
import org.smartregister.chw.harmreduction.fragment.BaseHarmReductionCallDialogFragment;

public class HarmReductionFloatingMenu extends BaseHarmReductionFloatingMenu {
    public FloatingActionButton fab;

    private View activityMain;

    private View callLayout;

    private View referLayout;

    private LinearLayout menuBar;

    private boolean isFabMenuOpen;

    private Animation fabOpen;

    private Animation fabClose;

    private Animation rotateForward;

    private Animation rotateBack;

    private OnClickFloatingMenu onClickFloatingMenu;

    private MemberObject MEMBER_OBJECT;

    public HarmReductionFloatingMenu(Context context, MemberObject MEMBER_OBJECT) {
        super(context, MEMBER_OBJECT);
        this.MEMBER_OBJECT = MEMBER_OBJECT;
    }

    public void setFloatMenuClickListener(OnClickFloatingMenu onClickFloatingMenu) {
        this.onClickFloatingMenu = onClickFloatingMenu;
    }

    @Override
    protected void initUi() {
        inflate(getContext(), org.smartregister.chw.harmreduction.R.layout.view_harm_reduction_floating_menu, this);

        fabOpen = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close);
        rotateForward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_forward);
        rotateBack = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_back);

        activityMain = findViewById(org.smartregister.chw.harmreduction.R.id.harm_reduction_activity_main);
        menuBar = findViewById(org.smartregister.chw.harmreduction.R.id.harm_reduction_menu_bar);

        fab = findViewById(org.smartregister.chw.harmreduction.R.id.harm_reduction_fab);
        fab.setOnClickListener(this);

        callLayout = findViewById(org.smartregister.chw.harmreduction.R.id.harm_reduction_call_layout);
        callLayout.setOnClickListener(this);
        callLayout.setClickable(false);

        referLayout = findViewById(org.smartregister.chw.harmreduction.R.id.harm_reduction_refer_to_facility_layout);
        referLayout.setOnClickListener(this);
        referLayout.setClickable(false);

        View callFab = findViewById(org.smartregister.chw.harmreduction.R.id.harm_reduction_call_fab);
        if (callFab != null) {
            callFab.setOnClickListener(this);
            callFab.setClickable(false);
        }

        View referFab = findViewById(org.smartregister.chw.harmreduction.R.id.harm_reduction_refer_to_facility_fab);
        if (referFab != null) {
            referFab.setOnClickListener(this);
            referFab.setClickable(false);
        }

        if (menuBar != null) {
            menuBar.setVisibility(GONE);
        }
    }

    @Override
    public void onClick(View view) {
        if (onClickFloatingMenu != null) {
            onClickFloatingMenu.onClickMenu(view.getId());
        }
    }

    public void animateFAB() {
        if (isFabMenuOpen) {
            if (activityMain instanceof RelativeLayout) {
                ((RelativeLayout) activityMain).setBackgroundResource(org.smartregister.chw.core.R.color.transparent);
            } else {
                setBackgroundResource(org.smartregister.chw.core.R.color.transparent);
            }
            if (fab != null) {
                fab.startAnimation(rotateBack);
            }

            if (callLayout != null) {
                callLayout.startAnimation(fabClose);
                callLayout.setClickable(false);
            }

            if (referLayout != null) {
                referLayout.startAnimation(fabClose);
                referLayout.setClickable(false);
            }

            if (fab != null) {
                fab.setImageResource(org.smartregister.chw.harmreduction.R.drawable.ic_edit_white);
            }
            if (menuBar != null) {
                menuBar.setVisibility(GONE);
            }
            isFabMenuOpen = false;
        } else {
            if (activityMain instanceof RelativeLayout) {
                ((RelativeLayout) activityMain).setBackgroundResource(org.smartregister.chw.core.R.color.grey_tranparent_50);
            }
            if (fab != null) {
                fab.startAnimation(rotateForward);
                fab.setImageResource(org.smartregister.chw.harmreduction.R.drawable.ic_edit_white);
            }

            if (callLayout != null) {
                callLayout.startAnimation(fabOpen);
                callLayout.setClickable(true);
            }

            if (referLayout != null) {
                referLayout.startAnimation(fabOpen);
                referLayout.setClickable(true);
            }
            if (callLayout != null) {
                callLayout.setVisibility(VISIBLE);
            }
            if (referLayout != null) {
                referLayout.setVisibility(VISIBLE);
            }
            if (menuBar != null) {
                menuBar.setVisibility(VISIBLE);
            }
            isFabMenuOpen = true;
        }
    }

    public void launchCallWidget() {
        BaseHarmReductionCallDialogFragment.launchDialog((Activity) getContext(), MEMBER_OBJECT);
    }

    public void redraw(boolean hasPhoneNumber) {
        redrawWithOption(this, hasPhoneNumber);
    }

    public View getCallLayout() {
        return callLayout;
    }
}
