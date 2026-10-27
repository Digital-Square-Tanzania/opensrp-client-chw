package org.smartregister.chw.util;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.MotherMentorProfileActivity;
import org.smartregister.chw.core.model.NavigationOption;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.due.DueServiceItem;
import org.smartregister.chw.due.LandingDueServiceRepository;
import org.smartregister.chw.model.NavigationModelFlv;

import java.util.List;

/** App-owned labels and destinations; providers never depend on an Activity. */
public final class DueServiceNavigation {
    private DueServiceNavigation() { }

    public static String moduleLabel(Context context, String moduleId) {
        return LandingDueServiceRepository.MOTHER_MENTOR.equals(moduleId)
                ? context.getString(R.string.due_services_mother_mentor) : moduleId;
    }

    public static String serviceLabel(Context context, DueServiceItem item) {
        return serviceLabel(context, item.getModuleId(), item.getServiceCode());
    }

    public static String serviceLabel(Context context, String moduleId, String serviceCode) {
        return LandingDueServiceRepository.MOTHER_MENTOR.equals(moduleId)
                && "mother_mentor.followup".equals(serviceCode)
                ? context.getString(R.string.due_services_followup) : serviceCode;
    }

    public static boolean canOpen(DueServiceItem item, List<NavigationOption> options) {
        if (!LandingDueServiceRepository.MOTHER_MENTOR.equals(item.getModuleId())
                || !"mother_mentor.followup".equals(item.getServiceCode())) return false;
        for (NavigationOption option : options) {
            if (CoreConstants.DrawerMenu.MOTHER_MENTOR.equals(option.getMenuTitle())) return true;
        }
        return false;
    }

    public static void open(Activity activity, DueServiceItem item) {
        if (canOpen(item, new NavigationModelFlv().getNavigationItems())) {
            MotherMentorProfileActivity.startMe(activity, item.getSourceEntityId());
        } else {
            Toast.makeText(activity, R.string.due_services_profile_unavailable, Toast.LENGTH_LONG).show();
        }
    }
}
