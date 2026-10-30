package org.smartregister.chw.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.mothermentor.repository.VisitDetailsRepository;
import org.smartregister.chw.mothermentor.repository.VisitRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.domain.Client;
import org.smartregister.domain.Event;
import org.smartregister.domain.db.EventClient;
import org.smartregister.domain.jsonmapping.ClientClassification;
import org.smartregister.util.JsonFormUtils;

import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
