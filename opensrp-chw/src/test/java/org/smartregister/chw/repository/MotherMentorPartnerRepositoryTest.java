package org.smartregister.chw.repository;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.mockito.Mockito;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        MotherMentorPartnerRepository.ensureColumns(cipher);
        repository = new MotherMentorPartnerRepository(cipher);
        db.execSQL("INSERT INTO ec_family VALUES ('family', 0), ('other-family', 0), ('closed-family', 1)");
        add("mother", "family", 0); add("household", "family", 0); add("other", "other-family", 0);
        add("legacy", "family", 0); add("linked", "family", 0); add("closed", "family", 1); add("closed-household", "closed-family", 0);
        db.execSQL("INSERT INTO ec_mothermentor_enrollment VALUES ('mother', '12-34-5678-123456', 0)");
        db.execSQL("INSERT INTO ec_mothermentor_enroll_partner (base_entity_id, relational_id, is_closed, mother_base_entity_id) VALUES ('legacy', 'family', 0, NULL), ('linked', 'family', 0, 'mother')");
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
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(ec_mothermentor_enroll_partner)", null)) { assertEquals(8, cursor.getCount()); }
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
