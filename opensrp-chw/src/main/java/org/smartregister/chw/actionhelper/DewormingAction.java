package org.smartregister.chw.actionhelper;

import android.content.Context;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.Utils;
import org.smartregister.chw.util.UtilsFlv;
import org.smartregister.domain.Alert;
import org.smartregister.util.JsonFormUtils;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import timber.log.Timber;

public class DewormingAction extends HomeVisitActionHelper {
    private final Context context;
    private final String serviceIteration;
    private MemberObject memberObject;
    private String str_date;
    private Date parsedDate;
    private Alert alert;

    public DewormingAction(Context context, String serviceIteration, Alert alert) {
        this.context = context;
        this.serviceIteration = serviceIteration;
        this.alert = alert;
    }

    public DewormingAction(Context context, String serviceIteration, Alert alert, MemberObject member) {
        this(context,serviceIteration,alert);
        this.memberObject=member;
    }


    @Override
    public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
        return isOverDue() ? BaseAncHomeVisitAction.ScheduleStatus.OVERDUE : BaseAncHomeVisitAction.ScheduleStatus.DUE;
    }

    private boolean isOverDue() {
        return new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
    }

    public JSONObject preProcess(JSONObject jsonObject, String iteration) {
        try {
            JSONArray fields = JsonFormUtils.fields(jsonObject);

            String title = jsonObject.getJSONObject(JsonFormConstants.STEP1).getString("title");
            String formatted_count = MessageFormat.format("{0}", Utils.getDayOfMonthWithSuffix(Integer.valueOf(serviceIteration), context));
            jsonObject.getJSONObject(JsonFormConstants.STEP1).put("title", MessageFormat.format(title, formatted_count));

            JSONObject visit_field = JsonFormUtils.getFieldJSONObject(fields, "deworming{0}_date");
            visit_field.put("key", MessageFormat.format(visit_field.getString("key"), iteration, context));
            visit_field.put("hint", MessageFormat.format(visit_field.getString("hint"), Utils.getDayOfMonthWithSuffix(Integer.valueOf(serviceIteration), context)));
        }catch (Exception e){
            Timber.e(e);
        }

        return jsonObject;
    }

    @Override
    public String getPreProcessed() {
        try{
            String formName= Constants.JSON_FORM.CHILD_HOME_VISIT.getDEWORMING();
            JSONObject form= org.smartregister.chw.util.JsonFormUtils.getJson(context, formName, memberObject.getBaseEntityId());

            String minDate= UtilsFlv.changeDateFormat(memberObject.getDob(),"yyyy-MM-dd'T'HH:mm:ss","dd-MM-yyyy");
            Objects.requireNonNull(JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(form), "deworming{0}_date"))
                    .put(JsonFormConstants.MIN_DATE, minDate);
            return form.toString().replace("{0}",serviceIteration);
        }
        catch (Exception e){Timber.e(e);}
        return "";
    }


    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            str_date = org.smartregister.chw.util.JsonFormUtils.getValue(jsonObject, MessageFormat.format("deworming{0}_date", serviceIteration));

            try {
                parsedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(str_date);
            } catch (ParseException e) {
                parsedDate = null;
            }

        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {
        if (parsedDate != null) {
            return MessageFormat.format("{0} {1}",
                    context.getString(R.string.date_given),
                    new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(parsedDate));
        }
        return context.getString(R.string.not_given);
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(str_date)) {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        }

        if (parsedDate != null) {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.PENDING;
        }
    }
}