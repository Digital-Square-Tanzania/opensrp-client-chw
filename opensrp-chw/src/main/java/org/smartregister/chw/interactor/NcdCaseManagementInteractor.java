package org.smartregister.chw.interactor;

import static org.smartregister.chw.util.Constants.JsonForm.NCD_FOLLOWUP_CLINICAL_ADHERENCE;
import static org.smartregister.chw.util.Constants.JsonForm.NCD_FOLLOWUP_DANGER_SIGNS;
import static org.smartregister.chw.util.Constants.JsonForm.NCD_FOLLOWUP_LIFESTYLE;
import static org.smartregister.chw.util.Constants.JsonForm.NCD_FOLLOWUP_PSYCHOSOCIAL;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.R;
import org.smartregister.chw.actionhelper.NcdClinicalAdherenceActionHelper;
import org.smartregister.chw.actionhelper.NcdDangerSignsActionHelper;
import org.smartregister.chw.actionhelper.NcdLifestyleActionHelper;
import org.smartregister.chw.actionhelper.NcdPsychosocialActionHelper;
import org.smartregister.chw.ncd.contract.BaseNcdVisitContract;
import org.smartregister.chw.ncd.interactor.BaseNcdVisitInteractor;
import org.smartregister.chw.ncd.model.BaseNcdVisitAction;
import org.smartregister.chw.util.Constants;

import java.util.LinkedHashMap;

import timber.log.Timber;

public class NcdCaseManagementInteractor extends BaseNcdVisitInteractor {

    public NcdCaseManagementInteractor() {
        super(Constants.EncounterType.NCD_MONTHLY_FOLLOWUP);
    }

    @Override
    protected String getEncounterType() {
        return Constants.EncounterType.NCD_MONTHLY_FOLLOWUP;
    }

    @Override
    protected String getTableName() {
        return Constants.TableName.NCD_CASE_MANAGEMENT_FOLLOWUP;
    }

    @Override
    protected void populateActionList(@NonNull BaseNcdVisitContract.InteractorCallBack callback) {
        Runnable task = () -> {
            LinkedHashMap<String, BaseNcdVisitAction> actionMap = new LinkedHashMap<>();

            try {
                BaseNcdVisitAction clinicalAdherence = buildAction(
                        context.getString(R.string.ncd_followup_action_clinical_adherence),
                        NCD_FOLLOWUP_CLINICAL_ADHERENCE,
                        new NcdClinicalAdherenceActionHelper());

                BaseNcdVisitAction dangerSigns = buildAction(
                        context.getString(R.string.ncd_followup_action_danger_signs),
                        NCD_FOLLOWUP_DANGER_SIGNS,
                        new NcdDangerSignsActionHelper());

                BaseNcdVisitAction lifestyle = buildAction(
                        context.getString(R.string.ncd_followup_action_lifestyle),
                        NCD_FOLLOWUP_LIFESTYLE,
                        new NcdLifestyleActionHelper());

                BaseNcdVisitAction psychosocial = buildAction(
                        context.getString(R.string.ncd_followup_action_psychosocial),
                        NCD_FOLLOWUP_PSYCHOSOCIAL,
                        new NcdPsychosocialActionHelper());

                actionMap.put(context.getString(R.string.ncd_followup_action_clinical_adherence), clinicalAdherence);
                actionMap.put(context.getString(R.string.ncd_followup_action_danger_signs), dangerSigns);
                actionMap.put(context.getString(R.string.ncd_followup_action_lifestyle), lifestyle);
                actionMap.put(context.getString(R.string.ncd_followup_action_psychosocial), psychosocial);
            } catch (BaseNcdVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callback.preloadActions(actionMap));
        };

        appExecutors.diskIO().execute(task);
    }

    private BaseNcdVisitAction buildAction(String title, String formName,
                                           BaseNcdVisitAction.NcdVisitActionHelper helper)
            throws BaseNcdVisitAction.ValidationException {

        BaseNcdVisitAction.Builder builder = getBuilder(title)
                .withOptional(false)
                .withScheduleStatus(BaseNcdVisitAction.ScheduleStatus.DUE)
                .withFormName(formName)
                .withDetails(details)
                .withHelper(helper)
                .withProcessingMode(BaseNcdVisitAction.ProcessingMode.SEPARATE);

        if (memberObject != null && StringUtils.isNotBlank(memberObject.getBaseEntityId())) {
            builder = builder.withBaseEntityID(memberObject.getBaseEntityId());
        }

        return builder.build();
    }
}
