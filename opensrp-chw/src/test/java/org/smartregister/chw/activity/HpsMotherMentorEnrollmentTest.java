package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.view.ContextThemeWrapper;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.view.menu.MenuBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.hps.domain.MemberObject;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.util.AllClientsUtils;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class HpsMotherMentorEnrollmentTest {
    private static final String CLIENT_ID = "hps-client";
    private static final String FAMILY_ID = "hps-family";

    private Context context;
    private HpsMemberProfileActivity activity;
    private MemberObject member;
    private ChwApplication.Flavor flavor;
    private MockedStatic<ChwApplication> application;
    private MockedStatic<MotherMentorDao> motherMentor;
    private MockedStatic<AllClientsUtils> allClients;

    @Before
    public void setUp() {
        context = new ContextThemeWrapper(
                RuntimeEnvironment.getApplication(), R.style.Theme_AppCompat);
        activity = mock(
                HpsMemberProfileActivity.class,
                withSettings()
                        .spiedInstance(Robolectric.buildActivity(HpsMemberProfileActivity.class).get())
                        .defaultAnswer(CALLS_REAL_METHODS));
        doReturn(new MenuInflater(context)).when(activity).getMenuInflater();
        doReturn(context.getResources()).when(activity).getResources();
        doReturn(null).when(activity).getSupportActionBar();
        doNothing().when(activity).startActivity(any(Intent.class));

        member = mock(MemberObject.class);
        when(member.getBaseEntityId()).thenReturn(CLIENT_ID);
        when(member.getFamilyBaseEntityId()).thenReturn(FAMILY_ID);
        when(member.getGender()).thenReturn("Female");
        when(member.getAge()).thenReturn(32);
        ReflectionHelpers.setField(activity, "memberObject", member);

        flavor = mock(ChwApplication.Flavor.class);
        when(flavor.hasMotherMentor()).thenReturn(true);
        application = mockStatic(ChwApplication.class);
        application.when(ChwApplication::getApplicationFlavor).thenReturn(flavor);
        motherMentor = mockStatic(MotherMentorDao.class);
        allClients = mockStatic(AllClientsUtils.class);
    }

    @After
    public void tearDown() {
        allClients.close();
        motherMentor.close();
        application.close();
    }

    @Test
    @Config(qualifiers = "en")
    public void eligibleFemaleHasLocalizedEnglishEnrollmentOption() {
        MenuItem enrollment = enrollment(menu());

        assertTrue(enrollment.isVisible());
        assertEquals("Mother Mentor Enrollment", enrollment.getTitle().toString());
    }

    @Test
    @Config(qualifiers = "sw")
    public void eligibleFemaleHasLocalizedSwahiliEnrollmentOption() {
        MenuItem enrollment = enrollment(menu());

        assertTrue(enrollment.isVisible());
        assertEquals("Usajili kwenye huduma za Mama Kinara", enrollment.getTitle().toString());
    }

    @Test
    public void maleClientHasNoMotherMentorEnrollmentOption() {
        when(member.getGender()).thenReturn("Male");

        assertFalse(enrollment(menu()).isVisible());
    }

    @Test
    public void missingGenderHasNoMotherMentorEnrollmentOption() {
        when(member.getGender()).thenReturn(null);

        assertFalse(enrollment(menu()).isVisible());
    }

    @Test
    public void disabledMotherMentorFlavorHidesEnrollmentOption() {
        when(flavor.hasMotherMentor()).thenReturn(false);

        assertFalse(enrollment(menu()).isVisible());
    }

    @Test
    public void alreadyEnrolledClientHasNoDuplicateEnrollmentOption() {
        motherMentor.when(() -> MotherMentorDao.isRegisteredForMotherMentor(CLIENT_ID))
                .thenReturn(true);

        assertFalse(enrollment(menu()).isVisible());
    }

    @Test
    public void hpsProfileDoesNotExposeSecondaryMotherMentorEnrollments() {
        MenuBuilder menu = menu();

        assertNotVisible(menu, R.id.action_mother_mentor_enroll_iit);
        assertNotVisible(menu, R.id.action_mothermentor_enroll_iit);
        assertNotVisible(menu, R.id.action_mothermentor_enroll_partner);
        assertNotVisible(menu, R.id.action_mothermentor_enroll_child_eid);
    }

    @Test
    public void eligibleEnrollmentClickOpensPrimaryFormWithExactClientAndFamilyDetails() {
        MenuItem enrollment = enrollment(menu());

        assertTrue(activity.onOptionsItemSelected(enrollment));

        ArgumentCaptor<Intent> intent = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivity(intent.capture());
        Intent registration = intent.getValue();
        assertEquals(
                MotherMentorRegisterActivity.class.getName(),
                registration.getComponent().getClassName());
        assertEquals(CLIENT_ID,
                registration.getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID));
        assertEquals(FAMILY_ID,
                registration.getStringExtra(Constants.ACTIVITY_PAYLOAD.FAMILY_BASE_ENTITY_ID));
        assertEquals("Female", registration.getStringExtra(
                org.smartregister.chw.kvp.util.Constants.ACTIVITY_PAYLOAD.GENDER));
        assertEquals(32, registration.getIntExtra(
                org.smartregister.chw.kvp.util.Constants.ACTIVITY_PAYLOAD.AGE, -1));
        assertEquals(Constants.FORMS.MOTHER_MENTOR_ENROLLMENT,
                registration.getStringExtra(Constants.ACTIVITY_PAYLOAD.MOTHER_MENTOR_FORM_NAME));
    }

    @Test
    public void staleEnrollmentClickDoesNotEnrollAnAlreadyEnrolledClientAgain() {
        MenuItem enrollment = enrollment(menu());
        motherMentor.when(() -> MotherMentorDao.isRegisteredForMotherMentor(CLIENT_ID))
                .thenReturn(true);

        activity.onOptionsItemSelected(enrollment);

        verify(activity, never()).startActivity(any(Intent.class));
    }

    @Test
    public void staleEnrollmentClickDoesNotEnrollAMaleClient() {
        MenuItem enrollment = enrollment(menu());
        when(member.getGender()).thenReturn("Male");

        activity.onOptionsItemSelected(enrollment);

        verify(activity, never()).startActivity(any(Intent.class));
    }

    @Test
    public void staleEnrollmentClickRespectsDisabledMotherMentorFlavor() {
        MenuItem enrollment = enrollment(menu());
        when(flavor.hasMotherMentor()).thenReturn(false);

        activity.onOptionsItemSelected(enrollment);

        verify(activity, never()).startActivity(any(Intent.class));
    }

    private MenuBuilder menu() {
        MenuBuilder menu = new MenuBuilder(context);
        assertTrue(activity.onCreateOptionsMenu(menu));
        return menu;
    }

    private MenuItem enrollment(MenuBuilder menu) {
        MenuItem item = menu.findItem(R.id.action_mother_mentor_enrollment);
        assertNotNull(item);
        return item;
    }

    private void assertNotVisible(MenuBuilder menu, int id) {
        MenuItem item = menu.findItem(id);
        assertFalse(item != null && item.isVisible());
    }
}
