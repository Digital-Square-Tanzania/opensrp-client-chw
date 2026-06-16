package org.smartregister.chw.resources;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class DrugTerminologyAssetScanTest {

    private static final String OLD_TERM = "ma" + "dawa ya kulevya";
    private static final String NEW_TERM = "dawa za kulevya";
    private static final List<String> AFFECTED_FILES = Arrays.asList(
            "src/nacp/assets/json.form-sw/agyw_screening.json",
            "src/nacp/assets/json.form-sw/hps_education_on_behavioural_change.json",
            "src/nacp/res/values-sw/strings.xml"
    );

    @Test
    public void shouldNotContainDeprecatedDrugTerminology() throws Exception {
        for (String relativePath : AFFECTED_FILES) {
            String content = readText(relativePath).toLowerCase();

            Assert.assertFalse("Found deprecated terminology in " + relativePath, content.contains(OLD_TERM));
            Assert.assertTrue("Missing updated terminology in " + relativePath, content.contains(NEW_TERM));
        }
    }

    private static String readText(String relativePath) throws IOException {
        return new String(Files.readAllBytes(resolvePath(relativePath)), StandardCharsets.UTF_8);
    }

    private static Path resolvePath(String relativePath) {
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
