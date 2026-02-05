package org.smartregister.chw.dao;

import android.database.MatrixCursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.domain.PNCHealthFacilityVisitSummary;
import org.smartregister.repository.Repository;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ChwPNCDaoTest extends ChwPNCDao {

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
    public void testGetLastHealthFacilityVisitSummary() {
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"delivery_date"});
        matrixCursor.addRow(new Object[]{"12-03-2005"});

        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        @Nullable PNCHealthFacilityVisitSummary chwPNCDao = ChwPNCDao.getLastHealthFacilityVisitSummary("12345", database);

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertNotNull(chwPNCDao);
    }

    @Test
    public void testGetLastHealthFacilityVisitSummaryReturnsNull() {
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"delivery_date"});

        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        @Nullable PNCHealthFacilityVisitSummary chwPNCDao = ChwPNCDao.getLastHealthFacilityVisitSummary("12345", database);

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertNull(chwPNCDao);
    }


    @Test
    public void testGetLastPNCHealthFacilityVisits() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"delivery_date"});
        matrixCursor.addRow(new Object[]{"12-03-2005"});

        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        List<VisitDetail> visitDetail = ChwPNCDao.getLastPNCHealthFacilityVisits("12345");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(visitDetail.size(), 1);
    }

    @Test
    public void testGetLastPNCHealthFacilityVisitsReturnsNull() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"delivery_date"});

        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        List<VisitDetail> visitDetail = ChwPNCDao.getLastPNCHealthFacilityVisits("12345");

        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(visitDetail.size(), 0);
    }

}
