package org.smartregister.chw.actionhelper;

import android.content.Context;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.immunization.domain.ServiceWrapper;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class ChildCommunicationAssessmentCounselingActionHelper extends HomeVisitActionHelper {
    private final int ageInMonth;

    private String jsonPayload;

    private String communicatesWithChild = "";

    private String communicatesWithChildObservation = "";

    private final String visitId;

    private final Map<String, Boolean> visitNumberMap = new HashMap<>();

    private final ServiceWrapper serviceWrapper;

    public ChildCommunicationAssessmentCounselingActionHelper(int ageInMonth, Context context, String visitId, ServiceWrapper serviceWrapper) {
        this.ageInMonth = ageInMonth;
        this.visitId = visitId;
        this.context = context;
        this.serviceWrapper = serviceWrapper;
    }

    @Override
    public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
        this.jsonPayload = jsonString;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONArray fields = JsonFormUtils.fields(jsonObject);

            JSONObject child_age_in_month = JsonFormUtils.getFieldJSONObject(fields, "child_age_in_months");
            assert child_age_in_month != null;
            child_age_in_month.remove(JsonFormConstants.VALUE);
            child_age_in_month.put(JsonFormConstants.VALUE, ageInMonth);

            for (Map.Entry<String, Boolean> entry : visitNumberMap.entrySet()) {
                if (entry.getValue()) {
                    JsonFormUtils.getFieldJSONObject(fields, entry.getKey()).put("value", "true");
                }
            }
            String pages = bangoKititaPages();
            if (!pages.isEmpty()) {
                JSONObject bango_kitita_reference_skip_logic = org.smartregister.chw.anc.util.JsonFormUtils.getFieldJSONObject(fields, "bango_kitita_reference_skip_logic");
                if (bango_kitita_reference_skip_logic != null) {
                    bango_kitita_reference_skip_logic.put("value", "true");
                }
                JSONObject bango_kitita_reference = org.smartregister.chw.anc.util.JsonFormUtils.getFieldJSONObject(fields, "bango_kitita_reference");
                if (bango_kitita_reference != null) {
                    bango_kitita_reference.put("text", MessageFormat.format("{0}: {1}", context.getString(R.string.malezi_bango_kitita_message), pages));
                }
            }

            return jsonObject.toString();

        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            communicatesWithChild = JsonFormUtils.getValue(jsonObject, "communication_with_child");
            communicatesWithChildObservation = JsonFormUtils.getValue(jsonObject, "child_communication_observation");
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(communicatesWithChildObservation)) {
            return BaseAncHomeVisitAction.Status.PENDING;
        }
        if (communicatesWithChildObservation.contains("chk_force_smile") || communicatesWithChildObservation.contains("chk_child_asleep")) {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        } else if ((communicatesWithChild.equalsIgnoreCase("yes") && communicatesWithChildObservation.contains("chk_sounds_and_gestures")) || (communicatesWithChild.equalsIgnoreCase("yes") && communicatesWithChildObservation.contains("chk_looks_into_eyes"))) {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        }
    }

    private int visitNumber() {
        if (this.visitId != null) {
            return getPncHomeVisitNumber();
        } else {
            return getChildHomeVisitNumber();
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

    private String bangoKititaPages() {
        int visitNumber = visitNumber();
        Map<Integer, String> bangoKititaPages = new HashMap<>();
        bangoKititaPages.put(1, String.format(context.getString(R.string.bango_kitita_three_pages), "2", ",","3", "4"));
        bangoKititaPages.put(2, String.format(context.getString(R.string.bango_kitita_two_pages), "5", "6"));
        bangoKititaPages.put(3, String.format(context.getString(R.string.bango_kitita_three_pages), "7", ",","8", "9"));
        bangoKititaPages.put(4, String.format(context.getString(R.string.bango_kitita_one_page), "10"));
        bangoKititaPages.put(5, String.format(context.getString(R.string.bango_kitita_one_page), "12"));
        bangoKititaPages.put(6, String.format(context.getString(R.string.bango_kitita_one_page), "14"));
        bangoKititaPages.put(7, String.format(context.getString(R.string.bango_kitita_one_page), "17"));
        bangoKititaPages.put(8, String.format(context.getString(R.string.bango_kitita_one_page), "19"));
        bangoKititaPages.put(9, String.format(context.getString(R.string.bango_kitita_one_page), "23"));
        bangoKititaPages.put(10, String.format(context.getString(R.string.bango_kitita_one_page), "27"));
        bangoKititaPages.put(11, String.format(context.getString(R.string.bango_kitita_one_page), "29"));
        bangoKititaPages.put(12, String.format(context.getString(R.string.bango_kitita_one_page), "31"));
        bangoKititaPages.put(13, String.format(context.getString(R.string.bango_kitita_one_page), "33"));
        bangoKititaPages.put(14, String.format(context.getString(R.string.bango_kitita_one_page), "36"));
        bangoKititaPages.put(15, String.format(context.getString(R.string.bango_kitita_one_page), "39"));
        bangoKititaPages.put(16, String.format(context.getString(R.string.bango_kitita_one_page), "42"));
        bangoKititaPages.put(17, String.format(context.getString(R.string.bango_kitita_one_page), "42"));
        bangoKititaPages.put(18, String.format(context.getString(R.string.bango_kitita_one_page), "42"));
        bangoKititaPages.put(19, String.format(context.getString(R.string.bango_kitita_one_page), "42"));
        bangoKititaPages.put(20, String.format(context.getString(R.string.bango_kitita_one_page), "43"));
        bangoKititaPages.put(21, String.format(context.getString(R.string.bango_kitita_one_page), "43"));
        bangoKititaPages.put(22, String.format(context.getString(R.string.bango_kitita_one_page), "43"));
        bangoKititaPages.put(23, String.format(context.getString(R.string.bango_kitita_one_page), "43"));
        bangoKititaPages.put(24, String.format(context.getString(R.string.bango_kitita_one_page), "43"));
        bangoKititaPages.put(25, String.format(context.getString(R.string.bango_kitita_one_page), "43"));
        String pages = bangoKititaPages.get(visitNumber);
        return (pages != null) ? pages : "";
    }
}
