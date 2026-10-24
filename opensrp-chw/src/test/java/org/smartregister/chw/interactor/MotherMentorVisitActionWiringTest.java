package org.smartregister.chw.interactor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.Application;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.contract.BaseMotherMentorVisitContract;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.interactor.BaseMotherMentorServiceVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.ChildEidFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.MotherMentorFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.MotherMentorHouseholdVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.MotherMentorServiceVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.PartnerFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.util.AppExecutors;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.clientandeventmodel.Event;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorVisitActionWiringTest {
    @Test
    public void everyActionUsesItsOwnTopLevelLibraryHelper() throws Exception {
        try (MockedStatic<MotherMentorLibrary> library = mockLibrary()) {
            Set<Class<?>> helpers = new HashSet<>();
            BaseMotherMentorServiceVisitInteractor[] workflows = {
                new MotherMentorServiceVisitInteractor("services"),
                new MotherMentorHouseholdVisitInteractor("household", "ec_family"),
                new MotherMentorFollowUpVisitInteractor("iit"),
                new PartnerFollowUpVisitInteractor("partner"),
                new ChildEidFollowUpVisitInteractor("eid")
            };
            int[] actionCounts = {6, 4, 4, 3, 4};
            for (int i = 0; i < workflows.length; i++) {
                assertEquals(
                        "org.smartregister.chw.mothermentor.interactor",
                        workflows[i].getClass().getPackage().getName());
                Map<String, BaseMotherMentorVisitAction> actions = actions(workflows[i]);
                assertEquals(actionCounts[i], actions.size());
                for (BaseMotherMentorVisitAction action : actions.values()) {
                    Class<?> helper = action.getMotherMentorVisitActionHelper().getClass();
                    assertEquals(
                            "org.smartregister.chw.mothermentor.actionhelper",
                            helper.getPackage().getName());
                    assertFalse(helper.isMemberClass());
                    assertTrue(
                            "Helper reused by different actions: " + helper, helpers.add(helper));
                }
            }
            assertEquals(21, helpers.size());
        }
    }

    @Test
    public void libraryFollowUpInteractorsPreserveEventTypesTablesAndReportCategories() {
        try (MockedStatic<MotherMentorLibrary> library = mockLibrary()) {
            BaseMotherMentorServiceVisitInteractor[] workflows = {
                new MotherMentorFollowUpVisitInteractor(
                        MotherMentorFollowUpType.MOTHER_IIT.getEventType()),
                new PartnerFollowUpVisitInteractor(MotherMentorFollowUpType.PARTNER.getEventType()),
                new ChildEidFollowUpVisitInteractor(
                        MotherMentorFollowUpType.CHILD_EID.getEventType())
            };
            String[] eventTypes = {
                "MotherMentor Follow-up Visit",
                "MotherMentor Partner Follow-up Visit",
                "MotherMentor Child EID Follow-up Visit"
            };
            String[] categories = {"MOTHER_IIT", "PARTNER", "CHILD_EID"};
            for (int i = 0; i < workflows.length; i++) {
                assertEquals(
                        eventTypes[i],
                        ReflectionHelpers.callInstanceMethod(workflows[i], "getEncounterType"));
                assertEquals(
                        Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT,
                        ReflectionHelpers.callInstanceMethod(workflows[i], "getTableName"));
                Event event = new Event();
                ReflectionHelpers.callInstanceMethod(
                        workflows[i],
                        "prepareEvent",
                        ReflectionHelpers.ClassParameter.from(Event.class, event));
                final String category = categories[i];
                assertEquals(
                        1,
                        event.getObs().stream()
                                .filter(
                                        obs ->
                                                DBConstants.KEY.FOLLOW_UP_CATEGORY.equals(
                                                                obs.getFieldCode())
                                                        && category.equals(obs.getValues().get(0)))
                                .count());
                assertTrue(
                        event.getObs().stream()
                                .anyMatch(
                                        obs ->
                                                DBConstants.KEY.MOTHERMENTOR_VISIT_DATE.equals(
                                                        obs.getFieldCode())));
            }
        }
    }

    @Test
    public void householdInteractorUsesConfiguredTableAndFallsBackForNonHouseholdClients() {
        try (MockedStatic<MotherMentorLibrary> library = mockLibrary();
                MockedStatic<MotherMentorDao> dao = Mockito.mockStatic(MotherMentorDao.class)) {
            MotherMentorHouseholdVisitInteractor interactor =
                    new MotherMentorHouseholdVisitInteractor(
                            "household", "configured_family_table");
            MemberObject household = new MemberObject();
            MemberObject member = new MemberObject();
            dao.when(
                            () ->
                                    MotherMentorDao.getHouseholdMember(
                                            "family-id", "configured_family_table"))
                    .thenReturn(household);
            dao.when(() -> MotherMentorDao.getVisitMember("member-id")).thenReturn(member);
            assertSame(household, interactor.getMemberClient("family-id", null));
            dao.verify(() -> MotherMentorDao.getVisitMember("family-id"), never());
            assertSame(member, interactor.getMemberClient("member-id", null));
            assertNull(interactor.getMemberClient("", null));
        }
    }

    @Test
    public void changingTracingStatusClearsHiddenAnswersAndTheirCompletionStateInEveryCategory()
            throws Exception {
        try (MockedStatic<MotherMentorLibrary> library = mockLibrary()) {
            checkHiddenAnswers(
                    new MotherMentorFollowUpVisitInteractor("iit"),
                    "mothermentor_",
                    DBConstants.KEY.SERVICE_INTERRUPTION_REASON,
                    DBConstants.KEY.EXPECTED_FACILITY_VISIT_DATE,
                    DBConstants.KEY.RETURNED_TO_FACILITY);
            checkHiddenAnswers(
                    new PartnerFollowUpVisitInteractor("partner"),
                    "mothermentor_partner_",
                    DBConstants.KEY.PARTNER_NON_ATTENDANCE_REASON,
                    DBConstants.KEY.PARTNER_EXPECTED_FACILITY_VISIT_DATE,
                    DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY);
            checkHiddenAnswers(
                    new ChildEidFollowUpVisitInteractor("eid"),
                    "mothermentor_child_eid_",
                    DBConstants.KEY.CHILD_EID_NON_ATTENDANCE_REASON,
                    DBConstants.KEY.CHILD_EID_EXPECTED_FACILITY_VISIT_DATE,
                    DBConstants.KEY.CHILD_EID_ARRIVED_AT_FACILITY);
        }
    }

    private void checkHiddenAnswers(
            BaseMotherMentorServiceVisitInteractor interactor,
            String prefix,
            String reasonKey,
            String dateKey,
            String arrivalKey)
            throws Exception {
        Map<String, BaseMotherMentorVisitAction> actions = actions(interactor);
        BaseMotherMentorVisitAction status = byForm(actions, prefix + "follow_up_status");
        boolean mother = "mothermentor_".equals(prefix);
        BaseMotherMentorVisitAction reason =
                mother ? byForm(actions, prefix + "service_interruption_reason") : null;
        if (!mother) {
            assertFalse(
                    actions.values().stream()
                            .anyMatch(
                                    action ->
                                            (prefix + "non_attendance_reason")
                                                    .equals(action.getFormName())));
        }
        BaseMotherMentorVisitAction arrival =
                byForm(
                        actions,
                        prefix
                                + ("mothermentor_".equals(prefix)
                                        ? "return_to_facility"
                                        : "arrival"));
        BaseMotherMentorVisitAction comments = byForm(actions, prefix + "follow_up_comment");
        assertFalse(arrival.isValid());
        status.setJsonPayload(form(DBConstants.KEY.FOLLOW_UP_STATUS, "found"));
        assertTrue(arrival.isValid());
        if (reason != null) {
            assertTrue(reason.isValid());
            reason.setJsonPayload(form(reasonKey, "transport_cost", dateKey, "18-10-2026"));
            assertEquals(BaseMotherMentorVisitAction.Status.COMPLETED, reason.getActionStatus());
        }
        arrival.setJsonPayload(form(arrivalKey, "no"));
        comments.setJsonPayload(form(DBConstants.KEY.COMMENTS, "Follow up"));
        BaseMotherMentorVisitAction test = null;
        if (prefix.contains("child_eid")) {
            test = byForm(actions, prefix + "test_type");
            test.setJsonPayload(form(DBConstants.KEY.CHILD_EID_TEST_TYPE, "six_weeks"));
        }
        status.setJsonPayload(form(DBConstants.KEY.FOLLOW_UP_STATUS, "not_found"));
        for (BaseMotherMentorVisitAction hidden : Arrays.asList(reason, arrival, test)) {
            if (hidden == null) continue;
            assertFalse(hidden.isValid());
            assertNull(hidden.getJsonPayload());
            assertNull(hidden.getSubTitle());
            assertEquals(BaseMotherMentorVisitAction.Status.PENDING, hidden.getActionStatus());
        }
        assertTrue(comments.isValid());
        assertEquals(BaseMotherMentorVisitAction.Status.COMPLETED, comments.getActionStatus());
        status.setJsonPayload(form(DBConstants.KEY.FOLLOW_UP_STATUS, "found"));
        assertTrue(arrival.isValid());
        if (reason != null) {
            assertTrue(reason.isValid());
            assertEquals(BaseMotherMentorVisitAction.Status.PENDING, reason.getActionStatus());
        }
        assertEquals(BaseMotherMentorVisitAction.Status.PENDING, arrival.getActionStatus());
    }

    private Map<String, BaseMotherMentorVisitAction> actions(
            BaseMotherMentorServiceVisitInteractor interactor) throws Exception {
        AppExecutors executors = new AppExecutors(Runnable::run, Runnable::run, Runnable::run);
        // Both base interactors own an executor field; keep callbacks synchronous in this test.
        for (Class<?> type = interactor.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getName().equals("appExecutors")) {
                    field.setAccessible(true);
                    field.set(interactor, executors);
                }
            }
        }
        BaseMotherMentorVisitContract.View view = mock(BaseMotherMentorVisitContract.View.class);
        when(view.getContext()).thenReturn(RuntimeEnvironment.application);
        when(view.getEditMode()).thenReturn(false);
        BaseMotherMentorVisitContract.InteractorCallBack callback =
                mock(BaseMotherMentorVisitContract.InteractorCallBack.class);
        interactor.calculateActions(view, new MemberObject(), callback);
        ArgumentCaptor<LinkedHashMap> captor = ArgumentCaptor.forClass(LinkedHashMap.class);
        verify(callback).preloadActions(captor.capture());
        return captor.getValue();
    }

    private MockedStatic<MotherMentorLibrary> mockLibrary() {
        MockedStatic<MotherMentorLibrary> mocked = Mockito.mockStatic(MotherMentorLibrary.class);
        MotherMentorLibrary library = mock(MotherMentorLibrary.class);
        org.smartregister.Context context = mock(org.smartregister.Context.class);
        when(library.context()).thenReturn(context);
        when(context.applicationContext()).thenReturn(RuntimeEnvironment.application);
        mocked.when(MotherMentorLibrary::getInstance).thenReturn(library);
        return mocked;
    }

    private BaseMotherMentorVisitAction byForm(
            Map<String, BaseMotherMentorVisitAction> actions, String name) {
        for (BaseMotherMentorVisitAction action : actions.values()) {
            if (name.equals(action.getFormName())) return action;
        }
        throw new AssertionError("Missing action: " + name);
    }

    private String form(String... values) throws Exception {
        JSONArray fields = new JSONArray();
        for (int i = 0; i < values.length; i += 2) {
            fields.put(new JSONObject().put("key", values[i]).put("value", values[i + 1]));
        }
        return new JSONObject()
                .put("count", 1)
                .put("step1", new JSONObject().put("fields", fields))
                .toString();
    }
}
