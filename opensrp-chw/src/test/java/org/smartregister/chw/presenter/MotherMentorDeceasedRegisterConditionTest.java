package org.smartregister.chw.presenter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.mothermentor.contract.MotherMentorRegisterFragmentContract;
import org.smartregister.chw.mothermentor.presenter.BaseMotherMentorRegisterFragmentPresenter;
import org.smartregister.chw.mothermentor.util.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorDeceasedRegisterConditionTest {
    private SQLiteDatabase database;

    @Before
    public void setUp() {
        database = SQLiteDatabase.create(null);
        database.execSQL("CREATE TABLE ec_family_member (base_entity_id TEXT, dod TEXT)");
        database.execSQL("INSERT INTO ec_family_member VALUES ('living', NULL),"
                + " ('deceased', '2026-10-20'), ('closed', NULL),"
                + " ('empty-date', ''), ('legacy-null-date', 'null')");
        for (String table : new String[] {
                Constants.TABLES.MOTHERMENTOR_ENROLLMENT,
                Constants.TABLES.MOTHERMENTOR_ENROLL_IIT,
                Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER,
                Constants.TABLES.MOTHERMENTOR_ENROLL_CHILD_EID
        }) {
            database.execSQL("CREATE TABLE " + table
                    + " (base_entity_id TEXT, is_closed INTEGER, status TEXT, screening_status TEXT)");
            database.execSQL("INSERT INTO " + table + " VALUES"
                    + " ('living', 0, 'client', NULL), ('deceased', 0, 'client', NULL),"
                    + " ('closed', 1, 'client', NULL), ('empty-date', 0, 'client', NULL),"
                    + " ('legacy-null-date', 0, 'client', NULL)");
        }
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void primaryRegisterExcludesRecordedDeathsEvenWhenEnrollmentRemainsOpen() {
        assertLivingClients(primaryPresenter());
    }

    @Test
    public void iitRegisterExcludesRecordedDeathsEvenWhenEnrollmentRemainsOpen() {
        assertLivingClients(secondaryPresenter(Constants.TABLES.MOTHERMENTOR_ENROLL_IIT));
    }

    @Test
    public void partnerRegisterExcludesRecordedDeathsEvenWhenEnrollmentRemainsOpen() {
        assertLivingClients(secondaryPresenter(Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER));
    }

    @Test
    public void childEidRegisterExcludesRecordedDeathsEvenWhenEnrollmentRemainsOpen() {
        assertLivingClients(secondaryPresenter(Constants.TABLES.MOTHERMENTOR_ENROLL_CHILD_EID));
    }

    @Test
    public void primaryRegisterStillExcludesContactsAndIneligibleScreeningRecords() {
        database.execSQL("INSERT INTO ec_family_member VALUES ('contact', NULL), ('ineligible', NULL)");
        database.execSQL("INSERT INTO " + Constants.TABLES.MOTHERMENTOR_ENROLLMENT
                + " VALUES ('contact', 0, 'contact', NULL), ('ineligible', 0, 'client', '-')");

        assertLivingClients(primaryPresenter());
    }

    private void assertLivingClients(BaseMotherMentorRegisterFragmentPresenter presenter) {
        String table = presenter.getMainTable();
        String condition = presenter.getMainCondition();
        List<String> clients = new ArrayList<>();
        try (Cursor cursor = database.rawQuery(
                "SELECT base_entity_id FROM " + table + " WHERE " + condition
                        + " ORDER BY base_entity_id", null)) {
            while (cursor.moveToNext()) clients.add(cursor.getString(0));
        }
        assertEquals(Arrays.asList("empty-date", "legacy-null-date", "living"), clients);
        try (Cursor cursor = database.rawQuery(
                "SELECT count(*) FROM " + table + " WHERE " + condition, null)) {
            cursor.moveToFirst();
            assertEquals(3, cursor.getInt(0));
        }
    }

    private MotherMentorRegisterFragmentPresenter primaryPresenter() {
        return new MotherMentorRegisterFragmentPresenter(
                mock(MotherMentorRegisterFragmentContract.View.class),
                mock(MotherMentorRegisterFragmentContract.Model.class), null);
    }

    private MotherMentorSecondaryEnrollmentRegisterFragmentPresenter secondaryPresenter(String table) {
        return new MotherMentorSecondaryEnrollmentRegisterFragmentPresenter(
                mock(MotherMentorRegisterFragmentContract.View.class),
                mock(MotherMentorRegisterFragmentContract.Model.class), null, table);
    }
}
