package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.text.InputFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.vijay.jsonwizard.fragments.JsonWizardFormFragment;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.util.MotherMentorFormWizardUtils;
import org.smartregister.chw.util.MotherMentorTestAssets;
import org.smartregister.family.activity.FamilyWizardFormActivity;
import org.smartregister.family.util.Constants;
import org.smartregister.util.LangUtils;

import java.util.Iterator;
import java.util.Locale;

@Config(sdk = 28)
public class MotherMentorReasonLengthTest extends BaseUnitTest {
    private static final String IIT_FORM = "mothermentor_iit_follow_up_status";
    private static final String REASON = "iit_sababu_nyingine";
    private static final String[] SPECIFY_FORMS = {
        "mothermentor_enrollment", "mothermentor_visit_type", "mothermentor_visit_education_provided",
        "mothermentor_visit_referal", "mothermentor_followup_visit", IIT_FORM,
        "mothermentor_iit_return_to_facility", "mothermentor_service_interruption_reason",
        "mothermentor_child_eid_follow_up_status", "mothermentor_child_eid_hei",
        "mothermentor_child_eid_non_attendance_reason", "mothermentor_child_eid_record_result",
        "mothermentor_partner_follow_up_status", "mothermentor_partner_non_attendance_reason",
        "mothermentor_partner_hiv_test", "mothermentor_partner_hepatitis_b_test",
        "mothermentor_partner_syphilis_test", "mothermentor_household_follow_up_status",
        "mothermentor_household_visit_purpose", "mother_mentor_household_visit_topics_taught",
        "mother_mentor_household_members_who_received_counselling",
        "mother_mentor_household_members_who_received_counselling_legacy"
    };

    private final Context context = RuntimeEnvironment.getApplication();
    private final String originalLanguage = LangUtils.getLanguage(context);

    @After
    public void restoreLanguage() {
        LangUtils.saveLanguage(context, originalLanguage);
    }

