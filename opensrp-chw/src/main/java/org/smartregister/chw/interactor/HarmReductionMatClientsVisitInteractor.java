package org.smartregister.chw.interactor;

import org.smartregister.chw.R;
import org.smartregister.chw.actionhelper.HarmReductionMatClientsFollowupActionHelper;
import org.smartregister.chw.harmreduction.contract.BaseHarmReductionVisitContract;
import org.smartregister.chw.harmreduction.domain.VisitDetail;
import org.smartregister.chw.harmreduction.interactor.BaseHarmReductionVisitInteractor;
import org.smartregister.chw.harmreduction.model.BaseHarmReductionVisitAction;
import org.smartregister.chw.harmreduction.util.Constants;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class HarmReductionMatClientsVisitInteractor extends BaseHarmReductionVisitInteractor {
    private static final String MAT_CLIENTS_FOLLOWUP_EVENT = "Harm Reduction MAT Clients Followup";

    public HarmReductionMatClientsVisitInteractor() {
        super(MAT_CLIENTS_FOLLOWUP_EVENT);
    }

    @Override
    protected void populateActionList(BaseHarmReductionVisitContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            try {
                evaluateMatClientsFollowup(details);
            } catch (BaseHarmReductionVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    @Override
    protected String getEncounterType() {
        return MAT_CLIENTS_FOLLOWUP_EVENT;
    }

    private void evaluateMatClientsFollowup(Map<String, List<VisitDetail>> details)
            throws BaseHarmReductionVisitAction.ValidationException {
        HarmReductionMatClientsFollowupActionHelper actionHelper = new HarmReductionMatClientsFollowupActionHelper();
        BaseHarmReductionVisitAction action = getBuilder(context.getString(R.string.harm_reduction_mat_clients_followup_visit))
                .withOptional(false)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.FORMS.HARM_REDUCTION_MAT_FOLLOWUP)
                .build();
        actionList.put(context.getString(R.string.harm_reduction_mat_clients_followup_visit), action);
    }
}
