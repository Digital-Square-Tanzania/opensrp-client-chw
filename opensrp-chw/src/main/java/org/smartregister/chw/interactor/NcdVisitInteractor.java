package org.smartregister.chw.interactor;

import static org.smartregister.chw.util.Constants.JsonForm.getNcdClientEducationForm;
import static org.smartregister.chw.util.Constants.JsonForm.getNcdVitalsForm;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.R;
import org.smartregister.chw.actionhelper.NcdClientEducationActionHelper;
import org.smartregister.chw.actionhelper.NcdVitalsActionHelper;
import org.smartregister.chw.ncd.contract.BaseNcdVisitContract;
import org.smartregister.chw.ncd.interactor.BaseNcdVisitInteractor;
import org.smartregister.chw.ncd.model.BaseNcdVisitAction;
import org.smartregister.chw.ncd.util.Constants;

import java.util.LinkedHashMap;

import timber.log.Timber;

public class NcdVisitInteractor extends BaseNcdVisitInteractor {

    private static final String ENCOUNTER_TYPE = "Diabetes and Hypertension Screening Followup";
    private static final String TABLE_NAME = "ec_diabetes_hypertension_followup";

    public NcdVisitInteractor() {
        super(Constants.EVENT_TYPE.NCD_FOLLOW_UP_VISIT);
    }

    @Override
    protected String getEncounterType() {
        return ENCOUNTER_TYPE;
    }

    @Override
    protected String getTableName() {
        return TABLE_NAME;
    }

    @Override
    protected void populateActionList(@NonNull BaseNcdVisitContract.InteractorCallBack callback) {
        Runnable task = () -> {
            LinkedHashMap<String, BaseNcdVisitAction> actionMap = new LinkedHashMap<>();

            try {
                BaseNcdVisitAction vitals = buildVitalsAction();
                BaseNcdVisitAction clientEducation = buildClientEducationAction();

                actionMap.put(context.getString(R.string.ncd_visit_action_diabetes_title), vitals);
                actionMap.put(context.getString(R.string.ncd_visit_action_hypertension_title), clientEducation);
            } catch (BaseNcdVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callback.preloadActions(actionMap));
        };

        appExecutors.diskIO().execute(task);
    }

    private BaseNcdVisitAction buildVitalsAction() throws BaseNcdVisitAction.ValidationException {
        BaseNcdVisitAction.Builder builder = getBuilder(context.getString(R.string.ncd_visit_action_diabetes_title))
                .withOptional(false)
                .withScheduleStatus(BaseNcdVisitAction.ScheduleStatus.DUE)
                .withFormName(getNcdVitalsForm())
                .withDetails(details)
                .withHelper(new NcdVitalsActionHelper())
                .withProcessingMode(BaseNcdVisitAction.ProcessingMode.SEPARATE);

        if (memberObject != null && StringUtils.isNotBlank(memberObject.getBaseEntityId())) {
            builder = builder.withBaseEntityID(memberObject.getBaseEntityId());
        }

        return builder.build();
    }

    private BaseNcdVisitAction buildClientEducationAction() throws BaseNcdVisitAction.ValidationException {
        BaseNcdVisitAction.Builder builder = getBuilder(context.getString(R.string.ncd_visit_action_hypertension_title))
                .withOptional(false)
                .withScheduleStatus(BaseNcdVisitAction.ScheduleStatus.DUE)
                .withFormName(getNcdClientEducationForm())
                .withDetails(details)
                .withHelper(new NcdClientEducationActionHelper())
                .withProcessingMode(BaseNcdVisitAction.ProcessingMode.SEPARATE);

        if (memberObject != null && StringUtils.isNotBlank(memberObject.getBaseEntityId())) {
            builder = builder.withBaseEntityID(memberObject.getBaseEntityId());
        }

        return builder.build();
    }
}
