package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.vijay.jsonwizard.activities.JsonWizardFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.AllConstants;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.repository.MotherMentorChildEidRepository;
import org.smartregister.chw.repository.MotherMentorPartnerRepository;
import org.smartregister.chw.util.MotherMentorChildEidFormUtils;
import org.smartregister.chw.util.MotherMentorFormWizardUtils;
import org.smartregister.chw.util.MotherMentorPartnerFormUtils;
import org.smartregister.chw.util.MotherMentorTestAssets;
import org.smartregister.family.activity.FamilyWizardFormActivity;
import org.smartregister.family.domain.FamilyMetadata;
import org.smartregister.family.util.AppExecutors;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.LangUtils;
import org.smartregister.view.activity.BaseProfileActivity;

import java.util.HashMap;
import java.util.Map;

/** Exercises actual form launch methods without adding a manifest-only test activity. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorFormWizardLaunchTest {
    private MockedStatic<Utils> metadata;

    @Before
    public void setUp() {
        FamilyMetadata family =
                new FamilyMetadata(
                        JsonWizardFormActivity.class,
                        FamilyWizardFormActivity.class,
                        BaseProfileActivity.class,
                        "unique_id",
                        false);
        metadata = mockStatic(Utils.class);
        metadata.when(Utils::metadata).thenReturn(family);
    }

    @After
    public void tearDown() {
        if (metadata != null) metadata.close();
        LangUtils.saveLanguage(RuntimeEnvironment.getApplication(), "en");
    }

    @Test
    public void serviceAndAllFollowUpFormsLaunchWithLocalizedWizardNavigation() throws Exception {
        for (String language : new String[] {"en", "sw"}) {
            language(language);
            CapturingVisitActivity activity =
                    Robolectric.buildActivity(CapturingVisitActivity.class, new Intent()).get();
            for (String name :
                    new String[] {
                        "mothermentor_visit_type",
                        "mothermentor_follow_up_status",
                        "mothermentor_partner_follow_up_status",
                        "mothermentor_child_eid_test_type"
                    }) {
                JSONObject form = asset(language, name);

                activity.startFormActivity(form);

                assertTrue(config(activity.launch.intent).isWizard());
                assertFalse(
                        activity.launch.intent.getBooleanExtra(
                                Constants.WizardFormActivity.EnableOnCloseDialog, true));
                assertLaunch(activity, activity.launch, form);
            }
        }
    }

    @Test
    public void householdActionsLaunchWithLocalizedWizardNavigation() throws Exception {
        for (String language : new String[] {"en", "sw"}) {
            language(language);
            CapturingHouseholdActivity activity =
                    Robolectric.buildActivity(CapturingHouseholdActivity.class, new Intent()).get();
            JSONObject form = asset(language, "mothermentor_household_visit_purpose");

            activity.startFormActivity(form);

            assertTrue(config(activity.launch.intent).isWizard());
            assertFalse(
                    activity.launch.intent.getBooleanExtra(
                            Constants.WizardFormActivity.EnableOnCloseDialog, true));
            assertLaunch(activity, activity.launch, form);
        }
    }

    @Test
    public void newPartnerRegistrationKeepsExtrasAndUsesWizard() throws Exception {
        for (String language : new String[] {"en", "sw"}) {
            language(language);
            CapturingPartnerActivity activity =
                    Robolectric.buildActivity(CapturingPartnerActivity.class, new Intent()).get();
            JSONObject form = asset(language, "all_clients_registration_form");
            HashMap<String, String> extras = new HashMap<>();
            extras.put("registration_context", "mother-mentor-partner");

            activity.startFormActivity(form, extras);

            assertTrue(config(activity.launch.intent).isWizard());
            assertEquals(
                    "mother-mentor-partner",
                    activity.launch.intent.getStringExtra("registration_context"));
            assertLaunch(activity, activity.launch, form);
        }
    }

    @Test
    public void newChildRegistrationKeepsExtrasAndUsesWizard() throws Exception {
        for (String language : new String[] {"en", "sw"}) {
            language(language);
            CapturingChildActivity activity =
                    Robolectric.buildActivity(CapturingChildActivity.class, new Intent()).get();
            JSONObject form = asset(language, "all_clients_registration_form");
            HashMap<String, String> extras = new HashMap<>();
            extras.put("registration_context", "mother-mentor-child");

            activity.startFormActivity(form, extras);

            assertTrue(config(activity.launch.intent).isWizard());
            assertEquals(
                    "mother-mentor-child",
                    activity.launch.intent.getStringExtra("registration_context"));
            assertLaunch(activity, activity.launch, form);
        }
    }

    @Test
    public void existingPartnerLinkKeepsPreparedIdentityAndStepsInBothLanguages() throws Exception {
        for (String language : new String[] {"en", "sw"}) {
            language(language);
            CapturingPartnerActivity activity =
                    Robolectric.buildActivity(CapturingPartnerActivity.class, new Intent()).get();
            JSONObject form = asset(language, MotherMentorPartnerFormUtils.FORM);
            MotherMentorPartnerRepository repository = mock(MotherMentorPartnerRepository.class);
            when(repository.prepareForm(any(JSONObject.class), eq("mother"), eq("partner")))
                    .thenAnswer(
                            call -> {
                                MotherMentorPartnerFormUtils.bindIdentity(
                                        form, person("mother"), person("partner"));
                                return form;
                            });
            injectSelection(activity, repository);

            try (SelectionFormFixture fixture = new SelectionFormFixture(form)) {
                ReflectionHelpers.callInstanceMethod(
                        activity,
                        "openPartnerForm",
                        ReflectionHelpers.ClassParameter.from(String.class, "partner"));
            }

            assertNotNull(activity.launch.intent);
            assertTrue(config(activity.launch.intent).isWizard());
            assertEquals("partner", launchedJson(activity.launch).getString("entity_id"));
            assertEquals(
                    "mother",
                    com.vijay.jsonwizard.utils.FormUtils.getFieldFromForm(
                                    launchedJson(activity.launch), "mother_base_entity_id")
                            .getString("value"));
            assertLaunch(activity, activity.launch, form);
        }
    }

    @Test
    public void existingChildLinkKeepsPreparedIdentityAndStepsInBothLanguages() throws Exception {
        for (String language : new String[] {"en", "sw"}) {
            language(language);
            CapturingChildActivity activity =
                    Robolectric.buildActivity(CapturingChildActivity.class, new Intent()).get();
            JSONObject form = asset(language, MotherMentorChildEidFormUtils.FORM);
            MotherMentorChildEidRepository repository = mock(MotherMentorChildEidRepository.class);
            when(repository.prepareForm(any(JSONObject.class), eq("mother"), eq("child")))
                    .thenAnswer(
                            call -> {
                                MotherMentorChildEidFormUtils.bindIdentity(
                                        form, person("mother"), person("child"));
                                return form;
                            });
            injectSelection(activity, repository);

            try (SelectionFormFixture fixture = new SelectionFormFixture(form)) {
                ReflectionHelpers.callInstanceMethod(
                        activity,
                        "openChildForm",
                        ReflectionHelpers.ClassParameter.from(String.class, "child"));
            }

            assertNotNull(activity.launch.intent);
            assertTrue(config(activity.launch.intent).isWizard());
            assertEquals("child", launchedJson(activity.launch).getString("entity_id"));
            assertEquals(
                    "mother",
                    com.vijay.jsonwizard.utils.FormUtils.getFieldFromForm(
                                    launchedJson(activity.launch), "mother_base_entity_id")
                            .getString("value"));
            assertLaunch(activity, activity.launch, form);
        }
    }

    @Test
    public void profileFormsUseWizardInsteadOfTheInheritedNonWizardIntent() throws Exception {
        for (String language : new String[] {"en", "sw"}) {
            language(language);
            CapturingProfileActivity activity =
                    Robolectric.buildActivity(CapturingProfileActivity.class, new Intent()).get();
            JSONObject form = asset(language, "mothermentor_enroll_iit");

            activity.startFormActivity(form);

            assertTrue(config(activity.launch.intent).isWizard());
            assertLaunch(activity, activity.launch, form);
        }
    }

    @Test
    public void configuringExistingIntentRetainsNameExtrasAndAllFormSteps() throws Exception {
        for (String language : new String[] {"en", "sw"}) {
            Context context = language(language);
            JSONObject form = asset(language, "mothermentor_enrollment");
            Form previous = new Form();
            previous.setName("Preserved registration title");
            previous.setWizard(false);
            Intent intent = new Intent(context, FamilyWizardFormActivity.class);
            intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, form.toString());
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, previous);
            intent.putExtra("selected_client", "mother");

            Intent configured = MotherMentorFormWizardUtils.configureIntent(context, intent);

            assertSame(intent, configured);
            assertTrue(config(configured).isWizard());
            assertEquals("Preserved registration title", config(configured).getName());
            assertEquals("mother", configured.getStringExtra("selected_client"));
            assertEquals(
                    form.toString(), configured.getStringExtra(Constants.JSON_FORM_EXTRA.JSON));
            assertNavigation(context, config(configured));
        }
    }

    private Context language(String language) {
        LangUtils.saveLanguage(RuntimeEnvironment.getApplication(), language);
        return LangUtils.setAppLocale(RuntimeEnvironment.getApplication(), language);
    }

    private JSONObject asset(String language, String name) throws Exception {
        return MotherMentorTestAssets.form(
                "sw".equals(language) ? "json.form-sw" : "json.form", name + ".json");
    }

    private void assertLaunch(Context context, Launch launch, JSONObject form) throws Exception {
        assertNotNull(launch.intent);
        assertEquals(JsonFormUtils.REQUEST_CODE_GET_JSON, launch.requestCode);
        assertEquals(
                FamilyWizardFormActivity.class.getName(),
                launch.intent.getComponent().getClassName());
        assertEquals(form.toString(), launchedJson(launch).toString());
        assertNavigation(context, config(launch.intent));
    }

    private void assertNavigation(Context context, Form form) {
        assertFalse(form.isHidePreviousButton());
        assertFalse(form.isHidePreviousIcon());
        assertEquals(
                context.getString(org.smartregister.chw.core.R.string.next), form.getNextLabel());
        assertEquals(
                context.getString(org.smartregister.chw.core.R.string.back),
                form.getPreviousLabel());
        assertEquals(
                context.getString(org.smartregister.chw.core.R.string.save), form.getSaveLabel());
        assertEquals(
                org.smartregister.chw.core.R.color.family_navigation,
                form.getNavigationBackground());
        assertEquals(
                org.smartregister.chw.core.R.mipmap.ic_cross_white, form.getHomeAsUpIndicator());
    }

    private Form config(Intent intent) {
        return (Form) intent.getSerializableExtra(JsonFormConstants.JSON_FORM_KEY.FORM);
    }

    private JSONObject launchedJson(Launch launch) throws Exception {
        return new JSONObject(launch.intent.getStringExtra(Constants.JSON_FORM_EXTRA.JSON));
    }

    private void injectSelection(Activity activity, Object repository) {
        ReflectionHelpers.setField(activity, "repository", repository);
        ReflectionHelpers.setField(activity, "motherId", "mother");
        ReflectionHelpers.setField(
                activity,
                "executors",
                new AppExecutors(Runnable::run, Runnable::run, Runnable::run));
    }

    private Map<String, String> person(String id) {
        Map<String, String> values = new HashMap<>();
        values.put("base_entity_id", id);
        values.put("relational_id", "family");
        values.put("first_name", id);
        return values;
    }

    private static class SelectionFormFixture implements AutoCloseable {
        private final MockedStatic<FormUtils> forms = mockStatic(FormUtils.class);
        private final MockedStatic<org.smartregister.util.Utils> preferences =
                mockStatic(org.smartregister.util.Utils.class);

        SelectionFormFixture(JSONObject form) throws Exception {
            org.smartregister.util.FormUtils formUtils =
                    mock(org.smartregister.util.FormUtils.class);
            forms.when(FormUtils::getFormUtils).thenReturn(formUtils);
            when(formUtils.getFormJson(anyString())).thenReturn(form);
            AllSharedPreferences sharedPreferences = mock(AllSharedPreferences.class);
            when(sharedPreferences.getPreference(AllConstants.CURRENT_LOCATION_ID))
                    .thenReturn("test-location");
            preferences.when(org.smartregister.util.Utils::getAllSharedPreferences)
                    .thenReturn(sharedPreferences);
        }

        @Override
        public void close() {
            preferences.close();
            forms.close();
        }
    }

    private static class Launch {
        private Intent intent;
        private int requestCode;

        void capture(Intent intent, int requestCode) {
            this.intent = intent;
            this.requestCode = requestCode;
        }
    }

    public static class CapturingVisitActivity extends MotherMentorVisitActivity {
        final Launch launch = new Launch();

        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            launch.capture(intent, requestCode);
        }
    }

    public static class CapturingHouseholdActivity extends MotherMentorHouseholdVisitActivity {
        final Launch launch = new Launch();

        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            launch.capture(intent, requestCode);
        }
    }

    public static class CapturingPartnerActivity extends MotherMentorPartnerRegisterActivity {
        final Launch launch = new Launch();

        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            launch.capture(intent, requestCode);
        }

        @Override
        public void showProgressDialog(int title) {}

        @Override
        public void hideProgressDialog() {}
    }

    public static class CapturingChildActivity extends MotherMentorChildEidRegisterActivity {
        final Launch launch = new Launch();

        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            launch.capture(intent, requestCode);
        }

        @Override
        public void showProgressDialog(int title) {}

        @Override
        public void hideProgressDialog() {}
    }

    public static class CapturingProfileActivity extends MotherMentorProfileActivity {
        final Launch launch = new Launch();

        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            launch.capture(intent, requestCode);
        }
    }
}
