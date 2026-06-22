package org.smartregister.chw.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class IccmReferralFormUtils {

    private static final Set<String> NON_CHILD_EXCLUDED_PROBLEMS =
            new HashSet<>(Arrays.asList("sever_pneumonia", "diarrhea_with_signs_of_dehydration"));
    private static final Set<String> NON_CHILD_EXCLUDED_SERVICES =
            new HashSet<>(Arrays.asList("ors", "ors_zinc_co_pack"));

    private IccmReferralFormUtils() {
    }

    public static void updateUnifiedReferralFields(JSONArray fields,
                                                   JSONArray dangerSigns,
                                                   JSONArray preReferralManagement,
                                                   boolean isChild,
                                                   boolean isFemaleOfReproductiveAge) throws Exception {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            String fieldName = field.optString("name");
            if ("problem".equals(fieldName)) {
                JSONArray options = field.getJSONArray("options");
                filterOptions(options, "name", isChild, isFemaleOfReproductiveAge);
                applyUnifiedSelections(options, dangerSigns);
            } else if ("service_before_referral".equals(fieldName)) {
                JSONArray options = field.getJSONArray("options");
                filterServiceOptions(options, "name", isChild);
                applyUnifiedSelections(options, preReferralManagement);
            }
        }
    }

    public static void updateNativeReferralFields(JSONArray fields,
                                                  boolean isChild,
                                                  boolean isFemaleOfReproductiveAge) throws Exception {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            String fieldKey = field.optString("key");
            if ("problem".equals(fieldKey)) {
                filterOptions(field.getJSONArray("options"), "key", isChild, isFemaleOfReproductiveAge);
            } else if ("service_before_referral".equals(fieldKey)) {
                filterServiceOptions(field.getJSONArray("options"), "key", isChild);
            }
        }
    }

    private static void filterOptions(JSONArray options,
                                      String optionKeyName,
                                      boolean isChild,
                                      boolean isFemaleOfReproductiveAge) throws Exception {
        for (int i = options.length() - 1; i >= 0; i--) {
            JSONObject option = options.getJSONObject(i);
            String optionKey = option.optString(optionKeyName);

            if (!isChild && NON_CHILD_EXCLUDED_PROBLEMS.contains(optionKey)) {
                options.remove(i);
                continue;
            }

            if (!isFemaleOfReproductiveAge && "pregnant_client".equals(optionKey)) {
                options.remove(i);
                continue;
            }

            if (!isChild && "inability_to_drink_or_breastfeed".equals(optionKey)) {
                option.put("text", getAdultDrinkOptionLabel(option.optString("text")));
            }
        }
    }

    private static void filterServiceOptions(JSONArray options, String optionKeyName, boolean isChild) throws Exception {
        for (int i = options.length() - 1; i >= 0; i--) {
            JSONObject option = options.getJSONObject(i);
            if (!isChild && NON_CHILD_EXCLUDED_SERVICES.contains(option.optString(optionKeyName))) {
                options.remove(i);
            }
        }
    }

    private static void applyUnifiedSelections(JSONArray options, JSONArray selectedValues) throws Exception {
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            if (isKeyInJsonArray(selectedValues, option.getString("name"))) {
                JSONObject properties = new JSONObject();
                properties.put("checked", true);
                option.put("properties", properties);
            }
        }
    }

    public static boolean isKeyInJsonArray(JSONArray values, String optionName) throws Exception {
        for (int i = 0; i < values.length(); i++) {
            if (values.getString(i).equals(optionName)) {
                return true;
            }
        }
        return false;
    }

    public static String getAdultDrinkOptionLabel(String currentLabel) {
        if (currentLabel != null && currentLabel.toLowerCase().contains("kunyonya")) {
            return "Hawezi kunywa";
        }
        return "Inability to drink";
    }
}
