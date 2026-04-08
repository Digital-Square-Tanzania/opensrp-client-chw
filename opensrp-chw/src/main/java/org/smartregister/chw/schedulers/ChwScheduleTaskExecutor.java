package org.smartregister.chw.schedulers;

import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.contract.ScheduleService;
import org.smartregister.chw.core.schedulers.ScheduleTaskExecutor;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fp.util.FamilyPlanningConstants;
import org.smartregister.chw.malaria.util.Constants;
import org.smartregister.chw.task.ANCVisitScheduler;
import org.smartregister.chw.task.ChildHomeVisitScheduler;
import org.smartregister.chw.task.FamilyKitCheckScheduler;
import org.smartregister.chw.task.FpVisitScheduler;
import org.smartregister.chw.task.HivVisitScheduler;
import org.smartregister.chw.task.MalariaScheduler;
import org.smartregister.chw.task.NcdCaseManagementVisitScheduler;
import org.smartregister.chw.task.PNCVisitScheduler;
import org.smartregister.chw.task.RoutineHouseHoldVisitScheduler;
import org.smartregister.chw.task.TbVisitScheduler;
import org.smartregister.chw.task.WashCheckScheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChwScheduleTaskExecutor extends ScheduleTaskExecutor {

    private static ChwScheduleTaskExecutor scheduleTaskExecutor;

    protected ChwScheduleTaskExecutor() {
        //scheduleServiceMap.put();
    }

    public static ChwScheduleTaskExecutor getInstance() {
        if (scheduleTaskExecutor == null) {
            scheduleTaskExecutor = new ChwScheduleTaskExecutor();
        }
        return scheduleTaskExecutor;
    }

    @Override
    protected Map<String, List<ScheduleService>> getClassifier() {
        if (scheduleServiceMap == null || scheduleServiceMap.size() == 0) {
            scheduleServiceMap = new HashMap<>();

            initializeChildClassifier(scheduleServiceMap);

            if (ChwApplication.getApplicationFlavor().hasANC())
                initializeANCClassifier(scheduleServiceMap);

            if (ChwApplication.getApplicationFlavor().hasPNC())
                initializePNCClassifier(scheduleServiceMap);

            initializeMalariaClassifier(scheduleServiceMap);

            if (ChwApplication.getApplicationFlavor().hasWashCheck())
                initializeWashClassifier(scheduleServiceMap);

            if (ChwApplication.getApplicationFlavor().hasFamilyKitCheck())
                initializeFamilyKitClassifier(scheduleServiceMap);

            if (ChwApplication.getApplicationFlavor().hasFamilyPlanning())
                initializeFPClassifier(scheduleServiceMap);

            if (ChwApplication.getApplicationFlavor().hasTB())
                initializeTBClassifier(scheduleServiceMap);

            if (ChwApplication.getApplicationFlavor().hasHIV())
                initializeHIVClassifier(scheduleServiceMap);

            if (ChwApplication.getApplicationFlavor().hasRoutineVisit())
                initializeRoutineHouseholdClassifier(scheduleServiceMap);

            initializeNcdCaseManagementClassifier(scheduleServiceMap);

        }
        return scheduleServiceMap;
    }

    private void addToClassifers(String eventType, Map<String, List<ScheduleService>> classifier, List<ScheduleService> scheduleServices) {
        List<ScheduleService> services = classifier.get(eventType);
        if (services == null)
            services = new ArrayList<>();

        services.addAll(scheduleServices);
        classifier.put(eventType, services);
    }

    private void initializeChildClassifier(Map<String, List<ScheduleService>> classifier) {
        List<ScheduleService> scheduleServices = new ArrayList<>();
        scheduleServices.add(new ChildHomeVisitScheduler());

        addToClassifers(CoreConstants.EventType.CHILD_HOME_VISIT, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.CHILD_VISIT_NOT_DONE, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.CHILD_REGISTRATION, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.UPDATE_CHILD_REGISTRATION, classifier, scheduleServices);
    }

    private void initializeANCClassifier(Map<String, List<ScheduleService>> classifier) {
        List<ScheduleService> scheduleServices = new ArrayList<>();
        scheduleServices.add(new ANCVisitScheduler());

        addToClassifers(CoreConstants.EventType.ANC_REGISTRATION, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.ANC_HOME_VISIT, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.ANC_HOME_VISIT_NOT_DONE, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.ANC_HOME_VISIT_NOT_DONE_UNDO, classifier, scheduleServices);
    }

    private void initializePNCClassifier(Map<String, List<ScheduleService>> classifier) {
        List<ScheduleService> scheduleServices = new ArrayList<>();
        scheduleServices.add(new PNCVisitScheduler());

        addToClassifers(CoreConstants.EventType.PREGNANCY_OUTCOME, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.PNC_REGISTRATION, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.PNC_HOME_VISIT, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.PNC_HOME_VISIT_NOT_DONE, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.PNC_HOME_VISIT_NOT_DONE_UNDO, classifier, scheduleServices);
    }

    private void initializeMalariaClassifier(Map<String, List<ScheduleService>> classifier) {
        List<ScheduleService> scheduleServices = new ArrayList<>();
        scheduleServices.add(new MalariaScheduler());
        addToClassifers(Constants.EVENT_TYPE.MALARIA_FOLLOW_UP_VISIT, classifier, scheduleServices);
        addToClassifers(Constants.EVENT_TYPE.MALARIA_CONFIRMATION, classifier, scheduleServices);
    }

    private void initializeWashClassifier(Map<String, List<ScheduleService>> classifier) {
        List<ScheduleService> scheduleServices = new ArrayList<>();
        scheduleServices.add(new WashCheckScheduler());

        addToClassifers(CoreConstants.EventType.FAMILY_REGISTRATION, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.UPDATE_FAMILY_REGISTRATION, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.WASH_CHECK, classifier, scheduleServices);
    }

    private void initializeFamilyKitClassifier(Map<String, List<ScheduleService>> classifier) {
        List<ScheduleService> scheduleServices = new ArrayList<>();
        scheduleServices.add(new FamilyKitCheckScheduler());

        addToClassifers(CoreConstants.EventType.FAMILY_REGISTRATION, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.UPDATE_FAMILY_REGISTRATION, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.UPDATE_CHILD_REGISTRATION, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.FAMILY_KIT, classifier, scheduleServices);
    }

    private void initializeFPClassifier(Map<String, List<ScheduleService>> classifier) {
        List<ScheduleService> scheduleServices = new ArrayList<>();
        scheduleServices.add(new FpVisitScheduler());
        addToClassifers(FamilyPlanningConstants.EVENT_TYPE.FP_FOLLOW_UP_VISIT, classifier, scheduleServices);
        addToClassifers(FamilyPlanningConstants.EVENT_TYPE.FP_REGISTRATION, classifier, scheduleServices);
    }

    private void initializeHIVClassifier(Map<String, List<ScheduleService>> classifier) {
        List<ScheduleService> scheduleServices = new ArrayList<>();
        scheduleServices.add(new HivVisitScheduler());
        addToClassifers(org.smartregister.chw.hiv.util.Constants.EventType.FOLLOW_UP_VISIT, classifier, scheduleServices);
        addToClassifers(org.smartregister.chw.hiv.util.Constants.EventType.REGISTRATION, classifier, scheduleServices);
    }

    private void initializeTBClassifier(Map<String, List<ScheduleService>> classifier) {
        List<ScheduleService> scheduleServices = new ArrayList<>();
        scheduleServices.add(new TbVisitScheduler());
        addToClassifers(org.smartregister.chw.tb.util.Constants.EventType.FOLLOW_UP_VISIT, classifier, scheduleServices);
        addToClassifers(org.smartregister.chw.tb.util.Constants.EventType.REGISTRATION, classifier, scheduleServices);
    }

    private void initializeRoutineHouseholdClassifier(Map<String, List<ScheduleService>> classifier) {
        List<ScheduleService> scheduleServices = new ArrayList<>();
        scheduleServices.add(new RoutineHouseHoldVisitScheduler());

        addToClassifers(CoreConstants.EventType.FAMILY_REGISTRATION, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.UPDATE_FAMILY_REGISTRATION, classifier, scheduleServices);
        addToClassifers(CoreConstants.EventType.ROUTINE_HOUSEHOLD_VISIT, classifier, scheduleServices);
    }

    private void initializeNcdCaseManagementClassifier(Map<String, List<ScheduleService>> classifier) {
        List<ScheduleService> scheduleServices = new ArrayList<>();
        scheduleServices.add(new NcdCaseManagementVisitScheduler());

        addToClassifers(org.smartregister.chw.util.Constants.EncounterType.NCD_MONTHLY_FOLLOWUP, classifier, scheduleServices);
        addToClassifers(org.smartregister.chw.ncd.util.Constants.EVENT_TYPE.DIABETES_HYPERTENSION_CONFIRMATION_EVENT, classifier, scheduleServices);
    }
}
