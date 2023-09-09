package org.smartregister.chw.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cocoahero.android.geojson.Point;
import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.turf.TurfMeasurement;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.model.CommunityResponderModel;
import org.smartregister.chw.core.utils.CoreConstants;

import java.util.ArrayList;
import java.util.List;

import io.ona.kujaku.utils.CoordinateUtils;
import io.ona.kujaku.views.KujakuMapView;
import timber.log.Timber;

public class AncMemberMapActivity extends AppCompatActivity {

    public static final String RECYCLER_VIEW_POSITION_PROPERTY = "recycler-view-position";
    private KujakuMapView kujakuMapView;
    private GeoJsonSource communityTransportersSource;
    private LatLng userLocation;
    private static int BOUNDING_BOX_PADDING = 100;
    public static final String GPS = "GPS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anc_member_map);

        kujakuMapView = findViewById(R.id.kujakuMapView);
        kujakuMapView.onCreate(savedInstanceState);
        kujakuMapView.showCurrentLocationBtn(true);
        kujakuMapView.setDisableMyLocationOnMapMove(true);

        userLocation = extractUserLocation();

        kujakuMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {
                Style.Builder builder = new Style.Builder().fromUri("asset://ba_anc_style.json");
                mapboxMap.setStyle(builder, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        communityTransportersSource = style.getSourceAs("community-transporters-data-set");

                        FeatureCollection featureCollection = loadCommunityTransporters();
                        BoundingBox boundingBox = showCommunityTransporters(mapboxMap, featureCollection);

                        zoomToPatientLocation(mapboxMap, boundingBox);
                        addCommunityTransporterClickListener(kujakuMapView);
                    }
                });
            }
        });
    }

    private void addCommunityTransporterClickListener(@NonNull KujakuMapView kujakuMapView) {
        kujakuMapView.setOnFeatureClickListener(features -> {
            // We only pick the first one
            Feature feature = features.get(0);
            featureClicked(feature);
        }, "community-transporters");
    }

    private void featureClicked(@NonNull Feature feature) {
        String responderName = feature.getStringProperty(CoreConstants.JsonAssets.RESPONDER_NAME);
        String respondersPhoneNumber = feature.getStringProperty(CoreConstants.JsonAssets.RESPONDER_PHONE_NUMBER);
        Toast.makeText(AncMemberMapActivity.this, String.format("Clicked on RESPONDER\nName: %s\nPhone No: %s", responderName, respondersPhoneNumber), Toast.LENGTH_LONG)
                .show();
    }

    @Nullable
    private LatLng extractUserLocation() {

        double latitude = Double.parseDouble("-1.9885");
        double longitude = Double.parseDouble("33.7799");

        String gpsString = this.getIntent().getStringExtra(GPS);

        if (gpsString != null) {
            try {
                String[] gpsCoordinates = gpsString.split(" ");
                latitude = Double.parseDouble(gpsCoordinates[0]);
                longitude = Double.parseDouble(gpsCoordinates[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new LatLng(latitude, longitude);
    }

    private void zoomToPatientLocation(@NonNull MapboxMap mapboxMap, @Nullable BoundingBox boundingBox) {
        if (userLocation != null && boundingBox == null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(userLocation)
                    .zoom(16)
                    .build();

            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        if (userLocation != null) {
            MarkerOptions markerOptions = markerOptions = new MarkerOptions()
                    .position(userLocation);
            mapboxMap.addMarker(markerOptions);
        }

        if (boundingBox != null && !CoordinateUtils.isLocationInBounds(userLocation, boundingBox.north(), boundingBox.south(), boundingBox.east(), boundingBox.west())) {
            double north = boundingBox.north();
            double south = boundingBox.south();
            double east = boundingBox.east();
            double west = boundingBox.west();

            if (userLocation.getLatitude() > north) {
                north = userLocation.getLatitude();
            } else if (userLocation.getLatitude() < south) {
                south = userLocation.getLatitude();
            }

            if (userLocation.getLongitude() > east) {
                east = userLocation.getLongitude();
            } else if (userLocation.getLongitude() < west) {
                west = userLocation.getLongitude();
            }

            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.from(north, east, south, west), BOUNDING_BOX_PADDING));
        }
    }

    @Nullable
    private BoundingBox showCommunityTransporters(@NonNull MapboxMap mapboxMap, @Nullable FeatureCollection featureCollection) {
        if (featureCollection != null && featureCollection.features() != null && featureCollection.features().size() > 0 && communityTransportersSource != null) {
            //CameraPosition cameraPosition = new CameraPosition.Builder(). featureCollection.bbox();
            BoundingBox boundingBox = featureCollection.bbox();

            if (boundingBox == null) {
                double[] bbox = TurfMeasurement.bbox(featureCollection);
                boundingBox = BoundingBox.fromLngLats(bbox[0], bbox[1], bbox[2], bbox[3]);
            }

            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.from(boundingBox.north(), boundingBox.east(), boundingBox.south(), boundingBox.west()), BOUNDING_BOX_PADDING));

            communityTransportersSource.setGeoJson(featureCollection);
            return boundingBox;
        }

        return null;
    }

    @Nullable
    private FeatureCollection loadCommunityTransporters() {
        return FeatureCollection.fromFeatures(getRespondersFeatureList());
    }

    private List<Feature> getRespondersFeatureList() {
        List<CommunityResponderModel> communityResponders = ChwApplication.getInstance().communityResponderRepository().readAllResponders();
        List<Feature> featureList = new ArrayList<>();
        int counter = 0;
        for (CommunityResponderModel communityResponderModel : communityResponders) {
            try {
                if (communityResponderModel.getResponderLocation() == null) continue;
                Feature feature = getFeature(communityResponderModel);
                feature.addNumberProperty(RECYCLER_VIEW_POSITION_PROPERTY, counter);
                featureList.add(feature);

                counter++;
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
        return featureList;
    }

    private Feature getFeature(CommunityResponderModel communityResponderModel) throws JSONException {
        String[] latLong = communityResponderModel.getResponderLocation().split(" ");
        double latitude = Double.parseDouble(latLong[0]);
        double longitude = Double.parseDouble(latLong[1]);
        com.cocoahero.android.geojson.Feature feature = new com.cocoahero.android.geojson.Feature();
        feature.setGeometry(new Point(latitude, longitude));
        JSONObject properties = new JSONObject();
        properties.put(CoreConstants.JsonAssets.RESPONDER_NAME, communityResponderModel.getResponderName());
        properties.put(CoreConstants.JsonAssets.RESPONDER_PHONE_NUMBER, communityResponderModel.getResponderPhoneNumber());
        feature.setProperties(properties);
        return Feature.fromJson(feature.toJSON().toString());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (kujakuMapView != null) {
            kujakuMapView.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (kujakuMapView != null)
            kujakuMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (kujakuMapView != null)
            kujakuMapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (kujakuMapView != null)
            kujakuMapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (kujakuMapView != null)
            kujakuMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (kujakuMapView != null)
            kujakuMapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (kujakuMapView != null)
            kujakuMapView.onDestroy();
    }
}
