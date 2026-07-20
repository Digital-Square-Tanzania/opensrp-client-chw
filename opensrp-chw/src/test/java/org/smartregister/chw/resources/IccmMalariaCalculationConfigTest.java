package org.smartregister.chw.resources;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IccmMalariaCalculationConfigTest {

    private static final String ICCM_MALARIA_CALCULATION_RULES_PATH = "src/nacp/assets/rule/iccm_malaria_calculation.yml";

    @Test
    public void aluTabletDispensingCalculationShouldSupportDecimalWeightValues() throws Exception {
        String rules = readText(ICCM_MALARIA_CALCULATION_RULES_PATH);

        Assert.assertTrue(
                "ALU tablet calculation should parse weight as a decimal value",
                rules.contains("float weightValue = Float.parseFloat(weight.toString());")
        );
        Assert.assertFalse(
                "ALU tablet calculation should not use Integer.parseInt because decimal weights throw and resolve to 0",
                rules.contains("int weightValue = Integer.parseInt(weight);")
        );
    }

    private static String readText(String relativePath) throws Exception {
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
