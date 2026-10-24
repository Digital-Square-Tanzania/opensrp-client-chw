package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Intent;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.util.MotherMentorTestAssets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exercises the actual history views and edit intents using the packaged library forms. */
@Config(sdk = 28)
public class MotherMentorMedicalHistoryRenderTest extends BaseUnitTest {
    private static final String COMMENTS = "Counselled mother, partner and child, next visit agreed.";
    private final List<ActivityController<Activity>> controllers = new ArrayList<>();

    @After
    public void destroyActivities() {
        for (ActivityController<Activity> controller : controllers) controller.destroy();
    }

    @Test
    @Config(qualifiers = "en")
    public void englishHistoryUsesCodedAnswersAndClinicalOrderWithCommentsLast() throws Exception {
        assertLocalizedServiceHistory("json.form", "json.form-sw");
    }

    @Test
    @Config(qualifiers = "sw")
    public void swahiliHistoryUsesCodedAnswersAndClinicalOrderWithCommentsLast() throws Exception {
        assertLocalizedServiceHistory("json.form-sw", "json.form");
    }

    @Test
    public void onlyLatestVisitCanBeEditedAndRepeatServicesStayInServicesWorkflow() {
        Activity activity = activity();
        Visit first = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        first.setDate(new Date(1_700_000_000_000L));
        first.setProcessed(false);
        Visit latest = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        add(latest, "attendance_type", "repeat", "Repeat");
        add(latest, "follow_up_status", "continuing_services", "Continuing services");
        LinearLayout cards = render(activity, null, first, latest);

        assertEquals(2, cards.getChildCount());
        assertEquals(View.VISIBLE, edit(cards.getChildAt(0)).getVisibility());
        assertEquals(View.GONE, edit(cards.getChildAt(1)).getVisibility());
        TextView saved = cards.getChildAt(1).findViewWithTag("visit_completion_state");
        assertTrue(saved.getText().toString()
                .contains(activity.getString(R.string.mothermentor_history_saved)));
        assertTrue(edit(cards.getChildAt(0)).performClick());
        Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
        assertVisitIntent(intent, MotherMentorVisitActivity.class);
        assertFalse(intent.getBooleanExtra(MotherMentorVisitActivity.EXTRA_FOLLOW_UP_VISIT, true));
        assertTrue(activity.isFinishing());
    }

