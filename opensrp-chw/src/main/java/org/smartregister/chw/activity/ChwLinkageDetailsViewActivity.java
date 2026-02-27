package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.widget.Toolbar;

import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.dao.ReferralDao;
import org.smartregister.chw.referral.domain.MemberObject;
import org.smartregister.chw.referral.util.Constants;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Task;
import org.smartregister.view.customcontrols.CustomFontTextView;

/**
 * Author issyzac on 07/05/2024
 */
public class ChwLinkageDetailsViewActivity extends ChwReferralDetailsViewActivity {

    public static void startChwLinkageDetailsViewActivity(Activity activity, MemberObject memberObject, CommonPersonObjectClient client) {
        Intent intent = new Intent(activity, ChwLinkageDetailsViewActivity.class);
        intent.putExtra(Constants.ReferralMemberObject.MEMBER_OBJECT, memberObject);
        activity.startActivity(intent);
    }

    @Override
    protected void setupViews() {

        String taskId = ReferralDao.getTaskIdByReasonReference(getMemberObject().getBaseEntityId());
        Task task = ChwApplication.getInstance().getTaskRepository().getTaskByIdentifier(taskId);
        if (!task.getBusinessStatus().equalsIgnoreCase("Complete")) {
            createCancelReferral(task);
        } else {
            showFeedBackView(task);
        }

        //Change Referral Labels to Linkage
        CustomFontTextView linkageTypeLabel = findViewById(R.id.referral_type_label);
        linkageTypeLabel.setText("Linkage Type");

        LinearLayout referralFacilityLayout = findViewById(R.id.referral_facility_layout);
        referralFacilityLayout.setVisibility(View.GONE);

        CustomFontTextView linkageDate = findViewById(R.id.referral_date_label);
        linkageDate.setText("Linkage Date");

        //Update the toolbar
        Toolbar toolbar = findViewById(R.id.back_referrals_toolbar);
        toolbar.setTitle("Back to Linkages");

    }

    @Override
    protected void createCancelReferral(Task task) {
        //To implement
    }

    @Override
    protected void showFeedBackView(Task task) {
        //To implement
    }
}
