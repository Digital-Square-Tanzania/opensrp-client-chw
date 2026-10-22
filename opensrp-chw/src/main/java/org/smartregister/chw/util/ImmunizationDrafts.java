package org.smartregister.chw.util;

import android.os.Bundle;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.contract.ImmunizationSaveHost;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

/** Keeps dialog acknowledgements and accepted drafts with their original visit actions. */
public class ImmunizationDrafts {
    private static final String REQUESTS = "immunization_requests";
    private static final String DRAFTS = "immunization_drafts";
    private Bundle requests = new Bundle();
    private Bundle drafts = new Bundle();

    public void restoreState(Bundle state) {
        if (state == null) return;
        Bundle savedRequests = state.getBundle(REQUESTS);
        Bundle savedDrafts = state.getBundle(DRAFTS);
        if (savedRequests != null) requests = new Bundle(savedRequests);
        if (savedDrafts != null) drafts = new Bundle(savedDrafts);
    }

    public void saveState(Bundle state) {
        state.putBundle(REQUESTS, new Bundle(requests));
        state.putBundle(DRAFTS, new Bundle(drafts));
    }

    public boolean bind(String ownerId, BaseAncHomeVisitAction action) {
        if (!(action.getDestinationFragment() instanceof ImmunizationSaveHost.Dialog)) return true;
        if (TextUtils.isEmpty(ownerId)) return false;
        String requestId = UUID.randomUUID().toString();
        requests.putString(action.getTitle(), requestId);
        ((ImmunizationSaveHost.Dialog) action.getDestinationFragment()).bindToVisitAction(
                ownerId, action.getTitle(), requestId, action.getJsonPayload());
        return true;
    }

    public boolean accept(String ownerId, Map<String, BaseAncHomeVisitAction> actions,
                          String visitId, String actionId, String childId, String requestId, String payload) {
        BaseAncHomeVisitAction action = actions.get(actionId);
        if (!matches(action, ownerId, visitId, childId) || !action.isEnabled() || !action.isValid()
                || TextUtils.isEmpty(requestId)
                || !requestId.equals(requests.getString(actionId)) || !payloadMatches(payload, childId)) return false;
        Bundle previous = drafts.getBundle(actionId);
        if (previous != null && requestId.equals(previous.getString("request_id"))) {
            return payload.equals(previous.getString("payload"));
        }
        // Validators synchronously read the destination fragment's accepted dates.
        if (!((ImmunizationSaveHost.Dialog) action.getDestinationFragment()).applyAcceptedPayload(payload)) return false;
        action.setJsonPayload(payload);
        Bundle draft = new Bundle();
        draft.putString("visit_id", visitId);
        draft.putString("child_id", childId);
        draft.putString("request_id", requestId);
        draft.putString("payload", payload);
        drafts.putBundle(actionId, draft);
        // An earlier vaccination edit can invalidate later windows through the real validator.
        for (String key : new ArrayList<>(requests.keySet())) {
            BaseAncHomeVisitAction other = actions.get(key);
            if (other != null && TextUtils.isEmpty(other.getJsonPayload())) {
                drafts.remove(key);
                requests.remove(key);
            }
        }
        return true;
    }

    public void restoreActions(String ownerId, Map<String, BaseAncHomeVisitAction> actions) {
        // Replay in visit order: each accepted window recalculates the following one.
        for (Map.Entry<String, BaseAncHomeVisitAction> entry : actions.entrySet()) {
            Bundle draft = drafts.getBundle(entry.getKey());
            BaseAncHomeVisitAction action = entry.getValue();
            if (draft == null || !matches(action, ownerId, draft.getString("visit_id"), draft.getString("child_id"))) continue;
            String payload = draft.getString("payload");
            if (payloadMatches(payload, draft.getString("child_id"))
                    && ((ImmunizationSaveHost.Dialog) action.getDestinationFragment()).applyAcceptedPayload(payload)
                    && !payload.equals(action.getJsonPayload())) action.setJsonPayload(payload);
        }
    }

    private boolean matches(BaseAncHomeVisitAction action, String ownerId, String visitId, String childId) {
        return !TextUtils.isEmpty(ownerId) && ownerId.equals(visitId) && !TextUtils.isEmpty(childId)
                && action != null && childId.equals(action.getBaseEntityID())
                && action.getDestinationFragment() instanceof ImmunizationSaveHost.Dialog;
    }

    private boolean payloadMatches(String payload, String childId) {
        if (TextUtils.isEmpty(payload) || TextUtils.isEmpty(childId)) return false;
        try {
            JSONObject form = new JSONObject(payload);
            if (!childId.equals(form.optString("entity_id"))) return false;
            JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
            int missingReasonsFields = 0;
            for (int index = 0; index < fields.length(); index++) {
                if ("reasons_no_vaccination".equals(fields.getJSONObject(index).optString("key"))) missingReasonsFields++;
            }
            return missingReasonsFields == 1;
        } catch (JSONException e) {
            return false;
        }
    }
}
