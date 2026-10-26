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
public class MotherMentorChildEidRepositoryTest {
    private SQLiteDatabase db;
    private net.zetetic.database.sqlcipher.SQLiteDatabase cipher;
    private MotherMentorChildEidRepository repository;
    @Before public void setup() {
        db = SQLiteDatabase.create(null);
        db.execSQL("CREATE TABLE ec_family (base_entity_id TEXT, is_closed INTEGER)");
        db.execSQL("CREATE TABLE ec_family_member (base_entity_id TEXT, relational_id TEXT, first_name TEXT, last_name TEXT, is_closed INTEGER, gender TEXT, dob TEXT, dob_unknown TEXT)");
        db.execSQL("CREATE TABLE ec_mothermentor_enrollment (base_entity_id TEXT, ctc_number TEXT, is_closed INTEGER)");
        db.execSQL("CREATE TABLE ec_mothermentor_enroll_child_eid (base_entity_id TEXT, relational_id TEXT, is_closed INTEGER)");
        cipher = Mockito.mock(net.zetetic.database.sqlcipher.SQLiteDatabase.class);
        Mockito.when(cipher.rawQuery(anyString(), org.mockito.ArgumentMatchers.<String[]>any())).thenAnswer(call -> db.rawQuery(call.getArgument(0), call.getArgument(1)));
        Mockito.doAnswer(call -> { db.execSQL(call.getArgument(0)); return null; }).when(cipher).execSQL(anyString());
        MotherMentorChildEidRepository.ensureColumns(cipher);
        repository = new MotherMentorChildEidRepository(cipher);
        db.execSQL("INSERT INTO ec_family VALUES ('family', 0), ('other-family', 0), ('closed-family', 1)");
        add("mother", "family", 0); add("household", "family", 0); add("other", "other-family", 0);
        add("legacy", "family", 0); add("linked", "family", 0); add("closed", "family", 1); add("closed-household", "closed-family", 0);
        db.execSQL("UPDATE ec_family_member SET dob='1990-01-01' WHERE base_entity_id='mother'");
        db.execSQL("INSERT INTO ec_mothermentor_enrollment VALUES ('mother', '12-34-5678-123456', 0)");
        db.execSQL("INSERT INTO ec_mothermentor_enroll_child_eid (base_entity_id, relational_id, is_closed, mother_base_entity_id) VALUES ('legacy', 'family', 0, NULL), ('linked', 'family', 0, 'mother')");
    }
    @After public void cleanup() { db.close(); }
    @Test public void candidatesExcludeSelfClosedAndLinkedButAllowLegacy() {
        assertEquals(Arrays.asList("household", "legacy"), ids(repository.candidates("mother", "family", true)));
        assertEquals(Arrays.asList("household", "legacy", "other"), ids(repository.candidates("mother", "family", false)));
    }
    @Test public void candidatesAllowBothSexesButExcludeEighteenMonthsFutureUnknownAndApproximateDob() {
        add("female", "family", 0); add("too-old", "family", 0); add("future", "family", 0);
        add("unknown", "family", 0); add("invalid", "family", 0); add("approximate", "family", 0);
        db.execSQL("UPDATE ec_family_member SET gender='Female' WHERE base_entity_id='female'");
        db.execSQL("UPDATE ec_family_member SET dob=? WHERE base_entity_id='too-old'", new Object[]{org.joda.time.LocalDate.now().minusMonths(18).toString()});
        db.execSQL("UPDATE ec_family_member SET dob=? WHERE base_entity_id='future'", new Object[]{org.joda.time.LocalDate.now().plusDays(1).toString()});
        db.execSQL("UPDATE ec_family_member SET dob=NULL WHERE base_entity_id='unknown'");
        db.execSQL("UPDATE ec_family_member SET dob='2026-02-31' WHERE base_entity_id='invalid'");
        db.execSQL("UPDATE ec_family_member SET dob_unknown='true' WHERE base_entity_id='approximate'");
        assertEquals(Arrays.asList("female", "household", "legacy"), ids(repository.candidates("mother", "family", true)));
    }
    @Test public void otherClientsRetainTheirOwnHousehold() {
        assertEquals("other-family", repository.findClient("other").familyId());
        assertNull(repository.findClient("closed"));
        assertNull(repository.findClient("closed-household"));
    }
    @Test public void onlyActivePrimaryMothersCanLink() {
        assertNotNull(repository.findMother("mother"));
        assertEquals("12-34-5678-123456", repository.findMother("mother").values.get("ctc_number"));
        assertNull(repository.findMother("household"));
        db.execSQL("UPDATE ec_mothermentor_enrollment SET is_closed = 1");
        assertNull(repository.findMother("mother"));
    }
    @Test public void additiveMigrationIsRepeatableAndPreservesLegacyData() {
        MotherMentorChildEidRepository.ensureColumns(cipher);
        MotherMentorChildEidRepository.ensureColumns(cipher);
        assertEquals("family", repository.existingEnrollment("legacy").get("relational_id"));
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(ec_mothermentor_enroll_child_eid)", null)) { assertEquals(7, cursor.getCount()); }
    }
    @Test public void legacyEnrollmentCannotBypassEligibilityOrOverwriteExistingLink() {
        assertTrue(repository.canEnrollChild("other"));
        assertFalse(repository.canEnrollChild("linked"));
        assertFalse(repository.canEnrollChild("legacy"));
        db.execSQL("UPDATE ec_family_member SET gender='Female' WHERE base_entity_id='other'");
        assertTrue(repository.canEnrollChild("other"));
        db.execSQL("UPDATE ec_family_member SET dob=? WHERE base_entity_id='other'",
                new Object[]{org.joda.time.LocalDate.now().minusMonths(18).toString()});
        assertFalse(repository.canEnrollChild("other"));
    }
    @Test(expected = IllegalArgumentException.class) public void savingRechecksAgeAfterSelection() throws Exception {
        db.execSQL("UPDATE ec_family_member SET dob=? WHERE base_entity_id='household'",
                new Object[]{org.joda.time.LocalDate.now().minusMonths(18).toString()});
        repository.save(null, "mother", "household", null);
    }
    @Test public void newLinkImmediatelyRemovesCandidate() {
        db.execSQL("UPDATE ec_mothermentor_enroll_child_eid SET mother_base_entity_id = 'mother' WHERE base_entity_id = 'legacy'");
        assertEquals(Arrays.asList("household"), ids(repository.candidates("mother", "family", true)));
    }
    private void add(String id, String family, int closed) { db.execSQL("INSERT INTO ec_family_member VALUES (?, ?, ?, 'Test', ?, 'Male', ?, 'false')", new Object[]{id, family, id, closed, org.joda.time.LocalDate.now().minusMonths(6).toString()}); }
    private List<String> ids(List<MotherMentorChildEidRepository.Person> people) {
        List<String> ids = new ArrayList<>(); for (MotherMentorChildEidRepository.Person person : people) ids.add(person.id()); return ids;
    }
}
