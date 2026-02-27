package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hps.domain.VisitDetail;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class HpsAnnualCensusStep7BuildingInspectionActionHelper implements BaseHpsVisitAction.HpsVisitActionHelper {

    private String jsonPayload;
    private String submittedPayload;

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> details) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        return jsonPayload;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        this.submittedPayload = jsonPayload;
    }

    @Override
    public BaseHpsVisitAction.ScheduleStatus getPreProcessedStatus() {
        return null;
    }

    @Override
    public String getPreProcessedSubTitle() {
        return null;
    }

    @Override
    public String postProcess(String s) {
        return null;
    }

    @Override
    public String evaluateSubTitle() {
        try {
            if (submittedPayload == null) return null;
            JSONObject json = new JSONObject(submittedPayload);
            String foodShops = CoreJsonFormUtils.getValue(json, "number_of_food_shop_visited");
            String restaurants = CoreJsonFormUtils.getValue(json, "number_of_restaurants_visited");
            String markets = CoreJsonFormUtils.getValue(json, "number_of_markets_visited");
            StringBuilder sb = new StringBuilder();
            if (foodShops != null && !foodShops.trim().isEmpty()) sb.append("Food shops ").append(foodShops.trim());
            if (restaurants != null && !restaurants.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Restaurants ").append(restaurants.trim());
            }
            if (markets != null && !markets.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Markets ").append(markets.trim());
            }
            return sb.length() == 0 ? null : sb.toString();
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    @Override
    public BaseHpsVisitAction.Status evaluateStatusOnPayload() {
        try {
            if (submittedPayload == null) return BaseHpsVisitAction.Status.PENDING;
            JSONObject json = new JSONObject(submittedPayload);
            String[] keys = new String[]{
                    "number_of_food_shop_visited",
                    "number_of_restaurants_visited",
                    "number_of_butcheries_visited",
                    "number_of_bar_and_clubs_visited",
                    "number_of_guest_house_visited",
                    "number_of_local_food_vendors_visited",
                    "number_of_markets_visited",
                    "number_of_public_toilets_visited",
                    "number_of_bus_stations_visited",
                    "number_of_primary_schools_visited",
                    "number_of_secondary_schools_visited",
                    "number_of_hospital_visited",
                    "number_of_health_centers_visited",
                    "number_of_dispensaries_visited",
                    "number_of_offices_visited",
                    "number_of_universities_college_visited",
                    "number_of_food_shop_that_Met_the_Standards",
                    "number_of_restaurants_that_Met_the_Standards",
                    "number_of_butcheries_that_Met_the_Standards",
                    "number_of_bar_and_clubs_that_Met_the_Standards",
                    "number_of_guest_house_that_Met_the_Standards",
                    "number_of_loca_food_vendors_that_Met_the_Standards",
                    "number_of_markets_that_Met_the_Standards",
                    "number_of_public_toilets_that_Met_the_Standards",
                    "number_of_bus_stations_that_Met_the_Standards",
                    "number_of_primary_schools_that_Met_the_Standards",
                    "number_of_secondary_schools_that_Met_the_Standards",
                    "number_of_hospital_that_Met_the_Standards",
                    "number_of_health_centers_that_Met_the_Standards",
                    "number_of_dispensaries_that_Met_the_Standards",
                    "number_of_offices_that_Met_the_Standards",
                    "number_of_universities_college_that_Met_the_Standards"
            };
            for (String k : keys) {
                String v = CoreJsonFormUtils.getValue(json, k);
                if (v != null && !v.trim().isEmpty()) return BaseHpsVisitAction.Status.COMPLETED;
            }
            return BaseHpsVisitAction.Status.PENDING;
        } catch (Exception e) {
            Timber.e(e);
            return BaseHpsVisitAction.Status.PENDING;
        }
    }

    @Override
    public void onPayloadReceived(BaseHpsVisitAction baseHpsVisitAction) { /* no-op */ }
}
