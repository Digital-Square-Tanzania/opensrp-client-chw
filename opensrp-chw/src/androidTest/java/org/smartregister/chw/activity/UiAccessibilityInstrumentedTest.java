package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.R;
import org.smartregister.chw.util.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicReference;

/** Native rendering fixtures. No login, client writes, or server calls. */
@RunWith(AndroidJUnit4.class)
public class UiAccessibilityInstrumentedTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

    @Test
    public void loginRemainsScrollableWithLargeTextAndSeparatePasswordTarget() throws Exception {
        RegisterFilterActivity activity = host();
        AtomicReference<View> root = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> {
            View view = inflateLargeText(activity, R.layout.activity_login);
            activity.setContentView(view);
            root.set(view);
        });
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(() -> {
            View checkbox = root.get().findViewById(R.id.login_show_password_checkbox);
            assertTarget(checkbox);
            assertFalse(checkbox.getContentDescription().toString().isEmpty());
            assertTarget(root.get().findViewById(R.id.login_user_name_edit_text));
            assertTarget(root.get().findViewById(R.id.login_password_edit_text));
            assertTarget(root.get().findViewById(R.id.login_login_btn));
            assertTarget(root.get().findViewById(R.id.forgot_password));
            View middle = root.get().findViewById(R.id.middle_section);
            View bottom = root.get().findViewById(R.id.bottom_section);
            assertTrue("Login footer must follow the form", bottom.getTop() >= middle.getBottom());
            ((ScrollView) root.get()).fullScroll(View.FOCUS_DOWN);
        });
        capture(activity, "login-large-text-fixture");
        instrumentation.runOnMainSync(activity::finish);
    }

    @Test
    public void registerSearchHasLabeledTargetsAndSpaceForClearButton() throws Exception {
        RegisterFilterActivity activity = host();
        instrumentation.runOnMainSync(() -> {
            for (int layout : new int[]{R.layout.base_register_search_bar,
                    R.layout.fragment_client_selection_register, R.layout.fragment_mama_kinara_register}) {
                View root = inflateLargeText(activity, layout);
                int width = Math.round(320 * activity.getResources().getDisplayMetrics().density);
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(width * 2, View.MeasureSpec.AT_MOST));
                root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
                View clear = root.findViewById(R.id.btn_search_cancel);
                clear.setVisibility(View.VISIBLE);
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(width * 2, View.MeasureSpec.AT_MOST));
                root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
                assertTarget(clear);
                assertFalse(clear.getContentDescription().toString().isEmpty());
                TextView search = root.findViewById(R.id.edt_search);
                if (layout == R.layout.base_register_search_bar) {
                    assertTrue("White search fields need legible hints",
                            androidx.core.graphics.ColorUtils.calculateContrast(
                                    search.getCurrentHintTextColor(), android.graphics.Color.WHITE) >= 4.5);
                    assertTrue("White search fields need legible input text",
                            androidx.core.graphics.ColorUtils.calculateContrast(
                                    search.getCurrentTextColor(), android.graphics.Color.WHITE) >= 4.5);
                }
                assertTrue("Search field must retain at least 100dp beside filter actions",
                        search.getMeasuredWidth() / search.getResources().getDisplayMetrics().density >= 100);
                assertTrue("Search text needs room before the clear target",
                        search.getPaddingEnd() >= clear.getMeasuredWidth());
            }
        });
        instrumentation.runOnMainSync(activity::finish);
    }

    @Test
    public void enabledFiltersCanScrollToLastLabeledControl() throws Exception {
        RegisterFilterActivity activity = host();
        instrumentation.runOnMainSync(() -> {
            View toggle = activity.findViewById(R.id.enable_filter_switch);
            assertTarget(toggle);
            assertFalse(toggle.getContentDescription().toString().isEmpty());
            toggle.performClick();
            assertEquals(View.VISIBLE, activity.findViewById(R.id.filters_list).getVisibility());
            assertEquals(R.id.hiv_status_filter,
                    activity.findViewById(R.id.title_hiv_status).getLabelFor());
            assertEquals(R.id.prep_status_filter,
                    activity.findViewById(R.id.title_prep_status).getLabelFor());
            View prep = activity.findViewById(R.id.prep_status_filter);
            prep.requestFocus();
            prep.requestRectangleOnScreen(new Rect(0, 0, prep.getWidth(), prep.getHeight()), true);
        });
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(() -> {
            View prep = activity.findViewById(R.id.prep_status_filter);
            Rect visible = new Rect();
            assertTrue("Last filter must be reachable", prep.getGlobalVisibleRect(visible));
            assertTarget(prep);
        });
        capture(activity, "filters-large-text");
        instrumentation.runOnMainSync(activity::finish);
    }

    @Test
    public void enrollmentChoicesRemainReachableAtLargeTextSizes() throws Exception {
        RegisterFilterActivity activity = host();
        int[] layouts = {R.layout.mothermentor_register_partner, R.layout.mothermentor_register_child_eid};
        int[] lastChoices = {R.id.partner_register_new, R.id.child_register_new};
        for (int i = 0; i < layouts.length; i++) {
            final int layout = layouts[i];
            final int lastChoice = lastChoices[i];
            instrumentation.runOnMainSync(() -> activity.setContentView(inflateLargeText(activity, layout)));
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(() -> {
                View choice = activity.findViewById(lastChoice);
                assertTarget(choice);
                choice.requestFocus();
                choice.requestRectangleOnScreen(new Rect(0, 0, choice.getWidth(), choice.getHeight()), true);
            });
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(() -> {
                Rect visible = new Rect();
                assertTrue("Last enrollment action must be reachable",
                        activity.findViewById(lastChoice).getGlobalVisibleRect(visible));
            });
            capture(activity, i == 0 ? "partner-large-text-fixture" : "child-large-text-fixture");
        }
        instrumentation.runOnMainSync(activity::finish);
    }

    @Test
    public void translatedFilterLabelsKeepCanonicalQueryValues() {
        Context context = instrumentation.getTargetContext();
        String[] languages = {"en", "sw", "fr"};
        String[] positiveLabels = {"Positive", "Chanya", "Positif"};
        for (int i = 0; i < languages.length; i++) {
            Configuration configuration = new Configuration(context.getResources().getConfiguration());
            configuration.setLocale(new java.util.Locale(languages[i]));
            android.content.res.Resources resources = context.createConfigurationContext(configuration).getResources();
            String[] values = resources.getStringArray(R.array.hiv_status_filter_options);
            String[] labels = resources.getStringArray(R.array.ui_hiv_status_filter_labels);
            assertEquals(values.length, labels.length);
            for (int color : new int[]{R.color.chw_primary, R.color.opensrp_primary, R.color.text_blue}) {
                assertTrue("Action colors need 4.5:1 contrast in " + languages[i],
                        androidx.core.graphics.ColorUtils.calculateContrast(android.graphics.Color.WHITE,
                                resources.getColor(color)) >= 4.5);
            }
            assertEquals("Positive", values[1]);
            assertEquals(positiveLabels[i], labels[1]);
            assertEquals("Not initiated", resources.getStringArray(R.array.prep_status_filter_options)[2]);
            assertEquals(resources.getStringArray(R.array.prep_status_filter_options).length,
                    resources.getStringArray(R.array.ui_prep_status_filter_labels).length);
        }
    }

    @Test
    public void longReportsKeepTheirLastActionReachableAndHeadingReadable() throws Exception {
        RegisterFilterActivity activity = host();
        instrumentation.runOnMainSync(() -> activity.setContentView(
                inflateLargeText(activity, R.layout.activity_tb_leprosy_reports)));
        instrumentation.waitForIdleSync();
        SystemClock.sleep(200);
        instrumentation.runOnMainSync(() -> {
            androidx.core.widget.NestedScrollView scroll = activity.findViewById(R.id.ui_report_scroll);
            scroll.fullScroll(View.FOCUS_DOWN);
        });
        instrumentation.waitForIdleSync();
        SystemClock.sleep(200);
        instrumentation.runOnMainSync(() -> {
            View last = activity.findViewById(R.id.tb_leprosy_report_merged);
            Rect visible = new Rect();
            assertTrue("Last report action must be reachable", last.getGlobalVisibleRect(visible));
            assertTrue("Last report action must be fully visible", visible.height() >= last.getHeight() - 2);
            TextView heading = activity.findViewById(R.id.toolbar_title);
            for (int line = 0; line < heading.getLayout().getLineCount(); line++) {
                assertEquals("Report heading must remain readable", 0,
                        heading.getLayout().getEllipsisCount(line));
            }
        });
        capture(activity, "report-last-action-large-text-fixture");
        instrumentation.runOnMainSync(activity::finish);
    }

    @Test
    public void vaccinationDateActionCanBeScrolledFullyIntoView() throws Exception {
        RegisterFilterActivity activity = host();
        instrumentation.runOnMainSync(() -> activity.setContentView(
                inflateLargeText(activity, R.layout.multiple_vaccination_dialog_view)));
        instrumentation.waitForIdleSync();
        SystemClock.sleep(150);
        instrumentation.runOnMainSync(() -> {
            View action = activity.findViewById(R.id.add_date_separately);
            action.requestRectangleOnScreen(new Rect(0, 0, action.getWidth(), action.getHeight()), true);
        });
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(() -> {
            View action = activity.findViewById(R.id.add_date_separately);
            Rect visible = new Rect();
            assertTrue(action.getGlobalVisibleRect(visible));
            assertTrue("Complete vaccination action must be visible", visible.height() >= action.getHeight() - 2);
        });
        capture(activity, "vaccination-date-action-large-text-fixture");
        instrumentation.runOnMainSync(activity::finish);
    }

    @Test
    public void clientRowKeepsLongMetadataInsideItsColumn() throws Exception {
        RegisterFilterActivity activity = host();
        instrumentation.runOnMainSync(() -> {
            View root = inflateLargeText(activity, R.layout.all_client_register_list_row);
            TextView type = root.findViewById(R.id.tv_opdRegisterListRow_registerType);
            TextView location = root.findViewById(R.id.tv_opdRegisterListRow_location);
            type.setText(R.string.mothermentor_child_eid_register_tag);
            location.setText("Long village name - ward name - district name");
            ((TextView) root.findViewById(R.id.tv_opdRegisterListRow_gender)).setText("Female");
            float density = root.getResources().getDisplayMetrics().density;
            int width = Math.round(320 * density);
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
            View column = root.findViewById(R.id.child_column);
            View action = root.findViewById(R.id.go_to_profile_image_view);
            assertTrue("Metadata must stay before the action column", column.getRight() <= action.getLeft());
            assertTrue("Location must remain in its column", location.getWidth() <= column.getWidth());
            TextView gender = root.findViewById(R.id.tv_opdRegisterListRow_gender);
            TextView divider = root.findViewById(R.id.tv_opdRegisterListRow_firstDotDivider);
            assertTrue("Sex and location need the same first-line baseline",
                    gender.getTop() + gender.getBaseline() == location.getTop() + location.getBaseline());
            assertTrue("Sex must start at the metadata row's leading edge", gender.getLeft() == 0);
            assertTrue("Location must follow sex and its separator", location.getLeft() >= divider.getRight());
            assertTrue("Shared text styles must not indent location", location.getPaddingStart() == 0);

            assertTrue("Location must show its complete text", location.getLayout().getHeight() <= location.getHeight());
            assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(
                    location.getCurrentTextColor(), android.graphics.Color.WHITE) >= 4.5);
        });
        instrumentation.runOnMainSync(activity::finish);
    }

    @Test
    public void visitToolbarAndNativeFormChoicesHaveUsableTargets() throws Exception {
        RegisterFilterActivity activity = host();
        instrumentation.runOnMainSync(() -> {
            int[] layouts = {R.layout.activity_base_mothermentor_visit_activity,
                    R.layout.native_form_item_radiobutton, R.layout.native_form_item_checkbox,
                    R.layout.native_item_radio_button};
            for (int resource : layouts) {
                View root = inflateLargeText(activity, resource);
                float density = root.getResources().getDisplayMetrics().density;
                root.measure(View.MeasureSpec.makeMeasureSpec(Math.round(320 * density), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(Math.round(600 * density), View.MeasureSpec.AT_MOST));
                root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
                if (resource == R.layout.activity_base_mothermentor_visit_activity) {
                    assertTarget(root.findViewById(R.id.close));
                    assertTarget(root.findViewById(R.id.customFontTextViewSubmit));
                    assertFalse(root.findViewById(R.id.close).getContentDescription().toString().isEmpty());
                } else if (resource == R.layout.native_form_item_radiobutton) {
                    assertTarget(root);
                } else if (resource == R.layout.native_form_item_checkbox) {
                    assertTarget(root.findViewById(R.id.checkbox));
                } else {
                    assertTarget(root.findViewById(R.id.mainRadioButton));
                }
            }
        });
        instrumentation.runOnMainSync(activity::finish);
    }

    @Test
    public void motherMentorProfileKeepsIdentityMetadataInSeparateReadableRows() throws Exception {
        RegisterFilterActivity activity = host();
        instrumentation.runOnMainSync(() -> {
            View root = inflateLargeText(activity, R.layout.activity_mothermentor_profile);
            ((TextView) root.findViewById(R.id.textview_name)).setText("Example client name, 30");
            ((TextView) root.findViewById(R.id.textview_address)).setText("Village name - ward - district");
            ((TextView) root.findViewById(R.id.textview_id)).setText("123456789012345");
            TextView ctc = root.findViewById(R.id.textview_ctc_number);
            ctc.setText("CTC ID: 12-34-5678-901234");
            ctc.setVisibility(View.VISIBLE);
            activity.setContentView(root);
        });
        instrumentation.waitForIdleSync();
        SystemClock.sleep(150);
        instrumentation.runOnMainSync(() -> {
            TextView gender = activity.findViewById(R.id.textview_gender);
            TextView address = activity.findViewById(R.id.textview_address);
            TextView id = activity.findViewById(R.id.textview_id);
            assertTrue("Address must follow gender", address.getTop() >= gender.getBottom());
            assertTrue("Client ID must follow address", id.getTop() >= address.getBottom());
            Rect visible = new Rect();
            assertTrue("Client ID must remain visible", id.getGlobalVisibleRect(visible));
        });
        capture(activity, "mother-profile-identity-large-text-fixture");
        instrumentation.runOnMainSync(activity::finish);
    }

    /** Optional screenshot sweep. Empty templates are explicitly distinct from signed-in flows. */
    @Test
    public void rendersOwnedLayoutTemplatesForReview() throws Exception {
        String names = InstrumentationRegistry.getArguments().getString("auditLayouts", "");
        if (names.isEmpty()) return;
        RegisterFilterActivity activity = host();
        JSONArray results = new JSONArray();
        for (String name : names.split(",")) {
            JSONObject result = new JSONObject().put("layout", name).put("kind", "resource-template");
            AtomicReference<View> root = new AtomicReference<>();
            try {
                instrumentation.runOnMainSync(() -> {
                    int resource = activity.getResources().getIdentifier(name, "layout", activity.getPackageName());
                    View view = inflateLargeText(activity, resource);
                    activity.setContentView(view);
                    root.set(view);
                });
                instrumentation.waitForIdleSync();
                SystemClock.sleep(150);
                JSONArray clipped = new JSONArray();
                instrumentation.runOnMainSync(() -> collectClippedText(root.get(), clipped));
                result.put("clippedTextCandidates", clipped).put("inflated", true);
                capture(activity, "template-" + name);
            } catch (RuntimeException error) {
                result.put("inflated", false).put("error", error.toString());
            }
            results.put(result);
        }
        File directory = new File(activity.getFilesDir(), "ui-audit");
        try (FileOutputStream output = new FileOutputStream(new File(directory, "template-results.json"))) {
            output.write(results.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        instrumentation.runOnMainSync(activity::finish);
    }

    private void collectClippedText(View view, JSONArray results) {
        if (view.getVisibility() != View.VISIBLE) return;
        if (view instanceof TextView && !(view instanceof EditText)) {
            TextView text = (TextView) view;
            if (text.getLayout() != null && text.length() > 0) {
                int lines = text.getLayout().getLineCount();
                boolean ellipsis = false;
                for (int i = 0; i < lines; i++) ellipsis |= text.getLayout().getEllipsisCount(i) > 0;
                if (ellipsis || text.getLayout().getHeight() > text.getHeight()
                        - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom() + 2) {
                    results.put(text.getId() == View.NO_ID ? "unidentified-label"
                            : text.getResources().getResourceEntryName(text.getId()));
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collectClippedText(group.getChildAt(i), results);
        }
    }

    private RegisterFilterActivity host() {
        Context context = instrumentation.getTargetContext();
        Intent intent = new Intent(context, RegisterFilterActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Constants.ENABLE_HIV_STATUS_FILTER, true)
                .putExtra(Constants.ENABLE_PREP_STATUS_FILTER, true)
                .putExtra(Constants.ENABLE_DATE_RANGE_FILTER, true);
        return (RegisterFilterActivity) instrumentation.startActivitySync(intent);
    }

    private View inflateLargeText(Context context, int resource) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.fontScale = 2f;
        Context large = context.createConfigurationContext(configuration);
        return LayoutInflater.from(new ContextThemeWrapper(large, R.style.ChwTheme_NoActionBar))
                .inflate(resource, null, false);
    }

    private void assertTarget(View view) {
        float density = view.getResources().getDisplayMetrics().density;
        assertTrue("Touch target too short: " + view.getId(), view.getMeasuredHeight() / density >= 47.5f);
        assertTrue("Touch target too narrow: " + view.getId(), view.getMeasuredWidth() / density >= 47.5f);
    }

    private void capture(Context context, String name) throws Exception {
        SystemClock.sleep(300);
        Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
        File directory = new File(context.getFilesDir(), "ui-audit");
        assertTrue(directory.exists() || directory.mkdirs());
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, output);
        }
        screenshot.recycle();
    }
}
