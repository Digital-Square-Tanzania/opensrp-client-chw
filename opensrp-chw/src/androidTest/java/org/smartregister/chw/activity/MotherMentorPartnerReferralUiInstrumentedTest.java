package org.smartregister.chw.activity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.*;

import android.app.Instrumentation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.fragments.JsonFormFragment;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.repository.MotherMentorPartnerClosureRepository;
import org.smartregister.chw.mothermentor.repository.VisitRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitCompletion;
import org.smartregister.family.util.Utils;
import org.smartregister.util.LangUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
public class MotherMentorPartnerReferralUiInstrumentedTest {
    private final Instrumentation device = InstrumentationRegistry.getInstrumentation();
    private final Context context = device.getTargetContext();
    private static final String REFERRAL_QUESTION = "Rufaa ya upimaji imetolewa?";
    private static final String REFERRAL_PROMPT = "Mpe mwenzi rufaa ya upimaji.";
    private static final String ATTENDANCE_QUESTION = "Mwenzi amehudhuria kituoni?";

    /** Run only on a disposable emulator database after signing in and disabling connectivity. */
    @Test
    public void referralAndAttendanceSaveFromTheNativeVisitScreens() throws Exception {
        Assume.assumeTrue("This test creates records on a disposable offline emulator",
                "true".equals(InstrumentationRegistry.getArguments().getString("offlinePartnerUi")));
        assertNull("The emulator must be offline before seeding test records",
                context.getSystemService(ConnectivityManager.class).getActiveNetwork());
        String originalLanguage = LangUtils.getLanguage(context);
        LangUtils.saveLanguage(context, "sw");
        try {
            verifyFirstVisit();
            verifyReferralRetry();
            verifyAttendance();
        } finally {
            LangUtils.saveLanguage(context, originalLanguage);
        }
    }

    private void verifyFirstVisit() throws Exception {
        String partner = fixture(null);
        try (ActivityScenario<MotherMentorVisitActivity> visit = launch(partner)) {
            JsonFormActivity tracing = openAction(visit, "mothermentor_partner_follow_up_status");
            choose(tracing, FOLLOW_UP_STATUS, "found");
            choose(tracing, PARTNER_FACILITY_AGREEMENT, "agreed");
            assertRequired(tracing, PARTNER_TESTING_REFERRAL_GIVEN);
            choose(tracing, PARTNER_TESTING_REFERRAL_GIVEN, "no");
            assertVisibleText(tracing, REFERRAL_PROMPT, true);
            capture("mwenzi-referral-required");
            save(tracing);

            JsonFormActivity education = openAction(visit, "mothermentor_partner_education");
            choose(education, PARTNER_EDUCATION_PROVIDED, "no");
            save(education);
            submit();
            assertSaved(partner, "tracing", PARTNER_TESTING_REFERRAL_GIVEN, "no");
            assertProjectedPhase(partner, "referral");
        }
    }

    private void verifyReferralRetry() throws Exception {
        String partner = fixture("no");
        Instrumentation.ActivityMonitor monitor = monitorWizard();
        try (ActivityScenario<MotherMentorVisitActivity> visit = launch(partner)) {
            JsonFormActivity referral = awaitWizard(monitor);
            assertVisibleText(referral, REFERRAL_QUESTION, true);
            assertVisibleText(referral, ATTENDANCE_QUESTION, false);
            assertRequired(referral, PARTNER_TESTING_REFERRAL_GIVEN);
            choose(referral, PARTNER_TESTING_REFERRAL_GIVEN, "no");
            assertVisibleText(referral, REFERRAL_PROMPT, true);
            choose(referral, PARTNER_TESTING_REFERRAL_GIVEN, "yes");
            assertVisibleText(referral, REFERRAL_PROMPT, false);
            save(referral);
            submit();
            assertSaved(partner, "referral", PARTNER_TESTING_REFERRAL_GIVEN, "yes");
            assertProjectedPhase(partner, "attendance");
        }
    }

