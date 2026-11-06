package org.smartregister.chw.activity;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.Application;
import android.content.Intent;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.util.MotherMentorFormWizardUtils;
import org.smartregister.family.activity.FamilyWizardFormActivity;
import org.smartregister.family.domain.FamilyMetadata;
import org.smartregister.family.util.Utils;
import org.smartregister.view.activity.BaseProfileActivity;
import com.vijay.jsonwizard.activities.JsonWizardFormActivity;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorMotherChildWizardTest {
    @Test
    public void onlyRoutineMotherFormsWithChildAssessmentUseTheChildWizard() throws Exception {
        FamilyMetadata family = new FamilyMetadata(JsonWizardFormActivity.class,
                FamilyWizardFormActivity.class, BaseProfileActivity.class, "unique_id", false);
        try (MockedStatic<Utils> metadata = mockStatic(Utils.class)) {
            metadata.when(Utils::metadata).thenReturn(family);
            assertLaunch(form("Mother Mentor Visit Type", true), MotherMentorVisitWizardActivity.class);
            assertLaunch(form("Mother Mentor Visit Type", false), FamilyWizardFormActivity.class);
            assertLaunch(form("Mother Mentor Child EID Follow-up Visit", true), FamilyWizardFormActivity.class);
        }
        assertTrue(FamilyWizardFormActivity.class.isAssignableFrom(MotherMentorVisitWizardActivity.class));
    }

    private void assertLaunch(JSONObject form, Class<?> activity) {
        Intent intent = MotherMentorFormWizardUtils.getFormIntent(RuntimeEnvironment.getApplication(), form);
        assertEquals(activity.getName(), intent.getComponent().getClassName());
        assertEquals(form.toString(), intent.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON));
        Form config = (Form) intent.getSerializableExtra(JsonFormConstants.JSON_FORM_KEY.FORM);
        assertTrue(config.isWizard());
    }

    private JSONObject form(String encounter, boolean childAssessment) throws Exception {
        JSONArray fields = new JSONArray();
        if (childAssessment) fields.put(new JSONObject().put("key", DBConstants.KEY.MAMA_CHILD_STATUS));
        return new JSONObject().put("encounter_type", encounter)
                .put("step1", new JSONObject().put("fields", fields));
    }
}
