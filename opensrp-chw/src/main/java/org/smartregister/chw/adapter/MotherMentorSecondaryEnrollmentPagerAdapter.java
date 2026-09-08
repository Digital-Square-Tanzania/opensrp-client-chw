package org.smartregister.chw.adapter;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import org.smartregister.chw.R;
import org.smartregister.chw.fragment.MotherMentorSecondaryEnrollmentRegisterFragment;

public class MotherMentorSecondaryEnrollmentPagerAdapter extends FragmentPagerAdapter {
    private static final String TABLE_MOTHERMENTOR_ENROLL_IIT = "ec_mothermentor_enroll_it";
    private static final String TABLE_MOTHERMENTOR_ENROLL_PARTNER = "ec_mothermentor_enroll_partner";
    private static final String TABLE_MOTHERMENTOR_ENROLL_CHILD_EID = "ec_mothermentor_enroll_child_eid";

    private final Context context;
    private final MotherMentorSecondaryEnrollmentRegisterFragment[] fragments = new MotherMentorSecondaryEnrollmentRegisterFragment[3];

    public MotherMentorSecondaryEnrollmentPagerAdapter(FragmentManager fragmentManager, Context context) {
        super(fragmentManager);
        this.context = context;
        fragments[0] = MotherMentorSecondaryEnrollmentRegisterFragment.newInstance(TABLE_MOTHERMENTOR_ENROLL_IIT);
        fragments[1] = MotherMentorSecondaryEnrollmentRegisterFragment.newInstance(TABLE_MOTHERMENTOR_ENROLL_PARTNER);
        fragments[2] = MotherMentorSecondaryEnrollmentRegisterFragment.newInstance(TABLE_MOTHERMENTOR_ENROLL_CHILD_EID);
    }

    @Override
    public Fragment getItem(int position) {
        if (position >= 0 && position < fragments.length) {
            return fragments[position];
        }
        return fragments[0];
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getString(R.string.mothermentor_iit);
            case 1:
                return context.getString(R.string.mothermentor_partner);
            case 2:
                return context.getString(R.string.mothermentor_child_eid);
            default:
                return null;
        }
    }

    public void setSearchText(String searchText) {
        for (MotherMentorSecondaryEnrollmentRegisterFragment fragment : fragments) {
            fragment.setSharedSearchText(searchText);
        }
    }
}
