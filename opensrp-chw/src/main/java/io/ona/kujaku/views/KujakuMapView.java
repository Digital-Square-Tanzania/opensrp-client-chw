package io.ona.kujaku.views;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.Collections;
import java.util.List;

import io.ona.kujaku.helpers.MapboxLocationComponentWrapper;
import io.ona.kujaku.layers.KujakuLayer;

public class KujakuMapView extends MapView {

    private final MapboxLocationComponentWrapper mapboxLocationComponentWrapper = new MapboxLocationComponentWrapper();
    @Nullable
    private MapboxMap mapboxMap;
    @Nullable
    private OnFeatureClickListener onFeatureClickListener;
    @NonNull
    private String[] featureClickLayerIds = new String[0];
    @Nullable
    private MapboxMap.OnMapClickListener mapClickListener;

    public KujakuMapView(@NonNull Context context) {
        super(context);
    }

    public KujakuMapView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public KujakuMapView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MapboxLocationComponentWrapper getMapboxLocationComponentWrapper() {
        return mapboxLocationComponentWrapper;
    }

    public void showCurrentLocationBtn(boolean isVisible) {
        // No-op. The app does not depend on Kujaku's floating button UI.
    }

    public void setDisableMyLocationOnMapMove(boolean disableMyLocationOnMapMove) {
        // No-op. The local replacement does not manage location component state.
    }

    public void enableAddPoint(boolean canAddPoint) {
        // No-op.
    }

    public void addLayer(@NonNull KujakuLayer kujakuLayer) {
        // No-op. This app only uses feature click handling on existing style layers.
    }

    public void setOnFeatureClickListener(@Nullable OnFeatureClickListener listener, @Nullable String... layerIds) {
        onFeatureClickListener = listener;
        featureClickLayerIds = layerIds == null ? new String[0] : layerIds;
        bindFeatureClickListener();
    }

    @Override
    public void getMapAsync(@NonNull OnMapReadyCallback callback) {
        super.getMapAsync(readyMap -> {
            mapboxMap = readyMap;
            bindFeatureClickListener();
            callback.onMapReady(readyMap);
        });
    }

    private void bindFeatureClickListener() {
        if (mapboxMap == null) {
            return;
        }

        if (mapClickListener != null) {
            mapboxMap.removeOnMapClickListener(mapClickListener);
            mapClickListener = null;
        }

        if (onFeatureClickListener == null) {
            return;
        }

        mapClickListener = point -> {
            List<Feature> features = queryFeatures(point);
            if (features.isEmpty()) {
                return false;
            }
            onFeatureClickListener.onFeaturesClicked(features);
            return true;
        };

        mapboxMap.addOnMapClickListener(mapClickListener);
    }

    @NonNull
    private List<Feature> queryFeatures(@NonNull LatLng point) {
        if (mapboxMap == null) {
            return Collections.emptyList();
        }

        PointF screenPoint = mapboxMap.getProjection().toScreenLocation(point);
        List<Feature> features;
        if (featureClickLayerIds.length == 0) {
            features = mapboxMap.queryRenderedFeatures(screenPoint);
        } else {
            features = mapboxMap.queryRenderedFeatures(screenPoint, featureClickLayerIds);
        }

        return features == null ? Collections.emptyList() : features;
    }

    public interface OnFeatureClickListener {
        void onFeaturesClicked(@NonNull List<Feature> features);
    }
}
