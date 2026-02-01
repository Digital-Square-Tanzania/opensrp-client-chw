package org.smartregister.chw.repository;

import android.content.Context;
import android.database.Cursor;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.format.DateTimeFormat;
import org.json.JSONObject;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.repository.VisitDetailsRepository;
import org.smartregister.chw.anc.repository.VisitRepository;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.repository.ScheduleRepository;
import org.smartregister.chw.core.rule.PNCHealthFacilityVisitRule;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.ChwPNCDao;
import org.smartregister.chw.dao.WashCheckDao;
import org.smartregister.chw.domain.PNCHealthFacilityVisitSummary;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.PNCVisitUtil;
import org.smartregister.chw.util.RepositoryUtils;
import org.smartregister.chw.util.RepositoryUtilsFlv;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.domain.db.Column;
import org.smartregister.domain.Event;
import org.smartregister.domain.db.EventClient;
import org.smartregister.family.util.DBConstants;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.immunization.repository.RecurringServiceTypeRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.IMDatabaseUtils;
import org.smartregister.reporting.ReportingLibrary;
import org.smartregister.repository.AlertRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.util.DatabaseMigrationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
                case 7:
                    upgradeToVersion7(db);
                    break;
                case 8:
                    upgradeToVersion8(db);
                    break;
                case 10:
                    upgradeToVersion10(db);
                    break;
                case 11:
                    upgradeToVersion11(db);
                    break;
                case 12:
                    upgradeToVersion12(db, oldVersion);
                    break;
                case 13:
                    upgradeToVersion13(db);
                    break;
                case 14:
                    upgradeToVersion14(db);
                    break;
                case 15:
                    upgradeToVersion15(db);
                    break;
                case 16:
                    upgradeToVersion16(db);
                    break;
                case 17:
                    upgradeToVersion17(db);
                    break;
                default:
                    break;
            }
            upgradeTo++;
        }
    }

    private static void upgradeToVersion2(Context context, SQLiteDatabase db) {
        try {
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_EVENT_ID_COL);
            db.execSQL(VaccineRepository.EVENT_ID_INDEX);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_FORMSUBMISSION_ID_COL);
            db.execSQL(VaccineRepository.FORMSUBMISSION_INDEX);

            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL_INDEX);

