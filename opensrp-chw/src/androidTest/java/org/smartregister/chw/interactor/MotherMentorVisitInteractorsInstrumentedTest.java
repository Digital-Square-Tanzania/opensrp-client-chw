package org.smartregister.chw.interactor;

import static org.junit.Assert.*;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.*;

import android.content.Context;
import android.content.res.Configuration;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.mothermentor.contract.BaseMotherMentorVisitContract;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.dao.MotherMentorPartnerFollowUpDao;
import org.smartregister.chw.mothermentor.interactor.BaseMotherMentorServiceVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.ChildEidFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.MotherMentorFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.MotherMentorHouseholdVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.MotherMentorServiceVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.PartnerFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Exercises the installed library, packaged forms and real executor callbacks on Android. These
 * component tests do not log in, submit visits or modify client records.
 */
@RunWith(AndroidJUnit4.class)
public class MotherMentorVisitInteractorsInstrumentedTest {
    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private String formFolder = "json.form";

    @Test
    public void serviceActionsCompleteAndFollowAttendanceVisibility() throws Exception {
        Map<String, BaseMotherMentorVisitAction> actions =
                actions(
                        new MotherMentorServiceVisitInteractor(
                                Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES),
                        6);
        BaseMotherMentorVisitAction attendance = byForm(actions, "mothermentor_visit_type");
        BaseMotherMentorVisitAction education =
                byForm(actions, "mothermentor_visit_education_provided");
        assertFalse(education.isValid());
        answer(
                attendance,
                ATTENDANCE_TYPE,
                "repeat",
                FOLLOW_UP_STATUS,
                "continuing_services",
                MOTHERMENTOR_REPORTING_VERSION,
                "2",
                PRIMARY_MOTHERMENTOR_CLIENT,
                "true",
                VISIT_PREGNANCY_STATUS,
                "breastfeeding",
                NEW_PMTCT_ART_INITIATION,
                "no",
                IIT_ENROLLED,
                "true",
                IIT_TRACING_VISIT,
                "no");
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
            assertTrue(action.getFormName(), action.isValid());
            completed(action);
        }
        answer(attendance, ATTENDANCE_TYPE, "repeat", FOLLOW_UP_STATUS, "deceased");
        assertFalse(education.isValid());
        assertFalse(byForm(actions, "mothermentor_visit_group_linkage").isValid());
        assertFalse(byForm(actions, "mothermentor_visit_referal").isValid());
        assertFalse(byForm(actions, "mothermentor_next_visit_date").isValid());
        assertTrue(byForm(actions, "mothermentor_visit_comment").isValid());
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
            Map<String, BaseMotherMentorVisitAction> map = actions(new PartnerFixture(false), 7);
            BaseMotherMentorVisitAction status = byForm(map, "mothermentor_partner_follow_up_status");
            BaseMotherMentorVisitAction arrival = byForm(map, "mothermentor_partner_arrival");
            BaseMotherMentorVisitAction education = byForm(map, "mothermentor_partner_education");
            assertTrue(status.isValid());
            assertFalse(arrival.isValid());
            answer(status, FOLLOW_UP_STATUS, "found");
            pending(status);
            answer(status, FOLLOW_UP_STATUS, "found", PARTNER_FACILITY_AGREEMENT, "agreed");
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
    public void partnerAttendanceRequiresAllTestBranchesAndClearsHiddenResultsInBothLanguages() throws Exception {
        for (String language : new String[] {"en", "sw"}) {
            useLanguage(language);
            Map<String, BaseMotherMentorVisitAction> map = actions(new PartnerFixture(true), 7);
            BaseMotherMentorVisitAction arrival = byForm(map, "mothermentor_partner_arrival");
            assertFalse(byForm(map, "mothermentor_partner_follow_up_status").isValid());
            assertTrue(arrival.isValid());
            answer(arrival, PARTNER_ARRIVED_AT_FACILITY, "yes");
            pending(arrival);
            assertFalse(byForm(map, "mothermentor_partner_hiv_test").isValid());
            answer(arrival, PARTNER_ARRIVED_AT_FACILITY, "yes",
                    PARTNER_FACILITY_ARRIVAL_DATE, "28-10-2026");
            completed(arrival);
            BaseMotherMentorVisitAction hiv = byForm(map, "mothermentor_partner_hiv_test");
            BaseMotherMentorVisitAction syphilis = byForm(map, "mothermentor_partner_syphilis_test");
            BaseMotherMentorVisitAction hepatitis = byForm(map, "mothermentor_partner_hepatitis_b_test");
            assertTrue(hiv.isValid());
            assertFalse(syphilis.isValid());
            answer(hiv, PARTNER_HIV_TESTED, "no", PARTNER_HIV_NOT_TESTED_REASON, "already_infected");
            completed(hiv);
            assertTrue(syphilis.isValid());
            answer(syphilis, PARTNER_SYPHILIS_TESTED, "no", PARTNER_SYPHILIS_NOT_TESTED_REASON, "other");
            pending(syphilis);
            assertFalse(hepatitis.isValid());
            answer(syphilis, PARTNER_SYPHILIS_TESTED, "no", PARTNER_SYPHILIS_NOT_TESTED_REASON, "other",
                    PARTNER_SYPHILIS_NOT_TESTED_REASON_OTHER, "Component fixture reason");
            completed(syphilis);
            assertTrue(hepatitis.isValid());
            answer(hepatitis, PARTNER_HEPATITIS_B_TESTED, "yes");
            pending(hepatitis);
            answer(hepatitis, PARTNER_HEPATITIS_B_TESTED, "yes", PARTNER_HEPATITIS_B_RESULT, "negative");
            completed(hepatitis);
            BaseMotherMentorVisitAction education = byForm(map, "mothermentor_partner_education");
            answer(education, PARTNER_EDUCATION_PROVIDED, "yes",
                    PARTNER_EDUCATION_TOPICS, new JSONArray().put("hiv").put("syphilis").put("hepatitis_b"));
            completed(education);
            answer(arrival, PARTNER_ARRIVED_AT_FACILITY, "no");
            completed(arrival);
            for (BaseMotherMentorVisitAction test : Arrays.asList(hiv, syphilis, hepatitis)) {
                assertFalse(test.isValid());
                assertNull(test.getJsonPayload());
            }
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
        Class<?> implementation = interactor instanceof PartnerFixture
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
                new MemberObject(),
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
