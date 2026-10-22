package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.SystemClock;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Real Android history rendering; fixtures never read or write clinical records or preferences. */
@RunWith(AndroidJUnit4.class)
public class MotherMentorHistoryRenderingInstrumentedTest {
    private static final String COMMENTS = "QA history, partner counselled, next visit agreed.";
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private Activity activity;
    private Configuration originalConfiguration;
    private ScrollView scroll;
    private LinearLayout cards;
    private String directory;
    private String oppositeDirectory;

    @Before
    public void launchFixtureActivity() throws Exception {
        Intent intent = new Intent(instrumentation.getTargetContext(), MotherMentorHistoryTestActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity = instrumentation.startActivitySync(intent);
        onMain(() -> {
            originalConfiguration = new Configuration(activity.getResources().getConfiguration());
            activity.setTheme(org.smartregister.family.R.style.FamilyTheme_NoActionBar);
            activity.setTitle("Mother Mentor history fixture — no client data");
        });
    }

    @After
    public void restoreConfigurationAndClose() throws Exception {
        if (activity == null) return;
        onMain(() -> {
            if (originalConfiguration != null) updateConfiguration(originalConfiguration);
            activity.finish();
        });
        instrumentation.waitForIdleSync();
    }

    @Test
    public void englishServiceHistoryUsesCodesAndKeepsCommentsLast() throws Exception {
        serviceHistory("en");
    }

    @Test
    public void swahiliServiceHistoryUsesCodesAndKeepsCommentsLast() throws Exception {
        serviceHistory("sw");
    }

    @Test
    public void householdHistoryShowsZeroAndLocalizedMultiselectAnswers() throws Exception {
        for (String language : Arrays.asList("en", "sw")) {
            onMain(() -> {
                locale(language);
                Visit draft = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
                draft.setProcessed(false);
                add(draft, "step2:household_members_found", "0", "0");
                Visit completed = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
                Map<String, String> expected = new LinkedHashMap<>();
                coded(completed, expected, "mothermentor_household_visit_purpose",
                        "purpose_of_visit", "routine_follow_up");
                scalar(completed, expected, "mother_mentor_household_members_who_received_counselling",
                        "household_members_found", "2");
                coded(completed, expected, "mother_mentor_household_members_who_received_counselling",
                        "participants", "mother", "partner");
                coded(completed, expected, "mother_mentor_household_visit_topics_taught",
                        "topics_taught", "medication_adherence", "infant_feeding");
                scalar(completed, expected, "mothermentor_household_visit_comment", "comments", COMMENTS);
                metadata(expected, 2, true);

                render(null, draft, completed);
                assertEquals(2, cards.getChildCount());
                assertRows(cards.getChildAt(0), expected);
                Map<String, String> zero = new LinkedHashMap<>();
                zero.put("household_members_found",
                        label(field("mother_mentor_household_members_who_received_counselling",
                                "household_members_found")) + "\n0");
                metadata(zero, 1, false);
                assertRows(cards.getChildAt(1), zero);
                assertBulletCount(cards.getChildAt(0), "participants", 2);
                assertBulletCount(cards.getChildAt(0), "comments", 0);
            });
            evidence(language + "-household");
        }
    }

    @Test
    public void explicitFollowUpHistoriesUseStatusClinicalFieldsThenCommentsInBothLanguages()
            throws Exception {
        for (String language : Arrays.asList("en", "sw")) {
            for (MotherMentorFollowUpType type : MotherMentorFollowUpType.values()) {
                onMain(() -> {
                    locale(language);
                    Visit visit = visit(type.getEventType());
                    Map<String, String> expected = new LinkedHashMap<>();
                    if (type == MotherMentorFollowUpType.MOTHER_IIT) {
                        coded(visit, expected, "mothermentor_follow_up_status", "follow_up_status", "found");
                        coded(visit, expected, "mothermentor_service_interruption_reason",
                                "service_interruption_reason", "forgot");
                        coded(visit, expected, "mothermentor_return_to_facility", "returned_to_facility", "yes");
                        scalar(visit, expected, "mothermentor_return_to_facility",
                                "facility_return_date", "2026-10-22");
                        scalar(visit, expected, "mothermentor_follow_up_comment", "comments", COMMENTS);
                    } else if (type == MotherMentorFollowUpType.PARTNER) {
                        coded(visit, expected, "mothermentor_partner_follow_up_status", "follow_up_status", "found");
                        coded(visit, expected, "mothermentor_partner_arrival", "partner_arrived_at_facility", "yes");
                        scalar(visit, expected, "mothermentor_partner_arrival",
                                "partner_facility_arrival_date", "2026-10-22");
                        scalar(visit, expected, "mothermentor_partner_follow_up_comment", "comments", COMMENTS);
                    } else {
                        coded(visit, expected, "mothermentor_child_eid_follow_up_status", "follow_up_status", "found");
                        coded(visit, expected, "mothermentor_child_eid_test_type",
                                "child_eid_test_type", "nine_months");
                        coded(visit, expected, "mothermentor_child_eid_arrival",
                                "child_eid_arrived_at_facility", "yes");
                        scalar(visit, expected, "mothermentor_child_eid_arrival",
                                "child_eid_facility_arrival_date", "2026-10-22");
                        scalar(visit, expected, "mothermentor_child_eid_follow_up_comment", "comments", COMMENTS);
                    }
                    metadata(expected, 1, true);
                    render(type, visit);
                    assertRows(cards.getChildAt(0), expected);
                    assertBulletCount(cards.getChildAt(0), "comments", 0);
                });
                evidence(language + "-" + type.getValue().toLowerCase(Locale.ROOT));
            }
        }
    }

    private void serviceHistory(String language) throws Exception {
        onMain(() -> {
            locale(language);
            Visit visit = visit(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
            Map<String, String> expected = new LinkedHashMap<>();
            coded(visit, expected, "mothermentor_visit_type", "attendance_type", "repeat");
            coded(visit, expected, "mothermentor_visit_type", "follow_up_status", "continuing_services");
            coded(visit, expected, "mothermentor_visit_group_linkage",
                    "has_been_linked_to_psychosocial_support_group", "yes");
            coded(visit, expected, "mothermentor_visit_group_linkage",
                    "has_been_linked_to_iga_group", "already_in_group");
            coded(visit, expected, "mothermentor_visit_education_provided", "education_provided", "yes");
            coded(visit, expected, "mothermentor_visit_education_provided", "way_education_provided", "personal");
            coded(visit, expected, "mothermentor_visit_education_provided", "topics_being_provided",
                    "nutrition", "medication_adherence");
            coded(visit, expected, "mothermentor_visit_referal", "referral_given", "yes");
            coded(visit, expected, "mothermentor_visit_referal", "type_of_referral_given",
                    "mental_health", "nutrition");
            scalar(visit, expected, "mothermentor_next_visit_date", "next_appointment_date", "2026-11-20");
            scalar(visit, expected, "mothermentor_visit_comment", "comments", COMMENTS);
            add(visit, "mothermentor_reporting_version", "2", "Internal reporting flag");
            metadata(expected, 1, true);

            render(null, visit);
            assertRows(cards.getChildAt(0), expected);
            assertBulletCount(cards.getChildAt(0), "topics_being_provided", 2);
            assertBulletCount(cards.getChildAt(0), "comments", 0);
            TextView method = cards.getChildAt(0).findViewWithTag("way_education_provided");
            assertTrue(method.getText().toString().contains("sw".equals(language) ? "Binafsi" : "Individual"));
            TextView topics = cards.getChildAt(0).findViewWithTag("topics_being_provided");
            assertTrue(topics.getText().toString().contains("sw".equals(language) ? "Lishe" : "Nutrition"));
        });
        evidence(language + "-services");
    }

    private void render(MotherMentorFollowUpType type, Visit... visits) {
        MotherMentorMedicalHistoryActivityFlv flavor = new MotherMentorMedicalHistoryActivityFlv(type);
        View history = flavor.bindViews(activity);
        scroll = findScroll(history);
        assertNotNull("Real history layout must provide a scroll container", scroll);
        history.setBackgroundColor(Color.WHITE);
        scroll.setFillViewport(true);
        activity.setContentView(history);
        flavor.processViewData(Arrays.asList(visits), activity);
        // Match the concrete activity's customization after rendering the shared Core layout.
        TextView title = history.findViewById(
                org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle);
        assertNotNull(title);
        title.setText(R.string.mothermentor_visit_history_title);
        assertEquals(activity.getString(R.string.mothermentor_visit_history_title),
                title.getText().toString());
        cards = history.findViewById(org.smartregister.chw.core.R.id.linearLayoutHealthFacilityVisitDetails);
        assertNotNull(cards);
        assertTrue(history.isAttachedToWindow());
    }

    private ScrollView findScroll(View view) {
        if (view instanceof ScrollView) return (ScrollView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ScrollView found = findScroll(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private void assertRows(View card, Map<String, String> expected) {
        LinearLayout details = card.findViewById(R.id.visit_details_layout);
        Map<String, String> actual = new LinkedHashMap<>();
        for (int i = 0; i < details.getChildCount(); i++) {
            TextView row = (TextView) details.getChildAt(i);
            if (row.getVisibility() == View.VISIBLE) {
                assertTrue("Row must be attached to the real activity", row.isAttachedToWindow());
                actual.put((String) row.getTag(), row.getText().toString().trim());
            }
        }
        assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.keySet()));
        assertEquals(expected, actual);
    }

    private void assertBulletCount(View card, String key, int count) {
        TextView row = card.findViewWithTag(key);
        assertNotNull(row);
        assertEquals(count, ((Spanned) row.getText()).getSpans(0, row.length(), BulletSpan.class).length);
    }

    private void coded(Visit visit, Map<String, String> expected, String form, String key,
            String... codes) throws Exception {
        JSONObject field = field(form, key);
        JSONObject opposite = field(oppositeDirectory, form, key);
        List<String> translated = new ArrayList<>();
        List<String> saved = new ArrayList<>();
        for (String code : codes) {
            translated.add(option(field, code));
            saved.add(option(opposite, code));
        }
        String raw = codes.length == 1 ? codes[0] : new JSONArray(Arrays.asList(codes)).toString();
        add(visit, key, raw, String.join(", ", saved));
        expected.put(key, label(field) + "\n" + String.join("\n", translated));
    }

    private void scalar(Visit visit, Map<String, String> expected, String form, String key,
            String value) throws Exception {
        add(visit, key, value, value);
        expected.put(key, label(field(form, key)) + "\n" + value);
    }

    private void metadata(Map<String, String> expected, int number, boolean completed) {
        expected.put("visit_number", activity.getString(R.string.mothermentor_history_visit_number)
                + "\n" + number);
        expected.put("visit_completion_state", activity.getString(R.string.mothermentor_history_visit_completion_state)
                + "\n" + activity.getString(completed
                ? R.string.mothermentor_history_completed : R.string.mothermentor_history_saved));
    }

    private Visit visit(String event) {
        Visit visit = new Visit();
        visit.setVisitType(event);
        visit.setBaseEntityId("history-rendering-fixture-not-a-client");
        visit.setDate(new Date());
        visit.setProcessed(true);
        visit.setVisitDetails(new LinkedHashMap<>());
        return visit;
    }

    private void add(Visit visit, String key, String raw, String readable) {
        VisitDetail detail = new VisitDetail();
        detail.setVisitKey(key);
        detail.setDetails(raw);
        detail.setHumanReadable(readable);
        visit.getVisitDetails().computeIfAbsent(key, ignored -> new ArrayList<>()).add(detail);
    }

    private JSONObject field(String form, String key) throws Exception {
        return field(directory, form, key);
    }

    private JSONObject field(String localeDirectory, String form, String key) throws Exception {
        try (InputStream input = activity.getAssets().open(localeDirectory + "/" + form + ".json");
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            JSONArray fields = new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8))
                    .getJSONObject("step1").getJSONArray("fields");
            for (int i = 0; i < fields.length(); i++) {
                JSONObject field = fields.getJSONObject(i);
                if (key.equals(field.optString("key"))) return field;
            }
        }
        throw new AssertionError("Missing packaged form field: " + form + ":" + key);
    }

    private String option(JSONObject field, String key) throws Exception {
        JSONArray options = field.getJSONArray("options");
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            if (key.equals(option.getString("key"))) return option.getString("text");
        }
        throw new AssertionError("Missing packaged option: " + key);
    }

