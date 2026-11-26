package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.vijay.jsonwizard.utils.FormUtils;

import org.joda.time.LocalDate;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.smartregister.chw.R;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.util.MotherMentorTestAssets;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorEnrollmentAgeTest {
    private final LocalDate today = new LocalDate(2026, 11, 24);

    @Test
    public void newNonpregnantEnrollmentUsesTheInclusiveBoundaryInBothLocales() throws Exception {
        for (String locale : new String[] {"json.form", "json.form-sw"}) {
            JSONObject form = MotherMentorTestAssets.form(locale, "mothermentor_enrollment.json");
            assertFalse(FormUtils.getFieldFromForm(form, "child_over_18_months").has("key"));
            setValue(form, "pregnancy_status_selection", "not_pregnant");
            JSONObject dob = FormUtils.getFieldFromForm(form, "child_dob");
            for (String savedStatus : new String[] {"not_pregnant", "breast_feeding"}) {
                setValue(form, "pregnancy_status", savedStatus);
                dob.put("value", "25-04-2025");
                assertEquals(0, MotherMentorRegisterActivity.motherEnrollmentError(form, today));
                dob.put("value", "24-04-2025");
                assertEquals(0, MotherMentorRegisterActivity.motherEnrollmentError(form, today));
                for (String olderDob : new String[] {"23-04-2025", "24-11-2024"}) {
                    dob.put("value", olderDob);
                    assertEquals(R.string.mothermentor_enrollment_child_over_age,
                            MotherMentorRegisterActivity.motherEnrollmentError(form, today));
                }
            }
        }
    }

    @Test
    public void onlyAnOlderChildGetsTheAgeMessage() throws Exception {
        JSONObject form = MotherMentorTestAssets.form("json.form", "mothermentor_enrollment.json");
        setValue(form, "pregnancy_status_selection", "not_pregnant");
        for (String invalidDob : new String[] {"", "31-02-2026", "25-11-2026"}) {
            setValue(form, "child_dob", invalidDob);
            assertEquals(org.smartregister.chw.mothermentor.R.string.error_unable_to_save_form,
                    MotherMentorRegisterActivity.motherEnrollmentError(form, today));
        }
        setValue(form, "pregnancy_status_selection", "pregnant");
        setValue(form, "child_dob", "24-11-2024");
        assertEquals(0, MotherMentorRegisterActivity.motherEnrollmentError(form, today));
    }

    @Test
    public void olderFormPayloadsCanUseTheStoredPregnancyStatusAsTheSelection() throws Exception {
        JSONObject form = MotherMentorTestAssets.form("json.form", "mothermentor_enrollment.json");
        for (String status : new String[] {"none", "not_pregnant", "bf", "breast_feeding"}) {
            setValue(form, "pregnancy_status", status);
            setValue(form, "child_dob", "2025-04-24");
            assertEquals(0, MotherMentorRegisterActivity.motherEnrollmentError(form, today));
            setValue(form, "child_dob", "2025-04-23");
            assertEquals(R.string.mothermentor_enrollment_child_over_age,
                    MotherMentorRegisterActivity.motherEnrollmentError(form, today));
        }
    }

    @Test
    public void ageMessageMatchesBothTranslations() {
        assertEquals("The child is over 19 months. The mother will not be enrolled in Mama Kinara.",
                ageMessage(Locale.ENGLISH));
        assertEquals("Mtoto amezidi miezi 19. Mama hataandikishwa Mama Kinara.",
                ageMessage(new Locale("sw")));
    }

    @Test
    public void olderChildSaveShowsTheMessageAndReturnsToTheCaller() throws Exception {
        MotherMentorRegisterActivity activity = Robolectric.buildActivity(MotherMentorRegisterActivity.class).get();
        activity.setTheme(R.style.Theme_AppCompat);
        JSONObject form = MotherMentorTestAssets.form("json.form", "mothermentor_enrollment.json");
        setValue(form, "pregnancy_status_selection", "not_pregnant");
        setValue(form, "child_dob", LocalDate.now().minusMonths(24).toString("dd-MM-yyyy"));
        Intent data = new Intent().putExtra(Constants.JSON_FORM_EXTRA.JSON, form.toString());

        activity.onActivityResult(Constants.REQUEST_CODE_GET_JSON, Activity.RESULT_OK, data);

        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        TextView message = dialog.findViewById(android.R.id.message);
        assertEquals(activity.getString(R.string.mothermentor_enrollment_child_over_age), message.getText().toString());
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertTrue(activity.isFinishing());
    }

    private static void setValue(JSONObject form, String key, String value) throws Exception {
        FormUtils.getFieldFromForm(form, key).put("value", value);
    }

    private static String ageMessage(Locale locale) {
        Application application = RuntimeEnvironment.getApplication();
        Configuration configuration = new Configuration(application.getResources().getConfiguration());
        configuration.setLocale(locale);
        return application.createConfigurationContext(configuration)
                .getString(R.string.mothermentor_enrollment_child_over_age);
    }
}
