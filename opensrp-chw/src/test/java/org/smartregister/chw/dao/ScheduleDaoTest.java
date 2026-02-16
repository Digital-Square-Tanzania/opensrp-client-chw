package org.smartregister.chw.dao;

import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.smartregister.repository.Repository;

import java.util.List;

import static org.smartregister.chw.util.TestCursorUtils.mockCursor;

public class ScheduleDaoTest extends ScheduleDao {

    @Mock
    private Repository repository;

    @Mock
    private SQLiteDatabase database;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        setRepository(repository);
    }
    @Test
    public void testGetActiveFamilies() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"base_entity_id"}, new Object[]{"12345"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        List<String> activeFamilies = ScheduleDao.getActiveFamilies("FAMILY HOME VISIT", "Home visit");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        assert activeFamilies != null;
        Assert.assertEquals(activeFamilies.size(), 1);
    }

    @Test
    public void testGetActiveANCWomen() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"base_entity_id"}, new Object[]{"12345"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        List<String> activeANCWomen = ScheduleDao.getActiveANCWomen("ANC HOME VISIT", "Home visit");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        assert activeANCWomen != null;
        Assert.assertEquals(activeANCWomen.size(), 1);
    }

    @Test
    public void testGetActivePNCWomen() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"base_entity_id"}, new Object[]{"12345"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        List<String> activePNCWomen = ScheduleDao.getActivePNCWomen("PNC HOME VISIT", "Home visit");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(activePNCWomen.size(), 1);
    }

    @Test
    public void testGetActiveFPWomen() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"base_entity_id"}, new Object[]{"12345"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        List<String> activeFPWomen = ScheduleDao.getActiveFPWomen("FP HOME VISIT", "Home visit");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(activeFPWomen.size(), 1);
    }

    @Test
    public void testGetActiveChildren() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"base_entity_id"}, new Object[]{"12345"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        List<String> activeChildren = ScheduleDao.getActiveChildren("CHILD HOME VISIT", "Home visit");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(activeChildren.size(), 1);
    }

    @Test
    public void testGetActiveChildrenUnder5AndGirlsAge9to11() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"base_entity_id"}, new Object[]{"12345"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        List<String> activeChildrenFiveAndGirlsNineToEleven = ScheduleDao.getActiveChildrenUnder5AndGirlsAge9to11("CHILD HOME VISIT", "Home visit");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(activeChildrenFiveAndGirlsNineToEleven.size(), 1);
    }
}
