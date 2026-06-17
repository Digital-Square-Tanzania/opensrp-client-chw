package org.smartregister.chw.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.smartregister.chw.R;
import org.smartregister.chw.adapter.MotherMentorSecondaryEnrollmentPagerAdapter;

public class MotherMentorSecondaryEnrollmentsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mothermentor_secondary_enrollments, container, false);

        ViewPager viewPager = view.findViewById(R.id.mothermentor_secondary_view_pager);
        viewPager.setAdapter(new MotherMentorSecondaryEnrollmentPagerAdapter(getChildFragmentManager(), requireContext()));
        viewPager.setOffscreenPageLimit(2);

        TabLayout tabLayout = view.findViewById(R.id.mothermentor_secondary_tabs);
        tabLayout.setupWithViewPager(viewPager);

        return view;
    }
}
