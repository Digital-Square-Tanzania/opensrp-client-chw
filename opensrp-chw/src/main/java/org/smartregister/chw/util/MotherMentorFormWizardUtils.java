package org.smartregister.chw.util;

import android.content.Context;
import android.content.Intent;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.chw.core.R;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.Utils;

/** Shared wizard navigation for the library forms opened by Mother Mentor screens. */
public final class MotherMentorFormWizardUtils {
    private MotherMentorFormWizardUtils() {}

    public static Form getFormConfig(Context context) {
        return configureForm(context, new Form());
    }

    public static Intent getFormIntent(Context context, JSONObject jsonForm) {
        Intent intent = new Intent(context, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        return configureIntent(context, intent);
    }

    public static Intent configureIntent(Context context, Intent intent) {
        Form form = (Form) intent.getSerializableExtra(JsonFormConstants.JSON_FORM_KEY.FORM);
        if (form == null) form = new Form();
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, configureForm(context, form));
        return intent;
    }

    private static Form configureForm(Context context, Form form) {
        form.setWizard(true);
        form.setActionBarBackground(R.color.family_actionbar);
        form.setNavigationBackground(R.color.family_navigation);
        form.setHomeAsUpIndicator(R.mipmap.ic_cross_white);
        form.setNextLabel(context.getString(R.string.next));
        form.setPreviousLabel(context.getString(R.string.back));
        form.setSaveLabel(context.getString(R.string.save));
        return form;
    }
}
