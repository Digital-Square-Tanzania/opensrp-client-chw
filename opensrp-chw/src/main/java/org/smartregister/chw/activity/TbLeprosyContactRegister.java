package org.smartregister.chw.activity;

import static org.smartregister.util.JsonFormUtils.ENTITY_ID;
import static org.smartregister.util.JsonFormUtils.ENCOUNTER_LOCATION;
import static org.smartregister.util.JsonFormUtils.FIELDS;
import static org.smartregister.util.JsonFormUtils.VALUE;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.chw.tbleprosy.util.Constants;
import org.smartregister.chw.tbleprosy.util.TbLeprosyJsonFormUtils;
import org.smartregister.chw.tbleprosy.util.TbLeprosyUtil;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.repository.AllSharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public class TbLeprosyContactRegister extends AppCompatActivity {

    private static final String CONTACT_EVENT_TYPE = "TBLeprosy Contacts";
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CONTACT = 1;

    private String indexBaseEntityId;
    private String familyBaseEntityId;

    private final Map<String, ContactPerson> familyMembersCache = new HashMap<>();
    private final Map<String, ContactPerson> otherClientsCache = new HashMap<>();
    private final Set<String> registeredScreeningIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.smartregister.chw.tbleprosy.R.layout.tbleprosy_register_contact);

        indexBaseEntityId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID);
        familyBaseEntityId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.FAMILY_BASE_ENTITY_ID);

        setupToolbar();
        initializeOptions();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(org.smartregister.chw.tbleprosy.R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initializeOptions() {
        RelativeLayout familyContactsOption = findViewById(org.smartregister.chw.tbleprosy.R.id.existingClient);
        RelativeLayout allClientsOption = findViewById(org.smartregister.chw.tbleprosy.R.id.newClient);

        familyContactsOption.setOnClickListener(view -> showFamilyContactsDialog());
        allClientsOption.setOnClickListener(view -> showAllClientsDialog());
    }

    private void showFamilyContactsDialog() {
        List<ContactPerson> familyCandidates = getNewContactCandidates();
        if (familyCandidates.isEmpty()) {
            Toast.makeText(this, getString(org.smartregister.chw.R.string.tbleprosy_no_contacts_available), Toast.LENGTH_SHORT).show();
            return;
        }

        ExistingContactOptions options = new ExistingContactOptions(familyCandidates, Collections.emptyList());
        showContactsDialog(options, R.string.tbleprosy_contact_family_dialog_title);
    }

    private void showAllClientsDialog() {
        ExistingContactOptions options = getExistingContactOptions();
        if (options.isEmpty()) {
            Toast.makeText(this, getString(org.smartregister.chw.R.string.tbleprosy_no_contacts_available), Toast.LENGTH_SHORT).show();
            return;
        }
        showContactsDialog(options, R.string.tbleprosy_contact_all_clients_dialog_title);
    }

    private void showContactsDialog(ExistingContactOptions options, int titleRes) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tbleprosy_contact_selector, null, false);
        EditText searchInput = dialogView.findViewById(R.id.search_input);
        RecyclerView contactsRecycler = dialogView.findViewById(R.id.contact_list);
        TextView emptyView = dialogView.findViewById(R.id.empty_view);

        ContactSelectionAdapter adapter = new ContactSelectionAdapter(options.familyMembers, options.otherClients, null);

        contactsRecycler.setLayoutManager(new LinearLayoutManager(this));
        contactsRecycler.setAdapter(adapter);
        updateEmptyViewVisibility(adapter, emptyView);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        adapter.setOnContactSelectedListener(contact -> {
            dialog.dismiss();
            startContactForm(contact.getBaseEntityId());
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // intentionally blank
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s == null ? "" : s.toString());
                updateEmptyViewVisibility(adapter, emptyView);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // intentionally blank
            }
        });

        dialog.show();
    }

    private void updateEmptyViewVisibility(ContactSelectionAdapter adapter, TextView emptyView) {
        if (emptyView != null) {
            emptyView.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private List<ContactPerson> getNewContactCandidates() {
        List<ContactPerson> allMembers = loadFamilyMembers();
        if (allMembers.isEmpty()) {
            return Collections.emptyList();
        }


        Set<String> screeningIds = loadRegisteredScreeningIds();

        List<ContactPerson> candidates = new ArrayList<>();
        for (ContactPerson person : allMembers) {
            String baseEntityId = person.getBaseEntityId();
            if (screeningIds.contains(baseEntityId)) {
                continue;
            }
            if (StringUtils.equalsIgnoreCase(indexBaseEntityId, baseEntityId)) {
                continue;
            }
            candidates.add(person);
        }
        return candidates;
    }

    private ExistingContactOptions getExistingContactOptions() {
        List<ContactPerson> family = new ArrayList<>();
        for (ContactPerson person : loadFamilyMembers()) {
            String baseEntityId = person.getBaseEntityId();
            if (StringUtils.equalsIgnoreCase(indexBaseEntityId, baseEntityId)) {
                continue;
            }
            family.add(person);
        }

        List<ContactPerson> others = new ArrayList<>();
        for (ContactPerson person : loadOtherClients()) {
            String baseEntityId = person.getBaseEntityId();
            if (familyMembersCache.containsKey(baseEntityId)) {
                continue;
            }
            if (StringUtils.equalsIgnoreCase(indexBaseEntityId, baseEntityId)) {
                continue;
            }
            others.add(person);
        }

        return new ExistingContactOptions(family, others);
    }

    private List<ContactPerson> loadFamilyMembers() {
        if (familyMembersCache.isEmpty()) {
            if (StringUtils.isBlank(familyBaseEntityId)) {
                Timber.w("Family base entity id not supplied for TB contact registration");
                return Collections.emptyList();
            }

            try {
                CommonRepository repository = ChwApplication.getInstance()
                        .getContext()
                        .commonrepository(CoreConstants.TABLE_NAME.FAMILY_MEMBER);

                List<CommonPersonObject> memberObjects = repository.findByRelational_IDs(familyBaseEntityId);
                if (memberObjects != null) {
                    for (CommonPersonObject memberObject : memberObjects) {
                        Map<String, String> columnMaps = memberObject.getColumnmaps();
                        String baseEntityId = org.smartregister.util.Utils.getValue(columnMaps, DBConstants.KEY.BASE_ENTITY_ID, false);

                        if (StringUtils.isBlank(baseEntityId)) {
                            continue;
                        }

                        String isClosed = org.smartregister.util.Utils.getValue(columnMaps, "is_closed", false);
                        if (StringUtils.equals("1", isClosed)) {
                            continue;
                        }

                        String displayName = buildDisplayName(columnMaps, baseEntityId);
                        String ageString = buildAgeDisplay(columnMaps);
                        String uniqueId = org.smartregister.util.Utils.getValue(columnMaps, DBConstants.KEY.UNIQUE_ID, true);

                        familyMembersCache.put(baseEntityId, new ContactPerson(baseEntityId, displayName, ageString, uniqueId));
                    }
                }
            } catch (Exception e) {
                Timber.e(e, "Error loading family members for TB contact registration");
            }
        }

        return new ArrayList<>(familyMembersCache.values());
    }

    private List<ContactPerson> loadOtherClients() {
        if (!otherClientsCache.isEmpty()) {
            return new ArrayList<>(otherClientsCache.values());
        }

        Cursor cursor = null;

        try {
            SQLiteDatabase readableDatabase = ChwApplication.getInstance()
                    .getRepository()
                    .getReadableDatabase();

            if (readableDatabase == null) {
                return Collections.emptyList();
            }

            String sql = "SELECT m.base_entity_id, m.first_name, m.middle_name, m.last_name, m.dob, m.unique_id " +
                    "FROM ec_family_member m " +
                    "LEFT JOIN ec_tbleprosy_screening s ON s.base_entity_id = m.base_entity_id AND s.is_closed = 0 " +
                    "LEFT JOIN ec_tbleprosy_contacts c ON c.base_entity_id = m.base_entity_id AND c.is_closed = 0 " +
                    "WHERE m.is_closed = 0 AND s.base_entity_id IS NULL AND c.base_entity_id IS NULL";

            cursor = readableDatabase.rawQuery(sql, null);
            while (cursor != null && cursor.moveToNext()) {
                String baseEntityId = cursor.getString(0);
                if (StringUtils.isBlank(baseEntityId)) {
                    continue;
                }

                if (familyMembersCache.containsKey(baseEntityId)) {
                    continue;
                }

                String firstName = cursor.getString(1);
                String middleName = cursor.getString(2);
                String lastName = cursor.getString(3);
                String dob = cursor.getString(4);
                String uniqueId = cursor.getString(5);

                Map<String, String> columnMaps = new HashMap<>();
                columnMaps.put(DBConstants.KEY.FIRST_NAME, firstName);
                columnMaps.put(DBConstants.KEY.MIDDLE_NAME, middleName);
                columnMaps.put(DBConstants.KEY.LAST_NAME, lastName);
                columnMaps.put(DBConstants.KEY.DOB, dob);

                String displayName = buildDisplayName(columnMaps, baseEntityId);
                String ageString = buildAgeDisplay(columnMaps);

                otherClientsCache.put(baseEntityId, new ContactPerson(baseEntityId, displayName, ageString, uniqueId));
            }
        } catch (Exception e) {
            Timber.w(e, "Unable to load other potential TB contacts");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return new ArrayList<>(otherClientsCache.values());
    }

    private Set<String> loadRegisteredScreeningIds() {
        if (registeredScreeningIds.isEmpty()) {
            Cursor cursor = null;

            try {
                SQLiteDatabase readableDatabase = ChwApplication.getInstance()
                        .getRepository()
                        .getReadableDatabase();

                if (readableDatabase == null) {
                    return registeredScreeningIds;
                }

                cursor = readableDatabase.rawQuery(
                        "SELECT DISTINCT base_entity_id FROM ec_tbleprosy_screening WHERE is_closed = 0",
                        null);

                while (cursor != null && cursor.moveToNext()) {
                    String baseEntityId = cursor.getString(0);
                    if (StringUtils.isNotBlank(baseEntityId)) {
                        registeredScreeningIds.add(baseEntityId);
                    }
                }
            } catch (Exception e) {
                Timber.w(e, "Unable to load TB screening enrolments");
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return registeredScreeningIds;
    }

    private String buildDisplayName(Map<String, String> columnMaps, String fallback) {
        String firstName = org.smartregister.util.Utils.getValue(columnMaps, DBConstants.KEY.FIRST_NAME, true);
        String middleName = org.smartregister.util.Utils.getValue(columnMaps, DBConstants.KEY.MIDDLE_NAME, true);
        String lastName = org.smartregister.util.Utils.getValue(columnMaps, DBConstants.KEY.LAST_NAME, true);

        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotBlank(firstName)) {
            builder.append(firstName.trim());
        }
        if (StringUtils.isNotBlank(middleName)) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(middleName.trim());
        }
        if (StringUtils.isNotBlank(lastName)) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(lastName.trim());
        }

        String name = builder.toString();
        return StringUtils.isNotBlank(name) ? name : fallback;
    }

    private String buildAgeDisplay(Map<String, String> columnMaps) {
        String dob = org.smartregister.util.Utils.getValue(columnMaps, DBConstants.KEY.DOB, false);
        if (StringUtils.isBlank(dob)) {
            return "";
        }
        try {
            DateTime birthDate = new DateTime(dob);
            int age = Math.max(0, new Period(birthDate, DateTime.now()).getYears());
            return age > 0 ? String.format(Locale.getDefault(), "%d", age) : "0";
        } catch (Exception e) {
            Timber.w(e, "Unable to parse date of birth for TB contact");
            return "";
        }
    }

    private void startContactForm(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            Toast.makeText(this, getString(org.smartregister.chw.R.string.tbleprosy_no_contacts_available), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject jsonForm = FormUtils.getFormUtils().getFormJson(Constants.FORMS.TBLEPROSY_CONTACT_REGISTRATION);
            if (jsonForm == null) {
                Toast.makeText(this, org.smartregister.chw.core.R.string.error_unable_to_start_form, Toast.LENGTH_SHORT).show();
                return;
            }

            jsonForm.put(ENTITY_ID, baseEntityId);
            jsonForm.put(Constants.JSON_FORM_EXTRA.EVENT_TYPE, CONTACT_EVENT_TYPE);

            AllSharedPreferences sharedPreferences = Utils.getAllSharedPreferences();
            JSONObject metadata = jsonForm.optJSONObject(TbLeprosyJsonFormUtils.METADATA);
            if (metadata != null) {
                metadata.put(ENCOUNTER_LOCATION, TbLeprosyJsonFormUtils.locationId(sharedPreferences));
            }

            injectHiddenField(jsonForm, Constants.STEP_ONE, DBConstants.KEY.RELATIONAL_ID, familyBaseEntityId);
            injectHiddenField(jsonForm, Constants.STEP_ONE, "index_client_id", indexBaseEntityId);

            Intent intent = Utils.formActivityIntent(this, jsonForm.toString());
            startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
        } catch (JSONException e) {
            Timber.e(e, "Unable to prepare TB contact registration form");
            Toast.makeText(this, org.smartregister.chw.core.R.string.error_unable_to_start_form, Toast.LENGTH_SHORT).show();
        }
    }

    private void injectHiddenField(JSONObject form, String step, String fieldKey, String fieldValue) throws JSONException {
        if (StringUtils.isBlank(fieldValue)) {
            return;
        }

        JSONObject stepObject = form.optJSONObject(step);
        if (stepObject == null) {
            stepObject = new JSONObject();
            form.put(step, stepObject);
        }

        JSONArray fields = stepObject.optJSONArray(FIELDS);
        if (fields == null) {
            fields = new JSONArray();
            stepObject.put(FIELDS, fields);
        }

        JSONObject existing = findFieldByKey(fields, fieldKey);
        if (existing == null) {
            existing = new JSONObject();
            existing.put("type", "hidden");
            existing.put("openmrs_entity_parent", "");
            existing.put("openmrs_entity", "concept");
            existing.put("openmrs_entity_id", fieldKey);
            existing.put("key", fieldKey);
            fields.put(existing);
        }

        existing.put(VALUE, fieldValue);
    }

    private JSONObject findFieldByKey(JSONArray fields, String key) {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject fieldObject = fields.optJSONObject(i);
            if (fieldObject != null && key.equals(fieldObject.optString("key"))) {
                return fieldObject;
            }
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON && resultCode == RESULT_OK && data != null) {
            String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
            if (StringUtils.isBlank(jsonString)) {
                return;
            }

            try {
                TbLeprosyUtil.saveFormEvent(jsonString);
                Toast.makeText(this, getString(org.smartregister.chw.R.string.tbleprosy_contact_registration_success), Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) {
                Timber.e(e, "Error saving TB contact registration");
                Toast.makeText(this, org.smartregister.chw.core.R.string.error_unable_to_save_form, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static class ContactPerson {
        private final String baseEntityId;
        private final String displayName;
        private final String ageDisplay;
        private final String uniqueId;

        ContactPerson(String baseEntityId, String displayName, String ageDisplay, String uniqueId) {
            this.baseEntityId = baseEntityId;
            this.displayName = displayName;
            this.ageDisplay = ageDisplay;
            this.uniqueId = uniqueId;
        }

        String getBaseEntityId() {
            return baseEntityId;
        }

        String getDisplayName() {
            return displayName;
        }

        String getAgeDisplay() {
            return ageDisplay;
        }

        String getUniqueId() {
            return uniqueId;
        }

        CharSequence getDisplayLabel() {
            String details = getSubtitle();
            if (StringUtils.isNotBlank(details)) {
                return displayName + " (" + details + ")";
            }
            return displayName;
        }

        String getSubtitle() {
            StringBuilder builder = new StringBuilder();
            if (StringUtils.isNotBlank(uniqueId)) {
                builder.append(uniqueId);
            }
            if (StringUtils.isNotBlank(ageDisplay)) {
                if (builder.length() > 0) {
                    builder.append(" · ");
                }
                builder.append(ageDisplay);
            }
            return builder.toString();
        }

        boolean matches(String query) {
            if (StringUtils.isBlank(query)) {
                return true;
            }
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            return (displayName != null && displayName.toLowerCase(Locale.getDefault()).contains(lowerQuery))
                    || (uniqueId != null && uniqueId.toLowerCase(Locale.getDefault()).contains(lowerQuery))
                    || (baseEntityId != null && baseEntityId.toLowerCase(Locale.getDefault()).contains(lowerQuery));
        }
    }

    private static class ExistingContactOptions {
        private final List<ContactPerson> familyMembers;
        private final List<ContactPerson> otherClients;

        ExistingContactOptions(List<ContactPerson> familyMembers, List<ContactPerson> otherClients) {
            this.familyMembers = familyMembers;
            this.otherClients = otherClients;
        }

        boolean isEmpty() {
            return (familyMembers == null || familyMembers.isEmpty())
                    && (otherClients == null || otherClients.isEmpty());
        }
    }

    private interface OnContactSelectedListener {
        void onContactSelected(ContactPerson contact);
    }

    private static class ContactRow {
        private final boolean header;
        private final String headerTitle;
        private final ContactPerson contact;

        private ContactRow(boolean header, String headerTitle, ContactPerson contact) {
            this.header = header;
            this.headerTitle = headerTitle;
            this.contact = contact;
        }

        static ContactRow header(String title) {
            return new ContactRow(true, title, null);
        }

        static ContactRow contact(ContactPerson contact) {
            return new ContactRow(false, null, contact);
        }

        boolean isHeader() {
            return header;
        }

        String getHeaderTitle() {
            return headerTitle;
        }

        ContactPerson getContact() {
            return contact;
        }
    }

    private class ContactSelectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final List<ContactPerson> family;
        private final List<ContactPerson> others;
        private final List<ContactRow> rows = new ArrayList<>();
        private OnContactSelectedListener onContactSelectedListener;

        ContactSelectionAdapter(List<ContactPerson> family, List<ContactPerson> others, OnContactSelectedListener listener) {
            this.family = family == null ? Collections.emptyList() : family;
            this.others = others == null ? Collections.emptyList() : others;
            this.onContactSelectedListener = listener;
            rebuildRows("");
        }

        void setOnContactSelectedListener(OnContactSelectedListener listener) {
            this.onContactSelectedListener = listener;
        }

        void filter(String query) {
            rebuildRows(query);
            notifyDataSetChanged();
        }

        private void rebuildRows(String query) {
            rows.clear();
            String trimmedQuery = query == null ? "" : query.trim();

            List<ContactPerson> filteredFamily = filterContacts(family, trimmedQuery);
            if (!filteredFamily.isEmpty()) {
                rows.add(ContactRow.header(getString(R.string.tbleprosy_contact_family_header)));
                for (ContactPerson person : filteredFamily) {
                    rows.add(ContactRow.contact(person));
                }
            }

            List<ContactPerson> filteredOthers = filterContacts(others, trimmedQuery);
            if (!filteredOthers.isEmpty()) {
                rows.add(ContactRow.header(getString(R.string.tbleprosy_contact_other_header)));
                for (ContactPerson person : filteredOthers) {
                    rows.add(ContactRow.contact(person));
                }
            }
        }

        private List<ContactPerson> filterContacts(List<ContactPerson> source, String query) {
            if (StringUtils.isBlank(query)) {
                return new ArrayList<>(source);
            }

            List<ContactPerson> results = new ArrayList<>();
            for (ContactPerson person : source) {
                if (person.matches(query)) {
                    results.add(person);
                }
            }
            return results;
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position).isHeader() ? TYPE_HEADER : TYPE_CONTACT;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                View view = inflater.inflate(R.layout.item_tbleprosy_contact_header, parent, false);
                return new HeaderViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.item_tbleprosy_contact_row, parent, false);
                return new ContactViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ContactRow row = rows.get(position);
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).bind(row.getHeaderTitle());
            } else if (holder instanceof ContactViewHolder) {
                ((ContactViewHolder) holder).bind(row.getContact());
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        class HeaderViewHolder extends RecyclerView.ViewHolder {
            private final TextView titleView;

            HeaderViewHolder(@NonNull View itemView) {
                super(itemView);
                titleView = itemView.findViewById(R.id.header_title);
            }

            void bind(String title) {
                titleView.setText(title);
            }
        }

        class ContactViewHolder extends RecyclerView.ViewHolder {
            private final TextView nameView;
            private final TextView subtitleView;

            ContactViewHolder(@NonNull View itemView) {
                super(itemView);
                nameView = itemView.findViewById(R.id.contact_name);
                subtitleView = itemView.findViewById(R.id.contact_details);
            }

            void bind(ContactPerson contact) {
                nameView.setText(contact.getDisplayName());
                String subtitle = contact.getSubtitle();
                subtitleView.setText(subtitle);
                subtitleView.setVisibility(StringUtils.isBlank(subtitle) ? View.GONE : View.VISIBLE);

                itemView.setOnClickListener(v -> {
                    if (onContactSelectedListener != null) {
                        onContactSelectedListener.onContactSelected(contact);
                    }
                });
            }
        }
    }
}
