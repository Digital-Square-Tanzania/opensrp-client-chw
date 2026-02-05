package org.smartregister.chw.repository;

import android.content.Context;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.smartregister.chw.anc.repository.VisitRepository;
import org.smartregister.chw.util.DatabaseMigrationUtils;
import org.smartregister.chw.util.RepositoryUtils;
import org.smartregister.domain.db.Column;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.IMDatabaseUtils;
import org.smartregister.reporting.ReportingLibrary;
import org.smartregister.repository.AlertRepository;
import org.smartregister.repository.EventClientRepository;

import java.util.Arrays;
import java.util.Collections;

import timber.log.Timber;

public class ChwRepositoryFlv {

    public static void onUpgrade(Context context, SQLiteDatabase db, int oldVersion, int newVersion) {
        Timber.w(ChwRepository.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        int upgradeTo = oldVersion + 1;
        while (upgradeTo <= newVersion) {
            switch (upgradeTo) {
                case 2:
                    upgradeToVersion2(context, db);
                    break;
                case 3:
                    upgradeToVersion3(db);
                    break;
                case 4:
                    upgradeToVersion4(db);
                    break;
                case 5:
                    upgradeToVersion5(db);
                    break;
                case 6:
                    upgradeToVersion6(db);
                    break;
                default:
                    break;
            }
            upgradeTo++;
        }
    }

    private static void upgradeToVersion2(Context context, SQLiteDatabase db) {
        try {
            // nuke the database and recreate everything

            // add missing vaccine columns
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_EVENT_ID_COL);
            db.execSQL(VaccineRepository.EVENT_ID_INDEX);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_FORMSUBMISSION_ID_COL);
            db.execSQL(VaccineRepository.FORMSUBMISSION_INDEX);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL_INDEX);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_HIA2_STATUS_COL);
            IMDatabaseUtils.accessAssetsAndFillDataBaseForVaccineTypes(context, db);

            // add missing event repository table
            Column[] columns = {EventClientRepository.event_column.formSubmissionId};
            EventClientRepository.createIndex(db, EventClientRepository.Table.event, columns);

            db.execSQL(VaccineRepository.ALTER_ADD_CREATED_AT_COLUMN);
            VaccineRepository.migrateCreatedAt(db);

            db.execSQL(RecurringServiceRecordRepository.ALTER_ADD_CREATED_AT_COLUMN);
            RecurringServiceRecordRepository.migrateCreatedAt(db);

            // add missing alert table info
            db.execSQL(AlertRepository.ALTER_ADD_OFFLINE_COLUMN);
            db.execSQL(AlertRepository.OFFLINE_INDEX);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_TEAM_COL);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_TEAM_ID_COL);
            db.execSQL(RecurringServiceRecordRepository.UPDATE_TABLE_ADD_TEAM_COL);
            db.execSQL(RecurringServiceRecordRepository.UPDATE_TABLE_ADD_TEAM_ID_COL);

            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_CHILD_LOCATION_ID_COL);
            db.execSQL(RecurringServiceRecordRepository.UPDATE_TABLE_ADD_CHILD_LOCATION_ID_COL);

            // setup reporting
            ReportingLibrary reportingLibrary = ReportingLibrary.getInstance();
            String childIndicatorsConfigFile = "config/child-reporting-indicator-definitions.yml";
            String ancIndicatorConfigFile = "config/anc-reporting-indicator-definitions.yml";
            String pncIndicatorConfigFile = "config/pnc-reporting-indicator-definitions.yml";
            for (String configFile : Collections.unmodifiableList(
                    Arrays.asList(childIndicatorsConfigFile, ancIndicatorConfigFile, pncIndicatorConfigFile))) {
                reportingLibrary.readConfigFile(configFile, db);
            }

        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion2 ");
        }
    }

    private static void upgradeToVersion3(SQLiteDatabase db) {
        try {
            RepositoryUtils.addDetailsColumnToFamilySearchTable(db);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion3");
        }
    }

    private static void upgradeToVersion4(SQLiteDatabase db) {
        try {
            EventClientRepository.createTable(db, EventClientRepository.Table.foreignEvent, EventClientRepository.event_column.values());
            EventClientRepository.createTable(db, EventClientRepository.Table.foreignClient, EventClientRepository.client_column.values());
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion4");
        }
    }

    private static void upgradeToVersion5(SQLiteDatabase db) {
        try {
            db.execSQL(VisitRepository.ADD_VISIT_GROUP_COLUMN);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion5");
        }
    }

    private static void upgradeToVersion6(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE ec_family_member_location ADD COLUMN provider_id VARCHAR;");
            DatabaseMigrationUtils.fillFamilyMemberLocationTableWithProviderIds(db);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion6");
        }
    }
}