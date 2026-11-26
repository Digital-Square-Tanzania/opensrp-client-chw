package org.smartregister.chw.adapter;

import android.content.Context;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import org.smartregister.chw.R;
import org.smartregister.chw.fragment.MotherMentorSecondaryEnrollmentRegisterFragment;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;

public class MotherMentorSecondaryEnrollmentPagerAdapter extends FragmentPagerAdapter {
    private final Context context;
    private boolean iitHistoryMode;
    private boolean partnerHistoryMode;
    private boolean childHistoryMode;
    private final MotherMentorSecondaryEnrollmentRegisterFragment[] fragments = new MotherMentorSecondaryEnrollmentRegisterFragment[3];

    public MotherMentorSecondaryEnrollmentPagerAdapter(FragmentManager fragmentManager, Context context) {
        super(fragmentManager);
        this.context = context;
        fragments[0] = MotherMentorSecondaryEnrollmentRegisterFragment.newInstance(MotherMentorFollowUpType.MOTHER_IIT);
        fragments[1] = MotherMentorSecondaryEnrollmentRegisterFragment.newInstance(MotherMentorFollowUpType.PARTNER);
        fragments[2] = MotherMentorSecondaryEnrollmentRegisterFragment.newInstance(MotherMentorFollowUpType.CHILD_EID);
    }

    @Override
    public Fragment getItem(int position) {
        if (position >= 0 && position < fragments.length) {
            return fragments[position];
        }
        return fragments[0];
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object item = super.instantiateItem(container, position);
        if (item instanceof MotherMentorSecondaryEnrollmentRegisterFragment) {
            fragments[position] = (MotherMentorSecondaryEnrollmentRegisterFragment) item;
            if (position == 0) fragments[position].setHistoryMode(iitHistoryMode);
            if (position == 1) fragments[position].setHistoryMode(partnerHistoryMode);
            if (position == 2) fragments[position].setHistoryMode(childHistoryMode);
        }
        return item;
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

    public void setIitHistoryMode(boolean historyMode) {
        iitHistoryMode = historyMode;
        fragments[0].setHistoryMode(historyMode);
    }

    public void setPartnerHistoryMode(boolean historyMode) {
        partnerHistoryMode = historyMode;
        fragments[1].setHistoryMode(historyMode);
    }

    public void setChildHistoryMode(boolean historyMode) {
        childHistoryMode = historyMode;
        fragments[2].setHistoryMode(historyMode);
    }

    public void setSearchText(String searchText) {
        for (MotherMentorSecondaryEnrollmentRegisterFragment fragment : fragments) {
            fragment.setSharedSearchText(searchText);
        }
    }
}
