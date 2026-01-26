package org.smartregister.chw.activity;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.smartregister.chw.fragment.PatchedOpdFormFragment;
import org.smartregister.opd.activity.BaseOpdFormActivity;

/**
 * Compatibility wrapper to keep OPD forms working with native-form 3.1.9.
 * - Restores getForm() expected by older OPD code.
 * - Uses a patched fragment that avoids the missing method crash.
 */
public class PatchedOpdFormActivity extends BaseOpdFormActivity {

    /** Re-introduced method matching the old native-form API. */
    public Form getForm() {
        // Avoid calling form()/getForm() chain (recurses). Read the private field via reflection.
        try {
            java.lang.reflect.Field field = com.vijay.jsonwizard.activities.JsonFormActivity.class.getDeclaredField("form");
            field.setAccessible(true);
            Object value = field.get(this);
            if (value instanceof Form) {
                return (Form) value;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    protected void initializeFormFragmentCore() {
        PatchedOpdFormFragment opdFormFragment = PatchedOpdFormFragment.getFormFragment(JsonFormConstants.FIRST_STEP_NAME);
        getSupportFragmentManager().beginTransaction().add(org.smartregister.opd.R.id.container, opdFormFragment).commit();
    }
}
