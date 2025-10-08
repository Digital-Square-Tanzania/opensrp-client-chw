package org.smartregister.chw.presenter;

import org.smartregister.chw.ncd.contract.NcdRegisterContract;
import org.smartregister.chw.ncd.contract.NcdRegisterFragmentContract;
import org.smartregister.chw.ncd.presenter.BaseNcdRegisterFragmentPresenter;

public class NcdRegisterFragmentPresenter extends BaseNcdRegisterFragmentPresenter {

    private static final double DIABETES_RISK_THRESHOLD = 0.09175944d;
    private static final int SYSTOLIC_BP_THRESHOLD = 140;
    private static final int DIASTOLIC_BP_THRESHOLD = 90;

    public NcdRegisterFragmentPresenter(NcdRegisterFragmentContract.View view, NcdRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainCondition() {
        String mainTable = this.getMainTable();
        String riskScoreExpr = "CAST(IFNULL(NULLIF(" + mainTable + ".risk_score,''),'0') AS REAL) >= " + DIABETES_RISK_THRESHOLD;
        String systolicExpr = "CAST(IFNULL(NULLIF(" + mainTable + ".systolic_bp,''),'0') AS REAL) >= " + SYSTOLIC_BP_THRESHOLD;
        String diastolicExpr = "CAST(IFNULL(NULLIF(" + mainTable + ".diastolic_bp,''),'0') AS REAL) >= " + DIASTOLIC_BP_THRESHOLD;

        return " " + mainTable + ".is_closed = 0 AND dhf.base_entity_id is null AND dhc.base_entity_id is null"
                + " AND (" + riskScoreExpr + " OR " + systolicExpr + " OR " + diastolicExpr + ") ";
    }
}
