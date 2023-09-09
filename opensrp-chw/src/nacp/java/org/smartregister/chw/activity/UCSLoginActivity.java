package org.smartregister.chw.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.smartregister.AllConstants;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fragment.EnvironmentSelectDialogFragment;
import org.smartregister.chw.util.UCSSwitchConstants;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.PermissionUtils;
import org.smartregister.util.Utils;

import java.net.MalformedURLException;
import java.net.URL;

import timber.log.Timber;

/**
 * Author issyzac on 25/07/2023
 */
public class UCSLoginActivity extends LoginActivity {

    private TextView environmentIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        environmentIndicator = findViewById(R.id.env_indicator);

        setServerUrl();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(getString(R.string.environment_switch));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().toString().equalsIgnoreCase(getString(R.string.environment_switch)) && hasPermissions()) {
            this.startActivity(new Intent(this, UCSEnvironmentSwitchActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean hasPermissions() {
        return PermissionUtils.isPermissionGranted(this
                , new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}
                , CoreConstants.RQ_CODE.STORAGE_PERMISIONS);
    }

    private void setServerUrl() {
        try {

            AllSharedPreferences sharedPreferences = Utils.getAllSharedPreferences();
            // Check if the preferences for the environment have been set yet
            if (!sharedPreferences.getPreference(UCSSwitchConstants.UCS_ENVIRONMENT).isEmpty()) {
                if (sharedPreferences.getPreference(UCSSwitchConstants.UCS_ENVIRONMENT).equalsIgnoreCase(UCSSwitchConstants.PRODUCTION_ENV)) {
                    updateEnvironmentUrl(BuildConfig.opensrp_url);
                    setEnvironmentIndicator(UCSSwitchConstants.PRODUCTION_ENV);
                    updateSyncFilter();
                } else if (sharedPreferences.getPreference(UCSSwitchConstants.UCS_ENVIRONMENT).equalsIgnoreCase(UCSSwitchConstants.STAGING_ENV)) {
                    updateEnvironmentUrl(BuildConfig.opensrp_url_debug);
                    setEnvironmentIndicator(UCSSwitchConstants.STAGING_ENV);
                } else {
                    //This is testing env
                    updateEnvironmentUrl(BuildConfig.opensrp_url_debug);
                    setEnvironmentIndicator(UCSSwitchConstants.DEVELOPMENT_ENV);
                }
            } else {
                EnvironmentSelectDialogFragment switchFrag = new EnvironmentSelectDialogFragment(new UpdateEnvironmentIndicator() {
                    @Override
                    public void onEnvironmentSelected(String indicator) {
                        setEnvironmentIndicator(indicator);
                    }
                });
                switchFrag.show(this.getSupportFragmentManager(), "switch_env");
                Toast.makeText(this, "Environment not configured yet, please choose environment ...", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSyncFilter() {
        //NOT REQUIRED
    }

    private void updateEnvironmentUrl(String baseUrl) {
        try {

            AllSharedPreferences allSharedPreferences = Utils.getAllSharedPreferences();

            URL url = new URL(baseUrl);

            String base = url.getProtocol() + "://" + url.getHost();
            int port = url.getPort();

            Timber.i("Base URL: %s", base);
            Timber.i("Port: %s", port);

            allSharedPreferences.saveHost(base);
            allSharedPreferences.savePort(port);

            allSharedPreferences.savePreference(AllConstants.DRISHTI_BASE_URL, baseUrl);

            Timber.i("Saved URL: %s", allSharedPreferences.fetchHost(""));
            Timber.i("Port: %s", allSharedPreferences.fetchPort(0));
        } catch (MalformedURLException e) {
            Timber.e("Malformed Url: %s", baseUrl);
        }
    }

    private void setEnvironmentIndicator(String selectedEnv) {
        switch (selectedEnv) {
            case UCSSwitchConstants.PRODUCTION_ENV:
                environmentIndicator.setText("");
                break;
            case UCSSwitchConstants.STAGING_ENV:
                environmentIndicator.setText(UCSSwitchConstants.STAGING_ENV);
                environmentIndicator.setTextColor(getResources().getColor(R.color.primary));
                break;
            default:
                environmentIndicator.setText(UCSSwitchConstants.DEVELOPMENT_ENV);
                environmentIndicator.setTextColor(getResources().getColor(R.color.alert_urgent_red));
                break;
        }
    }

    public interface UpdateEnvironmentIndicator {
        void onEnvironmentSelected(String indicator);
    }

}
