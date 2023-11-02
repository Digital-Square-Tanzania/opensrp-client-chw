package org.smartregister.chw.util;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.ClientReferralActivity;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.commonregistry.AllCommonsRepository;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.Client;
import org.smartregister.domain.db.EventClient;
import org.smartregister.family.FamilyLibrary;
import org.smartregister.family.util.DBConstants;
import org.smartregister.growthmonitoring.domain.ZScore;
import org.smartregister.growthmonitoring.repository.WeightForHeightRepository;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.repository.BaseRepository;
import org.smartregister.sync.helper.ECSyncHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Utils extends org.smartregister.chw.core.utils.Utils {

    public static final String dd_MMM_yyyy = "dd MMM yyyy";

    public static void launchClientReferralActivity(Activity activity, List<ReferralTypeModel> referralTypeModels, String baseEntityId) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ENTITY_ID, baseEntityId);
        bundle.setClassLoader(ReferralTypeModel.class.getClassLoader());
        bundle.putParcelableArrayList(Constants.REFERRAL_TYPES, (ArrayList<ReferralTypeModel>) referralTypeModels);
        activity.startActivity(new Intent(activity, ClientReferralActivity.class).putExtras(bundle));
    }

    @NotNull
    public static List<ReferralTypeModel> getCommonReferralTypes(Activity activity, String baseEntityId) {
        CommonPersonObjectClient client = Utils.getCommonPersonObjectClient(baseEntityId);
        String gender = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.GENDER, false);

        List<ReferralTypeModel> referralTypeModels = new ArrayList<>();
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            referralTypeModels.add(new ReferralTypeModel(activity.getString(R.string.gbv_referral),
                    Constants.JSON_FORM.getGbvReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_GBV));
            referralTypeModels.add(new ReferralTypeModel(activity.getString(R.string.hts_referral),
                    CoreConstants.JSON_FORM.getHtsReferralForm(), CoreConstants.TASKS_FOCUS.CONVENTIONAL_HIV_TEST));
            if (gender.equalsIgnoreCase("Male")) {
                referralTypeModels.add(new ReferralTypeModel(activity.getString(R.string.vmmc_referral),
                        CoreConstants.JSON_FORM.getVmmcReferralForm(), CoreConstants.TASKS_FOCUS.VMMC));
            }
            if (gender.equalsIgnoreCase("Female") && isMemberOfReproductiveAge(client, 10, 49)) {
                referralTypeModels.add(new ReferralTypeModel(activity.getString(R.string.pregnancy_confirmation_referral),
                        CoreConstants.JSON_FORM.getPregnancyConfirmationReferralForm(), CoreConstants.TASKS_FOCUS.PREGNANCY_CONFIRMATION));
            }
        }
        return referralTypeModels;
    }

    public static String toCSV(List<String> list) {
        String result = "";
        if (list.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String s : list) {
                sb.append(s).append(", ");
            }
            result = sb.deleteCharAt(sb.length() - 2).toString();
        }
        return result;
    }

    public static void setupBottomNavigation(BottomNavigationHelper bottomNavigationHelper,
                                             BottomNavigationView bottomNavigationView,
                                             BottomNavigationView.OnNavigationItemSelectedListener listener) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);

            bottomNavigationView.getMenu().clear();
            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu);
            bottomNavigationHelper.disableShiftMode(bottomNavigationView);
            bottomNavigationView.setOnNavigationItemSelectedListener(listener);
        }

        if (bottomNavigationView != null && !ChwApplication.getApplicationFlavor().hasQR())
            bottomNavigationView.getMenu().removeItem(org.smartregister.family.R.id.action_scan_qr);

        if (bottomNavigationView != null && !ChwApplication.getApplicationFlavor().hasJobAids())
            bottomNavigationView.getMenu().removeItem(org.smartregister.family.R.id.action_job_aids);

        if (bottomNavigationView != null && !ChwApplication.getApplicationFlavor().hasReports())
            bottomNavigationView.getMenu().removeItem(R.id.action_report);
    }

    public static double getWFHZScore(String gender, String height, String weight) {
        double zScore = 0.0;
        List<ZScore> zScoreValues = new WeightForHeightRepository().findZScoreVariables(gender, Double.parseDouble(height));
        if (zScoreValues.size() > 0) {
            zScore = zScoreValues.get(0).getZ(Double.parseDouble(weight));
        }
        return zScore;
    }

    public static String formatDateForVisual(String date, String inputFormat) {
        if (StringUtils.isEmpty(date)) return "";
        SimpleDateFormat format = new SimpleDateFormat(inputFormat);
        Date newDate = null;
        try {
            newDate = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        format = new SimpleDateFormat(dd_MMM_yyyy);
        return format.format(newDate);
    }

    public static String getClientName(String firstName, String middleName, String lastName) {
        String trimFirstName = firstName.trim();
        String trimMiddleName = middleName.trim();
        String trimLastName = lastName.trim();
        if (ChwApplication.getApplicationFlavor().hasSurname()) {
            return getName(trimFirstName, trimMiddleName, trimLastName);
        } else {
            return Utils.getName(trimFirstName, trimMiddleName);
        }
    }


    public static String getClientGender(String baseEntityId) {
        CommonRepository commonRepository = org.smartregister.family.util.Utils.context().commonrepository(org.smartregister.family.util.Utils.metadata().familyMemberRegister.tableName);

        final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(baseEntityId);
        final CommonPersonObjectClient client = new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());
        return org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.GENDER, false);
    }

    /*
     * For CBHS Registration
     */
    public static void updateAgeAndGender(JSONArray fields, int age, String gender) throws Exception {
        boolean foundAge = false;
        boolean foundGender = false;
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (field.getString("name").equals("age")) {
                field.getJSONObject("properties").put("text", String.valueOf(age));
                foundAge = true;
            }
            if (field.getString("name").equals("gender")) {
                field.getJSONObject("properties").put("text", gender);
                foundGender = true;
            }
            if (foundAge && foundGender) {
                return;
            }
        }
    }


    /**
     * Removes a user, used for removing clients marked as Deceased
     *
     * @param familyID
     * @param closeFormJsonString
     * @param providerId
     * @return
     * @throws Exception
     */
    public static String removeUser(String familyID, JSONObject closeFormJsonString, String providerId) throws Exception {
        String res = null;
        Triple<Pair<Date, String>, String, List<Event>> triple = CoreJsonFormUtils.processRemoveMemberEvent(familyID, org.smartregister.family.util.Utils.getAllSharedPreferences(), closeFormJsonString, providerId);
        if (triple != null && triple.getLeft() != null) {
            processEvents(triple.getRight());

            if (triple.getLeft().second.equalsIgnoreCase(CoreConstants.EventType.REMOVE_CHILD)) {
                updateRepo(triple, org.smartregister.family.util.Utils.metadata().familyMemberRegister.tableName);
                updateRepo(triple, CoreConstants.TABLE_NAME.CHILD);
            } else if (triple.getLeft().second.equalsIgnoreCase(CoreConstants.EventType.REMOVE_FAMILY)) {
                updateRepo(triple, org.smartregister.family.util.Utils.metadata().familyRegister.tableName);
            } else {
                updateRepo(triple, org.smartregister.family.util.Utils.metadata().familyMemberRegister.tableName);
            }
            res = triple.getLeft().second;
        }

        long lastSyncTimeStamp = getAllSharedPreferences().fetchLastUpdatedAtDate(0);
        Date lastSyncDate = new Date(lastSyncTimeStamp);
        FamilyLibrary.getInstance().getClientProcessorForJava().processClient(FamilyLibrary.getInstance().getEcSyncHelper().getEvents(lastSyncDate, BaseRepository.TYPE_Unprocessed));
        getAllSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
        return res;
    }

    private static void processEvents(List<Event> events) throws Exception {
        ECSyncHelper syncHelper = ChwApplication.getInstance().getEcSyncHelper();
        List<EventClient> clients = new ArrayList<>();
        for (Event e : events) {
            JSONObject json = new JSONObject(CoreJsonFormUtils.gson.toJson(e));
            syncHelper.addEvent(e.getBaseEntityId(), json);

            org.smartregister.domain.Event event = CoreJsonFormUtils.gson.fromJson(json.toString(), org.smartregister.domain.Event.class);
            clients.add(new EventClient(event, new Client(e.getBaseEntityId())));
        }
        FamilyLibrary.getInstance().getClientProcessorForJava().processClient(clients);
    }

    private static void updateRepo(Triple<Pair<Date, String>, String, List<Event>> triple, String tableName) {
        AllCommonsRepository commonsRepository = ChwApplication.getInstance().getAllCommonsRepository(tableName);

        Date date_removed = new Date();
        Date dod = null;
        if (triple.getLeft() != null && triple.getLeft().first != null) {
            dod = triple.getLeft().first;
        }

        if (commonsRepository != null && dod == null) {
            ContentValues values = new ContentValues();
            values.put(DBConstants.KEY.DATE_REMOVED, getDBFormatedDate(date_removed));
            commonsRepository.update(tableName, values, triple.getMiddle());
            commonsRepository.updateSearch(triple.getMiddle());
            commonsRepository.close(triple.getMiddle());
        }

        // enter the date of death
        if (dod != null && commonsRepository != null) {
            ContentValues values = new ContentValues();
            values.put(DBConstants.KEY.DOD, getDBFormatedDate(dod));
            commonsRepository.update(tableName, values, triple.getMiddle());
            commonsRepository.updateSearch(triple.getMiddle());
        }

    }

    private static String getDBFormatedDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }


}
