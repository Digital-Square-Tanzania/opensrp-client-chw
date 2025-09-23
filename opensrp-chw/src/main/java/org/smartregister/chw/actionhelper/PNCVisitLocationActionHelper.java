package org.smartregister.chw.actionhelper;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;

import timber.log.Timber;

public class PNCVisitLocationActionHelper extends HomeVisitActionHelper {
    private String gpsLocation;

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONArray fields = org.smartregister.chw.anc.util.JsonFormUtils.fields(jsonObject);
            gpsLocation = org.smartregister.chw.anc.util.JsonFormUtils.getFieldValue(fields, "gps");
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {
        if (gpsLocation != null && !gpsLocation.isEmpty()) {
            return context.getString(R.string.pnc_hv_location_captured);
        }else{
            return "";
        }
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(gpsLocation))
            return BaseAncHomeVisitAction.Status.PENDING;
        return BaseAncHomeVisitAction.Status.COMPLETED;
    }
}