    @Test
    public void explicitFollowUpEventDeterminesEditedWorkflow() {
        for (MotherMentorFollowUpType type : MotherMentorFollowUpType.values()) {
            Activity activity = activity();
            Visit visit = visit(type.getEventType());
            add(visit, "follow_up_status", "found", "Found");
            LinearLayout cards = render(activity, MotherMentorFollowUpType.MOTHER_IIT, visit);

            assertEquals(View.VISIBLE, edit(cards.getChildAt(0)).getVisibility());
            assertTrue(edit(cards.getChildAt(0)).performClick());
            Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
            assertVisitIntent(intent, MotherMentorVisitActivity.class);
            assertTrue(intent.getBooleanExtra(MotherMentorVisitActivity.EXTRA_FOLLOW_UP_VISIT, false));
            assertEquals(type.getValue(),
                    intent.getStringExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE));
        }
    }

    @Test
    public void householdAnswersAndContactEventsEditHouseholdServices() {
        for (String event : Arrays.asList(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES,
                Constants.EVENT_TYPE.MOTHERMENTOR_CONTACT_VISIT)) {
            Activity activity = activity();
            Visit visit = visit(event);
            if (Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES.equals(event)) {
                add(visit, "step1:purpose_of_visit", "routine_follow_up", "Routine follow-up");
            }
            LinearLayout cards = render(activity, null, visit);

            assertTrue(edit(cards.getChildAt(0)).performClick());
            assertVisitIntent(Shadows.shadowOf(activity).getNextStartedActivity(),
                    MotherMentorHouseholdVisitActivity.class);
        }
    }

    @Test
    public void partialHouseholdDraftWithZeroMembersStillEditsHouseholdServices() {
        Activity activity = activity();
        Visit visit = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        visit.setProcessed(false);
        add(visit, "step2:household_members_found", "0", "0");
        LinearLayout cards = render(activity, null, visit);

        TextView members = cards.getChildAt(0).findViewWithTag("household_members_found");
        assertNotNull(members);
        assertTrue(members.getText().toString().trim().endsWith("\n0"));
        assertEquals(View.VISIBLE, edit(cards.getChildAt(0)).getVisibility());
        assertTrue(edit(cards.getChildAt(0)).performClick());
        assertVisitIntent(Shadows.shadowOf(activity).getNextStartedActivity(),
                MotherMentorHouseholdVisitActivity.class);
    }

    @Test
    public void legacyServicesInFollowUpHistoryRemainReadOnly() {
        Activity activity = activity();
        Visit legacy = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        add(legacy, "follow_up_status", "found", "Found");
        LinearLayout cards = render(activity, MotherMentorFollowUpType.MOTHER_IIT, legacy);

        assertEquals(View.GONE, edit(cards.getChildAt(0)).getVisibility());
    }

    private void assertLocalizedServiceHistory(String locale, String oppositeLocale) throws Exception {
        Activity activity = activity();
        Visit visit = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
        Map<String, String> expected = new LinkedHashMap<>();
        answer(visit, expected, locale, oppositeLocale, "mothermentor_visit_type",
                "attendance_type", "repeat");
        answer(visit, expected, locale, oppositeLocale, "mothermentor_visit_type",
                "follow_up_status", "continuing_services");
        answer(visit, expected, locale, oppositeLocale, "mothermentor_visit_group_linkage",
                "has_been_linked_to_psychosocial_support_group", "yes");
        answer(visit, expected, locale, oppositeLocale, "mothermentor_visit_group_linkage",
                "has_been_linked_to_iga_group", "already_in_group");
        answer(visit, expected, locale, oppositeLocale, "mothermentor_visit_education_provided",
                "education_provided", "yes");
        answer(visit, expected, locale, oppositeLocale, "mothermentor_visit_education_provided",
                "way_education_provided", "personal");
        answer(visit, expected, locale, oppositeLocale, "mothermentor_visit_education_provided",
                "topics_being_provided", "hiv_and_aids", "medication_adherence");
        answer(visit, expected, locale, oppositeLocale, "mothermentor_visit_referal",
                "referral_given", "yes");
        answer(visit, expected, locale, oppositeLocale, "mothermentor_visit_referal",
                "type_of_referral_given", "mental_health", "nutrition");
        add(visit, "next_appointment_date", "2026-11-20", "2026-11-20");
        expected.put("next_appointment_date",
                label(field(locale, "mothermentor_next_visit_date", "next_appointment_date"))
                        + "\n2026-11-20");
        add(visit, "comments", COMMENTS, COMMENTS);
        expected.put("comments", label(field(locale, "mothermentor_visit_comment", "comments"))
                + "\n" + COMMENTS);
        // Internal workflow flags must not leak into the rendered history.
        add(visit, "mothermentor_reporting_version", "2", "Internal version");
        expected.put("visit_number", activity.getString(R.string.mothermentor_history_visit_number)
                + "\n1");
        expected.put("visit_completion_state",
                activity.getString(R.string.mothermentor_history_visit_completion_state) + "\n"
                        + activity.getString(R.string.mothermentor_history_completed));

        LinearLayout cards = render(activity, null, visit);
        LinearLayout details = cards.getChildAt(0).findViewById(R.id.visit_details_layout);
        Map<String, String> actual = new LinkedHashMap<>();
        for (int i = 0; i < details.getChildCount(); i++) {
            TextView row = (TextView) details.getChildAt(i);
            if (row.getVisibility() == View.VISIBLE) {
                actual.put((String) row.getTag(), row.getText().toString().trim());
            }
        }
        assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.keySet()));
        assertEquals(expected, actual);
        TextView comment = details.findViewWithTag("comments");
        assertEquals(0, ((Spanned) comment.getText())
                .getSpans(0, comment.length(), BulletSpan.class).length);
        TextView topics = details.findViewWithTag("topics_being_provided");
        assertEquals(2, ((Spanned) topics.getText())
                .getSpans(0, topics.length(), BulletSpan.class).length);
    }

    private void answer(Visit visit, Map<String, String> expected, String locale,
            String oppositeLocale, String form, String key, String... codes) throws Exception {
        JSONObject localField = field(locale, form, key);
        JSONObject oppositeField = field(oppositeLocale, form, key);
        List<String> values = new ArrayList<>();
        for (String code : codes) {
            add(visit, key, code, option(oppositeField, code));
            values.add(option(localField, code));
        }
        expected.put(key, label(localField) + "\n" + String.join("\n", values));
    }

    private JSONObject field(String locale, String form, String key) throws Exception {
        JSONArray fields = MotherMentorTestAssets.form(locale, form + ".json")
                .getJSONObject("step1").getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            if (key.equals(fields.getJSONObject(i).optString("key"))) return fields.getJSONObject(i);
        }
        throw new AssertionError("Missing packaged form field: " + form + ":" + key);
    }

    private String option(JSONObject field, String code) throws Exception {
        JSONArray options = field.getJSONArray("options");
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            if (code.equals(option.getString("key"))) return option.getString("text");
        }
        throw new AssertionError("Missing packaged form option: " + code);
    }

    private String label(JSONObject field) {
        return field.optString("label", field.optString("hint"));
    }

    private Visit visit(String event) {
        Visit visit = new Visit();
        visit.setVisitType(event);
        visit.setBaseEntityId("mother-mentor-client");
        visit.setDate(new Date(1_800_000_000_000L));
        visit.setProcessed(true);
        visit.setVisitDetails(new LinkedHashMap<>());
        return visit;
    }

    private void add(Visit visit, String key, String coded, String humanReadable) {
        VisitDetail detail = new VisitDetail();
        detail.setVisitKey(key);
        detail.setDetails(coded);
        detail.setHumanReadable(humanReadable);
        visit.getVisitDetails().computeIfAbsent(key, ignored -> new ArrayList<>()).add(detail);
    }

    private Activity activity() {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class);
        controller.get().setTheme(org.smartregister.family.R.style.FamilyTheme_NoActionBar);
        controllers.add(controller);
        return controller.create().get();
    }

    private LinearLayout render(Activity activity, MotherMentorFollowUpType type, Visit... visits) {
        MotherMentorMedicalHistoryActivityFlv flavor = new MotherMentorMedicalHistoryActivityFlv(type);
        View view = flavor.bindViews(activity);
        flavor.processViewData(Arrays.asList(visits), activity);
        return view.findViewById(org.smartregister.chw.core.R.id.linearLayoutHealthFacilityVisitDetails);
    }

    private View edit(View card) {
        return card.findViewById(R.id.textview_edit);
    }

    private void assertVisitIntent(Intent intent, Class<? extends Activity> destination) {
        assertNotNull(intent);
        assertEquals(destination.getName(), intent.getComponent().getClassName());
        assertEquals("mother-mentor-client",
                intent.getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID));
        assertTrue(intent.getBooleanExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, false));
    }
}
