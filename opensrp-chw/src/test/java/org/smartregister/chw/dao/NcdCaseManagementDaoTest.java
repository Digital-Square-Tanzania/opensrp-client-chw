package org.smartregister.chw.dao;

import net.zetetic.database.MatrixCursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.smartregister.dao.AbstractDao;
import org.smartregister.repository.Repository;

public class NcdCaseManagementDaoTest extends AbstractDao {

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
    public void hasDeathRegisterRecordReturnsTrueWhenRowExists() {
        Mockito.doReturn(countCursor(1)).when(database).rawQuery(Mockito.any(), Mockito.any());

        Assert.assertTrue(NcdCaseManagementDao.hasDeathRegisterRecord("base-entity-id"));
    }

    @Test
    public void hasDeathRegisterRecordReturnsFalseWhenRowDoesNotExist() {
        Mockito.doReturn(countCursor(0)).when(database).rawQuery(Mockito.any(), Mockito.any());

        Assert.assertFalse(NcdCaseManagementDao.hasDeathRegisterRecord("base-entity-id"));
    }

    @Test
    public void hasRemoveMemberEventReturnsTrueWhenEventExists() {
        Mockito.doReturn(countCursor(1)).when(database).rawQuery(Mockito.any(), Mockito.any());

        Assert.assertTrue(NcdCaseManagementDao.hasRemoveMemberEvent("base-entity-id"));
    }

    @Test
    public void hasMortalityRecordShortCircuitsWhenRemoveMemberEventExists() {
        Mockito.doReturn(countCursor(1)).when(database).rawQuery(Mockito.any(), Mockito.any());

        Assert.assertTrue(NcdCaseManagementDao.hasMortalityRecord("base-entity-id"));
        Mockito.verify(database, Mockito.times(1)).rawQuery(Mockito.any(), Mockito.any());
    }

    @Test
    public void isNcdCaseClosedReturnsTrueWhenCaseIsClosed() {
        Mockito.doReturn(countCursor(1)).when(database).rawQuery(Mockito.any(), Mockito.any());

        Assert.assertTrue(NcdCaseManagementDao.isNcdCaseClosed("base-entity-id"));
    }

    @Test
    public void isNcdCaseClosedReturnsFalseWhenCaseIsOpen() {
        Mockito.doReturn(countCursor(0)).when(database).rawQuery(Mockito.any(), Mockito.any());

        Assert.assertFalse(NcdCaseManagementDao.isNcdCaseClosed("base-entity-id"));
    }

    private MatrixCursor countCursor(int count) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"cnt"});
        cursor.addRow(new Object[]{count});
        return cursor;
    }
}
