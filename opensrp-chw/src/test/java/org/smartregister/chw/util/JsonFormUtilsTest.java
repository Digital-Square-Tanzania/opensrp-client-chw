package org.smartregister.chw.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class JsonFormUtilsTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Test
    public void getTimeZone() {
        String timeZone = JsonFormUtils.getTimeZone();
        boolean matches = timeZone.matches("^\\+\\d\\d:\\d0$");
        assert (matches);
    }

    @Test
    public void testGetValue() {
        JSONObject jsonObject = null;
        String key = "exclusive_breast_feeding";
        try {
             jsonObject = new JSONObject("{\"count\":\"1\",\"encounter_type\":\"Exclusive breast feeding\",\"entity_id\":\"1234567890\",\"metadata\":{\"phonenumber\":{\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_data_type\":\"phonenumber\",\"openmrs_entity_id\":\"openmrs_entity_id\"},\"encounter_location\":\"\"},\"step1\":{\"title\":\"Exclusive breastfeeding\",\"fields\":[{\"key\":\"exclusive_breast_feeding\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"\",\"openmrs_data_type\":\"exclusive_breast_feeding\",\"type\":\"spinner\",\"image\":\"ic_form_bf\",\"hint\":\"Did the child receive any liquid or food other than breast milk yesterday and last night?\",\"values\":[\"Yes\",\"No\"],\"openmrs_choice_ids\":{\"Yes\":\"key_yes\",\"No\":\"key_no\"},\"value\":\"Yes\"}]}}");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Assert.assertEquals("Yes", JsonFormUtils.getValue(jsonObject, key));
    }

    @Test
    public void testPopulateExistingHeadSetsReadOnlyFields() throws Exception {
        JSONObject form = new JSONObject();
        form.put(JsonFormUtils.METADATA, new JSONObject());

        JSONObject stepTwo = new JSONObject();
        JSONArray fields = new JSONArray();
        stepTwo.put(JsonFormConstants.FIELDS, fields);
        form.put(org.smartregister.family.util.JsonFormUtils.STEP2, stepTwo);

        addField(fields, "existing_head");
        addField(fields, "first_name");
        addField(fields, "middle_name");
        addField(fields, "surname");
        addField(fields, DBConstants.KEY.DOB);
        addField(fields, "age");
        addField(fields, "sex");
        addField(fields, "unique_id");
        addField(fields, "phone_number");
        addField(fields, "other_phone_number");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JsonFormConstants.VALUE, "");

        java.util.HashMap<String, String> columnMap = new java.util.HashMap<>();
        columnMap.put(DBConstants.KEY.FIRST_NAME, "Jane");
        columnMap.put(DBConstants.KEY.MIDDLE_NAME, "M");
        columnMap.put(DBConstants.KEY.LAST_NAME, "Doe");
        columnMap.put(DBConstants.KEY.GENDER, "Female");
        columnMap.put(DBConstants.KEY.DOB, "1990-01-01");
        columnMap.put(DBConstants.KEY.UNIQUE_ID, "12345");
        columnMap.put(DBConstants.KEY.PHONE_NUMBER, "0712345678");
        columnMap.put(DBConstants.KEY.OTHER_PHONE_NUMBER, "0799999999");

        CommonPersonObjectClient client = new CommonPersonObjectClient("case-id", columnMap, "Jane Doe");
        client.setColumnmaps(columnMap);

        JsonFormUtils.populateExistingHead(form, client);

        JSONObject uniqueIdField = findField(fields, "unique_id");
        Assert.assertEquals("12345", uniqueIdField.optString(JsonFormConstants.VALUE));
        Assert.assertEquals("true", uniqueIdField.optString(JsonFormUtils.READ_ONLY));

        JSONObject existingHeadField = findField(fields, "existing_head");
        Assert.assertEquals("case-id", existingHeadField.optString(JsonFormConstants.VALUE));

        JSONObject phoneField = findField(fields, "phone_number");
        Assert.assertEquals("0712345678", phoneField.optString(JsonFormConstants.VALUE));
        Assert.assertEquals("", phoneField.optString(JsonFormUtils.READ_ONLY));
    }

    @Test
    public void testPopulateExistingHeadStripsHyphensFromUniqueId() throws Exception {
        JSONObject form = new JSONObject();
        form.put(JsonFormUtils.METADATA, new JSONObject());

        JSONObject stepTwo = new JSONObject();
        JSONArray fields = new JSONArray();
        stepTwo.put(JsonFormConstants.FIELDS, fields);
        form.put(org.smartregister.family.util.JsonFormUtils.STEP2, stepTwo);

        addField(fields, "unique_id");

        java.util.HashMap<String, String> columnMap = new java.util.HashMap<>();
        columnMap.put(DBConstants.KEY.UNIQUE_ID, "123-45-678");

        CommonPersonObjectClient client = new CommonPersonObjectClient("case-id", columnMap, "Jane Doe");
        client.setColumnmaps(columnMap);

        JsonFormUtils.populateExistingHead(form, client);

        JSONObject uniqueIdField = findField(fields, "unique_id");
        Assert.assertEquals("12345678", uniqueIdField.optString(JsonFormConstants.VALUE));
        Assert.assertEquals("true", uniqueIdField.optString(JsonFormUtils.READ_ONLY));
    }

    @Test
    public void testPopulateExistingHeadFormatsDobAndMapsGender() throws Exception {
        JSONObject form = new JSONObject();
        form.put(JsonFormUtils.METADATA, new JSONObject());

        JSONObject stepTwo = new JSONObject();
        JSONArray fields = new JSONArray();
        stepTwo.put(JsonFormConstants.FIELDS, fields);
        form.put(org.smartregister.family.util.JsonFormUtils.STEP2, stepTwo);

        addField(fields, "dob");
        addField(fields, "sex");
        addField(fields, "age");

        java.util.HashMap<String, String> columnMap = new java.util.HashMap<>();
        columnMap.put(DBConstants.KEY.DOB, "1990-01-01");
        columnMap.put(DBConstants.KEY.GENDER, "F");

        CommonPersonObjectClient client = new CommonPersonObjectClient("case-id", columnMap, "Jane Doe");
        client.setColumnmaps(columnMap);

        JsonFormUtils.populateExistingHead(form, client);

        JSONObject dobField = findField(fields, "dob");
        Assert.assertEquals("01-01-1990", dobField.optString(JsonFormConstants.VALUE));

        JSONObject sexField = findField(fields, "sex");
        Assert.assertEquals("Female", sexField.optString(JsonFormConstants.VALUE));

        JSONObject ageField = findField(fields, "age");
        Assert.assertNotNull(ageField);
    }

    @Test
    public void testPopulateExistingHeadWritesAgeCalculatedWhenAgeMissing() throws Exception {
        JSONObject form = new JSONObject();
        form.put(JsonFormUtils.METADATA, new JSONObject());

        JSONObject stepTwo = new JSONObject();
        JSONArray fields = new JSONArray();
        stepTwo.put(JsonFormConstants.FIELDS, fields);
        form.put(org.smartregister.family.util.JsonFormUtils.STEP2, stepTwo);

        addField(fields, "dob");
        addField(fields, "age_calculated");

        java.util.HashMap<String, String> columnMap = new java.util.HashMap<>();
        columnMap.put(DBConstants.KEY.DOB, "1990-01-01");

        CommonPersonObjectClient client = new CommonPersonObjectClient("case-id", columnMap, "Jane Doe");
        client.setColumnmaps(columnMap);

        JsonFormUtils.populateExistingHead(form, client);

        JSONObject ageCalculated = findField(fields, "age_calculated");
        Assert.assertNotNull(ageCalculated);
        Assert.assertTrue(ageCalculated.has(JsonFormConstants.VALUE));
    }

    @Test
    public void testPopulateExistingHeadPrefillsStep1ForNacpFlavorCalculations() throws Exception {
        JSONObject form = new JSONObject();
        form.put(JsonFormUtils.METADATA, new JSONObject());

        // Step 1 with client_first_name, client_middle_name, fam_name
        JSONObject stepOne = new JSONObject();
        JSONArray stepOneFields = new JSONArray();
        stepOne.put(JsonFormConstants.FIELDS, stepOneFields);
        form.put(org.smartregister.family.util.JsonFormUtils.STEP1, stepOne);
        addField(stepOneFields, "client_first_name");
        addField(stepOneFields, "client_middle_name");
        addField(stepOneFields, "fam_name");

        // Step 2 (minimal) so method executes normally
        JSONObject stepTwo = new JSONObject();
        JSONArray stepTwoFields = new JSONArray();
        stepTwo.put(JsonFormConstants.FIELDS, stepTwoFields);
        form.put(org.smartregister.family.util.JsonFormUtils.STEP2, stepTwo);
        addField(stepTwoFields, "first_name");

        java.util.HashMap<String, String> columnMap = new java.util.HashMap<>();
        columnMap.put(DBConstants.KEY.FIRST_NAME, "Jane");
        columnMap.put(DBConstants.KEY.MIDDLE_NAME, "M");
        columnMap.put(DBConstants.KEY.LAST_NAME, "Doe");

        CommonPersonObjectClient client = new CommonPersonObjectClient("case-id", columnMap, "Jane Doe");
        client.setColumnmaps(columnMap);

        JsonFormUtils.populateExistingHead(form, client);

        JSONObject s1First = findField(stepOneFields, "client_first_name");
        JSONObject s1Middle = findField(stepOneFields, "client_middle_name");
        JSONObject s1Fam = findField(stepOneFields, "fam_name");

        Assert.assertEquals("Jane", s1First.optString(JsonFormConstants.VALUE));
        Assert.assertEquals("M", s1Middle.optString(JsonFormConstants.VALUE));
        Assert.assertEquals("Doe", s1Fam.optString(JsonFormConstants.VALUE));
    }

    private JSONObject addField(JSONArray fields, String key) throws JSONException {
        JSONObject field = new JSONObject();
        field.put(JsonFormUtils.KEY, key);
        fields.put(field);
        return field;
    }

    private JSONObject findField(JSONArray fields, String key) {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject obj = fields.optJSONObject(i);
            if (obj != null && key.equals(obj.optString(JsonFormUtils.KEY))) {
                return obj;
            }
        }
        return null;
    }
}
