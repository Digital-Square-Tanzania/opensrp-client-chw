package org.smartregister.chw.interactor;

import android.util.Pair;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.domain.Alert;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.jsonmapping.VaccineGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DefaultChildHomeVisitInteractorFlvNormalizationTest extends BaseUnitTest {

    @Test
    public void testNormalizeIssuedVaccinesCanonicalizesNamesForScheduleMatching() {
        DefaultChildHomeVisitInteractorFlv interactor = new DefaultChildHomeVisitInteractorFlv() {
        };
        Date dateGiven = LocalDate.now().minusMonths(3).toDate();

        org.smartregister.immunization.domain.Vaccine mrOne = new org.smartregister.immunization.domain.Vaccine();
        mrOne.setBaseEntityId("child-id");
        mrOne.setName("MR1");
        mrOne.setDate(dateGiven);
        mrOne.setCalculation(1);

        org.smartregister.immunization.domain.Vaccine mrTwo = new org.smartregister.immunization.domain.Vaccine();
        mrTwo.setBaseEntityId("child-id");
        mrTwo.setName("mr_2");
        mrTwo.setDate(dateGiven);
        mrTwo.setCalculation(2);

        List<org.smartregister.immunization.domain.Vaccine> normalizedVaccines = interactor.normalizeIssuedVaccines(
                Arrays.asList(mrOne, mrTwo),
                Arrays.asList(VaccineRepo.Vaccine.mr1, VaccineRepo.Vaccine.mr2)
        );

        assertEquals("mr 1", normalizedVaccines.get(0).getName());
        assertEquals("mr 2", normalizedVaccines.get(1).getName());
        assertEquals("MR1", mrOne.getName());
        assertEquals(dateGiven, normalizedVaccines.get(0).getDate());
        assertEquals("mr1", interactor.normalizeVaccineNameForMatching(normalizedVaccines.get(0).getName()));
        Assert.assertNotSame(mrOne, normalizedVaccines.get(0));
    }

    @Test
    public void testRemoveIssuedVaccinesFromPendingFiltersPreviouslyRecordedVaccines() {
        DefaultChildHomeVisitInteractorFlv interactor = new DefaultChildHomeVisitInteractorFlv() {
        };

        org.smartregister.immunization.domain.Vaccine bcg = buildVaccine("BCG");
        org.smartregister.immunization.domain.Vaccine opvZero = buildVaccine("opv_0");
        org.smartregister.immunization.domain.Vaccine ipv = buildVaccine("IPV");

        VaccineGroup birthGroup = new VaccineGroup();
        birthGroup.name = "Birth";
        VaccineGroup fourteenWeeksGroup = new VaccineGroup();
        fourteenWeeksGroup.name = "14 weeks";

        Map<VaccineGroup, List<Pair<VaccineRepo.Vaccine, Alert>>> pendingVaccines = new LinkedHashMap<>();
        pendingVaccines.put(birthGroup, Arrays.asList(
                new Pair<>(VaccineRepo.Vaccine.bcg, null),
                new Pair<>(VaccineRepo.Vaccine.opv0, null)
        ));
        pendingVaccines.put(fourteenWeeksGroup, Arrays.asList(
                new Pair<>(VaccineRepo.Vaccine.ipv, null),
                new Pair<>(VaccineRepo.Vaccine.opv3, null)
        ));

        Map<VaccineGroup, List<Pair<VaccineRepo.Vaccine, Alert>>> filteredVaccines = interactor.removeIssuedVaccinesFromPending(
                pendingVaccines,
                Arrays.asList(bcg, opvZero, ipv),
                Arrays.asList(VaccineRepo.Vaccine.bcg, VaccineRepo.Vaccine.opv0, VaccineRepo.Vaccine.ipv, VaccineRepo.Vaccine.opv3)
        );

        Assert.assertFalse(filteredVaccines.containsKey(birthGroup));
        assertEquals(1, filteredVaccines.get(fourteenWeeksGroup).size());
        assertEquals(VaccineRepo.Vaccine.opv3, filteredVaccines.get(fourteenWeeksGroup).get(0).first);
    }

    @Test
    public void testMergeIssuedVaccinesFromVisitDetailsIncludesMedicalHistoryVaccines() {
        DefaultChildHomeVisitInteractorFlv interactor = new DefaultChildHomeVisitInteractorFlv() {
        };

        List<org.smartregister.immunization.domain.Vaccine> repositoryVaccines = new ArrayList<>();
        repositoryVaccines.add(buildVaccine("OPV 1"));

        Map<String, List<VisitDetail>> medicalHistory = new HashMap<>();
        medicalHistory.put("visit-one", Arrays.asList(
                buildVisitDetail("bcg", "2025-12-05"),
                buildVisitDetail("opv_0", "2025-12-05"),
                buildVisitDetail("opv_1", "2026-04-22"),
                buildVisitDetail("rota_3", "2026-11-17"),
                buildVisitDetail("opv_2", org.smartregister.chw.anc.util.Constants.HOME_VISIT.VACCINE_NOT_GIVEN),
                buildVisitDetail("reasons_no_vaccination", "{\"missing_vaccines\":[]}"),
                buildVisitDetail("danger_signs", "None", "danger_sign")
        ));

        List<org.smartregister.immunization.domain.Vaccine> mergedVaccines = interactor.mergeIssuedVaccinesFromVisitDetails(repositoryVaccines, medicalHistory, "child-id");

        assertEquals(4, mergedVaccines.size());
        assertEquals("OPV 1", mergedVaccines.get(0).getName());
        assertEquals("bcg", mergedVaccines.get(1).getName());
        assertEquals("opv_0", mergedVaccines.get(2).getName());
        assertEquals("rota_3", mergedVaccines.get(3).getName());
        assertEquals("child-id", mergedVaccines.get(1).getBaseEntityId());
    }

    private org.smartregister.immunization.domain.Vaccine buildVaccine(String name) {
        org.smartregister.immunization.domain.Vaccine vaccine = new org.smartregister.immunization.domain.Vaccine();
        vaccine.setBaseEntityId("child-id");
        vaccine.setName(name);
        vaccine.setDate(LocalDate.now().minusMonths(3).toDate());
        return vaccine;
    }

    private VisitDetail buildVisitDetail(String visitKey, String details) {
        return buildVisitDetail(visitKey, details, "vaccine");
    }

    private VisitDetail buildVisitDetail(String visitKey, String details, String parentCode) {
        VisitDetail visitDetail = new VisitDetail();
        visitDetail.setParentCode(parentCode);
        visitDetail.setVisitKey(visitKey);
        visitDetail.setDetails(details);
        return visitDetail;
    }
}
