package org.smartregister.chw.due;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitCompletion;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Exercises appointment projection from actual persisted visit JSON and enrollment rows. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorDueServiceProviderTest {
    private static final LocalDate TODAY = new LocalDate(2026, 10, 27);
    private SQLiteDatabase db;
    private MotherMentorDueServiceProvider provider;
    private TimeZone originalZone;
    private DateTimeZone originalJodaZone;

    @Before
    public void setUp() {
        originalZone = TimeZone.getDefault();
        originalJodaZone = DateTimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Dar_es_Salaam"));
        DateTimeZone.setDefault(DateTimeZone.forID("Africa/Dar_es_Salaam"));
        db = SQLiteDatabase.create(null);
        db.execSQL("CREATE TABLE ec_mothermentor_enrollment (base_entity_id TEXT PRIMARY KEY, "
                + "is_closed INTEGER, status TEXT, screening_status TEXT)");
        db.execSQL("CREATE TABLE visits (base_entity_id TEXT, visit_id TEXT PRIMARY KEY, visit_type TEXT, "
                + "visit_date INTEGER, visit_json TEXT, processed INTEGER, updated_at INTEGER, form_submission_id TEXT)");
        db.execSQL("CREATE TABLE event (eventId TEXT PRIMARY KEY, baseEntityId TEXT, eventType TEXT, "
                + "eventDate TEXT, json TEXT, syncStatus TEXT, validationStatus TEXT, formSubmissionId TEXT, updatedAt INTEGER)");
        net.zetetic.database.sqlcipher.SQLiteDatabase cipher = mock(net.zetetic.database.sqlcipher.SQLiteDatabase.class);
        when(cipher.rawQuery(anyString(), any(String[].class)))
                .thenAnswer(call -> db.rawQuery(call.getArgument(0), call.getArgument(1)));
        when(cipher.rawQuery(anyString(), isNull()))
                .thenAnswer(call -> db.rawQuery(call.getArgument(0), null));
        provider = new MotherMentorDueServiceProvider(cipher);
    }

    @After
    public void tearDown() {
        db.close();
        TimeZone.setDefault(originalZone);
        DateTimeZone.setDefault(originalJodaZone);
    }

    @Test
    public void appointmentTodayIsDueYesterdayIsOverdueAndTomorrowIsNotListed() throws Exception {
        enroll("today", "yesterday", "tomorrow");
        visit("today", "t", full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00"));
        visit("yesterday", "y", full("2026-10-20", "2026-10-26", "2026-10-20T10:00:00+03:00"));
        visit("tomorrow", "f", full("20-10-2026", "28-10-2026", "2026-10-20T10:00:00+03:00"));

        ModuleDueResult result = load(TODAY, "today", "yesterday", "tomorrow");
        assertEquals(ModuleDueResult.Coverage.COMPLETE, result.getCoverage());
        assertEquals(2, result.getItems().size());
        DueServiceItem today = item(result, "today");
        assertEquals(DueServiceItem.Status.DUE, today.getStatus());
        assertEquals(TODAY, today.getDueDate());
        assertEquals(TODAY.plusDays(1), today.getOverdueFrom());
        assertEquals("today", today.getSourceEntityId());
        assertEquals(LandingDueServiceRepository.MOTHER_MENTOR, today.getModuleId());
        assertEquals(MotherMentorDueServiceProvider.SERVICE_CODE, today.getServiceCode());
        assertEquals(DueServiceItem.Status.OVERDUE, item(result, "yesterday").getStatus());
        assertEquals(Collections.singleton(MotherMentorDueServiceProvider.SERVICE_CODE), result.getSupportedServiceCodes());
    }

    @Test
    public void evaluationDateControlsMidnightRolloverWithoutRewritingVisits() throws Exception {
        enroll("client");
        visit("client", "v", full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00"));
        LocalDate beforeMidnight = new LocalDate(new DateTime("2026-10-27T23:59:59+03:00").getMillis());
        LocalDate afterMidnight = new LocalDate(new DateTime("2026-10-28T00:00:00+03:00").getMillis());
        assertEquals(DueServiceItem.Status.DUE, item(load(beforeMidnight, "client"), "client").getStatus());
        assertEquals(DueServiceItem.Status.OVERDUE, item(load(afterMidnight, "client"), "client").getStatus());
    }

    @Test
    public void onlyAuthorizedActivePrimaryClientsAreIncluded() throws Exception {
        enroll("allowed", "hidden", "closed", "screening", "notClient", "legacy");
        db.execSQL("UPDATE ec_mothermentor_enrollment SET is_closed = 1 WHERE base_entity_id = 'closed'");
        db.execSQL("UPDATE ec_mothermentor_enrollment SET screening_status = '-' WHERE base_entity_id = 'screening'");
        db.execSQL("UPDATE ec_mothermentor_enrollment SET status = 'contact' WHERE base_entity_id = 'notClient'");
        db.execSQL("UPDATE ec_mothermentor_enrollment SET status = NULL, screening_status = NULL WHERE base_entity_id = 'legacy'");
        // Partner/EID-only clients have visits but no primary Mother Mentor enrollment.
        for (String id : new String[]{"allowed", "hidden", "closed", "screening", "notClient", "legacy", "partner", "eid"}) {
            visit(id, id, full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00"));
        }
        ModuleDueResult result = load(TODAY, "allowed", "closed", "screening", "notClient", "legacy", "partner", "eid");
        assertEquals(2, result.getItems().size());
        item(result, "allowed");
        item(result, "legacy");
    }

    @Test
    public void emptyCohortAndNeverVisitedClientDoNotInventAppointments() {
        enroll("client");
        assertTrue(load(TODAY).getItems().isEmpty());
        ModuleDueResult result = load(TODAY, "client");
        assertEquals(ModuleDueResult.Coverage.COMPLETE, result.getCoverage());
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    public void incompleteOrUnprocessedDraftCannotReplaceLastCompletedAppointment() throws Exception {
        enroll("client");
        visit("client", "saved", full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00"));
        visit("client", "draft", full("26-10-2026", "05-11-2026", "2026-10-26T10:00:00+03:00"));
        db.execSQL("UPDATE visits SET processed = 0 WHERE visit_id = 'draft'");
        JSONObject incomplete = event("27-10-2026", "2026-10-27T10:00:00+03:00", "repeat", "continuing_services", null);
        visit("client", "incomplete", incomplete);
        ModuleDueResult result = load(TODAY, "client");
        assertEquals(ModuleDueResult.Coverage.COMPLETE, result.getCoverage());
        assertEquals(TODAY, item(result, "client").getDueDate());
    }

    @Test
    public void unrelatedVisitTypesDoNotReplaceServiceAppointment() throws Exception {
        enroll("client");
        visit("client", "saved", full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00"));
        visit("client", "unrelated", full("27-10-2026", "05-11-2026", "2026-10-27T10:00:00+03:00"));
        db.execSQL("UPDATE visits SET visit_type = 'MotherMentor Contact Visit' WHERE visit_id = 'unrelated'");
        assertEquals(TODAY, item(load(TODAY, "client"), "client").getDueDate());
    }

    @Test
    public void voidedServiceDoesNotSupersedePreviousAppointment() throws Exception {
        enroll("client");
        visit("client", "saved", full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00"));
        JSONObject voided = full("27-10-2026", "05-11-2026", "2026-10-27T10:00:00+03:00");
        voided.put("voided", true);
        visit("client", "voided", voided);
        assertEquals(TODAY, item(load(TODAY, "client"), "client").getDueDate());
    }

    @Test
    public void importedOlderClinicalVisitCannotSupersedeNewerAppointment() throws Exception {
        enroll("client");
        visit("client", "newer", full("25-10-2026", "05-11-2026", "2026-10-25T10:00:00+03:00"));
        visit("client", "imported", full("10-10-2026", "20-10-2026", "2026-10-10T10:00:00+03:00"));
        db.execSQL("UPDATE visits SET visit_date = ?, updated_at = ? WHERE visit_id = 'imported'",
                new Object[]{time("2026-10-27T12:00:00+03:00"), time("2026-10-27T12:00:00+03:00")});
        assertTrue(load(TODAY, "client").getItems().isEmpty());
    }

    @Test
    public void originalEventDateOrdersLegacyImportsWithoutVisitDateObservation() throws Exception {
        enroll("client");
        visit("client", "newer", full(null, "05-11-2026", "2026-10-25T10:00:00+03:00"));
        visit("client", "oldImport", full(null, "20-10-2026", "2026-10-10T10:00:00+03:00"));
        db.execSQL("UPDATE visits SET visit_date = ?, updated_at = ? WHERE visit_id = 'oldImport'",
                new Object[]{time("2026-10-27T12:00:00+03:00"), time("2026-10-27T12:00:00+03:00")});
        assertTrue(load(TODAY, "client").getItems().isEmpty());
    }

    @Test
    public void persistedDateSupportsLegacyVisitWithoutEitherRecordedDate() throws Exception {
        enroll("client");
        visit("client", "legacy", full(null, "27-10-2026", null));
        assertEquals(TODAY, item(load(TODAY, "client"), "client").getDueDate());
    }

    @Test
    public void sameClinicalDayUsesEventTimeBeforeImportUpdateTime() throws Exception {
        enroll("client");
        visit("client", "morning", full("25-10-2026", "20-10-2026", "2026-10-25T09:00:00+03:00"));
        visit("client", "afternoon", full("25-10-2026", "05-11-2026", "2026-10-25T15:00:00+03:00"));
        db.execSQL("UPDATE visits SET updated_at = ? WHERE visit_id = 'morning'",
                new Object[]{time("2026-10-27T12:00:00+03:00")});
        assertTrue(load(TODAY, "client").getItems().isEmpty());
    }

    @Test
    public void sameClinicalAndEventTimeUsesLatestEdit() throws Exception {
        enroll("client");
        visit("client", "first", full("25-10-2026", "20-10-2026", "2026-10-25T09:00:00+03:00"));
        visit("client", "edited", full("25-10-2026", "05-11-2026", "2026-10-25T09:00:00+03:00"));
        db.execSQL("UPDATE visits SET updated_at = ? WHERE visit_id = 'edited'",
                new Object[]{time("2026-10-27T12:00:00+03:00")});
        assertTrue(load(TODAY, "client").getItems().isEmpty());
    }

    @Test
    public void missedContactPreservesOutstandingAppointment() throws Exception {
        enroll("client");
        visit("client", "saved", full("20-10-2026", "26-10-2026", "2026-10-20T10:00:00+03:00"));
        visit("client", "missed", event("27-10-2026", "2026-10-27T10:00:00+03:00", "repeat", "not_available", null));
        DueServiceItem item = item(load(TODAY, "client"), "client");
        assertEquals(TODAY.minusDays(1), item.getDueDate());
        assertEquals(DueServiceItem.Status.OVERDUE, item.getStatus());
    }

    @Test
    public void terminalFollowupOutcomesCancelPreviousAppointment() throws Exception {
        for (String status : new String[]{"deceased", "relocated", "transferred_to_another_facility", "stopped_services"}) {
            enroll(status);
            visit(status, status + ":saved", full("20-10-2026", "26-10-2026", "2026-10-20T10:00:00+03:00"));
            JSONObject terminal = event("27-10-2026", "2026-10-27T10:00:00+03:00", "repeat", status, null);
            assertTrue(MotherMentorVisitCompletion.isComplete(terminal.getJSONArray("obs")));
            visit(status, status + ":terminal", terminal);
            assertTrue(status, load(TODAY, status).getItems().isEmpty());
        }
    }

    @Test
    public void malformedLatestJsonMakesCoveragePartialAndSuppressesUnreliableClientOnly() throws Exception {
        enroll("bad", "good");
        visit("bad", "old", full("20-10-2026", "26-10-2026", "2026-10-20T10:00:00+03:00"));
        visit("bad", "badJson", full("27-10-2026", "05-11-2026", "2026-10-27T10:00:00+03:00"));
        db.execSQL("UPDATE visits SET visit_json = '{malformed', visit_date = ? WHERE visit_id = 'badJson'",
                new Object[]{time("2026-10-27T10:00:00+03:00")});
        visit("good", "goodVisit", full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00"));
        ModuleDueResult result = load(TODAY, "bad", "good");
        assertEquals(ModuleDueResult.Coverage.PARTIAL, result.getCoverage());
        assertEquals(1, result.getItems().size());
        assertEquals("good", result.getItems().get(0).getClientId());
    }

    @Test
    public void invalidAppointmentDateCannotBecomeDueThroughLenientParsing() throws Exception {
        enroll("client");
        visit("client", "old", full("20-10-2026", "26-10-2026", "2026-10-20T10:00:00+03:00"));
        visit("client", "invalid", full("25-10-2026", "31-02-2026", "2026-10-25T10:00:00+03:00"));
        ModuleDueResult result = load(TODAY, "client");
        assertEquals(ModuleDueResult.Coverage.PARTIAL, result.getCoverage());
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    public void invalidClinicalDateDoesNotFallBackToImportDateAndReportOldAppointment() throws Exception {
        enroll("client");
        visit("client", "old", full("20-10-2026", "26-10-2026", "2026-10-20T10:00:00+03:00"));
        visit("client", "invalid", full("31-02-2026", "05-11-2026", "2026-10-27T10:00:00+03:00"));
        ModuleDueResult result = load(TODAY, "client");
        assertEquals(ModuleDueResult.Coverage.PARTIAL, result.getCoverage());
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    public void completedVisitRescheduleEditAndDeletionAreReflectedOnNextLoad() throws Exception {
        enroll("client");
        visit("client", "old", full("20-10-2026", "26-10-2026", "2026-10-20T10:00:00+03:00"));
        DueServiceItem original = item(load(TODAY, "client"), "client");
        assertEquals(DueServiceItem.Status.OVERDUE, original.getStatus());

        visit("client", "new", full("27-10-2026", "05-11-2026", "2026-10-27T10:00:00+03:00"));
        assertTrue(load(TODAY, "client").getItems().isEmpty());

        db.execSQL("UPDATE visits SET visit_json = ? WHERE visit_id = 'new'",
                new Object[]{full("27-10-2026", "27-10-2026", "2026-10-27T10:00:00+03:00").toString()});
        DueServiceItem edited = item(load(TODAY, "client"), "client");
        assertEquals(DueServiceItem.Status.DUE, edited.getStatus());
        assertEquals(original.getOccurrenceKey(), edited.getOccurrenceKey());

        db.execSQL("DELETE FROM visits WHERE visit_id = 'new'");
        assertEquals(DueServiceItem.Status.OVERDUE, item(load(TODAY, "client"), "client").getStatus());
        db.execSQL("DELETE FROM visits WHERE visit_id = 'old'");
        assertTrue(load(TODAY, "client").getItems().isEmpty());
    }

    @Test
    public void syncedValidEventProducesAppointmentWithoutMaterializedVisitRow() throws Exception {
        enroll("client");
        imported("client", "remote", "remote-form",
                full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00"));
        ModuleDueResult result = load(TODAY, "client");
        assertEquals(ModuleDueResult.Coverage.COMPLETE, result.getCoverage());
        assertEquals(1, result.getItems().size());
        assertEquals(DueServiceItem.Status.DUE, item(result, "client").getStatus());
    }

    @Test
    public void eventFallbackPreservesActivePrimaryAndAuthorizedCohortRestrictions() throws Exception {
        enroll("allowed", "hidden", "closed", "screening");
        db.execSQL("UPDATE ec_mothermentor_enrollment SET is_closed = 1 WHERE base_entity_id = 'closed'");
        db.execSQL("UPDATE ec_mothermentor_enrollment SET screening_status = '-' WHERE base_entity_id = 'screening'");
        for (String client : new String[]{"allowed", "hidden", "closed", "screening", "partnerOnly"}) {
            imported(client, client, client + "-form",
                    full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00"));
        }
        ModuleDueResult result = load(TODAY, "allowed", "closed", "screening", "partnerOnly");
        assertEquals(1, result.getItems().size());
        assertEquals("allowed", result.getItems().get(0).getClientId());
    }

    @Test
    public void newerImportedClinicalVisitSupersedesOlderLocalAppointment() throws Exception {
        enroll("client");
        visit("client", "local", full("20-10-2026", "26-10-2026", "2026-10-20T10:00:00+03:00"));
        imported("client", "remote", "remote-form",
                full("25-10-2026", "05-11-2026", "2026-10-25T10:00:00+03:00"));
        assertTrue(load(TODAY, "client").getItems().isEmpty());
    }

    @Test
    public void sameDayLocalRescheduleWinsOverOldEventLogRegardlessOfImportTimestamp() throws Exception {
        enroll("client");
        visit("client", "local", full("25-10-2026", "05-11-2026", "2026-10-25T09:00:00+03:00"));
        imported("client", "remote", "old-remote-form",
                full("25-10-2026", "26-10-2026", "2026-10-25T15:00:00+03:00"));
        db.execSQL("UPDATE event SET updatedAt = ? WHERE eventId = 'remote'",
                new Object[]{time("2026-10-27T12:00:00+03:00")});
        assertTrue(load(TODAY, "client").getItems().isEmpty());
    }

    @Test
    public void localRescheduleAlsoWinsWhenHistoricalEventWasInsertedFirst() throws Exception {
        enroll("client");
        imported("client", "remote", "old-remote-form",
                full("25-10-2026", "26-10-2026", "2026-10-25T15:00:00+03:00"));
        visit("client", "local", full("25-10-2026", "05-11-2026", "2026-10-25T09:00:00+03:00"));
        assertTrue(load(TODAY, "client").getItems().isEmpty());
    }

    @Test
    public void unprocessedLocalEditSuppressesSameDayImportedAppointment() throws Exception {
        enroll("client");
        imported("client", "remote", "old-remote-form",
                full("25-10-2026", "26-10-2026", "2026-10-25T10:00:00+03:00"));
        visit("client", "draft", event("25-10-2026", "2026-10-25T10:00:00+03:00", "repeat", "continuing_services", null));
        db.execSQL("UPDATE visits SET processed = 0 WHERE visit_id = 'draft'");
        assertTrue(load(TODAY, "client").getItems().isEmpty());
    }

    @Test
    public void representedFormSubmissionDoesNotReappearFromEventLogWithStaleClinicalDate() throws Exception {
        enroll("client");
        imported("client", "remote", "shared-form",
                full("26-10-2026", "27-10-2026", "2026-10-26T10:00:00+03:00"));
        visit("client", "local", full("25-10-2026", "05-11-2026", "2026-10-25T10:00:00+03:00"));
        db.execSQL("UPDATE visits SET form_submission_id = 'shared-form' WHERE visit_id = 'local'");
        assertTrue(load(TODAY, "client").getItems().isEmpty());
    }

    @Test
    public void deleteEventTombstoneRemovesHistoricalAppointment() throws Exception {
        enroll("client");
        imported("client", "remote", "target-form",
                full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00"));
        JSONObject deleted = new JSONObject().put("eventDate", "2026-10-26T10:00:00+03:00")
                .put("details", new JSONObject().put(
                        org.smartregister.chw.anc.util.Constants.JSON_FORM_EXTRA.DELETE_FORM_SUBMISSION_ID,
                        "target-form"));
        persistEvent("client", "delete", "Delete Event", "delete-form", deleted, "Synced", "Valid");
        assertTrue(load(TODAY, "client").getItems().isEmpty());
    }

    @Test
    public void deleteEventCanUseLegacyFormSubmissionFallback() throws Exception {
        enroll("client");
        imported("client", "remote", "target-form",
                full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00"));
        persistEvent("client", "delete", "Delete Event", "target-form",
                new JSONObject().put("eventDate", "2026-10-26T10:00:00+03:00"), "Synced", "Valid");
        assertTrue(load(TODAY, "client").getItems().isEmpty());
    }

    @Test
    public void invalidDeleteEventCannotRemoveValidAppointment() throws Exception {
        enroll("client");
        imported("client", "remote", "target-form",
                full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00"));
        JSONObject deleted = new JSONObject().put("eventDate", "2026-10-26T10:00:00+03:00")
                .put("details", new JSONObject().put(
                        org.smartregister.chw.anc.util.Constants.JSON_FORM_EXTRA.DELETE_FORM_SUBMISSION_ID,
                        "target-form"));
        persistEvent("client", "delete", "Delete Event", "delete-form", deleted, "Synced", "Invalid");
        assertEquals(TODAY, item(load(TODAY, "client"), "client").getDueDate());
    }

    @Test
    public void deletingLatestLocalEditDoesNotResurrectOlderImportedAppointmentOnSameDay() throws Exception {
        enroll("client");
        imported("client", "original", "original-form",
                full("25-10-2026", "26-10-2026", "2026-10-25T09:00:00+03:00"));
        db.execSQL("UPDATE event SET updatedAt = ? WHERE eventId = 'original'",
                new Object[]{time("2026-10-25T09:00:00+03:00")});
        visit("client", "edit", full("25-10-2026", "05-11-2026", "2026-10-25T11:00:00+03:00"));
        db.execSQL("UPDATE visits SET updated_at = ? WHERE visit_id = 'edit'",
                new Object[]{time("2026-10-25T11:00:00+03:00")});
        deletion("client", "delete-edit", "edit-form", "2026-10-25T12:00:00+03:00");

        assertTrue(load(TODAY, "client").getItems().isEmpty());
    }

    @Test
    public void deletedEditMissingLocalRowStillSuppressesOldDayUsingItsUnsyncedSourceEvent() throws Exception {
        enroll("client");
        imported("client", "original", "original-form",
                full("25-10-2026", "26-10-2026", "2026-10-25T09:00:00+03:00"));
        db.execSQL("UPDATE event SET updatedAt = ? WHERE eventId = 'original'",
                new Object[]{time("2026-10-25T09:00:00+03:00")});
        JSONObject edited = full("25-10-2026", "05-11-2026", "2026-10-25T11:00:00+03:00");
        visit("client", "edit", edited);
        persistEvent("client", "edit-event", Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES,
                "edit-form", edited, "NotSynced", "Valid");
        db.execSQL("UPDATE event SET updatedAt = ? WHERE eventId = 'edit-event'",
                new Object[]{time("2026-10-25T11:00:00+03:00")});
        deletion("client", "delete-edit", "edit-form", "2026-10-25T12:00:00+03:00");
        db.execSQL("DELETE FROM visits WHERE visit_id = 'edit'");

        assertTrue(load(TODAY, "client").getItems().isEmpty());
    }

    @Test
    public void deletingHistoricalFormPreservesNewerCurrentLocalEditOnSameClinicalDay() throws Exception {
        enroll("client");
        imported("client", "original", "original-form",
                full("25-10-2026", "26-10-2026", "2026-10-25T09:00:00+03:00"));
        db.execSQL("UPDATE event SET updatedAt = ? WHERE eventId = 'original'",
                new Object[]{time("2026-10-25T09:00:00+03:00")});
        deletion("client", "delete-original", "original-form", "2026-10-25T10:00:00+03:00");
        visit("client", "edit", full("25-10-2026", "27-10-2026", "2026-10-25T11:00:00+03:00"));
        db.execSQL("UPDATE visits SET updated_at = ? WHERE visit_id = 'edit'",
                new Object[]{time("2026-10-25T11:00:00+03:00")});

        ModuleDueResult result = load(TODAY, "client");
        assertEquals(1, result.getItems().size());
        assertEquals(TODAY, item(result, "client").getDueDate());
        assertEquals(DueServiceItem.Status.DUE, item(result, "client").getStatus());
    }

    @Test
    public void voidedImportedEventDoesNotProduceAppointment() throws Exception {
        enroll("client");
        JSONObject voided = full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00");
        voided.put("voided", true);
        imported("client", "remote", "remote-form", voided);
        assertTrue(load(TODAY, "client").getItems().isEmpty());
    }

    @Test
    public void unsyncedOrUnvalidatedEventsAreNotUsedAsCompletedAppointments() throws Exception {
        enroll("unsynced", "invalid", "unchecked", "valid");
        for (String client : new String[]{"unsynced", "invalid", "unchecked", "valid"}) {
            imported(client, client, client + "-form",
                    full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00"));
        }
        db.execSQL("UPDATE event SET syncStatus = 'Unsynced' WHERE baseEntityId = 'unsynced'");
        db.execSQL("UPDATE event SET validationStatus = 'Invalid' WHERE baseEntityId = 'invalid'");
        db.execSQL("UPDATE event SET validationStatus = NULL WHERE baseEntityId = 'unchecked'");
        db.execSQL("UPDATE event SET syncStatus = 'sYnCeD' WHERE baseEntityId = 'valid'");
        ModuleDueResult result = load(TODAY, "unsynced", "invalid", "unchecked", "valid");
        assertEquals(ModuleDueResult.Coverage.COMPLETE, result.getCoverage());
        assertEquals(1, result.getItems().size());
        assertEquals("valid", result.getItems().get(0).getClientId());
    }

    @Test
    public void malformedImportedJsonProducesPartialCoverageWithoutDroppingGoodClients() throws Exception {
        enroll("bad", "good");
        imported("bad", "bad", "bad-form",
                full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00"));
        db.execSQL("UPDATE event SET json = '{broken' WHERE eventId = 'bad'");
        imported("good", "good", "good-form",
                full("20-10-2026", "27-10-2026", "2026-10-20T10:00:00+03:00"));
        ModuleDueResult result = load(TODAY, "bad", "good");
        assertEquals(ModuleDueResult.Coverage.PARTIAL, result.getCoverage());
        assertEquals(1, result.getItems().size());
        assertEquals("good", result.getItems().get(0).getClientId());
    }

    @Test
    public void invalidImportedAppointmentDateProducesPartialCoverage() throws Exception {
        enroll("client");
        imported("client", "remote", "remote-form",
                full("20-10-2026", "31-02-2026", "2026-10-20T10:00:00+03:00"));
        ModuleDueResult result = load(TODAY, "client");
        assertEquals(ModuleDueResult.Coverage.PARTIAL, result.getCoverage());
        assertTrue(result.getItems().isEmpty());
    }

    private void enroll(String... ids) {
        for (String id : ids) db.execSQL("INSERT INTO ec_mothermentor_enrollment VALUES (?, 0, 'client', 'positive')",
                new Object[]{id});
    }

    private void visit(String client, String id, JSONObject event) {
        long persisted = time("2026-10-25T12:00:00+03:00");
        db.execSQL("INSERT INTO visits VALUES (?, ?, ?, ?, ?, 1, ?, ?)",
                new Object[]{client, id, Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES, persisted, event.toString(), persisted, id + "-form"});
    }

    private void imported(String client, String id, String formId, JSONObject event) throws Exception {
        persistEvent(client, id, Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES, formId, event, "Synced", "Valid");
    }

    private void deletion(String client, String id, String targetForm, String deletedAt) throws Exception {
        JSONObject deleted = new JSONObject().put("eventDate", deletedAt)
                .put("details", new JSONObject().put(
                        org.smartregister.chw.anc.util.Constants.JSON_FORM_EXTRA.DELETE_FORM_SUBMISSION_ID,
                        targetForm));
        persistEvent(client, id, "Delete Event", id + "-form", deleted, "Synced", "Valid");
        db.execSQL("UPDATE event SET updatedAt = ? WHERE eventId = ?", new Object[]{time(deletedAt), id});
    }

    private void persistEvent(String client, String id, String eventType, String formId,
                              JSONObject event, String syncStatus, String validationStatus) throws Exception {
        event.put("baseEntityId", client).put("eventId", id).put("eventType", eventType).put("formSubmissionId", formId);
        db.execSQL("INSERT INTO event VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{id, client, eventType,
                event.optString("eventDate", "2026-10-25T12:00:00+03:00"), event.toString(), syncStatus,
                validationStatus, formId, time("2026-10-25T12:00:00+03:00")});
    }

    private ModuleDueResult load(LocalDate date, String... clients) {
        Map<String, String> names = new LinkedHashMap<>();
        for (String client : clients) names.put(client, "Client " + client);
        return provider.load(new DueQuery(date, names));
    }

    private DueServiceItem item(ModuleDueResult result, String client) {
        for (DueServiceItem item : result.getItems()) if (client.equals(item.getClientId())) return item;
        throw new AssertionError("Missing client " + client + " in " + result.getCoverage());
    }

    private JSONObject full(String clinicalDate, String appointment, String eventDate) throws Exception {
        JSONObject event = event(clinicalDate, eventDate, "repeat", "continuing_services", appointment);
        assertTrue("Fixture must meet the module's real completion requirements",
                MotherMentorVisitCompletion.isComplete(event.getJSONArray("obs")));
        return event;
    }

    private JSONObject event(String clinicalDate, String eventDate, String attendance,
                             String status, String appointment) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(DBConstants.KEY.ATTENDANCE_TYPE, attendance);
        values.put(DBConstants.KEY.FOLLOW_UP_STATUS, status);
        if (clinicalDate != null) values.put(DBConstants.KEY.MOTHERMENTOR_VISIT_DATE, clinicalDate);
        if (appointment != null) {
            values.put(DBConstants.KEY.HAS_BEEN_LINKED_TO_PSYCHOSOCIAL_SUPPORT_GROUP, "no");
            values.put(DBConstants.KEY.HAS_BEEN_LINKED_TO_IGA_GROUP, "no");
            values.put(DBConstants.KEY.EDUCATION_PROVIDED, "no");
            values.put(DBConstants.KEY.REFERRAL_GIVEN, "no");
            values.put(DBConstants.KEY.NEXT_APPOINTMENT_DATE, appointment);
        }
        JSONArray observations = new JSONArray();
        for (Map.Entry<String, String> value : values.entrySet()) observations.put(new JSONObject()
                .put("fieldCode", value.getKey()).put("values", new JSONArray().put(value.getValue())));
        JSONObject event = new JSONObject().put("obs", observations);
        if (eventDate != null) event.put("eventDate", eventDate);
        return event;
    }

    private long time(String value) { return new DateTime(value).getMillis(); }
}
