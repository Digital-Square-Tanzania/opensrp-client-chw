package org.smartregister.chw.model;

import org.junit.Assert;
import org.junit.Test;

public class HarmReductionMatClientsRegisterFragmentModelTest {

    @Test
    public void mainColumnsSelectLatestMatAppointmentDate() {
        String[] columns = new HarmReductionMatClientsRegisterFragmentModel()
                .mainColumns("ec_harm_reduction_risk_assessment");

        String appointmentColumn = columns[columns.length - 1];
        Assert.assertTrue(appointmentColumn.contains("v.visit_type = 'Harm Reduction MAT Clients Followup'"));
        Assert.assertTrue(appointmentColumn.contains("vd.visit_key = 'next_appointment_date'"));
        Assert.assertTrue(appointmentColumn.contains("ORDER BY CAST(v.visit_date AS INTEGER) DESC LIMIT 1"));
        Assert.assertTrue(appointmentColumn.endsWith("AS next_appointment_date"));
    }
}