    private void verifyAttendance() throws Exception {
        String partner = fixture("yes");
        Instrumentation.ActivityMonitor monitor = monitorWizard();
        try (ActivityScenario<MotherMentorVisitActivity> visit = launch(partner)) {
            JsonFormActivity attendance = awaitWizard(monitor);
            assertVisibleText(attendance, ATTENDANCE_QUESTION, true);
            assertVisibleText(attendance, REFERRAL_QUESTION, false);
            choose(attendance, PARTNER_ARRIVED_AT_FACILITY, "yes");
            assertRequired(attendance, PARTNER_FACILITY_ARRIVAL_DATE);
            onView(withHint(startsWith("Tarehe aliyohudhuria kituoni"))).perform(click());
            onView(withId(android.R.id.button1)).perform(click());
            save(attendance);
            submit();
            capture("mwenzi-attendance-confirmation");
            assertFalse(MotherMentorPartnerClosureRepository.getState(database(), partner, null).isClosed());
            onView(withId(android.R.id.button1)).perform(click());
            assertSaved(partner, "attendance", PARTNER_ARRIVED_AT_FACILITY, "yes");
            assertEquals("attended",
                    MotherMentorPartnerClosureRepository.getState(database(), partner, null).getStatus());
            assertNotNull(org.smartregister.chw.mothermentor.dao.MotherMentorDao.getPartnerHistoryMember(partner));
        }
    }

    private String fixture(String priorReferral) throws Exception {
        String id = UUID.randomUUID().toString();
        String family = UUID.randomUUID().toString();
        String linkage = UUID.randomUUID().toString();
        SQLiteDatabase db = database();
        insert(db, "ec_family", BASE_ENTITY_ID, family, FIRST_NAME, "Synthetic Mwenzi", IS_CLOSED, "0");
        insert(db, "ec_family_member", BASE_ENTITY_ID, id, RELATIONAL_ID, family, FIRST_NAME, "Synthetic",
                LAST_NAME, "Mwenzi", DOB, "1990-01-01", GENDER, "Male", IS_CLOSED, "0");
        insert(db, Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER, BASE_ENTITY_ID, id,
                PARTNER_LINKAGE_ID, linkage, IS_CLOSED, "0", LAST_INTERACTED_WITH, "1");
        if (priorReferral != null) {
            String previous = new JSONObject().put("obs", new JSONArray()
                    .put(obs(FOLLOW_UP_CATEGORY, "PARTNER"))
                    .put(obs(PARTNER_FOLLOW_UP_SCHEMA, "3"))
                    .put(obs(PARTNER_LINKAGE_ID, linkage))
                    .put(obs(PARTNER_FOLLOW_UP_PHASE, "referral"))
                    .put(obs(PARTNER_TESTING_REFERRAL_GIVEN, priorReferral))
                    .put(obs(MOTHERMENTOR_VISIT_DATE, LocalDate.now().minusDays(1).toString()))).toString();
            insert(db, VisitRepository.VISIT_TABLE, VISIT_ID, UUID.randomUUID().toString(),
                    FORM_SUBMISSION_ID, UUID.randomUUID().toString(), BASE_ENTITY_ID, id,
                    VISIT_TYPE, MotherMentorFollowUpType.PARTNER.getEventType(), PROCESSED, "1",
                    VISIT_DATE, Long.toString(LocalDate.now().minusDays(1).toDate().getTime()),
                    VISIT_JSON, previous);
            MotherMentorPartnerClosureRepository.rebuild(db, id);
        }
        return id;
    }

    private SQLiteDatabase database() {
        return ChwApplication.getInstance().getRepository().getWritableDatabase();
    }

    private void insert(SQLiteDatabase db, String table, String... pairs) {
        ContentValues values = new ContentValues();
        for (int i = 0; i < pairs.length; i += 2) values.put(pairs[i], pairs[i + 1]);
        db.insertOrThrow(table, null, values);
    }

    private JSONObject obs(String key, String value) throws Exception {
        return new JSONObject().put("fieldCode", key).put("values", new JSONArray().put(value));
    }

