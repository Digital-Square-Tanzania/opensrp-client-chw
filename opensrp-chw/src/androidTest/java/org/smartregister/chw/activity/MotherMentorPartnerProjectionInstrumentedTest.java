package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.sync.CoreClientProcessor;
import org.smartregister.chw.domain.mother_mentor_reports.MotherMentorReportObject;
import org.smartregister.chw.mothermentor.dao.MotherMentorPartnerFollowUpDao;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.domain.Client;
import org.smartregister.domain.Event;
import org.smartregister.domain.jsonmapping.ClientField;
import org.smartregister.domain.jsonmapping.Column;
import org.smartregister.domain.jsonmapping.Table;
import org.smartregister.util.JsonFormUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Native EC mapping and report SQL, with synthetic writes rolled back even on assertion failure. */
@RunWith(AndroidJUnit4.class)
public class MotherMentorPartnerProjectionInstrumentedTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test
    public void projectedPartnerResultsExpireByLinkageAndReportFirstTestPerPregnancy() throws Exception {
        SQLiteDatabase db = ChwApplication.getInstance().getRepository().getWritableDatabase();
        assertTrue(db.getVersion() >= 58);
        String partner = UUID.randomUUID().toString();
        String mother = UUID.randomUUID().toString();
        String linkage = UUID.randomUUID().toString();
        String episode = UUID.randomUUID().toString();
        Constructor<CoreClientProcessor> constructor = CoreClientProcessor.class.getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);
        CoreClientProcessor processor = constructor.newInstance(context);
        ClientField mappings;
        try (InputStreamReader reader = new InputStreamReader(
                context.getAssets().open("ec_client_fields.json"), StandardCharsets.UTF_8)) {
            mappings = JsonFormUtils.gson.fromJson(reader, ClientField.class);
        }
        Table followUp = null;
        for (Table table : mappings.bindobjects) {
            if (Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT.equals(table.name)) followUp = table;
        }
        assertTrue("Packaged follow-up mapping is missing", followUp != null);
        MotherMentorReportObject report = new MotherMentorReportObject(date("2099-04-15"));
        db.beginTransaction();
        try {
            JSONObject before = report.getIndicatorData();
            assertEquals(0, before.getInt("query-errors"));
            insert(db, "ec_family_member", map(BASE_ENTITY_ID, partner, GENDER, "Male",
                    DOB, "2050-01-01", IS_CLOSED, "0"));
            insert(db, Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER,
                    map(BASE_ENTITY_ID, partner, MOTHER_BASE_ENTITY_ID, mother,
                            PARTNER_LINKAGE_ID, linkage, IS_CLOSED, "0", LAST_INTERACTED_WITH, "1"));
            projectVisit(db, processor, followUp, partner, mother, linkage, episode,
                    "2099-04-20", true, "negative", "hiv,hepatitis_b");
            // A saved EC projection does not finalize a draft visit.
            projectVisit(db, processor, followUp, partner, mother, linkage, episode,
                    "2099-04-19", false, "positive", "syphilis");
            assertTrue(active(db, partner, "2099-04-20"));
            assertFalse(active(db, partner, "2099-04-21"));
            JSONObject first = report.getIndicatorData();
            assertDelta(before, first, "mm-8", 1);
            assertDelta(before, first, "mm-8-hiv-negative", 1);
            assertDelta(before, first, "mm-12", 1);
            assertDelta(before, first, "mm-12-hiv", 1);
            assertDelta(before, first, "mm-12-hepatitis-b", 1);

            String relinkage = UUID.randomUUID().toString();
            db.execSQL("UPDATE " + Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER + " SET "
                    + PARTNER_LINKAGE_ID + " = ? WHERE " + BASE_ENTITY_ID + " = ?",
                    new Object[] {relinkage, partner});
            assertTrue("An old completion must not end a new linkage", active(db, partner, "2099-04-21"));
            projectVisit(db, processor, followUp, partner, mother, relinkage, episode,
                    "2099-04-21", true, "positive", "syphilis");
            JSONObject repeated = report.getIndicatorData();
            assertDelta(before, repeated, "mm-8-hiv-negative", 1);
            assertDelta(before, repeated, "mm-8-hiv-positive", 0);
            assertDelta(before, repeated, "mm-12", 1);
            assertDelta(before, repeated, "mm-12-syphilis", 1);

            // Importing an earlier qualifying test moves this pregnancy's first tests out of April.
            projectVisit(db, processor, followUp, partner, mother, linkage, episode,
                    "2099-03-20", true, "negative", "hiv");
            JSONObject earlier = report.getIndicatorData();
            assertDelta(before, earlier, "mm-8", 0);
            assertDelta(before, earlier, "mm-12", 1);
            projectVisit(db, processor, followUp, partner, mother, UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(), "2099-04-22", true, "positive", "hiv");
            JSONObject nextPregnancy = report.getIndicatorData();
            assertDelta(before, nextPregnancy, "mm-8", 1);
            assertDelta(before, nextPregnancy, "mm-8-hiv-positive", 1);
            assertEquals(0, nextPregnancy.getInt("query-errors"));
        } finally {
            // Never commit these synthetic records or alter any existing client.
            db.endTransaction();
        }
        try (Cursor rows = db.rawQuery("SELECT 1 FROM ec_family_member WHERE " + BASE_ENTITY_ID + " = ?",
                new String[] {partner})) {
            assertFalse("Synthetic cohort was not rolled back", rows.moveToFirst());
        }
    }

    /** Optional narrow probe for the explicitly created UI-test partner; never exports IDs or names. */
    @Test
    public void auditSyntheticPartnerWorkflowProjection() throws Exception {
        long started = SystemClock.elapsedRealtime();
        SQLiteDatabase db = ChwApplication.getInstance().getRepository().getReadableDatabase();
        String motherWaja = InstrumentationRegistry.getArguments()
                .getString("auditMotherWajaId", "").trim();
        Assume.assumeTrue("Pass auditMotherWajaId to select the exact UI synthetic cohort",
                !motherWaja.isEmpty());
        String mother = syntheticClientId(db, "neema", motherWaja, null, null);
        assertTrue("Requested synthetic mother WAJA was not found uniquely", mother != null);
        String partner = syntheticClientId(db, "juma", InstrumentationRegistry.getArguments()
                        .getString("auditPartnerWajaId", "").trim(), mother,
                Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER);
        String child = syntheticClientId(db, "amina", InstrumentationRegistry.getArguments()
                        .getString("auditChildWajaId", "").trim(), mother,
                Constants.TABLES.MOTHERMENTOR_ENROLL_CHILD_EID);
        MotherMentorPartnerFollowUpDao.Context state = MotherMentorPartnerFollowUpDao.getContext(
                partner, null, new Date());
        JSONObject result = new JSONObject();
        result.put("cohort_selector", "exact synthetic mother WAJA; linked clients or their exact WAJAs");
        result.put("requested_mother_waja_matched", true);
        result.put("database_version", db.getVersion());
        result.put("schema_59_present", db.getVersion() >= 59);
        result.put("binary_report_index_present", hasRow(db,
                "SELECT 1 FROM sqlite_master WHERE type = 'index' AND name = 'mothermentor_report_visit_details'"));
        boolean binaryColumns = true;
        int indexColumns = 0;
        try (Cursor rows = db.rawQuery("PRAGMA index_xinfo(mothermentor_report_visit_details)", null)) {
            while (rows.moveToNext()) {
                if (rows.getInt(rows.getColumnIndexOrThrow("key")) != 1) continue;
                binaryColumns &= "BINARY".equals(rows.getString(rows.getColumnIndexOrThrow("coll")));
                indexColumns++;
            }
        }
        result.put("report_index_both_columns_binary", binaryColumns && indexColumns == 2);
        boolean indexedLookup = false;
        try (Cursor rows = db.rawQuery("EXPLAIN QUERY PLAN SELECT MAX(details) FROM visit_details "
                + "WHERE visit_id = ? AND visit_key = ?", new String[] {"synthetic-index-probe", PARTNER_HIV_RESULT})) {
            while (rows.moveToNext()) {
                indexedLookup |= rows.getString(rows.getColumnIndexOrThrow("detail"))
                        .contains("mothermentor_report_visit_details");
            }
        }
        result.put("report_detail_lookup_uses_binary_index", indexedLookup);
        result.put("synthetic_partner_registration", registrationCheck(db, partner));
        result.put("synthetic_mother_registration", registrationCheck(db, mother));
        result.put("synthetic_maternal_enrollment", maternalEnrollmentCheck(db, mother));
        result.put("synthetic_partner_enrollment", linkedEnrollmentCheck(
                db, Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER, partner, mother));
        result.put("synthetic_maternal_finalized_service_present", mother != null && hasRow(db,
                "SELECT 1 FROM visits WHERE " + BASE_ENTITY_ID + " = ? AND " + PROCESSED
                        + " = 1 AND " + VISIT_TYPE + " = ?",
                mother, Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES));
        result.put("recent_synthetic_maternal_services", maternalServiceVisits(db, mother));
        result.put("linkage_present", !state.getLinkageId().isEmpty());
        result.put("mother_link_present", !state.getMotherId().isEmpty());
        result.put("pregnancy_episode_present", !state.getPregnancyEpisodeId().isEmpty());
        result.put("mother_pregnant", state.getMotherPregnant());
        result.put("attendance_pending", state.isAttendancePending());
        result.put("active", state.isActive());
        String[] fields = {PARTNER_FOLLOW_UP_SCHEMA, PARTNER_FOLLOW_UP_PHASE, FOLLOW_UP_STATUS,
                PARTNER_FACILITY_AGREEMENT, PARTNER_NOT_FOUND_REASON, PARTNER_FOLLOW_UP_END,
                PARTNER_ARRIVED_AT_FACILITY, PARTNER_HIV_TESTED, PARTNER_HIV_RESULT,
                PARTNER_SYPHILIS_TESTED, PARTNER_SYPHILIS_RESULT, PARTNER_HEPATITIS_B_TESTED,
                PARTNER_HEPATITIS_B_RESULT, PARTNER_EDUCATION_PROVIDED, PARTNER_EDUCATION_TOPICS};
        result.put("recent_synthetic_visits", projectedVisits(db, partner, fields));
        result.put("synthetic_child_registration", registrationCheck(db, child));
        result.put("synthetic_child_enrollment", linkedEnrollmentCheck(
                db, Constants.TABLES.MOTHERMENTOR_ENROLL_CHILD_EID, child, mother));
        String[] childFields = {CHILD_EID_FOLLOW_UP_SCHEMA, FOLLOW_UP_STATUS, CHILD_EID_TEST_TYPE,
                CHILD_EID_ARRIVED_AT_FACILITY, CHILD_EID_FINAL_HIV_TEST_DONE, CHILD_EID_FINAL_HIV_RESULT};
        result.put("recent_synthetic_child_visits", projectedVisits(db, child, childFields));
        String reportMonth = InstrumentationRegistry.getArguments().getString("auditReportMonth", "2026-10");
        assertTrue("Expected YYYY-MM report month", reportMonth.matches("[0-9]{4}-[0-9]{2}"));
        long reportStarted = SystemClock.elapsedRealtime();
        JSONObject report = new MotherMentorReportObject(date(reportMonth + "-15")).getIndicatorData();
        JSONObject reportReceipt = new JSONObject().put("month", reportMonth)
                .put("scope", "Monthly aggregate; cohort details above are anchored to the exact synthetic mother WAJA");
        for (String key : new String[]{"mm-1", "mm-2", "mm-3", "mm-6", "mm-7",
                "mm-8", "mm-8-hiv-positive", "mm-8-hiv-negative",
                "mm-8-syphilis-positive", "mm-8-syphilis-negative", "mm-8-hepatitis-b-positive",
                "mm-8-hepatitis-b-negative", "mm-8-missing-records", "mm-11", "mm-11-positive",
                "mm-11-negative", "mm-11-pending-results", "mm-11-missing-records",
                "mm-11-linkage-missing-records", "mm-12", "mm-12-hiv", "mm-12-syphilis",
                "mm-12-hepatitis-b", "mm-12-missing-records", "query-errors"}) {
            reportReceipt.put(key, report.has(key) ? report.get(key) : JSONObject.NULL);
        }
        result.put("monthly_report", reportReceipt);
        result.put("report_elapsed_ms", SystemClock.elapsedRealtime() - reportStarted);
        result.put("audit_elapsed_ms", SystemClock.elapsedRealtime() - started);
        File directory = new File(context.getExternalFilesDir(null), "issue1107-private");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (FileOutputStream output = new FileOutputStream(new File(directory, "synthetic-partner-projection.json"))) {
            output.write(result.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }

    private String syntheticClientId(SQLiteDatabase db, String firstName, String waja,
            String mother, String enrollmentTable) {
        String surname = "lower(trim(m." + LAST_NAME + ")) = 'jaribiokinara'";
        if ("neema".equals(firstName) && mother == null && enrollmentTable == null) {
            // The household head can inherit the family name without storing a member surname.
            surname = "(" + surname + " OR (trim(coalesce(m." + LAST_NAME + ", '')) = ''"
                    + " AND EXISTS (SELECT 1 FROM ec_family audit_family WHERE audit_family."
                    + BASE_ENTITY_ID + " = m." + RELATIONAL_ID + " AND lower(trim(audit_family."
                    + FIRST_NAME + ")) = 'jaribiokinara')))";
        }
        String sql = "SELECT DISTINCT m." + BASE_ENTITY_ID + " FROM ec_family_member m WHERE "
                + "lower(trim(m." + FIRST_NAME + ")) = ? AND lower(trim(m." + MIDDLE_NAME
                + ")) = 'uhakiki' AND " + surname;
        java.util.List<String> arguments = new java.util.ArrayList<>();
        arguments.add(firstName);
        if (!waja.isEmpty()) {
            assertTrue("Synthetic WAJA must contain only digits", waja.matches("[0-9]{5,20}"));
            sql += " AND trim(m." + UNIQUE_ID + ") = ?";
            arguments.add(waja);
        } else if (mother != null && enrollmentTable != null) {
            sql += " AND EXISTS (SELECT 1 FROM " + enrollmentTable + " e WHERE e."
                    + BASE_ENTITY_ID + " = m." + BASE_ENTITY_ID + " AND e."
                    + MOTHER_BASE_ENTITY_ID + " = ?)";
            arguments.add(mother);
        } else {
            return null;
        }
        try (Cursor rows = db.rawQuery(sql, arguments.toArray(new String[0]))) {
            assertTrue("Synthetic cohort selection is ambiguous; pass the client's exact WAJA",
                    rows.getCount() <= 1);
            return rows.moveToFirst() ? rows.getString(0) : null;
        }
    }

    private JSONObject registrationCheck(SQLiteDatabase db, String client) throws Exception {
        JSONObject result = new JSONObject().put("present", client != null);
        if (client == null) return result;
        result.put("family_link_present", hasRow(db, "SELECT 1 FROM ec_family_member m JOIN ec_family f ON f."
                + BASE_ENTITY_ID + " = m." + RELATIONAL_ID + " WHERE m." + BASE_ENTITY_ID + " = ?", client));
        result.put("registration_event_present", hasRow(db, "SELECT 1 FROM event WHERE "
                + "baseEntityId = ? AND eventType = 'Family Member Registration'", client));
        return result;
    }

    private JSONArray maternalServiceVisits(SQLiteDatabase db, String mother) throws Exception {
        JSONArray result = new JSONArray();
        String[] fields = {ATTENDANCE_TYPE, FOLLOW_UP_STATUS, VISIT_PREGNANCY_STATUS,
                NEW_PMTCT_ART_INITIATION, HAS_BEEN_LINKED_TO_PSYCHOSOCIAL_SUPPORT_GROUP,
                HAS_BEEN_LINKED_TO_IGA_GROUP, EDUCATION_PROVIDED, WAY_EDUCATION_PROVIDED,
                TOPICS_BEING_PROVIDED, REFERRAL_GIVEN, TYPE_OF_REFERRAL_GIVEN, NEXT_APPOINTMENT_DATE};
        try (Cursor visits = db.rawQuery("SELECT " + VISIT_ID + ", " + PROCESSED + " FROM visits WHERE "
                + BASE_ENTITY_ID + " = ? AND " + VISIT_TYPE + " = ? ORDER BY CAST(" + VISIT_DATE
                + " AS INTEGER) DESC LIMIT 3", new String[] {mother, Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES})) {
            while (visits.moveToNext()) {
                JSONObject visit = new JSONObject().put("finalized", visits.getInt(1) == 1);
                for (String field : fields) {
                    try (Cursor detail = db.rawQuery("SELECT MAX(" + DETAILS + ") FROM visit_details WHERE "
                            + VISIT_ID + " = ? AND " + VISIT_KEY + " = ?",
                            new String[] {visits.getString(0), field})) {
                        String answer = detail.moveToFirst() && !detail.isNull(0) ? detail.getString(0) : "";
                        if (NEXT_APPOINTMENT_DATE.equals(field)) {
                            visit.put("next_appointment_date_captured", !answer.trim().isEmpty());
                        } else {
                            visit.put(field, answer.isEmpty() ? JSONObject.NULL : answer);
                        }
                    }
                }
                result.put(visit);
            }
        }
        return result;
    }

    private JSONObject maternalEnrollmentCheck(SQLiteDatabase db, String mother) throws Exception {
        JSONObject result = new JSONObject().put("present", false);
        if (mother == null) return result;
        try (Cursor row = db.rawQuery("SELECT * FROM " + Constants.TABLES.MOTHERMENTOR_ENROLLMENT
                + " WHERE " + BASE_ENTITY_ID + " = ?", new String[] {mother})) {
            if (!row.moveToFirst()) return result;
            String episode = column(row, MOTHER_PREGNANCY_EPISODE_ID);
            result.put("present", true);
            result.put("ctc_number_present", !column(row, CTC_NUMBER).isEmpty());
            result.put("pregnancy_status", column(row, PREGNANCY_STATUS));
            result.put("art_status", column(row, ART_STATUS));
            result.put("pregnancy_episode_present", !episode.isEmpty());
            result.put("episode_matches_retained_enrollment_event", !episode.isEmpty() && hasRow(db,
                    "SELECT 1 FROM event WHERE baseEntityId = ? AND formSubmissionId = ? "
                            + "AND eventType IN ('Mother Mentor Enrollment', 'MotherMentor Enrollment')", mother, episode));
            result.put("enrollment_event_date_present", hasRow(db, "SELECT 1 FROM event WHERE "
                    + "baseEntityId = ? AND eventType IN ('Mother Mentor Enrollment', 'MotherMentor Enrollment') "
                    + "AND trim(coalesce(eventDate, '')) <> ''", mother));
        }
        return result;
    }

    private JSONObject linkedEnrollmentCheck(SQLiteDatabase db, String table, String client, String mother)
            throws Exception {
        JSONObject result = new JSONObject().put("present", false);
        if (client == null) return result;
        try (Cursor row = db.rawQuery("SELECT * FROM " + table + " WHERE " + BASE_ENTITY_ID + " = ?",
                new String[] {client})) {
            if (!row.moveToFirst()) return result;
            result.put("present", true);
            result.put("linked_to_synthetic_mother", mother != null && mother.equals(column(row, MOTHER_BASE_ENTITY_ID)));
            if (Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER.equals(table)) {
                result.put("linkage_present", !column(row, PARTNER_LINKAGE_ID).isEmpty());
                result.put("pregnancy_episode_present", !column(row, MOTHER_PREGNANCY_EPISODE_ID).isEmpty());
                result.put("maternal_ctc_present", !column(row, MOTHER_CTC_NUMBER).isEmpty());
                result.put("maternal_ctc_matches_enrollment", mother != null
                        && !column(row, MOTHER_CTC_NUMBER).isEmpty() && hasRow(db,
                        "SELECT 1 FROM " + Constants.TABLES.MOTHERMENTOR_ENROLLMENT + " WHERE "
                                + BASE_ENTITY_ID + " = ? AND " + CTC_NUMBER + " = ?",
                        mother, column(row, MOTHER_CTC_NUMBER)));
                result.put("pregnancy_episode_matches_maternal_enrollment", mother != null
                        && !column(row, MOTHER_PREGNANCY_EPISODE_ID).isEmpty() && hasRow(db,
                        "SELECT 1 FROM " + Constants.TABLES.MOTHERMENTOR_ENROLLMENT + " WHERE "
                                + BASE_ENTITY_ID + " = ? AND " + MOTHER_PREGNANCY_EPISODE_ID + " = ?",
                        mother, column(row, MOTHER_PREGNANCY_EPISODE_ID)));
            }
        }
        return result;
    }

    private JSONArray projectedVisits(SQLiteDatabase db, String client, String[] fields) throws Exception {
        JSONArray visits = new JSONArray();
        if (client == null) return visits;
        StringBuilder selected = new StringBuilder("v." + PROCESSED + " AS finalized");
        selected.append(", v.").append(VISIT_ID).append(" AS audit_visit, f.")
                .append(BASE_ENTITY_ID).append(" AS audit_submission, f.")
                .append(MOTHER_BASE_ENTITY_ID).append(", f.").append(MOTHER_PREGNANCY_EPISODE_ID)
                .append(", f.").append(PARTNER_MOTHER_PREGNANT);
        for (String field : fields) selected.append(", f.").append(field);
        try (Cursor rows = db.rawQuery("SELECT " + selected + " FROM "
                + Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT + " f LEFT JOIN visits v ON v."
                + FORM_SUBMISSION_ID + " = f." + BASE_ENTITY_ID + " AND v." + BASE_ENTITY_ID
                + " = f." + ENTITY_ID + " WHERE f." + ENTITY_ID + " = ? ORDER BY CAST(f."
                + LAST_INTERACTED_WITH + " AS INTEGER) DESC LIMIT 5", new String[] {client})) {
            while (rows.moveToNext()) {
                JSONObject visit = new JSONObject();
                visit.put("finalized", rows.getInt(rows.getColumnIndexOrThrow("finalized")) == 1);
                for (String field : fields) {
                    int index = rows.getColumnIndex(field);
                    if (index >= 0) visit.put(field, rows.isNull(index) ? JSONObject.NULL : rows.getString(index));
                }
                visit.put("source_projection", projectionParity(db, client, column(rows, "audit_submission"),
                        column(rows, "audit_visit"), rows, fields));
                if (Arrays.asList(fields).contains(PARTNER_FOLLOW_UP_SCHEMA)) {
                    visit.put("mother_snapshot_present", !column(rows, MOTHER_BASE_ENTITY_ID).isEmpty());
                    visit.put("pregnancy_episode_snapshot_present", !column(rows, MOTHER_PREGNANCY_EPISODE_ID).isEmpty());
                    visit.put("maternal_pregnant_snapshot", column(rows, PARTNER_MOTHER_PREGNANT));
                }
                if (Arrays.asList(fields).contains(CHILD_EID_FINAL_HIV_TEST_DONE)) {
                    visit.put("final_test_date_captured", hasRow(db, "SELECT 1 FROM visit_details WHERE "
                            + VISIT_ID + " = ? AND " + VISIT_KEY + " = ? AND trim(coalesce(" + DETAILS
                            + ", '')) <> ''", column(rows, "audit_visit"), CHILD_EID_FINAL_HIV_TEST_DATE));
                }
                visits.put(visit);
            }
        }
        return visits;
    }

    private JSONObject projectionParity(SQLiteDatabase db, String client, String submission, String visit,
            Cursor projection, String[] fields) throws Exception {
        JSONObject receipt = new JSONObject().put("source_event_present", false);
        try (Cursor row = db.rawQuery("SELECT json FROM event WHERE baseEntityId = ? AND formSubmissionId = ? "
                + "ORDER BY updatedAt DESC LIMIT 1", new String[] {client, submission})) {
            if (!row.moveToFirst()) return receipt;
            JSONObject source = new JSONObject(row.getString(0));
            JSONArray observations = source.optJSONArray("obs");
            receipt.put("source_event_present", true);
            int captured = 0;
            int projected = 0;
            int detailed = 0;
            for (String field : fields) {
                String answer = null;
                for (int i = 0; observations != null && i < observations.length(); i++) {
                    JSONObject observation = observations.optJSONObject(i);
                    if (observation == null || !field.equals(observation.optString("fieldCode"))) continue;
                    JSONArray values = observation.optJSONArray("values");
                    if (values != null && values.length() > 0 && !values.isNull(0)) {
                        answer = values.length() == 1 ? values.optString(0) : values.toString();
                    }
                    break;
                }
                if (answer == null || answer.isEmpty()) continue;
                captured++;
                if (normalizedAnswer(answer).equals(normalizedAnswer(column(projection, field)))) projected++;
                try (Cursor detail = db.rawQuery("SELECT MAX(" + DETAILS + ") FROM visit_details WHERE "
                        + VISIT_ID + " = ? AND " + VISIT_KEY + " = ?", new String[] {visit, field})) {
                    if (detail.moveToFirst() && !detail.isNull(0)
                            && normalizedAnswer(answer).equals(normalizedAnswer(detail.getString(0)))) detailed++;
                }
            }
            receipt.put("captured_workflow_fields", captured);
            receipt.put("ec_fields_matching_source", projected);
            receipt.put("visit_details_matching_source", detailed);
            receipt.put("all_captured_fields_projected", captured > 0 && captured == projected);
            receipt.put("all_captured_fields_in_visit_details", captured > 0 && captured == detailed);
        }
        return receipt;
    }

    private boolean hasRow(SQLiteDatabase db, String sql, String... arguments) {
        try (Cursor row = db.rawQuery(sql, arguments)) {
            return row.moveToFirst();
        }
    }

    private String column(Cursor row, String field) {
        int index = row.getColumnIndex(field);
        return index < 0 || row.isNull(index) ? "" : row.getString(index).trim();
    }

    private String normalizedAnswer(String answer) {
        String[] tokens = answer.replace("[", "").replace("]", "").replace("\"", "").replace(" ", "").split(",");
        Arrays.sort(tokens);
        return Arrays.toString(tokens);
    }

    private void projectVisit(SQLiteDatabase db, CoreClientProcessor processor, Table mapping,
            String partner, String mother, String linkage, String pregnancy, String serviceDate,
            boolean processed, String hivResult, String topics) throws Exception {
        String id = UUID.randomUUID().toString();
        Map<String, String> values = map(FOLLOW_UP_CATEGORY, "PARTNER", PARTNER_FOLLOW_UP_SCHEMA, "2",
                PARTNER_LINKAGE_ID, linkage, MOTHER_BASE_ENTITY_ID, mother,
                MOTHER_PREGNANCY_EPISODE_ID, pregnancy, PARTNER_MOTHER_PREGNANT, "yes",
                MOTHERMENTOR_VISIT_DATE, serviceDate, PARTNER_FOLLOW_UP_PHASE, "attendance",
                PARTNER_ARRIVED_AT_FACILITY, "yes", PARTNER_FACILITY_ARRIVAL_DATE, serviceDate,
                PARTNER_FOLLOW_UP_END, "completed", PARTNER_HIV_TESTED, "yes", PARTNER_HIV_RESULT, hivResult,
                PARTNER_SYPHILIS_TESTED, "yes", PARTNER_SYPHILIS_RESULT, "negative",
                PARTNER_HEPATITIS_B_TESTED, "yes", PARTNER_HEPATITIS_B_RESULT, "negative",
                PARTNER_EDUCATION_PROVIDED, "yes", PARTNER_EDUCATION_TOPICS, topics);
        JSONArray observations = new JSONArray();
        for (Map.Entry<String, String> value : values.entrySet()) {
            JSONArray answers = PARTNER_EDUCATION_TOPICS.equals(value.getKey())
                    ? new JSONArray(value.getValue().split(",")) : new JSONArray().put(value.getValue());
            observations.put(new JSONObject().put("fieldCode", value.getKey())
                    .put("formSubmissionField", value.getKey()).put("values", answers)
                    .put("saveObsAsArray", PARTNER_EDUCATION_TOPICS.equals(value.getKey())));
        }
        JSONObject source = new JSONObject().put("baseEntityId", partner).put("formSubmissionId", id)
                .put("eventType", MotherMentorFollowUpType.PARTNER.getEventType())
                .put("eventDate", serviceDate + "T12:00:00.000+03:00").put("obs", observations);
        Event event = JsonFormUtils.gson.fromJson(source.toString(), Event.class);
        Client client = JsonFormUtils.gson.fromJson(new JSONObject().put("baseEntityId", partner).toString(), Client.class);
        ContentValues projected = new ContentValues();
        for (Column column : mapping.columns) processor.processCaseModel(event, client, column, projected);
        assertEquals("Core must key EC visits by form submission", id, projected.getAsString(BASE_ENTITY_ID));
        assertEquals(partner, projected.getAsString(ENTITY_ID));
        assertEquals("2", projected.getAsString(PARTNER_FOLLOW_UP_SCHEMA));
        assertEquals(pregnancy, projected.getAsString(MOTHER_PREGNANCY_EPISODE_ID));
        db.insertOrThrow(Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT, null, projected);
        insert(db, "visits", map(VISIT_ID, id, BASE_ENTITY_ID, partner, FORM_SUBMISSION_ID, id,
                VISIT_TYPE, MotherMentorFollowUpType.PARTNER.getEventType(), PROCESSED, processed ? "1" : "0",
                VISIT_DATE, Long.toString(date(serviceDate).getTime()), VISIT_JSON, source.toString()));
        for (Map.Entry<String, String> value : values.entrySet()) {
            insert(db, "visit_details", map(VISIT_DETAILS_ID, UUID.randomUUID().toString(), VISIT_ID, id,
                    VISIT_KEY, value.getKey(), DETAILS, value.getValue(), PROCESSED, processed ? "1" : "0"));
        }
    }

    private boolean active(SQLiteDatabase db, String partner, String today) throws Exception {
        try (Cursor row = db.rawQuery("SELECT 1 FROM " + Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER
                + " p WHERE p." + BASE_ENTITY_ID + " = ? AND "
                + MotherMentorPartnerFollowUpDao.activeEnrollmentCondition("p", date(today)), new String[] {partner})) {
            return row.moveToFirst();
        }
    }

    private void assertDelta(JSONObject before, JSONObject after, String key, int delta) throws Exception {
        assertEquals(key, before.getInt(key) + delta, after.getInt(key));
    }

    private void insert(SQLiteDatabase db, String table, Map<String, String> values) {
        ContentValues content = new ContentValues();
        for (Map.Entry<String, String> value : values.entrySet()) content.put(value.getKey(), value.getValue());
        db.insertOrThrow(table, null, content);
    }

    private Map<String, String> map(String... values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put(values[i], values[i + 1]);
        return result;
    }

    private Date date(String date) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date);
    }
}
