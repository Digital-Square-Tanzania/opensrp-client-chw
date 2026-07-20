package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.JsonFormUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class MalariaAction implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {
    private String fam_llin;
    private String llin_2days;
    private String llin_condition;
    private Context context;

    @Override
    public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
        this.context = context;
    }

    @Override
    public String getPreProcessed() {
        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            fam_llin = JsonFormUtils.getValue(jsonObject, "fam_llin");
            llin_2days = JsonFormUtils.getValue(jsonObject, "llin_2days");
            llin_condition = JsonFormUtils.getValue(jsonObject, "llin_condition");
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
        return null;
    }

    @Override
    public String getPreProcessedSubTitle() {
        return null;
    }

    @Override
    public String postProcess(String s) {
        return null;
    }

    @Override
    public String evaluateSubTitle() {
        if (fam_llin.equalsIgnoreCase("No"))
            return MessageFormat.format(context.getString(R.string.uses_net) + ": " + "{0}", context.getString(R.string.anc_malaria_field_no));
        else
            return MessageFormat.format(context.getString(R.string.uses_net) + ": " + "{0}",
                    (fam_llin.equalsIgnoreCase("Yes") ? context.getString(R.string.anc_malaria_field_yes) : context.getString(R.string.anc_malaria_field_no))
                            + "\n" + MessageFormat.format(context.getString(R.string.slept_under_net) + ": " + "{0}",
                            (llin_2days.equalsIgnoreCase("Yes") ? context.getString(R.string.anc_malaria_field_yes) : context.getString(R.string.anc_malaria_field_no))
                                    + "\n" + MessageFormat.format(context.getString(R.string.net_condition) + ": " + "{0}",
                                    (llin_condition.equalsIgnoreCase("Good") ? context.getString(R.string.anc_malaria_net_condition_good) : context.getString(R.string.anc_malaria_net_condition_bad)))));
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(fam_llin)) {
            return BaseAncHomeVisitAction.Status.PENDING;
        }

        if (fam_llin.equalsIgnoreCase("Yes") && llin_2days.equalsIgnoreCase("Yes") && llin_condition.equalsIgnoreCase("Good")) {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        }
    }

    @Override
    public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
        Timber.v("onPayloadReceived");
    }
}
