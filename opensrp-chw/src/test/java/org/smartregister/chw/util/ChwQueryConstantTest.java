package org.smartregister.chw.util;

import org.junit.Assert;
import org.junit.Test;

public class ChwQueryConstantTest {

    @Test
    public void testIndependentClientsExcludeOpenSoberHouseEnrollment() {
        Assert.assertTrue(ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY.contains(
                "SELECT ec_harm_reduction_sober_house_enrollment.base_entity_id AS base_entity_id\n" +
                        "    FROM ec_harm_reduction_sober_house_enrollment where ec_harm_reduction_sober_house_enrollment.is_closed is 0 AND ec_harm_reduction_sober_house_enrollment.detoxification_done = 'yes'"
        ));
    }

    @Test
    public void testSoberHouseClientsHaveDedicatedRegisterTypeBranch() {
        Assert.assertTrue(ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY.contains("/*ONLY Sober House clients*/"));
        Assert.assertTrue(ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY.contains("'Harm Reduction Sober House'                 AS register_type"));
        Assert.assertTrue(ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY.contains("inner join ec_harm_reduction_sober_house_enrollment"));
    }

    @Test
    public void testHarmReductionBranchExcludesOpenSoberHouseEnrollment() {
        String query = ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY;
        int harmReductionIndex = query.indexOf("/*ONLY Harm Reduction clients*/");
        int soberHouseIndex = query.indexOf("/*ONLY Sober House clients*/");

        Assert.assertTrue(harmReductionIndex >= 0);
        Assert.assertTrue(soberHouseIndex > harmReductionIndex);

        String harmReductionSection = query.substring(harmReductionIndex, soberHouseIndex);
        Assert.assertTrue(harmReductionSection.contains("SELECT ec_harm_reduction_sober_house_enrollment.base_entity_id AS base_entity_id"));
        Assert.assertTrue(harmReductionSection.contains("ec_harm_reduction_sober_house_enrollment.is_closed is 0 AND ec_harm_reduction_sober_house_enrollment.detoxification_done = 'yes'"));
    }

    @Test
    public void testMotherMentorClientsHaveDedicatedRegisterTypeBranch() {
        String query = ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY;
        Assert.assertTrue(query.contains("/*ONLY Mother Mentor clients*/"));
        Assert.assertTrue(query.contains("'Mother Mentor'                             AS register_type"));
        Assert.assertTrue(query.contains("inner join ec_mothermentor_enrollment"));
    }

    @Test
    public void testIndependentClientsExcludeActiveMotherMentorEnrollment() {
        Assert.assertTrue(ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY.contains(
                "SELECT ec_mothermentor_enrollment.base_entity_id AS base_entity_id\n" +
                        "    FROM ec_mothermentor_enrollment\n" +
                        "    WHERE ec_mothermentor_enrollment.is_closed is 0 AND (ec_mothermentor_enrollment.status IS NULL OR ec_mothermentor_enrollment.status = 'client') AND (ec_mothermentor_enrollment.screening_status IS NULL OR ec_mothermentor_enrollment.screening_status != '-')"
        ));
    }

    @Test
    public void testQueryAddsGenericHarmReductionRegisterType() {
        Assert.assertTrue(ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY.contains("/*ONLY Harm Reduction clients*/"));
        Assert.assertTrue(ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY.contains("'HARM REDUCTION'                             AS register_type"));
    }
}
