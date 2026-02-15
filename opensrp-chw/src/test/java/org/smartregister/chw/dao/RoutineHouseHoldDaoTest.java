package org.smartregister.chw.dao;

import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.repository.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.smartregister.chw.util.TestCursorUtils.mockCursor;

public class RoutineHouseHoldDaoTest extends RoutineHouseHoldDao {
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
    public void testGetLastRoutineVisitDateReturnsZero() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"base_entity_id"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        long time = RoutineHouseHoldDao.getLastRoutineVisitDate("12345");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(time, 0);
    }

    @Test
    public void testGetEventDetails() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"count"}, new Object[]{2});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        long eventDate = new Date().getTime();
        String baseEntityID = "12345";
        String eventName = "routine-check";

        Map<String, List<VisitDetail>> eventDetails = RoutineHouseHoldDao.getEventDetails(eventDate, baseEntityID, eventName);

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(eventDetails.size(), 1);
    }
}
