package org.smartregister.chw.actionhelper;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.PNCVisitUtil;

import java.text.MessageFormat;

import timber.log.Timber;

public class PNCMalariaPreventionActionHelper extends HomeVisitActionHelper {
    private String malaria_protective_measures;

    private String malaria_protective_measures_keys;

    private String llin_2days;

    private String llin_condition;

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            malaria_protective_measures_keys = org.smartregister.chw.util.JsonFormUtils.getValue(jsonObject, "malaria_protective_measures");
            malaria_protective_measures = org.smartregister.chw.util.JsonFormUtils.getCheckBoxValue(jsonObject, "malaria_protective_measures");
            llin_2days = org.smartregister.chw.util.JsonFormUtils.getValue(jsonObject, "llin_2days");
            llin_condition = org.smartregister.chw.util.JsonFormUtils.getValue(jsonObject, "llin_condition");
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {
        StringBuilder stringBuilder = new StringBuilder();
        String malaria_protective_measures_values = MessageFormat.format("{0}: {1}", context.getString(R.string.pnc_malaria_prevention), malaria_protective_measures) + "\n";
        String llin_2days_value = MessageFormat.format("{0}: {1}", context.getString(R.string.slept_under_net), StringUtils.capitalize(PNCVisitUtil.getTranslatedValue(context,llin_2days.trim().toLowerCase()))) + "\n";
        String llin_condition_value = MessageFormat.format("{0}: {1}", context.getString(R.string.net_condition), StringUtils.capitalize(PNCVisitUtil.getTranslatedValue(context,llin_condition.trim().toLowerCase())));
        stringBuilder.append(malaria_protective_measures_values);
        if (!llin_2days.isEmpty()) {
            stringBuilder.append(llin_2days_value);
        }
        if (!llin_condition.isEmpty()) {
            stringBuilder.append(llin_condition_value);
        }
        return stringBuilder.toString();
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(malaria_protective_measures_keys)) {
            return BaseAncHomeVisitAction.Status.PENDING;
        } else if (malaria_protective_measures_keys.contains("chk_none") || llin_2days.equalsIgnoreCase("No") || llin_condition.equalsIgnoreCase("Poor")) {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        }
    }
}
