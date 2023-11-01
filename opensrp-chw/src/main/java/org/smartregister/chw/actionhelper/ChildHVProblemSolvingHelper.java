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

public class ChildHVProblemSolvingHelper extends HomeVisitActionHelper {
    private String child_playing_challenge;

    @Override
    public void onPayloadReceived(String s) {
        try {
            JSONObject jsonObject = new JSONObject(s);
            child_playing_challenge = JsonFormUtils.getValue(jsonObject, "child_playing_challenge");
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {
        child_playing_challenge = getYesNoTranslation(child_playing_challenge);
        return MessageFormat.format("{0}: {1}", context.getString(R.string.child_problem_solving_task), child_playing_challenge);
    }

    public String getYesNoTranslation(String subtitleText) {
        if ("yes".equals(subtitleText)) {
            return context.getString(R.string.yes);
        } else if ("no".equals(subtitleText)) {
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
        if (StringUtils.isBlank(child_playing_challenge)) {
            return BaseAncHomeVisitAction.Status.PENDING;
        }

        if (StringUtils.isNotBlank(child_playing_challenge)) {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        } else {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        }
    }
}
