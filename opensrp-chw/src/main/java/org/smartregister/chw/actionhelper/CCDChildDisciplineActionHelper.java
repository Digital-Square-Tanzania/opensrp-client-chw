package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.JsonFormUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import timber.log.Timber;
public class CCDChildDisciplineActionHelper extends HomeVisitActionHelper {

    private Context context;
    private String correctingChild = "";
    private String correctingChildKeySelected = "";

    public CCDChildDisciplineActionHelper(Context context){
        this.context = context;
    }

    @Override
    public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
        this.context = context;
    }

    @Override
    public String getPreProcessed() {
        return super.getPreProcessed();
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try{
            JSONObject jsonObject = new JSONObject(jsonPayload);
            correctingChild = JsonFormUtils.getCheckBoxValue(jsonObject, "caregiver_child_correction");
            correctingChildKeySelected = JsonFormUtils.getValue(jsonObject, "caregiver_child_correction");
        }catch (Exception e){
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {
        return MessageFormat.format("{0}: {1}", context.getString(R.string.ccd_child_discipline_subtitle),correctingChild);
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (correctingChild.isEmpty()) {
            return BaseAncHomeVisitAction.Status.PENDING;
        } else if (correctingChildKeySelected.contains("chk_scolds_child")) {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        }
    }

}