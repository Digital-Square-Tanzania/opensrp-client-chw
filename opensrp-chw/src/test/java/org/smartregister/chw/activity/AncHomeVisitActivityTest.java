package org.smartregister.chw.activity;

import android.content.Intent;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.anc.contract.BaseAncHomeVisitContract;
import org.smartregister.chw.anc.util.Constants;

/**
 * @author rkodev
 */
public class AncHomeVisitActivityTest extends BaseUnitTest {

    private AncHomeVisitActivity activity;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private BaseAncHomeVisitContract.Presenter presenter;

    private ActivityController<AncHomeVisitActivity> controller;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        controller = Robolectric.buildActivity(AncHomeVisitActivity.class);
        activity = controller.get();

        activity = Mockito.spy(activity);
        // mute this presenter
        Mockito.doNothing().when(activity).registerPresenter();
        ReflectionHelpers.setField(activity, "presenter", presenter);
    }

    @Test
    public void testStartMeLoadsActivity() {
        Mockito.doNothing().when(activity).startActivityForResult(Mockito.any(Intent.class), Mockito.anyInt());

        String baseEntityID = "baseEntityID";
        AncHomeVisitActivity.startMe(activity, baseEntityID, true);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);

        Mockito.verify(activity).startActivityForResult(intentArgumentCaptor.capture(), integerArgumentCaptor.capture());


        Assert.assertEquals(intentArgumentCaptor.getValue().getStringExtra(Constants.ANC_MEMBER_OBJECTS.BASE_ENTITY_ID), baseEntityID);
        Assert.assertTrue(intentArgumentCaptor.getValue().getBooleanExtra(Constants.ANC_MEMBER_OBJECTS.EDIT_MODE, false));
        Assert.assertEquals(AncHomeVisitActivity.class.getName(), intentArgumentCaptor.getValue().getComponent().getClassName());
        Assert.assertEquals((int) integerArgumentCaptor.getValue(), Constants.REQUEST_CODE_HOME_VISIT);
    }

    @Test
    public void testStartFormActivity() {
        JSONObject json = new JSONObject();
        activity.startFormActivity(json);
        Mockito.verify(activity).startActivityForResult(Mockito.any(Intent.class), Mockito.anyInt());
    }

    @Test
    public void testSubmittedAndCloseCloseActivity() {
        activity.submittedAndClose();
        Mockito.verify(activity).close();
    }

    @After
    public void tearDown() {
        try {
            activity.finish();
            controller.pause().stop().destroy(); //destroy controller if we can
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
