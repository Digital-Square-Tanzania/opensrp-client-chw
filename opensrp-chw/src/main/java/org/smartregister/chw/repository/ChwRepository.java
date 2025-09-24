package org.smartregister.chw.repository;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;

import org.smartregister.AllConstants;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.repository.CoreChwRepository;
import org.smartregister.reporting.ReportingLibrary;

import java.util.Arrays;
import java.util.Collections;

import timber.log.Timber;

public class ChwRepository extends CoreChwRepository {
    private Context context;
    private static String appVersionCodePref = "APP_VERSION_CODE";

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
