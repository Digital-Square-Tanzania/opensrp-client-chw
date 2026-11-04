package org.smartregister.chw.repository;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.mockito.Mockito;
import org.smartregister.chw.mothermentor.repository.MotherMentorPartnerClosureRepository;
import org.smartregister.chw.mothermentor.repository.VisitRepository;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorPartnerClosure;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorPartnerRepositoryTest {
    private SQLiteDatabase db;
    private net.zetetic.database.sqlcipher.SQLiteDatabase cipher;
    private MotherMentorPartnerRepository repository;
    @Before public void setup() {
        db = SQLiteDatabase.create(null);
        db.execSQL("CREATE TABLE ec_family (base_entity_id TEXT, is_closed INTEGER)");
        db.execSQL("CREATE TABLE ec_family_member (base_entity_id TEXT, relational_id TEXT, first_name TEXT, last_name TEXT, is_closed INTEGER, gender TEXT, dob TEXT, dod TEXT)");
        db.execSQL("CREATE TABLE ec_mothermentor_enrollment (base_entity_id TEXT, ctc_number TEXT, is_closed INTEGER)");
        db.execSQL("CREATE TABLE ec_mothermentor_enroll_partner (base_entity_id TEXT, relational_id TEXT, is_closed INTEGER)");
        cipher = Mockito.mock(net.zetetic.database.sqlcipher.SQLiteDatabase.class);
        Mockito.when(cipher.rawQuery(anyString(), org.mockito.ArgumentMatchers.<String[]>any())).thenAnswer(call -> db.rawQuery(call.getArgument(0), call.getArgument(1)));
        Mockito.doAnswer(call -> { db.execSQL(call.getArgument(0)); return null; }).when(cipher).execSQL(anyString());
        Mockito.when(cipher.update(anyString(), any(ContentValues.class), anyString(), any(String[].class)))
                .thenAnswer(call -> db.update(call.getArgument(0), call.getArgument(1),
                        call.getArgument(2), call.getArgument(3)));
        MotherMentorPartnerRepository.ensureColumns(cipher);
        db.execSQL("ALTER TABLE ec_mothermentor_enrollment ADD COLUMN pregnancy_status TEXT");
        db.execSQL("CREATE TABLE ec_mothermentor_followup_visit (base_entity_id TEXT, entity_id TEXT, follow_up_category TEXT, follow_up_status TEXT, partner_arrived_at_facility TEXT, partner_facility_arrival_date TEXT)");
        VisitRepository.createTable(cipher);
        db.execSQL(VisitRepository.ADD_VISIT_GROUP_COLUMN);
        MotherMentorPartnerWorkflowMigration.upgrade(cipher);
        MotherMentorPartnerClosureRepository.upgrade(cipher);
        repository = new MotherMentorPartnerRepository(cipher);
        db.execSQL("INSERT INTO ec_family VALUES ('family', 0), ('other-family', 0), ('closed-family', 1)");
        add("mother", "family", 0); add("household", "family", 0); add("other", "other-family", 0);
        add("legacy", "family", 0); add("linked", "family", 0); add("closed", "family", 1); add("closed-household", "closed-family", 0);
        db.execSQL("INSERT INTO ec_mothermentor_enrollment (base_entity_id, ctc_number, is_closed) VALUES ('mother', '12-34-5678-123456', 0)");
        db.execSQL("INSERT INTO ec_mothermentor_enroll_partner (base_entity_id, relational_id, is_closed, mother_base_entity_id) VALUES ('legacy', 'family', 0, NULL), ('linked', 'family', 0, 'mother')");
        MotherMentorPartnerClosureRepository.rebuild(cipher, "legacy");
        MotherMentorPartnerClosureRepository.rebuild(cipher, "linked");
    }
    @After public void cleanup() { db.close(); }
    @Test public void candidatesExcludeSelfClosedAndLinkedButAllowLegacy() {
        assertEquals(Arrays.asList("household", "legacy"), ids(repository.candidates("mother", "family", true)));
        assertEquals(Arrays.asList("household", "legacy", "other"), ids(repository.candidates("mother", "family", false)));
    }
    @Test public void candidatesRequireMaleAndAgeAtLeastFifteen() {
        add("female", "family", 0); add("underage", "family", 0); add("unknown", "family", 0);
        add("invalid", "family", 0); add("birthday", "family", 0);
        db.execSQL("UPDATE ec_family_member SET gender='Female' WHERE base_entity_id='female'");
        String cutoff = org.joda.time.LocalDate.now().minusYears(15).toString();
        db.execSQL("UPDATE ec_family_member SET dob=? WHERE base_entity_id='birthday'", new Object[]{cutoff});
        db.execSQL("UPDATE ec_family_member SET dob=? WHERE base_entity_id='underage'", new Object[]{org.joda.time.LocalDate.parse(cutoff).plusDays(1).toString()});
        db.execSQL("UPDATE ec_family_member SET dob=NULL WHERE base_entity_id='unknown'");
        db.execSQL("UPDATE ec_family_member SET dob='2000-02-31' WHERE base_entity_id='invalid'");
        assertEquals(Arrays.asList("birthday", "household", "legacy"), ids(repository.candidates("mother", "family", true)));
    }
    @Test public void otherClientsRetainTheirOwnHousehold() {
        assertEquals("other-family", repository.findClient("other").familyId());
        assertNull(repository.findClient("closed"));
        assertNull(repository.findClient("closed-household"));
    }
    @Test public void onlyActivePrimaryMothersCanLink() {
        assertNotNull(repository.findMother("mother"));
        assertNull(repository.findMother("household"));
        db.execSQL("UPDATE ec_mothermentor_enrollment SET is_closed = 1");
        assertNull(repository.findMother("mother"));
    }
    @Test public void additiveMigrationIsRepeatableAndPreservesLegacyData() {
        MotherMentorPartnerRepository.ensureColumns(cipher);
        MotherMentorPartnerRepository.ensureColumns(cipher);
        assertEquals("family", repository.existingEnrollment("legacy").get("relational_id"));
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(ec_mothermentor_enroll_partner)", null)) { assertEquals(19, cursor.getCount()); }
    }
    @Test public void legacyEnrollmentCannotBypassEligibilityOrOverwriteExistingLink() {
        assertTrue(repository.canEnrollPartner("other"));
        assertFalse(repository.canEnrollPartner("linked"));
        assertFalse(repository.canEnrollPartner("legacy"));
        db.execSQL("UPDATE ec_family_member SET gender='Female' WHERE base_entity_id='other'");
        assertFalse(repository.canEnrollPartner("other"));
        db.execSQL("UPDATE ec_family_member SET gender='Male',dob=? WHERE base_entity_id='other'",
                new Object[]{org.joda.time.LocalDate.now().minusYears(14).toString()});
        assertFalse(repository.canEnrollPartner("other"));
    }
    @Test(expected = IllegalArgumentException.class) public void savingRechecksAgeAfterSelection() throws Exception {
        db.execSQL("UPDATE ec_family_member SET dob=? WHERE base_entity_id='household'",
                new Object[]{org.joda.time.LocalDate.now().minusYears(14).toString()});
        repository.save(null, "mother", "household", null);
    }
    @Test public void newLinkImmediatelyRemovesCandidate() {
        db.execSQL("UPDATE ec_mothermentor_enroll_partner SET mother_base_entity_id = 'mother' WHERE base_entity_id = 'legacy'");
        assertEquals(Arrays.asList("household"), ids(repository.candidates("mother", "family", true)));
    }
    @Test public void historicalLegacyCompletionAllowsRelinkingAndPreservesNewLinkage() throws Exception {
        completedVisit("completed", org.joda.time.LocalDate.now().minusDays(1).toString());
        assertTrue(repository.canEnrollPartner("linked"));
        assertTrue(ids(repository.candidates("mother", "family", true)).contains("linked"));
        assertTrue(repository.existingEnrollment("linked").isEmpty());

        resetLinkage("new-link");
        assertFalse(repository.canEnrollPartner("linked"));
        assertFalse(ids(repository.candidates("mother", "family", true)).contains("linked"));
        assertEquals("new-link", repository.existingEnrollment("linked").get("partner_linkage_id"));
    }
    @Test public void sameDayLegacyAllNoAndDraftCompletionKeepTheLinkageActive() throws Exception {
        completedVisit("completed", org.joda.time.LocalDate.now().toString());
        assertFalse(repository.canEnrollPartner("linked"));
        db.execSQL("UPDATE ec_mothermentor_followup_visit SET mothermentor_visit_date = ?",
                new Object[]{org.joda.time.LocalDate.now().minusDays(1).toString()});
        db.execSQL("UPDATE visits SET processed = 0");
        assertFalse(repository.canEnrollPartner("linked"));
        assertFalse(ids(repository.candidates("mother", "family", true)).contains("linked"));
    }
    @Test public void qualifyingTestClosesImmediatelyAndAllowsFutureLinkageWithoutDeletingHistory()
            throws Exception {
        attendanceVisit("tested", org.joda.time.LocalDate.now().toString(), true, true,
                "completed", MotherMentorPartnerClosure.ATTENDED_TESTED);
        assertTrue(repository.canEnrollPartner("linked"));
        assertTrue(ids(repository.candidates("mother", "family", true)).contains("linked"));
        assertTrue(repository.existingEnrollment("linked").isEmpty());
        resetLinkage("new-link");
        assertFalse(repository.canEnrollPartner("linked"));
        assertEquals("new-link", repository.existingEnrollment("linked").get("partner_linkage_id"));
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM visits", null)) {
            assertTrue(cursor.moveToFirst());
            assertEquals(1, cursor.getInt(0));
        }
    }
    @Test public void draftQualifyingTestDoesNotCloseTheLinkage() throws Exception {
        attendanceVisit("draft-tested", org.joda.time.LocalDate.now().toString(), true, false,
                "completed", MotherMentorPartnerClosure.ATTENDED_TESTED);
        assertFalse(repository.canEnrollPartner("linked"));
        assertFalse(ids(repository.candidates("mother", "family", true)).contains("linked"));
        assertFalse(repository.existingEnrollment("linked").isEmpty());
    }
    @Test public void currentAllNoAttendanceRemainsActiveEvenWhenServiceDateIsHistorical()
            throws Exception {
        attendanceVisit("ongoing", org.joda.time.LocalDate.now().minusDays(1).toString(), false,
                true, "", MotherMentorPartnerClosure.ACTIVE);
        assertFalse(repository.canEnrollPartner("linked"));
        assertFalse(repository.existingEnrollment("linked").isEmpty());
    }
    @Test public void finalizedPartnerDeathBlocksRelinkingAcrossLinkagesButDraftOrOtherCategoryDoesNot() {
        db.execSQL("INSERT INTO ec_mothermentor_followup_visit (base_entity_id, entity_id, follow_up_category, "
                + "follow_up_status, partner_follow_up_schema, partner_follow_up_phase, partner_not_found_reason, "
                + "partner_follow_up_end, partner_linkage_id) VALUES "
                + "('death', 'other', 'PARTNER', 'not_found', '2', 'tracing', 'deceased', 'deceased', 'old-link')");
        db.execSQL("INSERT INTO visits (form_submission_id, base_entity_id, processed, visit_type) "
                + "VALUES ('death', 'other', 0, 'MotherMentor Partner Follow-up Visit')");
        assertTrue(repository.canEnrollPartner("other"));
        db.execSQL("UPDATE visits SET processed = 1 WHERE form_submission_id = 'death'");
        assertFalse(repository.canEnrollPartner("other"));
        assertFalse(ids(repository.candidates("mother", "family", false)).contains("other"));
        db.execSQL("UPDATE ec_mothermentor_followup_visit SET follow_up_category = 'CHILD_EID'");
        assertTrue(repository.canEnrollPartner("other"));
        assertTrue(ids(repository.candidates("mother", "family", false)).contains("other"));
    }
    private void completedVisit(String id, String day) throws Exception {
        attendanceVisit(id, day, false, true, "completed", null);
    }
    private void attendanceVisit(String id, String day, boolean tested, boolean processed,
            String end, String status) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(DBConstants.KEY.FOLLOW_UP_CATEGORY, "PARTNER");
        values.put(DBConstants.KEY.FOLLOW_UP_STATUS, "found");
        values.put(DBConstants.KEY.MOTHERMENTOR_VISIT_DATE, day);
        values.put(DBConstants.KEY.PARTNER_FOLLOW_UP_SCHEMA, "2");
        values.put(DBConstants.KEY.PARTNER_FOLLOW_UP_PHASE, "attendance");
        values.put(DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY, "yes");
        values.put(DBConstants.KEY.PARTNER_FACILITY_ARRIVAL_DATE, day);
        values.put(DBConstants.KEY.PARTNER_FOLLOW_UP_END, end);
        values.put(DBConstants.KEY.PARTNER_LINKAGE_ID, "legacy:linked");
        values.put(DBConstants.KEY.PARTNER_EDUCATION_PROVIDED, "no");
        values.put(DBConstants.KEY.PARTNER_HIV_TESTED, tested ? "yes" : "no");
        values.put(DBConstants.KEY.PARTNER_HIV_NOT_TESTED_REASON, "stock_out");
        if (tested) values.put(DBConstants.KEY.PARTNER_HIV_RESULT, "negative");
        values.put(DBConstants.KEY.PARTNER_SYPHILIS_TESTED, "no");
        values.put(DBConstants.KEY.PARTNER_SYPHILIS_NOT_TESTED_REASON, "stock_out");
        values.put(DBConstants.KEY.PARTNER_HEPATITIS_B_TESTED, "no");
        values.put(DBConstants.KEY.PARTNER_HEPATITIS_B_NOT_TESTED_REASON, "stock_out");
        if (status != null) values.put(DBConstants.KEY.MWENZI_STATUS, status);
        ContentValues projection = new ContentValues();
        projection.put(DBConstants.KEY.BASE_ENTITY_ID, id);
        projection.put(DBConstants.KEY.ENTITY_ID, "linked");
        JSONArray observations = new JSONArray();
        for (Map.Entry<String, String> value : values.entrySet()) {
            projection.put(value.getKey(), value.getValue());
            observations.put(new JSONObject().put("fieldCode", value.getKey())
                    .put("values", new JSONArray().put(value.getValue())));
        }
        db.insertOrThrow("ec_mothermentor_followup_visit", null, projection);
        ContentValues visit = new ContentValues();
        visit.put(DBConstants.KEY.VISIT_ID, id);
        visit.put(DBConstants.KEY.FORM_SUBMISSION_ID, id);
        visit.put(DBConstants.KEY.BASE_ENTITY_ID, "linked");
        visit.put(DBConstants.KEY.PROCESSED, processed ? 1 : 0);
        visit.put(DBConstants.KEY.VISIT_TYPE, "MotherMentor Partner Follow-up Visit");
        visit.put(DBConstants.KEY.VISIT_DATE,
                org.joda.time.LocalDate.parse(day).toDateTimeAtStartOfDay().getMillis());
        visit.put(DBConstants.KEY.VISIT_JSON, new JSONObject().put("obs", observations).toString());
        db.insertOrThrow("visits", null, visit);
        MotherMentorPartnerClosureRepository.rebuild(cipher, "linked");
    }
    private void resetLinkage(String linkage) {
        ContentValues reset = new ContentValues();
        reset.put(DBConstants.KEY.PARTNER_LINKAGE_ID, linkage);
        for (Map.Entry<String, String> value : MotherMentorPartnerClosureRepository
                .observationValues(MotherMentorPartnerClosure.active()).entrySet()) {
            reset.put(value.getKey(), value.getValue());
        }
        db.update("ec_mothermentor_enroll_partner", reset, "base_entity_id = ?",
                new String[]{"linked"});
    }
    @Test public void deceasedPartnerCannotBeSelectedOrEnrolledWhenMemberRecordRemainsOpen() {
        db.execSQL("UPDATE ec_family_member SET dod='2026-10-20' WHERE base_entity_id='household'");
        assertNull(repository.findClient("household"));
        assertFalse(repository.canEnrollPartner("household"));
        assertEquals(Arrays.asList("legacy"), ids(repository.candidates("mother", "family", true)));
        assertEquals(Arrays.asList("legacy", "other"), ids(repository.candidates("mother", "family", false)));
    }
    @Test public void deceasedPartnerCannotBeRelinkedAfterPreviousEnrollmentIsClosed() {
        db.execSQL("UPDATE ec_mothermentor_enroll_partner SET is_closed=1 WHERE base_entity_id='linked'");
        db.execSQL("UPDATE ec_family_member SET dod='2026-10-20' WHERE base_entity_id='linked'");
        assertFalse(repository.canEnrollPartner("linked"));
        assertEquals(Arrays.asList("household", "legacy"), ids(repository.candidates("mother", "family", true)));
    }
    @Test(expected = IllegalArgumentException.class) public void savingRechecksPartnerDeathAfterSelection() throws Exception {
        db.execSQL("UPDATE ec_family_member SET dod='2026-10-20' WHERE base_entity_id='household'");
        repository.save(null, "mother", "household", null);
    }
    @Test(expected = IllegalArgumentException.class) public void deceasedMotherCannotLinkWhenEnrollmentWasReplayedOpen() throws Exception {
        db.execSQL("UPDATE ec_family_member SET dod='2026-10-20' WHERE base_entity_id='mother'");
        assertNull(repository.findMother("mother"));
        repository.save(null, "mother", "household", null);
    }
    private void add(String id, String family, int closed) { db.execSQL("INSERT INTO ec_family_member (base_entity_id, relational_id, first_name, last_name, is_closed, gender, dob) VALUES (?, ?, ?, 'Test', ?, 'Male', '1990-01-01')", new Object[]{id, family, id, closed}); }
    private List<String> ids(List<MotherMentorPartnerRepository.Person> people) {
        List<String> ids = new ArrayList<>(); for (MotherMentorPartnerRepository.Person person : people) ids.add(person.id()); return ids;
    }
}
