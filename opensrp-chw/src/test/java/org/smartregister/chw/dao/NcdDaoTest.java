package org.smartregister.chw.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import net.sqlcipher.MatrixCursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.smartregister.chw.ncd.domain.MemberObject;
import org.smartregister.dao.AbstractDao;
import org.smartregister.family.util.DBConstants;
import org.smartregister.repository.Repository;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class NcdDaoTest extends AbstractDao {

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
    public void getAtRiskClientsShouldReturnMemberObjects() {
        MatrixCursor cursor = new MatrixCursor(new String[]{DBConstants.KEY.BASE_ENTITY_ID});
        cursor.addRow(new Object[]{"base-1"});
        cursor.addRow(new Object[]{"base-2"});
        Mockito.doReturn(cursor).when(database).rawQuery(Mockito.anyString(), Mockito.<String[]>isNull());

        try (MockedStatic<org.smartregister.chw.ncd.dao.NcdDao> staticDao = Mockito.mockStatic(org.smartregister.chw.ncd.dao.NcdDao.class)) {
            staticDao.when(() -> org.smartregister.chw.ncd.dao.NcdDao.getMember("base-1"))
                    .thenReturn(createMember("base-1"));
            staticDao.when(() -> org.smartregister.chw.ncd.dao.NcdDao.getMember("base-2"))
                    .thenReturn(createMember("base-2"));

            List<MemberObject> members = NcdDao.getAtRiskClients();

            assertEquals(2, members.size());
            assertEquals("base-1", members.get(0).getBaseEntityId());
            assertEquals("base-2", members.get(1).getBaseEntityId());
        }
    }

    @Test
    public void getConfirmedClientsShouldDeduplicateMembers() {
        MatrixCursor cursor = new MatrixCursor(new String[]{DBConstants.KEY.BASE_ENTITY_ID});
        cursor.addRow(new Object[]{"base-3"});
        cursor.addRow(new Object[]{"base-3"});
        Mockito.doReturn(cursor).when(database).rawQuery(Mockito.anyString(), Mockito.<String[]>isNull());

        try (MockedStatic<org.smartregister.chw.ncd.dao.NcdDao> staticDao = Mockito.mockStatic(org.smartregister.chw.ncd.dao.NcdDao.class)) {
            MemberObject member = createMember("base-3");
            staticDao.when(() -> org.smartregister.chw.ncd.dao.NcdDao.getMember("base-3"))
                    .thenReturn(member);

            List<MemberObject> members = NcdDao.getConfirmedClients();

            assertEquals(1, members.size());
            staticDao.verify(() -> org.smartregister.chw.ncd.dao.NcdDao.getMember("base-3"), Mockito.times(1));
        }
    }

    @Test
    public void getClientByIdShouldDelegateToLibraryDao() {
        try (MockedStatic<org.smartregister.chw.ncd.dao.NcdDao> staticDao = Mockito.mockStatic(org.smartregister.chw.ncd.dao.NcdDao.class)) {
            MemberObject member = createMember("base-4");
            staticDao.when(() -> org.smartregister.chw.ncd.dao.NcdDao.getMember("base-4"))
                    .thenReturn(member);

            MemberObject result = NcdDao.getClientById("base-4");

            assertEquals("base-4", result.getBaseEntityId());
            staticDao.verify(() -> org.smartregister.chw.ncd.dao.NcdDao.getMember("base-4"));
        }
    }

    @Test
    public void getClientByIdShouldReturnNullWhenBlank() {
        try (MockedStatic<org.smartregister.chw.ncd.dao.NcdDao> staticDao = Mockito.mockStatic(org.smartregister.chw.ncd.dao.NcdDao.class)) {
            assertNull(NcdDao.getClientById(" "));
            staticDao.verifyNoInteractions();
        }
    }

    private MemberObject createMember(String baseEntityId) {
        MemberObject memberObject = new MemberObject();
        memberObject.setBaseEntityId(baseEntityId);
        return memberObject;
    }
}
