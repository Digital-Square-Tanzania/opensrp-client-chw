package org.smartregister.chw.fragment;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.fragments.JsonWizardFormFragment;

import org.smartregister.opd.OpdLibrary;
import org.smartregister.opd.R;
import org.smartregister.opd.fragment.BaseOpdFormFragment;
import org.smartregister.opd.pojo.OpdMetadata;

/**
 * OPD fragment compatible with native-form 3.1.9 (no getForm()).
 * Avoids calling the removed method by resolving the Form via the new API.
 */
public class PatchedOpdFormFragment extends BaseOpdFormFragment {

    public static PatchedOpdFormFragment getFormFragment(String stepName) {
        PatchedOpdFormFragment fragment = new PatchedOpdFormFragment();
        Bundle bundle = new Bundle();
        bundle.putString(JsonFormConstants.JSON_FORM_KEY.STEPNAME, stepName);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    private Form resolveForm() {
        Activity activity = getActivity();
        if (activity instanceof JsonFormActivity) {
            JsonFormActivity jsonFormActivity = (JsonFormActivity) activity;
            try {
                java.lang.reflect.Field field = JsonFormActivity.class.getDeclaredField("form");
                field.setAccessible(true);
                Object value = field.get(jsonFormActivity);
                if (value instanceof Form) {
                    return (Form) value;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Override
    public void updateVisibilityOfNextAndSave(boolean next, boolean save) {
        // replicate BaseOpdFormFragment logic without relying on activity#getForm()
        Form form = resolveForm();
        OpdMetadata opdMetadata = OpdLibrary.getInstance().getOpdConfiguration().getOpdMetadata();

        // Call JsonWizardFormFragment's logic manually to keep buttons consistent
        if (form != null && form.isWizard()) {
            getMenu().findItem(com.vijay.jsonwizard.R.id.action_next)
                    .setVisible(next && form.isShowNextInToolbarWhenWizard());
            getMenu().findItem(com.vijay.jsonwizard.R.id.action_save).setVisible(save);
        } else {
            getMenu().findItem(com.vijay.jsonwizard.R.id.action_next).setVisible(next);
            getMenu().findItem(com.vijay.jsonwizard.R.id.action_save).setVisible(save);
        }

        if (form != null && form.isWizard() && opdMetadata != null
                && !opdMetadata.isFormWizardValidateRequiredFieldsBefore()) {
            this.getMenu().findItem(R.id.action_save).setVisible(save);
        }
    }
}
