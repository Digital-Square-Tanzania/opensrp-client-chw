package org.smartregister.chw.fragment;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.Application;
import android.content.Intent;

import androidx.fragment.app.FragmentActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.activity.MotherMentorProfileActivity;
import org.smartregister.chw.ayp.dao.AypDao;
import org.smartregister.chw.dao.FamilyDao;
import org.smartregister.chw.model.FamilyDetailsModel;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorClientRoutingTest {
    @Test
    public void partnerTagOpensPartnerFollowUpProfile() {
        assertRoute("MOTHER MENTOR PARTNER", MotherMentorFollowUpType.PARTNER);
    }

    @Test
    public void childEidTagOpensChildEidFollowUpProfile() {
        assertRoute("MOTHER MENTOR CHILD EID", MotherMentorFollowUpType.CHILD_EID);
    }

    @Test
    public void motherIitTagOpensMotherIitFollowUpProfile() {
        assertRoute("Mother Mentor IIT", MotherMentorFollowUpType.MOTHER_IIT);
    }

    @Test
    public void primaryMotherMentorTagKeepsPrimaryProfile() {
        assertRoute("Mother Mentor", null);
    }

    private void assertRoute(String tag, MotherMentorFollowUpType expectedType) {
        for (boolean hasFamily : new boolean[] {false, true}) {
            FragmentActivity activity = mock(FragmentActivity.class);
            when(activity.getPackageName()).thenReturn("org.smartregister.chw.moh.testing");
            AllClientsRegisterFragment fragment =
                    mock(AllClientsRegisterFragment.class, CALLS_REAL_METHODS);
            doReturn(activity).when(fragment).getActivity();
            CommonPersonObjectClient client = mock(CommonPersonObjectClient.class);
            when(client.getCaseId()).thenReturn("client-id");
            when(client.entityId()).thenReturn("client-id");
            Map<String, String> details = new HashMap<>();
            details.put(AllClientsRegisterFragment.REGISTER_TYPE, tag);
            when(client.getDetails()).thenReturn(details);
            try (MockedStatic<AypDao> ayp = mockStatic(AypDao.class);
                    MockedStatic<FamilyDao> family = mockStatic(FamilyDao.class)) {
                family.when(() -> FamilyDao.getFamilyDetail("client-id"))
                        .thenReturn(hasFamily ? mock(FamilyDetailsModel.class) : null);
                fragment.goToClientDetailActivity(client);
                ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
                verify(activity).startActivity(intentCaptor.capture());
                Intent intent = intentCaptor.getValue();
                assertEquals(
                        MotherMentorProfileActivity.class.getName(),
                        intent.getComponent().getClassName());
                assertEquals(
                        "client-id",
                        intent.getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID));
                assertEquals(
                        expectedType != null,
                        intent.getBooleanExtra("extra_follow_up_profile", false));
                assertEquals(
                        expectedType == null ? null : expectedType.getValue(),
                        intent.getStringExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE));
            }
        }
    }
}
