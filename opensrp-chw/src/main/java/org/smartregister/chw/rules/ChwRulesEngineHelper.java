package org.smartregister.chw.rules;

import android.content.Context;

import com.vijay.jsonwizard.rules.RulesEngineHelper;

import org.smartregister.chw.util.DiabeticRiskCalculator;
import org.smartregister.chw.util.Utils;

public class ChwRulesEngineHelper extends RulesEngineHelper {

    private final Context context;

    public ChwRulesEngineHelper(Context context) {
        this.context = context;
    }

    public double getWFHZScore(String gender, String height, String weight) {
        return Utils.getWFHZScore(gender, height, weight);
    }

    public double getDiabesityRiskScore(String age, String familyHistory, String waistCircumference,
                                        String systolicBloodPressure, String diastolicBloodPressure) {
        return DiabeticRiskCalculator.calculateDiabeticRiskScore(age, familyHistory, waistCircumference,
                systolicBloodPressure, diastolicBloodPressure);
    }

    public String getDiabesityRiskCondition(String riskScore, String systolicBloodPressure,
                                            String diastolicBloodPressure) {
        return DiabeticRiskCalculator.getDiabesityRiskCondition(context, riskScore, systolicBloodPressure,
                diastolicBloodPressure);
    }
}
