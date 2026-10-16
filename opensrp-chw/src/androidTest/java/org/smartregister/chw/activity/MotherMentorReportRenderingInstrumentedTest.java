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
                            + "period:document.getElementById('report_period')?.textContent,"
                            + "facility:document.getElementById('reporting_facility')?.textContent,"
                            + "psg:document.getElementById('mm-1')?.textContent,"
                            + "zero:document.getElementById('mm-2')?.textContent,"
                            + "child:document.getElementById('mm-10')?.textContent,"
                            + "girls:document.getElementById('mm-10-girls-female-1-11m')?.textContent,"
                            + "sourceLimitCorrect:document.body.textContent.includes('Ind. 8, 11, 12:'),"
                            + "unavailable:document.getElementById('mm-8')?.textContent,"
                            + "households:document.getElementById('mm-13')?.textContent,"
                            + "aggregate:document.getElementById('mm-16')?.textContent,"
                            + "unknownAge:document.getElementById('mm-16-female-25-59')?.textContent,"
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
        assertEquals(42, rendered.getInt("rows"));
        assertEquals("April 2026", rendered.getString("period"));
        assertEquals("TEST FIXTURE — no client data", rendered.getString("facility"));
        assertEquals("3", rendered.getString("psg"));
        assertEquals("0", rendered.getString("zero"));
        assertEquals("1", rendered.getString("child"));
        assertEquals("1", rendered.getString("girls"));
        assertTrue(rendered.getBoolean("sourceLimitCorrect"));
        assertEquals("—", rendered.getString("unavailable"));
        assertEquals("2", rendered.getString("households"));
        assertEquals("10", rendered.getString("aggregate"));
        assertEquals("—", rendered.getString("unknownAge"));
        assertTrue(rendered.getString("coverage").contains("Ind. 14"));
        assertTrue(rendered.getString("coverage").contains("Ind. 5"));
        assertTrue(rendered.getBoolean("errorHidden"));
        assertTrue(rendered.getString("unclassified").contains("kiashiria 7: 1"));

        File evidence = context.getExternalFilesDir(null);
        try (FileOutputStream output = new FileOutputStream(new File(evidence, "mother-mentor-report-fixture.json"))) {
            output.write(rendered.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
        try (FileOutputStream output = new FileOutputStream(new File(evidence, "mother-mentor-report-fixture.png"))) {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, output);
        }
        screenshot.recycle();
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
                            + "&& document.getElementById('mm-1').textContent === '—'");
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
            return "{\"nameValuePairs\":{\"mm-1\":3,\"mm-1-male-total\":1,\"mm-1-female-total\":2,"
                    + "\"mm-1-male-20-24\":1,\"mm-1-female-25-59\":2,\"mm-2\":0,"
                    + "\"mm-5-missing-records\":2,\"mm-10\":1,\"mm-10-girls-female-1-11m\":1,\"mm-7\":4,\"mm-7-unclassified\":1,\"mm-13\":2,\"mm-14-missing-records\":1,\"mm-16\":10,\"mm-16-male-total\":3,\"mm-16-female-total\":7}}";
        }
    }
}