//            EventClientRepository.createTable(db, EventClientRepository.Table.path_reports, EventClientRepository.report_column.values());
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_HIA2_STATUS_COL);

            IMDatabaseUtils.accessAssetsAndFillDataBaseForVaccineTypes(context, db);

        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion2 ");
        }

    }

    private static void upgradeToVersion3(SQLiteDatabase db) {
        try {
            Column[] columns = {EventClientRepository.event_column.formSubmissionId};
            EventClientRepository.createIndex(db, EventClientRepository.Table.event, columns);

            db.execSQL(VaccineRepository.ALTER_ADD_CREATED_AT_COLUMN);
            VaccineRepository.migrateCreatedAt(db);

            db.execSQL(RecurringServiceRecordRepository.ALTER_ADD_CREATED_AT_COLUMN);
            RecurringServiceRecordRepository.migrateCreatedAt(db);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion3 ");
        }
        try {
            Column[] columns = {EventClientRepository.event_column.formSubmissionId};
            EventClientRepository.createIndex(db, EventClientRepository.Table.event, columns);


        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion3 " + e.getMessage());
        }
    }

    private static void upgradeToVersion4(SQLiteDatabase db) {
        try {
            db.execSQL(AlertRepository.ALTER_ADD_OFFLINE_COLUMN);
            db.execSQL(AlertRepository.OFFLINE_INDEX);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_TEAM_COL);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_TEAM_ID_COL);
            db.execSQL(RecurringServiceRecordRepository.UPDATE_TABLE_ADD_TEAM_COL);
            db.execSQL(RecurringServiceRecordRepository.UPDATE_TABLE_ADD_TEAM_ID_COL);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion4 ");
        }

    }

    private static void upgradeToVersion5(SQLiteDatabase db) {
        try {
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_CHILD_LOCATION_ID_COL);
            db.execSQL(RecurringServiceRecordRepository.UPDATE_TABLE_ADD_CHILD_LOCATION_ID_COL);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion5 ");
        }
    }

    private static void upgradeToVersion7(SQLiteDatabase db) {
        try {
            //TODO Child Refactor
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion7 ");
        }
    }

    private static void upgradeToVersion8(SQLiteDatabase db) {
        try {
            //db.execSQL(HomeVisitRepository.UPDATE_TABLE_ADD_HOME_VISIT_ID);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion8 ");
        }
    }

    private static void upgradeToVersion10(SQLiteDatabase db) {
        try {
            for (String query : RepositoryUtilsFlv.DROP_VISITS_INFO_TABLES) {
                db.execSQL(query);
            }

            // recreate tables
            VisitRepository.createTable(db);
            VisitDetailsRepository.createTable(db);

            //reprocess all the ANC visit events
            List<Event> events = getEvents(db, new String[]{Constants.EventType.ANC_HOME_VISIT});
            for (Event event : events) {
                NCUtils.processHomeVisit(new EventClient(event), db);
            }

            // update recurring services
            db.execSQL(RecurringServiceTypeRepository.ADD_SERVICE_GROUP_COLUMN);
            // merge service records

            for (String query : RepositoryUtils.UPDATE_REPOSITORY_TYPES) {
                db.execSQL(query);
            }

            // add missing columns to the DB
            List<String> columns = new ArrayList<>();
            columns.add(ChildDBConstants.KEY.ENTRY_POINT);
            DatabaseMigrationUtils.addFieldsToFTSTable(db, CoreChwApplication.createCommonFtsObject(), CoreConstants.TABLE_NAME.CHILD, columns);

        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion10 ");
        }
    }

    private static void upgradeToVersion11(SQLiteDatabase db) {
        try {
            // add all the wash check tasks to the visit table // will assist the event
            List<String> wash_visits = WashCheckDao.getAllWashCheckVisits(db);
            for (String visit_id : wash_visits) {
                db.execSQL("delete from visits where visit_id = '" + visit_id + "'");
                db.execSQL("delete from visit_details where visit_id = '" + visit_id + "'");
            }

            // reprocess all wash check events
            List<EventClient> eventClients = WashCheckDao.getWashCheckEvents(db);
            for (EventClient eventClient : eventClients) {
                if (eventClient == null) continue;

                NCUtils.processHomeVisit(eventClient); // save locally
            }

            // add missing columns to the DB
            List<String> columns = new ArrayList<>();
            columns.add(ChildDBConstants.KEY.RELATIONAL_ID);
            DatabaseMigrationUtils.addFieldsToFTSTable(db, CoreChwApplication.createCommonFtsObject(), CoreConstants.TABLE_NAME.FAMILY_MEMBER, columns);

            // add missing columns
            List<String> child_columns = new ArrayList<>();
            child_columns.add(DBConstants.KEY.DOB);
            child_columns.add(DBConstants.KEY.DATE_REMOVED);
            DatabaseMigrationUtils.addFieldsToFTSTable(db, CoreChwApplication.createCommonFtsObject(), CoreConstants.TABLE_NAME.CHILD, child_columns);

        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private static void upgradeToVersion12(SQLiteDatabase db, int oldDbVersion) {
        ReportingLibrary reportingLibraryInstance = ReportingLibrary.getInstance();
        if (oldDbVersion == 11) {
            db.execSQL(RepositoryUtilsFlv.addLbwColumnQuery);
            reportingLibraryInstance.truncateIndicatorDefinitionTables(db);
        }
        initializeIndicatorDefinitions(reportingLibraryInstance, db);
    }

    private static void upgradeToVersion13(SQLiteDatabase sqLiteDatabase) {
        try {
            List<Event> events = getEvents(sqLiteDatabase, new String[]{Constants.EventType.PREGNANCY_OUTCOME});
            processHFNextVisitDateObs(events, sqLiteDatabase);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private static void upgradeToVersion14(SQLiteDatabase db) {
        try {
            // delete possible duplication
            db.execSQL(RepositoryUtils.DELETE_DUPLICATE_SCHEDULES);
            db.execSQL(ScheduleRepository.USER_UNIQUE_INDEX);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion2 ");
        }
    }

    private static void initializeIndicatorDefinitions(ReportingLibrary reportingLibrary, SQLiteDatabase sqLiteDatabase) {
        String childIndicatorsConfigFile = "config/child-reporting-indicator-definitions.yml";
        String ancIndicatorConfigFile = "config/anc-reporting-indicator-definitions.yml";
        String pncIndicatorConfigFile = "config/pnc-reporting-indicator-definitions.yml";
        for (String configFile : Collections.unmodifiableList(
                Arrays.asList(childIndicatorsConfigFile, ancIndicatorConfigFile, pncIndicatorConfigFile))) {
            reportingLibrary.readConfigFile(configFile, sqLiteDatabase);
        }
    }

    // helpers
    private static List<Event> getEvents(SQLiteDatabase db, String[] selectionArgs) {
        List<Event> events = new ArrayList<>();
        Cursor cursor = null;
        try {
            String[] myStringArray = {"json"};
            cursor = db.query(EventClientRepository.Table.event.name(), myStringArray, " eventType = ? ", selectionArgs, null, null, " eventDate ASC ", null);
            events = readEvents(cursor);
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return events;
    }

    private static List<Event> readEvents(Cursor cursor) {
        List<Event> events = new ArrayList<>();
        ECSyncHelper syncHelper = ChwApplication.getInstance().getEcSyncHelper();
        try {
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    String json = cursor.getString(cursor.getColumnIndex("json"));
                    Event event = syncHelper.convert(new JSONObject(json), Event.class);
                    events.add(event);
                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            cursor.close();
        }
        return events;
    }

    private static void processHFNextVisitDateObs(List<Event> events, SQLiteDatabase sqLiteDatabase) {
        // Save missing PNC Health Facility visit (next visit) details
        for (Event event : events) {
            Visit visit = AncLibrary.getInstance().visitRepository().getLatestVisit(event.getBaseEntityId(), Constants.EventType.PNC_HOME_VISIT, sqLiteDatabase);
            Obs obs = getPncHfNextVisitObs(event.getBaseEntityId(), sqLiteDatabase);

            if (visit != null && obs != null) {
                Map<String, List<VisitDetail>> visitDetails = null;
                List<Obs> obsList = new ArrayList<>();
                obsList.add(obs);
                try {
                    visitDetails = NCUtils.eventsObsToDetails(obsList, visit.getVisitId(), null);
                } catch (Exception ex) {
                    Timber.e(ex);
                }

                if (visitDetails != null && !visitDetails.isEmpty()) {
                    VisitDetail detail = visitDetails.get(Constants.FORM_SUBMISSION_FIELD.pncHfNextVisitDateFieldType).get(0);
                    if (sqLiteDatabase != null) {
                        AncLibrary.getInstance().visitDetailsRepository().addVisitDetails(detail, sqLiteDatabase);
                    } else {
                        AncLibrary.getInstance().visitDetailsRepository().addVisitDetails(detail);
                    }
                }
            }

        }

    }

    private static Obs getPncHfNextVisitObs(String baseEntityId, SQLiteDatabase sqLiteDatabase) {
        Obs pncHfNextVisitDateObs = null;
        PNCHealthFacilityVisitSummary summary = ChwPNCDao.getLastHealthFacilityVisitSummary(baseEntityId, sqLiteDatabase);
        if (summary != null) {
            PNCHealthFacilityVisitRule visitRule = PNCVisitUtil.getNextPNCHealthFacilityVisit(summary.getDeliveryDate(), summary.getLastVisitDate());
            String pncHfNextVisitDate = DateTimeFormat.forPattern("dd-MM-yyyy").print(visitRule.getDueDate());
            pncHfNextVisitDateObs = new Obs().withFormSubmissionField(Constants.FORM_SUBMISSION_FIELD.pncHfNextVisitDateFieldType)
                    .withFieldDataType("spacer")
                    .withValue(pncHfNextVisitDate)
                    .withFieldCode(Constants.FORM_SUBMISSION_FIELD.pncHfNextVisitDateFieldType)
                    .withFieldType("formsubmissionField")
                    .withParentCode("")
                    .withHumanReadableValues(new ArrayList<>());
        }

        return pncHfNextVisitDateObs;
    }

    private static void upgradeToVersion15(SQLiteDatabase db) {
        try {
            // delete possible duplication
            db.execSQL(RepositoryUtils.ADD_MISSING_REPORTING_COLUMN);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private static void upgradeToVersion16(SQLiteDatabase db) {
        try {
           RepositoryUtils.addDetailsColumnToFamilySearchTable(db);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private static void upgradeToVersion17(SQLiteDatabase db) {
        try {
            db.execSQL(VisitRepository.ADD_VISIT_GROUP_COLUMN);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion17");
        }
    }
}
