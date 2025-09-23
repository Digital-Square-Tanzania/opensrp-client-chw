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
import org.smartregister.chw.util.PNCVisitUtil;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class ChildNewBornCareIntroductionActionHelper extends HomeVisitActionHelper {
    private final Context context;

    private final String visitId;

    private String premature_baby;

    private String jsonString;

    public ChildNewBornCareIntroductionActionHelper(Context context, String visitId) {
        this.context = context;
        this.visitId = visitId;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            premature_baby = org.smartregister.chw.util.JsonFormUtils.getValue(jsonObject, "premature_baby");
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
            JSONObject visit_1 = JsonFormUtils.getFieldJSONObject(fields, "visit_1");
            if(visit_1 != null){
                if (this.visitId.equalsIgnoreCase("1")) {
                    visit_1.put("value", "true");
                }else{
                    visit_1.put("value", "false");
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
        if (!premature_baby.isEmpty()) {
            return MessageFormat.format("{0}: {1}", context.getString(R.string.baby_premature), StringUtils.capitalize(PNCVisitUtil.getTranslatedValue(context, premature_baby.trim().toLowerCase())));
        }
        return null;
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(premature_baby) && this.visitId.equalsIgnoreCase("1")) {
            return BaseAncHomeVisitAction.Status.PENDING;
        } else if (!StringUtils.isBlank(premature_baby) && premature_baby.equalsIgnoreCase("Yes")) {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        }
    }
}
