package org.smartregister.chw.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import org.smartregister.chw.R;
import org.smartregister.chw.util.ReportUtils;

/**
 * Dedicated activity to render and print the pre-MAT report with an explicit print action.
 */
public class HarmReductionPreMatPrintActivity extends AppCompatActivity {
    private static final String REPORT_BASE_URL = "https://appassets.androidplatform.net/assets/reports/harmreduction/";
    private static String pendingHtml;
    private static String pendingJobName;

    private WebView webView;

    public static void start(AppCompatActivity activity, String htmlContent, String jobName) {
        pendingHtml = htmlContent;
        pendingJobName = jobName;
        activity.startActivity(new Intent(activity, HarmReductionPreMatPrintActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (pendingHtml == null || pendingJobName == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_reports_view);
        setupToolbar();

        webView = findViewById(R.id.webview);
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        ReportUtils.setPrintJobName(pendingJobName);
        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return assetLoader.shouldInterceptRequest(Uri.parse(url));
            }
        });

        webView.getSettings().setJavaScriptEnabled(false);
        webView.loadDataWithBaseURL(REPORT_BASE_URL, pendingHtml, "text/html", "UTF-8", null);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(org.smartregister.chw.core.R.id.back_to_nav_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(org.smartregister.chw.core.R.drawable.ic_arrow_back_white_24dp);
            actionBar.setElevation(0);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        if (toolbar != null) {
            toolbar.setTitle(R.string.harm_reduction_pre_mat_session_history);
            android.view.View titleView = toolbar.findViewById(org.smartregister.chw.core.R.id.toolbar_title);
            if (titleView instanceof android.widget.TextView) {
                ((android.widget.TextView) titleView).setText(R.string.harm_reduction_pre_mat_session_history);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reports_view_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_print) {
            if (webView != null) {
                ReportUtils.printTheWebPage(webView, this);
            } else {
                Toast.makeText(this, R.string.reports_title, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
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
