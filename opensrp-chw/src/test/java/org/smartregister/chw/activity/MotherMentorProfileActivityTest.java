package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;

/** Guards against routing the localized follow-up button by its displayed text. */
public class MotherMentorProfileActivityTest extends BaseUnitTest {

    @Test
    public void partnerVisitCanCompleteWithoutRemovedReasonSection() throws Exception {
        assertTrue(
                completedFollowUp(
                        MotherMentorFollowUpType.PARTNER,
                        DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY,
                        "no"));
        assertFalse(
                completedFollowUp(
                        MotherMentorFollowUpType.PARTNER,
                        DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY,
                        "yes"));
        assertTrue(
                completedFollowUp(
                        MotherMentorFollowUpType.PARTNER,
                        DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY,
                        "yes",
                        DBConstants.KEY.PARTNER_FACILITY_ARRIVAL_DATE,
                        "19-10-2026"));
    }

    @Test
    public void childEidVisitNeedsTestStageAndAttendanceButNoReasonSection() throws Exception {
        assertFalse(
                completedFollowUp(
                        MotherMentorFollowUpType.CHILD_EID,
                        DBConstants.KEY.CHILD_EID_ARRIVED_AT_FACILITY,
                        "no"));
        assertTrue(
                completedFollowUp(
                        MotherMentorFollowUpType.CHILD_EID,
                        DBConstants.KEY.CHILD_EID_TEST_TYPE,
                        "six_weeks",
                        DBConstants.KEY.CHILD_EID_ARRIVED_AT_FACILITY,
                        "no"));
        assertFalse(
                completedFollowUp(
                        MotherMentorFollowUpType.CHILD_EID,
                        DBConstants.KEY.CHILD_EID_TEST_TYPE,
                        "six_weeks",
                        DBConstants.KEY.CHILD_EID_ARRIVED_AT_FACILITY,
                        "yes"));
        assertTrue(
                completedFollowUp(
                        MotherMentorFollowUpType.CHILD_EID,
                        DBConstants.KEY.CHILD_EID_TEST_TYPE,
                        "six_weeks",
                        DBConstants.KEY.CHILD_EID_ARRIVED_AT_FACILITY,
                        "yes",
                        DBConstants.KEY.CHILD_EID_FACILITY_ARRIVAL_DATE,
                        "19-10-2026"));
    }

    @Test
    public void motherIitVisitStillRequiresInterruptionReasonAndExpectedDate() throws Exception {
        assertFalse(
                completedFollowUp(
                        MotherMentorFollowUpType.MOTHER_IIT,
                        DBConstants.KEY.RETURNED_TO_FACILITY,
                        "no"));
        assertTrue(
                completedFollowUp(
                        MotherMentorFollowUpType.MOTHER_IIT,
                        DBConstants.KEY.SERVICE_INTERRUPTION_REASON,
                        "transport_cost",
                        DBConstants.KEY.EXPECTED_FACILITY_VISIT_DATE,
                        "19-10-2026",
                        DBConstants.KEY.RETURNED_TO_FACILITY,
                        "no"));
    }

    private boolean completedFollowUp(MotherMentorFollowUpType type, String... answers)
            throws Exception {
        Intent intent =
                new Intent()
                        .putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true)
                        .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE, type.getValue());
        MotherMentorProfileActivity activity =
                Robolectric.buildActivity(MotherMentorProfileActivity.class, intent).get();
        JSONArray observations =
                new JSONArray()
                        .put(
                                new JSONObject()
                                        .put("fieldCode", DBConstants.KEY.FOLLOW_UP_STATUS)
                                        .put("values", new JSONArray().put("found")));
        for (int i = 0; i < answers.length; i += 2) {
            observations.put(
                    new JSONObject()
                            .put("fieldCode", answers[i])
                            .put("values", new JSONArray().put(answers[i + 1])));
        }
        Visit visit = new Visit();
        visit.setVisitType(type.getEventType());
        visit.setJson(new JSONObject().put("obs", observations).toString());
        return ReflectionHelpers.callInstanceMethod(
                activity,
                "hasCompletedMotherMentorServiceSections",
                ReflectionHelpers.ClassParameter.from(Visit.class, visit));
    }

    @Test
    public void contactButtonIsHiddenOnlyForFollowUpNavigationContext() {
        MotherMentorProfileActivity homeProfile =
                Robolectric.buildActivity(MotherMentorProfileActivity.class, new Intent()).get();
        assertFalse(homeProfile.isFollowUpProfile());

        Intent followUpIntent =
                new Intent().putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true);
        MotherMentorProfileActivity followUpProfile =
                Robolectric.buildActivity(MotherMentorProfileActivity.class, followUpIntent).get();
        assertTrue(followUpProfile.isFollowUpProfile());
    }

    @Test
    public void recordFollowUpClickLaunchesClientVisitRegardlessOfButtonLabel() {
        MotherMentorProfileActivity activity =
                Mockito.spy(Robolectric.buildActivity(MotherMentorProfileActivity.class).get());
        MemberObject member = new MemberObject();
        member.setBaseEntityId("test-client-id");
        ReflectionHelpers.setField(activity, "memberObject", member);
        Mockito.doNothing().when(activity).openRecordClientVisit();

        View view = Mockito.mock(View.class);
        Mockito.when(view.getId())
                .thenReturn(org.smartregister.chw.mothermentor.R.id.textview_record_mothermentor);
        activity.onClick(view);

        Mockito.verify(activity).openRecordClientVisit();
    }

    @Test
    public void profileCategoryComesFromStableIntentExtra() {
        Intent partnerIntent =
                new Intent()
                        .putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true)
                        .putExtra(
                                MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                                MotherMentorFollowUpType.PARTNER.getValue());
        MotherMentorProfileActivity partnerProfile =
                Robolectric.buildActivity(MotherMentorProfileActivity.class, partnerIntent).get();
        assertEquals(MotherMentorFollowUpType.PARTNER, partnerProfile.getFollowUpType());

        Intent childIntent =
                new Intent()
                        .putExtra(MotherMentorProfileActivity.EXTRA_FOLLOW_UP_PROFILE, true)
                        .putExtra(
                                MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                                MotherMentorFollowUpType.CHILD_EID.getValue());
        MotherMentorProfileActivity childProfile =
                Robolectric.buildActivity(MotherMentorProfileActivity.class, childIntent).get();
        assertEquals(MotherMentorFollowUpType.CHILD_EID, childProfile.getFollowUpType());
    }
}
