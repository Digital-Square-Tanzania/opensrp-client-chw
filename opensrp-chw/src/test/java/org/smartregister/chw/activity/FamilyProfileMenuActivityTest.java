package org.smartregister.chw.activity;

import android.content.Intent;

import org.smartregister.chw.BaseActivityTest;
import org.smartregister.chw.core.activity.CoreFamilyProfileMenuActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.family.util.Constants;

public class FamilyProfileMenuActivityTest extends BaseActivityTest<FamilyProfileMenuActivity> {
    @Override
    protected Intent getControllerIntent() {
        Intent intent = new Intent();
        intent.putExtra(Constants.INTENT_KEY.BASE_ENTITY_ID, "12345");
        intent.putExtra(CoreFamilyProfileMenuActivity.MENU, CoreConstants.MenuType.ChangeHead);
        return intent;
    }

    @Override
    protected Class<FamilyProfileMenuActivity> getActivityClass() {
        return FamilyProfileMenuActivity.class;
    }
}
