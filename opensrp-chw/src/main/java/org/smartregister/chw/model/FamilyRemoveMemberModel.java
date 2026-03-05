package org.smartregister.chw.model;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.anc.util.DBConstants;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.model.CoreFamilyRemoveMemberModel;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.chw.util.Utils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.domain.FamilyEventClient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

public class FamilyRemoveMemberModel extends CoreFamilyRemoveMemberModel {

    @Override
    public String getForm(CommonPersonObjectClient client) {
        Date dob = Utils.dobStringToDate(Utils.getValue(client.getColumnmaps(), DBConstants.KEY.DOB, false));
        if (ChwApplication.getApplicationFlavor().showChildrenUnderFiveAndGirlsAgeNineToEleven()) {
            return ((dob != null && getDiffYears(dob, new Date()) >= 11) ? CoreConstants.JSON_FORM.getFamilyDetailsRemoveMember() : CoreConstants.JSON_FORM.getFamilyDetailsRemoveChild());
        }
        return ((dob != null && getDiffYears(dob, new Date()) >= 5) ? CoreConstants.JSON_FORM.getFamilyDetailsRemoveMember() : CoreConstants.JSON_FORM.getFamilyDetailsRemoveChild());
    }


    @Override
    public JSONObject getFormAsJson(String formName, String entityId, String baseEntityId, String currentLocationId) throws Exception {
        JSONObject form = getFormUtils().getFormJson(formName);
        if (form == null) {
            return null;
        }

        JsonFormUtils.populateExistingHead(form, baseEntityId);

        return JsonFormUtils.getFormAsJson(form, formName, entityId, currentLocationId);
    }

    public List<FamilyEventClient> processFamilyMemberRemoval(String jsonString) {
        List<FamilyEventClient> familyEventClientList = new ArrayList<>();
        try {
            // Existing flow to create the family client/event
            FamilyEventClient familyEventClient = org.smartregister.family.util.JsonFormUtils.processFamilyUpdateForm(
                    org.smartregister.family.util.Utils.context().allSharedPreferences(), jsonString);
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
                    familyEventClient.getClient().addIdentifier(org.smartregister.family.util.Utils.metadata().uniqueIdentifierKey, familyUid);
                }

                // Set relationships on the family client to the chosen existing head
                familyEventClient.getClient().addRelationship(
                        org.smartregister.family.util.Utils.metadata().familyRegister.familyHeadRelationKey, chosenHead);
                familyEventClient.getClient().addRelationship(
                        org.smartregister.family.util.Utils.metadata().familyRegister.familyCareGiverRelationKey, chosenHead);



                // Close the reserved unique ID used to derive the family identifier so it won't be reused
                try {
                    String reserved = extractReservedFamilyIdFromStepOne(form);
                    if (StringUtils.isNotBlank(reserved)) {
                        org.smartregister.CoreLibrary.getInstance()
                                .context()
                                .getUniqueIdRepository()
                                .close(reserved);
                    }
                } catch (Exception e) {
                    timber.log.Timber.w(e, "Failed to close reserved family unique ID");
                }

                // Only the family event/client is needed; do not create a head client
                familyEventClientList.add(familyEventClient);
                return familyEventClientList;
            }

            // Fallback to the library's default behavior (new head)
            return super.processFamilyMemberRemoval(jsonString);
        } catch (Exception e) {
            Timber.e(e);
        }
        return familyEventClientList;
    }

    /**
     * Extracts the reserved OpenSRP ID used for the family registration form from Step 1.
     * Prefers the dedicated "family_unique_id" when available, otherwise falls back to Step 1 "unique_id".
     * Removes the "_family" suffix (case-insensitive) and any hyphens to get the raw OpenSRP ID to close.
     */
    private String extractReservedFamilyIdFromStepOne(JSONObject form) {
        if (form == null) return null;
        try {
            JSONObject stepOne = form.optJSONObject(org.smartregister.family.util.JsonFormUtils.STEP1);
            if (stepOne == null) return null;
            JSONArray fields = stepOne.optJSONArray(com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS);
            if (fields == null) return null;

            String familyUniqueId = getFieldValue(fields, "family_unique_id");
            if (StringUtils.isBlank(familyUniqueId)) {
                familyUniqueId = getFieldValue(fields, "unique_id");
            }
            if (StringUtils.isBlank(familyUniqueId)) return null;

            String id = familyUniqueId;
            // Normalize by removing suffix and hyphens
            String lower = id.toLowerCase();
            if (lower.endsWith("_family")) {
                id = id.substring(0, id.length() - "_family".length());
            }
            id = id.replace("-", "");
            return StringUtils.isNotBlank(id) ? id : null;
        } catch (Exception e) {
            timber.log.Timber.w(e);
            return null;
        }
    }

    private String getFieldValue(JSONArray fields, String key) {
        try {
            if (fields == null) return null;
            for (int i = 0; i < fields.length(); i++) {
                JSONObject obj = fields.optJSONObject(i);
                if (obj != null && key.equals(obj.optString(com.vijay.jsonwizard.constants.JsonFormConstants.KEY))) {
                    return obj.optString(com.vijay.jsonwizard.constants.JsonFormConstants.VALUE);
                }
            }
        } catch (Exception e) {
            timber.log.Timber.w(e);
        }
        return null;
    }
}

