package org.smartregister.chw.fragment;

import android.view.View;

import androidx.appcompat.widget.Toolbar;

import org.smartregister.chw.activity.MotherMentorProfileActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.model.MotherMentorRegisterFragmentModel;
import org.smartregister.chw.mothermentor.fragment.BaseMotherMentorRegisterFragment;
import org.smartregister.chw.presenter.MotherMentorRegisterFragmentPresenter;

import timber.log.Timber;

public class MotherMentorRegisterFragment extends BaseMotherMentorRegisterFragment {
    private View view;

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new MotherMentorRegisterFragmentPresenter(this, new MotherMentorRegisterFragmentModel(), null);
    }

    @Override
    protected void openProfile(String baseEntityId) {
        MotherMentorProfileActivity.startMe(requireActivity(), baseEntityId);
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        this.view = view;
        setupNavigationMenu(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (view != null) {
            setupNavigationMenu(view);
        }
    }

    private void setupNavigationMenu(View view) {
        Toolbar toolbar = view.findViewById(org.smartregister.R.id.register_toolbar);
        if (toolbar == null) {
            return;
        }

        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.setContentInsetsRelative(0, 0);
        toolbar.setContentInsetStartWithNavigation(0);

        try {
            NavigationMenu.getInstance(getActivity(), null, toolbar);
        } catch (NullPointerException e) {
            Timber.e(e);
        }
    }
}
