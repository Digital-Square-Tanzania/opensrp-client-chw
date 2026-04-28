package org.smartregister.chw.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.smartregister.chw.activity.HarmReductionUsedNeedlesAndSyringesCollectionDetailsActivity;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class HarmReductionUsedNeedlesCollectionConfigTest {

    @Test
    public void shouldUseRevisedCollectionColumnsInNacpEcClientFields() throws Exception {
        JSONObject file = new JSONObject(readText("src/nacp/assets/ec_client_fields.json"));
        JSONObject bindObject = findBindObject(file.getJSONArray("bindobjects"), "ec_harm_reduction_safety_box_collection");
        JSONArray columns = bindObject.getJSONArray("columns");

        List<String> columnNames = new ArrayList<>();
        for (int i = 0; i < columns.length(); i++) {
            columnNames.add(columns.getJSONObject(i).getString("column_name"));
        }

        Assert.assertTrue(columnNames.contains("collection_site_gps"));
        Assert.assertTrue(columnNames.contains("number_of_used_needles_and_syringes_collected"));
        Assert.assertTrue(columnNames.contains("issues_challenges_related_to_collection_of_used_needles_and_syringes"));
        Assert.assertFalse(columnNames.contains("other_collection"));
        Assert.assertFalse(columnNames.contains("fixed_bins"));
        Assert.assertFalse(columnNames.contains("total_safety_boxes_collected"));
        Assert.assertFalse(columnNames.contains("name_of_ow"));
    }

    @Test
    public void shouldShowOnlyRevisedFieldsInCollectionDetails() throws Exception {
        Field field = HarmReductionUsedNeedlesAndSyringesCollectionDetailsActivity.class.getDeclaredField("COLLECTION_FIELDS");
        field.setAccessible(true);

        String[] actual = (String[]) field.get(null);
        String[] expected = {
                "date_of_collection",
                "maskani_name",
                "collection_site_gps",
                "number_of_used_needles_and_syringes_collected",
                "issues_challenges_related_to_collection_of_used_needles_and_syringes"
        };

        Assert.assertArrayEquals(expected, actual);
    }

    private static JSONObject findBindObject(JSONArray bindObjects, String name) throws Exception {
        for (int i = 0; i < bindObjects.length(); i++) {
            JSONObject bindObject = bindObjects.getJSONObject(i);
            if (name.equals(bindObject.optString("name"))) {
                return bindObject;
            }
        }

        throw new AssertionError("Missing bind object: " + name);
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
