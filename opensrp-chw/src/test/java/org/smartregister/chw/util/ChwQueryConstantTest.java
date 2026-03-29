package org.smartregister.chw.util;

import org.junit.Assert;
import org.junit.Test;

public class ChwQueryConstantTest {

    @Test
    public void testIndependentClientsExcludeOpenSoberHouseEnrollment() {
        Assert.assertTrue(ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY.contains(
                "SELECT ec_harm_reduction_sober_house_enrollment.base_entity_id AS base_entity_id\n" +
                        "    FROM ec_harm_reduction_sober_house_enrollment where ec_harm_reduction_sober_house_enrollment.is_closed is 0"
        ));
    }

    @Test
    public void testSoberHouseClientsHaveDedicatedRegisterTypeBranch() {
        Assert.assertTrue(ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY.contains("/*ONLY Sober House clients*/"));
        Assert.assertTrue(ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY.contains("'Harm Reduction Sober House'                 AS register_type"));
        Assert.assertTrue(ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY.contains("inner join ec_harm_reduction_sober_house_enrollment"));
    }

    @Test
    public void testQueryDoesNotAddGenericHarmReductionRegisterType() {
        Assert.assertFalse(ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY.contains("/*ONLY Harm Reduction clients*/"));
        Assert.assertFalse(ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY.contains("'HARM REDUCTION'                             AS register_type"));
    }
}
