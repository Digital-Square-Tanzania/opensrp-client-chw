package org.smartregister.chw.repository;

import net.sqlcipher.Cursor;

import org.smartregister.domain.Location;
import org.smartregister.domain.LocationTag;
import org.smartregister.repository.LocationRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class ChwLocationRepository extends LocationRepository {
    String LOCATION_TABLE = "location";
    String LOCATION_TAG_TABLE = "location_tag";

    public List<Location> getAllLocationsWithTags() {
        List<Location> locations = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery(
                    ("SELECT * FROM " +
                            LOCATION_TABLE + " INNER JOIN " + LOCATION_TAG_TABLE +
                            " on " + LOCATION_TABLE +"._id = " + LOCATION_TAG_TABLE +".location_id"),
                    null);
            while (cursor.moveToNext()) {
                locations.add(readCursor(cursor));
            }
            cursor.close();
        } catch (Exception e) {
            Timber.e(e);
        }
        return locations;
    }

    @Override
    protected Location readCursor(Cursor cursor) {
        String geoJson = cursor.getString(cursor.getColumnIndex(GEOJSON));
        Location location = gson.fromJson(geoJson, Location.class);
        int locationTagNameIndex = 6;
        String tagName = cursor.getString(locationTagNameIndex);
        Set<LocationTag> locationTagSet = new HashSet<>();
        LocationTag locationTag = new LocationTag();
        locationTag.setName(tagName);
        locationTagSet.add(locationTag);
        location.setLocationTags(locationTagSet);
        return location;
    }
}
