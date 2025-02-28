package org.smartregister.chw.interactor;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.anc.contract.BaseAncHomeVisitContract;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.interactor.BaseAncHomeVisitInteractor;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.anc.util.VisitUtils;
import org.smartregister.chw.core.dao.PNCDao;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

public class PncHomeVisitInteractor extends BaseAncHomeVisitInteractor {

    private String motherID;
    private String parentVisitID;
    private Flavor flavor = new PncHomeVisitInteractorFlv();

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
        super.submitVisit(editMode, memberID, map, callBack);
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

            List<Object> ecd_modules_present = new ArrayList<>();
            ecd_modules_present.add(areEcdModulesPresent(baseEvent));

            baseEvent.addObs(new Obs("concept", "text", "ecd_modules_present", "",
                    ecd_modules_present, ecd_modules_present, null, "ecd_modules_present"));
        }
    }

    private static boolean areEcdModulesPresent(Event baseEvent) {
        for (Obs obs : baseEvent.getObs()) {
            String fieldCode = obs.getFieldCode();
            if (fieldCode != null && (
                    fieldCode.equals("ccd_development_screening_assessment_module") ||
                            fieldCode.equals("child_development_issues") ||
                            fieldCode.equals("ccd_introduction_module") ||
                            fieldCode.equals("ccd_communication_assessment_module") ||
                            fieldCode.equals("ccd_problem_solving_module") ||
                            fieldCode.equals("ccd_caregiver_responsiveness_module") ||
                            fieldCode.equals("ccd_play_assessment_counselling_module"))) {
                return true;
            }
        }
        return false;
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
