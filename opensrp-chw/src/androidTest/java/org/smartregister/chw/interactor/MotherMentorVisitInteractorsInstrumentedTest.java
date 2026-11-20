package org.smartregister.chw.interactor;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.*;

import android.app.Instrumentation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.vijay.jsonwizard.fragments.JsonFormFragment;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.activity.MotherMentorVisitWizardActivity;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.util.MotherMentorFormWizardUtils;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.contract.BaseMotherMentorVisitContract;
import org.smartregister.chw.mothermentor.dao.MotherMentorMotherVisitDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.dao.MotherMentorPartnerFollowUpDao;
import org.smartregister.chw.mothermentor.interactor.BaseMotherMentorServiceVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.ChildEidFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.MotherMentorFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.MotherMentorHouseholdVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.MotherMentorServiceVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.PartnerFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherChildRepository;
import org.smartregister.chw.mothermentor.repository.VisitRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherChildUtils;
import org.smartregister.util.LangUtils;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Exercises the installed library, packaged forms and real executor callbacks on Android.
 * Database fixtures are local and use synthetic records.
 */
@RunWith(AndroidJUnit4.class)
public class MotherMentorVisitInteractorsInstrumentedTest {
    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private String formFolder = "json.form";

    @Test
    public void serviceActionsCompleteAndFollowAttendanceVisibility() throws Exception {
        Map<String, BaseMotherMentorVisitAction> actions = actions(new MotherServiceFixture("pregnant", ""), 7);
        BaseMotherMentorVisitAction attendance = byForm(actions, "mothermentor_visit_type");
        BaseMotherMentorVisitAction education =
                byForm(actions, "mothermentor_visit_education_provided");
        assertFalse(education.isValid());
        answer(attendance, ATTENDANCE_TYPE, "repeat", FOLLOW_UP_STATUS, "found",
                VISIT_PREGNANCY_STATUS, "pregnant");
        BaseMotherMentorVisitAction child = byForm(actions, "mothermentor_visit_child");
        assertFalse(child.isValid());
        answer(
                byForm(actions, "mothermentor_visit_group_linkage"),
                HAS_BEEN_LINKED_TO_PSYCHOSOCIAL_SUPPORT_GROUP,
                "already_in_group",
                HAS_BEEN_LINKED_TO_IGA_GROUP,
                "yes");
        answer(education, EDUCATION_PROVIDED, "no");
        answer(byForm(actions, "mothermentor_visit_referal"), REFERRAL_GIVEN, "no");
        answer(
                byForm(actions, "mothermentor_next_visit_date"),
                NEXT_APPOINTMENT_DATE,
                "26-10-2026");
        answer(byForm(actions, "mothermentor_visit_comment"), COMMENTS, "Emulator component test");
        for (BaseMotherMentorVisitAction action : actions.values()) {
            if (action == child) continue;
            assertTrue(action.getFormName(), action.isValid());
            completed(action);
        }
        answer(attendance, ATTENDANCE_TYPE, "repeat", FOLLOW_UP_STATUS, "not_found",
                MAMA_NOT_FOUND_REASON, "deceased");
        assertFalse(education.isValid());
        assertFalse(byForm(actions, "mothermentor_visit_group_linkage").isValid());
        assertFalse(byForm(actions, "mothermentor_visit_referal").isValid());
        assertFalse(byForm(actions, "mothermentor_next_visit_date").isValid());
        assertTrue(byForm(actions, "mothermentor_visit_comment").isValid());
    }

