package org.smartregister.chw.actionhelper;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.JsonFormUtils;

import java.text.MessageFormat;

import timber.log.Timber;

public class ChildHVChildSafetyActionHelper extends HomeVisitActionHelper {
    private String child_safety_counselled;

    @Override
    public void onPayloadReceived(String s) {
        try {
            JSONObject jsonObject = new JSONObject(s);
            child_safety_counselled = JsonFormUtils.getValue(jsonObject, "child_safety_counselled");
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {
        child_safety_counselled = getYesNoTranslation(child_safety_counselled);
        return MessageFormat.format("{0}: {1}", context.getString(R.string.child_safety), child_safety_counselled);
    }

    public String getYesNoTranslation(String subtitleText) {
        if ("Yes".equals(subtitleText)) {
            return context.getString(R.string.yes);
        } else if ("No".equals(subtitleText)) {
            return context.getString(R.string.no);
        } else {
            return subtitleText;
        }
    }

    @Override
    public String postProcess(String jsonPayload) {
        return super.postProcess(jsonPayload);
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(child_safety_counselled)) {
            return BaseAncHomeVisitAction.Status.PENDING;
        }

        if (StringUtils.isNotBlank(child_safety_counselled) &&
                (child_safety_counselled.equalsIgnoreCase("Ndio") ||
                        child_safety_counselled.equalsIgnoreCase("Yes") ||
                        child_safety_counselled.equalsIgnoreCase("Ndiyo"))) {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        }
    }
}

