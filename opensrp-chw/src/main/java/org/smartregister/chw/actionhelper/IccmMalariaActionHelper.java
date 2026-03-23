package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.malaria.contract.BaseIccmVisitContract;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.malaria.domain.IccmMemberObject;
import org.smartregister.chw.malaria.domain.VisitDetail;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;
import org.smartregister.chw.malaria.util.AppExecutors;
import org.smartregister.chw.referral.util.JsonFormConstants;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.IccmReferralActionUtils;
import org.smartregister.chw.util.IccmVisitUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class IccmMalariaActionHelper implements BaseIccmVisitAction.IccmVisitActionHelper {
    private final Context context;

    private String jsonPayload;

    private final HashMap<String, Boolean> checkObject = new HashMap<>();

    private final IccmMemberObject memberObject;

    private final Map<String, List<VisitDetail>> details;

    private final LinkedHashMap<String, BaseIccmVisitAction> actionList;

    private final BaseIccmVisitContract.InteractorCallBack callBack;

    private final String isPneumoniaSuspect;

    private final String diarrheaSigns;

    private String interpretationForMrdtTwo;

    public IccmMalariaActionHelper(Context context, String enrollmentFormSubmissionId, Map<String, List<VisitDetail>> details, LinkedHashMap<String, BaseIccmVisitAction> actionList, BaseIccmVisitContract.InteractorCallBack callBack, String isPneumoniaSuspect, String diarrheaSigns) {
        this.context = context;
        this.memberObject = IccmDao.getMember(enrollmentFormSubmissionId);
        this.details = details;
        this.actionList = actionList;
        this.callBack = callBack;
        this.isPneumoniaSuspect = isPneumoniaSuspect;
        this.diarrheaSigns = diarrheaSigns;
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

            interpretationForMrdtTwo = CoreJsonFormUtils.getValue(jsonObject, "interpretation_for_mrdt_two");
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

            boolean shouldRetainReferralAction = IccmReferralActionUtils.shouldKeepReferralFromMalaria(
                    isPneumoniaSuspect,
                    interpretationForMrdtTwo,
                    diarrheaSigns
            );
            syncReferralAction(shouldRetainReferralAction, shouldRetainReferralAction);
        } catch (Exception e) {
            Timber.e(e);
        }

        new AppExecutors().mainThread().execute(() -> callBack.preloadActions(actionList));

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

    private void syncReferralAction(boolean shouldRetainReferralAction, boolean addIfMissing) {
        IccmReferralActionUtils.updateReferralAction(
                context,
                memberObject,
                memberObject.getIccmEnrollmentFormSubmissionId(),
                actionList,
                details,
                shouldRetainReferralAction,
                addIfMissing
        );
    }

}
