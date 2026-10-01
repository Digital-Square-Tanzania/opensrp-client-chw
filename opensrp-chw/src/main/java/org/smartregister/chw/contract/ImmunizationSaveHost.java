package org.smartregister.chw.contract;

/** Routes a vaccination draft to its original visit action, including after recreation. */
public interface ImmunizationSaveHost {
    boolean saveImmunization(String visitId, String actionId, String childId, String requestId, String payload);

    interface Dialog {
        boolean applyAcceptedPayload(String payload);

        void bindToVisitAction(String visitId, String actionId, String requestId, String acceptedPayload);
    }
}
