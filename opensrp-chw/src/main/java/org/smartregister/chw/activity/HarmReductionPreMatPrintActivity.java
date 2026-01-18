package org.smartregister.chw.activity;

import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import org.smartregister.chw.util.ReportUtils;

import timber.log.Timber;

/**
 * Dedicated activity to render and print the pre-MAT report to avoid context issues
 * when invoking the PrintManager from other screens.
 */
public class HarmReductionPreMatPrintActivity extends AppCompatActivity {
    private static final String REPORT_BASE_URL = "https://appassets.androidplatform.net/assets/reports/harmreduction/";
    private static String pendingHtml;
    private static String pendingJobName;

    public static void start(AppCompatActivity activity, String htmlContent, String jobName) {
        pendingHtml = htmlContent;
        pendingJobName = jobName;
        activity.startActivity(new android.content.Intent(activity, HarmReductionPreMatPrintActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (pendingHtml == null || pendingJobName == null) {
            finish();
            return;
        }

        WebView webView = new WebView(this);
        setContentView(webView);

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        ReportUtils.setPrintJobName(pendingJobName);
        webView.setWebViewClient(new WebViewClientCompat() {
            private boolean printed = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!printed) {
                    printed = true;
                    try {
                        ReportUtils.printTheWebPage(view, HarmReductionPreMatPrintActivity.this);
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    // Give the system a moment to show the print dialog before we close
                    view.postDelayed(() -> {
                        cleanup();
                        finish();
                    }, 500);
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return assetLoader.shouldInterceptRequest(android.net.Uri.parse(url));
            }
        });

        webView.getSettings().setJavaScriptEnabled(false);
        webView.loadDataWithBaseURL(REPORT_BASE_URL, pendingHtml, "text/html", "UTF-8", null);
    }

    @Override
    protected void onDestroy() {
        cleanup();
        super.onDestroy();
    }

    private void cleanup() {
        pendingHtml = null;
        pendingJobName = null;
    }
}