    private String label(JSONObject field) {
        return field.optString("label", field.optString("hint"));
    }

    private void locale(String language) {
        Configuration configuration = new Configuration(originalConfiguration);
        configuration.setLocale(new Locale(language));
        updateConfiguration(configuration);
        directory = "sw".equals(language) ? "json.form-sw" : "json.form";
        oppositeDirectory = "sw".equals(language) ? "json.form" : "json.form-sw";
    }

    @SuppressWarnings("deprecation")
    private void updateConfiguration(Configuration configuration) {
        activity.getResources().updateConfiguration(configuration, activity.getResources().getDisplayMetrics());
    }

    private void evidence(String name) throws Exception {
        instrumentation.waitForIdleSync();
        File folder = new File(instrumentation.getTargetContext().getExternalFilesDir(null),
                "mother-mentor-history-fixture");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        JSONObject rendered = new JSONObject();
        onMain(() -> {
            JSONArray snapshots = new JSONArray();
            for (int card = 0; card < cards.getChildCount(); card++) {
                JSONObject rows = new JSONObject();
                LinearLayout details = cards.getChildAt(card).findViewById(R.id.visit_details_layout);
                for (int i = 0; i < details.getChildCount(); i++) {
                    TextView row = (TextView) details.getChildAt(i);
                    if (row.getVisibility() == View.VISIBLE) {
                        rows.put((String) row.getTag(), row.getText().toString());
                    }
                }
                snapshots.put(rows);
            }
            rendered.put("fixture", "In-memory history only; no client records");
            rendered.put("cards", snapshots);
            scroll.fullScroll(View.FOCUS_UP);
        });
        try (FileOutputStream output = new FileOutputStream(new File(folder, name + ".json"))) {
            output.write(rendered.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        screenshot(new File(folder, name + "-top.png"));
        onMain(() -> scroll.fullScroll(View.FOCUS_DOWN));
        screenshot(new File(folder, name + "-bottom.png"));
    }

    private void screenshot(File file) throws Exception {
        instrumentation.waitForIdleSync();
        SystemClock.sleep(200);
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull("Device screenshot unavailable", bitmap);
        try (FileOutputStream output = new FileOutputStream(file)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally {
            bitmap.recycle();
        }
    }

    private void onMain(MainAction action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> {
            try {
                action.run();
            } catch (Throwable error) {
                failure.set(error);
            }
        });
        Throwable error = failure.get();
        if (error instanceof Exception) throw (Exception) error;
        if (error instanceof Error) throw (Error) error;
    }

    private interface MainAction {
        void run() throws Exception;
    }
}
