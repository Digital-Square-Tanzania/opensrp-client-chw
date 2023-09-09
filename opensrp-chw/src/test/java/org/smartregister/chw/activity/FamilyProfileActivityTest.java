package org.smartregister.chw.activity;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.presenter.FamilyProfilePresenter;
import org.smartregister.domain.FetchStatus;
import org.smartregister.family.fragment.BaseFamilyProfileMemberFragment;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.JsonFormUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.times;

@RunWith(RobolectricTestRunner.class)
@Config(application = ChwApplication.class, sdk = 22)
public class FamilyProfileActivityTest {

    private FamilyProfileActivity activity;
    private ActivityController<FamilyProfileActivity> controller;
    @Mock
    private FamilyProfilePresenter presenter;

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
        String TEST_CARE_GIVER = "45645sdfs64564544";
        testIntent.putExtra(Constants.INTENT_KEY.FAMILY_HEAD, TEST_CARE_GIVER);
        String TEST_FAMILY_HEAD = "hsdf34453";
        testIntent.putExtra(Constants.INTENT_KEY.PRIMARY_CAREGIVER, TEST_FAMILY_HEAD);
        controller = Robolectric.buildActivity(FamilyProfileActivity.class, testIntent).create().start();

        activity = controller.get();
        ReflectionHelpers.setField(activity, "presenter", presenter);
    }

    @After
    public void tearDown() {
        try {
            activity.finish();
            controller.pause().stop().destroy(); //destroy controller if we can
        } catch (Exception e) {
            e.printStackTrace();
        }

        //logout
        Context.getInstance().session().expire();
        System.gc();
    }

    @Test
    public void refreshPresenter() throws Exception {

        FamilyProfileActivity spyActivity = Mockito.spy(activity);
        FamilyProfilePresenter presenter = Mockito.mock(FamilyProfilePresenter.class);

        ReflectionHelpers.setField(spyActivity, "presenter", presenter);
        // verify current presenter
        Assert.assertEquals(spyActivity.presenter(), presenter);

        ReflectionHelpers.callInstanceMethod(spyActivity, "refreshPresenter");

        // verify new presenter
        Assert.assertNotEquals(spyActivity.presenter(), presenter);
    }

    @Test
    public void testOnActivityResultVerifyJsonReceived() throws Exception {

        FamilyProfileActivity spyActivity = Mockito.spy(activity);
        JSONObject form = getFormJson(RuntimeEnvironment.application, CoreConstants.JSON_FORM.getFamilyMemberRegister());

        FamilyProfilePresenter presenter = Mockito.mock(FamilyProfilePresenter.class);
        ReflectionHelpers.setField(spyActivity, "presenter", presenter);

        int resultCode = Activity.RESULT_OK;
        int requestCode = JsonFormUtils.REQUEST_CODE_GET_JSON;
        Intent data = new Intent();
        data.putExtra(Constants.JSON_FORM_EXTRA.JSON, form.toString());

        Mockito.when(presenter.updatePrimaryCareGiver(spyActivity.getApplicationContext(), form.toString(), null, null)).thenReturn(true);

        spyActivity.onActivityResult(requestCode, resultCode, data);

        Mockito.verify(presenter).updatePrimaryCareGiver(spyActivity.getApplicationContext(), form.toString(), null, null);
    }

    public JSONObject getFormJson(Application mContext, String formIdentity) {
        if (mContext != null) {
            try {
                InputStream inputStream = mContext.getApplicationContext().getAssets()
                        .open("json" + ".form/" + formIdentity + ".json");
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                String jsonString;
                StringBuilder stringBuilder = new StringBuilder();

                while ((jsonString = reader.readLine()) != null) {
                    stringBuilder.append(jsonString);
                }
                inputStream.close();

                return new JSONObject(stringBuilder.toString());
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /*
    @Test
    public void testOnActivityResultChangeComplete() throws Exception {

        FamilyProfileActivity spyActivity = Mockito.spy(activity);

        int resultCode = Activity.RESULT_OK;
        int requestCode = CoreConstants.ProfileActivityResults.CHANGE_COMPLETED;
        Intent data = new Intent();
        data.putExtra(Constants.INTENT_KEY.FAMILY_HEAD, TEST_CARE_GIVER);
        data.putExtra(Constants.INTENT_KEY.PRIMARY_CAREGIVER, TEST_FAMILY_HEAD);


        BaseFamilyProfileMemberFragment profileMemberFragment = Mockito.mock(BaseFamilyProfileMemberFragment.class);
        Mockito.when(spyActivity.getProfileMemberFragment()).thenReturn(profileMemberFragment);

        // execute
        spyActivity.onActivityResult(requestCode, resultCode, data);

        // verify
        PowerMockito.verifyPrivate(spyActivity).invoke("refreshMemberFragment", TEST_CARE_GIVER, TEST_FAMILY_HEAD);
    }
     */

    @Test
    public void testActivityLoaded() {
        Assert.assertNotNull(activity);
    }

    @Test
    public void testRefreshMemberFragment() throws Exception {

        FamilyProfileActivity spyActivity = Mockito.spy(activity);
        BaseFamilyProfileMemberFragment fragment = Mockito.mock(BaseFamilyProfileMemberFragment.class);
        Mockito.when(spyActivity.getProfileMemberFragment()).thenReturn(fragment);

        String careGiverID = "123456";
        String familyHeadID = "7234556";

        // test that updates are sent to the member fragment
        ReflectionHelpers.callInstanceMethod(spyActivity, "refreshMemberFragment",
                ReflectionHelpers.ClassParameter.from(String.class, careGiverID),
                ReflectionHelpers.ClassParameter.from(String.class, familyHeadID)
        );

        Mockito.verify(fragment).setPrimaryCaregiver(careGiverID);
        Mockito.verify(fragment).setFamilyHead(familyHeadID);

        // test that updates are sent to the refresh member list

        Mockito.verify(spyActivity).refreshMemberList(FetchStatus.fetched);
    }

    @Test
    public void testSetEventDate() {
        FamilyProfileActivity spyActivity = Mockito.spy(activity);
        spyActivity.setEventDate(Mockito.anyString());
        Mockito.verify(spyActivity, times(1)).setEventDate(Mockito.anyString());
    }

    @Test
    public void testGetPresenter(){
        Assert.assertEquals(presenter, activity.getFamilyProfilePresenter());
    }
}
