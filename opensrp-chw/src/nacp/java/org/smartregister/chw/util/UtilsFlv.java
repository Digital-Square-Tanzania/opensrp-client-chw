package org.smartregister.chw.util;

import static org.smartregister.chw.util.FnInterfaces.KeyValue;

import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.view.Menu;
import android.widget.DatePicker;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.core.rule.MalariaFollowUpRule;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.MalariaVisitUtil;
import org.smartregister.chw.fp.dao.FpDao;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.malaria.dao.MalariaDao;
import org.smartregister.chw.tb.dao.TbDao;
import org.smartregister.client.utils.constants.JsonFormConstants;
import org.smartregister.dao.AbstractDao;
import org.smartregister.util.Utils;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class UtilsFlv {
    private static class UpdateFollowUpMenuItem extends AsyncTask<Void, Void, Void> {
        private final String baseEntityId;
        private Menu menu;
        private MalariaFollowUpRule malariaFollowUpRule;

        public UpdateFollowUpMenuItem(String baseEntityId, Menu menu) {
            this.baseEntityId = baseEntityId;
            this.menu = menu;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Date malariaTestDate = MalariaDao.getMalariaTestDate(baseEntityId);
            Date followUpDate = MalariaDao.getMalariaFollowUpVisitDate(baseEntityId);
            malariaFollowUpRule = MalariaVisitUtil.getMalariaStatus(malariaTestDate,followUpDate);
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            if (malariaFollowUpRule != null && StringUtils.isNotBlank(malariaFollowUpRule.getButtonStatus()) &&
                    !CoreConstants.VISIT_STATE.EXPIRED.equalsIgnoreCase(malariaFollowUpRule.getButtonStatus())) {
                menu.findItem(R.id.action_malaria_followup_visit).setVisible(true);
            }
        }
    }
    public static void updateMalariaMenuItems(String baseEntityId, Menu menu) {
        if (MalariaDao.isRegisteredForMalaria(baseEntityId)) {
            Utils.startAsyncTask(new UpdateFollowUpMenuItem(baseEntityId, menu), null);
        } else {
            menu.findItem(R.id.action_malaria_registration).setVisible(true);
        }
    }

    public static void updateFpMenuItems(String baseEntityId, Menu menu) {
        menu.findItem(R.id.action_fp_initiation).setVisible(!FpDao.isRegisteredForFp(baseEntityId));
    }

    public static void updateHivMenuItems(String baseEntityId, Menu menu) {
        if (HivDao.isRegisteredForHiv(baseEntityId)) {
            menu.findItem(R.id.action_cbhs_registration).setVisible(false);
        }else{
            menu.findItem(R.id.action_cbhs_registration).setVisible(true);
        }
    }

    public static void updateTbMenuItems(String baseEntityId, Menu menu) {
        if (TbDao.isRegisteredForTb(baseEntityId)) {
            menu.findItem(R.id.action_tb_registration).setVisible(false);
        }else{
            menu.findItem(R.id.action_tb_registration).setVisible(true);
        }
    }
    public static Date getDateFromDatePicker(@NonNull DatePicker datePicker) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(
                datePicker.getYear()
                ,datePicker.getMonth()
                ,datePicker.getDayOfMonth());
        return calendar.getTime();
    }

    @SafeVarargs @NonNull
    public static <T> T coalesce(T val1, T val2, T ... input) {
        if (val1!=null) return val1;
        if (val2!=null) return val2;
        for(T val:input){
            if(val!=null){return val;}
        }
       throw new IllegalArgumentException("All arguments are null");
    }


    /**
     * Retrieves a value from a JSONObject based on a dot-separated path.
     * The path allows for deep retrieval from nested JSONObjects and JSONArrays.
     *
     * @param json The source JSONObject from which to retrieve the value.
     * @param path A dot-separated string indicating the retrieval path.
     *             For JSONArrays in the path, use the desired index as part of the path.
     *
     * @return The value found at the specified path. The return type is unchecked,
     *         so this method should be used with care, ensuring that the expected
     *         return type matches the actual value at the given path in the JSON.
     *         Works optimally for JSONArray, JSONObject, and Java primitives.
     *         Returns null if the path is not found or in case of an error.
     */
    @SuppressWarnings("unchecked")
    public static <T> T jsonGet(JSONObject json, String path) {
        try {
            Object current = json;
            for (String part : path.split("\\.")) {
                if (current instanceof JSONObject) {
                    current = ((JSONObject) current).get(part);
                } else if (current instanceof JSONArray) {
                    current = ((JSONArray) current).get(Integer.parseInt(part));
                }
            }
            return (T) current;
        } catch (JSONException e) {
            Timber.e(e);
        }
        return null;
    }
    public static <T> T jsonGet(String json, String path,T defaultValue) {
        return coalesce(jsonGet(json,path),defaultValue);
    }
    public static <T> T jsonGet(String json, String path) {
        return ex(()->jsonGet(new JSONObject(json),path));
    }

    public static KeyValue<String,String> getFieldKeyValuePair(JSONObject field){
        return new KeyValue<>(
                field.optString(JsonFormConstants.KEY),
                field.optString(JsonFormConstants.VALUE));
    }
    public static <T> T ex(FnInterfaces.Producer<T> function){
        return ex(function,null);
    }
    public static void ex(FnInterfaces.Runnable function){
        try{ function.run();}
        catch (Exception e){ Timber.e(e);}
    }
    public static <T> T ex(FnInterfaces.Producer<T> function, T defaultVal){
        try{ return function.produce();}
        catch (Exception e){ Timber.e(e);}
        return defaultVal;
    }
    public static boolean isValidDOBDateFormat(String value){
        try { AbstractDao.getDobDateFormat().parse(value);
            return true; }
        catch (ParseException e) { return false; }
    }
    public static String getEnglishString(Context context, int stringResId) {
        // Save the current configuration
        Configuration configuration = context.getResources().getConfiguration();
        Locale savedLocale = configuration.locale;
        // Set locale to the default (assumed to be English here)
        configuration.setLocale(Locale.ENGLISH);
        // Get a new context with the updated configuration
        Context defaultContext = context.createConfigurationContext(configuration);
        // Get the string from the default resources
        String defaultString = defaultContext.getResources().getString(stringResId);
        // Restore the original configuration
        configuration.setLocale(savedLocale);
        context.createConfigurationContext(configuration);
        return defaultString;
    }
}
