package org.smartregister.chw.interactor;

import static org.junit.Assert.*;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.*;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.mothermentor.contract.BaseMotherMentorVisitContract;
import org.smartregister.chw.mothermentor.domain.MemberObject;
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
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

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
    public void partnerFollowUpRequiresDatesAndClearsHiddenAnswers() throws Exception {
        followUp(
                new PartnerFollowUpVisitInteractor(MotherMentorFollowUpType.PARTNER.getEventType()),
                "mothermentor_partner_",
                PARTNER_NON_ATTENDANCE_REASON,
                PARTNER_NON_ATTENDANCE_REASON_OTHER,
                PARTNER_EXPECTED_FACILITY_VISIT_DATE,
                PARTNER_ARRIVED_AT_FACILITY,
                PARTNER_FACILITY_ARRIVAL_DATE,
                false);
    }

    @Test
    public void childEidFollowUpIncludesTestStageAndClearsHiddenAnswers() throws Exception {
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
                actions(interactor, mother || child ? 4 : 3);
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
        assertEquals(
                "org.smartregister.chw.mothermentor.interactor",
                interactor.getClass().getPackage().getName());
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
                        context.getAssets().open("json.form/" + action.getFormName() + ".json");
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
