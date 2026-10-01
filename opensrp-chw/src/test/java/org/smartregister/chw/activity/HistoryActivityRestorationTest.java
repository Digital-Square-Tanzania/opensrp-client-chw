package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.widget.TextView;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.CoreLibrary;
import org.smartregister.util.Session;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.interactor.AypOutSchoolMedicalHistoryInteractor;
import org.smartregister.chw.interactor.TbLeprosyObservationResultsInteractor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

@Config(sdk = 28)
public class HistoryActivityRestorationTest extends BaseUnitTest {
    @Before
    public void startTestSession() {
        Session session = ReflectionHelpers.getField(CoreLibrary.getInstance().context().userService(), "session");
        session.start(session.lengthInMilliseconds()).setPassword(java.util.UUID.randomUUID().toString());
    }

    @After
    public void endTestSession() {
        CoreLibrary.getInstance().context().userService().logoutSession();
    }

    @Test
    public void aypHistoryWithoutRestorableMemberClosesSafely() {
        ActivityController<AypOutSchoolMedicalHistoryActivity> controller =
                Robolectric.buildActivity(AypOutSchoolMedicalHistoryActivity.class).create();
        assertTrue(controller.get().isFinishing());
        assertNull(controller.get().getPresenter());
        controller.destroy();
    }

    @Test
    public void tbHistoryWithoutRestorableMemberClosesSafely() {
        ActivityController<TbLeprosyObservationResultsActivity> controller =
                Robolectric.buildActivity(TbLeprosyObservationResultsActivity.class).create();
        assertTrue(controller.get().isFinishing());
        assertNull(controller.get().getPresenter());
        controller.destroy();
    }

    @Test
    public void aypLaunchRetainsItsOwnMemberAfterAnotherLaunchAndParcelRoundTrip() {
        Activity source = Robolectric.buildActivity(Activity.class).get();
        org.smartregister.chw.ayp.domain.MemberObject first = new org.smartregister.chw.ayp.domain.MemberObject();
        first.setBaseEntityId("first-client");
        first.setFirstName("Ada");
        first.setLastName("Alpha");
        AypOutSchoolMedicalHistoryActivity.startMe(source, first);
        Intent intent = parcelRoundTrip(Shadows.shadowOf(source).getNextStartedActivity());
        first.setBaseEntityId("second-client");
        first.setFirstName("Different");
        AypOutSchoolMedicalHistoryActivity.startMe(source, first);
        try (MockedConstruction<AypOutSchoolMedicalHistoryInteractor> construction =
                     mockConstruction(AypOutSchoolMedicalHistoryInteractor.class)) {
            ActivityController<AypOutSchoolMedicalHistoryActivity> controller =
                    Robolectric.buildActivity(AypOutSchoolMedicalHistoryActivity.class, intent).create();
            assertNotNull(controller.get().getPresenter());
            assertEquals(1, construction.constructed().size());
            verify(construction.constructed().get(0)).getMemberHistory(eq("first-client"), any(), any());
            assertTrue(((TextView) controller.get().findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle))
                    .getText().toString().contains("Ada Alpha"));
            controller.destroy();
        }
    }

    @Test
    public void tbLaunchRetainsItsOwnMemberAfterAnotherLaunchAndParcelRoundTrip() {
        Activity source = Robolectric.buildActivity(Activity.class).get();
        org.smartregister.chw.tbleprosy.domain.MemberObject first = new org.smartregister.chw.tbleprosy.domain.MemberObject();
        first.setBaseEntityId("first-client");
        first.setFirstName("Ada");
        first.setLastName("Alpha");
        TbLeprosyObservationResultsActivity.startMe(source, first);
        Intent intent = parcelRoundTrip(Shadows.shadowOf(source).getNextStartedActivity());
        first.setBaseEntityId("second-client");
        first.setFirstName("Different");
        TbLeprosyObservationResultsActivity.startMe(source, first);
        try (MockedConstruction<TbLeprosyObservationResultsInteractor> construction =
                     mockConstruction(TbLeprosyObservationResultsInteractor.class)) {
            ActivityController<TbLeprosyObservationResultsActivity> controller =
                    Robolectric.buildActivity(TbLeprosyObservationResultsActivity.class, intent).create();
            assertNotNull(controller.get().getPresenter());
            assertEquals(1, construction.constructed().size());
            verify(construction.constructed().get(0)).getMemberHistory(eq("first-client"), any(), any());
            assertTrue(((TextView) controller.get().findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle))
                    .getText().toString().contains("Ada Alpha"));
            controller.destroy();
        }
    }

    private Intent parcelRoundTrip(Intent intent) {
        Parcel parcel = Parcel.obtain();
        try {
            intent.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return Intent.CREATOR.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }

}
