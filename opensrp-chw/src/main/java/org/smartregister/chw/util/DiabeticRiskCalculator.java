package org.smartregister.chw.util;

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
            double familyHistoryValue = familyHistory.equalsIgnoreCase("Yes") ? 1.0 : 0.0;
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
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

}
