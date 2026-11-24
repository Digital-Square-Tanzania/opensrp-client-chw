package org.smartregister.chw.presenter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.model.FamilyProfileMemberModel;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class FamilyProfileMemberRemovalTest {
    @Test
    public void membersAndCountExcludeBothRecordedAndProfileDeaths() {
        FamilyProfileMemberModel model = new FamilyProfileMemberModel();
        FamilyProfileMemberPresenter presenter =
                new FamilyProfileMemberPresenter(null, model, null, "household", "mother", null);
        String condition = presenter.getMainCondition();
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.execSQL("CREATE TABLE ec_family (base_entity_id TEXT, family_head TEXT)");
            db.execSQL("CREATE TABLE ec_family_member (base_entity_id TEXT, relational_id TEXT,"
                    + " date_removed TEXT, dod TEXT)");
            db.execSQL("CREATE TABLE ec_child (base_entity_id TEXT, date_removed TEXT, dod TEXT)");
            db.execSQL("INSERT INTO ec_family VALUES ('household', 'mother')");
            db.execSQL("INSERT INTO ec_family_member VALUES"
                    + " ('mother', 'previous-household', NULL, NULL),"
                    + " ('living-child', 'household', NULL, NULL),"
                    + " ('eid-death', 'household', '2026-11-25', '2026-11-24'),"
                    + " ('profile-death', 'household', '2026-11-25', NULL),"
                    + " ('child-record-removed', 'household', NULL, NULL),"
                    + " ('other-household-child', 'other-household', NULL, NULL)");
            db.execSQL("INSERT INTO ec_child VALUES"
                    + " ('living-child', NULL, NULL),"
                    + " ('eid-death', '2026-11-25', '2026-11-24'),"
                    + " ('profile-death', '2026-11-25', NULL),"
                    + " ('child-record-removed', '2026-11-25', '2026-11-24')");
            try (Cursor rows = db.rawQuery("SELECT ec_family_member.base_entity_id"
                    + " FROM ec_family_member LEFT JOIN ec_child USING (base_entity_id)"
                    + " WHERE " + condition + " ORDER BY ec_family_member.base_entity_id", null)) {
                assertEquals(2, rows.getCount());
                assertTrue(rows.moveToNext());
                assertEquals("living-child", rows.getString(0));
                assertTrue(rows.moveToNext());
                assertEquals("mother", rows.getString(0));
            }
            try (Cursor count = db.rawQuery(new SmartRegisterQueryBuilder().Endquery(
                    model.countSelect("ec_family_member", condition)), null)) {
                assertTrue(count.moveToFirst());
                assertEquals(2, count.getInt(0));
            }
        }
    }
}
