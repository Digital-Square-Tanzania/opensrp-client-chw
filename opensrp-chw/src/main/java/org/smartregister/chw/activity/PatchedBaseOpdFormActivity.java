package org.smartregister.chw.activity;

import com.vijay.jsonwizard.activities.JsonFormBaseActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.smartregister.chw.fragment.PatchedOpdFormFragment;
import org.smartregister.opd.activity.BaseOpdFormActivity;

public class PatchedBaseOpdFormActivity extends BaseOpdFormActivity {

    public Form getForm() {
        try {
            java.lang.reflect.Field field = JsonFormBaseActivity.class.getDeclaredField("form");
            field.setAccessible(true);
            Object value = field.get(this);
            return value instanceof Form ? (Form) value : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void initializeFormFragmentCore() {
        PatchedOpdFormFragment opdFormFragment = PatchedOpdFormFragment.getFormFragment(JsonFormConstants.FIRST_STEP_NAME);
        getSupportFragmentManager().beginTransaction()
                .add(org.smartregister.opd.R.id.container, opdFormFragment)
                .commit();
    }
}
