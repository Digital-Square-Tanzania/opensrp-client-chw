package org.smartregister.chw.util;

import org.json.JSONObject;
import org.robolectric.RuntimeEnvironment;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Reads the same merged library and flavor assets packaged in the app. */
public final class MotherMentorTestAssets {
    private MotherMentorTestAssets() { }
    public static JSONObject form(String locale, String name) throws Exception {
        try (InputStream input = RuntimeEnvironment.getApplication().getAssets().open(locale + "/" + name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
        }
    }
}
