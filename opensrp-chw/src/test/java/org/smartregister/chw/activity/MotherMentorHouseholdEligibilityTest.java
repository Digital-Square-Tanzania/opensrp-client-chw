package org.smartregister.chw.activity;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;

import androidx.appcompat.view.menu.MenuBuilder;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.mothermentor.dao.MotherMentorHouseholdEligibilityDao;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class MotherMentorHouseholdEligibilityTest {
    private static final String FAMILY = "household-1129";
    private MockedStatic<ChwApplication> application;
    private MockedStatic<MotherMentorHouseholdEligibilityDao> eligibility;
    private ChwApplication.Flavor flavor;
    private FamilyProfileActivity activity;
    private MenuBuilder menu;

    @Before
    public void setUp() {
        flavor = mock(ChwApplication.Flavor.class);
        when(flavor.hasMotherMentor()).thenReturn(true);
        application = mockStatic(ChwApplication.class);
        application.when(ChwApplication::getApplicationFlavor).thenReturn(flavor);
        eligibility = mockStatic(MotherMentorHouseholdEligibilityDao.class);
        activity = Robolectric.buildActivity(FamilyProfileActivity.class).get();
        activity.setTheme(R.style.Theme_AppCompat);
        ReflectionHelpers.setField(activity, "familyBaseEntityId", FAMILY);
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_AppCompat);
        menu = new MenuBuilder(context);
        menu.add(0, R.id.action_mother_mentor_household_enrollment, 0, "Household enrollment");
    }

    @After
    public void tearDown() {
        eligibility.close();
        application.close();
    }

    @Test
    public void householdWithoutAnEligibleMotherHasNoEnrollmentOptionOrErrorToast() {
        activity.onPrepareOptionsMenu(menu);
        assertFalse(option().isVisible());
        activity.startMotherMentorHouseholdEnrollment(FAMILY);
        assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void menuReflectsEligibilityChangesWithoutRecreatingTheProfile() {
        activity.onPrepareOptionsMenu(menu);
        assertFalse(option().isVisible());
        eligibility.when(() -> MotherMentorHouseholdEligibilityDao.hasEligibleMother(FAMILY)).thenReturn(true);
        activity.onPrepareOptionsMenu(menu);
        assertTrue(option().isVisible());
        eligibility.when(() -> MotherMentorHouseholdEligibilityDao.hasEligibleMother(FAMILY)).thenReturn(false);
        activity.onPrepareOptionsMenu(menu);
        assertFalse(option().isVisible());
    }

    @Test
    public void anEnrolledHouseholdOrDisabledModuleIsNotOfferedAgain() {
        eligibility.when(() -> MotherMentorHouseholdEligibilityDao.hasEligibleMother(FAMILY)).thenReturn(true);
        eligibility.when(() -> MotherMentorHouseholdEligibilityDao.isEnrolled(FAMILY)).thenReturn(true);
        activity.onPrepareOptionsMenu(menu);
        assertFalse(option().isVisible());
        eligibility.when(() -> MotherMentorHouseholdEligibilityDao.isEnrolled(FAMILY)).thenReturn(false);
        when(flavor.hasMotherMentor()).thenReturn(false);
        activity.onPrepareOptionsMenu(menu);
        assertFalse(option().isVisible());
    }

    @Test
    public void directLaunchIsSilentWhenEligibilityChangedAndOpensTheExactHouseholdWhenValid() {
        Activity host = Robolectric.buildActivity(Activity.class).get();
        MotherMentorRegisterActivity.startHouseholdEnrollment(host, FAMILY);
        assertNull(Shadows.shadowOf(host).getNextStartedActivity());
        assertNull(ShadowToast.getTextOfLatestToast());
        eligibility.when(() -> MotherMentorHouseholdEligibilityDao.hasEligibleMother(FAMILY)).thenReturn(true);
        MotherMentorRegisterActivity.startHouseholdEnrollment(host, FAMILY);
        Intent intent = Shadows.shadowOf(host).getNextStartedActivity();
        assertNotNull(intent);
        assertEquals(FAMILY, intent.getStringExtra(org.smartregister.chw.mothermentor.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID));
        assertEquals(MotherMentorRegisterActivity.class.getName(), intent.getComponent().getClassName());
    }

    @Test
    public void losingTheEligibleMotherWhileAFormIsOpenSilentlyStopsItsSave() throws Exception {
        MotherMentorRegisterActivity registration = Robolectric.buildActivity(MotherMentorRegisterActivity.class).get();
        ReflectionHelpers.setField(registration, "BASE_ENTITY_ID", FAMILY);
        ReflectionHelpers.setField(registration, "FORM_NAME", "mama_kinara_household_enrolment");
        JSONObject form = new JSONObject().put("encounter_type", "mama_kinara_household_enrolment")
                .put("count", "1").put("step1", new JSONObject().put("fields", new JSONArray()));
        Intent data = new Intent().putExtra("json", form.toString());
        registration.onActivityResult(org.smartregister.chw.mothermentor.util.Constants.REQUEST_CODE_GET_JSON,
                Activity.RESULT_OK, data);
        eligibility.verify(() -> MotherMentorHouseholdEligibilityDao.hasEligibleMother(FAMILY));
        assertNull(ShadowToast.getTextOfLatestToast());
        assertNull(Shadows.shadowOf(registration).getNextStartedActivity());
    }

    @Test
    public void motherEnrollmentUsesTheSharedPregnancyAndChildWindowGate() throws Exception {
        MotherMentorRegisterActivity registration = Robolectric.buildActivity(MotherMentorRegisterActivity.class).get();
        for (String status : new String[] {"pregnant", "breast_feeding", "not_pregnant"}) {
            JSONObject form = new JSONObject().put("count", "1").put("step1", new JSONObject().put("fields",
                    new JSONArray().put(new JSONObject().put("key", "pregnancy_status").put("value", status))
                            .put(new JSONObject().put("key", "child_dob").put("value", "01-01-2020"))));
            boolean eligible = ReflectionHelpers.callInstanceMethod(registration, "isEligibleMother",
                    ReflectionHelpers.ClassParameter.from(JSONObject.class, form));
            assertEquals(!"not_pregnant".equals(status), eligible);
        }
    }

    private MenuItem option() { return menu.findItem(R.id.action_mother_mentor_household_enrollment); }
}
