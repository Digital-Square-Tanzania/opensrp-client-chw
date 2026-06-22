package org.smartregister.chw.util;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HpsDeathRegistrationSummaryUtil {

    static final String CAUSE_OF_DEATH = "cause_of_death";
    static final String CAUSE_OF_DEATH_SPECIFY = "cause_of_death_specify";
    static final String OTHER = "other";

    private HpsDeathRegistrationSummaryUtil() {
    }

    public static LinkedHashMap<String, String> getDisplayValues(Map<String, String> visitDetails, String otherCauseLabel) {
        LinkedHashMap<String, String> displayValues = new LinkedHashMap<>();
        if (visitDetails == null || visitDetails.isEmpty()) {
            return displayValues;
        }

        displayValues.putAll(visitDetails);

        String causeOfDeath = StringUtils.trimToEmpty(displayValues.get(CAUSE_OF_DEATH));
        String specifiedCauseOfDeath = StringUtils.trimToEmpty(displayValues.get(CAUSE_OF_DEATH_SPECIFY));

        if (StringUtils.isBlank(causeOfDeath) && StringUtils.isNotBlank(specifiedCauseOfDeath)) {
            displayValues.put(CAUSE_OF_DEATH, specifiedCauseOfDeath);
        } else if (OTHER.equalsIgnoreCase(causeOfDeath)) {
            displayValues.put(CAUSE_OF_DEATH, StringUtils.defaultIfBlank(specifiedCauseOfDeath, otherCauseLabel));
        }

        displayValues.remove(CAUSE_OF_DEATH_SPECIFY);

        return displayValues;
    }
}
