package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hps.domain.VisitDetail;
import org.smartregister.chw.hps.model.BaseHpsVisitAction;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Builds the population step subtitle from household and age-band totals.
 */
public class HpsAnnualCensusStep1PopulationActionHelper implements BaseHpsVisitAction.HpsVisitActionHelper {

    public interface Callback {
        void onHouseholdCountCaptured(String households);
    }

    private final Callback callback;
    private Context context;
    private String jsonPayload;
    private String submittedPayload;
    private String householdCountValue;

    public HpsAnnualCensusStep1PopulationActionHelper(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> details) {
        this.jsonPayload = jsonPayload;
        this.context = context;
    }

    @Override
    public String getPreProcessed() {
        return jsonPayload;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            this.submittedPayload = jsonPayload;
            JSONObject jsonObject = new JSONObject(jsonPayload);
            householdCountValue = CoreJsonFormUtils.getValue(jsonObject, "number_of_house_hold");
            if (callback != null) callback.onHouseholdCountCaptured(householdCountValue);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public BaseHpsVisitAction.ScheduleStatus getPreProcessedStatus() { return null; }

    @Override
    public String getPreProcessedSubTitle() { return null; }

    @Override
    public String postProcess(String s) { return null; }

    @Override
    public String evaluateSubTitle() {
        try {
            if (submittedPayload == null) return null;

            JSONObject jsonObject = new JSONObject(submittedPayload);
            String householdCount = CoreJsonFormUtils.getValue(jsonObject, "number_of_house_hold");
            long maleTotal = 0L;
            long femaleTotal = 0L;
            boolean hasMaleData = false;
            boolean hasFemaleData = false;

            String[] maleKeys = new String[]{
                    "number_of_male_by_age_group_less_1",
                    "number_of_male_by_age_group_1_4",
                    "number_of_male_by_age_group_5_14",
                    "number_of_male_by_age_group_15_49",
                    "number_of_male_by_age_group_50_59",
                    "number_of_male_by_age_group_equal_and_above_60"
            };

            String[] femaleKeys = new String[]{
                    "number_of_female_by_age_group_less_1",
                    "number_of_female_by_age_group_1_4",
                    "number_of_female_by_age_group_5_14",
                    "number_of_female_by_age_group_15_49",
                    "number_of_female_by_age_group_50_59",
                    "number_of_female_by_age_group_equal_and_above_60"
            };

            for (String key : maleKeys) {
                String value = CoreJsonFormUtils.getValue(jsonObject, key);
                if (value != null && !value.trim().isEmpty()) {
                    hasMaleData = true;
                    maleTotal += parseLong(value);
                }
            }

            for (String key : femaleKeys) {
                String value = CoreJsonFormUtils.getValue(jsonObject, key);
                if (value != null && !value.trim().isEmpty()) {
                    hasFemaleData = true;
                    femaleTotal += parseLong(value);
                }
            }

            StringBuilder subtitle = new StringBuilder();
            if (householdCount != null && !householdCount.trim().isEmpty()) {
                appendSegment(subtitle, context != null
                        ? context.getString(R.string.hps_annual_census_population_subtitle_households, householdCount.trim())
                        : "Households " + householdCount.trim());
            }
            if (hasMaleData) {
                appendSegment(subtitle, context != null
                        ? context.getString(R.string.hps_annual_census_population_subtitle_male, String.valueOf(maleTotal))
                        : "Male " + maleTotal);
            }
            if (hasFemaleData) {
                appendSegment(subtitle, context != null
                        ? context.getString(R.string.hps_annual_census_population_subtitle_female, String.valueOf(femaleTotal))
                        : "Female " + femaleTotal);
            }

            return subtitle.length() == 0 ? null : subtitle.toString();
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    @Override
    public BaseHpsVisitAction.Status evaluateStatusOnPayload() {
        if (householdCountValue == null || householdCountValue.trim().isEmpty())
            return BaseHpsVisitAction.Status.PENDING;
        return BaseHpsVisitAction.Status.COMPLETED;
    }

    @Override
    public void onPayloadReceived(BaseHpsVisitAction baseHpsVisitAction) { /* no-op */ }

    private void appendSegment(StringBuilder subtitle, String segment) {
        if (segment == null || segment.trim().isEmpty()) {
            return;
        }

        if (subtitle.length() > 0) {
            subtitle.append(", ");
        }
        subtitle.append(segment);
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return 0L;
        }
    }

}
