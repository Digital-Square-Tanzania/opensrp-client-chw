package org.smartregister.chw.util;

public interface ChwQueryConstant {
    String ALL_CLIENTS_SELECT_QUERY = "SELECT * FROM (" +
            "/*INDEPENDENT MEMBERS*/\n" +
            "SELECT ec_family_member.first_name,\n" +
            "       ec_family_member.middle_name,\n" +
            "       ec_family_member.last_name,\n" +
            "       ec_family_member.gender,\n" +
            "       ec_family_member.dob,\n" +
            "       ec_family_member.base_entity_id,\n" +
            "       ec_family_member.id                   as _id,\n" +
            "       'Independent'                         AS register_type,\n" +
            "       ec_family_member.relational_id        as relationalid,\n" +
            "       ec_family.village_town                as home_address,\n" +
            "       NULL                                  AS mother_first_name,\n" +
            "       NULL                                  AS mother_last_name,\n" +
            "       NULL                                  AS mother_middle_name,\n" +
            "       ec_family_member.last_interacted_with AS last_interacted_with\n" +
            "FROM ec_family_member\n" +
            "         inner join ec_family on ec_family.base_entity_id = ec_family_member.relational_id\n" +
            "where ec_family_member.date_removed is null\n" +
            "  AND ec_family.entity_type = 'ec_independent_client'\n" +
            "  AND ec_family_member.base_entity_id IN (%s)\n" +
            "  AND ec_family_member.base_entity_id NOT IN (\n" +
            "    SELECT ec_agyw_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_agyw_register where ec_agyw_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_kvp_prep_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_kvp_prep_register where ec_kvp_prep_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_anc_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_anc_register where ec_anc_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_pregnancy_outcome.base_entity_id AS base_entity_id\n" +
            "    FROM ec_pregnancy_outcome where ec_pregnancy_outcome.delivery_date is not null\n" +
            "    UNION ALL\n" +
            "    SELECT ec_child.base_entity_id AS base_entity_id\n" +
            "    FROM ec_child\n" +
            "    UNION ALL\n" +
            "    SELECT ec_malaria_confirmation.base_entity_id AS base_entity_id\n" +
            "    FROM ec_malaria_confirmation\n" +
            "    WHERE datetime('NOW') <= datetime(ec_malaria_confirmation.last_interacted_with/1000, 'unixepoch', 'localtime','+15 days')\n" +
            "    AND ec_malaria_confirmation.is_closed = 0 AND ec_malaria_confirmation.malaria = 1 \n" +
            "    UNION ALL\n" +
            "    SELECT ec_iccm_enrollment.entity_id AS base_entity_id\n" +
            "    FROM ec_iccm_enrollment\n" +
            "    WHERE date('now') <= date(strftime('%Y-%m-%d', ec_iccm_enrollment.last_interacted_with / 1000, 'unixepoch', 'localtime'))\n" +
            "    AND ec_iccm_enrollment.is_closed = 0  \n" +
            "    UNION ALL\n" +
            "    SELECT ec_family_planning.base_entity_id AS base_entity_id\n" +
            "    FROM ec_family_planning\n" +
            "    UNION ALL\n" +
            "    SELECT ec_tb_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_tb_register\n" +
            "    WHERE ec_tb_register.tb_case_closure_date is null\n" +
            "    UNION ALL\n" +
            "    SELECT ec_cbhs_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_cbhs_register\n" +
            "    WHERE is_closed is 0 \n" +
            ")" +
            "UNION ALL" +
            "/* CBHS REGISTER */\n" +
            "\n" +
            "SELECT ec_family_member.first_name               AS first_name,\n" +
            "       ec_family_member.middle_name              AS middle_name,\n" +
            "       ec_family_member.last_name                AS last_name,\n" +
            "       ec_family_member.gender                   AS gender,\n" +
            "       ec_family_member.dob                      AS dob,\n" +
            "       ec_family_member.base_entity_id           AS base_entity_id,\n" +
            "       ec_family_member.id                       as _id,\n" +
            "       'CBHS'                                     AS register_type,\n" +
            "       ec_family_member.relational_id            as relationalid,\n" +
            "       ec_family.village_town                    as home_address,\n" +
            "       ec_cbhs_register.last_interacted_with      AS last_interacted_with,\n" +
            "       NULL                                      AS mother_first_name,\n" +
            "       NULL                                      AS mother_last_name,\n" +
            "       NULL                                      AS mother_middle_name\n" +
            "FROM ec_cbhs_register\n" +
            "         inner join ec_family_member on ec_family_member.base_entity_id = ec_cbhs_register.base_entity_id\n" +
            "         inner join ec_family on ec_family.base_entity_id = ec_family_member.relational_id\n" +
            "where ec_family_member.date_removed is null\n" +
            "  AND ec_cbhs_register.is_closed is 0\n" +
            "  AND ec_cbhs_register.base_entity_id IN (%s)\n" +
            "  AND ec_family_member.base_entity_id NOT IN (\n" +
            "    SELECT ec_agyw_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_agyw_register where ec_agyw_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_kvp_prep_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_kvp_prep_register where ec_kvp_prep_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_anc_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_anc_register\n" +
            "    UNION ALL\n" +
            "    SELECT ec_pregnancy_outcome.base_entity_id AS base_entity_id\n" +
            "    FROM ec_pregnancy_outcome\n" +
            "    UNION ALL\n" +
            "    SELECT ec_child.base_entity_id AS base_entity_id\n" +
            "    FROM ec_child\n" +
            "    UNION ALL\n" +
            "    SELECT ec_tb_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_tb_register\n" +
            "    WHERE ec_tb_register.tb_case_closure_date is null\n" +
            ")\n" +
            "\n" +
            "UNION ALL\n" +
            "/*OTHER FAMILY MEMBERS*/\n" +
            "SELECT ec_family_member.first_name,\n" +
            "       ec_family_member.middle_name,\n" +
            "       ec_family_member.last_name,\n" +
            "       ec_family_member.gender,\n" +
            "       ec_family_member.dob,\n" +
            "       ec_family_member.base_entity_id,\n" +
            "       ec_family_member.id                   as _id,\n" +
            "       NULL                                  AS register_type,\n" +
            "       ec_family_member.relational_id        as relationalid,\n" +
            "       ec_family.village_town                as home_address,\n" +
            "       NULL                                  AS mother_first_name,\n" +
            "       NULL                                  AS mother_last_name,\n" +
            "       NULL                                  AS mother_middle_name,\n" +
            "       ec_family_member.last_interacted_with AS last_interacted_with\n" +
            "FROM ec_family_member\n" +
            "         inner join ec_family on ec_family.base_entity_id = ec_family_member.relational_id\n" +
            "where ec_family_member.date_removed is null\n" +
            "  AND (ec_family.entity_type = 'ec_family' OR ec_family.entity_type is null)\n" +
            "  AND ec_family_member.base_entity_id IN (%s)\n" +
            "  AND ec_family_member.base_entity_id NOT IN (\n" +
            "    SELECT ec_agyw_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_agyw_register where ec_agyw_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_kvp_prep_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_kvp_prep_register where ec_kvp_prep_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_anc_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_anc_register where ec_anc_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_pregnancy_outcome.base_entity_id AS base_entity_id\n" +
            "    FROM ec_pregnancy_outcome where ec_pregnancy_outcome.delivery_date is not null\n" +
            "    UNION ALL\n" +
            "    SELECT ec_child.base_entity_id AS base_entity_id\n" +
            "    FROM ec_child\n" +
            "    UNION ALL\n" +
            "    SELECT ec_malaria_confirmation.base_entity_id AS base_entity_id\n" +
            "    FROM ec_malaria_confirmation\n" +
            "    WHERE datetime('NOW') <= datetime(ec_malaria_confirmation.last_interacted_with/1000, 'unixepoch', 'localtime','+15 days')\n" +
            "    AND ec_malaria_confirmation.is_closed = 0 AND ec_malaria_confirmation.malaria = 1 \n" +
            "    UNION ALL\n" +
            "    SELECT ec_iccm_enrollment.entity_id AS base_entity_id\n" +
            "    FROM ec_iccm_enrollment\n" +
            "    WHERE date('now') <= date(strftime('%Y-%m-%d', ec_iccm_enrollment.last_interacted_with / 1000, 'unixepoch', 'localtime'))\n" +
            "    AND ec_iccm_enrollment.is_closed = 0  \n" +
            "    UNION ALL\n" +
            "    SELECT ec_family_planning.base_entity_id AS base_entity_id\n" +
            "    FROM ec_family_planning\n" +
            "    UNION ALL\n" +
            "    SELECT ec_tb_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_tb_register\n" +
            "    WHERE ec_tb_register.tb_case_closure_date is null\n" +
            "    UNION ALL\n" +
            "    SELECT ec_cbhs_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_cbhs_register\n" +
            ")\n" +
            "UNION ALL\n" +
            "/* ANC REGISTER */\n" +
            "SELECT ec_family_member.first_name          AS first_name,\n" +
            "       ec_family_member.middle_name         AS middle_name,\n" +
            "       ec_family_member.last_name           AS last_name,\n" +
            "       ec_family_member.gender              AS gender,\n" +
            "       ec_family_member.dob                 AS dob,\n" +
            "       ec_family_member.base_entity_id      AS base_entity_id,\n" +
            "       ec_family_member.id                  as _id,\n" +
            "       'ANC'                                AS register_type,\n" +
            "       ec_family_member.relational_id       as relationalid,\n" +
            "       ec_family.village_town               as home_address,\n" +
            "       ec_anc_register.last_interacted_with AS last_interacted_with,\n" +
            "       NULL                                 AS mother_first_name,\n" +
            "       NULL                                 AS mother_last_name,\n" +
            "       NULL                                 AS mother_middle_name\n" +
            "FROM ec_anc_register\n" +
            "         inner join ec_family_member on ec_family_member.base_entity_id = ec_anc_register.base_entity_id\n" +
            "         inner join ec_family on ec_family.base_entity_id = ec_family_member.relational_id\n" +
            "where ec_family_member.date_removed is null\n" +
            "  and ec_anc_register.is_closed is 0\n" +
            "  and ec_anc_register.base_entity_id IN (%s)\n" +
            "\n" +
            "UNION ALL\n" +
            "\n" +
            "/* PNC REGISTER */\n" +
            "\n" +
            "SELECT ec_family_member.first_name               AS first_name,\n" +
            "       ec_family_member.middle_name              AS middle_name,\n" +
            "       ec_family_member.last_name                AS last_name,\n" +
            "       ec_family_member.gender                   AS gender,\n" +
            "       ec_family_member.dob                      AS dob,\n" +
            "       ec_family_member.base_entity_id           AS base_entity_id,\n" +
            "       ec_family_member.id                       as _id,\n" +
            "       'PNC'                                     AS register_type,\n" +
            "       ec_family_member.relational_id            as relationalid,\n" +
            "       ec_family.village_town                    as home_address,\n" +
            "       ec_pregnancy_outcome.last_interacted_with AS last_interacted_with,\n" +
            "       NULL                                      AS mother_first_name,\n" +
            "       NULL                                      AS mother_last_name,\n" +
            "       NULL                                      AS mother_middle_name\n" +
            "FROM ec_pregnancy_outcome\n" +
            "         inner join ec_family_member on ec_family_member.base_entity_id = ec_pregnancy_outcome.base_entity_id\n" +
            "         inner join ec_family on ec_family.base_entity_id = ec_family_member.relational_id\n" +
            "where ec_family_member.date_removed is null\n" +
            "  and ec_pregnancy_outcome.is_closed is 0\n" +
            "  and ec_pregnancy_outcome.delivery_date is not null\n" +
            "  AND ec_pregnancy_outcome.base_entity_id NOT IN\n" +
            "      (SELECT base_entity_id FROM ec_anc_register WHERE ec_anc_register.is_closed IS 0)\n" +
            "  AND ec_pregnancy_outcome.base_entity_id IN (%s)\n" +
            "\n" +
            "UNION ALL\n" +
            "/* CHILD REGISTER */\n" +
            "\n" +
            "SELECT ec_family_member.first_name     AS first_name,\n" +
            "       ec_family_member.middle_name    AS middle_name,\n" +
            "       ec_family_member.last_name      AS last_name,\n" +
            "       ec_family_member.gender         AS gender,\n" +
            "       ec_family_member.dob            AS dob,\n" +
            "       ec_family_member.base_entity_id AS base_entity_id,\n" +
            "       ec_family_member.id             as _id,\n" +
            "       'Child'                         AS register_type,\n" +
            "       ec_family_member.relational_id  as relationalid,\n" +
            "       ec_family.village_town          as home_address,\n" +
            "       ec_child.last_interacted_with   AS last_interacted_with,\n" +
            "       ec_child.mother_first_name      AS mother_first_name,\n" +
            "       ec_child.mother_middle_name     AS mother_middle_name,\n" +
            "       ec_child.mother_last_name       AS mother_last_name\n" +
            "FROM (SELECT ec_child.*,\n" +
            "             mother.first_name  AS mother_first_name,\n" +
            "             mother.last_name   AS mother_last_name,\n" +
            "             mother.middle_name AS mother_middle_name\n" +
            "      FROM ec_child\n" +
            "               inner join ec_family on ec_family.base_entity_id = ec_child.relational_id\n" +
            "               INNER JOIN ec_family_member AS mother ON ec_family.primary_caregiver = mother.base_entity_id\n" +
            "     ) ec_child\n" +
            "         inner join ec_family_member on ec_family_member.base_entity_id = ec_child.base_entity_id\n" +
            "         inner join ec_family on ec_family.base_entity_id = ec_family_member.relational_id\n" +
            "WHERE ec_family_member.is_closed = '0'\n" +
            "  AND ec_family_member.date_removed is null\n" +
            "  AND cast(strftime('%Y-%m-%d %H:%M:%S', 'now') - strftime('%Y-%m-%d %H:%M:%S', ec_child.dob) as int) > 0\n" +
            "  AND ec_child.base_entity_id IN (%s)\n" +
            "\n" +
            "UNION ALL\n" +
            "/* TB REGISTER */\n" +
            "\n" +
            "SELECT ec_family_member.first_name               AS first_name,\n" +
            "       ec_family_member.middle_name              AS middle_name,\n" +
            "       ec_family_member.last_name                AS last_name,\n" +
            "       ec_family_member.gender                   AS gender,\n" +
            "       ec_family_member.dob                      AS dob,\n" +
            "       ec_family_member.base_entity_id           AS base_entity_id,\n" +
            "       ec_family_member.id                       as _id,\n" +
            "       'TB'                                      AS register_type,\n" +
            "       ec_family_member.relational_id            as relationalid,\n" +
            "       ec_family.village_town                    as home_address,\n" +
            "       ec_tb_register.last_interacted_with       AS last_interacted_with,\n" +
            "       NULL                                      AS mother_first_name,\n" +
            "       NULL                                      AS mother_last_name,\n" +
            "       NULL                                      AS mother_middle_name\n" +
            "FROM ec_tb_register\n" +
            "         inner join ec_family_member on ec_family_member.base_entity_id = ec_tb_register.base_entity_id\n" +
            "         inner join ec_family on ec_family.base_entity_id = ec_family_member.relational_id\n" +
            "where ec_family_member.date_removed is null\n" +
            "  and ec_tb_register.is_closed is 0\n" +
            "  and ec_tb_register.tb_case_closure_date is null\n" +
            "  AND ec_tb_register.base_entity_id IN (%s)\n" +
            "  AND ec_family_member.base_entity_id NOT IN (\n" +
            "    SELECT ec_agyw_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_agyw_register where ec_agyw_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_kvp_prep_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_kvp_prep_register where ec_kvp_prep_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_anc_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_anc_register\n" +
            "    UNION ALL\n" +
            "    SELECT ec_pregnancy_outcome.base_entity_id AS base_entity_id\n" +
            "    FROM ec_pregnancy_outcome\n" +
            "    UNION ALL\n" +
            "    SELECT ec_child.base_entity_id AS base_entity_id\n" +
            "    FROM ec_child\n" +
            ")\n" +
            "\n" +
            "UNION ALL\n" +
            "/*ONLY MALARIA PATIENTS*/\n" +
            "SELECT ec_family_member.first_name,\n" +
            "       ec_family_member.middle_name,\n" +
            "       ec_family_member.last_name,\n" +
            "       ec_family_member.gender,\n" +
            "       ec_family_member.dob,\n" +
            "       ec_family_member.base_entity_id,\n" +
            "       ec_family_member.id                          as _id,\n" +
            "       'Malaria'                                    AS register_type,\n" +
            "       ec_family_member.relational_id               as relationalid,\n" +
            "       ec_family.village_town                       as home_address,\n" +
            "       NULL                                         AS mother_first_name,\n" +
            "       NULL                                         AS mother_last_name,\n" +
            "       NULL                                         AS mother_middle_name,\n" +
            "       ec_malaria_confirmation.last_interacted_with AS last_interacted_with\n" +
            "FROM ec_family_member\n" +
            "         inner join ec_family on ec_family.base_entity_id = ec_family_member.relational_id\n" +
            "         inner join ec_malaria_confirmation\n" +
            "                    on ec_family_member.base_entity_id = ec_malaria_confirmation.base_entity_id\n" +
            "where ec_family_member.date_removed is null\n" +
            "  AND datetime('NOW') <= datetime(ec_malaria_confirmation.last_interacted_with/1000, 'unixepoch', 'localtime','+15 days')\n" +
            "  AND ec_malaria_confirmation.is_closed = 0 AND ec_malaria_confirmation.malaria = 1 \n" +
            "  AND ec_family_member.base_entity_id IN (%s)\n" +
            "  AND ec_family_member.base_entity_id NOT IN (\n" +
            "    SELECT ec_agyw_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_agyw_register where ec_agyw_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_kvp_prep_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_kvp_prep_register where ec_kvp_prep_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_anc_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_anc_register\n" +
            "    UNION ALL\n" +
            "    SELECT ec_pregnancy_outcome.base_entity_id AS base_entity_id\n" +
            "    FROM ec_pregnancy_outcome\n" +
            "    UNION ALL\n" +
            "    SELECT ec_child.base_entity_id AS base_entity_id\n" +
            "    FROM ec_child\n" +
            "    UNION ALL\n" +
            "    SELECT ec_family_planning.base_entity_id AS base_entity_id\n" +
            "    FROM ec_family_planning\n" +
            "    UNION ALL\n" +
            "    SELECT ec_tb_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_tb_register\n" +
            "    WHERE ec_tb_register.tb_case_closure_date is null\n" +
            "    UNION ALL\n" +
            "    SELECT ec_cbhs_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_cbhs_register\n" +
            ")\n" +
            "UNION ALL\n" +
            "/*ONLY ICCM CLIENTS*/\n" +
            "SELECT ec_family_member.first_name,\n" +
            "       ec_family_member.middle_name,\n" +
            "       ec_family_member.last_name,\n" +
            "       ec_family_member.gender,\n" +
            "       ec_family_member.dob,\n" +
            "       ec_iccm_enrollment.base_entity_id,\n" +
            "       ec_family_member.id                          as _id,\n" +
            "       'iCCM'                                    AS register_type,\n" +
            "       ec_family_member.relational_id               as relationalid,\n" +
            "       ec_family.village_town                       as home_address,\n" +
            "       NULL                                         AS mother_first_name,\n" +
            "       NULL                                         AS mother_last_name,\n" +
            "       NULL                                         AS mother_middle_name,\n" +
            "       ec_iccm_enrollment.last_interacted_with AS last_interacted_with\n" +
            "FROM ec_family_member\n" +
            "         inner join ec_family on ec_family.base_entity_id = ec_family_member.relational_id\n" +
            "         inner join ec_iccm_enrollment\n" +
            "                    on ec_family_member.base_entity_id = ec_iccm_enrollment.entity_id\n" +
            "where ec_family_member.date_removed is null\n" +
            "  AND date('now') <= date(strftime('%Y-%m-%d', ec_iccm_enrollment.last_interacted_with / 1000, 'unixepoch', 'localtime'))\n" +
            "  AND ec_iccm_enrollment.is_closed = 0 \n" +
            "  AND ec_family_member.base_entity_id IN (%s)\n" +
            "  AND ec_family_member.base_entity_id NOT IN (\n" +
            "    SELECT ec_agyw_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_agyw_register where ec_agyw_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_kvp_prep_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_kvp_prep_register where ec_kvp_prep_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_anc_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_anc_register\n" +
            "    UNION ALL\n" +
            "    SELECT ec_pregnancy_outcome.base_entity_id AS base_entity_id\n" +
            "    FROM ec_pregnancy_outcome\n" +
            "    UNION ALL\n" +
            "    SELECT ec_child.base_entity_id AS base_entity_id\n" +
            "    FROM ec_child\n" +
            "    UNION ALL\n" +
            "    SELECT ec_family_planning.base_entity_id AS base_entity_id\n" +
            "    FROM ec_family_planning\n" +
            "    UNION ALL\n" +
            "    SELECT ec_malaria_confirmation.base_entity_id AS base_entity_id\n" +
            "    FROM ec_malaria_confirmation\n" +
            "    WHERE datetime('NOW') <= datetime(ec_malaria_confirmation.last_interacted_with/1000, 'unixepoch', 'localtime','+15 days')\n" +
            "    AND ec_malaria_confirmation.is_closed = 0 AND ec_malaria_confirmation.malaria = 1 \n" +
            "    UNION ALL\n" +
            "    SELECT ec_tb_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_tb_register\n" +
            "    WHERE ec_tb_register.tb_case_closure_date is null\n" +
            "    UNION ALL\n" +
            "    SELECT ec_cbhs_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_cbhs_register\n" +
            ")\n" +
            "UNION ALL\n" +
            "\n" +
            "/*ONLY FAMILY PLANNING PATIENTS*/\n" +
            "SELECT ec_family_member.first_name,\n" +
            "       ec_family_member.middle_name,\n" +
            "       ec_family_member.last_name,\n" +
            "       ec_family_member.gender,\n" +
            "       ec_family_member.dob,\n" +
            "       ec_family_member.base_entity_id,\n" +
            "       ec_family_member.id                          as _id,\n" +
            "       'Family Planning'                             AS register_type,\n" +
            "       ec_family_member.relational_id               as relationalid,\n" +
            "       ec_family.village_town                       as home_address,\n" +
            "       NULL                                         AS mother_first_name,\n" +
            "       NULL                                         AS mother_last_name,\n" +
            "       NULL                                         AS mother_middle_name,\n" +
            "       ec_family_planning.last_interacted_with AS last_interacted_with\n" +
            "FROM ec_family_member\n" +
            "         inner join ec_family on ec_family.base_entity_id = ec_family_member.relational_id\n" +
            "         inner join ec_family_planning\n" +
            "                    on ec_family_member.base_entity_id = ec_family_planning.base_entity_id\n" +
            "where ec_family_member.date_removed is null\n" +
            "  AND ec_family_planning.is_closed is 0\n" +
            "  AND ec_family_member.base_entity_id IN (%s)\n" +
            "  AND ec_family_member.base_entity_id NOT IN (\n" +
            "    SELECT ec_agyw_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_agyw_register where ec_agyw_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_kvp_prep_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_kvp_prep_register where ec_kvp_prep_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_anc_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_anc_register\n" +
            "    UNION ALL\n" +
            "    SELECT ec_pregnancy_outcome.base_entity_id AS base_entity_id\n" +
            "    FROM ec_pregnancy_outcome\n" +
            "    UNION ALL\n" +
            "    SELECT ec_child.base_entity_id AS base_entity_id\n" +
            "    FROM ec_child\n" +
            "    UNION ALL\n" +
            "    SELECT ec_malaria_confirmation.base_entity_id AS base_entity_id\n" +
            "    FROM ec_malaria_confirmation\n" +
            "    UNION ALL\n" +
            "    SELECT ec_tb_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_tb_register\n" +
            "    WHERE ec_tb_register.tb_case_closure_date is null\n" +
            "    UNION ALL\n" +
            "    SELECT ec_hiv_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_hiv_register)\n" +
            "UNION ALL\n" +
            "\n" +
            "/*ONLY AGYW clients*/\n" +
            "SELECT ec_family_member.first_name,\n" +
            "       ec_family_member.middle_name,\n" +
            "       ec_family_member.last_name,\n" +
            "       ec_family_member.gender,\n" +
            "       ec_family_member.dob,\n" +
            "       ec_family_member.base_entity_id,\n" +
            "       ec_family_member.id                          as _id,\n" +
            "       'AGYW'                             AS register_type,\n" +
            "       ec_family_member.relational_id               as relationalid,\n" +
            "       ec_family.village_town                       as home_address,\n" +
            "       NULL                                         AS mother_first_name,\n" +
            "       NULL                                         AS mother_last_name,\n" +
            "       NULL                                         AS mother_middle_name,\n" +
            "       ec_agyw_register.last_interacted_with AS last_interacted_with\n" +
            "FROM ec_family_member\n" +
            "         inner join ec_family on ec_family.base_entity_id = ec_family_member.relational_id\n" +
            "         inner join ec_agyw_register\n" +
            "                    on ec_family_member.base_entity_id = ec_agyw_register.base_entity_id\n" +
            "where ec_family_member.date_removed is null\n" +
            "  AND ec_agyw_register.is_closed is 0\n" +
            "  AND ec_family_member.base_entity_id IN (%s)\n" +
            "  AND ec_family_member.base_entity_id NOT IN (\n" +
            "    SELECT ec_kvp_prep_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_kvp_prep_register where ec_kvp_prep_register.is_closed is 0\n" +
            "    UNION ALL\n" +
            "    SELECT ec_anc_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_anc_register\n" +
            "    UNION ALL\n" +
            "    SELECT ec_pregnancy_outcome.base_entity_id AS base_entity_id\n" +
            "    FROM ec_pregnancy_outcome\n" +
            "    UNION ALL\n" +
            "    SELECT ec_child.base_entity_id AS base_entity_id\n" +
            "    FROM ec_child\n" +
            "    UNION ALL\n" +
            "    SELECT ec_malaria_confirmation.base_entity_id AS base_entity_id\n" +
            "    FROM ec_malaria_confirmation\n" +
            "    UNION ALL\n" +
            "    SELECT ec_tb_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_tb_register\n" +
            "    WHERE ec_tb_register.tb_case_closure_date is null\n" +
            "    UNION ALL\n" +
            "    SELECT ec_cbhs_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_cbhs_register)\n" +
            "UNION ALL\n" +
            "\n" +
            "/*ONLY KVP/PrEP clients*/\n" +
            "SELECT ec_family_member.first_name,\n" +
            "       ec_family_member.middle_name,\n" +
            "       ec_family_member.last_name,\n" +
            "       ec_family_member.gender,\n" +
            "       ec_family_member.dob,\n" +
            "       ec_family_member.base_entity_id,\n" +
            "       ec_family_member.id                          as _id,\n" +
            "       'KVP/PrEP'                             AS register_type,\n" +
            "       ec_family_member.relational_id               as relationalid,\n" +
            "       ec_family.village_town                       as home_address,\n" +
            "       NULL                                         AS mother_first_name,\n" +
            "       NULL                                         AS mother_last_name,\n" +
            "       NULL                                         AS mother_middle_name,\n" +
            "       ec_kvp_prep_register.last_interacted_with AS last_interacted_with\n" +
            "FROM ec_family_member\n" +
            "         inner join ec_family on ec_family.base_entity_id = ec_family_member.relational_id\n" +
            "         inner join ec_kvp_prep_register\n" +
            "                    on ec_family_member.base_entity_id = ec_kvp_prep_register.base_entity_id\n" +
            "where ec_family_member.date_removed is null\n" +
            "  AND ec_kvp_prep_register.is_closed is 0\n" +
            "  AND ec_family_member.base_entity_id IN (%s)\n" +
            "  AND ec_family_member.base_entity_id NOT IN (\n" +
            "    SELECT ec_anc_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_anc_register\n" +
            "    UNION ALL\n" +
            "    SELECT ec_pregnancy_outcome.base_entity_id AS base_entity_id\n" +
            "    FROM ec_pregnancy_outcome\n" +
            "    UNION ALL\n" +
            "    SELECT ec_child.base_entity_id AS base_entity_id\n" +
            "    FROM ec_child\n" +
            "    UNION ALL\n" +
            "    SELECT ec_malaria_confirmation.base_entity_id AS base_entity_id\n" +
            "    FROM ec_malaria_confirmation\n" +
            "    UNION ALL\n" +
            "    SELECT ec_tb_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_tb_register\n" +
            "    WHERE ec_tb_register.tb_case_closure_date is null\n" +
            "    UNION ALL\n" +
            "    SELECT ec_cbhs_register.base_entity_id AS base_entity_id\n" +
            "    FROM ec_cbhs_register)\n" +
            ")\n" +
            "ORDER BY last_interacted_with DESC;";
}
