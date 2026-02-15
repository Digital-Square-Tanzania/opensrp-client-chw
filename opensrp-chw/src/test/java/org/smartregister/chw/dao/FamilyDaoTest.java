package org.smartregister.chw.dao;

import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.smartregister.chw.model.FamilyDetailsModel;
import org.smartregister.domain.AlertStatus;
import org.smartregister.repository.Repository;

import java.util.Map;

import static org.smartregister.chw.util.TestCursorUtils.mockCursor;

public class FamilyDaoTest extends FamilyDao {
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
    public void testGetFamilyServiceScheduleReturnsVisits() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"visit_state", "totals"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        Map<String, Integer> visits = FamilyDao.getFamilyServiceSchedule("12345");


        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(visits.size(), 0);
    }

    @Test
    public void testGetFamilyServiceScheduleWithChildrenOnlyUnderTwoReturnsVisits() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"visit_state", "totals"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        Map<String, Integer> visits = FamilyDao.getFamilyServiceScheduleWithChildrenOnlyUnderTwo("12345");


        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(visits.size(), 0);
    }

    @Test
    public void testGetMemberDueStatus() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"visit_state"}, new Object[]{"DUE"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        String visitState = FamilyDao.getMemberDueStatus("12345");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(visitState, "DUE");
    }

    @Test
    public void testGetMemberDueStatusReturnsEmptyString() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"visit_state"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        String visitState = FamilyDao.getMemberDueStatus("12345");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        String empty = "";
        Assert.assertEquals(visitState, empty);
    }

    @Test
    public void testGetMemberDueStatusForUnderTwoChildren() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"visit_state"}, new Object[]{"DUE"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        String visitState = FamilyDao.getMemberDueStatusForUnderTwoChildren("12345");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(visitState, "DUE");
    }

    @Test
    public void testGetMemberDueStatusForUnderTwoChildrenReturnsEmptyString() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"visit_state"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        String visitState = FamilyDao.getMemberDueStatusForUnderTwoChildren("12345");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        String empty = "";
        Assert.assertEquals(visitState, empty);
    }

    @Test
    public void testGetFamilyCreateDateReturnsEmptyString() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"event_date"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        long eventDate = FamilyDao.getFamilyCreateDate("12345");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(eventDate, 0);
    }

    @Test
    public void testFamilyHasChildUnderFiveTrue() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"underFive"}, new Object[]{2});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        Boolean familyHasChildUnderFive = FamilyDao.familyHasChildUnderFive("12345");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(familyHasChildUnderFive, true);
    }

    @Test
    public void testFamilyHasChildUnderFiveFalse() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"underFive"}, new Object[]{0});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        Boolean familyHasChildUnderFive = FamilyDao.familyHasChildUnderFive("12345");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(familyHasChildUnderFive, false);
    }

    @Test
    public void testIsFamily() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"count"}, new Object[]{2});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        Boolean isFamily = FamilyDao.isFamily("12345");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(isFamily, true);
    }

    @Test
    public void testIsFamilyReturnsFalse() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"count"}, new Object[]{0});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        Boolean isFamily = FamilyDao.isFamily("12345");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(isFamily, false);
    }

    @Test
    public void getFamilyAlertStatusReturnsCorrectStatus() {
        Cursor matrixCursor = mockCursor(new String[]{"case"}, new Object[]{"2"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        AlertStatus status = FamilyDao.getFamilyAlertStatus("entity-id-123");
        Assert.assertEquals(AlertStatus.complete, status);
    }


    @Test
    public void testGetFamilyDetail() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"base_entity_id"}, new Object[]{"12345"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        FamilyDetailsModel familyDetailsModel = FamilyDao.getFamilyDetail("12345");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        assert familyDetailsModel != null;
        Assert.assertEquals(familyDetailsModel.getBaseEntityId(), "12345");
    }

    @Test
    public void testGetFamilyDetailReturnsNull() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        Cursor matrixCursor = mockCursor(new String[]{"base_entity_id"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        FamilyDetailsModel familyDetail = FamilyDao.getFamilyDetail("12345");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertNull(familyDetail);
    }
}
