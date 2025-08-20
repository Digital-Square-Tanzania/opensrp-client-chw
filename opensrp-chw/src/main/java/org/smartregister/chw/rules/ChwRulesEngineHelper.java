package org.smartregister.chw.rules;

import com.vijay.jsonwizard.rules.RulesEngineHelper;

import org.smartregister.chw.util.Utils;

import org.smartregister.chw.util.DiabeticRiskCalculator;

public class ChwRulesEngineHelper extends RulesEngineHelper {

    public double getWFHZScore(String gender, String height, String weight) {
        return Utils.getWFHZScore(gender, height, weight);
    }

    public double getDiabesityRiskScore(String age, String waistCircumference, String familyHistory,
                                        String systolicBloodPressure, String diastolicBloodPressure) {
        return  DiabeticRiskCalculator.calculateDiabeticRiskScore(age, waistCircumference, familyHistory,
                systolicBloodPressure, diastolicBloodPressure);
    }
}
