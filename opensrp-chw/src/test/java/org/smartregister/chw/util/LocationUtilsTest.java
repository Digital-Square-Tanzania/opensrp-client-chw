package org.smartregister.chw.util;

import org.junit.Assert;
import org.junit.Test;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationProperty;
import org.smartregister.domain.LocationTag;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LocationUtilsTest {

    @Test
    public void hasADDOReturnsTrueWhenWardHasADDOTag() {
        Location ward = location("ward-id", null);
        List<LocationTag> locationTags = Collections.singletonList(locationTag("ward-id", "has_addo"));

        Assert.assertTrue(LocationUtils.hasADDO(ward, locationTags));
    }

    @Test
    public void hasADDOReturnsFalseWhenWardDoesNotHaveADDOTag() {
        Location ward = location("ward-id", null);
        List<LocationTag> locationTags = Collections.singletonList(locationTag("ward-id", "Ward"));

        Assert.assertFalse(LocationUtils.hasADDO(ward, locationTags));
    }

    @Test
    public void getWardReturnsCurrentLocationWhenCurrentLocationHasWardTag() {
        Location ward = location("ward-id", "district-id");
        List<LocationTag> locationTags = Collections.singletonList(locationTag("ward-id", "Ward"));

        String wardId = LocationUtils.getWard(Collections.singletonList(ward), locationTags, "ward-id", null);

        Assert.assertEquals("ward-id", wardId);
    }

    @Test
    public void getWardReturnsParentWardWhenCurrentLocationIsBelowWard() {
        Location ward = location("ward-id", "district-id");
        Location village = location("village-id", "ward-id");
        List<Location> locations = Arrays.asList(ward, village);
        List<LocationTag> locationTags = Collections.singletonList(locationTag("ward-id", "Ward"));

        String wardId = LocationUtils.getWard(locations, locationTags, "village-id", null);

        Assert.assertEquals("ward-id", wardId);
    }

    @Test
    public void getWardReturnsNullWhenWardCannotBeResolved() {
        Location village = location("village-id", "missing-parent-id");

        String wardId = LocationUtils.getWard(Collections.singletonList(village), Collections.<LocationTag>emptyList(), "village-id", null);

        Assert.assertNull(wardId);
    }

    private Location location(String id, String parentId) {
        Location location = new Location();
        location.setId(id);

        LocationProperty locationProperty = new LocationProperty();
        locationProperty.setParentId(parentId);
        location.setProperties(locationProperty);
        return location;
    }

    private LocationTag locationTag(String locationId, String name) {
        LocationTag locationTag = new LocationTag();
        locationTag.setLocationId(locationId);
        locationTag.setName(name);
        return locationTag;
    }
}