    @Test
    public void mpyaPregnancyAndNewbornAnswersAreRequiredInTheNativeWizard() throws Exception {
        String originalLanguage = LangUtils.getLanguage(context);
        LangUtils.saveLanguage(context, "sw");
        useLanguage("sw");
        try {
            Map<String, BaseMotherMentorVisitAction> pregnant = actions(new MotherServiceFixture("pregnant", ""), 7);
            BaseMotherMentorVisitAction entry = byForm(pregnant, "mothermentor_visit_type");
            withWizard(entry, activity -> {
                select(activity, ATTENDANCE_TYPE, "new");
                onMain(() -> {
                    View root = activity.getWindow().getDecorView();
                    assertNull(visibleRadio(root, FOLLOW_UP_STATUS, "found"));
                    assertNotNull(visibleRadio(root, VISIT_PREGNANCY_STATUS, "pregnant"));
                    assertSaveBlocked(activity, VISIT_PREGNANCY_STATUS);
                });
                select(activity, VISIT_PREGNANCY_STATUS, "pregnant");
                save(activity, entry);
            });
            completed(entry);
            assertFalse(byForm(pregnant, "mothermentor_visit_child").isValid());

            BaseMotherMentorVisitAction returning = byForm(
                    actions(new MotherServiceFixture("pregnant", ""), 7), "mothermentor_visit_type");
            withWizard(returning, activity -> {
                select(activity, ATTENDANCE_TYPE, "repeat");
                onMain(() -> {
                    View root = activity.getWindow().getDecorView();
                    assertNotNull(visibleRadio(root, FOLLOW_UP_STATUS, "found"));
                    assertNull(visibleRadio(root, VISIT_PREGNANCY_STATUS, "pregnant"));
                });
                select(activity, FOLLOW_UP_STATUS, "found");
                select(activity, VISIT_PREGNANCY_STATUS, "pregnant");
                save(activity, returning);
            });
            completed(returning);

            String dob = LocalDate.now().minusDays(1).toString();
            Map<String, BaseMotherMentorVisitAction> postDelivery =
                    actions(new MotherServiceFixture("breast_feeding", dob), 7);
            BaseMotherMentorVisitAction attendance = byForm(postDelivery, "mothermentor_visit_type");
            BaseMotherMentorVisitAction child = byForm(postDelivery, "mothermentor_visit_child");
            assertFalse(child.isValid());
            withWizard(attendance, activity -> {
                select(activity, ATTENDANCE_TYPE, "new");
                onMain(() -> assertNull(visibleRadio(
                        activity.getWindow().getDecorView(), MAMA_CHILD_STATUS, "alive")));
                save(activity, attendance);
            });
            assertEquals("Mtoto wa 1", child.getTitle());
            assertTrue(child.isValid());
            pending(child);
            withWizard(child, activity -> {
                select(activity, MAMA_CHILD_STATUS, "alive");
                select(activity, MAMA_BREASTFEEDING_STATUS, "breastfeeding");
                onMain(() -> {
                    View root = activity.getWindow().getDecorView();
                    assertNull(visibleRadio(root, MAMA_HEI_TEST_DONE, "no"));
                    assertNotNull(visibleRadio(root, MAMA_BIRTH_TEST_DONE, "no"));
                    assertSaveBlocked(activity, MAMA_BIRTH_TEST_DONE);
                });
                select(activity, MAMA_BIRTH_TEST_DONE, "yes");
                onMain(() -> {
                    assertNotNull(visibleRadio(activity.getWindow().getDecorView(),
                            MAMA_BIRTH_TEST_RESULT, "pending"));
                    assertSaveBlocked(activity, MAMA_BIRTH_TEST_DATE);
                });
                select(activity, MAMA_BIRTH_TEST_DONE, "no");
                save(activity, child);
            });
            completed(child);
        } finally {
            LangUtils.saveLanguage(context, originalLanguage);
        }
    }

    @Test
    public void mamaKadiRequiresReceiptDatesOnlyForAvailableTestResults() throws Exception {
        String originalLanguage = LangUtils.getLanguage(context);
        LangUtils.saveLanguage(context, "sw");
        useLanguage("sw");
        try {
            LocalDate today = LocalDate.now();
            LocalDate dob = today.minusDays(49);
            Map<String, BaseMotherMentorVisitAction> actions =
                    actions(new MotherServiceFixture("breast_feeding", dob.toString()), 7);
            BaseMotherMentorVisitAction attendance = byForm(actions, "mothermentor_visit_type");
            withWizard(attendance, activity -> {
                select(activity, ATTENDANCE_TYPE, "new");
                save(activity, attendance);
            });
            BaseMotherMentorVisitAction child = byForm(actions, "mothermentor_visit_child");
            assertTrue(child.isValid());

            withWizard(child, activity -> {
                select(activity, MAMA_CHILD_STATUS, "alive");
                select(activity, MAMA_BREASTFEEDING_STATUS, "breastfeeding");
                select(activity, MAMA_BIRTH_TEST_DONE, "yes");
                LocalDate birthSample = dob.plusDays(1);
                setDate(activity, MAMA_BIRTH_TEST_DATE, birthSample);
                onMain(() -> assertNull(visibleDate(activity, MAMA_BIRTH_TEST_RECEIVED_DATE)));
                select(activity, MAMA_BIRTH_TEST_RESULT, "negative");
                onMain(() -> assertSaveBlocked(activity, MAMA_BIRTH_TEST_RECEIVED_DATE));
                setDate(activity, MAMA_BIRTH_TEST_RECEIVED_DATE, dob);
                onMain(() -> assertEquals("Tarehe ya kupokea majibu haiwezi kuwa kabla ya tarehe ya kipimo",
                        visibleDate(activity, MAMA_BIRTH_TEST_RECEIVED_DATE).getError().toString()));
                setDate(activity, MAMA_BIRTH_TEST_RECEIVED_DATE, birthSample);
                select(activity, MAMA_BIRTH_TEST_RESULT, "pending");
                onMain(() -> assertNull(visibleDate(activity, MAMA_BIRTH_TEST_RECEIVED_DATE)));
                select(activity, MAMA_BIRTH_TEST_RESULT, "negative");
                onMain(() -> assertSaveBlocked(activity, MAMA_BIRTH_TEST_RECEIVED_DATE));
                setDate(activity, MAMA_BIRTH_TEST_RECEIVED_DATE, birthSample);

                select(activity, MAMA_HEI_TEST_DONE, "yes");
                setDate(activity, MAMA_HEI_TEST_DATE, today.minusDays(1));
                onMain(() -> assertNull(visibleDate(activity, MAMA_HEI_TEST_RECEIVED_DATE)));
                select(activity, MAMA_HEI_TEST_RESULT, "positive");
                select(activity, HEI_CHANYA_IMETHIBITISHWA, "no");
                onMain(() -> assertSaveBlocked(activity, MAMA_HEI_TEST_RECEIVED_DATE));
                setDate(activity, MAMA_HEI_TEST_RECEIVED_DATE, today.plusDays(1));
                onMain(() -> assertEquals("Weka tarehe sahihi ya kupokea majibu ambayo haipo katika siku zijazo",
                        visibleDate(activity, MAMA_HEI_TEST_RECEIVED_DATE).getError().toString()));
                setDate(activity, MAMA_HEI_TEST_RECEIVED_DATE, today);
                select(activity, MAMA_HEI_TEST_RESULT, "pending");
                onMain(() -> assertNull(visibleDate(activity, MAMA_HEI_TEST_RECEIVED_DATE)));
                select(activity, MAMA_HEI_TEST_RESULT, "negative");
                onMain(() -> assertSaveBlocked(activity, MAMA_HEI_TEST_RECEIVED_DATE));
                setDate(activity, MAMA_HEI_TEST_RECEIVED_DATE, today);
                save(activity, child);
            });
            completed(child);
            Map<String, String> answers = MotherMentorMotherChildUtils.values(new JSONObject(child.getJsonPayload()));
            assertEquals(dob.plusDays(1).toString("dd-MM-yyyy"), answers.get(MAMA_BIRTH_TEST_RECEIVED_DATE));
            assertEquals(today.toString("dd-MM-yyyy"), answers.get(MAMA_HEI_TEST_RECEIVED_DATE));
        } finally {
            LangUtils.saveLanguage(context, originalLanguage);
        }
    }

