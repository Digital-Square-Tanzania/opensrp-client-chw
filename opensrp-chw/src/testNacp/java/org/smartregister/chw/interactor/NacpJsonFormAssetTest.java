package org.smartregister.chw.interactor;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class NacpJsonFormAssetTest {

    @Test
    public void testNacpChildDangerSignsRulesFilesExist() throws Exception {
        File ruleDir = resolveAsset("rule");
        Set<String> missingRuleFiles = new LinkedHashSet<>();

        for (String formPath : new String[] {
                "json.form/child_hv_danger_sign.json",
                "json.form-sw/child_hv_danger_sign.json"
        }) {
            JSONObject form = new JSONObject(readAsset(formPath));
            Set<String> rulesFiles = new LinkedHashSet<>();
            collectRulesFiles(form, rulesFiles);

            for (String rulesFile : rulesFiles) {
                if (!new File(ruleDir, rulesFile).exists()) {
                    missingRuleFiles.add(formPath + " -> " + rulesFile);
                }
            }
        }

        assertTrue("Missing NACP form rule assets: " + missingRuleFiles, missingRuleFiles.isEmpty());
    }

    private void collectRulesFiles(Object value, Set<String> rulesFiles) throws Exception {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object childValue = object.get(key);
                if ("rules-file".equals(key)) {
                    rulesFiles.add(String.valueOf(childValue));
                } else {
                    collectRulesFiles(childValue, rulesFiles);
                }
            }
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                collectRulesFiles(array.get(i), rulesFiles);
            }
        }
    }

    private String readAsset(String assetName) throws Exception {
        return new String(Files.readAllBytes(resolveAsset(assetName).toPath()), StandardCharsets.UTF_8);
    }

    private File resolveAsset(String assetName) {
        File moduleRelative = new File("src/nacp/assets", assetName);
        if (moduleRelative.exists()) {
            return moduleRelative;
        }

        return new File("opensrp-chw/src/nacp/assets", assetName);
    }
}
