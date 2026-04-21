package org.smartregister.chw.util;

import org.joda.time.DateTime;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
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
import org.smartregister.domain.jsonmapping.util.LocationTree;
import org.smartregister.domain.jsonmapping.util.TreeNode;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.LocationRepository;
import org.smartregister.repository.LocationTagRepository;
import org.smartregister.util.AssetHandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

        // ICCM referrals should keep referralProblems for Task description only.
        if (!isIccmReferral(referralType)) {
            obsMap.put("problem", referralProblems);
        }

        for (String key : obsMap.keySet()) {
            List<Object> value = Collections.singletonList(obsMap.get(key));
            baseEvent.addObs(new Obs("concept", "text", key, "", value, value, "", key));
        }

    }

    private static boolean isIccmReferral(String referralType) {
        return CoreConstants.TASKS_FOCUS.ICCM_REFERRAL.equalsIgnoreCase(referralType);
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

        String locationData = CoreLibrary.getInstance().context().anmLocationController().get();
        if (locationData == null || locationData.trim().isEmpty()) {
            return getParentLocationIdWithTags(locations, locationId, "Ward");
        }

        try {
            LocationTree locationTree = AssetHandler.jsonStringToJava(locationData, LocationTree.class);
            if (locationTree == null || locationTree.getLocationsHierarchy() == null) {
                return getParentLocationIdWithTags(locations, locationId, "Ward");
            }

            TreeNode<String, org.smartregister.domain.jsonmapping.Location> locationNode =
                    findLocationNode(locationTree.getLocationsHierarchy(), locationId);
            if (locationNode == null || locationNode.getParent() == null) {
                return getParentLocationIdWithTags(locations, locationId, "Ward");
            }

            String parentLocationId = locationNode.getParent();
            if (hasLocationTag(locations, parentLocationId, "Ward")) {
                return parentLocationId;
            }

            return getParentLocationIdWithTags(locations, parentLocationId, "Ward");
        } catch (Exception e) {
            return getParentLocationIdWithTags(locations, locationId, "Ward");
        }
    }

    private static TreeNode<String, org.smartregister.domain.jsonmapping.Location> findLocationNode(
            LinkedHashMap<String, TreeNode<String, org.smartregister.domain.jsonmapping.Location>> locationMap,
            String locationId) {
        if (locationMap == null || locationId == null) {
            return null;
        }

        for (Map.Entry<String, TreeNode<String, org.smartregister.domain.jsonmapping.Location>> entry : locationMap.entrySet()) {
            TreeNode<String, org.smartregister.domain.jsonmapping.Location> treeNode = entry.getValue();
            if (treeNode == null) {
                continue;
            }
            if (locationId.equals(treeNode.getId())) {
                return treeNode;
            }

            TreeNode<String, org.smartregister.domain.jsonmapping.Location> childTreeNode =
                    findLocationNode(treeNode.getChildren(), locationId);
            if (childTreeNode != null) {
                return childTreeNode;
            }
        }
        return null;
    }

    private static boolean hasLocationTag(List<Location> locations, String locationId, String tagName) {
        LocationTagRepository locationTagReposity = new LocationTagRepository();
        List<LocationTag> allLocationTags = locationTagReposity.getAllLocationTags();
        for (Location location : locations) {
            if (location.getId().equals(locationId)) {
                for (LocationTag locationTag : allLocationTags) {
                    if (locationId.equals(locationTag.getLocationId()) && tagName.equalsIgnoreCase(locationTag.getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
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