    private ActivityScenario<MotherMentorVisitActivity> launch(String partner) {
        Intent intent = new Intent(context, MotherMentorVisitActivity.class)
                .putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, partner)
                .putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.MOTHERMENTOR_PROFILE)
                .putExtra(MotherMentorVisitActivity.EXTRA_FOLLOW_UP_VISIT, true)
                .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE, MotherMentorFollowUpType.PARTNER.getValue());
        return ActivityScenario.launch(intent);
    }

    private Instrumentation.ActivityMonitor monitorWizard() {
        return device.addMonitor(Utils.metadata().familyMemberFormActivity.getName(), null, false);
    }

    private JsonFormActivity awaitWizard(Instrumentation.ActivityMonitor monitor) {
        JsonFormActivity wizard = (JsonFormActivity) device.waitForMonitorWithTimeout(monitor, 12000);
        assertNotNull("The native wizard must open", wizard);
        device.waitForIdleSync();
        return wizard;
    }

    private JsonFormActivity openAction(ActivityScenario<MotherMentorVisitActivity> visit, String form)
            throws Exception {
        Instrumentation.ActivityMonitor monitor = monitorWizard();
        for (int retry = 0; retry < 50; retry++) {
            AtomicBoolean started = new AtomicBoolean();
            visit.onActivity(activity -> {
                for (BaseMotherMentorVisitAction action : activity.getMotherMentorVisitActions().values()) {
                    if (form.equals(action.getFormName()) && action.isValid()) {
                        activity.startForm(action);
                        started.set(true);
                        return;
                    }
                }
            });
            if (started.get()) return awaitWizard(monitor);
            SystemClock.sleep(100);
        }
        throw new AssertionError("Visit action did not become available: " + form);
    }

    private void choose(JsonFormActivity wizard, String key, String answer) {
        device.runOnMainSync(() -> {
            View option = find(fragment(wizard).getView(), view -> view instanceof RadioButton
                    && key.equals(view.getTag(com.vijay.jsonwizard.R.id.key))
                    && answer.equals(view.getTag(com.vijay.jsonwizard.R.id.childKey)));
            assertNotNull("Visible native option " + key + "=" + answer, option);
            option.performClick();
        });
        device.waitForIdleSync();
    }

    private void assertVisibleText(JsonFormActivity wizard, String text, boolean expected) {
        device.runOnMainSync(() -> assertEquals(text, expected, find(fragment(wizard).getView(),
                view -> view instanceof TextView && ((TextView) view).getText().toString().startsWith(text)) != null));
    }

    private View find(View root, java.util.function.Predicate<View> match) {
        if (root.isShown() && match.test(root)) return root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = find(group.getChildAt(i), match);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JsonFormFragment fragment(JsonFormActivity wizard) {
        return (JsonFormFragment) wizard.getSupportFragmentManager()
                .findFragmentById(com.vijay.jsonwizard.R.id.container);
    }

    private void assertRequired(JsonFormActivity wizard, String key) {
        device.runOnMainSync(() -> {
            JsonFormFragment form = fragment(wizard);
            assertFalse(form.save(false));
            assertTrue("Missing required-field validation for " + key, form.getPresenter()
                    .getInvalidFields().keySet().stream().anyMatch(field -> field.endsWith(key)));
        });
    }

    private void save(JsonFormActivity wizard) {
        device.runOnMainSync(() -> assertTrue(fragment(wizard).save(false)));
        device.waitForIdleSync();
    }

    private void submit() {
        onView(withId(org.smartregister.chw.mothermentor.R.id.customFontTextViewSubmit)).perform(click());
        device.waitForIdleSync();
    }

    private void assertSaved(String partner, String phase, String field, String value) throws Exception {
        VisitRepository repository = MotherMentorLibrary.getInstance().visitRepository();
        for (int attempt = 0; attempt < 60; attempt++) {
            Visit visit = repository.getLatestVisit(partner, MotherMentorFollowUpType.PARTNER.getEventType());
            if (visit != null && Boolean.TRUE.equals(visit.getProcessed())) {
                Map<String, String> answers = MotherMentorVisitCompletion.observationValues(
                        new JSONObject(visit.getJson()).getJSONArray("obs"));
                if (phase.equals(answers.get(PARTNER_FOLLOW_UP_PHASE)) && value.equals(answers.get(field))
                        && !LocalDate.now().minusDays(1).toString().equals(answers.get(MOTHERMENTOR_VISIT_DATE))) {
                    assertFalse(answers.containsKey(PARTNER_HIV_TESTED));
                    assertFalse(answers.containsKey(PARTNER_SYPHILIS_TESTED));
                    assertFalse(answers.containsKey(PARTNER_HEPATITIS_B_TESTED));
                    return;
                }
            }
            SystemClock.sleep(100);
        }
        throw new AssertionError("No finalized " + phase + " visit with " + field + "=" + value);
    }

    private void assertProjectedPhase(String partner, String expected) {
        try (android.database.Cursor result = database().rawQuery("SELECT " + PARTNER_NEXT_FOLLOW_UP_PHASE
                + " FROM " + Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER + " WHERE " + BASE_ENTITY_ID + " = ?",
                new String[] {partner})) {
            assertTrue(result.moveToFirst());
            assertEquals(expected, result.getString(0));
        }
    }

    private void capture(String name) throws Exception {
        device.waitForIdleSync();
        Bitmap screenshot = device.getUiAutomation().takeScreenshot();
        assertNotNull(screenshot);
        try (FileOutputStream out = new FileOutputStream(
                new File(context.getExternalFilesDir(null), name + ".png"))) {
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, out));
        } finally {
            screenshot.recycle();
        }
    }
}
