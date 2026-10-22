package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;

import com.vijay.jsonwizard.fragments.JsonWizardFormFragment;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.util.MotherMentorFormWizardUtils;
import org.smartregister.chw.util.MotherMentorTestAssets;
import org.smartregister.family.activity.FamilyWizardFormActivity;
import org.smartregister.util.LangUtils;

/** Exercises the existing native wizard with the actual bilingual enrollment assets. */
@Config(sdk = 28)
public class MotherMentorFormWizardNavigationTest extends BaseUnitTest {
    private static final String CTC_NUMBER = "12-34-5678-123456";

    @Test
    public void previousRetainsCtcAndArtAnswersInEnglishAndSwahili() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String originalLanguage = LangUtils.getLanguage(context);
        try {
            for (String language : new String[] {"en", "sw"}) {
                checkEnrollmentNavigation(context, language);
            }
        } finally {
            LangUtils.saveLanguage(context, originalLanguage);
        }
    }

    private void checkEnrollmentNavigation(Context context, String language) throws Exception {
        LangUtils.saveLanguage(context, language);
        Context localized = LangUtils.setAppLocale(context, language);
        JSONObject form =
                MotherMentorTestAssets.form(
                        "sw".equals(language) ? "json.form-sw" : "json.form",
                        "mothermentor_enrollment.json");
        Intent intent = MotherMentorFormWizardUtils.getFormIntent(localized, form);
        assertEquals(FamilyWizardFormActivity.class.getName(), intent.getComponent().getClassName());
        ActivityController<FamilyWizardFormActivity> controller =
                Robolectric.buildActivity(FamilyWizardFormActivity.class, intent);
        try {
            controller.create().start().resume().visible();
            FamilyWizardFormActivity activity = controller.get();
            idle(activity);

            assertEquals("step1", fragment(activity).getArguments().getString("stepName"));
            EditText ctc = widget(fragment(activity).getView(), "ctc_number", null, EditText.class);
            ctc.setText(CTC_NUMBER);
            ctc.clearFocus();
            advance(activity);

            JsonWizardFormFragment secondPage = fragment(activity);
            assertEquals("step2", secondPage.getArguments().getString("stepName"));
            View previous = secondPage.getView().findViewById(com.vijay.jsonwizard.R.id.previous);
            assertNotNull(previous);
            assertEquals(View.VISIBLE, previous.getVisibility());
            assertTrue(previous.isShown());
            assertTrue(previous.isEnabled());
            RadioButton artNo = widget(secondPage.getView(), "art_status", "no", RadioButton.class);
            assertTrue(artNo.performClick());
            assertTrue(artNo.isChecked());
            assertTrue(previous.performClick());
            idle(activity);

            assertEquals("step1", fragment(activity).getArguments().getString("stepName"));
            assertEquals(
                    CTC_NUMBER,
                    widget(fragment(activity).getView(), "ctc_number", null, EditText.class)
                            .getText().toString());
            advance(activity);
            assertEquals("step2", fragment(activity).getArguments().getString("stepName"));
            assertTrue(widget(fragment(activity).getView(), "art_status", "no", RadioButton.class).isChecked());

            JSONObject serialized = new JSONObject(activity.currentJsonState());
            assertEquals(CTC_NUMBER, value(serialized, "step1", "ctc_number"));
            assertEquals("no", value(serialized, "step2", "art_status"));
            assertFalse(activity.isFinishing());
        } finally {
            controller.pause().stop().destroy();
        }
    }

    private void advance(FamilyWizardFormActivity activity) {
        // This is the native Next handler, without the button's asynchronous progress dialog.
        fragment(activity).next();
        idle(activity);
    }

    private void idle(FamilyWizardFormActivity activity) {
        ShadowLooper.runUiThreadTasks();
        activity.getSupportFragmentManager().executePendingTransactions();
        ShadowLooper.runUiThreadTasks();
    }

    private JsonWizardFormFragment fragment(FamilyWizardFormActivity activity) {
        JsonWizardFormFragment fragment =
                (JsonWizardFormFragment) activity.getSupportFragmentManager()
                        .findFragmentById(com.vijay.jsonwizard.R.id.container);
        assertNotNull(fragment);
        assertNotNull(fragment.getView());
        return fragment;
    }

    private <T extends View> T widget(View root, String key, String option, Class<T> type) {
        T found = findWidget(root, key, option, type);
        assertNotNull("Expected native widget: " + key + "/" + option, found);
        return found;
    }

    private <T extends View> T findWidget(View root, String key, String option, Class<T> type) {
        if (type.isInstance(root)
                && key.equals(root.getTag(com.vijay.jsonwizard.R.id.key))
                && (option == null || option.equals(root.getTag(com.vijay.jsonwizard.R.id.childKey)))) {
            return type.cast(root);
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                T found = findWidget(group.getChildAt(i), key, option, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String value(JSONObject form, String step, String key) throws Exception {
        JSONArray fields = form.getJSONObject(step).getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (key.equals(field.optString("key"))) return field.optString("value");
        }
        throw new AssertionError("Missing form field: " + key);
    }
}
