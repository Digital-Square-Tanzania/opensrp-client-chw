package org.smartregister.chw.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorPartnerWorkflowMigrationTest {
    private SQLiteDatabase database;
    private net.zetetic.database.sqlcipher.SQLiteDatabase cipher;

    @Before
    public void setUp() {
        database = SQLiteDatabase.create(null);
        database.execSQL("CREATE TABLE " + Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT
                + " (base_entity_id TEXT, entity_id TEXT, follow_up_status TEXT, follow_up_category TEXT)");
        database.execSQL("CREATE TABLE " + Constants.TABLES.MOTHERMENTOR_ENROLLMENT
                + " (base_entity_id TEXT, art_status TEXT)");
        database.execSQL("CREATE TABLE " + Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER
                + " (base_entity_id TEXT, mother_base_entity_id TEXT)");
        database.execSQL("CREATE TABLE event (json TEXT, baseEntityId TEXT, formSubmissionId TEXT, "
                + "eventType TEXT, eventDate INTEGER, updatedAt INTEGER)");
        cipher = Mockito.mock(net.zetetic.database.sqlcipher.SQLiteDatabase.class);
        Mockito.when(cipher.rawQuery(anyString(), Mockito.<String[]>any()))
                .thenAnswer(call -> database.rawQuery(call.getArgument(0), call.getArgument(1)));
        Mockito.doAnswer(call -> {
            database.execSQL(call.getArgument(0));
            return null;
        }).when(cipher).execSQL(anyString());
        Mockito.doAnswer(call -> {
            database.execSQL(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(cipher).execSQL(anyString(), Mockito.<Object[]>any());
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void version57UpgradeIsIdempotentAndDoesNotInventLegacyAnswers() {
        database.execSQL("INSERT INTO " + Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT
                + " (base_entity_id, entity_id, follow_up_status) VALUES ('visit', 'partner', 'found')");
        database.execSQL("INSERT INTO " + Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER
                + " VALUES ('partner', 'mother')");

        upgrade();
        upgrade();

        assertEquals("found", followUpValue("visit", DBConstants.KEY.FOLLOW_UP_STATUS));
        for (String column : MotherMentorPartnerWorkflowMigration.FOLLOW_UP_COLUMNS) {
            assertNull(column, followUpValue("visit", column));
        }
        assertEquals("legacy:partner", value(Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER,
                "partner", DBConstants.KEY.PARTNER_LINKAGE_ID));
        assertNull(value(Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER,
                "partner", DBConstants.KEY.MOTHER_PREGNANCY_EPISODE_ID));
        assertNull(value(Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER,
                "partner", DBConstants.KEY.PARTNER_MOTHER_PREGNANT));
        assertEquals(1, count(Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT));
    }

    @Test
    public void maternalEpisodeUsesLatestRetainedEnrollmentSubmissionWithoutReplacingKnownIds()
            throws Exception {
        database.execSQL("INSERT INTO " + Constants.TABLES.MOTHERMENTOR_ENROLLMENT
                + " VALUES ('mother', 'on_art'), ('known', 'on_art')");
        event("mother", "older", "Mother Mentor Enrollment", 10, new JSONObject());
        event("mother", "latest", "Mother Mentor Enrollment", 20, new JSONObject());
        event("mother", "voided", "Mother Mentor Enrollment", 30,
                new JSONObject().put("voided", true));
        eventJson("mother", "malformed", "Mother Mentor Enrollment", 40, "invalid json");
        event("mother", "", "Mother Mentor Enrollment", 50, new JSONObject());
        event("known", "later-import", "Mother Mentor Enrollment", 60, new JSONObject());
        upgrade();
        database.execSQL("UPDATE " + Constants.TABLES.MOTHERMENTOR_ENROLLMENT + " SET "
                + DBConstants.KEY.MOTHER_PREGNANCY_EPISODE_ID
                + " = 'existing-episode' WHERE base_entity_id = 'known'");

        upgrade();

        assertEquals("latest", value(Constants.TABLES.MOTHERMENTOR_ENROLLMENT,
                "mother", DBConstants.KEY.MOTHER_PREGNANCY_EPISODE_ID));
        assertEquals("existing-episode", value(Constants.TABLES.MOTHERMENTOR_ENROLLMENT,
                "known", DBConstants.KEY.MOTHER_PREGNANCY_EPISODE_ID));
        assertEquals("on_art", value(Constants.TABLES.MOTHERMENTOR_ENROLLMENT,
                "mother", DBConstants.KEY.ART_STATUS));
    }

    @Test
    public void onlyActualObservationsOnMatchingVisitAndClientAreBackfilled() throws Exception {
        database.execSQL("INSERT INTO " + Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT
                + " (base_entity_id, entity_id, follow_up_status) VALUES ('matched', 'partner', 'found'), ('other', 'partner', 'found')");
        JSONArray observations = new JSONArray()
                .put(observation(DBConstants.KEY.PARTNER_FOLLOW_UP_SCHEMA, "2"))
                .put(observation(DBConstants.KEY.MOTHER_PREGNANCY_EPISODE_ID, "episode"))
                .put(observation(DBConstants.KEY.PARTNER_HIV_RESULT, "positive"))
                .put(observation(DBConstants.KEY.PARTNER_EDUCATION_TOPICS, "hiv", "hepatitis_b"));
        event("partner", "matched", "MotherMentor Partner Follow-up Visit", 10,
                new JSONObject().put("obs", observations));
        event("wrong-client", "other", "MotherMentor Partner Follow-up Visit", 20,
                new JSONObject().put("obs", observations));
        event("partner", "wrong-visit", "MotherMentor Partner Follow-up Visit", 30,
                new JSONObject().put("obs", observations));

        upgrade();
        upgrade();

        assertEquals("2", followUpValue("matched", DBConstants.KEY.PARTNER_FOLLOW_UP_SCHEMA));
        assertEquals("episode", followUpValue("matched", DBConstants.KEY.MOTHER_PREGNANCY_EPISODE_ID));
        assertEquals("positive", followUpValue("matched", DBConstants.KEY.PARTNER_HIV_RESULT));
        assertEquals("[\"hiv\",\"hepatitis_b\"]",
                followUpValue("matched", DBConstants.KEY.PARTNER_EDUCATION_TOPICS));
        assertNull(followUpValue("matched", DBConstants.KEY.PARTNER_HIV_TESTED));
        assertNull(followUpValue("matched", DBConstants.KEY.PARTNER_EDUCATION_PROVIDED));
        assertNull(followUpValue("matched", DBConstants.KEY.PARTNER_SYPHILIS_RESULT));
        assertNull(followUpValue("other", DBConstants.KEY.PARTNER_FOLLOW_UP_SCHEMA));
    }

    @Test
    public void voidedMalformedAndAbsentObservationsCannotReplaceRecordedValues() throws Exception {
        database.execSQL("INSERT INTO " + Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT
                + " (base_entity_id, entity_id, follow_up_status) VALUES ('retained', 'partner', 'found'), ('voided', 'partner', 'found'),"
                + " ('malformed', 'partner', 'found')");
        upgrade();
        database.execSQL("UPDATE " + Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT + " SET "
                + DBConstants.KEY.PARTNER_HIV_RESULT + " = 'negative' WHERE base_entity_id = 'retained'");
        JSONArray observations = new JSONArray()
                .put(observation(DBConstants.KEY.PARTNER_HIV_RESULT, "positive"));
        event("partner", "retained", "MotherMentor Partner Follow-up Visit", 10,
                new JSONObject().put("obs", observations));
        event("partner", "voided", "MotherMentor Partner Follow-up Visit", 20,
                new JSONObject().put("voided", true).put("obs", observations));
        eventJson("partner", "malformed", "MotherMentor Partner Follow-up Visit", 30, "{");

        upgrade();

        assertEquals("negative", followUpValue("retained", DBConstants.KEY.PARTNER_HIV_RESULT));
        assertNull(followUpValue("voided", DBConstants.KEY.PARTNER_HIV_RESULT));
        assertNull(followUpValue("malformed", DBConstants.KEY.PARTNER_HIV_RESULT));
        assertNull(followUpValue("retained", DBConstants.KEY.CHILD_EID_FINAL_HIV_RESULT));
        assertEquals(3, count(Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT));
    }

    @Test
    public void version58UpgradeAddsTheBinaryReportIndexWithoutChangingRowsOrTheLegacyIndex() {
        database.execSQL("CREATE TABLE visit_details (visit_id TEXT, visit_key TEXT, details TEXT)");
        database.execSQL("CREATE INDEX legacy_nocase ON visit_details "
                + "(visit_id COLLATE NOCASE, visit_key COLLATE NOCASE)");
        database.execSQL("INSERT INTO visit_details VALUES "
                + "('visit', 'result', 'negative'), ('VISIT', 'result', 'positive')");

        ChwRepositoryFlv.onUpgrade(null, cipher, 58, 59);
        ChwRepositoryFlv.onUpgrade(null, cipher, 58, 59);

        try (Cursor cursor = database.rawQuery("EXPLAIN QUERY PLAN SELECT MAX(details) "
                + "FROM visit_details WHERE visit_id = 'visit' AND visit_key = 'result'", null)) {
            StringBuilder plan = new StringBuilder();
            while (cursor.moveToNext()) plan.append(cursor.getString(cursor.getColumnIndexOrThrow("detail")));
            assertTrue(plan.toString(), plan.toString().contains("mothermentor_report_visit_details"));
        }
        try (Cursor cursor = database.rawQuery("SELECT MAX(details) FROM visit_details "
                + "WHERE visit_id = 'visit' AND visit_key = 'result'", null)) {
            assertTrue(cursor.moveToFirst());
            assertEquals("negative", cursor.getString(0));
        }
        try (Cursor cursor = database.rawQuery("SELECT name FROM sqlite_master WHERE "
                + "type = 'index' AND tbl_name = 'visit_details'", null)) {
            assertEquals(2, cursor.getCount());
        }
        assertEquals(2, count("visit_details"));
    }

    private void upgrade() {
        ChwRepositoryFlv.onUpgrade(null, cipher, 57, 58);
    }

    private static JSONObject observation(String key, String... values) throws Exception {
        JSONArray answers = new JSONArray();
        for (String value : values) answers.put(value);
        return new JSONObject().put("fieldCode", key).put("values", answers);
    }

    private void event(String client, String submission, String type, long time, JSONObject json) {
        eventJson(client, submission, type, time, json.toString());
    }

    private void eventJson(String client, String submission, String type, long time, String json) {
        database.execSQL("INSERT INTO event VALUES (?, ?, ?, ?, ?, ?)",
                new Object[]{json, client, submission, type, time, time});
    }

    private String followUpValue(String visit, String column) {
        return value(Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT, visit, column);
    }

    private String value(String table, String id, String column) {
        try (Cursor cursor = database.rawQuery("SELECT " + column + " FROM " + table
                + " WHERE base_entity_id = ?", new String[]{id})) {
            assertTrue(cursor.moveToFirst());
            return cursor.isNull(0) ? null : cursor.getString(0);
        }
    }

    private int count(String table) {
        try (Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM " + table, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }
}
