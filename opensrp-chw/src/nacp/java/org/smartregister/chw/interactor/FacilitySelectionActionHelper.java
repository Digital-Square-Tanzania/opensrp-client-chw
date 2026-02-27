package org.smartregister.chw.interactor;

import static org.smartregister.chw.anc.model.BaseAncHomeVisitAction.Status.COMPLETED;
import static org.smartregister.chw.anc.model.BaseAncHomeVisitAction.Status.PENDING;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.referral.util.LocationUtils;
import org.smartregister.chw.util.JsonFormUtilsFlv;
import org.smartregister.chw.util.JsonQ;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class FacilitySelectionActionHelper extends HomeVisitActionHelper {

    private final Map<String, ReferralHelperInfo> referralsInfo = new LinkedHashMap<>();
    private BaseAncHomeVisitAction.Status status;

    FacilitySelectionActionHelper(JSONObject referralProblem, String referralType, String baseEntityId) {
        ReferralHelperInfo info = new ReferralHelperInfo(referralType, baseEntityId, referralProblem);
        referralsInfo.put(info.stepName, info);
    }

    FacilitySelectionActionHelper(List<ReferralHelperInfo> infos) {
        for (ReferralHelperInfo info : infos) {
            referralsInfo.put(info.stepName, info);
        }
    }

    public static JSONObject copyReferralProblem(String referralForm, String dangerSignQnKey) {
        try {
            return copyReferralProblem(new JSONObject(referralForm), dangerSignQnKey);
        } catch (JSONException e) {
            Timber.e(e);
            return new JSONObject();
        }
    }

    public static JSONObject copyReferralProblem(JSONObject referralForm, String dangerSignQnKey) {
        try {
            JSONObject problem = JsonFormUtilsFlv.getQuestion(dangerSignQnKey, referralForm, new JSONObject());
            problem.put("key", "problem");
            problem.put("openmrs_entity_id", "problem");
            JSONArray options = problem.getJSONArray("options");

            List<String> value = JsonFormUtilsFlv.fromJsonArray(problem.getJSONArray("value"), Object::toString);
            for (int i = 0, len = options.length(); i < len; i++) {
                JSONObject option = options.getJSONObject(i);
                if (value.contains(option.optString("key"))) {
                    option.put("value", true);
                }
            }
            return problem;
        } catch (JSONException e) {
            Timber.e(e);
        }
        return new JSONObject();
    }

    void addInfo(ReferralHelperInfo info) {
        referralsInfo.put(info.stepName, info);
    }

    @Override
    public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
        super.onJsonFormLoaded(jsonString, context, details);
        this.context = context;
    }

    @Override
    public String getPreProcessed() {
        try {
            Map<String, String> facilityOptions = LocationUtils.INSTANCE.getFacilitiesKeyAndName();
            String formName = "referral_facility_selection";
            JSONObject jsonForm = FormUtils.getFormUtils().getFormJson(formName);
            jsonForm.put("count", referralsInfo.size());
            String step1 = jsonForm.getJSONObject("step1").toString();

            int i = 1;
            for (ReferralHelperInfo info : referralsInfo.values()) {
                JSONObject step = new JSONObject(step1);
                JsonFormUtilsFlv.overwriteQuestionOptions("chw_referral_hf", facilityOptions, step);

                step.put("title", info.stepName);
                step.put("baseEntityId", info.baseEntityId);
                jsonForm.put("step" + (i++), step);

                if (i <= referralsInfo.size()) {
                    step.put("next", "step" + (i));
                }
            }
            return jsonForm.toString();
        } catch (JSONException e) {
            Timber.e(e);
        }
        return "";
    }

    private BaseAncHomeVisitAction.Status evaluateStatus(String formPayload) {
        BaseAncHomeVisitAction.Status state = null;
        JsonQ form = JsonQ.fromJson(formPayload);
        List<String> serviceBeforeReferral = form.getStrings("step*.fields[?(@.key=='service_before_referral')].value");
        List<String> facilities = form.getStrings("step*.fields[?(@.key=='chw_referral_hf')].value");
        List<Date> dates = form.get("step*.fields[?(@.key=='referral_appointment_date')]").dateColumn("value");

        for (String s : serviceBeforeReferral) {
            state = s.isEmpty() || state == PENDING ? PENDING : COMPLETED;
        }
        for (String s : facilities) {
            state = s.isEmpty() || state == PENDING ? PENDING : COMPLETED;
        }
        boolean noFacilities = state == null || state == PENDING;
        return noFacilities
                || facilities.size() != dates.size()
                || facilities.size() != serviceBeforeReferral.size() ? PENDING : COMPLETED;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            String formName = "referral_facility_selection";
            JSONObject jsonForm = FormUtils.getFormUtils().getFormJson(formName);

            JSONObject payload = new JSONObject(jsonPayload);
            payload.put("count", referralsInfo.size());
            int i = 1;
            for (ReferralHelperInfo info : referralsInfo.values()) {
                JSONObject step = payload.getJSONObject("step" + (i++));
                step.getJSONArray("fields").put(info.problem);
                jsonForm.put("step1", step);
            }
            status = evaluateStatus(jsonPayload);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {
        return "";
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        return status != null ? status : PENDING;
    }

    boolean noInfo() {
        return referralsInfo.isEmpty();
    }

    public void remove(String referralStepName) {
        referralsInfo.remove(referralStepName);
    }

    static class ReferralHelperInfo {

        private final String type;
        private final String baseEntityId;
        private final JSONObject problem;
        private String stepName;

        ReferralHelperInfo(String type, String baseEntityId, JSONObject problem) {
            this.type = type;
            this.baseEntityId = baseEntityId;
            this.problem = problem;
            this.stepName = "";
        }

        ReferralHelperInfo(String type, String baseEntityId, JSONObject problem, String stepName) {
            this(type, baseEntityId, problem);
            this.stepName = stepName;
        }
    }
}
