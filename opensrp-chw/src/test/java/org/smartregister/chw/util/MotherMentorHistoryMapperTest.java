package org.smartregister.chw.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.util.MotherMentorHistoryMapper.Row;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorHistoryMapperTest {
    @Test
    @Config(qualifiers = "en")
    public void serviceQuestionsFollowActionAndFormOrderIncludingNewAttendanceFields() {
        Visit visit = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        answer(visit, "comments", "Review next month, with partner", "");
        answer(visit, "next_appointment_date", "23-11-2026", "");
        answer(visit, "referral_other", "Other referral, full text", "");
        answer(visit, "type_of_referral_given", "[legal_support, nutrition]", "");
        answer(visit, "referral_given", "yes", "Ndiyo");
        answer(visit, "education_other", "Other topic, full text", "");
        answer(visit, "topics_being_provided", "[nutrition, medication_adherence]", "");
        answer(visit, "way_education_provided", "personal", "Binafsi");
        answer(visit, "education_provided", "yes", "Ndiyo");
        answer(visit, "has_been_linked_to_psychosocial_support_group", "yes", "Ndiyo");
        answer(visit, "new_pmtct_art_initiation", "yes", "Ndiyo");
        answer(visit, "visit_pregnancy_status", "pregnant", "Mjamzito");
        answer(visit, "attendance_type", "new", "Mpya");

        List<Row> rows = mapper().map(visit);

        assertEquals(
                Arrays.asList(
                        "attendance_type", "visit_pregnancy_status", "new_pmtct_art_initiation",
                        "has_been_linked_to_psychosocial_support_group", "education_provided",
                        "way_education_provided", "topics_being_provided", "education_other",
                        "referral_given", "type_of_referral_given", "referral_other",
                        "next_appointment_date", "comments"),
                keys(rows));
        assertEquals("Attendance type", row(rows, "attendance_type").getLabel());
        assertEquals(Collections.singletonList("Individual"), row(rows, "way_education_provided").getValues());
        assertEquals(Arrays.asList("Nutrition", "Medication adherence"), row(rows, "topics_being_provided").getValues());
        assertEquals(Collections.singletonList("Other topic, full text"), row(rows, "education_other").getValues());
        assertEquals(Collections.singletonList("23-11-2026"), row(rows, "next_appointment_date").getValues());
        assertEquals(Collections.singletonList("Review next month, with partner"), row(rows, "comments").getValues());
        assertFalse(row(rows, "comments").isMultiValue());
    }

    @Test
    @Config(qualifiers = "sw")
    public void rawOptionCodesTranslateToSwahiliWithoutTranslatingFreeText() {
        Visit visit = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        answer(visit, "attendance_type", "new", "Repeat");
        answer(visit, "follow_up_status", "continuing_services", "Continuing services");
        answer(visit, "visit_pregnancy_status", "breast_feeding", "Breastfeeding");
        answer(visit, "way_education_provided", "personal", "Individual");
        answer(visit, "topics_being_provided", "[\"family_planning\",\"tb\"]", "");
        answer(visit, "comments", "yes, keep the original comment", "");

        List<Row> rows = mapper().map(visit);

        assertEquals("Aina ya hudhurio", row(rows, "attendance_type").getLabel());
        assertEquals(Collections.singletonList("Mpya"), row(rows, "attendance_type").getValues());
        assertEquals(Collections.singletonList("Anaendelea na huduma"), row(rows, "follow_up_status").getValues());
        assertEquals(Collections.singletonList("Ananyonyesha"), row(rows, "visit_pregnancy_status").getValues());
        assertEquals(Collections.singletonList("Binafsi"), row(rows, "way_education_provided").getValues());
        assertEquals(Arrays.asList("Uzazi wa mpango", "Kifua kikuu"), row(rows, "topics_being_provided").getValues());
        assertEquals(Collections.singletonList("yes, keep the original comment"), row(rows, "comments").getValues());
    }

    @Test
    @Config(qualifiers = "en")
    public void legacyLocalizedDisplayAnswersResolveThroughTheFieldsOwnOptions() {
        Visit visit = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        answer(visit, "attendance_type", "", "Mpya");
        answer(visit, "education_provided", "", "Ndiyo");
        answer(visit, "way_education_provided", "", "Kikundi");
        answer(visit, "topics_being_provided", "", "Uzazi wa mpango, Lishe");

        List<Row> rows = mapper().map(visit);

        assertEquals(Collections.singletonList("New"), row(rows, "attendance_type").getValues());
        assertEquals(Collections.singletonList("Yes"), row(rows, "education_provided").getValues());
        assertEquals(Collections.singletonList("Group"), row(rows, "way_education_provided").getValues());
        assertEquals(Arrays.asList("Family planning", "Nutrition"), row(rows, "topics_being_provided").getValues());
    }

    @Test
    @Config(qualifiers = "sw")
    public void historicPersonalLabelUsesTheCurrentFieldsSwahiliOption() {
        Visit visit = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        answer(visit, "way_education_provided", "", "Personal");

        assertEquals(Collections.singletonList("Binafsi"), row(mapper().map(visit), "way_education_provided").getValues());
    }

    @Test
    @Config(qualifiers = "en")
    public void historicSwahiliLostStatusCanBeReadInEnglish() {
        Visit visit = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        answer(visit, "follow_up_status", "", "Amepotea");

        assertEquals(
                "lost to follow-up",
                row(mapper().map(visit), "follow_up_status").getValues().get(0).toLowerCase(Locale.ROOT));
    }

    @Test
    @Config(qualifiers = "en")
    public void householdHistoryUsesItsFormOrderAndCoalescesStepPrefixedAliases() {
        Visit visit = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        answer(visit, "step1:comments", "Old comment", "");
        answer(visit, "comments", "Current comment, kept intact", "");
        answer(visit, "step1:purpose_of_visit", "new_registration", "");
        answer(visit, "purpose_of_visit", "routine_follow_up", "");
        answer(visit, "household_members_found", "3", "");
        answer(visit, "counselled_male", "1", "");
        answer(visit, "step1:participants", "[mother, partner]", "");
        answer(visit, "topics_taught", "[nutrition]", "");
        answer(visit, "household_referral_given", "no", "");

        List<Row> rows = mapper().map(visit);

        assertEquals(
                Arrays.asList(
                        "purpose_of_visit", "household_members_found", "counselled_male",
                        "participants", "topics_taught", "household_referral_given", "comments"),
                keys(rows));
        assertEquals(Collections.singletonList("Routine follow-up"), row(rows, "purpose_of_visit").getValues());
        assertEquals(Arrays.asList("Mother", "Partner"), row(rows, "participants").getValues());
        assertEquals(Collections.singletonList("3"), row(rows, "household_members_found").getValues());
        assertEquals(Collections.singletonList("Current comment, kept intact"), row(rows, "comments").getValues());
    }

    @Test
    public void checkboxesAcceptBracketedGroupsButScalarCommentsKeepCommas() {
        Visit visit = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        answer(visit, "topics_being_provided", "[nutrition],[],[family_planning]", "");
        answer(visit, "comments", "Nutrition, family planning; no extra visit", "");

        List<Row> rows = mapper().map(visit);

        assertEquals(Arrays.asList("Nutrition", "Family planning"), row(rows, "topics_being_provided").getValues());
        assertTrue(row(rows, "topics_being_provided").isMultiValue());
        assertEquals(Collections.singletonList("Nutrition, family planning; no extra visit"), row(rows, "comments").getValues());
    }

    @Test
    public void literalJsonLookingCommentsAreNotDecodedAsAnswerArrays() {
        Visit visit = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        String comment = "[\"Follow up, travel\"]";
        answer(visit, "comments", comment, "");

        assertEquals(Collections.singletonList(comment), row(mapper().map(visit), "comments").getValues());
    }

    @Test
    public void partialHouseholdDraftWithOnlyZeroFoundMembersStillUsesHouseholdWorkflow() {
        Visit visit = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        answer(visit, "step2:household_members_found", "0", "");

        assertTrue(MotherMentorHistoryMapper.isHouseholdVisit(visit));
        assertEquals(Collections.singletonList("household_members_found"), keys(mapper().map(visit)));
        assertEquals(Collections.singletonList("0"), row(mapper().map(visit), "household_members_found").getValues());
    }

    @Test
    public void removedOptionKeepsItsSavedLabelIncludingCommas() {
        Visit visit = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        answer(visit, "topics_being_provided", "[\"retired_option\"]", "[\"Archived topic, full phrase\"]");

        assertEquals(
                Collections.singletonList("Archived topic, full phrase"),
                row(mapper().map(visit), "topics_being_provided").getValues());
    }

    @Test
    public void partnerHistoryKeepsRecordedLegacyReasonBetweenStatusAndArrival() {
        Visit visit = visit(MotherMentorFollowUpType.PARTNER.getEventType());
        answer(visit, "comments", "Reached, will attend", "");
        answer(visit, "partner_arrived_at_facility", "yes", "");
        answer(visit, "partner_non_attendance_reason", "transport_cost", "");
        answer(visit, "follow_up_status", "found", "");

        assertEquals(
                Arrays.asList("follow_up_status", "partner_non_attendance_reason", "partner_arrived_at_facility", "comments"),
                keys(mapper().map(visit)));
    }

    @Test
    @Config(qualifiers = "sw")
    public void childEidHistoryFollowsStatusTestArrivalAndCommentOrder() {
        Visit visit = visit(MotherMentorFollowUpType.CHILD_EID.getEventType());
        answer(visit, "comments", "At the facility", "");
        answer(visit, "child_eid_arrived_at_facility", "yes", "");
        answer(visit, "child_eid_test_type", "nine_months", "9 months");
        answer(visit, "follow_up_status", "found", "Found");

        List<Row> rows = mapper().map(visit);

        assertEquals(Arrays.asList("follow_up_status", "child_eid_test_type", "child_eid_arrived_at_facility", "comments"), keys(rows));
        assertEquals(Collections.singletonList("Miezi 9"), row(rows, "child_eid_test_type").getValues());
        assertEquals(Collections.singletonList("Ndiyo"), row(rows, "child_eid_arrived_at_facility").getValues());
    }

    @Test
    public void iitHistoryFollowsStatusReasonReturnAndComments() {
        Visit visit = visit(MotherMentorFollowUpType.MOTHER_IIT.getEventType());
        answer(visit, "comments", "Review next month", "");
        answer(visit, "returned_to_facility", "yes", "");
        answer(visit, "service_interruption_reason", "forgot", "");
        answer(visit, "follow_up_status", "found", "");

        assertEquals(
                Arrays.asList("follow_up_status", "service_interruption_reason", "returned_to_facility", "comments"),
                keys(mapper().map(visit)));
    }

    @Test
    @Config(qualifiers = "en")
    public void oldGenericServiceEventsUseTheirRecordedFollowUpWorkflow() {
        Visit partner = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        answer(partner, "follow_up_status", "found", "");
        answer(partner, "partner_arrived_at_facility", "yes", "");
        Visit child = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        answer(child, "follow_up_status", "found", "");
        answer(child, "child_eid_test_type", "nine_months", "");
        Visit mother = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        answer(mother, "follow_up_status", "found", "");
        answer(mother, "returned_to_facility", "yes", "");

        assertEquals(Collections.singletonList("Yes"), row(mapper().map(partner), "partner_arrived_at_facility").getValues());
        assertEquals(Collections.singletonList("9 months"), row(mapper().map(child), "child_eid_test_type").getValues());
        assertEquals(Collections.singletonList("Yes"), row(mapper().map(mother), "returned_to_facility").getValues());

        // Household evidence takes priority even when a legacy generic encounter has extra data.
        answer(partner, "household_members_found", "0", "");
        assertTrue(MotherMentorHistoryMapper.isHouseholdVisit(partner));
        assertEquals("household_members_found", mapper().map(partner).get(0).getKey());
    }

    @Test
    public void namedChildFollowUpKeepsItsWorkflowWhenOldPartnerAnswersRemain() {
        Visit visit = visit(MotherMentorFollowUpType.CHILD_EID.getEventType());
        answer(visit, "follow_up_status", "found", "");
        answer(visit, "child_eid_test_type", "nine_months", "");
        answer(visit, "partner_arrived_at_facility", "yes", "");
        answer(visit, "comments", "Attend for the child's test", "");

        List<Row> rows = mapper().map(visit);

        assertEquals("child_eid_test_type", rows.get(1).getKey());
        assertEquals(Collections.singletonList("9 months"), row(rows, "child_eid_test_type").getValues());
    }

    @Test
    public void hiddenFlagsAndInternalFieldsStayHiddenWhileLabelledHistoricalAnswersRemain() {
        Visit visit = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        answer(visit, "mothermentor_reporting_version", "2", "");
        answer(visit, "primary_mothermentor_client", "true", "");
        answer(visit, "art_initiated_before_visit", "true", "");
        answer(visit, "entity_id", "internal-id", "");
        answer(visit, "unknown_internal_flag", "private", "");
        answer(visit, "visit_number", "123", "");
        answer(visit, "service_interruption_reason_other", "Old reason, original text", "");
        answer(visit, "comments", "Yes", "");

        List<Row> rows = mapper().map(visit);

        assertEquals(Arrays.asList("service_interruption_reason_other", "comments"), keys(rows));
        assertEquals(Collections.singletonList("Old reason, original text"), row(rows, "service_interruption_reason_other").getValues());
        assertEquals(Collections.singletonList("Yes"), row(rows, "comments").getValues());
    }

    private MotherMentorHistoryMapper mapper() {
        return new MotherMentorHistoryMapper(RuntimeEnvironment.application);
    }

    private Visit visit(String type) {
        Visit visit = new Visit();
        visit.setVisitType(type);
        visit.setVisitDetails(new LinkedHashMap<>());
        return visit;
    }

    private void answer(Visit visit, String key, String raw, String readable) {
        VisitDetail detail = new VisitDetail();
        detail.setVisitKey(key);
        detail.setDetails(raw);
        detail.setHumanReadable(readable);
        visit.getVisitDetails().put(key, Collections.singletonList(detail));
    }

    private Row row(List<Row> rows, String key) {
        for (Row row : rows) if (key.equals(row.getKey())) return row;
        throw new AssertionError("Missing history row: " + key);
    }

    private List<String> keys(List<Row> rows) {
        List<String> keys = new ArrayList<>();
        for (Row row : rows) keys.add(row.getKey());
        return keys;
    }
}
