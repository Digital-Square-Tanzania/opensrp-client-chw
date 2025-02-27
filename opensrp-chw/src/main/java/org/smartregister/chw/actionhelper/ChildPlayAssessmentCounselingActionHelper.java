package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
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

public class ChildPlayAssessmentCounselingActionHelper extends HomeVisitActionHelper {
    private final Context context;

    private final String visitId;

    private final Map<String, Boolean> visitNumberMap = new HashMap<>();

    private final ServiceWrapper serviceWrapper;

    private String jsonString;

    private String demonstrate_play_child;

    private String play_with_child;

    public ChildPlayAssessmentCounselingActionHelper(Context context, String visitId, ServiceWrapper serviceWrapper) {
        this.context = context;
        this.visitId = visitId;
        this.serviceWrapper = serviceWrapper;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            demonstrate_play_child = JsonFormUtils.getValue(jsonObject, "demonstrate_play_child");
            play_with_child = JsonFormUtils.getValue(jsonObject, "play_with_child");
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
//            populateVisitNumber();
            for (Map.Entry<String, Boolean> entry : visitNumberMap.entrySet()) {
                if (entry.getValue()) {
                    JsonFormUtils.getFieldJSONObject(fields, entry.getKey()).put("value", "true");
                }
            }
            String pages = bangoKititaPages();
            if (!pages.isEmpty()) {
                JSONObject bango_kitita_reference_skip_logic = JsonFormUtils.getFieldJSONObject(fields, "bango_kitita_reference_skip_logic");
                if (bango_kitita_reference_skip_logic != null) {
                    bango_kitita_reference_skip_logic.put("value", "true");
                }
                JSONObject bango_kitita_reference = JsonFormUtils.getFieldJSONObject(fields, "bango_kitita_reference");
                if (bango_kitita_reference != null) {
                    bango_kitita_reference.put("text", MessageFormat.format("{0}: <b>{1}</b>", context.getString(R.string.bango_kitita_message), pages));
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
        if (!StringUtils.isBlank(play_with_child) || !StringUtils.isBlank(demonstrate_play_child)) {
            if (visitNumber() <= 5) {
                if (demonstrate_play_child.contains("chk_move_baby_arms_legs") || demonstrate_play_child.contains("chk_baby_attention_shaker_toy")) {
                    return BaseAncHomeVisitAction.Status.COMPLETED;
                } else {
                    return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
                }
            } else {
                if (play_with_child.equalsIgnoreCase("Yes")) {
                    return BaseAncHomeVisitAction.Status.COMPLETED;
                } else {
                    return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
                }
            }
        } else {
            return BaseAncHomeVisitAction.Status.PENDING;
        }
    }

    private void populateVisitNumber() {
        int visitNumber = visitNumber();
        if (visitNumber >= 17 && visitNumber <= 25) {
            visitNumberMap.put("visit_17_visit_25", true);
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

    private String bangoKititaPages() {
        int visitNumber = visitNumber();
        Map<Integer, String> bangoKititaPages = new HashMap<>();
        bangoKititaPages.put(1, String.format(context.getString(R.string.bango_kitita_three_pages), "2", "3", "4"));
        bangoKititaPages.put(2, String.format(context.getString(R.string.bango_kitita_two_pages), "5", "6"));
        bangoKititaPages.put(3, String.format(context.getString(R.string.bango_kitita_three_pages), "7", "8", "9"));
        bangoKititaPages.put(4, String.format(context.getString(R.string.bango_kitita_one_page), "10"));
        bangoKititaPages.put(5, String.format(context.getString(R.string.bango_kitita_two_pages), "11", "13"));
        bangoKititaPages.put(6, String.format(context.getString(R.string.bango_kitita_one_page), "14"));
        bangoKititaPages.put(7, String.format(context.getString(R.string.bango_kitita_two_pages), "16", "18"));
        bangoKititaPages.put(8, String.format(context.getString(R.string.bango_kitita_one_page), "20"));
        bangoKititaPages.put(9, String.format(context.getString(R.string.bango_kitita_two_pages), "21", "22"));
        bangoKititaPages.put(10, String.format(context.getString(R.string.bango_kitita_two_pages), "24", "25"));
        bangoKititaPages.put(11, String.format(context.getString(R.string.bango_kitita_one_page), "28"));
        bangoKititaPages.put(12, String.format(context.getString(R.string.bango_kitita_two_pages), "30", "32"));
        bangoKititaPages.put(13, String.format(context.getString(R.string.bango_kitita_two_pages), "34", "35"));
        bangoKititaPages.put(14, String.format(context.getString(R.string.bango_kitita_two_pages), "37", "38"));
        bangoKititaPages.put(15, String.format(context.getString(R.string.bango_kitita_two_pages), "40", "41"));
        String page42 = String.format(context.getString(R.string.bango_kitita_one_page), "42");
        for (int i = 16; i <= 19; i++) {
            bangoKititaPages.put(i, page42);
        }

        String page43 = String.format(context.getString(R.string.bango_kitita_one_page), "43");
        for (int i = 20; i <= 25; i++) {
            bangoKititaPages.put(i, page43);
        }
        String pages = bangoKititaPages.get(visitNumber);
        return (pages != null) ? pages : "";
    }
}
