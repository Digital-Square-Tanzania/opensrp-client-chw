package org.smartregister.chw.util;

import org.joda.time.DateTime;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.Context;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreReferralUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationTag;
import org.smartregister.domain.Task;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.LocationRepository;
import org.smartregister.repository.LocationTagRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ReferralUtils extends CoreReferralUtils {

    public static void processReferral(String facilitySelectionForm, String baseEntityId, String referralType, String referralProblems) throws Exception {

        String selectedFacility = JsonQ.fromJson(facilitySelectionForm).get("step1.fields[?(@.key=='chw_referral_hf')].value").toString();

        AllSharedPreferences allSharedPreferences = Utils.getAllSharedPreferences();

        final Event baseEvent = JsonFormUtils.processJsonForm(allSharedPreferences, setEntityId(facilitySelectionForm, baseEntityId), CoreConstants.TABLE_NAME.REFERRAL);

        addReferralDetails(baseEvent, referralType, referralProblems);

        JsonFormUtils.tagEvent(allSharedPreferences, baseEvent);

        NCUtils.processEvent(baseEvent.getBaseEntityId(), new JSONObject(JsonFormUtils.gson.toJson(baseEvent)));

        createReferralTask(allSharedPreferences, baseEntityId, baseEvent.getFormSubmissionId(), referralProblems, selectedFacility, referralType);

    }

    private static void addReferralDetails(Event baseEvent, String referralType, String referralProblems) {
        if (baseEvent == null) return;

        long referralDate = System.currentTimeMillis();
        Map<String, Object> obsMap = new HashMap<>();
        obsMap.put("referral_status", "PENDING");
        obsMap.put("chw_referral_service", referralType);
        obsMap.put("referral_type", "community_to_facility_referral");
        obsMap.put("referral_date", referralDate);
        obsMap.put("referral_time", new SimpleDateFormat("HH:mm:ss.SSS", Locale.ENGLISH).format(referralDate));
        obsMap.put("problem", referralProblems);

        for (String key : obsMap.keySet()) {
            List<Object> value = Collections.singletonList(obsMap.get(key));
            baseEvent.addObs(new Obs("concept", "text", key, "", value, value, "", key));
        }

    }

    private static void createReferralTask(AllSharedPreferences allSharedPreferences, String baseEntityId, String formSubmissionId, String referralProblems, String selectedFacility, String referralFocus) {

        Task task = new Task();
        task.setIdentifier(UUID.randomUUID().toString());
        task.setPlanIdentifier(CoreConstants.REFERRAL_PLAN_ID);
        task.setGroupIdentifier(selectedFacility);
        task.setStatus(Task.TaskStatus.READY);
        task.setBusinessStatus(CoreConstants.BUSINESS_STATUS.REFERRED);
        task.setPriority(3);
        task.setCode(CoreConstants.JsonAssets.REFERRAL_CODE);
        task.setDescription(referralProblems);
        task.setFocus(referralFocus);
        task.setForEntity(baseEntityId);
        DateTime now = new DateTime();
        task.setExecutionStartDate(now);
        task.setAuthoredOn(now);
        task.setLastModified(now);
        task.setOwner(allSharedPreferences.fetchRegisteredANM());
        task.setSyncStatus(BaseRepository.TYPE_Created);
        task.setReasonReference(formSubmissionId);
        task.setRequester(allSharedPreferences.getANMPreferredName(allSharedPreferences.fetchRegisteredANM()));
        task.setLocation(allSharedPreferences.fetchUserLocalityId(allSharedPreferences.fetchRegisteredANM()));
        CoreChwApplication.getInstance().getTaskRepository().addOrUpdate(task);
    }

    public static void createLinkageTask(AllSharedPreferences sharedPreferences, String baseEntityId, String formSubmissionId, String minorAilments, String focus) {

        Task task = new Task();
        task.setIdentifier(UUID.randomUUID().toString());
        task.setPlanIdentifier(CoreConstants.ADDO_LINKAGE_PLAN_ID);
        task.setGroupIdentifier(getWard());
        task.setStatus(Task.TaskStatus.READY);
        task.setBusinessStatus(Constants.AddoLinkage.BUSINESS_STATUS);
        task.setPriority(2);
        task.setCode(Constants.AddoLinkage.CODE);
        task.setDescription(minorAilments);
        task.setFocus(focus);
        task.setForEntity(baseEntityId);
        DateTime now = new DateTime();
        task.setExecutionStartDate(now);
        task.setAuthoredOn(now);
        task.setLastModified(now);
        task.setOwner(sharedPreferences.fetchRegisteredANM());
        task.setSyncStatus(BaseRepository.TYPE_Created);
        task.setReasonReference(formSubmissionId);
        task.setRequester(sharedPreferences.getANMPreferredName(sharedPreferences.fetchRegisteredANM()));
        task.setLocation(sharedPreferences.fetchUserLocalityId(sharedPreferences.fetchRegisteredANM()));
        CoreChwApplication.getInstance().getTaskRepository().addOrUpdate(task);
    }

    private static String getWard() {
        LocationRepository locationRepository = new LocationRepository();
        List<Location> locations = locationRepository.getAllLocations();
        String locationId = Context.getInstance().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
        return getParentLocationIdWithTags(locations, locationId, "Ward");
    }

    private static String getParentLocationIdWithTags(List<Location> locations, String locationId, String tagName) {
        LocationTagRepository locationTagReposity = new LocationTagRepository();
        List<LocationTag> allLocationTags = locationTagReposity.getAllLocationTags();
        for (Location location : locations) {
            List<LocationTag> locationTags = new ArrayList<>();
            for (LocationTag locationTag : allLocationTags) {
                if (locationTag.getLocationId().equals(location.getId())) {
                    locationTags.add(locationTag);
                }
            }
            if (location.getId().equals(locationId)) {
                for (LocationTag locationTag : locationTags) {
                    if (locationTag.getName().equalsIgnoreCase(tagName))
                        return location.getId();
                    else
                        return getParentLocationIdWithTags(locations, location.getProperties().getParentId(), tagName);
                }
            }
        }
        return null;
    }

}
