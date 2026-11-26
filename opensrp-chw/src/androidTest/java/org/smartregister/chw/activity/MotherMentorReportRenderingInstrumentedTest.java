package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.R;
import org.smartregister.chw.util.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Rendering fixture only: does not sign in, call a server, or insert client records. */
@RunWith(AndroidJUnit4.class)
public class MotherMentorReportRenderingInstrumentedTest {
    @Test
    public void rendersWorkbookInsideReportActivityWithoutInventingUnavailableCounts() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        Intent intent = new Intent(context, MotherMentorReportsViewActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(ChwReportsViewActivity.ARG_REPORT_PATH, Constants.ReportConstants.ReportPaths.MOTHER_MENTOR_REPORT_PATH)
                .putExtra(ChwReportsViewActivity.ARG_REPORT_DATE, "April 2026")
                .putExtra(ChwReportsViewActivity.ARG_REPORT_TITLE, R.string.mother_mentor_reports)
                // Avoid opening an unauthenticated database before replacing the bridge below.
                .putExtra(ChwReportsViewActivity.ARG_REPORT_TYPE, "rendering-fixture");
        MotherMentorReportsViewActivity activity = (MotherMentorReportsViewActivity) instrumentation.startActivitySync(intent);
        WebView webView = activity.findViewById(R.id.webview);
        instrumentation.runOnMainSync(() -> {
            webView.addJavascriptInterface(new FixtureBridge(), "Android");
            webView.stopLoading();
            webView.loadUrl("https://appassets.androidplatform.net/assets/reports/mother-mentor-report.html");
        });

        JSONObject rendered = null;
        long deadline = SystemClock.uptimeMillis() + 15000;
        while (SystemClock.uptimeMillis() < deadline) {
            String output = evaluate(instrumentation, webView,
                    "JSON.stringify({rows:document.querySelectorAll('#report_body tr').length,"
                            + "numbers:[...document.querySelectorAll('#report_body .number-cell')].map(c=>c.textContent).join(','),"
                            + "label5:document.getElementById('mm-5')?.closest('tr')?.querySelector('th')?.textContent,"
                            + "birthBoy:document.getElementById('mm-7-boys-male-under-1m')?.textContent,"
                            + "householdAgeClosed:!document.getElementById('mm-15-female-25-59'),"
                            + "period:document.getElementById('report_period')?.textContent,"
                            + "facility:document.getElementById('reporting_facility')?.textContent,"
                            + "psg:document.getElementById('mm-3')?.textContent,"
                            + "zero:document.getElementById('mm-4')?.textContent,"
                            + "child:document.getElementById('mm-9')?.textContent,"
                            + "girls:document.getElementById('mm-9-girls-female-1-11m')?.textContent,"
                            + "partnerTests:document.getElementById('mm-12')?.textContent,"
                            + "positiveGirl:document.getElementById('mm-10-positive-female-12-23m')?.textContent,"
                            + "negativeBoy:document.getElementById('mm-10-negative-male-12-23m')?.textContent,"
                            + "partnerEducation:document.getElementById('mm-13')?.textContent,"
                            + "unavailable:document.getElementById('mm-13-hiv')?.textContent,"
                            + "households:document.getElementById('mm-14')?.textContent,"
                            + "householdCounselled:document.getElementById('mm-15')?.textContent,"
                            + "householdMale:document.getElementById('mm-15-male-total')?.textContent,"
                            + "householdFemale:document.getElementById('mm-15-female-total')?.textContent,"
                            + "aggregate:document.getElementById('mm-18')?.textContent,"
                            + "unknownAge:document.getElementById('mm-18-female-25-59')?.textContent,"
                            + "countingNotes:document.getElementById('counting_notes')?.textContent,"
                            + "coverage:document.getElementById('coverage_note')?.textContent,"
                            + "errorHidden:document.getElementById('report_error')?.hidden,"
                            + "unclassified:document.getElementById('unclassified_note')?.textContent})");
            Object decoded = new JSONTokener(output).nextValue();
            if (decoded instanceof String) {
                rendered = new JSONObject((String) decoded);
                if ("3".equals(rendered.optString("psg"))) break;
            }
            SystemClock.sleep(100);
        }
        assertTrue("Report fixture did not load", rendered != null);
        assertEquals(36, rendered.getInt("rows"));
        assertEquals("1,2,3,4,5,6,7,a,b,8,a,b,9,a,b,10,a,b,11,a,b,c,d,e,f,12,13,a,b,c,14,15,16,17,18,19",
                rendered.getString("numbers"));
        assertEquals("Idadi ya wateja wa PMTCT walioelimishwa na Mama Kinara", rendered.getString("label5"));
        assertEquals("1", rendered.getString("birthBoy"));
        assertTrue(rendered.getBoolean("householdAgeClosed"));
        assertEquals("April 2026", rendered.getString("period"));
        assertEquals("TEST FIXTURE — no client data", rendered.getString("facility"));
        assertEquals("3", rendered.getString("psg"));
        assertEquals("0", rendered.getString("zero"));
        assertEquals("1", rendered.getString("child"));
        assertEquals("1", rendered.getString("girls"));
        assertEquals("2", rendered.getString("partnerTests"));
        assertEquals("1", rendered.getString("positiveGirl"));
        assertEquals("1", rendered.getString("negativeBoy"));
        assertEquals("—", rendered.getString("partnerEducation"));
        assertEquals("—", rendered.getString("unavailable"));
        assertEquals("2", rendered.getString("households"));
        assertEquals("5", rendered.getString("householdCounselled"));
        assertEquals("—", rendered.getString("householdMale"));
        assertEquals("—", rendered.getString("householdFemale"));
        assertEquals("10", rendered.getString("aggregate"));
        assertEquals("—", rendered.getString("unknownAge"));
        assertTrue(rendered.getString("countingNotes").contains("Ind. 11"));
        assertTrue(rendered.getString("countingNotes").contains("Ind. 13"));
        assertTrue(!rendered.getString("countingNotes").contains("ART"));
        assertTrue(rendered.getString("coverage").contains("Ind. 15"));
        assertTrue(rendered.getString("coverage").contains("visits counted participants by role"));
        assertTrue(rendered.getString("coverage").contains("Ind. 11"));
        assertTrue(rendered.getString("coverage").contains("Ind. 13"));
        assertTrue(rendered.getBoolean("errorHidden"));
        assertTrue(rendered.getString("unclassified").contains("kiashiria 5: 1"));

        File evidence = context.getExternalFilesDir(null);
        try (FileOutputStream output = new FileOutputStream(new File(evidence, "mother-mentor-report-fixture.json"))) {
            output.write(rendered.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        if (Boolean.parseBoolean(InstrumentationRegistry.getArguments()
                .getString("captureScreenshot", "true"))) {
            Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
            try (FileOutputStream output = new FileOutputStream(new File(evidence, "mother-mentor-report-fixture.png"))) {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, output);
            }
            screenshot.recycle();
        }
        // Optional bounded capture window for an external UIAutomator dump during manual QA.
        String captureWindow = InstrumentationRegistry.getArguments().getString("captureWindowMs", "0");
        SystemClock.sleep(Math.min(30000, Long.parseLong(captureWindow)));
        // A broken bridge must show an error and unavailable cells, never fabricated zeros.
        instrumentation.runOnMainSync(() -> {
            webView.addJavascriptInterface(new InvalidFixtureBridge(), "Android");
            webView.reload();
        });
        boolean failureDisplayed = false;
        deadline = SystemClock.uptimeMillis() + 15000;
        while (SystemClock.uptimeMillis() < deadline) {
            String state = evaluate(instrumentation, webView,
                    "!!document.getElementById('report_error') && !document.getElementById('report_error').hidden "
                            + "&& document.getElementById('mm-3').textContent === '—'");
            if ("true".equals(state)) { failureDisplayed = true; break; }
            SystemClock.sleep(100);
        }
        assertTrue("Invalid report data must show unavailable cells and an error", failureDisplayed);
        instrumentation.runOnMainSync(activity::finish);
    }

    private String evaluate(Instrumentation instrumentation, WebView webView, String script) throws Exception {
        AtomicReference<String> output = new AtomicReference<>();
        CountDownLatch result = new CountDownLatch(1);
        instrumentation.runOnMainSync(() -> webView.evaluateJavascript(script, value -> {
            output.set(value);
            result.countDown();
        }));
        assertTrue("WebView did not return its rendered state", result.await(20, TimeUnit.SECONDS));
        return output.get();
    }

    public static class InvalidFixtureBridge extends FixtureBridge {
        @Override @JavascriptInterface public String getDataForReport() { return "invalid json"; }
    }

    public static class FixtureBridge {
        @JavascriptInterface
        public String getDataPeriod() { return "April 2026"; }

        @JavascriptInterface
        public String getReportingFacility() { return "TEST FIXTURE — no client data"; }

        @JavascriptInterface
        public String getDataForReport() {
            return "{\"nameValuePairs\":{\"mm-3\":3,\"mm-3-male-total\":1,\"mm-3-female-total\":2,"
                    + "\"mm-3-male-20-24\":1,\"mm-3-female-25-59\":2,\"mm-4\":0,"
                    + "\"mm-12\":2,\"mm-10\":3,"
                    + "\"mm-10-positive-female-12-23m\":1,\"mm-10-negative-male-12-23m\":1,"
                    + "\"mm-7-boys-male-under-1m\":1,\"mm-13-missing-records\":2,"
                    + "\"mm-11-missing-records\":2,\"mm-9\":1,\"mm-9-girls-female-1-11m\":1,\"mm-5\":4,\"mm-5-unclassified\":1,\"mm-14\":2,\"mm-15\":5,\"mm-15-role-visits\":1,\"mm-15-missing-records\":0,\"mm-18\":10,\"mm-18-male-total\":3,\"mm-18-female-total\":7}}";
        }
    }
}
