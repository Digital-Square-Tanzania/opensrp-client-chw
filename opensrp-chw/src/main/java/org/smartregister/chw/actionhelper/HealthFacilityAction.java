package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.JsonFormUtils;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class HealthFacilityAction extends HealthFacilityVisitAction {
    private Context context;

    private String anc_hf_visit;
    private String anc_hf_visit_date;
    private Date visitDate;


    public HealthFacilityAction(MemberObject memberObject, Map<Integer, LocalDate> dateMap) {
        super(memberObject, dateMap);
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        super.onJsonFormLoaded(jsonPayload, context, map);
        this.context = context;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);

            anc_hf_visit = JsonFormUtils.getValue(jsonObject, "anc_hf_visit");
            anc_hf_visit_date = JsonFormUtils.getValue(jsonObject, "anc_hf_visit_date");
            visitDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(anc_hf_visit_date);

        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public String getPreProcessed() {
        return super.getPreProcessed();
    }

    @Override
    public String evaluateSubTitle() {
        StringBuilder stringBuilder = new StringBuilder();
        if (anc_hf_visit.equalsIgnoreCase("No")) {
            stringBuilder.append(context.getString(R.string.visit_not_done).replace("\n", ""));
        } else {
            stringBuilder.append(MessageFormat.format("{0}: {1}\n", context.getString(R.string.date), new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(visitDate)));
        }
        return stringBuilder.toString();
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(anc_hf_visit)) {
            return BaseAncHomeVisitAction.Status.PENDING;
        }

        if (anc_hf_visit.equalsIgnoreCase("Yes")) {
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
