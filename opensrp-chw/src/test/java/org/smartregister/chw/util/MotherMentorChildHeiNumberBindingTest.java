package org.smartregister.chw.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.mothermentor.util.DBConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorChildHeiNumberBindingTest {
    @Test
    public void preparationBindsTheLinkedMothersCtcAndRestoresAnExistingSuffix() throws Exception {
        for (String language : languages()) {
            JSONObject form = form(language);
            Map<String, String> mother = mother();
            MotherMentorChildEidFormUtils.prepare(form, mother, child(),
                    Collections.singletonMap(DBConstants.KEY.HEI_NUMBER, "12-34-5678-123456-C-03"));
            JSONObject ctc = field(form, DBConstants.KEY.MOTHER_CTC_NUMBER);
            assertEquals("12-34-5678-123456", ctc.getString("value"));
            assertTrue(ctc.getBoolean("read_only"));
            assertFalse(ctc.has("editable"));
            assertEquals("03", field(form, DBConstants.KEY.HEI_FOLLOW_UP_NUMBER).getString("value"));
            assertEquals("child", form.getString("entity_id"));
        }
    }

    @Test
    public void savingRebuildsTheHeiNumberFromTrustedMotherDataInBothLanguages() throws Exception {
        for (String language : languages()) {
            JSONObject form = form(language);
            MotherMentorChildEidFormUtils.prepare(form, mother(), child(), Collections.emptyMap());
            field(form, DBConstants.KEY.HEI_REGISTERED).put("value", "yes");
            field(form, DBConstants.KEY.HEI_FOLLOW_UP_NUMBER).put("value", "01");
            field(form, DBConstants.KEY.MOTHER_CTC_NUMBER).put("value", "wrong-mother");
            field(form, DBConstants.KEY.HEI_NUMBER).put("value", "wrong-number");
            field(form, DBConstants.KEY.LAST_TEST_RESULT).put("value", "negative");
            MotherMentorChildEidFormUtils.bindIdentity(form, mother(), child());
            MotherMentorChildEidFormUtils.bindHeiNumber(form, mother());
            assertEquals("12-34-5678-123456-C-01", field(form, DBConstants.KEY.HEI_NUMBER).getString("value"));
            assertEquals("12-34-5678-123456", field(form, DBConstants.KEY.MOTHER_CTC_NUMBER).getString("value"));
            assertEquals("negative", field(form, DBConstants.KEY.LAST_TEST_RESULT).getString("value"));
        }
    }

    @Test
    public void notRegisteredDoesNotSaveAStaleHeiNumber() throws Exception {
        for (String language : languages()) {
            JSONObject form = form(language);
            field(form, DBConstants.KEY.HEI_REGISTERED).put("value", "no");
            field(form, DBConstants.KEY.HEI_NUMBER).put("value", "12-34-5678-123456-C-01");
            MotherMentorChildEidFormUtils.bindHeiNumber(form, Collections.emptyMap());
            assertEquals("", field(form, DBConstants.KEY.HEI_NUMBER).getString("value"));
        }
    }

    @Test
    public void incompleteFollowUpNumbersAndMissingMotherCtcCannotBeSaved() throws Exception {
        for (String language : languages()) {
            for (String suffix : new String[] {"", "1", "001", "ab"}) {
                JSONObject form = form(language);
                field(form, DBConstants.KEY.HEI_REGISTERED).put("value", "yes");
                field(form, DBConstants.KEY.HEI_FOLLOW_UP_NUMBER).put("value", suffix);
                assertRejected(form, mother());
            }
            JSONObject form = form(language);
            field(form, DBConstants.KEY.HEI_REGISTERED).put("value", "yes");
            field(form, DBConstants.KEY.HEI_FOLLOW_UP_NUMBER).put("value", "01");
            assertRejected(form, Collections.emptyMap());
        }
    }

    private void assertRejected(JSONObject form, Map<String, String> mother) throws Exception {
        try {
            MotherMentorChildEidFormUtils.bindHeiNumber(form, mother);
            throw new AssertionError("Incomplete HEI identity was accepted");
        } catch (IllegalArgumentException expected) {
            // The form also validates these values before the user can submit it.
        }
    }

    private JSONObject form(String language) throws Exception {
        return MotherMentorTestAssets.form(language, "mothermentor_enroll_child_eid.json");
    }

    private JSONObject field(JSONObject form, String key) throws Exception {
        return FormUtils.getFieldFromForm(form, key);
    }

    private Map<String, String> mother() {
        Map<String, String> mother = new HashMap<>();
        mother.put(DBConstants.KEY.BASE_ENTITY_ID, "mother");
        mother.put(DBConstants.KEY.CTC_NUMBER, " 12-34-5678-123456 ");
        return mother;
    }

    private Map<String, String> child() {
        return Collections.singletonMap(DBConstants.KEY.BASE_ENTITY_ID, "child");
    }

    private String[] languages() {
        return new String[] {"json.form", "json.form-sw"};
    }
}
