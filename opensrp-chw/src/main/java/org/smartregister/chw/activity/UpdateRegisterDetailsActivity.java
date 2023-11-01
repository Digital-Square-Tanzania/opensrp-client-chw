package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.CoreConstants.DB_CONSTANTS.NOTIFICATION_ID;
import static org.smartregister.chw.core.utils.CoreConstants.DB_CONSTANTS.NOTIFICATION_TYPE;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import org.smartregister.chw.core.activity.BaseChwNotificationDetailsActivity;
import org.smartregister.chw.core.activity.BaseChwNotificationRegister;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.task.ChwGoToMemberProfileBasedOnRegisterTask;
import org.smartregister.commonregistry.CommonPersonObjectClient;

public class UpdateRegisterDetailsActivity extends BaseChwNotificationDetailsActivity {

    private static boolean hideViewProfileAction;

    public static void startActivity(CommonPersonObjectClient client, Activity launcherActivity,
                                     String notificationId, String notificationType) {
        Intent intent = new Intent(launcherActivity, UpdateRegisterDetailsActivity.class);
        intent.putExtra(NOTIFICATION_ID, notificationId);
        intent.putExtra(NOTIFICATION_TYPE, notificationType);
        intent.putExtra(CoreConstants.INTENT_KEY.CLIENT, client);
            hideViewProfileAction = !(launcherActivity instanceof BaseChwNotificationRegister);
        launcherActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hideViewProfileAction) {
            viewProfileTextView.setVisibility(View.GONE);
        }
    }

    public void goToMemberProfile() {
        new ChwGoToMemberProfileBasedOnRegisterTask(commonPersonObjectClient, getIntent().getExtras(), notificationType, this).execute();
    }
}
