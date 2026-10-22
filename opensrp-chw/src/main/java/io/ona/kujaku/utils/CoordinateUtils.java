package io.ona.kujaku.utils;

import androidx.annotation.NonNull;

import com.mapbox.mapboxsdk.geometry.LatLng;

public final class CoordinateUtils {

    private CoordinateUtils() {
        // No instances.
    }

    public static boolean isLocationInBounds(@NonNull LatLng location, double north, double south, double east, double west) {
        return location.getLatitude() <= north
                && location.getLatitude() >= south
                && location.getLongitude() <= east
                && location.getLongitude() >= west;
    }
}
