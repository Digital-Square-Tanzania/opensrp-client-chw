package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.dao.PersonDao;

import timber.log.Timber;

public class PncChildNoMotherProfileActivity extends ChildProfileActivity {

    public static void startMe(Activity activity, MemberObject memberObject) {
        Intent intent = new Intent(activity, PncChildNoMotherProfileActivity.class);
        intent.putExtra(Constants.ANC_MEMBER_OBJECTS.MEMBER_PROFILE_OBJECT, memberObject);
        passToolbarTitle(activity, intent);
        activity.startActivity(intent);
    }


    @Override
    public void setParentName(String parentName) {
        this.textViewParentName.setText(getString(R.string.care_giver_initials) + ": " + PersonDao.getCareGiverNameForNoMotherChild(memberObject.getBaseEntityId()));
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();

        if (i == R.id.textview_record_visit || i == R.id.record_visit_done_bar) {
            openVisitHomeScreen(false);
        } else {
            super.onClick(view);
        }
    }

    private void openVisitHomeScreen(boolean isEditMode) {

        if (!isEditMode) {
            PncHomeVisitActivity.startMe(this, memberObject, false);
        }

    }
}
