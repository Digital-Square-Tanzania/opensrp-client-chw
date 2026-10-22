package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mockStatic;

import android.app.Application;
import android.content.Intent;

import com.vijay.jsonwizard.activities.JsonWizardFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.R;
import org.smartregister.chw.util.MotherMentorTestAssets;
import org.smartregister.family.activity.FamilyWizardFormActivity;
import org.smartregister.family.domain.FamilyMetadata;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.view.activity.BaseProfileActivity;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorRegisterFormTitleTest {

    @Test
    public void communityActivityLaunchUsesTheLocalizedFormTitle() throws Exception {
        for (String locale : new String[] {"json.form", "json.form-sw"}) {
            JSONObject form =
                    MotherMentorTestAssets.form(locale, "mothermentor_mobilization_session.json");
            String title = form.getJSONObject("step1").getString("title");

            CapturingRegisterActivity activity = launchForm(form);

            assertNull(launchedFormConfig(activity).getName());
            JSONObject launchedJson = launchedJson(activity);
            assertEquals(title, launchedJson.getJSONObject("step1").getString("title"));
            assertEquals(form.toString(), launchedJson.toString());
        }
    }

    @Test
    public void enrollmentLaunchKeepsTheRegistrationTitle() throws Exception {
        for (String locale : new String[] {"json.form", "json.form-sw"}) {
            JSONObject form = MotherMentorTestAssets.form(locale, "mothermentor_enrollment.json");

            CapturingRegisterActivity activity = launchForm(form);

            assertEquals(
                    activity.getString(R.string.mother_mentor_registration),
                    launchedFormConfig(activity).getName());
            assertEquals(form.toString(), launchedJson(activity).toString());
        }
    }

    private CapturingRegisterActivity launchForm(JSONObject form) {
        FamilyMetadata metadata =
                new FamilyMetadata(
                        JsonWizardFormActivity.class,
                        FamilyWizardFormActivity.class,
                        BaseProfileActivity.class,
                        "unique_id",
                        false);
        CapturingRegisterActivity activity =
                Robolectric.buildActivity(CapturingRegisterActivity.class, new Intent()).get();
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(Utils::metadata).thenReturn(metadata);
            activity.startFormActivity(form);
        }
        assertNotNull(activity.launchedIntent);
        assertEquals(JsonFormUtils.REQUEST_CODE_GET_JSON, activity.launchedRequestCode);
        assertEquals(
                FamilyWizardFormActivity.class.getName(),
                activity.launchedIntent.getComponent().getClassName());
        return activity;
    }

    private Form launchedFormConfig(CapturingRegisterActivity activity) {
        return (Form)
                activity.launchedIntent.getSerializableExtra(JsonFormConstants.JSON_FORM_KEY.FORM);
    }

    private JSONObject launchedJson(CapturingRegisterActivity activity) throws Exception {
        return new JSONObject(
                activity.launchedIntent.getStringExtra(
                        org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON));
    }

    public static class CapturingRegisterActivity extends MotherMentorRegisterActivity {
        private Intent launchedIntent;
        private int launchedRequestCode;

        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            launchedIntent = intent;
            launchedRequestCode = requestCode;
        }
    }
}
