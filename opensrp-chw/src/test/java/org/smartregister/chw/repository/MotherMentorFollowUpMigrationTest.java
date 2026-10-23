package org.smartregister.chw.repository;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorFollowUpMigrationTest {
    @Test
    public void version51UpgradeAddsChildAnswersWithoutLosingExistingFollowUps() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.execSQL("CREATE TABLE ec_mothermentor_followup_visit (base_entity_id TEXT, entity_id TEXT, follow_up_status TEXT, child_eid_test_type TEXT)");
            db.execSQL("INSERT INTO ec_mothermentor_followup_visit VALUES ('visit', 'client', 'found', 'nine_months')");
            net.zetetic.database.sqlcipher.SQLiteDatabase cipher = Mockito.mock(net.zetetic.database.sqlcipher.SQLiteDatabase.class);
            Mockito.when(cipher.rawQuery(anyString(), Mockito.<String[]>any()))
                    .thenAnswer(call -> db.rawQuery(call.getArgument(0), call.getArgument(1)));
            Mockito.doAnswer(call -> { db.execSQL(call.getArgument(0)); return null; }).when(cipher).execSQL(anyString());

            ChwRepositoryFlv.onUpgrade(null, cipher, 51, 52);
            ChwRepositoryFlv.onUpgrade(null, cipher, 51, 52);
            db.execSQL("UPDATE ec_mothermentor_followup_visit SET child_eid_non_attendance_reason = 'forgot', "
                    + "child_eid_non_attendance_reason_other = NULL, child_eid_expected_facility_visit_date = '2026-10-17', "
                    + "child_eid_arrived_at_facility = 'yes', child_eid_facility_arrival_date = '2026-10-17'");
            try (Cursor c = db.rawQuery("SELECT * FROM ec_mothermentor_followup_visit", null)) {
                assertEquals(1, c.getCount()); assertTrue(c.moveToFirst());
                assertEquals("client", c.getString(c.getColumnIndexOrThrow("entity_id")));
                assertEquals("found", c.getString(c.getColumnIndexOrThrow("follow_up_status")));
                assertEquals("nine_months", c.getString(c.getColumnIndexOrThrow("child_eid_test_type")));
                assertEquals("yes", c.getString(c.getColumnIndexOrThrow("child_eid_arrived_at_facility")));
            }
        }
    }
}
