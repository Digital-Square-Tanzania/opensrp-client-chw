package org.smartregister.chw.rules;

import android.content.Context;

import com.vijay.jsonwizard.rules.RulesEngineHelper;

import org.smartregister.chw.util.Utils;

import org.smartregister.chw.util.DiabeticRiskCalculator;

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
        timber.log.Timber.d("DiabRisk inputs received → age=[%s], familyHistory=[%s], waist=[%s], systolic=[%s], diastolic=[%s]",
                age, familyHistory, waistCircumference, systolicBloodPressure, diastolicBloodPressure);
        if (android.text.TextUtils.isEmpty(systolicBloodPressure)) {
            timber.log.Timber.w(new Throwable("STACK: systolic empty at calc time"),
                    "Systolic empty — capturing stack trace");
        }
        return DiabeticRiskCalculator.calculateDiabeticRiskScore(age, familyHistory, waistCircumference,
                systolicBloodPressure, diastolicBloodPressure);
    }

    public String getDiabesityRiskCondition(String riskScore, String systolicBloodPressure,
                                            String diastolicBloodPressure) {
        return DiabeticRiskCalculator.getDiabesityRiskCondition(context, riskScore, systolicBloodPressure,
                diastolicBloodPressure);
    }
}
