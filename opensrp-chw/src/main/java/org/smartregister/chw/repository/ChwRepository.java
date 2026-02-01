package org.smartregister.chw.repository;

import android.content.Context;
import android.database.sqlite.SQLiteOutOfMemoryException;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteStatement;

import org.smartregister.AllConstants;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.repository.CoreChwRepository;
import org.smartregister.exception.DatabaseMigrationException;
import org.smartregister.reporting.ReportingLibrary;

import java.util.Arrays;
import java.util.Collections;

import timber.log.Timber;

public class ChwRepository extends CoreChwRepository {
    private Context context;
    private static String appVersionCodePref = "APP_VERSION_CODE";

    private enum MigrationResult {
        SUCCESS,
        OOM_SKIPPED,
        FAILED
    }

    public ChwRepository(Context context, org.smartregister.Context openSRPContext) {
        super(context, AllConstants.DATABASE_NAME, BuildConfig.DATABASE_VERSION, openSRPContext.session(), CoreChwApplication.createCommonFtsObject(), openSRPContext.sharedRepositoriesArray());
        this.context = context;
    }

    private static boolean checkIfAppUpdated() {
        String savedAppVersion = ReportingLibrary.getInstance().getContext().allSharedPreferences().getPreference(appVersionCodePref);
        if (savedAppVersion.isEmpty()) {
            return true;
        } else {
            int savedVersion = Integer.parseInt(savedAppVersion);
            return (org.smartregister.chw.core.BuildConfig.VERSION_CODE > savedVersion);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Timber.w(ChwRepository.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        int upgradeTo = oldVersion + 1;
        while (upgradeTo <= newVersion) {
            switch (upgradeTo) {
                case 2:
                    upgradeToVersion2(db);
                    break;
                default:
                    break;
            }
            upgradeTo++;
        }
        ChwRepositoryFlv.onUpgrade(context, db, oldVersion, newVersion);
    }

    @Override
    public void onOpen(SQLiteDatabase database) {
        if (!CoreLibrary.getInstance().context().allSharedPreferences().isMigratedToSqlite4()) {
            MigrationResult result = performCipherMigrationToV4Safely(database);
            if (result == MigrationResult.FAILED) {
                throw new DatabaseMigrationException("Database migration to SQLiteCipher v4 was not successful");
            }
            CoreLibrary.getInstance().context().allSharedPreferences().setMigratedToSqlite4();
            if (result == MigrationResult.OOM_SKIPPED) {
                Timber.w("SQLiteCipher migration skipped due to OOM; consider clearing app data if issues persist");
            } else {
                Timber.i("Database migration to Cipher 4 complete");
            }
        } else {
            Timber.i("SQLiteCipher database is already v4");
        }

        database.execSQL("PRAGMA cipher_memory_security = OFF;");
        database.rawExecSQL("PRAGMA journal_mode = TRUNCATE;");
    }

    private MigrationResult performCipherMigrationToV4Safely(SQLiteDatabase database) {
        SQLiteStatement statement = null;
        try {
            try {
                database.execSQL("PRAGMA cipher_memory_security = OFF;");
            } catch (Exception e) {
                Timber.w(e, "Unable to disable cipher memory security before migration");
            }
            statement = database.compileStatement("PRAGMA cipher_migrate");
            long result = statement.simpleQueryForLong();
            return "0".equals(String.valueOf(result)) ? MigrationResult.SUCCESS : MigrationResult.FAILED;
        } catch (SQLiteOutOfMemoryException oom) {
            Timber.e(oom, "SQLiteCipher migration OOM; skipping migration");
            return MigrationResult.OOM_SKIPPED;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("out of memory")) {
                Timber.e(e, "SQLiteCipher migration OOM; skipping migration");
                return MigrationResult.OOM_SKIPPED;
            }
            Timber.e(e);
            return MigrationResult.FAILED;
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    private static void upgradeToVersion2(SQLiteDatabase db) {
        try {
            ReportingLibrary reportingLibraryInstance = ReportingLibrary.getInstance();
            String indicatorDataInitialisedPref = "INDICATOR_DATA_INITIALISED";

            boolean indicatorDataInitialised = Boolean.parseBoolean(reportingLibraryInstance.getContext().allSharedPreferences().getPreference(indicatorDataInitialisedPref));
            boolean isUpdated = checkIfAppUpdated();
            if (!indicatorDataInitialised || isUpdated) {

                String indicatorsConfigFile = "config/indicator-definitions.yml";

                for (String configFile : Collections.unmodifiableList(
                        Collections.singletonList(indicatorsConfigFile))) {
                    reportingLibraryInstance.readConfigFile(configFile, db);
                }

                reportingLibraryInstance.initIndicatorData(indicatorsConfigFile, db); // This will persist the data in the DB
                reportingLibraryInstance.getContext().allSharedPreferences().savePreference(indicatorDataInitialisedPref, "true");
                reportingLibraryInstance.getContext().allSharedPreferences().savePreference(appVersionCodePref, String.valueOf(org.smartregister.chw.core.BuildConfig.VERSION_CODE));
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }
}
