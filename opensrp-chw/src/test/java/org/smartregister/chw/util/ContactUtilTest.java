package org.smartregister.chw.util;

import android.content.Intent;

import junit.framework.TestCase;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.activity.FamilyProfileActivity;
import org.smartregister.chw.application.ChwApplication;

import java.util.LinkedHashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(application = ChwApplication.class, sdk = 22)
public class ContactUtilTest {

    private FamilyProfileActivity activity;
    private ActivityController<FamilyProfileActivity> controller;


    public ContactUtilTest() {
        // expectedResult.put(13, LocalDate.parse("20/06/2019", DateTimeFormat.forPattern("dd/MM/yyyy"))); today's date
        Map<Integer, LocalDate> expectedResult = new LinkedHashMap<>();
        expectedResult.put(21, LocalDate.parse("15/08/2019", DateTimeFormat.forPattern("dd/MM/yyyy")));
        expectedResult.put(27, LocalDate.parse("26/09/2019", DateTimeFormat.forPattern("dd/MM/yyyy")));
        expectedResult.put(31, LocalDate.parse("24/10/2019", DateTimeFormat.forPattern("dd/MM/yyyy")));
        expectedResult.put(35, LocalDate.parse("21/11/2019", DateTimeFormat.forPattern("dd/MM/yyyy")));
        expectedResult.put(37, LocalDate.parse("05/12/2019", DateTimeFormat.forPattern("dd/MM/yyyy")));
        expectedResult.put(39, LocalDate.parse("19/12/2019", DateTimeFormat.forPattern("dd/MM/yyyy")));
        expectedResult.put(40, LocalDate.parse("25/12/2019", DateTimeFormat.forPattern("dd/MM/yyyy")));
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = Context.getInstance();
        CoreLibrary.init(context);

        //Auto login by default
        String password = "pwd";
        context.session().start(context.session().lengthInMilliseconds());
        context.configuration().getDrishtiApplication().setPassword(password);
        context.session().setPassword(password);

        MockitoAnnotations.initMocks(this);
        Intent testIntent = new Intent();
        controller = Robolectric.buildActivity(FamilyProfileActivity.class, testIntent).create().start();
        activity = controller.get();
    }

    @Test
    public void testGetContactWeeksVisits() {
        LocalDate lastContact = LocalDate.parse("19/06/2019", DateTimeFormat.forPattern("dd/MM/yyyy"));
        LocalDate lastMenstrualPeriod = LocalDate.parse("20/03/2019", DateTimeFormat.forPattern("dd/MM/yyyy"));

        Map<Integer, LocalDate> contacts = ContactUtil.getContactWeeks(false, lastContact, lastMenstrualPeriod);

        TestCase.assertNotNull(contacts);
        //assertTrue(isEqual(contacts, expectedResult));
    }

    @After
    public void tearDown() {
        try {
            activity.finish();
            controller.pause().stop().destroy(); //destroy controller if we can
        } catch (Exception e) {
            // done
        }

        //logout
        Context.getInstance().session().expire();
        System.gc();
    }
}
