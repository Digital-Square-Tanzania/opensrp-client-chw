package org.smartregister.chw.actionhelper;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.immunization.domain.ServiceWrapper;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class ComplimentaryFeedingActionHelper extends HomeVisitActionHelper {
    ServiceWrapper serviceWrapper;
    private final Map<String, Boolean> visitNumberMap = new HashMap<>();
    private String jsonString;
    private String complementaryFeedingCounselling = "";


    public ComplimentaryFeedingActionHelper(ServiceWrapper serviceWrapper, Context context) {
        this.serviceWrapper = serviceWrapper;
        this.context = context;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray fields = JsonFormUtils.fields(jsonObject);
            populateVisitNumber();
            for (Map.Entry<String, Boolean> entry : visitNumberMap.entrySet()) {
                if (entry.getValue()) {
                    JsonFormUtils.getFieldJSONObject(fields, entry.getKey()).put("value", "true");
                }
            }
            return jsonObject.toString();
        } catch (JSONException e) {
            Timber.e(e);
        }
        return super.getPreProcessed();
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            complementaryFeedingCounselling = JsonFormUtils.getValue(jsonObject, "comp_feed_counselling_status");
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
        this.jsonString = jsonString;
    }

    @Override
    public String evaluateSubTitle() {
        if (!complementaryFeedingCounselling.isEmpty()) {
            return MessageFormat.format(context.getString(R.string.counselled_mother_for_comp_feeding) + " : {0}", complementaryFeedingCounselling.equalsIgnoreCase("yes") ? context.getString(R.string.yes) : context.getString(R.string.no));
        } else {
            return "";
        }
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (complementaryFeedingCounselling.equalsIgnoreCase("yes"))
            return BaseAncHomeVisitAction.Status.COMPLETED;
        else if (complementaryFeedingCounselling.equalsIgnoreCase("no"))
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        else
            return BaseAncHomeVisitAction.Status.PENDING;
    }

    private int getChildHomeVisitNumber() {
        final Pattern lastIntPattern = Pattern.compile("[^0-9]+([0-9]+)$");
        Matcher matcher = lastIntPattern.matcher(serviceWrapper.getName());
        if (matcher.find()) {
            String someNumberStr = matcher.group(1);
            if (someNumberStr != null) {
                return Integer.parseInt(someNumberStr);
            }
        }
        return 0;
    }

    private void populateVisitNumber() {
        int visitNumber = getChildHomeVisitNumber();
        if (visitNumber >= 8 && visitNumber <= 12) {
            visitNumberMap.put("visit_8_visit_12", true);
        }
    }
}
