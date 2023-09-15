package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.malaria.domain.IccmMemberObject;
import org.smartregister.chw.malaria.domain.VisitDetail;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;
import org.smartregister.chw.referral.util.JsonFormConstants;
import org.smartregister.chw.util.IccmVisitUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class IccmMalariaActionHelper implements BaseIccmVisitAction.IccmVisitActionHelper {
    private String jsonPayload;

    private final HashMap<String, Boolean> checkObject = new HashMap<>();

    private final IccmMemberObject memberObject;

    public IccmMalariaActionHelper(String enrollmentFormSubmissionId) {
        this.memberObject = IccmDao.getMember(enrollmentFormSubmissionId);
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            jsonObject.getJSONObject("global").put("weight", memberObject.getWeight());

            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            checkObject.clear();
            JSONObject jsonObject = new JSONObject(jsonPayload);
            String mrdtResults = CoreJsonFormUtils.getValue(jsonObject, "mrdt_results");
            checkObject.put("mrdt_results", StringUtils.isNotBlank(mrdtResults));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BaseIccmVisitAction.ScheduleStatus getPreProcessedStatus() {
        return null;
    }

    @Override
    public String getPreProcessedSubTitle() {
        return null;
    }

    @Override
    public String postProcess(String jsonPayload) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonPayload);
            JSONArray fields = org.smartregister.family.util.JsonFormUtils.fields(jsonObject);

            JSONObject malariaCompletionStatus = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "malaria_completion_status");
            assert malariaCompletionStatus != null;
            malariaCompletionStatus.put(JsonFormConstants.VALUE, IccmVisitUtils.getActionStatus(checkObject));

        } catch (Exception e) {
            Timber.e(e);
        }

        if (jsonObject != null) {
            return jsonObject.toString();
        }
        return null;
    }

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseIccmVisitAction.Status evaluateStatusOnPayload() {
        String status = IccmVisitUtils.getActionStatus(checkObject);
        if (status.equalsIgnoreCase(IccmVisitUtils.Complete)) {
            return BaseIccmVisitAction.Status.COMPLETED;
        }
        if (status.equalsIgnoreCase(IccmVisitUtils.Ongoing)) {
            return BaseIccmVisitAction.Status.PARTIALLY_COMPLETED;
        }
        return BaseIccmVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseIccmVisitAction baseIccmVisitAction) {
        //overridden
    }
}
