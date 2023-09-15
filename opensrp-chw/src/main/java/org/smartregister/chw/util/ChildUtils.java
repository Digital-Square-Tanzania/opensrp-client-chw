package org.smartregister.chw.util;

import android.content.Context;

import com.google.gson.reflect.TypeToken;

import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.CoreChildUtils;
import org.smartregister.chw.core.utils.ServiceTask;
import org.smartregister.domain.Event;
import org.smartregister.domain.Obs;
import org.smartregister.family.util.DBConstants;
import org.smartregister.util.DateUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ChildUtils extends CoreChildUtils {
    private static CoreChildUtils.Flavor childUtilsFlv = new ChildUtilsFlv();

    /**
     * Based on received vaccine list it'll return the fully immunized year.
     * Firstly it'll check with 2years vaccine list if it's match then return 2 year fully immunized.
     * Else it'll check  with 1year vaccine list otherwise it'll return empty string means not fully immunized.
     *
     * @param vaccineGiven
     * @return
     */
    public static String isFullyImmunized(List<String> vaccineGiven) {
        List<String> twoYrVac = Arrays.asList(childUtilsFlv.getTwoYearVaccines());
        if (vaccineGiven.containsAll(twoYrVac)) {
            return "2";
        }

        List<String> oneYrVac = Arrays.asList(childUtilsFlv.getOneYearVaccines());
        if (vaccineGiven.containsAll(oneYrVac)) {
            return "1";
        }

        return "";

    }

    public static String[] mainColumns(String tableName, String familyTable, String familyMemberTable) {
        ArrayList<String> columnList = new ArrayList<>();
        columnList.add(tableName + "." + DBConstants.KEY.RELATIONAL_ID + " as " + ChildDBConstants.KEY.RELATIONAL_ID);
        columnList.add(tableName + "." + DBConstants.KEY.LAST_INTERACTED_WITH);
        columnList.add(tableName + "." + DBConstants.KEY.BASE_ENTITY_ID);
        columnList.add(tableName + "." + DBConstants.KEY.FIRST_NAME);
        columnList.add(tableName + "." + DBConstants.KEY.MIDDLE_NAME);
        columnList.add(familyMemberTable + "." + DBConstants.KEY.FIRST_NAME + " as " + ChildDBConstants.KEY.FAMILY_FIRST_NAME);
        columnList.add(familyMemberTable + "." + DBConstants.KEY.LAST_NAME + " as " + ChildDBConstants.KEY.FAMILY_LAST_NAME);
        columnList.add(familyMemberTable + "." + DBConstants.KEY.MIDDLE_NAME + " as " + ChildDBConstants.KEY.FAMILY_MIDDLE_NAME);
        columnList.add(familyTable + "." + DBConstants.KEY.VILLAGE_TOWN + " as " + ChildDBConstants.KEY.FAMILY_HOME_ADDRESS);
        columnList.add(tableName + "." + DBConstants.KEY.LAST_NAME);
        columnList.add(tableName + "." + DBConstants.KEY.UNIQUE_ID);
        columnList.add(tableName + "." + DBConstants.KEY.GENDER);
        columnList.add(tableName + "." + DBConstants.KEY.DOB);
        columnList.add(tableName + "." + org.smartregister.family.util.Constants.JSON_FORM_KEY.DOB_UNKNOWN);
        columnList.add(tableName + "." + ChildDBConstants.KEY.LAST_HOME_VISIT);
        columnList.add(tableName + "." + ChildDBConstants.KEY.VISIT_NOT_DONE);
        columnList.add(tableName + "." + ChildDBConstants.KEY.CHILD_BF_HR);
        columnList.add(tableName + "." + ChildDBConstants.KEY.CHILD_PHYSICAL_CHANGE);
        columnList.add(tableName + "." + ChildDBConstants.KEY.BIRTH_CERT);
        columnList.add(tableName + "." + ChildDBConstants.KEY.BIRTH_CERT_ISSUE_DATE);
        columnList.add(tableName + "." + ChildDBConstants.KEY.BIRTH_CERT_NUMBER);
        columnList.add(tableName + "." + ChildDBConstants.KEY.BIRTH_CERT_NOTIFIICATION);
        columnList.add(tableName + "." + ChildDBConstants.KEY.ILLNESS_DATE);
        columnList.add(tableName + "." + ChildDBConstants.KEY.ILLNESS_DESCRIPTION);
        columnList.add(tableName + "." + ChildDBConstants.KEY.DATE_CREATED);
        columnList.add(tableName + "." + ChildDBConstants.KEY.ILLNESS_ACTION);
        columnList.add(tableName + "." + ChildDBConstants.KEY.VACCINE_CARD);

        return columnList.toArray(new String[columnList.size()]);


    }

    public static ServiceTask createServiceTaskFromEvent(String taskType, String details, String title, String formSubmissionId) {
        ServiceTask serviceTask = new ServiceTask();
        Event event = ChildUtils.gsonConverter.fromJson(details, new TypeToken<Event>() {
        }.getType());
        List<Obs> observations = event.getObs();
        for (Obs obs : observations) {
            if (obs.getFormSubmissionField().equalsIgnoreCase(formSubmissionId)) {
                List<Object> hu = obs.getHumanReadableValues();
                String value = "";
                for (Object object : hu) {
                    value = (String) object;
                }
                serviceTask.setTaskLabel(value);
            }
        }
        serviceTask.setTaskTitle(title);
        serviceTask.setTaskType(taskType);
        return serviceTask;

    }

    public static ServiceTask createECDTaskFromEvent(Context context, String taskType, String details, String title) {
        ServiceTask serviceTask = new ServiceTask();
        Event event = ChildUtils.gsonConverter.fromJson(details, new TypeToken<Event>() {
        }.getType());
        List<Obs> observations = event.getObs();
        String label = "";
        for (Obs obs : observations) {
            if (obs.getFormSubmissionField().equalsIgnoreCase("develop_warning_signs")) {
                List<Object> hu = obs.getHumanReadableValues();
                String value = "";
                for (Object object : hu) {
                    value = (String) object;
                }
                label = context.getString(R.string.dev_warning_sign) + Utils.getYesNoAsLanguageSpecific(context, value);
            }
            if (obs.getFormSubmissionField().equalsIgnoreCase("stim_skills")) {
                List<Object> hu = obs.getHumanReadableValues();
                String value = "";
                for (Object object : hu) {
                    value = (String) object;
                }
                label = label + "\n" + context.getString(R.string.care_stim_skill) + Utils.getYesNoAsLanguageSpecific(context, value);
            }
            if (obs.getFormSubmissionField().equalsIgnoreCase("early_learning")) {
                List<Object> hu = obs.getHumanReadableValues();
                String value = "";
                for (Object object : hu) {
                    value = (String) object;
                }
                label = label + "\n" + context.getString(R.string.early_learning) + Utils.getYesNoAsLanguageSpecific(context, value);
            }
        }
        serviceTask.setTaskLabel(label);
        serviceTask.setTaskTitle(title);
        serviceTask.setTaskType(taskType);
        return serviceTask;

    }

    public static String[] splitStringByNewline(String strWithNewline) {
        return strWithNewline.split("\n");
    }

    public static String getDurationFromTwoDate(Date dob, Date homeVisitServiceDate) {

        long timeDiff = Math.abs(homeVisitServiceDate.getTime() - dob.getTime());
        return DateUtil.getDuration(timeDiff);

    }
}