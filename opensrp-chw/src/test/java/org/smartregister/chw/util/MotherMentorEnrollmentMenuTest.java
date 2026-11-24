package org.smartregister.chw.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.view.MenuInflater;

import androidx.appcompat.view.menu.MenuBuilder;

import org.joda.time.DateTimeUtils;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;
import org.smartregister.repository.AllSharedPreferences;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class MotherMentorEnrollmentMenuTest {
    private ChwApplication.Flavor flavor;
    private MockedStatic<ChwApplication> application;
    private MockedStatic<org.smartregister.util.Utils> coreUtils;

    @Before
    public void setUp() {
        setDate(24);
        flavor = mock(ChwApplication.Flavor.class);
        when(flavor.hasMotherMentor()).thenReturn(true);
        application = mockStatic(ChwApplication.class);
        application.when(ChwApplication::getApplicationFlavor).thenReturn(flavor);
        AllSharedPreferences preferences = mock(AllSharedPreferences.class);
        coreUtils = mockStatic(org.smartregister.util.Utils.class, invocation ->
                "getAllSharedPreferences".equals(invocation.getMethod().getName())
                        ? preferences : invocation.callRealMethod());
    }

    @After
    public void tearDown() {
        coreUtils.close();
        application.close();
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void hidesBothActionsForGirlsAgedThreeAndNine() {
        assertActions("Female", "2023-11-24", false);
        assertActions("Female", "2017-11-24", false);
    }

    @Test
    public void offersBothActionsStartingOnTheTenthBirthday() {
        setDate(23);
        assertActions("Female", "2016-11-24", false);
        setDate(24);
        assertActions("Female", "2016-11-24", true);
    }

    @Test
    public void offersBothActionsForA50YearOldWoman() {
        assertActions("female", "1976-11-24", true);
    }

    @Test
    public void hidesBothActionsForMalesBelowAtAndAboveTen() {
        assertActions("Male", "2017-11-24", false);
        assertActions("Male", "2016-11-24", false);
        assertActions("Male", "1976-11-24", false);
    }

    @Test
    public void hidesBothActionsWhenAgeOrSexIsUnknown() {
        assertActions("Female", "", false);
        assertActions("Female", "invalid", false);
        assertActions("", "1976-11-24", false);
    }

    @Test
    public void disabledFeatureHidesBothActionsForAnOtherwiseEligibleClient() {
        when(flavor.hasMotherMentor()).thenReturn(false);
        assertActions("Female", "1976-11-24", false);
    }

    private void assertActions(String gender, String dob, boolean expected) {
        Map<String, String> columns = new HashMap<>();
        columns.put(DBConstants.KEY.GENDER, gender);
        columns.put(DBConstants.KEY.DOB, dob);
        CommonPersonObjectClient client = new CommonPersonObjectClient("synthetic-client", columns, "");
        client.setColumnmaps(columns);

        Application context = RuntimeEnvironment.getApplication();
        MenuBuilder menu = new MenuBuilder(context);
        new MenuInflater(context).inflate(org.smartregister.chw.core.R.menu.other_member_menu, menu);
        AllClientsUtils.updateOptionsMenu(menu, client);

        assertEquals(expected, menu.findItem(R.id.action_mother_mentor_enrollment).isVisible());
        assertEquals(expected, menu.findItem(R.id.action_mother_mentor_enroll_iit).isVisible());
    }

    private void setDate(int day) {
        DateTimeUtils.setCurrentMillisFixed(
                new LocalDate(2026, 11, day).toDateTimeAtStartOfDay().plusHours(12).getMillis());
    }
}
