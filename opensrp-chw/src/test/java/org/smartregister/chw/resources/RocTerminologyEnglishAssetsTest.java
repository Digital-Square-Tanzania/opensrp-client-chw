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
import java.util.regex.Pattern;

public class RocTerminologyEnglishAssetsTest {

    private static final Pattern ROC_ABBREVIATION = Pattern.compile("\\bRoC\\b");

    private static final List<String> ENGLISH_ASSET_PATHS = Arrays.asList(
            "src/nacp/res/values/strings.xml",
            "src/nacp/assets/json.form/ayp_out_school_enrollment.json",
            "src/nacp/assets/reports/harmreduction/5. HR Report.htm"
    );

    @Test
    public void testEnglishAssetsExpandRocAbbreviationInUserFacingText() throws Exception {
        for (String assetPath : ENGLISH_ASSET_PATHS) {
            String asset = readText(assetPath);
            Assert.assertFalse("User-facing English text should expand RoC in " + assetPath,
                    ROC_ABBREVIATION.matcher(asset).find());
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
