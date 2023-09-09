package org.smartregister.chw.dao;

import net.sqlcipher.MatrixCursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.dao.AbstractDao;
import org.smartregister.repository.Repository;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class EventDaoTest extends AbstractDao {

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
    public void getEvents() {
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"json"});
        String eventJson = "{\"type\":\"Event\",\"dateCreated\":\"2019-10-07T12:49:52.677+03:00\",\"dateEdited\":\"2019-09-18T13:10:56.369+03:00\",\"serverVersion\":1568801628666,\"clientApplicationVersion\":1,\"clientDatabaseVersion\":11,\"identifiers\":{},\"baseEntityId\":\"ec610c3c-7e37-4559-9fee-6c99d870b197\",\"locationId\":\"2c3a0ebd-f79d-4128-a6d3-5dfbffbd01c8\",\"eventDate\":\"2019-09-01T19:28:56.498+03:00\",\"eventType\":\"PNC Child Registration\",\"formSubmissionId\":\"4a89abf1-0907-45c5-81f4-5c64924a279c\",\"providerId\":\"chwone\",\"duration\":0,\"obs\":[{\"fieldType\":\"concept\",\"fieldDataType\":\"text\",\"fieldCode\":\"entry_point\",\"parentCode\":\"\",\"values\":[\"PNC\"],\"formSubmissionField\":\"entry_point\",\"humanReadableValues\":[]},{\"fieldType\":\"concept\",\"fieldDataType\":\"text\",\"fieldCode\":\"162558AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"parentCode\":\"\",\"values\":[\"1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"],\"formSubmissionField\":\"disabilities\",\"humanReadableValues\":[\"No\"]}],\"entityType\":\"ec_child\",\"version\":1567344536498,\"teamId\":\"d60e1ee9-19e9-4e7d-a949-39f790a0ceda\",\"team\":\"Huruma Dispensary\",\"_id\":\"afbfff6f-764a-480e-984b-2639017486d6\",\"_rev\":\"v70\"}";
        matrixCursor.addRow(new Object[]{eventJson});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        List<Event> events = EventDao.getEvents("some-base-entity-id", "some-event-type", 100);
        Assert.assertNotNull(events);
        Assert.assertEquals(events.size(), 1);
        Assert.assertEquals("ec610c3c-7e37-4559-9fee-6c99d870b197", events.get(0).getBaseEntityId());
    }

}