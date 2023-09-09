package org.smartregister.chw.actionhelper;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.smartregister.chw.anc.domain.VaccineDisplay;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.core.utils.VaccineScheduleUtil;
import org.smartregister.chw.core.utils.VisitVaccineUtil;
import org.smartregister.chw.fragment.BaseHomeVisitImmunizationFragmentFlv;
import org.smartregister.domain.Alert;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.domain.VaccineSchedule;
import org.smartregister.immunization.domain.VaccineWrapper;
import org.smartregister.immunization.domain.jsonmapping.VaccineGroup;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This view driver evaluates if a view is valid or not
 */
public class ImmunizationValidator implements BaseAncHomeVisitAction.Validator {
    private Map<String, BaseHomeVisitImmunizationFragmentFlv> fragments = new LinkedHashMap<>();
    private Map<String, DateTime> anchorDates = new HashMap<>();
    private Map<String, VaccineGroup> vaccineGroupMap = new HashMap<>();
    private List<String> keyPositions = new ArrayList<>();
    private int lastValidKeyPosition = 0;
    private HashMap<String, HashMap<String, VaccineSchedule>> vaccineSchedules;
    private Map<String, Date> administeredVaccines = new HashMap<>();
    private Map<Integer, Map<String, Date>> previousVaccines = new HashMap<>();

    private Map<String, BaseAncHomeVisitAction> actions = new HashMap<>();
    private Map<String, Integer> vaccineOrder = new HashMap<>();

    public ImmunizationValidator(
            List<VaccineGroup> vaccinesGroups,
            List<org.smartregister.immunization.domain.jsonmapping.Vaccine> specialVaccines,
            String vaccineCategory,
            List<Vaccine> vaccines
    ) {
        vaccineSchedules = VisitVaccineUtil.getSchedule(vaccinesGroups, specialVaccines, vaccineCategory);
        for (Vaccine vaccine : vaccines) {
            administeredVaccines.put(vaccine.getName(), vaccine.getDate());
        }
    }

    public void setActions(Map<String, BaseAncHomeVisitAction> actions) {
        this.actions = actions;
    }

    public void setVaccineOrder(Map<String, Integer> vaccineOrder) {
        this.vaccineOrder = vaccineOrder;
    }

    public void addFragment(String key, BaseHomeVisitImmunizationFragmentFlv fragment, VaccineGroup vaccineGroup, DateTime anchorDate) {
        if (!fragments.containsKey(key)) {
            keyPositions.add(key);
        }
        fragments.put(key, fragment);
        anchorDates.put(key, anchorDate);
        vaccineGroupMap.put(key, vaccineGroup);
    }

    /**
     * An immunization window is valid only if there are pending vaccines to be recorded
     *
     * @param s
     * @return
     */
    @Override
    public boolean isValid(String s) {
        BaseHomeVisitImmunizationFragmentFlv fragment = fragments.get(s);
        if (fragment == null)
            return false;

        return fragment.getVaccineDisplays().size() > 0;
    }

    /**
     * Only enable the other windows when an object is recorded.
     *
     * @param s
     * @return
     */
    @Override
    public boolean isEnabled(String s) {
        int position = keyPositions.indexOf(s);
        return position >= 0 && position <= lastValidKeyPosition;
    }

    public void resetVaccinesBelow(String s) {
        Integer pos = vaccineOrder.get(s);
        if (pos != null) {
            for (Map.Entry<String, BaseAncHomeVisitAction> entry : actions.entrySet()) {
                Integer entryPos = vaccineOrder.get(entry.getKey());
                if (entryPos != null && entryPos > pos && entry.getValue().getJsonPayload() != null) {
                    entry.getValue().setJsonPayload(null);
                }
            }
        }
    }

    /**
     * receives a notification on the changed action
     *
     * @param s
     */
    @Override
    public void onChanged(String s) {
        int position = keyPositions.indexOf(s);
        lastValidKeyPosition = position + 1;

        int next = position + 1;
        if (next == keyPositions.size())
            return;

        int x = position + 1;
        String key = s;

        Map<String, Date> receivedVacs = new HashMap<>(administeredVaccines);

        while (x < keyPositions.size()) {

            BaseHomeVisitImmunizationFragmentFlv prevFragment = fragments.get(key);
            key = keyPositions.get(x);
            BaseHomeVisitImmunizationFragmentFlv fragment = fragments.get(key);

            if (fragment == null)
                continue;

            // add all the received vaccines in the map
            if (prevFragment != null) {
                for (VaccineDisplay display : prevFragment.getVaccineDisplays().values()) {
                    if (display.getValid())
                        receivedVacs.put(display.getVaccineWrapper().getName(), display.getDateGiven());
                }
            }

            previousVaccines.put(position, receivedVacs);

            Map<String, Date> allReceivedVaccines = new HashMap<>();
            for (Map.Entry<Integer, Map<String, Date>> map : previousVaccines.entrySet()) {
                if (position >= map.getKey()) {
                    allReceivedVaccines.putAll(map.getValue());
                }
            }

            DateTime anchorDate = anchorDates.get(key);
            VaccineGroup vaccineGroup = vaccineGroupMap.get(key);

            List<VaccineWrapper> wrappers = VaccineScheduleUtil.recomputeSchedule(vaccineSchedules, anchorDate, vaccineGroup, allReceivedVaccines);
            List<VaccineDisplay> displays = generateDisplaysFromWrappers(wrappers, anchorDate.toDate());

            // update the vaccines
            Map<String, VaccineDisplay> linkedHashMap = new LinkedHashMap<>();
            for (VaccineDisplay vaccineDisplay : displays) {
                linkedHashMap.put(vaccineDisplay.getVaccineWrapper().getName(), vaccineDisplay);
            }
            fragment.setVaccineDisplays(linkedHashMap);

            x++;
        }

        String next_key = keyPositions.get(next);
        BaseHomeVisitImmunizationFragmentFlv nextFragment = fragments.get(next_key);
        while (nextFragment != null && nextFragment.getVaccineDisplays().size() == 0) {
            lastValidKeyPosition++;
            next++;
            if (next < keyPositions.size()) {
                next_key = keyPositions.get(next);
                nextFragment = fragments.get(next_key);
            } else {
                nextFragment = null;
            }
        }


        // reset all vaccines below
        resetVaccinesBelow(s);
    }

    private List<VaccineDisplay> generateDisplaysFromWrappers(List<VaccineWrapper> wrappers, Date startDate) {
        List<VaccineDisplay> displays = new ArrayList<>();
        for (VaccineWrapper vaccineWrapper : wrappers) {
            Alert alert = vaccineWrapper.getAlert();
            VaccineDisplay display = new VaccineDisplay();
            display.setVaccineWrapper(vaccineWrapper);
            display.setStartDate(alert != null ? new LocalDate(alert.startDate()).toDate() : startDate);
            display.setEndDate(alert != null && alert.expiryDate() != null ? new LocalDate(alert.expiryDate()).toDate() : new Date());
            display.setValid(false);
            displays.add(display);
        }
        return displays;
    }

}
