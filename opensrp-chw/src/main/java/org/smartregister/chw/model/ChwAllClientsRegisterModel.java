package org.smartregister.chw.model;

import static com.vijay.jsonwizard.utils.FormUtils.fields;
import static com.vijay.jsonwizard.utils.FormUtils.getFieldJSONObject;
import static org.smartregister.chw.util.JsonFormUtils.METADATA;
import static org.smartregister.family.util.JsonFormUtils.STEP2;
import static org.smartregister.util.JsonFormUtils.ENCOUNTER_LOCATION;
import static org.smartregister.util.JsonFormUtils.STEP1;

import android.content.Context;

import androidx.annotation.Nullable;

import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.chw.util.AllClientsUtils;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.opd.model.OpdRegisterActivityModel;
import org.smartregister.opd.pojo.OpdEventClient;
import org.smartregister.opd.utils.OpdUtils;

import java.util.List;

import timber.log.Timber;

public class ChwAllClientsRegisterModel extends OpdRegisterActivityModel {

    Context context;

    public ChwAllClientsRegisterModel(Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public JSONObject getFormAsJson(String formName, String entityId, String currentLocationId) {
        try {
            JSONObject form;
            if (context != null) {
                form = (new FormUtils()).getFormJsonFromRepositoryOrAssets(context, formName);
            } else {
                form = OpdUtils.getJsonFormToJsonObject(formName);
            }

            if (form == null) {
                return null;
            }

            form.getJSONObject(METADATA).put(ENCOUNTER_LOCATION, currentLocationId);

            String newEntityId = entityId;
            if (StringUtils.isNotBlank(entityId)) {
                newEntityId = entityId.replace("-", "");
            }

            JSONObject stepOneUniqueId = getFieldJSONObject(fields(form, STEP1), Constants.JSON_FORM_KEY.UNIQUE_ID);

            if (stepOneUniqueId != null) {
                stepOneUniqueId.remove(JsonFormUtils.VALUE);
                stepOneUniqueId.put(JsonFormUtils.VALUE, newEntityId + "_Family");
            }

            JSONObject stepTwoUniqueId = getFieldJSONObject(fields(form, STEP2), Constants.JSON_FORM_KEY.UNIQUE_ID);
            if (stepTwoUniqueId != null) {
                stepTwoUniqueId.remove(JsonFormUtils.VALUE);
                stepTwoUniqueId.put(JsonFormUtils.VALUE, newEntityId);
            }

            JsonFormUtils.addLocHierarchyQuestions(form);
            return form;

        } catch (Exception e) {
            Timber.e(e, "Error loading All Client registration form");
        }
        return null;
    }

    @Nullable
    @Override
    public List<OpdEventClient> processRegistration(String jsonString, FormTag formTag) {
        return AllClientsUtils.getOpdEventClients(jsonString);
    }
}
