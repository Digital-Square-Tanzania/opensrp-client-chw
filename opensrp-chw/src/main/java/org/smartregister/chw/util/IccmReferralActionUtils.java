package org.smartregister.chw.util;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.R;
import org.smartregister.chw.actionhelper.IccmReferralActionHelper;
import org.smartregister.chw.malaria.domain.IccmMemberObject;
import org.smartregister.chw.malaria.domain.VisitDetail;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class IccmReferralActionUtils {

    private IccmReferralActionUtils() {
        // Utility class
    }

    public static void updateReferralAction(Context context,
                                            IccmMemberObject memberObject,
                                            String enrollmentFormSubmissionId,
                                            Map<String, BaseIccmVisitAction> actionList,
                                            Map<String, List<VisitDetail>> details,
                                            boolean shouldRetainReferralAction,
                                            boolean addIfMissing) {
        IccmVisitStateTracker.getInstance().setIccmReferralModuleActive(shouldRetainReferralAction);

        String title = context.getString(R.string.iccm_referral);
        if (!shouldRetainReferralAction) {
            actionList.remove(title);
            return;
        }

        if (!addIfMissing || actionList.containsKey(title) || memberObject == null) {
            return;
        }

        try {
            IccmReferralActionHelper referralActionHelper = new IccmReferralActionHelper(
                    enrollmentFormSubmissionId,
                    actionList,
                    memberObject.getAge(),
                    memberObject.getGender()
            );
            BaseIccmVisitAction action = new BaseIccmVisitAction.Builder(context, title)
                    .withOptional(true)
                    .withHelper(referralActionHelper)
                    .withDetails(details)
                    .withBaseEntityID(memberObject.getBaseEntityId())
                    .withFormName(Constants.JsonForm.getIccmReferral())
                    .build();
            actionList.put(title, action);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public static boolean shouldKeepReferralFromDiarrhea(String isPneumoniaSuspect,
                                                         String clientPastMalariaTreatmentHistory,
                                                         String diarrheaSigns) {
        return hasAnyReferralTrigger(
                isTrue(isPneumoniaSuspect),
                isYes(clientPastMalariaTreatmentHistory),
                hasMeaningfulSelection(diarrheaSigns, "none")
        );
    }

    public static boolean shouldKeepReferralFromMalaria(String isPneumoniaSuspect,
                                                        String interpretationForMrdtTwo,
                                                        String diarrheaSigns,
                                                        String ableConductMrdtTest,
                                                        String isClientPregnant) {
        return hasAnyReferralTrigger(
                isTrue(isPneumoniaSuspect),
                hasMeaningfulSelection(interpretationForMrdtTwo, "control"),
                hasMeaningfulSelection(diarrheaSigns, "none"),
                isNo(ableConductMrdtTest),
                isYes(isClientPregnant) && hasAnsweredMrdtConductQuestion(ableConductMrdtTest)
        );
    }

    public static boolean shouldKeepReferralFromPhysicalExamination(int age,
                                                                    Constants.PneumoniaStatus pneumoniaStatus,
                                                                    String isPneumoniaSuspect,
                                                                    String clientPastMalariaTreatmentHistory) {
        boolean pneumoniaReferralTrigger = age <= 6
                && pneumoniaStatus == Constants.PneumoniaStatus.DISABLED
                && isTrue(isPneumoniaSuspect);
        return hasAnyReferralTrigger(
                pneumoniaReferralTrigger,
                isYes(clientPastMalariaTreatmentHistory)
        );
    }

    public static boolean isTrue(String value) {
        return "true".equalsIgnoreCase(StringUtils.trimToEmpty(value));
    }

    public static boolean isYes(String value) {
        return "yes".equalsIgnoreCase(StringUtils.trimToEmpty(value));
    }

    public static boolean isNo(String value) {
        return "no".equalsIgnoreCase(StringUtils.trimToEmpty(value));
    }

    static boolean hasAnsweredMrdtConductQuestion(String value) {
        return isYes(value) || isNo(value);
    }

    static boolean hasMeaningfulSelection(String value, String ignoredSelection) {
        String normalizedValue = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(normalizedValue)
                || "[]".equals(normalizedValue)
                || "{}".equals(normalizedValue)
                || "null".equalsIgnoreCase(normalizedValue)) {
            return false;
        }

        return StringUtils.isBlank(ignoredSelection)
                || !StringUtils.containsIgnoreCase(normalizedValue, ignoredSelection);
    }

    private static boolean hasAnyReferralTrigger(boolean... referralTriggers) {
        if (referralTriggers == null) {
            return false;
        }

        for (boolean referralTrigger : referralTriggers) {
            if (referralTrigger) {
                return true;
            }
        }

        return false;
    }
}
