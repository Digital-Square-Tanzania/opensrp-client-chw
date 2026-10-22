package org.smartregister.chw.util;

import android.content.Context;
import android.content.res.Configuration;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/** Reads history questions and coded answers from the same localized forms used for visits. */
public final class MotherMentorHistoryMapper {
    private static final String HISTORY_PREFIX = "mothermentor_history_";
    private static final Pattern BRACKET_GROUP = Pattern.compile("\\[([^\\]]*)\\]");
    private static final Set<String> HOUSEHOLD_FIELDS =
            new HashSet<>(
                    Arrays.asList(
                            DBConstants.KEY.PURPOSE_OF_VISIT,
                            DBConstants.KEY.OTHER_PURPOSE,
                            DBConstants.KEY.PARTICIPANTS,
                            DBConstants.KEY.TOPICS_TAUGHT,
                            DBConstants.KEY.OTHER_TOPIC,
                            DBConstants.KEY.HOUSEHOLD_MEMBERS_FOUND,
                            DBConstants.KEY.COUNSELLED_MALE,
                            DBConstants.KEY.COUNSELLED_FEMALE,
                            DBConstants.KEY.COUNSELLED_TOTAL,
                            DBConstants.KEY.HOUSEHOLD_REFERRAL_GIVEN,
                            DBConstants.KEY.HOUSEHOLD_REFERRAL_SERVICES,
                            DBConstants.KEY.HOUSEHOLD_REFERRAL_OTHER));
    private static final Set<String> INTERNAL_FIELDS =
            new HashSet<>(
                    Arrays.asList(
                            "visit_number", "visit_completion_state", "visit_id", "visit_type",
                            "entity_id", "base_entity_id", "relational_id", "mother_base_entity_id",
                            "start", "end", "today", "deviceid", "subscriberid", "simserial",
                            "encounter_date", "encounter_location", "encounter_type", "gps",
                            "latitude", "longitude", "location_id", "provider_id", "team_id",
                            DBConstants.KEY.MOTHERMENTOR_REPORTING_VERSION,
                            DBConstants.KEY.PRIMARY_MOTHERMENTOR_CLIENT,
                            DBConstants.KEY.IIT_ENROLLED,
                            DBConstants.KEY.PARTNER_ENROLLED,
                            DBConstants.KEY.CHILD_EID_ENROLLED,
                            DBConstants.KEY.ART_INITIATED_BEFORE_VISIT,
                            DBConstants.KEY.MOTHERMENTOR_VISIT_DATE));

    private final Context context;
    private final Context englishContext;
    private final Context swahiliContext;
    private final boolean swahili;
    private final Map<String, JSONObject> forms = new HashMap<>();

    public MotherMentorHistoryMapper(Context context) {
        this.context = context;
        englishContext = localeContext(Locale.ENGLISH);
        swahiliContext = localeContext(new Locale("sw"));
        swahili =
                "sw".equals(
                        context.getResources().getConfiguration().locale.getLanguage());
    }

    public List<Row> map(Visit visit) {
        if (visit == null || visit.getVisitDetails() == null) return Collections.emptyList();
        Map<String, List<VisitDetail>> recorded = canonicalDetails(visit.getVisitDetails());
        Set<String> ignored = new HashSet<>(INTERNAL_FIELDS);
        LinkedHashMap<String, Field> fields = new LinkedHashMap<>();
        for (String form : workflowForms(visit, recorded)) addForm(fields, ignored, form);

        List<Row> rows = new ArrayList<>();
        Row comments = null;
        for (Field field : fields.values()) {
            List<String> values = values(field, recorded.get(field.key));
            if (values.isEmpty()) continue;
            Row row = new Row(field.key, field.label, values, field.multiple);
            if (DBConstants.KEY.COMMENTS.equals(field.key)) comments = row;
            else rows.add(row);
        }

        // Historical fields outside the current form remain readable when an explicit history
        // label exists. Do not turn arbitrary internal observation IDs into user-facing headings.
        for (String key : new TreeSet<>(recorded.keySet())) {
            if (fields.containsKey(key) || ignored.contains(key)) continue;
            String label = resource(HISTORY_PREFIX + resourceName(key));
            if (label == null) continue;
            Field field = new Field(key, label, false);
            List<String> values = values(field, recorded.get(key));
            if (!values.isEmpty()) rows.add(new Row(key, label, values, false));
        }
        if (comments != null) rows.add(comments);
        return rows;
    }

