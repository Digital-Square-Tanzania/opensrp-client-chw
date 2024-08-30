package org.smartregister.chw.model;

import static org.smartregister.util.JsonFormUtils.FIELDS;
import static org.smartregister.util.JsonFormUtils.STEP1;

import com.google.common.collect.Sets;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.hivst.model.BaseHivstRegisterModel;
import org.smartregister.chw.hivst.util.HivstJsonFormUtils;
import org.smartregister.dao.LocationsDao;
import org.smartregister.domain.Location;
import org.smartregister.util.JsonFormUtils;

import java.util.List;

import timber.log.Timber;

public class HivstRegisterModel extends BaseHivstRegisterModel {
    @Override
    public JSONObject getFormAsJson(String formName, String entityId, String currentLocationId, String gender, int age) throws Exception {
        JSONObject jsonObject = HivstJsonFormUtils.getFormAsJson(formName);
        HivstJsonFormUtils.getRegistrationForm(jsonObject, entityId, currentLocationId);
        JSONObject global = jsonObject.getJSONObject("global");
        global.put("gender", gender);
        global.put("age", age);


        JSONArray fields = jsonObject.getJSONObject(STEP1).getJSONArray(FIELDS);

        JSONObject clientWard = JsonFormUtils.getFieldJSONObject(fields, "name_of_ward");
        if (clientWard != null) {
            try {
                List<Location> locationList = LocationsDao.getLocationsByTags(Sets.newHashSet("Ward"));
                JSONArray options = clientWard.getJSONArray("options");
                for (Location location : locationList) {
                    JSONObject optionNode = new JSONObject();
                    optionNode.put("text", StringUtils.capitalize(location.getProperties().getName()));
                    optionNode.put("key", StringUtils.capitalize(location.getProperties().getName()));
                    JSONObject propertyObject = new JSONObject();
                    propertyObject.put("presumed-id", location.getProperties().getUid());
                    propertyObject.put("confirmed-id", location.getProperties().getUid());
                    optionNode.put("property", propertyObject);

                    options.put(optionNode);
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        return jsonObject;
    }
}
