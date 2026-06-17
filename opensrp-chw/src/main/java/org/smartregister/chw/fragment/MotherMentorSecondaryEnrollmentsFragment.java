package org.smartregister.chw.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.smartregister.chw.R;
import org.smartregister.chw.adapter.MotherMentorSecondaryEnrollmentPagerAdapter;

public class MotherMentorSecondaryEnrollmentsFragment extends Fragment {
    private MotherMentorSecondaryEnrollmentPagerAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mothermentor_secondary_enrollments, container, false);
        setupSearchBar(view);

        ViewPager viewPager = view.findViewById(R.id.mothermentor_secondary_view_pager);
        adapter = new MotherMentorSecondaryEnrollmentPagerAdapter(getChildFragmentManager(), requireContext());
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(2);

        TabLayout tabLayout = view.findViewById(R.id.mothermentor_secondary_tabs);
        tabLayout.setupWithViewPager(viewPager);

        return view;
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
}
