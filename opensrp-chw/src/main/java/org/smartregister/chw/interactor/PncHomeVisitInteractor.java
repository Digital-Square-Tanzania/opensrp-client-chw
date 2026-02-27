package org.smartregister.chw.interactor;

import android.content.Context;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.contract.BaseAncHomeVisitContract;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.interactor.BaseAncHomeVisitInteractor;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.anc.util.VisitUtils;
import org.smartregister.chw.core.dao.PNCDao;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.chw.util.ReferralUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class PncHomeVisitInteractor extends BaseAncHomeVisitInteractor {

    private String motherID;
    private String parentVisitID;
    private Flavor flavor = new PncHomeVisitInteractorFlv();

    Context context;

    public PncHomeVisitInteractor(Context context) {
        this.context = context;
    }

    @Override
    public void calculateActions(final BaseAncHomeVisitContract.View view, final MemberObject memberObject, final BaseAncHomeVisitContract.InteractorCallBack callBack) {

        final Runnable runnable = () -> {
            // update the local database incase of manual date adjustment
            try {
                VisitUtils.processVisits(memberObject.getBaseEntityId());
            } catch (Exception e) {
                Timber.e(e);
            }

            final LinkedHashMap<String, BaseAncHomeVisitAction> actionList = new LinkedHashMap<>();

            try {
                for (Map.Entry<String, BaseAncHomeVisitAction> entry : flavor.calculateActions(view, memberObject, callBack).entrySet()) {
                    actionList.put(entry.getKey(), entry.getValue());
                }
            } catch (BaseAncHomeVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    @Override
    public void submitVisit(boolean editMode, String memberID, Map<String, BaseAncHomeVisitAction> map, BaseAncHomeVisitContract.InteractorCallBack callBack) {
        motherID = memberID;

        if (!editMode) {

            BaseAncHomeVisitAction facilitySelectionAction = map.get(context.getString(R.string.home_visit_facility_referral));

            if (facilitySelectionAction != null) {
                String facilitySelectionForm = facilitySelectionAction.getJsonPayload();
                if (facilitySelectionForm != null) {
                    try {

                        JSONObject facilitySelectionJsonObject = new JSONObject(facilitySelectionForm);
                        int facilitySelectionStepCount = facilitySelectionJsonObject.getInt(JsonFormConstants.COUNT);

                        BaseAncHomeVisitAction dangerSignsActionsMother = map.get(context.getString(R.string.pnc_danger_signs_mother));
                        assert dangerSignsActionsMother != null;
                        String dangerSignsForm = dangerSignsActionsMother.getJsonPayload();
                        assert dangerSignsForm != null;
                        JSONObject dangerSignsJsonObject = new JSONObject(dangerSignsForm);

                        String referralProblems = JsonFormUtils.getCheckBoxValue(dangerSignsJsonObject, "danger_signs_present_mama");

                        // If count is 1 then only mother had danger signs and requires referral otherwise baby/babies had danger signs and require referral
                        if (facilitySelectionStepCount == 1) {
                            ReferralUtils.processReferral(facilitySelectionForm, memberID, CoreConstants.TASKS_FOCUS.PNC_DANGER_SIGNS, referralProblems);
                        } else {

                            for (Map.Entry<String, BaseAncHomeVisitAction> actionEntry: map.entrySet()) {
                                // Process mother referral
                                if (!(actionEntry.getKey().equalsIgnoreCase(context.getString(R.string.pnc_hv_location)) ||
                                        actionEntry.getKey().equalsIgnoreCase(context.getString(R.string.home_visit_facility_referral)))) {
                                    if (actionEntry.getKey().equalsIgnoreCase(context.getString(R.string.pnc_danger_signs_mother))) {

                                        String motherReferralFacilitySelection = getReferralFacilitySelection(facilitySelectionJsonObject,
                                                facilitySelectionStepCount,
                                                actionEntry.getKey());
                                        ReferralUtils.processReferral(motherReferralFacilitySelection, memberID, CoreConstants.TASKS_FOCUS.PNC_DANGER_SIGNS, referralProblems);

                                    } else {
                                        // Process baby referral
                                        String babyBaseEntityId = actionEntry.getValue().getBaseEntityID();
                                        String babyDangerSignsForm = actionEntry.getValue().getJsonPayload();
                                        JSONObject babyDangerSignsJsonObject = new JSONObject(babyDangerSignsForm);
                                        String babyReferralProblems = JsonFormUtils.getCheckBoxValue(babyDangerSignsJsonObject, "danger_signs_present_child");
                                        String babyReferralFacilitySelection = getReferralFacilitySelection(facilitySelectionJsonObject,
                                                facilitySelectionStepCount,
                                                actionEntry.getKey());

                                        ReferralUtils.processReferral(babyReferralFacilitySelection, babyBaseEntityId, CoreConstants.TASKS_FOCUS.SICK_CHILD, babyReferralProblems);
                                    }
                                }
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

            }

        }

        super.submitVisit(editMode, memberID, map, callBack);
    }

    private String getReferralFacilitySelection(JSONObject facilitySelectionJsonObject, int stepCount, String actionTitle) throws JSONException {

        JSONObject stepJSONObject;

        for (int i = 1; i < stepCount; i++) {
            JSONObject stepObject = facilitySelectionJsonObject.getJSONObject("step" + i);
            if (stepObject.getString("title").equalsIgnoreCase(getStepTitle(actionTitle))) {
                stepJSONObject = facilitySelectionJsonObject.getJSONObject("step" + i);
                Iterator<String> keys = facilitySelectionJsonObject.keys();
                
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (key.startsWith("step") || key.equals("next")) {
                        keys.remove();
                    }
                }
                
                facilitySelectionJsonObject.put("step1", stepJSONObject);
            }
        }

        facilitySelectionJsonObject.put(JsonFormConstants.COUNT, 1);

        return facilitySelectionJsonObject.toString();

    }

    private String getStepTitle(String actionTitle) {
        return actionTitle.replaceAll("^.*?-(.*)", "$1-" + context.getString(R.string.home_visit_facility_referral));
    }

    @Override
    public MemberObject getMemberClient(String memberID) {
        // read all the member details from the database
        return PNCDao.getMember(memberID);
    }

    /**
     * For PNC events retain the 1st visit ID as the parent id for all the other events
     *
     * @param visit
     * @param parentEventType
     * @return
     */
    @Override
    protected String getParentVisitEventID(Visit visit, String parentEventType) {
        if (StringUtils.isBlank(parentEventType))
            parentVisitID = visit.getVisitId();

        return visit.getVisitId().equalsIgnoreCase(parentVisitID) ? null : parentVisitID;
    }

    /**
     * Injects implementation specific changes to the event
     *
     * @param baseEvent
     */
    protected void prepareEvent(Event baseEvent) {
        if (baseEvent != null) {
            // add anc date obs and last
            List<Object> list = new ArrayList<>();
            list.add(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
            baseEvent.addObs(new Obs("concept", "text", "pnc_visit_date", "",
                    list, new ArrayList<>(), null, "pnc_visit_date"));
        }
    }

    @Override
    protected void prepareSubEvent(Event baseEvent) {
        if (baseEvent != null) {
            List<Object> mother_id = new ArrayList<>();
            mother_id.add(motherID);
            baseEvent.addObs(new Obs("concept", "text", "pnc_mother_id", "",
                    mother_id, new ArrayList<>(), null, "pnc_mother_id"));
        }
    }

    @Override
    protected String getEncounterType() {
        return Constants.EVENT_TYPE.PNC_HOME_VISIT;
    }

    @Override
    protected String getTableName() {
        return Constants.TABLES.PREGNANCY_OUTCOME;
    }

    public interface Flavor {
        LinkedHashMap<String, BaseAncHomeVisitAction> calculateActions(final BaseAncHomeVisitContract.View view, MemberObject memberObject, final BaseAncHomeVisitContract.InteractorCallBack callBack) throws BaseAncHomeVisitAction.ValidationException;
    }
}
