package org.smartregister.chw.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.smartregister.chw.anc.util.Constants.EVENT_TYPE.DELETE_EVENT;
import static org.smartregister.chw.anc.util.Constants.JSON_FORM_EXTRA.DELETE_FORM_SUBMISSION_ID;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.mothermentor.dao.MotherMentorMotherVisitDao;
import org.smartregister.chw.mothermentor.repository.MotherMentorChildEidRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorIitRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherClosureRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherDeliveryRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.Answers;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.Entry;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.PendingTest;
import org.smartregister.chw.mothermentor.repository.VisitDetailsRepository;
import org.smartregister.chw.mothermentor.repository.VisitRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorIitState;
import org.smartregister.chw.mothermentor.util.MotherMentorHeiSchedule.Stage;
import org.smartregister.domain.Client;
import org.smartregister.domain.Event;
import org.smartregister.domain.db.EventClient;
import org.smartregister.domain.jsonmapping.ClientClassification;
import org.smartregister.util.JsonFormUtils;

import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Exercises the real app processor and shared visit cache using rolled-back UUID fixtures only. */
@RunWith(AndroidJUnit4.class)
public class MotherMentorVisitSyncInstrumentedTest {
    private SQLiteDatabase database;
    private ChwClientProcessor processor;
    private ClientClassification classification;
    private boolean transactionStarted;
    private final List<String> syntheticClients = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Constructor<ChwClientProcessor> constructor =
                ChwClientProcessor.class.getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);
        processor = constructor.newInstance(context);
        try (InputStreamReader reader = new InputStreamReader(
                context.getAssets().open("ec_client_classification.json"), StandardCharsets.UTF_8)) {
            classification = JsonFormUtils.gson.fromJson(reader, ClientClassification.class);
        }
        assertNotNull(classification);
        assertFalse(classification.case_classification_rules.isEmpty());
        database = ChwApplication.getInstance().getRepository().getWritableDatabase();
        database.beginTransaction();
        transactionStarted = true;
    }

    @After
    public void rollBackEverySyntheticWrite() {
        if (database == null) return;
        if (transactionStarted) {
            database.endTransaction();
            transactionStarted = false;
        }
        for (String client : syntheticClients) {
            assertEquals("Synthetic visit must not survive rollback", 0,
                    count(VisitRepository.VISIT_TABLE, BASE_ENTITY_ID, client));
            assertEquals("Synthetic enrollment must not survive rollback", 0,
                    count(Constants.TABLES.MOTHERMENTOR_ENROLLMENT, BASE_ENTITY_ID, client));
        }
    }

    @Test
    public void importsAndReplaysAllFourMotherMentorVisitTypesIncludingHouseholdServices()
            throws Exception {
        EventClient[] events = {
            visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES, "mother"),
            visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES, "household"),
            visit(MotherMentorFollowUpType.MOTHER_IIT.getEventType(), "iit"),
            visit(MotherMentorFollowUpType.PARTNER.getEventType(), "partner"),
            visit(MotherMentorFollowUpType.CHILD_EID.getEventType(), "child")
        };
        for (EventClient source : events) {
            process(source);
            String visitId = assertCompletedVisit(source.getEvent());
            int detailCount = count(VisitDetailsRepository.VISIT_DETAILS_TABLE, VISIT_ID, visitId);
            assertTrue("Imported visit observations must be cached", detailCount > 0);
            assertEquals("Imported details must already be processed", 0,
                    countUnprocessedDetails(visitId));
            assertDetail(visitId, MOTHERMENTOR_VISIT_DATE, "2099-04-20");
            if (MotherMentorFollowUpType.PARTNER.getEventType()
                    .equals(source.getEvent().getEventType())) {
                String topics = detail(visitId, PARTNER_EDUCATION_TOPICS);
                for (String topic : new String[] {"hiv", "syphilis", "hepatitis_b"}) {
                    assertTrue("Topic lost from synchronized visit", topics.contains(topic));
                }
                assertDetail(visitId, PARTNER_FOLLOW_UP_SCHEMA, "2");
                assertDetail(visitId, PARTNER_FOLLOW_UP_PHASE, "tracing");
            }
            process(source);
            assertEquals("Replay must preserve the cached visit identity", visitId,
                    assertCompletedVisit(source.getEvent()));
            assertEquals("Replay must not duplicate observations", detailCount,
                    count(VisitDetailsRepository.VISIT_DETAILS_TABLE, VISIT_ID, visitId));
        }
    }

    @Test
    public void replayPreservesAMatchingLocalDraftInsteadOfFinalizingOrReplacingIt() throws Exception {
        EventClient source = visit(MotherMentorFollowUpType.PARTNER.getEventType(), "partner");
        String visitId = UUID.randomUUID().toString();
        String draftJson = new JSONObject().put("synthetic_draft", true).toString();
        ContentValues draft = new ContentValues();
        draft.put(VISIT_ID, visitId);
        draft.put(BASE_ENTITY_ID, source.getEvent().getBaseEntityId());
        draft.put(FORM_SUBMISSION_ID, source.getEvent().getFormSubmissionId());
        draft.put(VISIT_TYPE, source.getEvent().getEventType());
        draft.put(PROCESSED, 0);
        draft.put(VISIT_JSON, draftJson);
        draft.put(PRE_PROCESSED, draftJson);
        draft.put(VISIT_DATE, 1L);
        draft.put(CREATED_AT, 1L);
        draft.put(UPDATED_AT, 1L);
        database.insertOrThrow(VisitRepository.VISIT_TABLE, null, draft);
        ContentValues detail = new ContentValues();
        detail.put(VISIT_DETAILS_ID, UUID.randomUUID().toString());
        detail.put(VISIT_ID, visitId);
        detail.put(VISIT_KEY, COMMENTS);
        detail.put(DETAILS, "Synthetic unsaved draft");
        detail.put(PROCESSED, 0);
        database.insertOrThrow(VisitDetailsRepository.VISIT_DETAILS_TABLE, null, detail);
        for (int replay = 0; replay < 2; replay++) {
            process(source);
            try (Cursor rows = database.rawQuery("SELECT " + PROCESSED + ", " + VISIT_JSON
                    + " FROM " + VisitRepository.VISIT_TABLE + " WHERE " + VISIT_ID + " = ?",
                    new String[] {visitId})) {
                assertTrue(rows.moveToFirst());
                assertEquals(0, rows.getInt(0));
                assertEquals(draftJson, rows.getString(1));
            }
            assertEquals(1, count(VisitRepository.VISIT_TABLE, FORM_SUBMISSION_ID,
                    source.getEvent().getFormSubmissionId()));
            assertEquals(1, count(VisitDetailsRepository.VISIT_DETAILS_TABLE, VISIT_ID, visitId));
            assertDetail(visitId, COMMENTS, "Synthetic unsaved draft");
        }
    }

    @Test
    public void partnerEnrollmentIsNotMisclassifiedAsACompletedVisit() throws Exception {
        EventClient enrollment = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_ENROLL_PARTNER, "partner");
        process(enrollment);
        assertEquals(0, count(VisitRepository.VISIT_TABLE, FORM_SUBMISSION_ID,
                enrollment.getEvent().getFormSubmissionId()));
    }

    @Test
    public void standaloneIitSyncReplaysAgreementReturnAndDeletion() throws Exception {
        String mother = UUID.randomUUID().toString();
        syntheticClients.add(mother);
        ContentValues enrollment = new ContentValues();
        enrollment.put(BASE_ENTITY_ID, mother);
        enrollment.put(IIT_EXPECTED_RETURN_DATE, "2020-04-01");
        enrollment.put(IS_CLOSED, 0);
        database.insertOrThrow(Constants.TABLES.MOTHERMENTOR_ENROLL_IIT, null, enrollment);

        EventClient agreement = iitVisit(mother, "10", MotherMentorIitState.NONE,
                IIT_RETURN_AGREEMENT, "agreed", SERVICE_INTERRUPTION_REASON, "forgot");
        process(agreement);
        process(agreement);
        assertCompletedVisit(agreement.getEvent());
        MotherMentorIitState state = MotherMentorIitRepository.getState(database, mother, null);
        assertEquals(MotherMentorIitState.REFERRED, state.getReturnState());
        assertEquals(1, state.getVisitCount());
        assertFalse(state.isClosed());

        EventClient returned = iitVisit(mother, "20", MotherMentorIitState.REFERRED,
                RETURNED_TO_FACILITY, "yes", FACILITY_RETURN_DATE, "2020-04-18");
        process(returned);
        String visitId = assertCompletedVisit(returned.getEvent());
        assertEquals(MotherMentorIitState.RETURNED_TO_CARE,
                MotherMentorIitRepository.getState(database, mother, null).getStatus());
        assertIitEnrollmentClosed(mother, true);

        process(deleteEvent(mother, UUID.randomUUID().toString(), returned.getEvent().getFormSubmissionId()));
        assertEquals(0, count(VisitDetailsRepository.VISIT_DETAILS_TABLE, VISIT_ID, visitId));
        state = MotherMentorIitRepository.getState(database, mother, null);
        assertEquals(MotherMentorIitState.REFERRED, state.getReturnState());
        assertEquals(1, state.getVisitCount());
        assertIitEnrollmentClosed(mother, false);
    }

    private EventClient iitVisit(String mother, String day, String returnState,
            String question, String answer, String detailKey, String detailAnswer) throws Exception {
        String eventType = MotherMentorFollowUpType.MOTHER_IIT.getEventType();
        JSONArray observations = new JSONArray();
        observation(observations, MOTHERMENTOR_VISIT_DATE, day + "-04-2020");
        observation(observations, FOLLOW_UP_CATEGORY, MotherMentorFollowUpType.MOTHER_IIT.getValue());
        observation(observations, IIT_FOLLOW_UP_SCHEMA, "2");
        observation(observations, FOLLOW_UP_STATUS, "found");
        observation(observations, IIT_RETURN_STATE, returnState);
        observation(observations, question, answer);
        observation(observations, detailKey, detailAnswer);
        JSONObject json = new JSONObject().put(EVENT_BASE_ENTITY_ID, mother)
                .put(EVENT_TYPE, eventType).put(EVENT_FORM_SUBMISSION_ID, UUID.randomUUID().toString())
                .put("eventId", UUID.randomUUID().toString())
                .put(EVENT_DATE_TIMESTAMP, "2020-04-" + day + "T12:00:00.000+03:00")
                .put("entityType", Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT)
                .put("obs", observations);
        Event event = JsonFormUtils.gson.fromJson(json.toString(), Event.class);
        Client client = JsonFormUtils.gson.fromJson(
                new JSONObject().put(EVENT_BASE_ENTITY_ID, mother).toString(), Client.class);
        return new EventClient(event, client);
    }

    private void assertIitEnrollmentClosed(String mother, boolean expected) {
        try (Cursor row = database.rawQuery("SELECT " + IS_CLOSED + " FROM "
                + Constants.TABLES.MOTHERMENTOR_ENROLL_IIT + " WHERE " + BASE_ENTITY_ID + " = ?",
                new String[] {mother})) {
            assertTrue(row.moveToFirst());
            assertEquals(expected, row.getInt(0) == 1);
        }
    }

    @Test
    public void recordingTheBirthResultKeepsWikiSixPendingAndDoesNotCreateAVisit() throws Exception {
        String mother = UUID.randomUUID().toString();
        syntheticClients.add(mother);
        LocalDate today = LocalDate.now();
        LocalDate visitDay = today;
        LocalDate dueSample = today.minusDays(1);
        LocalDate dob = today.minusDays(49);
        ContentValues enrollment = new ContentValues();
        enrollment.put(BASE_ENTITY_ID, mother);
        enrollment.put(PREGNANCY_STATUS, "breast_feeding");
        enrollment.put(CHILD_DOB, dob.toString());
        enrollment.put(IS_CLOSED, 0);
        database.insertOrThrow(Constants.TABLES.MOTHERMENTOR_ENROLLMENT, null, enrollment);

        JSONArray answers = new JSONArray();
        observation(answers, MOTHERMENTOR_VISIT_DATE, visitDay.toString());
        observation(answers, MAMA_ENROLLMENT_ID, MotherMentorMotherVisitDao.getContext(database, mother).getEnrollmentId());
        observation(answers, ATTENDANCE_TYPE, "repeat");
        observation(answers, FOLLOW_UP_STATUS, "found");
        observation(answers, MAMA_TAREHE_KUJIFUNGUA, dob.toString());
        observation(answers, MAMA_CHILD_STATUS, "alive");
        observation(answers, MAMA_BREASTFEEDING_STATUS, "breastfeeding");
        observation(answers, MAMA_BIRTH_TEST_WORKFLOW, "true");
        observation(answers, MAMA_BIRTH_TEST_DONE, "yes");
        observation(answers, MAMA_BIRTH_TEST_DATE, dob.toString());
        observation(answers, MAMA_BIRTH_TEST_RESULT, "pending");
        observation(answers, MAMA_HEI_TEST_DONE, "yes");
        observation(answers, MAMA_HEI_TEST_TYPE, "six_weeks");
        observation(answers, MAMA_HEI_TEST_DATE, dueSample.toString());
        observation(answers, MAMA_HEI_TEST_RESULT, "pending");
        String source = UUID.randomUUID().toString();
        ContentValues visit = new ContentValues();
        visit.put(VISIT_ID, UUID.randomUUID().toString());
        visit.put(BASE_ENTITY_ID, mother);
        visit.put(FORM_SUBMISSION_ID, source);
        visit.put(VISIT_TYPE, Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        visit.put(PROCESSED, 1);
        visit.put(VISIT_DATE, System.currentTimeMillis() - 3000);
        visit.put(VISIT_JSON, new JSONObject().put(EVENT_FORM_SUBMISSION_ID, source)
                .put(EVENT_DATE_TIMESTAMP, DateTime.now(DateTimeZone.UTC).plusHours(3).toString())
                .put("obs", answers).toString());
        database.insertOrThrow(VisitRepository.VISIT_TABLE, null, visit);

        List<PendingTest> pending = MotherMentorMotherResultEvent.pending(database, mother, today);
        assertEquals(2, pending.size());
        PendingTest birth = pending.get(0);
        assertEquals(Stage.AT_BIRTH, birth.getStage());
        assertTrue(birth.isOverdue(today));
        Map<String, String> returned = new HashMap<>();
        returned.put(MTOTO_RECEIVED_RESULT, "negative");
        LocalDate birthReceipt = today.minusDays(2);
        LocalDate correctedBirthReceipt = today.minusDays(1);
        returned.put(MTOTO_RESULT_RECEIVED_DATE, birthReceipt.toString());
        assertTrue(MotherMentorMotherResultEvent.save(database, mother, birth, returned, today));
        pending = MotherMentorMotherResultEvent.pending(database, mother, today);
        assertEquals(1, pending.size());
        assertEquals(Stage.SIX_WEEKS, pending.get(0).getStage());
        assertEquals(1, count(VisitRepository.VISIT_TABLE, BASE_ENTITY_ID, mother));
        try (Cursor row = database.rawQuery("SELECT " + MAMA_BIRTH_TEST_RESULT + " FROM "
                + Constants.TABLES.MOTHERMENTOR_ENROLLMENT + " WHERE " + BASE_ENTITY_ID + " = ?",
                new String[] {mother})) {
            assertTrue(row.moveToFirst());
            assertEquals("negative", row.getString(0));
        }

        List<Entry> reopened = MotherMentorMotherResultEvent.visible(database, mother, today);
        assertEquals(2, reopened.size());
        Entry recordedBirth = reopened.get(1);
        assertTrue(recordedBirth.isRecorded());
        assertEquals(Stage.AT_BIRTH, recordedBirth.getSample().getStage());
        assertEquals(birthReceipt, recordedBirth.getAnswers().getReceivedDate());
        assertEquals(1, count("event", EVENT_BASE_ENTITY_ID, mother));
        assertTrue(MotherMentorMotherResultEvent.save(database, mother, recordedBirth.getId(),
                recordedBirth.getRevision(), new Answers("negative", correctedBirthReceipt, "", "")));

        // Process the persisted revision as the sync pipeline would after importing it.
        try (Cursor row = database.rawQuery("SELECT " + EVENT_JSON + " FROM event WHERE "
                + EVENT_FORM_SUBMISSION_ID + " = ?", new String[] {recordedBirth.getId()})) {
            assertTrue(row.moveToFirst());
            Event event = JsonFormUtils.gson.fromJson(row.getString(0), Event.class);
            process(new EventClient(event));
            process(new EventClient(event));
        }
        List<Entry> birthHistory = MotherMentorMotherResultEvent.history(database, mother, today);
        assertEquals(1, birthHistory.size());
        assertEquals(recordedBirth.getId(), birthHistory.get(0).getId());
        assertTrue(birthHistory.get(0).isCorrected());
        assertEquals(correctedBirthReceipt, birthHistory.get(0).getAnswers().getReceivedDate());
        assertEquals(1, count("event", EVENT_BASE_ENTITY_ID, mother));
        assertEquals(1, count(VisitRepository.VISIT_TABLE, BASE_ENTITY_ID, mother));

        Entry due = MotherMentorMotherResultEvent.visible(database, mother, today).get(0);
        assertEquals(Stage.SIX_WEEKS, due.getSample().getStage());
        assertTrue(MotherMentorMotherResultEvent.save(database, mother, due.getId(), due.getRevision(),
                new Answers("positive", dueSample, "no", "")));
        Entry confirmation = MotherMentorMotherResultEvent.visible(database, mother, today).get(0);
        assertTrue(confirmation.getSample().isAwaitingConfirmation());
        assertTrue(MotherMentorMotherResultEvent.save(database, mother, confirmation.getId(),
                confirmation.getRevision(), Answers.confirmation("yes", "pending", null)));
        assertFalse(MotherMentorMotherClosureRepository.isClosed(database, mother));
        confirmation = MotherMentorMotherResultEvent.visible(database, mother, today).get(0);
        assertTrue(confirmation.getSample().isAwaitingConfirmation());
        assertFalse(confirmation.isRecorded());
        assertTrue(MotherMentorMotherResultEvent.save(database, mother, confirmation.getId(),
                confirmation.getRevision(), new Answers("positive", today, "", "")));
        assertTrue(MotherMentorMotherClosureRepository.isClosed(database, mother));

        List<Entry> history = MotherMentorMotherResultEvent.history(database, mother, today);
        assertEquals(4, history.size());
        assertEquals(Stage.AT_BIRTH, history.get(0).getSample().getStage());
        assertEquals(correctedBirthReceipt, history.get(0).getAnswers().getReceivedDate());
        assertEquals(dueSample, history.get(1).getAnswers().getReceivedDate());
        assertEquals("pending", history.get(2).getAnswers().getResult());
        Entry recorded = history.get(3);
        assertEquals(today, recorded.getAnswers().getReceivedDate());
        assertTrue(MotherMentorMotherResultEvent.save(database, mother, recorded.getId(), recorded.getRevision(),
                new Answers("negative", today, "", "")));
        MotherMentorMotherResultEvent.rebuild(database, mother);

        history = MotherMentorMotherResultEvent.history(database, mother, today);
        assertEquals(4, history.size());
        Entry corrected = history.get(3);
        assertEquals(recorded.getId(), corrected.getId());
        assertEquals(recorded.getRecordedAt(), corrected.getRecordedAt());
        assertEquals(today, corrected.getRecordedDate());
        assertEquals(dueSample, corrected.getSample().getTestDate());
        assertEquals(today, corrected.getAnswers().getReceivedDate());
        assertEquals("negative", corrected.getAnswers().getResult());
        assertTrue(corrected.isCorrected());
        assertEquals(dueSample, history.get(1).getAnswers().getReceivedDate());
        assertEquals("positive", history.get(1).getAnswers().getResult());
        assertFalse(MotherMentorMotherClosureRepository.isClosed(database, mother));
        assertEquals(4, count("event", EVENT_BASE_ENTITY_ID, mother));
        assertEquals(1, count(VisitRepository.VISIT_TABLE, BASE_ENTITY_ID, mother));
    }

    @Test
    public void standalonePositiveClosesTheSampleChildAndKeepsTheManuallyLinkedSiblingActive() throws Exception {
        String mother = UUID.randomUUID().toString();
        syntheticClients.add(mother);
        LocalDate day = LocalDate.now();
        LocalDate sampleDay = day.minusDays(2);
        LocalDate dob = sampleDay.minusMonths(18);
        ContentValues enrollment = new ContentValues();
        enrollment.put(BASE_ENTITY_ID, mother);
        enrollment.put(PREGNANCY_STATUS, "breast_feeding");
        enrollment.put(CHILD_DOB, dob.toString());
        enrollment.put(IS_CLOSED, 0);
        database.insertOrThrow(Constants.TABLES.MOTHERMENTOR_ENROLLMENT, null, enrollment);

        String child = manualChildEidLink(mother, dob, sampleDay.minusDays(1));
        JSONArray answers = new JSONArray();
        observation(answers, MOTHERMENTOR_VISIT_DATE, sampleDay.toString());
        observation(answers, MAMA_ENROLLMENT_ID, MotherMentorMotherVisitDao.getContext(database, mother).getEnrollmentId());
        observation(answers, ATTENDANCE_TYPE, "repeat");
        observation(answers, FOLLOW_UP_STATUS, "found");
        observation(answers, MAMA_TAREHE_KUJIFUNGUA, dob.toString());
        observation(answers, MAMA_CHILD_STATUS, "alive");
        observation(answers, MAMA_BREASTFEEDING_STATUS, "breastfeeding");
        observation(answers, MAMA_HEI_TEST_DONE, "yes");
        observation(answers, MAMA_HEI_TEST_TYPE, Stage.EIGHTEEN_MONTHS.getCode());
        observation(answers, MAMA_HEI_TEST_DATE, sampleDay.toString());
        observation(answers, MAMA_HEI_TEST_RECEIVED_DATE, sampleDay.toString());
        observation(answers, MAMA_HEI_TEST_RESULT, "positive");
        observation(answers, MAMA_HEI_CONFIRMATORY_TEST_DONE, "no");
        String source = UUID.randomUUID().toString();
        ContentValues visit = new ContentValues();
        visit.put(VISIT_ID, UUID.randomUUID().toString());
        visit.put(BASE_ENTITY_ID, mother);
        visit.put(FORM_SUBMISSION_ID, source);
        visit.put(VISIT_TYPE, Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        visit.put(PROCESSED, 1);
        visit.put(VISIT_DATE, sampleDay.toDateTimeAtStartOfDay().getMillis());
        visit.put(VISIT_JSON, new JSONObject().put(EVENT_FORM_SUBMISSION_ID, source)
                .put(EVENT_DATE_TIMESTAMP, sampleDay + "T12:00:00.000+03:00").put("obs", answers).toString());
        database.insertOrThrow(VisitRepository.VISIT_TABLE, null, visit);
        String sibling = manualChildEidLink(mother, dob.plusDays(4), day.minusDays(1));

        Entry pending = MotherMentorMotherResultEvent.visible(database, mother, day).get(0);
        assertEquals(child, pending.getSample().getChildId());
        assertTrue(MotherMentorMotherClosureRepository.hasOpenSibling(database, mother, child));
        assertTrue(MotherMentorMotherResultEvent.save(database, mother, pending.getId(), pending.getRevision(),
                Answers.confirmation("yes", "positive", day)));
        assertTrue(MotherMentorChildEidRepository.getState(database, child, null).isClosed());
        assertFalse(MotherMentorChildEidRepository.getState(database, sibling, null).isClosed());
        assertFalse(MotherMentorMotherClosureRepository.isClosed(database, mother));
        assertChildEidProjection(child, true);
        assertChildEidProjection(sibling, false);

        Entry recorded = MotherMentorMotherResultEvent.history(database, mother, day).get(0);
        assertEquals(child, recorded.getSample().getChildId());
        assertTrue(MotherMentorMotherResultEvent.save(database, mother, recorded.getId(), recorded.getRevision(),
                Answers.confirmation("yes", "negative", day)));
        assertFalse(MotherMentorChildEidRepository.getState(database, child, null).isClosed());
        assertChildEidProjection(child, false);
        assertChildEidProjection(sibling, false);
    }

    private String manualChildEidLink(String mother, LocalDate birth, LocalDate linkDay) throws Exception {
        String child = UUID.randomUUID().toString();
        syntheticClients.add(child);
        ContentValues enrollment = new ContentValues();
        enrollment.put(BASE_ENTITY_ID, child);
        enrollment.put(MOTHER_BASE_ENTITY_ID, mother);
        enrollment.put(CHILD_DOB, birth.toString());
        enrollment.put(IS_CLOSED, 0);
        database.insertOrThrow(Constants.TABLES.MOTHERMENTOR_ENROLL_CHILD_EID, null, enrollment);
        JSONArray fields = new JSONArray();
        observation(fields, MOTHER_BASE_ENTITY_ID, mother);
        observation(fields, CHILD_LINKING, "yes");
        observation(fields, CHILD_DOB, birth.toString());
        String id = UUID.randomUUID().toString();
        String date = linkDay + "T10:00:00.000+03:00";
        String eventType = Constants.EVENT_TYPE.MOTHER_MENTOR_ENROLL_CHILD_EID;
        JSONObject event = new JSONObject().put(EVENT_FORM_SUBMISSION_ID, id).put(EVENT_TYPE, eventType)
                .put(EVENT_BASE_ENTITY_ID, child).put(EVENT_DATE_TIMESTAMP, date).put("obs", fields);
        ContentValues row = new ContentValues();
        row.put(EVENT_FORM_SUBMISSION_ID, id);
        row.put(EVENT_BASE_ENTITY_ID, child);
        row.put(EVENT_TYPE, eventType);
        row.put(EVENT_DATE_TIMESTAMP, date);
        row.put(EVENT_JSON, event.toString());
        database.insertOrThrow("event", null, row);
        return child;
    }

    private void assertChildEidProjection(String child, boolean closed) {
        try (Cursor row = database.rawQuery("SELECT " + IS_CLOSED + ", " + MTOTO_EID_STATUS
                + " FROM " + Constants.TABLES.MOTHERMENTOR_ENROLL_CHILD_EID + " WHERE " + BASE_ENTITY_ID + " = ?",
                new String[] {child})) {
            assertTrue(row.moveToFirst());
            assertEquals(closed ? 1 : 0, row.getInt(0));
            assertEquals(closed ? "E4" : "active", row.getString(1));
        }
    }

    @Test
    public void syncedDeletionRebuildsDeliveryFromSurvivingVisitsAndThenEnrollment() throws Exception {
        String mother = UUID.randomUUID().toString();
        syntheticClients.add(mother);
        ContentValues enrollment = new ContentValues();
        enrollment.put(BASE_ENTITY_ID, mother);
        enrollment.put(PREGNANCY_STATUS, "pregnant");
        enrollment.put(IS_CLOSED, 0);
        database.insertOrThrow(Constants.TABLES.MOTHERMENTOR_ENROLLMENT, null, enrollment);

        String olderSource = UUID.randomUUID().toString();
        String newerSource = UUID.randomUUID().toString();
        String olderVisitId = saveDeliveryVisit(mother, olderSource, "20", "delivered");
        String newerVisitId = saveDeliveryVisit(mother, newerSource, "22", "stillbirth");
        MotherMentorMotherDeliveryRepository.rebuild(database, mother);
        MotherMentorMotherClosureRepository.rebuild(database, mother);
        assertProjection(mother, "2020-04-22", "", "not_pregnant", 1);

        EventClient deletion = deleteEvent(mother, olderSource, newerSource);
        for (int replay = 0; replay < 2; replay++) {
            process(deletion);
            assertEquals(0, count(VisitRepository.VISIT_TABLE, FORM_SUBMISSION_ID, newerSource));
            assertEquals(0, count(VisitDetailsRepository.VISIT_DETAILS_TABLE, VISIT_ID, newerVisitId));
            assertEquals(1, count(VisitRepository.VISIT_TABLE, FORM_SUBMISSION_ID, olderSource));
            assertEquals(1, count(VisitDetailsRepository.VISIT_DETAILS_TABLE, VISIT_ID, olderVisitId));
            assertProjection(mother, "2020-04-20", "2020-04-20", "breast_feeding", 0);
        }

        process(deleteEvent(mother, olderSource, null));
        assertEquals(0, count(VisitRepository.VISIT_TABLE, FORM_SUBMISSION_ID, olderSource));
        assertEquals(0, count(VisitDetailsRepository.VISIT_DETAILS_TABLE, VISIT_ID, olderVisitId));
        assertProjection(mother, "", "", "pregnant", 0);
    }

    private String saveDeliveryVisit(String mother, String source, String day, String outcome)
            throws Exception {
        String visitId = UUID.randomUUID().toString();
        String eventDate = "2020-04-" + day + "T12:00:00.000+03:00";
        JSONArray observations = new JSONArray();
        observation(observations, ATTENDANCE_TYPE, "repeat");
        observation(observations, FOLLOW_UP_STATUS, "found");
        observation(observations, MOTHERMENTOR_VISIT_DATE, day + "-04-2020");
        observation(observations, VISIT_PREGNANCY_STATUS, "not_pregnant");
        observation(observations, MAMA_PREGNANCY_OUTCOME, outcome);
        observation(observations, MAMA_TAREHE_KUJIFUNGUA, day + "-04-2020");
        JSONObject event = new JSONObject().put(EVENT_FORM_SUBMISSION_ID, source)
                .put(EVENT_DATE_TIMESTAMP, eventDate).put("obs", observations);
        long time = org.joda.time.DateTime.parse(eventDate).getMillis();
        ContentValues row = new ContentValues();
        row.put(VISIT_ID, visitId);
        row.put(BASE_ENTITY_ID, mother);
        row.put(FORM_SUBMISSION_ID, source);
        row.put(VISIT_TYPE, Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        row.put(VISIT_JSON, event.toString());
        row.put(PROCESSED, 1);
        row.put(VISIT_DATE, time);
        row.put(CREATED_AT, time);
        row.put(UPDATED_AT, time);
        database.insertOrThrow(VisitRepository.VISIT_TABLE, null, row);
        ContentValues detail = new ContentValues();
        detail.put(VISIT_DETAILS_ID, UUID.randomUUID().toString());
        detail.put(VISIT_ID, visitId);
        detail.put(VISIT_KEY, MAMA_TAREHE_KUJIFUNGUA);
        detail.put(DETAILS, day + "-04-2020");
        database.insertOrThrow(VisitDetailsRepository.VISIT_DETAILS_TABLE, null, detail);
        return visitId;
    }

    private EventClient deleteEvent(String mother, String submissionId, String target)
            throws Exception {
        JSONObject eventJson = new JSONObject().put(EVENT_BASE_ENTITY_ID, mother)
                .put(EVENT_TYPE, DELETE_EVENT).put(EVENT_FORM_SUBMISSION_ID, submissionId)
                .put(EVENT_DATE_TIMESTAMP, "2020-04-23T12:00:00.000+03:00");
        if (target != null) {
            eventJson.put("details", new JSONObject().put(DELETE_FORM_SUBMISSION_ID, target));
        }
        Event event = JsonFormUtils.gson.fromJson(eventJson.toString(), Event.class);
        Client client = JsonFormUtils.gson.fromJson(
                new JSONObject().put(EVENT_BASE_ENTITY_ID, mother).toString(), Client.class);
        return new EventClient(event, client);
    }

    private void assertProjection(String mother, String delivery, String dob, String pregnancy,
            int closed) {
        try (Cursor row = database.rawQuery("SELECT " + MAMA_TAREHE_KUJIFUNGUA + ", " + CHILD_DOB
                + ", " + PREGNANCY_STATUS + ", " + IS_CLOSED + " FROM "
                + Constants.TABLES.MOTHERMENTOR_ENROLLMENT + " WHERE " + BASE_ENTITY_ID + " = ?",
                new String[] {mother})) {
            assertTrue(row.moveToFirst());
            assertEquals("Projected delivery date", delivery, row.getString(0));
            assertEquals("Projected child date of birth", dob, row.getString(1));
            assertEquals("Projected pregnancy status", pregnancy, row.getString(2));
            assertEquals("Projected closure", closed, row.getInt(3));
        }
    }

    private void process(EventClient source) throws Exception {
        processor.processEvents(classification, null, null, source,
                source.getEvent(), source.getEvent().getEventType());
    }

    private EventClient visit(String eventType, String kind) throws Exception {
        String clientId = UUID.randomUUID().toString();
        syntheticClients.add(clientId);
        JSONArray observations = new JSONArray();
        observation(observations, MOTHERMENTOR_VISIT_DATE, "20-04-2099");
        observation(observations, COMMENTS, "Synthetic rolled-back " + kind + " sync fixture");
        if ("household".equals(kind)) {
            observation(observations, PURPOSE_OF_VISIT, "education");
            observation(observations, COUNSELLED_TOTAL, "3");
        } else if ("partner".equals(kind)) {
            observation(observations, FOLLOW_UP_CATEGORY, "PARTNER");
            observation(observations, PARTNER_FOLLOW_UP_SCHEMA, "2");
            observation(observations, PARTNER_FOLLOW_UP_PHASE, "tracing");
            observation(observations, FOLLOW_UP_STATUS, "found");
            observation(observations, PARTNER_FACILITY_AGREEMENT, "agreed");
            observation(observations, PARTNER_EDUCATION_PROVIDED, "yes");
            observation(observations, PARTNER_EDUCATION_TOPICS,
                    new JSONArray().put("hiv").put("syphilis").put("hepatitis_b"));
        } else if ("child".equals(kind)) {
            observation(observations, FOLLOW_UP_CATEGORY, "CHILD_EID");
            observation(observations, FOLLOW_UP_STATUS, "found");
            observation(observations, CHILD_EID_TEST_TYPE, "eighteen_months");
        } else if ("iit".equals(kind)) {
            observation(observations, FOLLOW_UP_CATEGORY, "MOTHER_IIT");
            observation(observations, FOLLOW_UP_STATUS, "not_found");
        }
        JSONObject json = new JSONObject().put(EVENT_BASE_ENTITY_ID, clientId)
                .put(EVENT_TYPE, eventType).put(EVENT_FORM_SUBMISSION_ID, UUID.randomUUID().toString())
                .put("eventId", UUID.randomUUID().toString())
                .put(EVENT_DATE_TIMESTAMP, "2099-04-20T12:00:00.000+03:00")
                .put("entityType", Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES.equals(eventType)
                        ? Constants.TABLES.MOTHERMENTOR_SERVICES
                        : Constants.EVENT_TYPE.MOTHER_MENTOR_ENROLL_PARTNER.equals(eventType)
                                ? Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER
                                : Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT)
                .put("obs", observations);
        Event event = JsonFormUtils.gson.fromJson(json.toString(), Event.class);
        Client client = JsonFormUtils.gson.fromJson(
                new JSONObject().put(EVENT_BASE_ENTITY_ID, clientId).toString(), Client.class);
        return new EventClient(event, client);
    }

    private void observation(JSONArray observations, String key, Object answer) throws Exception {
        JSONArray values = answer instanceof JSONArray ? (JSONArray) answer : new JSONArray().put(answer);
        observations.put(new JSONObject().put("fieldCode", key).put("formSubmissionField", key)
                .put("fieldType", "concept").put("fieldDataType", "text")
                .put("values", values).put("humanReadableValues", values)
                .put("saveObsAsArray", answer instanceof JSONArray));
    }

    private String assertCompletedVisit(Event event) {
        try (Cursor rows = database.rawQuery("SELECT " + VISIT_ID + ", " + BASE_ENTITY_ID
                + ", " + VISIT_TYPE + ", " + PROCESSED + ", " + VISIT_DATE
                + " FROM " + VisitRepository.VISIT_TABLE
                + " WHERE " + FORM_SUBMISSION_ID + " = ?",
                new String[] {event.getFormSubmissionId()})) {
            assertTrue("Expected imported visit for " + event.getEventType(), rows.moveToFirst());
            assertEquals(event.getBaseEntityId(), rows.getString(1));
            assertEquals(event.getEventType(), rows.getString(2));
            assertEquals(1, rows.getInt(3));
            assertEquals("Imported visit retains its recorded event timestamp",
                    event.getEventDate().getMillis(), rows.getLong(4));
            String id = rows.getString(0);
            assertFalse("Event replay created duplicate visits", rows.moveToNext());
            return id;
        }
    }

    private int count(String table, String key, String value) {
        try (Cursor rows = database.rawQuery("SELECT COUNT(*) FROM " + table + " WHERE " + key + " = ?",
                new String[] {value})) {
            assertTrue(rows.moveToFirst());
            return rows.getInt(0);
        }
    }

    private int countUnprocessedDetails(String visitId) {
        try (Cursor rows = database.rawQuery("SELECT COUNT(*) FROM "
                + VisitDetailsRepository.VISIT_DETAILS_TABLE + " WHERE " + VISIT_ID
                + " = ? AND ifnull(" + PROCESSED + ", 0) <> 1", new String[] {visitId})) {
            assertTrue(rows.moveToFirst());
            return rows.getInt(0);
        }
    }

    private void assertDetail(String visitId, String key, String expected) {
        assertEquals(key, expected, detail(visitId, key));
    }

    private String detail(String visitId, String key) {
        try (Cursor rows = database.rawQuery("SELECT " + DETAILS + " FROM "
                + VisitDetailsRepository.VISIT_DETAILS_TABLE + " WHERE " + VISIT_ID + " = ? AND "
                + VISIT_KEY + " = ?", new String[] {visitId, key})) {
            assertTrue("Missing imported observation " + key, rows.moveToFirst());
            String value = rows.getString(0);
            assertFalse("Duplicate imported observation " + key, rows.moveToNext());
            return value;
        }
    }
}
