package org.smartregister.chw.model;

import org.smartregister.chw.core.model.CoreHarmReductionRegisterFragmentModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HarmReductionMatClientsRegisterFragmentModel extends CoreHarmReductionRegisterFragmentModel {

    static final String NEXT_APPOINTMENT_DATE = "next_appointment_date";
    static final String MAT_FOLLOWUP_VISIT = "Harm Reduction MAT Clients Followup";

    @Override
    public String[] mainColumns(String tableName) {
        List<String> columns = new ArrayList<>(Arrays.asList(super.mainColumns(tableName)));
        columns.add("(SELECT vd.details FROM visit_details vd "
                + "INNER JOIN visits v ON v.visit_id = vd.visit_id "
                + "WHERE v.base_entity_id = " + tableName + ".base_entity_id "
                + "AND v.visit_type = '" + MAT_FOLLOWUP_VISIT + "' "
                + "AND vd.visit_key = '" + NEXT_APPOINTMENT_DATE + "' "
                + "AND IFNULL(vd.details, '') <> '' "
                + "ORDER BY CAST(v.visit_date AS INTEGER) DESC LIMIT 1) AS "
                + NEXT_APPOINTMENT_DATE);
        return columns.toArray(new String[0]);
    }
}
