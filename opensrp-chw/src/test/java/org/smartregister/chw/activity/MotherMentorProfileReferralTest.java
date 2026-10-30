package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.util.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Checks the real profile button-to-referral chooser route without requiring a phone number. */
public class MotherMentorProfileReferralTest extends BaseUnitTest {

    @Test
    public void noPhoneMotherCanOpenExistingPmtctReferral() {
        MotherMentorProfileActivity activity = profile(new Intent(), "mother-client");

        Intent referral = openReferralChooser(activity);

        assertReferralIntent(referral, "mother-client");
        ArrayList<ReferralTypeModel> types =
                referral.getParcelableArrayListExtra(Constants.REFERRAL_TYPES);
        assertNotNull(types);
        assertEquals(Arrays.asList(CoreConstants.TASKS_FOCUS.PMTCT), focuses(types));
        assertEquals(CoreConstants.JSON_FORM.getPmtctReferralForm(), types.get(0).getFormName());
    }

    @Test
    public void secondaryProfilesReferTheirOwnClientInsteadOfTheLinkedMother() {
        for (MotherMentorFollowUpType type : MotherMentorFollowUpType.values()) {
            String clientId = "follow-up-" + type.getValue();
            Intent profileIntent =
                    new Intent()
                            .putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true)
                            .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE, type.getValue());
            MotherMentorProfileActivity activity = profile(profileIntent, clientId);

            Intent referral = openReferralChooser(activity);
            assertReferralIntent(referral, clientId);
            ArrayList<ReferralTypeModel> types =
                    referral.getParcelableArrayListExtra(Constants.REFERRAL_TYPES);
            if (type == MotherMentorFollowUpType.CHILD_EID) {
                assertEquals(
                        Arrays.asList(
                                CoreConstants.TASKS_FOCUS.SICK_CHILD,
                                CoreConstants.TASKS_FOCUS.SUSPECTED_CHILD_GBV),
                        focuses(types));
                assertEquals(
                        CoreConstants.JSON_FORM.getChildUnifiedReferralForm(),
                        types.get(0).getFormName());
                assertEquals(
                        CoreConstants.JSON_FORM.getChildGbvReferralForm(),
                        types.get(1).getFormName());
            } else if (type == MotherMentorFollowUpType.PARTNER) {
                assertEquals(
                        Arrays.asList(
                                CoreConstants.TASKS_FOCUS.SICK_HIV,
                                CoreConstants.TASKS_FOCUS.CONVENTIONAL_HIV_TEST,
                                CoreConstants.TASKS_FOCUS.TBLEPROSY,
                                CoreConstants.TASKS_FOCUS.SUSPECTED_GBV),
                        focuses(types));
            } else {
                assertEquals(Arrays.asList(CoreConstants.TASKS_FOCUS.PMTCT), focuses(types));
            }
        }
    }

    @Test
    public void refreshedProfileUsesCurrentClientAndDoesNotDuplicateReferralChoices() {
        MotherMentorProfileActivity activity = profile(new Intent(), "original-client");
        activity.initializeFloatingMenu();
        ReflectionHelpers.setField(activity, "memberObject", member("refreshed-client"));

        Intent referral = openReferralChooser(activity);

        assertReferralIntent(referral, "refreshed-client");
        assertEquals(
                1,
                referral.<ReferralTypeModel>getParcelableArrayListExtra(Constants.REFERRAL_TYPES)
                        .size());
        assertEquals(1, activity.getReferralTypeModels().size());
    }

    @Test
    public void missingMemberDoesNotCreateAnUnboundReferralButton() {
        MotherMentorProfileActivity activity = profile(new Intent(), null);

        activity.initializeFloatingMenu();

        assertNull(activity.findViewById(org.smartregister.chw.mothermentor.R.id.mothermentor_fab));
        assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void disabledReferralFlavorDoesNotExposeAnEmptyChooser() {
        MotherMentorProfileActivity activity = profile(new Intent(), "mother-client");
        ChwApplication.Flavor disabledFlavor = Mockito.mock(ChwApplication.Flavor.class);

        try (MockedStatic<ChwApplication> application =
                Mockito.mockStatic(ChwApplication.class, Mockito.CALLS_REAL_METHODS)) {
            application.when(ChwApplication::getApplicationFlavor).thenReturn(disabledFlavor);

            activity.initializeFloatingMenu();
            activity.findViewById(org.smartregister.chw.mothermentor.R.id.mothermentor_fab)
                    .performClick();

            assertTrue(activity.getReferralTypeModels().isEmpty());
            assertEquals(
                    View.GONE,
                    activity.findViewById(
                                    org.smartregister.chw.mothermentor.R.id.refer_to_facility_layout)
                            .getVisibility());
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        }
    }

    private MotherMentorProfileActivity profile(Intent intent, String clientId) {
        MotherMentorProfileActivity activity =
                Robolectric.buildActivity(MotherMentorProfileActivity.class, intent).get();
        activity.setTheme(R.style.ChwTheme);
        activity.setContentView(new FrameLayout(activity));
        ReflectionHelpers.setField(
                activity, "memberObject", clientId == null ? null : member(clientId));
        return activity;
    }

    private MemberObject member(String clientId) {
        MemberObject member = new MemberObject();
        member.setBaseEntityId(clientId);
        member.setFamilyBaseEntityId("household-id");
        member.setPhoneNumber(null);
        return member;
    }

    private Intent openReferralChooser(MotherMentorProfileActivity activity) {
        assertTrue("NACP referral route must be enabled", BuildConfig.USE_UNIFIED_REFERRAL_APPROACH);
        activity.initializeFloatingMenu();
        View pencil =
                activity.findViewById(org.smartregister.chw.mothermentor.R.id.mothermentor_fab);
        assertNotNull(pencil);
        pencil.performClick();
        View refer =
                activity.findViewById(
                        org.smartregister.chw.mothermentor.R.id.refer_to_facility_layout);
        assertNotNull(refer);
        assertEquals(View.VISIBLE, refer.getVisibility());
        assertEquals(
                View.VISIBLE,
                activity.findViewById(org.smartregister.chw.mothermentor.R.id.menu_bar)
                        .getVisibility());
        refer.performClick();
        return Shadows.shadowOf(activity).getNextStartedActivity();
    }

    private void assertReferralIntent(Intent intent, String clientId) {
        assertNotNull(intent);
        assertNotNull(intent.getComponent());
        assertEquals(ClientReferralActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals(clientId, intent.getStringExtra(Constants.ENTITY_ID));
    }

    private List<String> focuses(List<ReferralTypeModel> referrals) {
        List<String> focuses = new ArrayList<>();
        for (ReferralTypeModel referral : referrals) {
            focuses.add(referral.getFocus());
        }
        return focuses;
    }
}
