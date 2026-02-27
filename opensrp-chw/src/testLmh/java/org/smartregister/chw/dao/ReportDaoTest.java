package org.smartregister.chw.dao;

import android.database.MatrixCursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.smartregister.chw.domain.EligibleChild;
import org.smartregister.chw.domain.VillageDose;
import org.smartregister.chw.util.ReportingConstants;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.Repository;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReportDaoTest extends ReportDao {
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
    public void testExtractRecordedLocations() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "location_id", "provider_id"});
        matrixCursor.addRow(new Object[]{"d5ff0ea1-bbc5-424d-84c2-5b084e10ef90", "demo"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        Map<String, String> providerList = ReportDao.extractRecordedLocations();
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        String expectedLocationId = null;
        String expectedProviderId = null;
        for (Map.Entry<String, String> entry : providerList.entrySet()) {
            expectedLocationId = entry.getKey();
            expectedProviderId = entry.getValue();
        }
        Assert.assertEquals(expectedLocationId, "d5ff0ea1-bbc5-424d-84c2-5b084e10ef90");
        Assert.assertEquals(expectedProviderId, "demo");
    }

    @Test
    public void testFetchAllVaccines() throws ParseException {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                VaccineRepository.CREATED_AT, VaccineRepository.ID_COLUMN, VaccineRepository.BASE_ENTITY_ID,
                VaccineRepository.PROGRAM_CLIENT_ID, VaccineRepository.NAME, VaccineRepository.CALCULATION,
                VaccineRepository.DATE, VaccineRepository.ANMID, VaccineRepository.LOCATION_ID,
                VaccineRepository.SYNC_STATUS, VaccineRepository.HIA2_STATUS, VaccineRepository.UPDATED_AT_COLUMN,
                VaccineRepository.EVENT_ID, VaccineRepository.FORMSUBMISSION_ID, VaccineRepository.OUT_OF_AREA,
                VaccineRepository.TEAM, VaccineRepository.TEAM_ID, VaccineRepository.CHILD_LOCATION_ID
        });
        matrixCursor.addRow(new Object[]{
                "2020-03-20 11:22:34", 1, "85e5dd54-ba27-46b1-b5c2-2bab06fd77e2",
                "chw", "bcg", 1,
                "1567112400000", "chaone", "402ecf03-af72-4c93-b099-e1ce327d815b",
                "Synced", "hia_2", "1584692554019",
                "23147a8f-d301-43a1-876e-93f30088e2d7", "d3e94182-a7c8-457a-a5a5-40354bfb37e4", 0,
                "Team A", "team_1", "4d30ecac-536b-4a90-b712-8613d3768717"
        });
        matrixCursor.addRow(new Object[]{
                "2020-03-20 11:22:34", 1, "85e5dd54-ba27-46b1-b5c2-2bab06fd77e2",
                "chw", "opv_0", 0,
                "1567112400000", "chaone", "402ecf03-af72-4c93-b099-e1ce327d815b",
                "Synced", "hia_2", "1584692554019",
                "23147a8f-d301-43a1-876e-93f30088e2d7", "d3e94182-a7c8-457a-a5a5-40354bfb37e4", 0,
                "Team A", "team_1", "4d30ecac-536b-4a90-b712-8613d3768717"
        });
        matrixCursor.addRow(new Object[]{
                "2020-03-20 11:22:34", 1, "69b2ba54-483d-48c2-baec-929b862f3ac1",
                "chw", "opv_0", 0,
                "1567112400000", "chaone", "402ecf03-af72-4c93-b099-e1ce327d815b",
                "Synced", "hia_2", "1584692554019",
                "23147a8f-d301-43a1-876e-93f30088e2d7", "d3e94182-a7c8-457a-a5a5-40354bfb37e4", 0,
                "Team A", "team_1", "4d30ecac-536b-4a90-b712-8613d3768717"
        });
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        // test values
        Map<String, List<Vaccine>> providerList = ReportDao.fetchAllVaccines();
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());

        // test results
        Assert.assertEquals(providerList.size(), 2);
        List<Vaccine> vaccines = providerList.get("85e5dd54-ba27-46b1-b5c2-2bab06fd77e2");
        Assert.assertEquals(vaccines.size(), 2);

        Vaccine vaccine = vaccines.get(0);
        Assert.assertEquals(vaccine.getCreatedAt(), EventClientRepository.dateFormat.parse("2020-03-20 11:22:34"));
    }

    @Test
    public void testFetchLiveEligibleChildrenReport() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "base_entity_id", "unique_id", "first_name", "last_name", "middle_name",
                "family_name", "dob", "gender", "location_id"
        });
        matrixCursor.addRow(new Object[]{
                "85e5dd54-ba27-46b1-b5c2-2bab06fd77e2", "12345", "Tonak", "Mboshp", "",
                "Maxwell", "2020-03-20 11:22:34", "Male", "402ecf03-af72-4c93-b099-e1ce327d815b"
        });

        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());


        List<EligibleChild> children = ReportDao.fetchLiveEligibleChildrenReport(new ArrayList<>(), new Date());

        Assert.assertEquals(children.size(), 0);
    }

    @Test
    public void testExtractRecordedLocationsReturnsEmptyArrayList() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "location_id", "provider_id"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        Map<String, String> providerList = ReportDao.extractRecordedLocations();
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(providerList.size(), 0);
    }

    @Test
    public void testEligibleChildrenReport() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "base_entity_id"});
        matrixCursor.addRow(new Object[]{"abdb6140-d54b-49d4-89e7-6f96c839c62b"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        ArrayList<String> communityIds = new ArrayList<>();
        communityIds.add("d5ff0ea1-bbc5-424d-84c2-5b084e10ef90");
        Date dueDate = new DateTime().plusDays(7).toDate();
        List<EligibleChild> eligibleChildren = ReportDao.eligibleChildrenReport(communityIds, dueDate);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(eligibleChildren.get(0).getID(), "abdb6140-d54b-49d4-89e7-6f96c839c62b");
    }

    @Test
    public void testEligibleChildrenReportReturnsEmptyList() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "base_entity_id"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        ArrayList<String> communityIds = new ArrayList<>();
        communityIds.add("d5ff0ea1-bbc5-424d-84c2-5b084e10ef90");
        Date dueDate = new DateTime().plusDays(7).toDate();
        List<EligibleChild> eligibleChildren = ReportDao.eligibleChildrenReport(communityIds, dueDate);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(eligibleChildren.size(), 0);
    }

    @Test
    public void testVillageDosesReportSummary() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "scheduleName"});
        matrixCursor.addRow(new Object[]{"ROTA 1"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        String villageName = "Grenaligne";
        Date dueDate = new DateTime().plusDays(7).toDate();
        Map<String, Integer> map = new TreeMap<>();
        map.put("ROTA", 0);
        List<VillageDose> villageDoseList = ReportDao.villageDosesReportSummary(villageName, dueDate);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(villageDoseList.get(0).getRecurringServices(), map);
    }

    @Test
    public void testFetchLiveVillageDosesReport() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "base_entity_id", "unique_id", "first_name", "last_name", "middle_name",
                "family_name", "dob", "gender", "location_id"
        });
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        List<VillageDose> villages = ReportDao.fetchLiveVillageDosesReport(new ArrayList<>(), new Date(), true, "Sample", new HashMap<>());

        Assert.assertEquals(villages.size(), 1);
    }

    @Test
    public void testMyCommunityActivityReportDetailsChildrenDewormed() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "base_entity_id"});
        matrixCursor.addRow(new Object[]{"abdb6140-d54b-49d4-89e7-6f96c839c62b"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        String indicatorCode = ReportingConstants.ChildIndicatorKeys.COUNT_OF_CHILDREN_12_59_DEWORMED;
        List<EligibleChild> eligibleChildren = ReportDao.myCommunityActivityReportDetails(indicatorCode);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(eligibleChildren.get(0).getID(), "abdb6140-d54b-49d4-89e7-6f96c839c62b");
    }

    @Test
    public void testMyCommunityActivityReportDetailsChildrenNotDewormed() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "base_entity_id"});
        matrixCursor.addRow(new Object[]{"abdb6140-d54b-49d4-89e7-6f96c839c62b"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        String indicatorCode = ReportingConstants.ChildIndicatorKeys.COUNT_OF_CHILDREN_12_59_NOT_DEWORMED;
        List<EligibleChild> eligibleChildren = ReportDao.myCommunityActivityReportDetails(indicatorCode);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(eligibleChildren.get(0).getID(), "abdb6140-d54b-49d4-89e7-6f96c839c62b");
    }

    @Test
    public void testMyCommunityActivityReportDetailsChildrenUpToDateVaccination() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "base_entity_id"});
        matrixCursor.addRow(new Object[]{"abdb6140-d54b-49d4-89e7-6f96c839c62b"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        String indicatorCode = ReportingConstants.ChildIndicatorKeys.COUNT_OF_CHILDREN_0_24_UPTO_DATE_VACCINATIONS;
        List<EligibleChild> eligibleChildren = ReportDao.myCommunityActivityReportDetails(indicatorCode);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(eligibleChildren.get(0).getID(), "abdb6140-d54b-49d4-89e7-6f96c839c62b");
    }

    @Test
    public void testMyCommunityActivityReportDetailsChildrenOverDueVaccination() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "base_entity_id"});
        matrixCursor.addRow(new Object[]{"abdb6140-d54b-49d4-89e7-6f96c839c62b"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        String indicatorCode = ReportingConstants.ChildIndicatorKeys.COUNT_OF_CHILDREN_0_24_OVERDUE_VACCINATIONS;
        List<EligibleChild> eligibleChildren = ReportDao.myCommunityActivityReportDetails(indicatorCode);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(eligibleChildren.get(0).getID(), "abdb6140-d54b-49d4-89e7-6f96c839c62b");
    }


    @Test
    public void testMyCommunityActivityReportDetailsChildrenWithBirthCerts() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "base_entity_id"});
        matrixCursor.addRow(new Object[]{"abdb6140-d54b-49d4-89e7-6f96c839c62b"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        String indicatorCode = ReportingConstants.ChildIndicatorKeys.COUNT_OF_CHILDREN_0_59_WITH_BIRTH_CERT;
        List<EligibleChild> eligibleChildren = ReportDao.myCommunityActivityReportDetails(indicatorCode);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(eligibleChildren.get(0).getID(), "abdb6140-d54b-49d4-89e7-6f96c839c62b");
    }

    @Test
    public void testMyCommunityActivityReportDetailsChildrenNoBirthCerts() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "base_entity_id"});
        matrixCursor.addRow(new Object[]{"abdb6140-d54b-49d4-89e7-6f96c839c62b"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        String indicatorCode = ReportingConstants.ChildIndicatorKeys.COUNT_OF_CHILDREN_0_59_WITH_NO_BIRTH_CERT;
        List<EligibleChild> eligibleChildren = ReportDao.myCommunityActivityReportDetails(indicatorCode);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(eligibleChildren.get(0).getID(), "abdb6140-d54b-49d4-89e7-6f96c839c62b");
    }

    @Test
    public void testMyCommunityActivityReportDetailsChildrenExclusiveBreastFeeding() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "base_entity_id"});
        matrixCursor.addRow(new Object[]{"abdb6140-d54b-49d4-89e7-6f96c839c62b"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        String indicatorCode = ReportingConstants.ChildIndicatorKeys.COUNT_OF_CHILDREN_0_5_EXCLUSIVELY_BREASTFEEDING;
        List<EligibleChild> eligibleChildren = ReportDao.myCommunityActivityReportDetails(indicatorCode);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(eligibleChildren.get(0).getID(), "abdb6140-d54b-49d4-89e7-6f96c839c62b");
    }

    @Test
    public void testMyCommunityActivityReportDetailsChildrenNotExclusiveBreastFeeding() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "base_entity_id"});
        matrixCursor.addRow(new Object[]{"abdb6140-d54b-49d4-89e7-6f96c839c62b"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        String indicatorCode = ReportingConstants.ChildIndicatorKeys.COUNT_OF_CHILDREN_0_5_NOT_EXCLUSIVELY_BREASTFEEDING;
        List<EligibleChild> eligibleChildren = ReportDao.myCommunityActivityReportDetails(indicatorCode);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(eligibleChildren.get(0).getID(), "abdb6140-d54b-49d4-89e7-6f96c839c62b");
    }


    @Test
    public void testMyCommunityActivityReportDetailsChildrenVitaminAReceived() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "base_entity_id"});
        matrixCursor.addRow(new Object[]{"abdb6140-d54b-49d4-89e7-6f96c839c62b"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        String indicatorCode = ReportingConstants.ChildIndicatorKeys.COUNT_OF_CHILDREN_6_59_VITAMIN_RECEIVED_A;
        List<EligibleChild> eligibleChildren = ReportDao.myCommunityActivityReportDetails(indicatorCode);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(eligibleChildren.get(0).getID(), "abdb6140-d54b-49d4-89e7-6f96c839c62b");
    }

    @Test
    public void testMyCommunityActivityReportDetailsChildrenVitaminANotReceived() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "base_entity_id"});
        matrixCursor.addRow(new Object[]{"abdb6140-d54b-49d4-89e7-6f96c839c62b"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        String indicatorCode = ReportingConstants.ChildIndicatorKeys.COUNT_OF_CHILDREN_6_59_VITAMIN_NOT_RECEIVED_A;
        List<EligibleChild> eligibleChildren = ReportDao.myCommunityActivityReportDetails(indicatorCode);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(eligibleChildren.get(0).getID(), "abdb6140-d54b-49d4-89e7-6f96c839c62b");
    }

    @Test
    public void testMyCommunityActivityReportDetailsReturnsEmptyArray() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                "base_entity_id"});
        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());
        String indicatorCode = ReportingConstants.ChildIndicatorKeys.COUNT_OF_CHILDREN_12_59_DEWORMED;
        List<EligibleChild> eligibleChildren = ReportDao.myCommunityActivityReportDetails(indicatorCode);
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertEquals(eligibleChildren.size(), 0);
    }
}