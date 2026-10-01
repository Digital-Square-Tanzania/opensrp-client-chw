package org.smartregister.chw.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KvpProfileSwahiliStringsTest {

    private static final String REGISTRATION_FORM_PATH =
            "src/nacp/assets/json.form-sw/kvp_prep_registration.json";
    private static final String DEFAULT_STRINGS_PATH = "src/nacp/res/values/strings.xml";
    private static final String STRINGS_PATH = "src/main/res/values-sw/strings.xml";
    private static final Map<String, String> EXPECTED_GROUPS = expectedGroups();

    @Test
    public void profileHeadingAndAllRegistrationGroupsHaveSwahiliTranslations() throws Exception {
        String strings = readText(STRINGS_PATH);

        assertTrue(strings.contains(
                "<string name=\"dominant_kvp_group\">Kundi kuu la KVP: %1$s</string>"));
        for (Map.Entry<String, String> group : EXPECTED_GROUPS.entrySet()) {
            assertTrue("Missing Swahili profile translation for " + group.getKey(),
                    strings.contains("<string name=\"kvp_" + group.getKey() + "\">"
                            + group.getValue() + "</string>"));
        }
    }

    @Test
    public void allLocalizedGroupsHaveDefaultResourceEntries() throws Exception {
        String strings = readText(DEFAULT_STRINGS_PATH);

        for (String group : EXPECTED_GROUPS.keySet()) {
            assertTrue("Missing default profile resource for " + group,
                    strings.contains("<string name=\"kvp_" + group + "\">"));
        }
    }

    @Test
    public void profileTranslationsMatchSwahiliRegistrationOptions() throws Exception {
        JSONObject step = new JSONObject(readText(REGISTRATION_FORM_PATH)).getJSONObject("step1");
        JSONArray fields = step.getJSONArray("fields");
        Map<String, String> registrationGroups = new LinkedHashMap<>();

        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (!field.getString("key").startsWith("client_group_")) {
                continue;
            }
            JSONArray options = field.getJSONArray("options");
            for (int j = 0; j < options.length(); j++) {
                JSONObject option = options.getJSONObject(j);
                registrationGroups.put(option.getString("key"), option.getString("text"));
            }
        }

        assertEquals(EXPECTED_GROUPS, registrationGroups);
    }

    private static Map<String, String> expectedGroups() {
        Map<String, String> groups = new LinkedHashMap<>();
        groups.put("fsw", "Mwanamke aliye katika hatari zaidi");
        groups.put("pwid", "Mjidunga dawa za kulevya");
        groups.put("prisoners", "Mfungwa");
        groups.put("inmate", "Mahabusu");
        groups.put("agyw", "Msichana rika balehe na mwanamke kijana");
        groups.put("discordant_couples", "Mwenza mwenye majibu kinzani");
        groups.put("fisherman", "Wavuvi");
        groups.put("truck_drivers", "Dereva wa lori");
        groups.put("miners", "Mchimbaji Madini");
        groups.put("other", "Makundi Mengine hatarishi");
        groups.put("msm", "Mwanaume aliye katika hatari zaidi");
        return groups;
    }

    private String readText(String relativePath) throws Exception {
        return new String(Files.readAllBytes(resolvePath(relativePath)), StandardCharsets.UTF_8);
    }

    private Path resolvePath(String relativePath) {
        Path direct = Paths.get(relativePath);
        if (Files.exists(direct)) {
            return direct;
        }

        Path modulePath = Paths.get("opensrp-chw").resolve(relativePath);
        if (Files.exists(modulePath)) {
            return modulePath;
        }

        throw new AssertionError("Could not resolve path: " + relativePath);
    }
}
