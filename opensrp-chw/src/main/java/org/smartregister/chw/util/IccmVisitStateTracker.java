package org.smartregister.chw.util;

public class IccmVisitStateTracker {

    private static IccmVisitStateTracker instance;

    private boolean iccmReferralModuleActive = true;

    private IccmVisitStateTracker() {

    }

    public static synchronized IccmVisitStateTracker getInstance() {
        if (instance == null) {
            instance = new IccmVisitStateTracker();
        }
        return instance;
    }

    public synchronized boolean getIsIccmReferralModouleActive() {
        return iccmReferralModuleActive;
    }

    public synchronized void setIccmReferralModuleActive(boolean iccmReferralModuleActive) {
        this.iccmReferralModuleActive = iccmReferralModuleActive;
    }
}