    @Test
    public void configuresOnlyBilingualSpecifyTextFields() throws Exception {
        for (String name : SPECIFY_FORMS) {
            JSONObject swahili = asset("sw", name);
            for (String language : new String[] {"en", "sw"}) {
                JSONObject original = asset(language, name);
                String source = original.toString();
                JSONObject launched = payload(launchIntent(language, original));
                for (Iterator<String> steps = swahili.keys(); steps.hasNext(); ) {
                    String stepName = steps.next();
                    if (!stepName.startsWith("step")) continue;
                    JSONArray fields = swahili.getJSONObject(stepName).getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject swahiliField = fields.getJSONObject(i);
                        String key = swahiliField.getString("key");
                        JSONObject launchedField = field(launched, stepName, key);
                        JSONObject sourceField = field(original, stepName, key);
                        boolean specify = "edit_text".equals(swahiliField.optString("type"))
                                && swahiliField.optString("hint").toLowerCase(Locale.ROOT).contains("taja");
                        if (specify) {
                            JSONObject limit = launchedField.getJSONObject("v_max_length");
                            assertEquals(100, limit.getInt("value"));
                            assertTrue(limit.getBoolean("is_fixed_size"));
                            assertEquals("sw".equals(language) ? "Upeo ni herufi 100" : "Maximum 100 characters",
                                    limit.getString("err"));
                            launchedField.remove("v_max_length");
                        }
                        assertEquals(name + " " + key, sourceField.toString(), launchedField.toString());
                    }
                }
                assertEquals(source, original.toString());
            }
        }
    }

    @Test
    public void clipboardPasteUpdatesNativeCounterAndSavedReasonAtEachBoundary() throws Exception {
        for (String language : new String[] {"en", "sw"}) {
            for (int length : new int[] {99, 100, 135}) {
                try (OpenReasonForm form = openReasonForm(language)) {
                    String text = synthetic(length);
                    String expected = text.substring(0, Math.min(length, 100));
                    paste(form.reason, text);
                    assertCounterAndText(form.reason, expected);
                    assertEquals(expected, field(new JSONObject(form.activity.currentJsonState()), "step1", REASON)
                            .getString("value"));

                    form.save();
                    assertEquals(Activity.RESULT_OK, shadowOf(form.activity).getResultCode());
                    JSONObject saved = field(payload(shadowOf(form.activity).getResultIntent()), "step1", REASON);
                    assertEquals(expected, saved.getString("value"));
                    assertEquals(REASON, saved.getString("openmrs_entity_id"));
                }
            }
        }
    }

    @Test
    public void partialPasteAndTypingUseTheSpaceLeftInTheInput() throws Exception {
        try (OpenReasonForm form = openReasonForm("sw")) {
            String prefix = synthetic(98);
            form.reason.append(prefix);
            paste(form.reason, "abc");
            assertCounterAndText(form.reason, prefix + "ab");
            form.reason.append("c");
            assertCounterAndText(form.reason, prefix + "ab");
            form.reason.getText().delete(98, 100);
            form.reason.append("d");
            assertCounterAndText(form.reason, prefix + "d");
        }
    }

    @Test
    public void saveStillRequiresAReasonAndRejectsOverLimitTextEvenWithoutTheInputFilter() throws Exception {
        try (OpenReasonForm form = openReasonForm("sw")) {
            form.save();
            assertFalse(form.activity.isFinishing());
            assertEquals(Activity.RESULT_CANCELED, shadowOf(form.activity).getResultCode());

            form.reason.setFilters(new InputFilter[0]);
            form.reason.setText(synthetic(135));
            assertEquals(135, form.reason.length());
            form.save();
            assertFalse(form.activity.isFinishing());
            assertEquals(Activity.RESULT_CANCELED, shadowOf(form.activity).getResultCode());
            assertEquals("Upeo ni herufi 100", form.reason.getError().toString());
        }
    }

    @Test
    public void launchBoundsALegacyPrefillWithoutChangingTheSourceAnswer() throws Exception {
        JSONObject saved = asset("en", IIT_FORM);
        String legacy = synthetic(99) + "\uD83D\uDE42" + synthetic(34);
        field(saved, "step1", REASON).put("value", legacy);

        JSONObject launch = payload(launchIntent("en", saved));

        assertEquals(synthetic(99), field(launch, "step1", REASON).getString("value"));
        assertEquals(legacy, field(saved, "step1", REASON).getString("value"));
    }

    private JSONObject asset(String language, String name) throws Exception {
        return MotherMentorTestAssets.form("sw".equals(language) ? "json.form-sw" : "json.form", name + ".json");
    }

    private Intent launchIntent(String language, JSONObject form) {
        LangUtils.saveLanguage(context, language);
        return MotherMentorFormWizardUtils.getFormIntent(LangUtils.setAppLocale(context, language), form);
    }

    private OpenReasonForm openReasonForm(String language) throws Exception {
        Intent intent = launchIntent(language, asset(language, IIT_FORM));
        ActivityController<FamilyWizardFormActivity> controller =
                Robolectric.buildActivity(FamilyWizardFormActivity.class, intent);
        controller.create().start().resume().visible();
        FamilyWizardFormActivity activity = controller.get();
        idle(activity);

        JsonWizardFormFragment fragment = (JsonWizardFormFragment) activity.getSupportFragmentManager()
                .findFragmentById(com.vijay.jsonwizard.R.id.container);
        assertNotNull(fragment);
        View root = fragment.requireView();
        clickRadio(activity, root, "follow_up_status", "not_found");
        clickRadio(activity, root, "iit_sababu_kutopatikana", "other");
        MaterialEditText reason = widget(root, REASON, null, MaterialEditText.class);
        assertNotNull(reason);
        assertTrue(reason.isShown());
        assertTrue(reason.requestFocus());
        return new OpenReasonForm(controller, fragment, reason);
    }

    private static void clickRadio(FamilyWizardFormActivity activity, View root, String key, String option) {
        RadioButton radio = widget(root, key, option, RadioButton.class);
        assertNotNull(key + "/" + option, radio);
        assertTrue(radio.isShown());
        assertTrue(radio.performClick());
        idle(activity);
    }

    private static void paste(MaterialEditText reason, String text) {
        ClipboardManager clipboard = (ClipboardManager) reason.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("synthetic reason", text));
        reason.setSelection(reason.length());
        assertTrue(reason.onTextContextMenuItem(android.R.id.paste));
    }

    private static void assertCounterAndText(MaterialEditText reason, String expected) {
        assertEquals(expected, reason.getText().toString());
        assertEquals(expected.length() + " / 100",
                ReflectionHelpers.<String>callInstanceMethod(reason, "getCharactersCounterText"));
        assertTrue(reason.isCharactersCountValid());
    }

    private static String synthetic(int length) {
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) value.append((char) ('a' + i % 26));
        return value.toString();
    }

    private static JSONObject payload(Intent intent) throws Exception {
        assertNotNull(intent);
        return new JSONObject(intent.getStringExtra(Constants.JSON_FORM_EXTRA.JSON));
    }

    private static JSONObject field(JSONObject form, String step, String key) throws Exception {
        JSONArray fields = form.getJSONObject(step).getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (key.equals(field.optString("key"))) return field;
        }
        throw new AssertionError("Missing " + step + ":" + key);
    }

    private static <T extends View> T widget(View root, String key, String option, Class<T> type) {
        if (type.isInstance(root) && key.equals(root.getTag(com.vijay.jsonwizard.R.id.key))
                && (option == null || option.equals(root.getTag(com.vijay.jsonwizard.R.id.childKey)))) {
            return type.cast(root);
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                T found = widget(group.getChildAt(i), key, option, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void idle(FamilyWizardFormActivity activity) {
        ShadowLooper.runUiThreadTasks();
        activity.getSupportFragmentManager().executePendingTransactions();
        ShadowLooper.runUiThreadTasks();
    }

    private static final class OpenReasonForm implements AutoCloseable {
        private final ActivityController<FamilyWizardFormActivity> controller;
        private final FamilyWizardFormActivity activity;
        private final JsonWizardFormFragment fragment;
        private final MaterialEditText reason;

        private OpenReasonForm(ActivityController<FamilyWizardFormActivity> controller,
                JsonWizardFormFragment fragment, MaterialEditText reason) {
            this.controller = controller;
            this.activity = controller.get();
            this.fragment = fragment;
            this.reason = reason;
        }

        private void save() {
            fragment.save(false);
            idle(activity);
        }

        @Override
        public void close() {
            controller.pause().stop().destroy();
        }
    }
}
