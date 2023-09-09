package org.smartregister.chw.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.smartregister.chw.R;

import java.io.File;

import timber.log.Timber;


/**
 * Author issyzac on 25/07/2023
 */
public class UCSEnvironmentSwitchActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new PreferenceFragment())
                .commit();
    }

    public static class PreferenceFragment extends PreferenceFragmentCompat implements
            Preference.OnPreferenceClickListener {

        private int countClick = 0;

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            //super.onCreatePreferences(savedInstanceState, rootKey);
            setPreferencesFromResource(R.xml.ucs_switch_env_preference, rootKey);

            Preference pref = findPreference("preference");
            if (pref != null)
                pref.setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference preference) {
            if (preference.getKey().equalsIgnoreCase("preference")) {
                if (countClick < 7) {
                    countClick++;
                }
                if (countClick == 7) {
                    confirmSwitchingEnvironment(getActivity(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i == -1) {
                                clearApplicationData();
                            } else {
                                countClick = 0;
                                preference.setVisible(true);
                                dialogInterface.dismiss();
                            }
                        }
                    });
                }
            }
            return false;
        }

        private void confirmSwitchingEnvironment(Context context, final DialogInterface.OnClickListener onDialogButtonClick) {
            final Boolean[] userResponse = {false};
            final AlertDialog alert = new AlertDialog.Builder(context, R.style.SettingsAlertDialog).create();
            View title_view = this.getLayoutInflater().inflate(R.layout.switch_env_dialog, null);
            alert.setCustomTitle(title_view);
            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), (dialog, which) -> {
                onDialogButtonClick.onClick(dialog, which);
                userResponse[0] = true;
                alert.dismiss();
            });
            alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), (dialog, which) -> {
                onDialogButtonClick.onClick(dialog, which);
                userResponse[0] = false;
                alert.dismiss();
            });
            alert.show();
        }

        public void clearApplicationData() {
            Toast.makeText(requireActivity(), "Clearing environment data wait ...", Toast.LENGTH_LONG).show();
            File appDir = new File(Environment.getDataDirectory() + File.separator + "data/org.smartregister.chw.moh");
            if (appDir.exists()) {
                String[] children = appDir.list();
                for (String s : children) {
                    if (!s.equals("lib")) {
                        deleteDir(new File(appDir, s));
                        Timber.i("File /data/data/APP_PACKAGE/" + s + " DELETED");
                    }
                }
            }  // TODO else part
            requireActivity().finish();
            System.exit(0);

        }

        public static boolean deleteDir(File dir) {
            if (dir != null && dir.isDirectory()) {
                String[] children = dir.list();
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }

            return dir.delete();
        }

    }

}
