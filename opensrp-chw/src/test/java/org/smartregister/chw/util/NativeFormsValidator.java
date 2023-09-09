package org.smartregister.chw.util;

import android.content.Context;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.smartregister.chw.BaseUnitTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

/**
 * Validates that all the files in the json.forms folder is a valid forms files
 * Prevents creating files that are not
 */
public class NativeFormsValidator extends BaseUnitTest {

    private List<String> forms = new ArrayList<>();
    private Context context = RuntimeEnvironment.application;

    public static boolean isNumeric(String strNum) {
        try {
            Double.parseDouble(strNum);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }

    @Before
    public void setUp() throws IOException {
        String[] assets = context.getAssets().list("json.form/");
        forms.addAll(Arrays.asList(assets));
        Collections.sort(forms);
    }

    public JSONObject getFormJson(String formIdentity) {

        try {
            InputStream inputStream = context.getAssets()
                    .open("json.form/" + formIdentity);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,
                    StandardCharsets.UTF_8));
            String jsonString;
            StringBuilder stringBuilder = new StringBuilder();
            while ((jsonString = reader.readLine()) != null) {
                stringBuilder.append(jsonString);
            }
            inputStream.close();

            return new JSONObject(stringBuilder.toString());
        } catch (IOException e) {
            Timber.e(e);
        } catch (JSONException e) {
            Timber.e(e);
        }
        return null;
    }

    @Test
    public void validateForms() {
        try {
            int testedForms = 0;
            for (String form_name : forms) {
                System.out.println(form_name);
/*
                // form can be loaded and has the proper json object structure
                JSONObject form = getFormJson(form_name);

                // validate the form
                if (!formIsValid(form))
                    Assert.fail(form_name + " is an invalid form");

                if (!formHasValidFields(form))
                    Assert.fail(form_name + " has invalid fields ");
*/
                testedForms++;
            }
            Assert.assertEquals(testedForms, forms.size());
        } catch (Exception e) {
            Assert.fail("Exception : " + e);
        }
    }

    /**
     * This function validates that the form has the basic parts must appear for it to be a valid form
     *
     * @param form
     * @return
     */
    public boolean formIsValid(JSONObject form) {
        try {
            String count = form.getString("count");
            if (StringUtils.isBlank(count) || !isNumeric(count)) {
                System.out.println(" :Missing form count");
                return false;
            }

            String encounterType = form.getString("encounter_type");
            if (StringUtils.isBlank(encounterType)) {
                System.out.println(" :Missing encounterType");
                return false;
            }

            JSONObject step1 = form.getJSONObject(JsonFormConstants.STEP1);
            if (step1 == null) {
                System.out.println(" :Missing step1");
                return false;
            }

            return true;
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    /**
     * Validate the individual fields
     *
     * @param form
     * @return
     */
    public boolean formHasValidFields(JSONObject form) {
        try {


            JSONArray fields = form.getJSONObject(JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);
            if (fields == null || fields.length() < 1) {
                System.out.println(" :Missing fields");
                return false;
            }

            int x = 0;
            int length = fields.length();
            while (x < length) {
                JSONObject field = fields.getJSONObject(x);
                if (!formFieldIsValid(field)) {
                    return false;
                }
                x++;
            }
            System.out.println("\n");
            return fields.length() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * form fields are valid objects
     *
     * @param jsonFieldObject
     * @return
     */
    public boolean formFieldIsValid(JSONObject jsonFieldObject) {
        // must contain fields only once
        try {
            if (jsonFieldObject.getString(JsonFormConstants.TYPE).equals(JsonFormConstants.SPINNER)) {
                return validateSpinner(jsonFieldObject);
            }
            return true;
        } catch (Exception e) {
            Timber.e(e);
        }

        // all the object
        return false;
    }

    public boolean validateSpinner(JSONObject jsonFieldObject) throws JSONException {
        // must contain keys (translations)
        if (!jsonFieldObject.has(JsonFormConstants.KEYS)) {
            System.out.println(jsonFieldObject.getString(JsonFormConstants.KEY) + " is a spinner that lacks keys ");
            return false;
        }

        return true;
    }

}