    public static boolean isHouseholdVisit(Visit visit) {
        if (visit == null) return false;
        if (Constants.EVENT_TYPE.MOTHERMENTOR_CONTACT_VISIT.equals(visit.getVisitType())) return true;
        if (visit.getVisitDetails() == null) return false;
        for (String key : visit.getVisitDetails().keySet()) {
            if (HOUSEHOLD_FIELDS.contains(canonicalKey(key))) return true;
        }
        return false;
    }

    private List<String> workflowForms(Visit visit, Map<String, List<VisitDetail>> recorded) {
        String type = visit.getVisitType();
        if (isHouseholdVisit(visit)) {
            return Arrays.asList(
                    "mothermentor_household_visit_purpose",
                    "mother_mentor_household_members_who_received_counselling",
                    "mother_mentor_household_visit_topics_taught",
                    "mothermentor_household_visit_comment");
        }
        // Follow-ups were originally saved using the generic services encounter. Only infer a
        // workflow when the normal attendance question is absent and category-specific data exists.
        boolean legacy =
                Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES.equals(type)
                        && !recorded.containsKey(DBConstants.KEY.ATTENDANCE_TYPE);
        if (MotherMentorFollowUpType.PARTNER.getEventType().equals(type)
                || (legacy
                        && hasAny(
                                recorded,
                                DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY,
                                DBConstants.KEY.PARTNER_FACILITY_ARRIVAL_DATE,
                                DBConstants.KEY.PARTNER_NON_ATTENDANCE_REASON,
                                DBConstants.KEY.PARTNER_EXPECTED_FACILITY_VISIT_DATE))) {
            return Arrays.asList(
                    "mothermentor_partner_follow_up_status",
                    "mothermentor_partner_non_attendance_reason",
                    "mothermentor_partner_arrival",
                    "mothermentor_partner_follow_up_comment");
        }
        if (MotherMentorFollowUpType.CHILD_EID.getEventType().equals(type)
                || (legacy
                        && hasAny(
                                recorded,
                                DBConstants.KEY.CHILD_EID_TEST_TYPE,
                                DBConstants.KEY.CHILD_EID_ARRIVED_AT_FACILITY,
                                DBConstants.KEY.CHILD_EID_NON_ATTENDANCE_REASON,
                                DBConstants.KEY.CHILD_EID_EXPECTED_FACILITY_VISIT_DATE))) {
            return Arrays.asList(
                    "mothermentor_child_eid_follow_up_status",
                    "mothermentor_child_eid_test_type",
                    "mothermentor_child_eid_non_attendance_reason",
                    "mothermentor_child_eid_arrival",
                    "mothermentor_child_eid_follow_up_comment");
        }
        if (Constants.EVENT_TYPE.MOTHER_MENTOR_FOLLOW_UP_VISIT.equals(type)
                || (legacy
                        && hasAny(
                                recorded,
                                DBConstants.KEY.SERVICE_INTERRUPTION_REASON,
                                DBConstants.KEY.SERVICE_INTERRUPTION_REASON_OTHER,
                                DBConstants.KEY.RETURNED_TO_FACILITY,
                                DBConstants.KEY.FACILITY_RETURN_DATE))) {
            return Arrays.asList(
                    "mothermentor_follow_up_status",
                    "mothermentor_service_interruption_reason",
                    "mothermentor_return_to_facility",
                    "mothermentor_follow_up_comment");
        }
        return Arrays.asList(
                "mothermentor_visit_type",
                "mothermentor_visit_group_linkage",
                "mothermentor_visit_education_provided",
                "mothermentor_visit_referal",
                "mothermentor_next_visit_date",
                "mothermentor_visit_comment");
    }

    private boolean hasAny(Map<String, List<VisitDetail>> recorded, String... keys) {
        for (String key : keys) if (recorded.containsKey(key)) return true;
        return false;
    }

    private void addForm(Map<String, Field> fields, Set<String> ignored, String name) {
        JSONObject english = form("json.form", name);
        JSONObject translated = form("json.form-sw", name);
        JSONObject selected = swahili && translated != null ? translated : english;
        if (selected == null) return;
        Map<String, JSONObject> englishFields = formFields(english);
        Map<String, JSONObject> translatedFields = formFields(translated);
        for (Map.Entry<String, JSONObject> entry : formFields(selected).entrySet()) {
            String key = entry.getKey();
            JSONObject definition = entry.getValue();
            String type = definition.optString("type");
            if ("hidden".equals(type) || "toaster_notes".equals(type)) {
                ignored.add(key);
                continue;
            }
            String label = definition.optString("label", definition.optString("hint"));
            if (StringUtils.isBlank(label)) label = resource(HISTORY_PREFIX + resourceName(key));
            if (StringUtils.isBlank(label)) continue;
            Field field = new Field(key, label, "check_box".equals(type));
            field.addOptions(englishFields.get(key), definition);
            field.addOptions(translatedFields.get(key), definition);
            addHistoricalAliases(field, definition);
            fields.put(key, field);
        }
    }

