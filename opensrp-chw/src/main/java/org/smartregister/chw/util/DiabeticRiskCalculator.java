package org.smartregister.chw.util;

import android.content.Context;

import org.smartregister.chw.R;

import timber.log.Timber;

public class DiabeticRiskCalculator {

    public static final double INTERCEPT = 12.092196283;
    public static final double AGE_DIASTOLIC_BP_COEF = 0.001217652;
    public static final double AGE_COEF = 0.122336374;
    public static final double WAIST_COEF = 0.034389303;
    public static final double FAMILY_HISTORY_COEF = 0.604930794;
    public static final double SYSTOLIC_BP_COEF = 0.002469807;
    public static final double DIASTOLIC_BP_COEF = 0.061461351;

    public static double calculateDiabeticRiskScore(String age, String familyHistory, String waistCircumference,
                                                      String systolicBloodPressure, String diastolicBloodPressure) {
        if (isBlank(age) || isBlank(waistCircumference) || isBlank(familyHistory)
                || isBlank(systolicBloodPressure) || isBlank(diastolicBloodPressure)) {
            return 0.0;
        }

        try {
            double ageValue = Double.parseDouble(age);
            double waistValue = Double.parseDouble(waistCircumference);
            double familyHistoryValue = isPositiveResponse(familyHistory) ? 1.0 : 0.0;
            double systolicBPValue = Double.parseDouble(systolicBloodPressure);
            double diastolicBPValue = Double.parseDouble(diastolicBloodPressure);

            double linearComponent = -INTERCEPT
                    - (AGE_DIASTOLIC_BP_COEF * ageValue * diastolicBPValue)
                    + (AGE_COEF * ageValue)
                    + (WAIST_COEF * waistValue)
                    + (FAMILY_HISTORY_COEF * familyHistoryValue)
                    + (SYSTOLIC_BP_COEF * systolicBPValue)
                    + (DIASTOLIC_BP_COEF * diastolicBPValue);

            double exponentialComponent = Math.exp(linearComponent);

            return exponentialComponent / (1.0 + exponentialComponent);
        } catch (NumberFormatException e) {
            Timber.e(e, "Error calculating diabetes risk score");
            return 0.0;
        }

    }

    public static String getDiabesityRiskCondition(Context context, String riskScore, String systolicBloodPressure,
                                                   String diastolicBloodPressure) {
        double risk = parseToDouble(riskScore);
        double systolic = parseToDouble(systolicBloodPressure);
        double diastolic = parseToDouble(diastolicBloodPressure);

        if (Double.isNaN(risk) || Double.isNaN(systolic) || Double.isNaN(diastolic)) {
            return "Invalid input values";
        }

        if (risk < 0 || systolic < 0 || diastolic < 0) {
            return "Invalid input values";
        }

        double diabetesRiskCutoff = 0.09175944;

        if (risk >= diabetesRiskCutoff && (systolic >= 140 || diastolic >= 90)) {
            return context.getString(R.string.both_diabetes_and_hypertenstion);
        } else if (risk >= diabetesRiskCutoff) {
            return context.getString(R.string.only_diabetes);
        } else if (systolic >= 140 || diastolic >= 90) {
            return context.getString(R.string.only_hypertension);
        } else {
            return "no_risk";
        }
    }
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static double parseToDouble(String value) {
        if (isBlank(value)) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            Timber.e(e, "Error parsing numeric value: %s", value);
            return Double.NaN;
        }
    }

    private static boolean isPositiveResponse(String response) {
        if (response == null) {
            return false;
        }
        String normalized = response.trim().toLowerCase();
        return normalized.equals("yes")
                || normalized.equals("true")
                || normalized.equals("1")
                || normalized.equals("ndio")
                || normalized.equals("ndiyo");
    }

}
