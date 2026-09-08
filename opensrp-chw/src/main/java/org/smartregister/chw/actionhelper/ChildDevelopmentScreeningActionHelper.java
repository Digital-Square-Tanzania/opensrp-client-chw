package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.immunization.domain.ServiceWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class ChildDevelopmentScreeningActionHelper extends HomeVisitActionHelper {
    private final String visitId;
    private final ServiceWrapper serviceWrapper;
    private String jsonString;
    private final Map<String, Boolean> visitNumberMap = new HashMap<>();
    private String child_development_issues;

    public ChildDevelopmentScreeningActionHelper(String visitId, ServiceWrapper serviceWrapper) {
        this.visitId = visitId;
        this.serviceWrapper = serviceWrapper;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            child_development_issues = JsonFormUtils.getValue(jsonObject, "child_development_issues");
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
        this.jsonString = jsonString;
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
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(child_development_issues)) {
            return BaseAncHomeVisitAction.Status.PENDING;
        } else if ((child_development_issues.equalsIgnoreCase("None") || child_development_issues.equalsIgnoreCase("Hamna"))) {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        } else if (!child_development_issues.contains("chk_none")) {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        }
    }

    private void populateVisitNumber() {
        int visitNumber = visitNumber();
        if ((visitNumber >= 3 && visitNumber <= 7) || visitNumber == 9 || (visitNumber >= 11 && visitNumber <= 15)) {
            visitNumberMap.put("visit_3_visit_16", true);
        } else if (visitNumber == 8 || visitNumber == 10 || visitNumber == 16) {
            visitNumberMap.put("visit_3_visit_16", true);
            visitNumberMap.put("visit_8_visit_10_visit_16", true);
        }
    }

    private int visitNumber() {
        if (this.visitId != null) {
            return getPncHomeVisitNumber();
        } else {
            return getChildHomeVisitNumber();
        }
    }

    private int getPncHomeVisitNumber() {
        switch (this.visitId) {
            case "1":
                return 1;
            case "3":
                return 2;
            case "8":
                return 3;
            case "21 - 27":
                return 4;
            case "35 - 41":
                return 5;
            default:
                return 0;
        }
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
}
