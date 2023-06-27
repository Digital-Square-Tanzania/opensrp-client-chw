package org.smartregister.chw.interactor;

import static com.vijay.jsonwizard.widgets.TimePickerFactory.KEY.VALUE;
import static org.smartregister.AllConstants.OPTIONS;
import static org.smartregister.client.utils.constants.JsonFormConstants.KEY;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.actionhelper.KvpPrEPPreventiveServicesActionHelper;
import org.smartregister.chw.actionhelper.KvpPrEPReferralServicesActionHelper;
import org.smartregister.chw.actionhelper.KvpPrEPSbccServicesActionHelper;
import org.smartregister.chw.actionhelper.KvpPrEPStructuralServicesActionHelper;
import org.smartregister.chw.actionhelper.KvpPrEPVisitTypeActionHelper;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.kvp.contract.BaseKvpVisitContract;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.interactor.BaseKvpVisitInteractor;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.referral.util.JsonFormConstants;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class KvpPrEPVisitInteractor extends BaseKvpVisitInteractor {
    @Override
    protected void populateActionList(BaseKvpVisitContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            try {
                evaluateVisitType(details);
                evaluateSBCCServices(details);
                evaluatePreventiveServices(details);
                evaluateStructuralServices(details);
                evaluateReferralServices(details);
            } catch (BaseKvpVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void evaluateVisitType(Map<String, List<VisitDetail>> details) throws BaseKvpVisitAction.ValidationException {

        KvpPrEPVisitTypeActionHelper actionHelper = new KvpPrEPVisitTypeActionHelper(memberObject.getBaseEntityId());
        BaseKvpVisitAction action = getBuilder(context.getString(R.string.kvp_prep_visit_type))
                .withOptional(false)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.KVP_PrEP_FOLLOWUP_FORMS.VISIT_TYPE)
                .build();

        actionList.put(context.getString(R.string.kvp_prep_visit_type), action);
    }

    private void evaluateSBCCServices(Map<String, List<VisitDetail>> details) throws BaseKvpVisitAction.ValidationException {

        JSONObject sbccServices = null;
        try {
            sbccServices = FormUtils.getFormUtils().getFormJson(Constants.KVP_PrEP_FOLLOWUP_FORMS.SBCC_SERVICES);

            if (details != null && !details.isEmpty()) {
                JSONArray fields = sbccServices.getJSONObject(org.smartregister.chw.util.Constants.JsonFormConstants.STEP1)
                        .getJSONArray(JsonFormConstants.FIELDS);

                JSONObject sbccServicesOffered = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "sbcc_services_offered");
                JSONArray options = sbccServicesOffered.getJSONArray(OPTIONS);

                JSONArray values = new JSONArray();
                if (!details.isEmpty() && details.containsKey("sbcc_services_offered")) {
                    List<VisitDetail> visitDetails = details.get("sbcc_services_offered");
                    for (VisitDetail visitDetail : visitDetails) {
                        for (int i = 0; i < options.length(); i++) {
                            if ((options.getJSONObject(i)).getString(KEY).equals(visitDetail.getDetails())) {
                                values.put(options.getJSONObject(i));
                                break;
                            }
                        }
                    }
                }
                sbccServicesOffered.put(VALUE, values.toString());
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        KvpPrEPSbccServicesActionHelper actionHelper = new KvpPrEPSbccServicesActionHelper();
        BaseKvpVisitAction action = getBuilder(context.getString(R.string.kvp_prep_sbcc_services))
                .withOptional(false)
                .withJsonPayload(sbccServices.toString())
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.KVP_PrEP_FOLLOWUP_FORMS.SBCC_SERVICES)
                .build();

        actionList.put(context.getString(R.string.kvp_prep_sbcc_services), action);
    }

    private void evaluatePreventiveServices(Map<String, List<VisitDetail>> details) throws BaseKvpVisitAction.ValidationException {
        KvpPrEPPreventiveServicesActionHelper actionHelper = new KvpPrEPPreventiveServicesActionHelper(memberObject.getBaseEntityId());
        BaseKvpVisitAction action = getBuilder(context.getString(R.string.kvp_prep_preventive_services))
                .withOptional(true)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.KVP_PrEP_FOLLOWUP_FORMS.PREVENTIVE_SERVICES)
                .build();

        actionList.put(context.getString(R.string.kvp_prep_preventive_services), action);
    }

    private void evaluateStructuralServices(Map<String, List<VisitDetail>> details) throws BaseKvpVisitAction.ValidationException {

        KvpPrEPStructuralServicesActionHelper actionHelper = new KvpPrEPStructuralServicesActionHelper();
        BaseKvpVisitAction action = getBuilder(context.getString(R.string.kvp_prep_structural_services))
                .withOptional(true)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.KVP_PrEP_FOLLOWUP_FORMS.STRUCTURAL_SERVICES)
                .build();

        actionList.put(context.getString(R.string.kvp_prep_structural_services), action);
    }

    private void evaluateReferralServices(Map<String, List<VisitDetail>> details) throws BaseKvpVisitAction.ValidationException {

        KvpPrEPReferralServicesActionHelper actionHelper = new KvpPrEPReferralServicesActionHelper();
        BaseKvpVisitAction action = getBuilder(context.getString(R.string.kvp_prep_referral_services))
                .withOptional(true)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.KVP_PrEP_FOLLOWUP_FORMS.REFERRAL_SERVICES)
                .build();

        actionList.put(context.getString(R.string.kvp_prep_referral_services), action);
    }

}