    private void addHistoricalAliases(Field field, JSONObject definition) {
        JSONArray options = definition.optJSONArray("options");
        if (options == null) return;
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (option != null) addHistoricalAlias(field, option.optString("key"));
        }
        // This was a recorded attendance status before it was removed from the visit form.
        if (DBConstants.KEY.FOLLOW_UP_STATUS.equals(field.key)) {
            addHistoricalAlias(field, "lost_to_follow_up");
        }
    }

    private void addHistoricalAlias(Field field, String key) {
        String name = HISTORY_PREFIX + resourceName(key);
        String label = firstAnswer(field.option(key), resource(name));
        if (StringUtils.isBlank(label)) return;
        field.options.put(optionName(key), label);
        for (Context locale : Arrays.asList(englishContext, swahiliContext)) {
            String oldLabel = resource(locale, name);
            if (StringUtils.isNotBlank(oldLabel)) field.options.put(optionName(oldLabel), label);
        }
    }

    private JSONObject form(String directory, String name) {
        String path = directory + "/" + name + ".json";
        if (!forms.containsKey(path)) {
            try (InputStream input = context.getAssets().open(path);
                    Scanner scanner = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
                forms.put(path, new JSONObject(scanner.next()));
            } catch (Exception e) {
                Timber.w(e, "Unable to read Mother Mentor history form %s", path);
                forms.put(path, null);
            }
        }
        return forms.get(path);
    }

    private Map<String, JSONObject> formFields(JSONObject form) {
        Map<String, JSONObject> fields = new LinkedHashMap<>();
        if (form == null) return fields;
        for (int step = 1; step <= form.optInt("count", 1); step++) {
            JSONObject page = form.optJSONObject("step" + step);
            JSONArray items = page == null ? null : page.optJSONArray("fields");
            if (items == null) continue;
            for (int i = 0; i < items.length(); i++) {
                JSONObject field = items.optJSONObject(i);
                if (field != null) fields.put(field.optString("key"), field);
            }
        }
        return fields;
    }

    private Map<String, List<VisitDetail>> canonicalDetails(Map<String, List<VisitDetail>> details) {
        Map<String, List<VisitDetail>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<VisitDetail>> entry : details.entrySet()) {
            String key = canonicalKey(entry.getKey());
            if (!result.containsKey(key)
                    || (hasAnswer(entry.getValue())
                            && (!hasAnswer(result.get(key)) || key.equals(entry.getKey())))) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    private boolean hasAnswer(List<VisitDetail> details) {
        if (details == null) return false;
        for (VisitDetail detail : details) {
            if (detail != null
                    && (StringUtils.isNotBlank(detail.getDetails())
                            || StringUtils.isNotBlank(detail.getHumanReadable()))) return true;
        }
        return false;
    }

    private static String canonicalKey(String key) {
        return key == null ? "" : key.replaceFirst("^step\\d+:", "");
    }

    private List<String> values(Field field, List<VisitDetail> details) {
        if (details == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (VisitDetail detail : details) {
            if (detail == null) continue;
            String raw = scalar(detail.getDetails());
            String readable = scalar(detail.getHumanReadable());
            if (field.multiple) {
                List<String> selections =
                        selections(
                                StringUtils.isNotBlank(raw)
                                        ? detail.getDetails()
                                        : detail.getHumanReadable());
                List<String> savedLabels = selections(detail.getHumanReadable());
                for (int i = 0; i < selections.size(); i++) {
                    String selection = selections.get(i);
                    String translated = field.option(selection);
                    String saved = savedLabels.size() == selections.size() ? savedLabels.get(i) : "";
                    if (translated == null) translated = field.option(saved);
                    if (translated == null) {
                        String legacy = resource(HISTORY_PREFIX + resourceName(selection));
                        translated = legacy != null ? legacy : firstAnswer(saved, selection);
                    }
                    if (StringUtils.isNotBlank(translated)) result.add(translated);
                }
            } else if (!field.options.isEmpty()) {
                String translated = field.option(raw);
                if (translated == null) translated = field.option(readable);
                if (translated == null) {
                    String legacy = resource(HISTORY_PREFIX + resourceName(raw));
                    translated = legacy != null ? legacy : firstAnswer(readable, raw);
                }
                if (StringUtils.isNotBlank(translated)) result.add(translated);
            } else {
                String text = firstAnswer(detail.getDetails(), detail.getHumanReadable());
                if (StringUtils.isNotBlank(text)) result.add(text);
            }
        }
        return field.multiple ? new ArrayList<>(new LinkedHashSet<>(result)) : result;
    }

    private String firstAnswer(String first, String second) {
        return StringUtils.isNotBlank(first) ? first : second;
    }

    private String scalar(String value) {
        if (StringUtils.isBlank(value)) return "";
        String trimmed = value.trim();
        try {
            JSONArray array = new JSONArray(trimmed);
            if (array.length() == 0) return "";
            if (array.length() == 1 && !(array.opt(0) instanceof JSONArray)) {
                return array.optString(0).trim();
            }
        } catch (Exception ignored) {
            // Plain free text, including commas and literal brackets, is not an array.
        }
        return trimmed;
    }

    private List<String> selections(String value) {
        if (StringUtils.isBlank(value)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        String trimmed = value.trim();
        try {
            JSONTokener tokens = new JSONTokener(trimmed);
            Object parsed = tokens.nextValue();
            if (parsed instanceof JSONArray && tokens.nextClean() == '\0') {
                JSONArray array = (JSONArray) parsed;
                for (int i = 0; i < array.length(); i++) {
                    Object item = array.opt(i);
                    if (item instanceof JSONArray) result.addAll(selections(item.toString()));
                    else if (item != null && item != JSONObject.NULL && !item.toString().trim().isEmpty()) {
                        result.add(item.toString().trim());
                    }
                }
                return result;
            }
        } catch (Exception ignored) {
            // Historical visits also store checkbox values as bracketed or plain CSV.
        }
        Matcher groups = BRACKET_GROUP.matcher(trimmed);
        if (groups.find()
                && groups.replaceAll("").replace(",", "").trim().isEmpty()) {
            groups.reset();
            while (groups.find()) result.addAll(selections(groups.group(1)));
            return result;
        }
        for (String item : trimmed.split(",")) {
            String clean = item.trim();
            if (!clean.isEmpty()) result.add(clean);
        }
        return result;
    }

    private String resource(String name) {
        return resource(context, name);
    }

    private String resource(Context locale, String name) {
        int id = locale.getResources().getIdentifier(name, "string", locale.getPackageName());
        return id == 0 ? null : locale.getString(id);
    }

    private Context localeContext(Locale locale) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    private static String resourceName(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");
    }

    private static String optionName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static final class Field {
        private final String key;
        private final String label;
        private final boolean multiple;
        private final Map<String, String> options = new HashMap<>();

        private Field(String key, String label, boolean multiple) {
            this.key = key;
            this.label = label;
            this.multiple = multiple;
        }

        private void addOptions(JSONObject aliases, JSONObject current) {
            if (aliases == null) return;
            JSONArray aliasOptions = aliases.optJSONArray("options");
            JSONArray currentOptions = current.optJSONArray("options");
            if (aliasOptions == null || currentOptions == null) return;
            Map<String, String> labels = new HashMap<>();
            for (int i = 0; i < currentOptions.length(); i++) {
                JSONObject option = currentOptions.optJSONObject(i);
                if (option != null) labels.put(option.optString("key"), option.optString("text"));
            }
            for (int i = 0; i < aliasOptions.length(); i++) {
                JSONObject option = aliasOptions.optJSONObject(i);
                if (option == null) continue;
                String key = option.optString("key");
                String label = labels.get(key);
                if (label != null) {
                    options.put(optionName(key), label);
                    options.put(optionName(option.optString("text")), label);
                }
            }
        }

        private String option(String value) {
            return options.get(optionName(value));
        }
    }

    public static final class Row {
        private final String key;
        private final String label;
        private final List<String> values;
        private final boolean multiValue;

        public Row(String key, String label, List<String> values, boolean multiValue) {
            this.key = key;
            this.label = label;
            this.values = Collections.unmodifiableList(new ArrayList<>(values));
            this.multiValue = multiValue;
        }

        public String getKey() { return key; }

        public String getLabel() { return label; }

        public List<String> getValues() { return values; }

        public boolean isMultiValue() { return multiValue; }
    }
}