    @Test
    public void mamaKadiChangesFeedingOptionsAfterABreastfeedingVisitIsSaved() throws Exception {
        String originalLanguage = LangUtils.getLanguage(context);
        LangUtils.saveLanguage(context, "sw");
        useLanguage("sw");
        SQLiteDatabase database = ChwApplication.getInstance().getRepository().getWritableDatabase();
        String motherId = UUID.randomUUID().toString();
        LocalDate today = LocalDate.now();
        LocalDate dob = today.minusDays(50);
        String enrollmentTable = Constants.TABLES.MOTHERMENTOR_ENROLLMENT;
        try {
            ContentValues enrollment = new ContentValues();
            enrollment.put(BASE_ENTITY_ID, motherId);
            enrollment.put(PREGNANCY_STATUS, "not_pregnant");
            enrollment.put(CHILD_DOB, dob.toString());
            enrollment.put(IS_CLOSED, 0);
            database.insertOrThrow(enrollmentTable, null, enrollment);

            Map<String, BaseMotherMentorVisitAction> firstVisit = mamaKadiActions(motherId);
            BaseMotherMentorVisitAction attendance = byForm(firstVisit, "mothermentor_visit_type");
            withWizard(attendance, activity -> {
                select(activity, ATTENDANCE_TYPE, "new");
                save(activity, attendance);
            });
            BaseMotherMentorVisitAction child = byForm(firstVisit, "mothermentor_visit_child");
            assertTrue(child.isValid());
            withWizard(child, activity -> {
                select(activity, MAMA_CHILD_STATUS, "alive");
                onMain(() -> {
                    View root = activity.getWindow().getDecorView();
                    assertFeedingOption(root, "breastfeeding", "Ananyonyesha");
                    assertFeedingOption(root, "never_breastfed", "Hajawahi kunyonyesha");
                    assertFeedingOption(root, "not_breastfeeding", "Ameacha kunyonyesha");
                });
                select(activity, MAMA_BIRTH_TEST_DONE, "no");
                select(activity, MAMA_BREASTFEEDING_STATUS, "not_breastfeeding");
                onMain(() -> assertSaveBlocked(activity, MAMA_TAREHE_ACHA_KUNYONYESHA));
                select(activity, MAMA_BREASTFEEDING_STATUS, "never_breastfed");
                select(activity, MAMA_HEI_TEST_DONE, "no");
                save(activity, child);
            });
            completed(child);
            Map<String, String> answers = MotherMentorMotherChildUtils.values(new JSONObject(child.getJsonPayload()));
            assertEquals("never_breastfed", answers.get(MAMA_BREASTFEEDING_STATUS));
            assertEquals("", answers.get(MAMA_TAREHE_ACHA_KUNYONYESHA));
            assertEquals("six_weeks", answers.get(MAMA_HEI_DUE_TEST));

            saveMotherVisit(database, motherId, today.minusDays(2), answers);
            assertEquals("never_breastfed", MotherMentorMotherChildRepository.getState(database, motherId, null)
                    .getBreastfeedingStatus());
            BaseMotherMentorVisitAction started = byForm(mamaKadiActions(motherId), "mothermentor_visit_type");
            withWizard(started, activity -> {
                select(activity, FOLLOW_UP_STATUS, "found");
                select(activity, MAMA_CHILD_STATUS, "alive");
                assertNeverBreastfedVisible(activity, true);
                select(activity, MAMA_BREASTFEEDING_STATUS, "breastfeeding");
                select(activity, MAMA_HEI_TEST_DONE, "no");
                save(activity, started);
            });
            completed(started);
            saveMotherVisit(database, motherId, today.minusDays(1),
                    MotherMentorMotherChildUtils.values(new JSONObject(started.getJsonPayload())));

            BaseMotherMentorVisitAction returning = byForm(mamaKadiActions(motherId), "mothermentor_visit_type");
            withWizard(returning, activity -> {
                select(activity, FOLLOW_UP_STATUS, "found");
                select(activity, MAMA_CHILD_STATUS, "alive");
                assertNeverBreastfedVisible(activity, false);
                onMain(() -> {
                    View root = activity.getWindow().getDecorView();
                    assertFeedingOption(root, "breastfeeding", "Ananyonyesha");
                    assertFeedingOption(root, "not_breastfeeding", "Ameacha kunyonyesha");
                });
            });
        } finally {
            database.delete(VisitRepository.VISIT_TABLE, BASE_ENTITY_ID + " = ?", new String[]{motherId});
            database.delete(enrollmentTable, BASE_ENTITY_ID + " = ?", new String[]{motherId});
            database.delete(Constants.TABLES.MOTHERMENTOR_MOTHER_CONTEXT, BASE_ENTITY_ID + " = ?",
                    new String[]{motherId});
            LangUtils.saveLanguage(context, originalLanguage);
        }
    }

