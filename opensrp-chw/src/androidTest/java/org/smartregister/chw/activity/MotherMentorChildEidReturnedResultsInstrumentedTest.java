package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.*;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.fragments.JsonFormFragment;

import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.util.MotherMentorChildEidFormUtils;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.JsonFormUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.chw.util.MotherMentorFormWizardUtils;
import org.smartregister.util.LangUtils;

import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Real native form widgets and returned JSON; no repository or client records are written. */
@RunWith(AndroidJUnit4.class)
public class MotherMentorChildEidReturnedResultsInstrumentedTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test
    public void englishReturnedResultsGateValidatesAndClearsSavedAnswers() throws Exception {
        verifyLanguage("en", "json.form");
    }

    @Test
    public void swahiliReturnedResultsGateValidatesAndClearsSavedAnswers() throws Exception {
        verifyLanguage("sw", "json.form-sw");
    }

    private void verifyLanguage(String language, String directory) throws Exception {
        String originalLanguage = LangUtils.getLanguage(context);
        try {
            LangUtils.saveLanguage(context, language);
            verifyRequiredGateAndNoResult(directory, language);
            verifyNoTestClearsRestoredResult(directory, language);
        } finally {
            LangUtils.saveLanguage(context, originalLanguage);
        }
    }

    private void verifyRequiredGateAndNoResult(String directory, String language) throws Exception {
        Fixture fixture = fixture(directory, "");
        try (ActivityScenario<JsonFormActivity> scenario = launch(fixture.form, language)) {
            openThirdStep(scenario);
            capture(language, "step3-initial");
            onActivity(scenario, activity -> {
                assertVisibleOptions(activity, LAST_TEST_RESULTS_RETURNED, "yes", "no");
                assertVisibleOptions(activity, LAST_TEST_RESULT);
                assertTrue("The localized gate label must be displayed",
                        hasText(fragment(activity).getView(), field(fixture.form,
                                LAST_TEST_RESULTS_RETURNED).getString("label")));
                fragment(activity).next();
                assertInvalidField(activity, LAST_TEST_RESULTS_RETURNED);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertStep(scenario, "step3");
            select(scenario, LAST_TEST_RESULTS_RETURNED, "yes");
            await(scenario, "Yes must display only the two real result options",
                    activity -> visibleOptions(activity, LAST_TEST_RESULT)
                            .equals(new HashSet<>(Arrays.asList("positive", "negative"))));
            onActivity(scenario, activity -> {
                fragment(activity).next();
                assertInvalidField(activity, LAST_TEST_RESULT);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertStep(scenario, "step3");
            select(scenario, LAST_TEST_RESULT, "positive");
            awaitValue(scenario, LAST_TEST_RESULT, "positive");
            capture(language, "step3-yes-positive");
            select(scenario, LAST_TEST_RESULTS_RETURNED, "no");
            awaitClearedAndHidden(scenario, LAST_TEST_RESULT);
            capture(language, "step3-no-hidden-result");

            // Returning to Yes must not resurrect the now-hidden positive selection.
            select(scenario, LAST_TEST_RESULTS_RETURNED, "yes");
            await(scenario, "Yes must show an unanswered result after No", activity -> {
                if (visibleOptions(activity, LAST_TEST_RESULT).size() != 2) return false;
                for (RadioButton option : radios(activity, LAST_TEST_RESULT)) {
                    if (option.isChecked()) return false;
                }
                return true;
            });
            select(scenario, LAST_TEST_RESULT, "negative");
            awaitValue(scenario, LAST_TEST_RESULT, "negative");
            select(scenario, LAST_TEST_RESULTS_RETURNED, "no");
            awaitClearedAndHidden(scenario, LAST_TEST_RESULT);
            next(scenario, "step4");
            capture(language, "step4-after-no");
            JSONObject saved = save(scenario);
            assertEquals("no", value(saved, LAST_TEST_RESULTS_RETURNED));
            assertEquals("", value(saved, LAST_TEST_RESULT));
            MotherMentorChildEidFormUtils.bindIdentity(saved, fixture.mother, fixture.child);
            assertEquals("waiting", value(saved, LAST_TEST_RESULT));
            MotherMentorChildEidFormUtils.prepareForEvent(saved);
            Event event = JsonFormUtils.processJsonForm(MotherMentorLibrary.getInstance()
                    .context().allSharedPreferences(), saved.toString(),
                    Constants.TABLES.MOTHERMENTOR_ENROLL_CHILD_EID);
            assertNotNull(event);
            assertEventAnswer(event, LAST_TEST_RESULTS_RETURNED, "no");
            assertEventAnswer(event, LAST_TEST_RESULT, "waiting");
            assertEquals(fixture.child.get(BASE_ENTITY_ID), saved.getString(ENTITY_ID));
        }
    }

    private void verifyNoTestClearsRestoredResult(String directory, String language) throws Exception {
        Fixture fixture = fixture(directory, "positive");
        try (ActivityScenario<JsonFormActivity> scenario = launch(fixture.form, language)) {
            openThirdStep(scenario);
            await(scenario, "A restored positive result must remain visible and selected", activity -> {
                RadioButton positive = radio(activity, LAST_TEST_RESULT, "positive");
                return positive != null && positive.isShown() && positive.isChecked();
            });
            capture(language, "step3-restored-positive");
            select(scenario, LAST_TEST, "no_test_has_been_done_yet");
            awaitClearedAndHidden(scenario, LAST_TEST_RESULTS_RETURNED);
            awaitClearedAndHidden(scenario, LAST_TEST_RESULT);
            capture(language, "step3-no-test-cleared");
            next(scenario, "step4");
            capture(language, "step4-after-no-test");
            JSONObject saved = save(scenario);
            assertEquals("no_test_has_been_done_yet", value(saved, LAST_TEST));
            assertEquals("", value(saved, LAST_TEST_RESULTS_RETURNED));
            assertEquals("", value(saved, LAST_TEST_RESULT));
            MotherMentorChildEidFormUtils.bindIdentity(saved, fixture.mother, fixture.child);
            MotherMentorChildEidFormUtils.prepareForEvent(saved);
            assertEquals("", value(saved, LAST_TEST_RESULTS_RETURNED));
            assertEquals("", value(saved, LAST_TEST_RESULT));
        }
    }

    private void capture(String language, String checkpoint) throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.waitForIdleSync();
        Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull("Synthetic form screenshot is unavailable", screenshot);
        File directory = new File(context.getCacheDir(), "issue1116-native");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (FileOutputStream output = new FileOutputStream(
                new File(directory, language + "-" + checkpoint + ".png"))) {
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally {
            screenshot.recycle();
        }
    }

    private void assertEventAnswer(Event event, String key, String answer) {
        for (Obs observation : event.getObs()) {
            if (key.equals(observation.getFormSubmissionField())) {
                assertEquals(key, observation.getFieldCode());
                assertTrue(key + " must retain " + answer, observation.getValues().contains(answer));
                return;
            }
        }
        fail("Missing serialized enrollment observation: " + key);
    }

    @SuppressWarnings("unchecked")
    private ActivityScenario<JsonFormActivity> launch(JSONObject form, String language) {
        Context localized = LangUtils.setAppLocale(context, language);
        Intent intent = MotherMentorFormWizardUtils.getFormIntent(localized, form);
        try {
            // Newer test core requires explicit result capture; retain source compatibility with 1.4.
            return (ActivityScenario<JsonFormActivity>) ActivityScenario.class
                    .getMethod("launchActivityForResult", Intent.class).invoke(null, intent);
        } catch (NoSuchMethodException olderTestCore) {
            return ActivityScenario.launch(intent);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Unable to launch the native enrollment form", failure);
        }
    }

    private void openThirdStep(ActivityScenario<JsonFormActivity> scenario) throws Exception {
        assertStep(scenario, "step1");
        next(scenario, "step2");
        next(scenario, "step3");
    }

    private void next(ActivityScenario<JsonFormActivity> scenario, String expectedStep) throws Exception {
        // Wizard navigation returns false even after scheduling a successful transaction.
        onActivity(scenario, activity -> fragment(activity).next());
        assertStep(scenario, expectedStep);
    }

    private JSONObject save(ActivityScenario<JsonFormActivity> scenario) throws Exception {
        onActivity(scenario, activity -> {
            JsonFormFragment form = fragment(activity);
            assertTrue(form.save(false));
            assertTrue("All fields must pass real native save validation",
                    form.getPresenter().getInvalidFields().isEmpty());
        });
        Instrumentation.ActivityResult result = scenario.getResult();
        assertEquals(Activity.RESULT_OK, result.getResultCode());
        assertNotNull(result.getResultData());
        return new JSONObject(result.getResultData().getStringExtra("json"));
    }

    private void assertInvalidField(JsonFormActivity activity, String field) {
        for (String key : fragment(activity).getPresenter().getInvalidFields().keySet()) {
            if (key.endsWith(field)) return;
        }
        fail("Expected native required-field validation for " + field);
    }

    private void assertStep(ActivityScenario<JsonFormActivity> scenario, String step) {
        await(scenario, "Expected native form " + step, activity -> step.equals(
                fragment(activity).getArguments().getString(JsonFormConstants.STEPNAME)));
    }

    private void select(ActivityScenario<JsonFormActivity> scenario, String key, String answer) throws Exception {
        onActivity(scenario, activity -> {
            RadioButton option = radio(activity, key, answer);
            assertNotNull("Missing native option " + key + ":" + answer, option);
            assertTrue("The native option must be visible", option.isShown());
            option.performClick();
        });
        awaitValue(scenario, key, answer);
    }

    private void awaitValue(ActivityScenario<JsonFormActivity> scenario, String key, String expected) {
        await(scenario, "Native answer was not written for " + key,
                activity -> expected.equals(value(new JSONObject(activity.currentJsonState()), key)));
    }

    private void awaitClearedAndHidden(ActivityScenario<JsonFormActivity> scenario, String key) {
        await(scenario, "Hidden native answer was not cleared for " + key, activity ->
                visibleOptions(activity, key).isEmpty()
                        && value(new JSONObject(activity.currentJsonState()), key).isEmpty());
    }

    private void await(ActivityScenario<JsonFormActivity> scenario, String message,
            CheckedPredicate condition) {
        long deadline = SystemClock.elapsedRealtime() + 10000;
        AtomicBoolean ready = new AtomicBoolean();
        while (SystemClock.elapsedRealtime() < deadline) {
            onActivity(scenario, activity -> ready.set(condition.test(activity)));
            if (ready.get()) return;
            SystemClock.sleep(50);
        }
        fail(message);
    }

    private void onActivity(ActivityScenario<JsonFormActivity> scenario, CheckedAction action) {
        scenario.onActivity(activity -> {
            try {
                action.run(activity);
            } catch (Exception failure) {
                throw new AssertionError(failure);
            }
        });
    }

    private JsonFormFragment fragment(JsonFormActivity activity) {
        return (JsonFormFragment) activity.getSupportFragmentManager()
                .findFragmentById(com.vijay.jsonwizard.R.id.container);
    }

    private RadioButton radio(JsonFormActivity activity, String key, String answer) {
        for (RadioButton option : radios(activity, key)) {
            if (answer.equals(option.getTag(com.vijay.jsonwizard.R.id.childKey))) return option;
        }
        return null;
    }

    private List<RadioButton> radios(JsonFormActivity activity, String key) {
        List<RadioButton> options = new ArrayList<>();
        collectRadios(fragment(activity).getView(), key, options);
        return options;
    }

    private void collectRadios(View view, String key, List<RadioButton> result) {
        if (view instanceof RadioButton
                && key.equals(view.getTag(com.vijay.jsonwizard.R.id.key))) {
            result.add((RadioButton) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                collectRadios(group.getChildAt(index), key, result);
            }
        }
    }

    private Set<String> visibleOptions(JsonFormActivity activity, String key) {
        Set<String> answers = new HashSet<>();
        for (RadioButton option : radios(activity, key)) {
            if (option.isShown()) answers.add((String) option.getTag(com.vijay.jsonwizard.R.id.childKey));
        }
        return answers;
    }

    private void assertVisibleOptions(JsonFormActivity activity, String key, String... expected) {
        assertEquals(new HashSet<>(Arrays.asList(expected)), visibleOptions(activity, key));
    }

    private boolean hasText(View view, String expected) {
        if (view instanceof TextView) {
            String rendered = ((TextView) view).getText().toString()
                    .replaceFirst("\\s*\\*\\s*$", "").replaceFirst("\\s+$", "");
            if (expected.equals(rendered)) return view.isShown();
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                if (hasText(group.getChildAt(index), expected)) return true;
            }
        }
        return false;
    }

    private Fixture fixture(String directory, String previousResult) throws Exception {
        JSONObject form;
        try (InputStream input = context.getAssets().open(directory + "/"
                + MotherMentorChildEidFormUtils.FORM + ".json");
                Scanner scanner = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
            form = new JSONObject(scanner.next());
        }
        Map<String, String> mother = person("Mother");
        mother.put(CTC_NUMBER, "12-34-5678-123456");
        Map<String, String> child = person("Child");
        child.put(GENDER, "Female");
        child.put(DOB, LocalDate.now().minusMonths(6).toString());
        Map<String, String> previous = new HashMap<>();
        previous.put(HEI_REGISTERED, "no");
        previous.put(BREASTFEEDING_STATUS, "breast_feeding");
        previous.put(LAST_TEST, "dbs_6");
        previous.put(LAST_TEST_RESULT, previousResult);
        MotherMentorChildEidFormUtils.prepare(form, mother, child, previous);
        return new Fixture(form, mother, child);
    }

    private Map<String, String> person(String firstName) {
        Map<String, String> person = new HashMap<>();
        person.put(BASE_ENTITY_ID, UUID.randomUUID().toString());
        person.put(RELATIONAL_ID, UUID.randomUUID().toString());
        person.put(FIRST_NAME, firstName);
        person.put(LAST_NAME, "NativeFormFixture");
        return person;
    }

    private JSONObject field(JSONObject form, String key) throws Exception {
        for (int step = 1; step <= form.getInt("count"); step++) {
            JSONArray fields = form.getJSONObject("step" + step).getJSONArray("fields");
            for (int index = 0; index < fields.length(); index++) {
                JSONObject field = fields.getJSONObject(index);
                if (key.equals(field.optString("key"))) return field;
            }
        }
        throw new AssertionError("Missing form field " + key);
    }

    private String value(JSONObject form, String key) throws Exception {
        return field(form, key).optString("value");
    }

    private interface CheckedAction {
        void run(JsonFormActivity activity) throws Exception;
    }

    private interface CheckedPredicate {
        boolean test(JsonFormActivity activity) throws Exception;
    }

    private static final class Fixture {
        final JSONObject form;
        final Map<String, String> mother;
        final Map<String, String> child;

        Fixture(JSONObject form, Map<String, String> mother, Map<String, String> child) {
            this.form = form;
            this.mother = mother;
            this.child = child;
        }
    }
}
