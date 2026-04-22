package org.smartregister.chw.model;

import org.json.JSONObject;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.family.model.BaseFamilyRegisterModel;

public class FamilyRegisterModel extends BaseFamilyRegisterModel {
    @Override
    public JSONObject getFormAsJson(String formName, String entityId, String currentLocationId) throws Exception {
        JSONObject form = getFormUtils().getFormJson(formName);
        if (form == null) {
            return null;
        }
        return JsonFormUtils.getFormAsJson(form, formName, entityId, currentLocationId);
    }
}