    private Map<String, BaseMotherMentorVisitAction> mamaKadiActions(String motherId) throws Exception {
        MemberObject member = new MemberObject();
        member.setBaseEntityId(motherId);
        return actions(new MotherMentorServiceVisitInteractor(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES), 7,
                member);
    }

    private void assertFeedingOption(View root, String value, String label) {
        RadioButton option = visibleRadio(root, MAMA_BREASTFEEDING_STATUS, value);
        assertNotNull(label, option);
        assertEquals(label, option.getText().toString());
    }

    private void assertNeverBreastfedVisible(MotherMentorVisitWizardActivity activity, boolean visible) {
        onMain(() -> assertEquals(visible, visibleRadio(activity.getWindow().getDecorView(),
                MAMA_BREASTFEEDING_STATUS, "never_breastfed") != null));
    }

    private void saveMotherVisit(SQLiteDatabase database, String motherId, LocalDate date,
            Map<String, String> answers) throws Exception {
        answers.put(MAMA_ENROLLMENT_ID, MotherMentorMotherVisitDao.getContext(database, motherId).getEnrollmentId());
        answers.put(MOTHERMENTOR_VISIT_DATE, date.toString());
        JSONArray observations = new JSONArray();
        for (Map.Entry<String, String> answer : answers.entrySet()) {
            if (!answer.getValue().isEmpty()) {
                observations.put(new JSONObject().put("fieldCode", answer.getKey())
                        .put("values", new JSONArray().put(answer.getValue())));
            }
        }
        String visitId = UUID.randomUUID().toString();
        Visit visit = new Visit();
        visit.setVisitId(visitId);
        visit.setFormSubmissionId(visitId);
        visit.setVisitType(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        visit.setBaseEntityId(motherId);
        Date timestamp = date.toDate();
        visit.setDate(timestamp);
        visit.setCreatedAt(timestamp);
        visit.setUpdatedAt(timestamp);
        visit.setProcessed(true);
        visit.setJson(new JSONObject().put(EVENT_FORM_SUBMISSION_ID, visitId)
                .put(EVENT_DATE_TIMESTAMP, date.toString()).put("obs", observations).toString());
        MotherMentorLibrary.getInstance().visitRepository().addVisit(visit, database);
    }

    @Test
    public void marudioAtSevenWeeksOffersBothTestsAndBoundsTheBirthDatePicker() throws Exception {
        String originalLanguage = LangUtils.getLanguage(context);
        LangUtils.saveLanguage(context, "sw");
        useLanguage("sw");
        try {
            LocalDate today = LocalDate.now();
            LocalDate dob = today.minusDays(49);
            BaseMotherMentorVisitAction attendance = byForm(
                    actions(new MotherServiceFixture("breast_feeding", dob.toString()), 7),
                    "mothermentor_visit_type");
            withWizard(attendance, activity -> {
                select(activity, ATTENDANCE_TYPE, "repeat");
                select(activity, FOLLOW_UP_STATUS, "found");
                select(activity, MAMA_CHILD_STATUS, "alive");
                select(activity, MAMA_BREASTFEEDING_STATUS, "breastfeeding");
                select(activity, MAMA_BIRTH_TEST_DONE, "yes");
                onMain(() -> {
                    View root = activity.getWindow().getDecorView();
                    assertNotNull(visibleRadio(root, MAMA_HEI_TEST_DONE, "no"));
                    EditText date = birthDateInput(root);
                    assertNotNull(date);
                    date.performClick();
                });
                onView(isAssignableFrom(DatePicker.class)).check((view, missing) -> {
                    assertNotNull(view);
                    DatePicker picker = (DatePicker) view;
                    assertEquals(dob, new LocalDate(picker.getMinDate()));
                    assertEquals(dob.plusDays(2), new LocalDate(picker.getMaxDate()));
                });
                onView(withId(android.R.id.button1)).perform(click());
                onMain(() -> assertSaveBlocked(activity, MAMA_BIRTH_TEST_RESULT));
                select(activity, MAMA_BIRTH_TEST_RESULT, "pending");
                select(activity, MAMA_HEI_TEST_DONE, "no");
                save(activity, attendance);
            });
            assertEquals(
                    attendance.getFormName(),
                    BaseMotherMentorVisitAction.Status.COMPLETED,
                    attendance.getActionStatus());
        } finally {
            LangUtils.saveLanguage(context, originalLanguage);
        }
    }

    private EditText birthDateInput(View view) {
        if (!view.isShown()) return null;
        if (view instanceof EditText && MAMA_BIRTH_TEST_DATE.equals(view.getTag(com.vijay.jsonwizard.R.id.key))) {
            return (EditText) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                EditText date = birthDateInput(group.getChildAt(index));
                if (date != null) return date;
            }
        }
        return null;
    }

