package org.smartregister.chw.util;

import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.smartregister.repository.Repository;

import java.util.ArrayList;

import static org.smartregister.chw.util.TestCursorUtils.mockCursor;

public class DatabaseMigrationUtilsTest extends DatabaseMigrationUtils{
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
    public void testGetFormSubmissionsIds() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        Cursor matrixCursor = mockCursor(new String[]{
                "form_submission_id"}, new Object[]{"d5ff0ea1-bbc5-424d-84c2-5b084e10ef90"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        ArrayList<String> formSubmissionsIds = DatabaseMigrationUtils.getFormSubmissionsIds(database);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(formSubmissionsIds.get(0), "d5ff0ea1-bbc5-424d-84c2-5b084e10ef90");
    }

    @Test
    public void testGetJSONLists() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        Cursor matrixCursor = mockCursor(new String[]{
                "json","formSubmissionId"},
                new Object[]{"{\n" +
                        "    \"formSubmissionId\": \"d5ff0ea1-bbc5-424d-84c2-5b084e10ef90\",\n" +
                        "    \"providerId\": \"chaone\"\n" +
                        "}","d5ff0ea1-bbc5-424d-84c2-5b084e10ef90"},
                new Object[]{"{\n" +
                        "    \"formSubmissionId\": \"d5ff0ea1-bbc5-424d-84c2-5b084e10ef80\",\n" +
                        "    \"providerId\": \"chaone\"\n" +
                        "}","d5ff0ea1-bbc5-424d-84c2-5b084e10ef80"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        ArrayList<String> formSubmissionsIds = new ArrayList<>();
        formSubmissionsIds.add("d5ff0ea1-bbc5-424d-84c2-5b084e10ef90");
        formSubmissionsIds.add("d5ff0ea1-bbc5-424d-84c2-5b084e10ef80");
        ArrayList<String> jsonLists = DatabaseMigrationUtils.getJSONLists(database,formSubmissionsIds);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(jsonLists.get(0), "{\n" +
                "    \"formSubmissionId\": \"d5ff0ea1-bbc5-424d-84c2-5b084e10ef90\",\n" +
                "    \"providerId\": \"chaone\"\n" +
                "}");
        Assert.assertEquals(jsonLists.get(1), "{\n" +
                "    \"formSubmissionId\": \"d5ff0ea1-bbc5-424d-84c2-5b084e10ef80\",\n" +
                "    \"providerId\": \"chaone\"\n" +
                "}");
    }


    @Test
    public void testBuildUpdateQuery() {
        ArrayList<String> jsonList = new ArrayList<>();
        jsonList.add("{\n" +
                "    \"formSubmissionId\": \"d5ff0ea1-bbc5-424d-84c2-5b084e10ef90\",\n" +
                "    \"providerId\": \"chaone\"\n" +
                "}");
        jsonList.add("{\n" +
                "    \"formSubmissionId\": \"d5ff0ea1-bbc5-424d-84c2-5b084e10ef80\",\n" +
                "    \"providerId\": \"chaone\"\n" +
                "}");
        String updateQuery = DatabaseMigrationUtils.buildUpdateQuery(jsonList);
        Assert.assertEquals(updateQuery, "UPDATE ec_family_member_location SET provider_id = (case when form_submission_id = 'd5ff0ea1-bbc5-424d-84c2-5b084e10ef90' then 'chaone'when form_submission_id = 'd5ff0ea1-bbc5-424d-84c2-5b084e10ef80' then 'chaone' end)");
    }
}
