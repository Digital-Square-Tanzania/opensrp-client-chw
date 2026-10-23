package org.smartregister.chw.fragment;

import android.os.Bundle;

/** Applied register filters and an independently saved dialog draft share this value object. */
public final class MotherMentorRegisterFilterState {
    private static final String ENABLED = "enabled";
    private static final String PREGNANT = "pregnant";
    private static final String BREASTFEEDING = "breastfeeding";
    private static final String UNDER_TWO = "under_two";
    private final boolean enabled;
    private final boolean pregnant;
    private final boolean breastfeeding;
    private final boolean underTwo;

    public MotherMentorRegisterFilterState(
            boolean enabled, boolean pregnant, boolean breastfeeding, boolean underTwo) {
        this.enabled = enabled;
        this.pregnant = pregnant;
        this.breastfeeding = breastfeeding;
        this.underTwo = underTwo;
    }

    public static MotherMentorRegisterFilterState allClients() {
        return new MotherMentorRegisterFilterState(false, false, false, false);
    }

    public static MotherMentorRegisterFilterState fromBundle(Bundle bundle) {
        return bundle == null
                ? allClients()
                : new MotherMentorRegisterFilterState(
                        bundle.getBoolean(ENABLED), bundle.getBoolean(PREGNANT),
                        bundle.getBoolean(BREASTFEEDING), bundle.getBoolean(UNDER_TWO));
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ENABLED, enabled);
        bundle.putBoolean(PREGNANT, pregnant);
        bundle.putBoolean(BREASTFEEDING, breastfeeding);
        bundle.putBoolean(UNDER_TWO, underTwo);
        return bundle;
    }

    public MotherMentorRegisterFilterState applied() {
        return enabled ? this : allClients();
    }

    public boolean isActive() {
        return enabled && (pregnant || breastfeeding || underTwo);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPregnant() {
        return pregnant;
    }

    public boolean isBreastfeeding() {
        return breastfeeding;
    }

    public boolean isUnderTwo() {
        return underTwo;
    }
}