    private void withWizard(BaseMotherMentorVisitAction action,
            java.util.function.Consumer<MotherMentorVisitWizardActivity> verify) throws Exception {
        Intent intent = MotherMentorFormWizardUtils.getFormIntent(context, new JSONObject(action.getJsonPayload()));
        assertEquals(MotherMentorVisitWizardActivity.class.getName(), intent.getComponent().getClassName());
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MotherMentorVisitWizardActivity activity =
                (MotherMentorVisitWizardActivity) instrumentation.startActivitySync(intent);
        try {
            instrumentation.waitForIdleSync();
            verify.accept(activity);
        } finally {
            onMain(activity::finish);
        }
    }

    private void select(MotherMentorVisitWizardActivity activity, String key, String value) {
        onMain(() -> {
            RadioButton option = visibleRadio(activity.getWindow().getDecorView(), key, value);
            assertNotNull(key + "=" + value, option);
            option.performClick();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private void setDate(MotherMentorVisitWizardActivity activity, String key, LocalDate day) {
        onMain(() -> {
            EditText field = visibleDate(activity, key);
            assertNotNull(key, field);
            field.setTag(com.vijay.jsonwizard.R.id.locale_independent_value, day.toString("dd-MM-yyyy"));
            field.setText(day.toString("dd/MM/yyyy"));
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private EditText visibleDate(MotherMentorVisitWizardActivity activity, String key) {
        for (View view : activity.getFormDataViews()) {
            if (view instanceof EditText && view.isShown()
                    && key.equals(view.getTag(com.vijay.jsonwizard.R.id.key))) return (EditText) view;
        }
        return null;
    }

    private void save(MotherMentorVisitWizardActivity activity, BaseMotherMentorVisitAction action) {
        AtomicReference<String> saved = new AtomicReference<>();
        onMain(() -> {
            JsonFormFragment form = fragment(activity);
            assertTrue(form.save(false));
            assertTrue(form.getPresenter().getInvalidFields().keySet().toString(),
                    form.getPresenter().getInvalidFields().isEmpty());
            saved.set(activity.currentJsonState());
        });
        action.setJsonPayload(saved.get());
    }

    private void assertSaveBlocked(MotherMentorVisitWizardActivity activity, String key) {
        JsonFormFragment form = fragment(activity);
        form.save(false);
        assertTrue(key, form.getPresenter().getInvalidFields().keySet().stream()
                .anyMatch(field -> field.endsWith(key)));
        assertFalse(activity.isFinishing());
    }

    private void onMain(Runnable action) {
        FutureTask<Void> task = new FutureTask<>(action, null);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(task);
        try {
            task.get();
        } catch (ExecutionException failure) {
            throw new AssertionError(failure.getCause());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private RadioButton visibleRadio(View view, String key, String value) {
        if (!view.isShown()) return null;
        if (view instanceof RadioButton && key.equals(view.getTag(com.vijay.jsonwizard.R.id.key))
                && value.equals(view.getTag(com.vijay.jsonwizard.R.id.childKey))) return (RadioButton) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                RadioButton candidate = visibleRadio(group.getChildAt(i), key, value);
                if (candidate != null) return candidate;
            }
        }
        return null;
    }

    private JsonFormFragment fragment(MotherMentorVisitWizardActivity activity) {
        return (JsonFormFragment) activity.getSupportFragmentManager()
                .findFragmentById(com.vijay.jsonwizard.R.id.container);
    }

    @Test
    public void householdActionsReadNativeCheckboxAnswersAndComplete() throws Exception {
        Map<String, BaseMotherMentorVisitAction> actions =
                actions(
                        new MotherMentorHouseholdVisitInteractor(
                                Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES, "ec_family"),
                        4);
        answer(
                byForm(actions, "mothermentor_household_visit_purpose"),
                PURPOSE_OF_VISIT,
                "routine_follow_up");
        answer(
                byForm(actions, "mother_mentor_household_members_who_received_counselling"),
                PARTICIPANTS,
                new JSONArray().put("mother").put("partner"));
        answer(
                byForm(actions, "mother_mentor_household_visit_topics_taught"),
                TOPICS_TAUGHT,
                new JSONArray().put("medication_adherence").put("nutrition"));
        answer(
                byForm(actions, "mothermentor_household_visit_comment"),
                COMMENTS,
                "Emulator household test");
        for (BaseMotherMentorVisitAction action : actions.values()) {
            assertTrue(action.getFormName(), action.isValid());
            completed(action);
        }
    }

    @Test
    public void motherIitFollowUpRequiresDatesAndClearsHiddenAnswers() throws Exception {
        followUp(
                new MotherMentorFollowUpVisitInteractor(
                        MotherMentorFollowUpType.MOTHER_IIT.getEventType()),
                "mothermentor_",
                SERVICE_INTERRUPTION_REASON,
                SERVICE_INTERRUPTION_REASON_OTHER,
                EXPECTED_FACILITY_VISIT_DATE,
                RETURNED_TO_FACILITY,
                FACILITY_RETURN_DATE,
                false);
    }

    @Test
    public void partnerTracingCollectsAgreementWithoutSameVisitAttendanceInBothLanguages() throws Exception {
        for (String language : new String[] {"en", "sw"}) {
            useLanguage(language);
            Map<String, BaseMotherMentorVisitAction> map = actions(new PartnerFixture(false), 5);
            BaseMotherMentorVisitAction status = byForm(map, "mothermentor_partner_follow_up_status");
            BaseMotherMentorVisitAction arrival = byForm(map, "mothermentor_partner_arrival");
            BaseMotherMentorVisitAction education = byForm(map, "mothermentor_partner_education");
            assertTrue(status.isValid());
            assertFalse(arrival.isValid());
            answer(status, FOLLOW_UP_STATUS, "found");
            pending(status);
            answer(status, FOLLOW_UP_STATUS, "found", PARTNER_FACILITY_AGREEMENT, "agreed");
            pending(status);
            answer(status, FOLLOW_UP_STATUS, "found", PARTNER_FACILITY_AGREEMENT, "agreed",
                    PARTNER_TESTING_REFERRAL_GIVEN, "no");
            completed(status);
            assertFalse("Attendance belongs to the next visit", arrival.isValid());
            assertTrue(education.isValid());
            answer(education, PARTNER_EDUCATION_PROVIDED, "no");
            completed(education);
            answer(status, FOLLOW_UP_STATUS, "not_found", PARTNER_NOT_FOUND_REASON, "other");
            pending(status);
            assertFalse(education.isValid());
            assertNull(education.getJsonPayload());
            answer(status, FOLLOW_UP_STATUS, "not_found", PARTNER_NOT_FOUND_REASON, "other",
                    PARTNER_NOT_FOUND_REASON_OTHER, "Component fixture explanation");
            completed(status);
            answer(status, FOLLOW_UP_STATUS, "not_found", PARTNER_NOT_FOUND_REASON, "deceased");
            completed(status);
            assertTrue(byForm(map, "mothermentor_partner_follow_up_comment").isOptional());
        }
    }

    @Test
    public void partnerAttendanceRequiresADateWithoutAnyTestActionsInBothLanguages() throws Exception {
        for (String language : new String[] {"en", "sw"}) {
            useLanguage(language);
            Map<String, BaseMotherMentorVisitAction> map = actions(new PartnerFixture(true), 5);
            BaseMotherMentorVisitAction arrival = byForm(map, "mothermentor_partner_arrival");
            assertFalse(byForm(map, "mothermentor_partner_follow_up_status").isValid());
            assertFalse(byForm(map, "mothermentor_partner_testing_referral").isValid());
            assertTrue(arrival.isValid());
            assertFalse(map.values().stream().anyMatch(action -> action.getFormName().endsWith("_test")));
            answer(arrival, PARTNER_ARRIVED_AT_FACILITY, "yes");
            pending(arrival);
            answer(arrival, PARTNER_ARRIVED_AT_FACILITY, "yes",
                    PARTNER_FACILITY_ARRIVAL_DATE, "28-10-2026");
            completed(arrival);
            BaseMotherMentorVisitAction education = byForm(map, "mothermentor_partner_education");
            assertTrue(education.isValid());
            assertTrue(education.isOptional());
            answer(arrival, PARTNER_ARRIVED_AT_FACILITY, "no");
            completed(arrival);
            assertFalse(education.isValid());
        }
    }

    @Test
    public void childEidFollowUpIncludesTestStageAndClearsHiddenAnswers() throws Exception {
        for (String language : new String[] {"en", "sw"}) {
            useLanguage(language);
        followUp(
                new ChildEidFollowUpVisitInteractor(
                        MotherMentorFollowUpType.CHILD_EID.getEventType()),
                "mothermentor_child_eid_",
                CHILD_EID_NON_ATTENDANCE_REASON,
                CHILD_EID_NON_ATTENDANCE_REASON_OTHER,
                CHILD_EID_EXPECTED_FACILITY_VISIT_DATE,
                CHILD_EID_ARRIVED_AT_FACILITY,
                CHILD_EID_FACILITY_ARRIVAL_DATE,
                true);
        }
    }

    private void followUp(
            BaseMotherMentorServiceVisitInteractor interactor,
            String prefix,
            String reasonKey,
            String otherKey,
            String expectedDateKey,
            String arrivalKey,
            String arrivalDateKey,
            boolean child)
            throws Exception {
        boolean mother = "mothermentor_".equals(prefix);
        Map<String, BaseMotherMentorVisitAction> actions =
                actions(interactor, child ? 5 : 4);
        BaseMotherMentorVisitAction status = byForm(actions, prefix + "follow_up_status");
        BaseMotherMentorVisitAction reason =
                mother ? byForm(actions, prefix + "service_interruption_reason") : null;
        if (!mother) {
            for (BaseMotherMentorVisitAction action : actions.values()) {
                assertNotEquals(prefix + "non_attendance_reason", action.getFormName());
            }
        }
        BaseMotherMentorVisitAction arrival =
                byForm(actions, prefix + (mother ? "return_to_facility" : "arrival"));
        BaseMotherMentorVisitAction comments = byForm(actions, prefix + "follow_up_comment");
        BaseMotherMentorVisitAction test = child ? byForm(actions, prefix + "test_type") : null;
        assertFalse(arrival.isValid());
        answer(status, FOLLOW_UP_STATUS, "found");
        assertTrue(arrival.isValid());
        if (reason != null) {
            assertTrue(reason.isValid());
            answer(reason, reasonKey, "other");
            pending(reason);
            answer(
                    reason,
                    reasonKey,
                    "other",
                    otherKey,
                    "Emulator reason",
                    expectedDateKey,
                    "18-10-2026");
            completed(reason);
        }
        answer(arrival, arrivalKey, "yes");
        pending(arrival);
        answer(arrival, arrivalKey, "yes", arrivalDateKey, "19-10-2026");
        completed(arrival);
        answer(comments, COMMENTS, "Emulator follow-up test");
        completed(comments);
        if (child) {
            answer(test, CHILD_EID_TEST_TYPE, "nine_months");
            completed(test);
            BaseMotherMentorVisitAction finalTest = byForm(actions, "mothermentor_child_eid_final_test");
            assertFalse(finalTest.isValid());
            answer(test, CHILD_EID_TEST_TYPE, "eighteen_months");
            assertTrue(finalTest.isValid());
            answer(finalTest, CHILD_EID_FINAL_HIV_TEST_DONE, "yes");
            pending(finalTest);
            answer(finalTest, CHILD_EID_FINAL_HIV_TEST_DONE, "yes",
                    CHILD_EID_FINAL_HIV_RESULT, "negative", CHILD_EID_FINAL_HIV_TEST_DATE, "19/10/2026");
            completed(finalTest);
            answer(test, CHILD_EID_TEST_TYPE, "nine_months");
            assertFalse(finalTest.isValid());
            assertNull(finalTest.getJsonPayload());
            answer(test, CHILD_EID_TEST_TYPE, "eighteen_months");
            assertTrue(finalTest.isValid());
            pending(finalTest);
            answer(finalTest, CHILD_EID_FINAL_HIV_TEST_DONE, "no");
            completed(finalTest);
            answer(arrival, arrivalKey, "no");
            assertFalse(finalTest.isValid());
            assertNull(finalTest.getJsonPayload());
        }
        answer(status, FOLLOW_UP_STATUS, "not_found");
        completed(status);
        for (BaseMotherMentorVisitAction hidden : Arrays.asList(reason, arrival, test)) {
            if (hidden == null) continue;
            assertFalse(hidden.isValid());
            assertNull(hidden.getJsonPayload());
            assertNull(hidden.getSubTitle());
            pending(hidden);
        }
        completed(comments);
        answer(status, FOLLOW_UP_STATUS, "found");
        assertTrue(arrival.isValid());
        if (reason != null) {
            assertTrue(reason.isValid());
            pending(reason);
        }
        pending(arrival);
        if (child) pending(test);
    }

    private Map<String, BaseMotherMentorVisitAction> actions(
            BaseMotherMentorServiceVisitInteractor interactor, int count) throws Exception {
        return actions(interactor, count, new MemberObject());
    }

    private Map<String, BaseMotherMentorVisitAction> actions(
            BaseMotherMentorServiceVisitInteractor interactor, int count, MemberObject member) throws Exception {
        Class<?> implementation = interactor instanceof PartnerFixture || interactor instanceof MotherServiceFixture
                ? interactor.getClass().getSuperclass() : interactor.getClass();
        assertEquals("org.smartregister.chw.mothermentor.interactor",
                implementation.getPackage().getName());
        BaseMotherMentorVisitContract.View view =
                (BaseMotherMentorVisitContract.View)
                        Proxy.newProxyInstance(
                                BaseMotherMentorVisitContract.View.class.getClassLoader(),
                                new Class<?>[] {BaseMotherMentorVisitContract.View.class},
                                (proxy, method, args) -> {
                                    if (method.getName().equals("getContext")
                                            || method.getName().equals("getMyContext"))
                                        return context;
                                    if (method.getName().equals("getEditMode")) return false;
                                    return null;
                                });
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<LinkedHashMap<String, BaseMotherMentorVisitAction>> result =
                new AtomicReference<>();
        interactor.calculateActions(
                view,
                member,
                new BaseMotherMentorVisitContract.InteractorCallBack() {
                    public void onMemberDetailsReloaded(MemberObject member) {}

                    public void onRegistrationSaved(boolean edit) {}

                    public void onSubmitted(String json) {
                        fail("Component test must not submit a visit");
                    }

                    public void preloadActions(
                            LinkedHashMap<String, BaseMotherMentorVisitAction> map) {
                        result.set(map);
                        ready.countDown();
                    }
                });
        assertTrue("Workflow callback timed out", ready.await(30, TimeUnit.SECONDS));
        assertEquals(count, result.get().size());
        Set<Class<?>> helpers = new HashSet<>();
        for (BaseMotherMentorVisitAction action : result.get().values()) {
            Class<?> helper = action.getMotherMentorVisitActionHelper().getClass();
            assertEquals(
                    "org.smartregister.chw.mothermentor.actionhelper",
                    helper.getPackage().getName());
            assertTrue("Each action needs a separate helper", helpers.add(helper));
            assertFalse(helper.isMemberClass());
        }
        return result.get();
    }

    private BaseMotherMentorVisitAction byForm(
            Map<String, BaseMotherMentorVisitAction> actions, String form) {
        for (BaseMotherMentorVisitAction action : actions.values()) {
            if (form.equals(action.getFormName())) return action;
        }
        throw new AssertionError("Missing action: " + form);
    }

    private void answer(BaseMotherMentorVisitAction action, Object... answers) throws Exception {
        JSONObject form;
        try (InputStream input =
                        context.getAssets().open(formFolder + "/" + action.getFormName() + ".json");
                Scanner scanner = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
            form = new JSONObject(scanner.next());
        }
        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        for (int i = 0; i < answers.length; i += 2) {
            boolean found = false;
            for (int j = 0; j < fields.length(); j++) {
                JSONObject field = fields.getJSONObject(j);
                if (answers[i].equals(field.optString("key"))) {
                    field.put("value", answers[i + 1]);
                    found = true;
                    break;
                }
            }
            assertTrue("Missing packaged form field: " + answers[i], found);
        }
        action.setJsonPayload(form.toString());
    }

    private void useLanguage(String language) {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration configuration = new Configuration(target.getResources().getConfiguration());
        configuration.setLocale(new Locale(language));
        context = target.createConfigurationContext(configuration);
        formFolder = "sw".equals(language) ? "json.form-sw" : "json.form";
    }

    private static class PartnerFixture extends PartnerFollowUpVisitInteractor {
        private final boolean attendance;

        PartnerFixture(boolean attendance) {
            super(MotherMentorFollowUpType.PARTNER.getEventType());
            this.attendance = attendance;
        }

        @Override
        protected MotherMentorPartnerFollowUpDao.Context loadContext(
                String partnerId, String excludedVisitId, Date serviceDate) {
            return new MotherMentorPartnerFollowUpDao.Context(
                    "component-link", "component-mother", "component-pregnancy", "yes", attendance, true);
        }
    }

    private static final class MotherServiceFixture extends MotherMentorServiceVisitInteractor {
        private final String pregnancyStatus;
        private final String dob;

        MotherServiceFixture(String pregnancyStatus, String dob) {
            super(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
            this.pregnancyStatus = pregnancyStatus;
            this.dob = dob;
        }

        @Override
        protected MotherMentorMotherVisitDao.Context loadMotherContext(String memberId) {
            return new MotherMentorMotherVisitDao.Context("test-enrollment", "", pregnancyStatus, dob, dob, "", 0);
        }
    }

    private void completed(BaseMotherMentorVisitAction action) {
        assertEquals(
                action.getFormName(),
                BaseMotherMentorVisitAction.Status.COMPLETED,
                action.getActionStatus());
    }

    private void pending(BaseMotherMentorVisitAction action) {
        assertEquals(
                action.getFormName(),
                BaseMotherMentorVisitAction.Status.PENDING,
                action.getActionStatus());
    }
}
