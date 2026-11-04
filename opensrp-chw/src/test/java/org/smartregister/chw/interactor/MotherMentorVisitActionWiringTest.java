package org.smartregister.chw.interactor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.Application;
import android.database.MatrixCursor;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
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
import org.smartregister.chw.mothermentor.dao.MotherMentorPartnerFollowUpDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.interactor.BaseMotherMentorServiceVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.ChildEidFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.MotherMentorFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.MotherMentorHouseholdVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.MotherMentorServiceVisitInteractor;
import org.smartregister.chw.mothermentor.interactor.PartnerFollowUpVisitInteractor;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherChildRepository;
import org.smartregister.chw.mothermentor.util.AppExecutors;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorPartnerClosure;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.repository.Repository;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Scanner;
import org.smartregister.util.FormUtils;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorVisitActionWiringTest {
    private FormUtils previousForms;

    @Before
    public void loadTheActualLibraryFormsWithoutAnApplicationSession() throws Exception {
        previousForms = ReflectionHelpers.getStaticField(FormUtils.class, "instance");
        FormUtils forms = mock(FormUtils.class);
        when(forms.getFormJson(anyString())).thenAnswer(call -> {
            try (Scanner scanner = new Scanner(RuntimeEnvironment.getApplication().getAssets()
                    .open("json.form/" + call.getArgument(0) + ".json"), "UTF-8").useDelimiter("\\A")) {
                return new JSONObject(scanner.next());
            }
        });
        ReflectionHelpers.setStaticField(FormUtils.class, "instance", forms);
    }

    @After
    public void restoreForms() {
        ReflectionHelpers.setStaticField(FormUtils.class, "instance", previousForms);
    }

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
            int[] actionCounts = {6, 5, 4, 7, 2};
            for (int i = 0; i < workflows.length; i++) {
                assertEquals(
                        "org.smartregister.chw.mothermentor.interactor",
                        workflows[i].getClass().getPackage().getName());
                Map<String, BaseMotherMentorVisitAction> actions = actions(workflows[i]);
                assertEquals(actionCounts[i], actions.size());
                if (workflows[i] instanceof ChildEidFollowUpVisitInteractor) {
                    answer(byForm(actions, "mothermentor_child_eid_follow_up_status"),
                            DBConstants.KEY.FOLLOW_UP_STATUS, "found", DBConstants.KEY.MAMA_CHILD_STATUS, "alive");
                    assertEquals(3, actions.size());
                    assertNotNull(byForm(actions, "mothermentor_child_eid_hei"));
                }
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
            assertEquals(25, helpers.size());
        }
    }

    @Test
    public void libraryFollowUpInteractorsPreserveEventTypesTablesAndReportCategories() {
        try (MockedStatic<MotherMentorLibrary> library = mockLibrary()) {
            BaseMotherMentorServiceVisitInteractor[] workflows = {
                new MotherMentorFollowUpVisitInteractor(
                        MotherMentorFollowUpType.MOTHER_IIT.getEventType()),
                new PartnerFollowUpVisitInteractor(MotherMentorFollowUpType.PARTNER.getEventType()) {
                    @Override
                    protected MotherMentorPartnerClosure loadClosureState(
                            String memberId, String excludedVisitId) {
                        assertEquals("metadata-client", memberId);
                        assertEquals("", excludedVisitId);
                        return MotherMentorPartnerClosure.active();
                    }
                },
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
                MemberObject member = new MemberObject();
                member.setBaseEntityId("metadata-client");
                ReflectionHelpers.setField(workflows[i], "memberObject", member);
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
            checkPartnerTracingHiddenAnswers();
            checkChildTracingHiddenAnswers();
        }
    }

    private void checkChildTracingHiddenAnswers() throws Exception {
        LocalDate dob = LocalDate.now().minusMonths(10);
        MotherMentorMotherChildRepository.State clinical = new MotherMentorMotherChildRepository.State(dob);
        try (MockedStatic<MotherMentorMotherChildRepository> states =
                Mockito.mockStatic(MotherMentorMotherChildRepository.class)) {
            states.when(() -> MotherMentorMotherChildRepository.getStateForChild(
                    any(SQLiteDatabase.class), eq("child"), nullable(String.class), any(LocalDate.class)))
                    .thenReturn(clinical);
            MemberObject member = new MemberObject(); member.setBaseEntityId("child"); member.setDob(dob.toString());
            Map<String, BaseMotherMentorVisitAction> map = actions(new ChildEidFollowUpVisitInteractor("eid"), member);
            BaseMotherMentorVisitAction status = byForm(map, "mothermentor_child_eid_follow_up_status");
            BaseMotherMentorVisitAction comments = byForm(map, "mothermentor_child_eid_follow_up_comment");
            assertEquals(2, map.size());
            answer(comments, DBConstants.KEY.COMMENTS, "Keep these comments");
            answer(status, DBConstants.KEY.FOLLOW_UP_STATUS, "found");
            assertEquals(2, map.size());
            answer(status, DBConstants.KEY.MAMA_CHILD_STATUS, "alive");
            BaseMotherMentorVisitAction firstHei = byForm(map, "mothermentor_child_eid_hei");
            answer(firstHei, DBConstants.KEY.MAMA_BREASTFEEDING_STATUS, "breastfeeding",
                    DBConstants.KEY.MTOTO_REFERRAL_AGREEMENT, "agreed");
            assertEquals(BaseMotherMentorVisitAction.Status.COMPLETED, firstHei.getActionStatus());
            answer(status, DBConstants.KEY.FOLLOW_UP_STATUS, "not_found",
                    DBConstants.KEY.MTOTO_NOT_FOUND_REASON, "other",
                    DBConstants.KEY.MTOTO_NOT_FOUND_REASON_OTHER, "Away today");
            assertEquals(2, map.size());
            assertTrue(map.values().stream().noneMatch(action -> "mothermentor_child_eid_hei".equals(action.getFormName())));
            assertEquals(BaseMotherMentorVisitAction.Status.COMPLETED, comments.getActionStatus());
            answer(status, DBConstants.KEY.FOLLOW_UP_STATUS, "found", DBConstants.KEY.MAMA_CHILD_STATUS, "alive");
            BaseMotherMentorVisitAction nextHei = byForm(map, "mothermentor_child_eid_hei");
            assertNotSame(firstHei, nextHei);
            assertEquals(BaseMotherMentorVisitAction.Status.PENDING, nextHei.getActionStatus());
            assertEquals("", org.smartregister.chw.mothermentor.util.JsonFormUtils.getValue(
                    new JSONObject(nextHei.getJsonPayload()), DBConstants.KEY.MTOTO_REFERRAL_AGREEMENT));
        }
    }

    private void answer(BaseMotherMentorVisitAction action, String... values) throws Exception {
        JSONObject form = new JSONObject(action.getJsonPayload());
        for (int i = 0; i < values.length; i += 2) {
            com.vijay.jsonwizard.utils.FormUtils.getFieldFromForm(form, values[i]).put("value", values[i + 1]);
        }
        action.setJsonPayload(form.toString());
    }

    private void checkPartnerTracingHiddenAnswers() throws Exception {
        try (MockedStatic<MotherMentorPartnerFollowUpDao> dao =
                Mockito.mockStatic(MotherMentorPartnerFollowUpDao.class)) {
            dao.when(() -> MotherMentorPartnerFollowUpDao.getContext(any(), any(), any(Date.class)))
                    .thenReturn(new MotherMentorPartnerFollowUpDao.Context(
                            "link", "mother", "pregnancy", "yes", false, true));
            Map<String, BaseMotherMentorVisitAction> map = actions(new PartnerFollowUpVisitInteractor(
                    MotherMentorFollowUpType.PARTNER.getEventType()));
            BaseMotherMentorVisitAction status = byForm(map, "mothermentor_partner_follow_up_status");
            BaseMotherMentorVisitAction education = byForm(map, "mothermentor_partner_education");
            assertFalse(education.isValid());
            status.setJsonPayload(form(DBConstants.KEY.FOLLOW_UP_STATUS, "found",
                    DBConstants.KEY.PARTNER_FACILITY_AGREEMENT, "agreed"));
            assertEquals(BaseMotherMentorVisitAction.Status.COMPLETED, status.getActionStatus());
            assertTrue(education.isValid());
            assertFalse("Tracing never reveals same-visit attendance",
                    byForm(map, "mothermentor_partner_arrival").isValid());
            education.setJsonPayload(form(DBConstants.KEY.PARTNER_EDUCATION_PROVIDED, "no"));
            assertEquals(BaseMotherMentorVisitAction.Status.COMPLETED, education.getActionStatus());
            status.setJsonPayload(form(DBConstants.KEY.FOLLOW_UP_STATUS, "not_found",
                    DBConstants.KEY.PARTNER_NOT_FOUND_REASON, "travelled"));
            assertFalse(education.isValid());
            assertNull(education.getJsonPayload());
            assertNull(education.getSubTitle());
            assertEquals(BaseMotherMentorVisitAction.Status.PENDING, education.getActionStatus());
            status.setJsonPayload(form(DBConstants.KEY.FOLLOW_UP_STATUS, "found",
                    DBConstants.KEY.PARTNER_FACILITY_AGREEMENT, "refused"));
            assertTrue(education.isValid());
            assertEquals(BaseMotherMentorVisitAction.Status.PENDING, education.getActionStatus());
            for (String disease : new String[] {"hiv", "syphilis", "hepatitis_b"}) {
                assertFalse(byForm(map, "mothermentor_partner_" + disease + "_test").isValid());
            }
            assertTrue(byForm(map, "mothermentor_partner_follow_up_comment").isOptional());
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
        MemberObject member = new MemberObject();
        member.setDob(LocalDate.now().minusMonths(10).toString());
        return actions(interactor, member);
    }

    private Map<String, BaseMotherMentorVisitAction> actions(
            BaseMotherMentorServiceVisitInteractor interactor, MemberObject member) throws Exception {
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
        interactor.calculateActions(view, member, callback);
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
        Repository repository = mock(Repository.class);
        SQLiteDatabase database = mock(SQLiteDatabase.class);
        when(library.getRepository()).thenReturn(repository);
        when(repository.getReadableDatabase()).thenReturn(database);
        // Wiring uses real interactors; its empty schema has no persisted household enrollment.
        when(database.rawQuery(anyString(), any(String[].class)))
                .thenAnswer(call -> new MatrixCursor(new String[] {"name"}));
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
