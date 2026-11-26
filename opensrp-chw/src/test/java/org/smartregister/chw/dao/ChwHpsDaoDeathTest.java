package org.smartregister.chw.dao;

import net.zetetic.database.MatrixCursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.smartregister.dao.AbstractDao;
import org.smartregister.repository.Repository;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ChwHpsDaoDeathTest extends AbstractDao {

    @Mock
    private Repository repository;

    @Mock
    private SQLiteDatabase database;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        setRepository(repository);
        Mockito.doReturn(database).when(repository).getReadableDatabase();
    }

    @Test
    public void completeRecordCheckRequiresBothDatesAndPreservesClosedRecords() {
        MatrixCursor cursor = new MatrixCursor(new String[]{"base_entity_id"});
        cursor.addRow(new Object[]{"base'entity-id"});
        Mockito.doReturn(cursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        assertTrue(ChwHpsDao.hasCompleteDeathRegisterRecord("base'entity-id"));

        String sql = verifyQueryUsesArgs("base'entity-id");
        assertTrue(sql.contains("FROM ec_hps_death_register"));
        assertTrue(sql.contains("TRIM(IFNULL(dod, '')) <> ''"));
        assertTrue(sql.contains("TRIM(IFNULL(dob, '')) <> ''"));
        assertFalse(sql.contains("is_closed"));
        assertFalse(sql.contains("FROM event"));
    }

    @Test
    public void completeRecordCheckReturnsFalseWhenNoCompleteRecordExists() {
        Mockito.doReturn(new MatrixCursor(new String[]{"base_entity_id"}))
                .when(database).rawQuery(Mockito.any(), Mockito.any());

        assertFalse(ChwHpsDao.hasCompleteDeathRegisterRecord("base-entity-id"));
    }

    @Test
    public void recordedDatePrefersDeathRegisterAndDoesNotReadFamilyMember() {
        Mockito.doReturn(dateCursor("10-06-2026"))
                .when(database).rawQuery(Mockito.any(), Mockito.any());

        assertEquals("10-06-2026", ChwHpsDao.getRecordedDateOfDeath("base'entity-id"));

        String sql = verifyQueryUsesArgs("base'entity-id");
        assertTrue(sql.contains("FROM ec_hps_death_register"));
        assertFalse(sql.contains("is_closed"));
        Mockito.verify(database, Mockito.times(1)).rawQuery(Mockito.any(), Mockito.any());
    }

    @Test
    public void recordedDateFallsBackToFamilyMemberWhenDeathRegisterHasNoDate() {
        Mockito.doReturn(new MatrixCursor(new String[]{"dod"}), dateCursor("2026-06-10"))
                .when(database).rawQuery(Mockito.any(), Mockito.any());

        assertEquals("2026-06-10", ChwHpsDao.getRecordedDateOfDeath("base'entity-id"));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String[]> argsCaptor = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(database, Mockito.times(2)).rawQuery(sqlCaptor.capture(), argsCaptor.capture());
        assertTrue(sqlCaptor.getAllValues().get(0).contains("FROM ec_hps_death_register"));
        assertTrue(sqlCaptor.getAllValues().get(1).contains("FROM ec_family_member"));
        for (String[] args : argsCaptor.getAllValues()) {
            assertArrayEquals(new String[]{"base'entity-id"}, args);
        }
        for (String sql : sqlCaptor.getAllValues()) {
            assertTrue(sql.contains("base_entity_id = ?"));
            assertFalse(sql.contains("base'entity-id"));
        }
    }

    @Test
    public void recordedDateFallsBackWhenDeathRegisterDateIsBlank() {
        Mockito.doReturn(dateCursor("  "), dateCursor("2026-06-10"))
                .when(database).rawQuery(Mockito.any(), Mockito.any());

        assertEquals("2026-06-10", ChwHpsDao.getRecordedDateOfDeath("base-entity-id"));
    }

    @Test
    public void recordedDateReturnsNullWhenNeitherTableHasADate() {
        Mockito.doReturn(new MatrixCursor(new String[]{"dod"}), new MatrixCursor(new String[]{"dod"}))
                .when(database).rawQuery(Mockito.any(), Mockito.any());

        assertNull(ChwHpsDao.getRecordedDateOfDeath("base-entity-id"));
    }

    @Test
    public void blankIdentifiersDoNotQueryTheDatabase() {
        for (String baseEntityId : new String[]{null, "", "  "}) {
            assertFalse(ChwHpsDao.hasCompleteDeathRegisterRecord(baseEntityId));
            assertNull(ChwHpsDao.getRecordedDateOfDeath(baseEntityId));
        }

        Mockito.verify(database, Mockito.never()).rawQuery(Mockito.any(), Mockito.any());
    }

    private MatrixCursor dateCursor(String dateOfDeath) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"dod"});
        cursor.addRow(new Object[]{dateOfDeath});
        return cursor;
    }

    private String verifyQueryUsesArgs(String baseEntityId) {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String[]> argsCaptor = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(database).rawQuery(sqlCaptor.capture(), argsCaptor.capture());
        assertTrue(sqlCaptor.getValue().contains("base_entity_id = ?"));
        assertFalse(sqlCaptor.getValue().contains(baseEntityId));
        assertArrayEquals(new String[]{baseEntityId}, argsCaptor.getValue());
        return sqlCaptor.getValue();
    }
}
