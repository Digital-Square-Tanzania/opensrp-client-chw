package org.smartregister.chw.dao;

import net.zetetic.database.MatrixCursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.smartregister.chw.core.utils.CoreConstants;
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
    public void hasOpenReferralUsesSelectionArgs() {
        Mockito.doReturn(countCursor(1)).when(database).rawQuery(Mockito.any(), Mockito.any());

        Assert.assertTrue(NcdCaseManagementDao.hasOpenReferral("base'entity-id"));
        verifyRawQueryUsesArgs("base'entity-id");
    }

    @Test
    public void hasDeathRegisterRecordReturnsTrueWhenRowExists() {
        Mockito.doReturn(countCursor(1)).when(database).rawQuery(Mockito.any(), Mockito.any());

        Assert.assertTrue(NcdCaseManagementDao.hasDeathRegisterRecord("base'entity-id"));
        verifyRawQueryUsesArgs("base'entity-id");
    }

    @Test
    public void hasDeathRegisterRecordReturnsFalseWhenRowDoesNotExist() {
        Mockito.doReturn(countCursor(0)).when(database).rawQuery(Mockito.any(), Mockito.any());

        Assert.assertFalse(NcdCaseManagementDao.hasDeathRegisterRecord("base-entity-id"));
    }

    @Test
    public void hasRemoveMemberEventReturnsTrueWhenEventExists() {
        Mockito.doReturn(countCursor(1)).when(database).rawQuery(Mockito.any(), Mockito.any());

        Assert.assertTrue(NcdCaseManagementDao.hasRemoveMemberEvent("base'entity-id"));
        verifyRawQueryUsesArgs("base'entity-id", CoreConstants.EventType.REMOVE_MEMBER);
    }

    @Test
    public void hasMortalityRecordShortCircuitsWhenRemoveMemberEventExists() {
        Mockito.doReturn(countCursor(1)).when(database).rawQuery(Mockito.any(), Mockito.any());

        Assert.assertTrue(NcdCaseManagementDao.hasMortalityRecord("base'entity-id"));
        Mockito.verify(database, Mockito.times(1)).rawQuery(Mockito.any(), Mockito.any());
        verifyRawQueryUsesArgs("base'entity-id", CoreConstants.EventType.REMOVE_MEMBER);
    }

    @Test
    public void isNcdCaseClosedReturnsTrueWhenCaseIsClosed() {
        Mockito.doReturn(countCursor(1)).when(database).rawQuery(Mockito.any(), Mockito.any());

        Assert.assertTrue(NcdCaseManagementDao.isNcdCaseClosed("base'entity-id"));
        verifyRawQueryUsesArgs("base'entity-id");
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

    private void verifyRawQueryUsesArgs(String... expectedArgs) {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String[]> argsCaptor = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(database, Mockito.atLeastOnce()).rawQuery(sqlCaptor.capture(), argsCaptor.capture());

        String sql = sqlCaptor.getAllValues().get(sqlCaptor.getAllValues().size() - 1);
        Assert.assertTrue(sql.contains("?"));
        Assert.assertFalse(sql.contains("base'entity-id"));
        Assert.assertArrayEquals(expectedArgs,
                argsCaptor.getAllValues().get(argsCaptor.getAllValues().size() - 1));
    }
}
