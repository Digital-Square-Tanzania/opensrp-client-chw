package org.smartregister.chw.util;

import com.nerdstone.neatformcore.domain.model.NFormViewData;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.dao.TreatmentSupporterDao;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TreatmentSupporterFormUtilTest {

    /**
     * Builds a NeatForm-shaped referral form containing the treatment supporter
     * section (gate + name + phone + relationship) plus an unrelated field.
     */
    private JSONObject referralFormWithSection() throws Exception {
        JSONArray fields = new JSONArray();
        fields.put(field("problem", "multi_choice_checkbox"));
        fields.put(spinnerField(TreatmentSupporterFormUtil.FIELD_GATE, "Yes", "No"));
        fields.put(field(TreatmentSupporterFormUtil.FIELD_NAME, "text_input_edit_text"));
        fields.put(field(TreatmentSupporterFormUtil.FIELD_PHONE, "text_input_edit_text"));
        fields.put(spinnerField("treatment_supporter_relationship",
                "Mother", "Father", "Brother", "Sister"));

        JSONObject step = new JSONObject().put("title", "Referral").put("fields", fields);
        return new JSONObject().put("steps", new JSONArray().put(step));
    }

    private JSONObject spinnerField(String name, String... optionNames) throws Exception {
        JSONArray options = new JSONArray();
        for (String optionName : optionNames) {
            options.put(new JSONObject().put("name", optionName).put("text", optionName));
        }
        return new JSONObject()
                .put("name", name)
                .put("type", "spinner")
                .put("properties", new JSONObject().put("hint", name))
                .put("options", options);
    }

    private JSONObject referralFormWithoutSection() throws Exception {
        JSONArray fields = new JSONArray();
        fields.put(field("problem", "multi_choice_checkbox"));
        fields.put(field("chw_referral_hf", "spinner"));
        JSONObject step = new JSONObject().put("title", "Referral").put("fields", fields);
        return new JSONObject().put("steps", new JSONArray().put(step));
    }

    private JSONObject field(String name, String type) throws Exception {
        return new JSONObject()
                .put("name", name)
                .put("type", type)
                .put("properties", new JSONObject().put("hint", name));
    }

    private JSONObject fieldByName(JSONObject form, String name) throws Exception {
        JSONArray fields = form.getJSONArray("steps").getJSONObject(0).getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            if (name.equals(fields.getJSONObject(i).optString("name"))) {
                return fields.getJSONObject(i);
            }
        }
        return null;
    }

    private String prop(JSONObject form, String fieldName, String key) throws Exception {
        JSONObject field = fieldByName(form, fieldName);
        return field == null ? null : field.getJSONObject("properties").optString(key, null);
    }

    // ----- injectValues (pure JSON) -----

    @Test
    public void injectValuesSetsGateNamePhoneAndRelationship() throws Exception {
        JSONObject form = referralFormWithSection();

        TreatmentSupporterFormUtil.injectValues(form, "Jane Doe", "0712345678", "Brother");

        // Spinner selection is the 0-based option index: gate "Yes" = 0, "Brother" = 2.
        Assert.assertEquals("0", prop(form, TreatmentSupporterFormUtil.FIELD_GATE, "selection"));
        Assert.assertEquals("Jane Doe", prop(form, TreatmentSupporterFormUtil.FIELD_NAME, "text"));
        Assert.assertEquals("0712345678", prop(form, TreatmentSupporterFormUtil.FIELD_PHONE, "text"));
        Assert.assertEquals("2", prop(form, TreatmentSupporterFormUtil.FIELD_RELATIONSHIP, "selection"));
    }

    @Test
    public void injectValuesLeavesRelationshipBlankWhenNotRecorded() throws Exception {
        JSONObject form = referralFormWithSection();

        TreatmentSupporterFormUtil.injectValues(form, "Jane Doe", "0712345678", null);

        Assert.assertEquals("Jane Doe", prop(form, TreatmentSupporterFormUtil.FIELD_NAME, "text"));
        Assert.assertNull(prop(form, TreatmentSupporterFormUtil.FIELD_RELATIONSHIP, "selection"));
    }

    @Test
    public void injectValuesLeavesRelationshipBlankWhenOptionUnknown() throws Exception {
        JSONObject form = referralFormWithSection();

        // A relationship value that is not one of the spinner options must not select anything.
        TreatmentSupporterFormUtil.injectValues(form, "Jane Doe", "0712345678", "Cousin");

        Assert.assertNull(prop(form, TreatmentSupporterFormUtil.FIELD_RELATIONSHIP, "selection"));
    }

    @Test
    public void injectValuesSetsGateEvenWhenDetailsBlank() throws Exception {
        JSONObject form = referralFormWithSection();

        TreatmentSupporterFormUtil.injectValues(form, "  ", null, "  ");

        Assert.assertEquals("0", prop(form, TreatmentSupporterFormUtil.FIELD_GATE, "selection"));
        Assert.assertNull(prop(form, TreatmentSupporterFormUtil.FIELD_NAME, "text"));
        Assert.assertNull(prop(form, TreatmentSupporterFormUtil.FIELD_PHONE, "text"));
        Assert.assertNull(prop(form, TreatmentSupporterFormUtil.FIELD_RELATIONSHIP, "selection"));
    }

    @Test
    public void injectValuesTrimsValues() throws Exception {
        JSONObject form = referralFormWithSection();

        TreatmentSupporterFormUtil.injectValues(form, "  Jane  ", "  0712  ", "  Sister  ");

        Assert.assertEquals("Jane", prop(form, TreatmentSupporterFormUtil.FIELD_NAME, "text"));
        Assert.assertEquals("0712", prop(form, TreatmentSupporterFormUtil.FIELD_PHONE, "text"));
        // "Sister" is option index 3.
        Assert.assertEquals("3", prop(form, TreatmentSupporterFormUtil.FIELD_RELATIONSHIP, "selection"));
    }

    @Test
    public void injectValuesIsNoOpWhenFormHasNoSection() throws Exception {
        JSONObject form = referralFormWithoutSection();
        String before = form.toString();

        TreatmentSupporterFormUtil.injectValues(form, "Jane Doe", "0712345678", "Mother");

        Assert.assertEquals(before, form.toString());
    }

    // ----- prefillFromRegistration guard paths (no static mocking required) -----

    @Test
    public void prefillHandlesNullForm() {
        // must not throw
        TreatmentSupporterFormUtil.prefillFromRegistration("abc-123", null);
    }

    @Test
    public void prefillLeavesFormUnchangedWhenNoRegistrationData() throws Exception {
        // No matching row in the (mocked) test DB => DAO yields no caregiver and any
        // lookup error is swallowed, so the form must be returned untouched.
        JSONObject form = referralFormWithSection();
        String before = form.toString();

        TreatmentSupporterFormUtil.prefillFromRegistration("no-such-entity", form);

        Assert.assertEquals(before, form.toString());
    }

    // ----- ensureTreatmentSupporterObs (save-time safety net) -----

    @Test
    public void viewDataCarriesObsMetadata() {
        NFormViewData data = TreatmentSupporterFormUtil.viewData(
                "text_input_edit_text", "treatment_supporter_name", "Jane Doe");

        Assert.assertEquals("Jane Doe", data.getValue());
        Assert.assertEquals("text_input_edit_text", data.getType());
        Map<String, Object> meta = data.getMetadata();
        Assert.assertEquals("concept", meta.get("openmrs_entity"));
        Assert.assertEquals("treatment_supporter_name", meta.get("openmrs_entity_id"));
        Assert.assertEquals("", meta.get("openmrs_entity_parent"));
        Assert.assertTrue(data.getVisible());
    }

    @Test
    public void ensureObsHandlesNullMap() {
        // must not throw
        TreatmentSupporterFormUtil.ensureTreatmentSupporterObs("abc-123", null);
    }

    @Test
    public void ensureObsLeavesMapUnchangedWhenNoRegistrationData() {
        // No matching row in the (mocked) test DB => no caregiver => map untouched.
        Map<String, NFormViewData> formData = new HashMap<>();
        formData.put("problem", TreatmentSupporterFormUtil.viewData("multi", "problem", "fever"));

        TreatmentSupporterFormUtil.ensureTreatmentSupporterObs("no-such-entity", formData);

        Assert.assertEquals(1, formData.size());
        Assert.assertFalse(formData.containsKey(TreatmentSupporterFormUtil.FIELD_GATE));
    }

    // ----- Caregiver.isPresent() -----

    @Test
    public void resolvePrefersOwnCapturedCaregiver() {
        // has_primary_caregiver == "Yes" => use the client's own name/phone/relationship,
        // ignoring the household fallback.
        TreatmentSupporterDao.Caregiver c = TreatmentSupporterDao.resolve(
                "client-1", "Yes", "Jane", "0712", "Mother",
                "head-9", "Head Person", "0755");
        Assert.assertTrue(c.isPresent());
        Assert.assertEquals("Jane", c.getName());
        Assert.assertEquals("0712", c.getPhone());
        Assert.assertEquals("Mother", c.getRelationship());
    }

    @Test
    public void resolveFallsBackToHouseholdCaregiver() {
        // No own answer => use the household caregiver's name + phone, relationship blank.
        TreatmentSupporterDao.Caregiver c = TreatmentSupporterDao.resolve(
                "client-1", "No", null, null, null,
                "head-9", "Head Person", "0755");
        Assert.assertTrue(c.isPresent());
        Assert.assertEquals("Head Person", c.getName());
        Assert.assertEquals("0755", c.getPhone());
        Assert.assertNull(c.getRelationship());
    }

    @Test
    public void resolveSkipsHouseholdCaregiverWhenItIsTheClient() {
        // The client must never be treated as their own caregiver.
        TreatmentSupporterDao.Caregiver c = TreatmentSupporterDao.resolve(
                "client-1", null, null, null, null,
                "client-1", "Self Person", "0755");
        Assert.assertFalse(c.isPresent());
    }

    @Test
    public void resolveNotPresentWhenNoSource() {
        Assert.assertFalse(TreatmentSupporterDao.resolve(
                "client-1", "No", null, null, null, null, null, null).isPresent());
        // Household id set but no usable details => not present.
        Assert.assertFalse(TreatmentSupporterDao.resolve(
                "client-1", null, null, null, null, "head-9", "  ", "  ").isPresent());
    }
}
