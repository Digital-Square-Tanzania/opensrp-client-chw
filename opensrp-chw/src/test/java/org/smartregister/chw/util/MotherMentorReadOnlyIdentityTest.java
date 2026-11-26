package org.smartregister.chw.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorReadOnlyIdentityTest {
    @Test
    public void partnerMotherIdentifiersAreDisabledAndReboundWithoutChangingClinicalAnswers()
            throws Exception {
        for (String locale : new String[] {"json.form", "json.form-sw"}) {
            JSONObject form =
                    MotherMentorTestAssets.form(locale, "mothermentor_enroll_partner.json");
            Map<String, String> mother = person("mother", "Asha");
            mother.put("ctc_number", "12-34-5678-123456");
            Map<String, String> partner = person("partner", "Juma");
            Map<String, String> answers = new HashMap<>();
            answers.put("hiv_tested", "yes");
            answers.put("partner_ctc", "98-76-5432-123456");

            MotherMentorPartnerFormUtils.prepare(form, mother, partner, answers);

            assertDisplayOnly(field(form, "mother_name"));
            assertDisplayOnly(field(form, "mother_ctc_number"));
            for (String key :
                    new String[] {
                        "partner_name", "partner_sex", "partner_dob", "partner_phone_number"
                    }) {
                assertDisabledWithoutEditButton(field(form, key));
            }
            assertEditable(field(form, "partner_ctc"));
            field(form, "mother_name").put("value", "Untrusted name");
            field(form, "mother_ctc_number").put("value", "00-00-0000-000000");

            MotherMentorPartnerFormUtils.bindIdentity(form, mother, partner);

            assertEquals("Asha Mtemi", field(form, "mother_name").getString("value"));
            assertEquals(
                    "12-34-5678-123456", field(form, "mother_ctc_number").getString("value"));
            assertEquals("yes", field(form, "hiv_tested").getString("value"));
            assertEquals("98-76-5432-123456", field(form, "partner_ctc").getString("value"));
        }
    }

    @Test
    public void childMotherNameIsDisabledAndReboundWithoutChangingClinicalAnswers() throws Exception {
        for (String locale : new String[] {"json.form", "json.form-sw"}) {
            JSONObject form =
                    MotherMentorTestAssets.form(locale, "mothermentor_enroll_child_eid.json");
            Map<String, String> mother = person("mother", "Neema");
            Map<String, String> child = person("child", "Amina");
            Map<String, String> answers = new HashMap<>();
            answers.put("hei_number", "12-34-5678-123456");
            answers.put("last_test_result", "negative");

            MotherMentorChildEidFormUtils.prepare(form, mother, child, answers);

            assertDisplayOnly(field(form, "mother_name"));
            for (String key : new String[] {"child_name", "child_sex", "child_dob"}) {
                assertDisabledWithoutEditButton(field(form, key));
            }
            assertDisabledWithoutEditButton(field(form, "hei_number"));
            assertEditable(field(form, "hei_follow_up_number"));
            field(form, "mother_name").put("value", "Untrusted name");

            MotherMentorChildEidFormUtils.bindIdentity(form, mother, child);

            assertEquals("Neema Mtemi", field(form, "mother_name").getString("value"));
            assertEquals("12-34-5678-123456", field(form, "hei_number").getString("value"));
            assertEquals("negative", field(form, "last_test_result").getString("value"));
        }
    }

    private void assertDisplayOnly(JSONObject field) throws Exception {
        assertFalse(field.has("v_required"));
        assertDisabledWithoutEditButton(field);
    }

    private void assertDisabledWithoutEditButton(JSONObject field) throws Exception {
        EditText input = new EditText(RuntimeEnvironment.getApplication());
        ImageView editButton = new ImageView(RuntimeEnvironment.getApplication());

        // Both native text and masked-text factories apply this dependency method.
        FormUtils.setEditMode(field, input, editButton);

        assertFalse(field.getString("key"), input.isEnabled());
        assertEquals(View.GONE, editButton.getVisibility());
    }

    private void assertEditable(JSONObject field) throws Exception {
        EditText input = new EditText(RuntimeEnvironment.getApplication());
        FormUtils.setEditMode(field, input, new ImageView(RuntimeEnvironment.getApplication()));
        assertTrue(field.getString("key"), input.isEnabled());
    }

    private Map<String, String> person(String id, String name) {
        Map<String, String> person = new HashMap<>();
        person.put("base_entity_id", id);
        person.put("relational_id", "family");
        person.put("first_name", name);
        person.put("last_name", "Mtemi");
        return person;
    }

    private JSONObject field(JSONObject form, String key) throws Exception {
        JSONObject field = FormUtils.getFieldFromForm(form, key);
        if (field == null) throw new AssertionError("Missing field " + key);
        return field;
    }
}
