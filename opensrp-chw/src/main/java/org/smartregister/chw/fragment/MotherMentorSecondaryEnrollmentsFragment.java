package org.smartregister.chw.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.MotherMentorRegisterActivity;
import org.smartregister.chw.adapter.MotherMentorSecondaryEnrollmentPagerAdapter;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.view.customcontrols.CustomFontTextView;
import org.smartregister.view.customcontrols.FontVariant;

import timber.log.Timber;

public class MotherMentorSecondaryEnrollmentsFragment extends Fragment {
    private MotherMentorSecondaryEnrollmentPagerAdapter adapter;
    private View rootView;
    private ViewPager viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mothermentor_secondary_enrollments, container, false);
        rootView = view;
        setupToolbar(view);
        setupSearchBar(view);

        viewPager = view.findViewById(R.id.mothermentor_secondary_view_pager);
        adapter = new MotherMentorSecondaryEnrollmentPagerAdapter(getChildFragmentManager(), requireContext());
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(2);

        TabLayout tabLayout = view.findViewById(R.id.mothermentor_secondary_tabs);
        tabLayout.setupWithViewPager(viewPager);
        applyPendingTabIndex();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (rootView != null) {
            setupToolbar(rootView);
        }
        applyPendingTabIndex();
    }

    private void setupToolbar(View view) {
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

        ImageView qrCodeScanImageView = view.findViewById(org.smartregister.R.id.scanQrCode);
        if (qrCodeScanImageView != null) {
            qrCodeScanImageView.setVisibility(View.GONE);
        }

        ImageView logo = view.findViewById(org.smartregister.R.id.opensrp_logo_image_view);
        if (logo != null) {
            logo.setVisibility(View.GONE);
        }

        CustomFontTextView titleView = view.findViewById(org.smartregister.R.id.txt_title_label);
        if (titleView != null) {
            titleView.setVisibility(View.VISIBLE);
            titleView.setText(getString(R.string.mothermentor_follow_up));
            titleView.setFontVariant(FontVariant.REGULAR);
        }
    }

    private void setupSearchBar(View view) {
        View searchBarLayout = view.findViewById(R.id.search_bar_layout);
        if (searchBarLayout != null) {
            searchBarLayout.setBackgroundResource(org.smartregister.chw.mothermentor.R.color.customAppThemeBlue);
        }

        View filterSortLayout = view.findViewById(R.id.filter_sort_layout);
        if (filterSortLayout != null) {
            filterSortLayout.setVisibility(View.GONE);
        }

        View dueOnlyLayout = view.findViewById(R.id.due_only_layout);
        if (dueOnlyLayout != null) {
            dueOnlyLayout.setVisibility(View.GONE);
        }

        EditText searchView = view.findViewById(R.id.edt_search);
        View searchCancelView = view.findViewById(R.id.btn_search_cancel);
        if (searchView != null) {
            searchView.setBackgroundResource(R.color.white);
            searchView.setCompoundDrawablesWithIntrinsicBounds(org.smartregister.chw.mothermentor.R.drawable.ic_action_search, 0, 0, 0);
            searchView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Not required
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchCancelView != null) {
                        searchCancelView.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                    }
                    if (adapter != null) {
                        adapter.setSearchText(s.toString());
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // Not required
                }
            });
        }

        if (searchCancelView != null && searchView != null) {
            searchCancelView.setOnClickListener(v -> searchView.setText(""));
        }
    }

    private void applyPendingTabIndex() {
        if (viewPager == null || getActivity() == null || getActivity().getIntent() == null
                || !getActivity().getIntent().hasExtra(MotherMentorRegisterActivity.EXTRA_SECONDARY_ENROLLMENT_TAB_INDEX)) {
            return;
        }

        viewPager.setCurrentItem(getInitialTabIndex(), false);
        getActivity().getIntent().removeExtra(MotherMentorRegisterActivity.EXTRA_SECONDARY_ENROLLMENT_TAB_INDEX);
    }

    private int getInitialTabIndex() {
        if (getActivity() == null || getActivity().getIntent() == null) {
            return MotherMentorRegisterActivity.TAB_INDEX_IIT;
        }

        int tabIndex = getActivity().getIntent().getIntExtra(
                MotherMentorRegisterActivity.EXTRA_SECONDARY_ENROLLMENT_TAB_INDEX,
                MotherMentorRegisterActivity.TAB_INDEX_IIT);
        if (tabIndex < MotherMentorRegisterActivity.TAB_INDEX_IIT || tabIndex > MotherMentorRegisterActivity.TAB_INDEX_CHILD_EID) {
            return MotherMentorRegisterActivity.TAB_INDEX_IIT;
        }
        return tabIndex;
    }
}
