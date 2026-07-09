package org.smartregister.chw.util;

import org.smartregister.AllConstants;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationTag;
import org.smartregister.domain.jsonmapping.util.LocationTree;
import org.smartregister.domain.jsonmapping.util.TreeNode;
import org.smartregister.repository.LocationRepository;
import org.smartregister.repository.LocationTagRepository;
import org.smartregister.util.AssetHandler;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public final class LocationUtils {
    private static final String WARD_TAG = "Ward";
    private static final String HAS_ADDO_TAG = "has_addo";
    private static final String COUNCIL_TAG = "Council";
    private static final String HAS_NCD_TAG = "council_has_ncd";

    private LocationUtils() {
    }

    public static boolean hasADDO() {
        try {
            LocationRepository locationRepository = new LocationRepository();
            List<Location> locations = locationRepository.getAllLocations();
            List<LocationTag> locationTags = new LocationTagRepository().getAllLocationTags();
            String wardLocationId = getWard(locations, locationTags);

            if (isBlank(wardLocationId)) {
                return false;
            }

            Location wardLocation = locationRepository.getLocationById(wardLocationId);
            return hasADDO(wardLocation, locationTags);
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    public static boolean hasNCD() {
        try {
            LocationRepository locationRepository = new LocationRepository();
            List<Location> locations = locationRepository.getAllLocations();
            List<LocationTag> locationTags = new LocationTagRepository().getAllLocationTags();
            TreeNode<String, org.smartregister.domain.jsonmapping.Location> councilNode = getCouncil(locations, locationTags);
            return hasNCD(councilNode, locationTags);
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    public static String getWard() {
        try {
            LocationRepository locationRepository = new LocationRepository();
            List<Location> locations = locationRepository.getAllLocations();
            List<LocationTag> locationTags = new LocationTagRepository().getAllLocationTags();
            return getWard(locations, locationTags);
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    static boolean hasADDO(Location wardLocation, List<LocationTag> locationTags) {
        return wardLocation != null && hasLocationTag(locationTags, wardLocation.getId(), HAS_ADDO_TAG);
    }

    static boolean hasNCD(TreeNode<String, org.smartregister.domain.jsonmapping.Location> councilNode, List<LocationTag> locationTags) {
        return councilNode != null && hasLocationTreeNodeTag(councilNode, locationTags, HAS_NCD_TAG);
    }

    static String getWard(List<Location> locations, List<LocationTag> locationTags) {
        String locationId = Context.getInstance().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
        String locationData = CoreLibrary.getInstance().context().anmLocationController().get();
        return getWard(locations, locationTags, locationId, locationData);
    }

    static TreeNode<String, org.smartregister.domain.jsonmapping.Location> getCouncil(List<Location> locations, List<LocationTag> locationTags) {
        String locationId = Context.getInstance().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
        String locationData = CoreLibrary.getInstance().context().anmLocationController().get();
        return getCouncil(locations, locationTags, locationId, locationData);
    }

    static String getWard(List<Location> locations, List<LocationTag> locationTags, String locationId, String locationData) {
        if (isBlank(locationId)) {
            return null;
        }

        if (isBlank(locationData)) {
            return getParentLocationIdWithTags(locations, locationTags, locationId, WARD_TAG);
        }

        try {
            LocationTree locationTree = AssetHandler.jsonStringToJava(locationData, LocationTree.class);
            if (locationTree == null || locationTree.getLocationsHierarchy() == null) {
                return getParentLocationIdWithTags(locations, locationTags, locationId, WARD_TAG);
            }

            String wardLocationId =
                    getLocationIdWithTagFromTree(
                            locationTree.getLocationsHierarchy(), locationTags, locationId, WARD_TAG);
            if (!isBlank(wardLocationId)) {
                return wardLocationId;
            }

            return getParentLocationIdWithTags(locations, locationTags, locationId, WARD_TAG);
        } catch (Exception e) {
            Timber.e(e);
            return getParentLocationIdWithTags(locations, locationTags, locationId, WARD_TAG);
        }
    }

    static TreeNode<String, org.smartregister.domain.jsonmapping.Location> getCouncil(List<Location> locations, List<LocationTag> locationTags, String locationId, String locationData) {
        if (isBlank(locationId)) {
            return null;
        }

        if (isBlank(locationData)) {
            return null;
        }

        try {
            LocationTree locationTree = AssetHandler.jsonStringToJava(locationData, LocationTree.class);
            if (locationTree == null || locationTree.getLocationsHierarchy() == null) {
                return null;
            }

            return getLocationNodeWithTagFromTree(
                    locationTree.getLocationsHierarchy(), locationTags, locationId, COUNCIL_TAG);
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    private static TreeNode<String, org.smartregister.domain.jsonmapping.Location> getLocationNodeWithTagFromTree(
            LinkedHashMap<String, TreeNode<String, org.smartregister.domain.jsonmapping.Location>> locationMap,
            List<LocationTag> locationTags,
            String locationId,
            String tagName) {
        if (locationMap == null || isBlank(locationId) || isBlank(tagName)) {
            return null;
        }

        TreeNode<String, org.smartregister.domain.jsonmapping.Location> locationNode =
                findLocationNode(locationMap, locationId);
        Set<String> visitedLocationIds = new HashSet<>();

        while (locationNode != null) {
            String currentLocationId = locationNode.getId();
            if (isBlank(currentLocationId) || visitedLocationIds.contains(currentLocationId)) {
                return null;
            }

            visitedLocationIds.add(currentLocationId);
            if (hasLocationTreeNodeTag(locationNode, locationTags, tagName)) {
                return locationNode;
            }

            String parentLocationId = locationNode.getParent();
            if (isBlank(parentLocationId)) {
                return null;
            }
            locationNode = findLocationNode(locationMap, parentLocationId);
        }
        return null;
    }

    private static String getLocationIdWithTagFromTree(
            LinkedHashMap<String, TreeNode<String, org.smartregister.domain.jsonmapping.Location>> locationMap,
            List<LocationTag> locationTags,
            String locationId,
            String tagName) {
        if (locationMap == null || isBlank(locationId) || isBlank(tagName)) {
            return null;
        }

        TreeNode<String, org.smartregister.domain.jsonmapping.Location> locationNode =
                findLocationNode(locationMap, locationId);
        Set<String> visitedLocationIds = new HashSet<>();

        while (locationNode != null) {
            String currentLocationId = locationNode.getId();
            if (isBlank(currentLocationId) || visitedLocationIds.contains(currentLocationId)) {
                return null;
            }

            visitedLocationIds.add(currentLocationId);
            if (hasLocationTreeNodeTag(locationNode, locationTags, tagName)) {
                return currentLocationId;
            }

            String parentLocationId = locationNode.getParent();
            if (isBlank(parentLocationId)) {
                return null;
            }
            locationNode = findLocationNode(locationMap, parentLocationId);
        }
        return null;
    }

    private static boolean hasLocationTreeNodeTag(
            TreeNode<String, org.smartregister.domain.jsonmapping.Location> locationNode,
            List<LocationTag> locationTags,
            String tagName) {
        if (locationNode == null || isBlank(tagName)) {
            return false;
        }

        String locationId = locationNode.getId();
        if (hasLocationTag(locationTags, locationId, tagName)) {
            return true;
        }

        org.smartregister.domain.jsonmapping.Location location = locationNode.getNode();
        if (location == null || location.getTags() == null) {
            return false;
        }

        for (String tag : location.getTags()) {
            if (tagName.equalsIgnoreCase(tag)) {
                return true;
            }
        }
        return false;
    }

    static TreeNode<String, org.smartregister.domain.jsonmapping.Location> findLocationNode(
            LinkedHashMap<String, TreeNode<String, org.smartregister.domain.jsonmapping.Location>> locationMap,
            String locationId) {
        if (locationMap == null || isBlank(locationId)) {
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

    static boolean hasLocationTag(List<LocationTag> locationTags, String locationId, String tagName) {
        if (locationTags == null || isBlank(locationId) || isBlank(tagName)) {
            return false;
        }

        for (LocationTag locationTag : locationTags) {
            if (locationTag == null) {
                continue;
            }
            if (locationId.equals(locationTag.getLocationId()) && tagName.equalsIgnoreCase(locationTag.getName())) {
                return true;
            }
        }
        return false;
    }

    static boolean hasLocationTag(List<Location> locations, List<LocationTag> locationTags, String locationId, String tagName) {
        if (!hasLocation(locations, locationId)) {
            return false;
        }
        return hasLocationTag(locationTags, locationId, tagName);
    }

    static String getParentLocationIdWithTags(List<Location> locations, List<LocationTag> locationTags, String locationId, String tagName) {
        return getParentLocationIdWithTags(locations, locationTags, locationId, tagName, new HashSet<String>());
    }

    private static String getParentLocationIdWithTags(List<Location> locations, List<LocationTag> locationTags, String locationId, String tagName, Set<String> visitedLocationIds) {
        if (locations == null || isBlank(locationId) || isBlank(tagName) || visitedLocationIds.contains(locationId)) {
            return null;
        }

        visitedLocationIds.add(locationId);
        for (Location location : locations) {
            if (location == null || !locationId.equals(location.getId())) {
                continue;
            }

            if (hasLocationTag(locationTags, location.getId(), tagName)) {
                return location.getId();
            }

            if (location.getProperties() == null || isBlank(location.getProperties().getParentId())) {
                return null;
            }

            return getParentLocationIdWithTags(locations, locationTags, location.getProperties().getParentId(), tagName, visitedLocationIds);
        }
        return null;
    }

    private static boolean hasLocation(List<Location> locations, String locationId) {
        if (locations == null || isBlank(locationId)) {
            return false;
        }

        for (Location location : locations) {
            if (location != null && locationId.equals(location.getId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}