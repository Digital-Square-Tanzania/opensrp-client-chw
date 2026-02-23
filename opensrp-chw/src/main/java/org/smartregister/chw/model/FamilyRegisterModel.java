package org.smartregister.chw.model;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.family.domain.FamilyEventClient;
import org.smartregister.family.model.BaseFamilyRegisterModel;
import org.smartregister.family.util.Utils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class FamilyRegisterModel extends BaseFamilyRegisterModel {
    @Override
    public JSONObject getFormAsJson(String formName, String entityId, String currentLocationId) throws Exception {
        JSONObject form = getFormUtils().getFormJson(formName);
        if (form == null) {
            return null;
        }
        return JsonFormUtils.getFormAsJson(form, formName, entityId, currentLocationId);
    }

    @Override
    public List<FamilyEventClient> processRegistration(String jsonString) {
        List<FamilyEventClient> familyEventClientList = new ArrayList<>();
        try {
            // Existing flow to create the family client/event
            FamilyEventClient familyEventClient = org.smartregister.family.util.JsonFormUtils.processFamilyUpdateForm(
                    Utils.context().allSharedPreferences(), jsonString);
            if (familyEventClient == null) {
                return familyEventClientList;
            }

            // Detect "existing head" path: prefer hidden family_head (Step 1), fallback to existing_head (Step 2)
            JSONObject form = new JSONObject(jsonString);
            String chosenHead = JsonFormUtils.getValue(form, "family_head");
            if (StringUtils.isBlank(chosenHead)) {
                chosenHead = JsonFormUtils.getValue(form, "existing_head");
            }

            if (StringUtils.isNotBlank(chosenHead)) {
                // Set family unique id from family_unique_id when present (avoids head dependency)
                String familyUid = JsonFormUtils.getValue(form, "family_unique_id");
                if (StringUtils.isNotBlank(familyUid)) {
                    familyEventClient.getClient().addIdentifier(Utils.metadata().uniqueIdentifierKey, familyUid);
                }

                // Set relationships on the family client to the chosen existing head
                familyEventClient.getClient().addRelationship(
                        Utils.metadata().familyRegister.familyHeadRelationKey, chosenHead);
                familyEventClient.getClient().addRelationship(
                        Utils.metadata().familyRegister.familyCareGiverRelationKey, chosenHead);

                // Only the family event/client is needed; do not create a head client
                familyEventClientList.add(familyEventClient);
                return familyEventClientList;
            }

            // Fallback to the library's default behavior (new head)
            return super.processRegistration(jsonString);
        } catch (Exception e) {
            Timber.e(e);
        }
        return familyEventClientList;
    }
}
