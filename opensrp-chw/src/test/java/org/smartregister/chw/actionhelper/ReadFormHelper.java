package org.smartregister.chw.actionhelper;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.AllConstants;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import timber.log.Timber;

public class ReadFormHelper {

    public static JSONObject getFormJson(Context mContext, String formIdentity) {
        if (mContext != null) {
            try {
                String locale = mContext.getResources().getConfiguration().locale.getLanguage();
                locale = locale.equalsIgnoreCase(Locale.ENGLISH.getLanguage()) ? "" : "-" + locale;

                InputStream inputStream;
                try {
                    inputStream = mContext.getApplicationContext().getAssets()
                            .open("json.form" + locale + "/" + formIdentity + AllConstants.JSON_FILE_EXTENSION);
                } catch (FileNotFoundException e) {
                    // file for the language not found, defaulting to english language
                    inputStream = mContext.getApplicationContext().getAssets()
                            .open("json.form/" + formIdentity + AllConstants.JSON_FILE_EXTENSION);
                }
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, UTF_8));
                String jsonString;
                StringBuilder stringBuilder = new StringBuilder();

                while ((jsonString = reader.readLine()) != null) {
                    stringBuilder.append(jsonString);
                }
                inputStream.close();

                return new JSONObject(stringBuilder.toString());
            } catch (IOException | JSONException e) {
                Timber.e(e);
            }
        }
        return null;
    }
}
