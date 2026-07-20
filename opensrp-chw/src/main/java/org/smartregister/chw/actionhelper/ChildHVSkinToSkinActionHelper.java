package org.smartregister.chw.actionhelper;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;

import timber.log.Timber;

public class ChildHVSkinToSkinActionHelper  extends HomeVisitActionHelper {
    private String skin_to_skin;

    @Override
    public void onPayloadReceived(String jsonString) {

        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            skin_to_skin = org.smartregister.chw.util.JsonFormUtils.getValue(jsonObject, "skin_to_skin_counselling");
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isNotBlank(skin_to_skin)) {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.PENDING;
        }
    }
}
