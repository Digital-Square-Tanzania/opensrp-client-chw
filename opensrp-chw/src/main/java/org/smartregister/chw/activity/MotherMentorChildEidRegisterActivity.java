package org.smartregister.chw.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.chw.model.ChwAllClientsRegisterModel;
import org.smartregister.chw.presenter.MotherMentorChildEidRegisterPresenter;
import org.smartregister.chw.repository.MotherMentorChildEidRepository;
import org.smartregister.chw.repository.MotherMentorChildEidRepository.Person;
import org.smartregister.chw.util.MotherMentorChildEidFormUtils;
import org.smartregister.domain.FetchStatus;
import org.smartregister.family.util.AppExecutors;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.opd.contract.OpdRegisterActivityContract;
import org.smartregister.opd.pojo.RegisterParams;
import org.smartregister.opd.utils.OpdJsonFormUtils;
import org.smartregister.util.LangUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

/** Uses the same client-selection and registration routes as TB contacts. */
public class MotherMentorChildEidRegisterActivity extends AppCompatActivity implements
        OpdRegisterActivityContract.View, MotherMentorChildEidRegisterPresenter.Callback {
    public static final String MOTHER_ID = "mother_base_entity_id";
    private static final String CHILD_ID = "selected_child_id";
    private final AppExecutors executors = new AppExecutors();
    private MotherMentorChildEidRepository repository;
    private MotherMentorChildEidRegisterPresenter registerPresenter;
    private String motherId;
    private String childId;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.mothermentor_register_child_eid);
        motherId = state == null ? getIntent().getStringExtra(MOTHER_ID) : state.getString(MOTHER_ID);
        childId = state == null ? null : state.getString(CHILD_ID);
        repository = new MotherMentorChildEidRepository(ChwApplication.getInstance().getRepository().getWritableDatabase());
        registerPresenter = new MotherMentorChildEidRegisterPresenter(this, new ChwAllClientsRegisterModel(this), this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> finish());
        findViewById(R.id.child_from_household).setOnClickListener(view -> loadCandidates(true));
        findViewById(R.id.child_from_all_clients).setOnClickListener(view -> loadCandidates(false));
        findViewById(R.id.child_register_new).setOnClickListener(view -> registerNewClient());
        if (motherId == null) finish();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LangUtils.setAppLocale(base, LangUtils.getLanguage(base.getApplicationContext())));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle state) {
        state.putString(MOTHER_ID, motherId);
        state.putString(CHILD_ID, childId);
        super.onSaveInstanceState(state);
    }

    private void loadCandidates(boolean householdOnly) {
        showProgressDialog(R.string.mothermentor_link_child_eid);
        executors.diskIO().execute(() -> {
            try {
                Person mother = repository.findMother(motherId);
                if (mother == null) throw new IllegalStateException("Mother is no longer enrolled");
                List<Person> candidates = repository.candidates(motherId, mother.familyId(), householdOnly);
                executors.mainThread().execute(() -> {
                    hideProgressDialog();
                    if (isFinishing() || isDestroyed()) return;
                    if (candidates.isEmpty()) displayToast(R.string.mothermentor_child_eid_none);
                    else showCandidates(candidates, householdOnly);
                });
            } catch (Exception e) {
                fail(e, R.string.mothermentor_child_eid_unavailable);
            }
        });
    }

    private void showCandidates(List<Person> candidates, boolean householdOnly) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_tbleprosy_contact_selector, null, false);
        EditText search = content.findViewById(R.id.search_input);
        TextView empty = content.findViewById(R.id.empty_view);
        RecyclerView list = content.findViewById(R.id.contact_list);
        search.setHint(R.string.mothermentor_child_eid_search);
        empty.setText(R.string.mothermentor_child_eid_none);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(householdOnly ? R.string.mothermentor_child_eid_household : R.string.mothermentor_child_eid_all_clients)
                .setView(content).setNegativeButton(android.R.string.cancel, null).create();
        ChildAdapter adapter = new ChildAdapter(candidates, person -> {
            dialog.dismiss();
            openChildForm(person.id());
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable text) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                adapter.filter(text.toString());
                empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });
        dialog.show();
    }

    private void registerNewClient() {
        String location = Utils.getAllSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
        if (location == null || location.trim().isEmpty()) {
            displayToast(org.smartregister.chw.core.R.string.error_unable_to_start_form);
            return;
        }
        childId = null;
        try {
            registerPresenter.startForm(CoreConstants.JSON_FORM.getAllClientRegistrationForm(), null, null, location);
        } catch (Exception e) {
            fail(e, org.smartregister.chw.core.R.string.error_unable_to_start_form);
        }
    }

    private void openChildForm(String id) {
        showProgressDialog(R.string.mothermentor_link_child_eid);
        executors.diskIO().execute(() -> {
            try {
                JSONObject form = FormUtils.getFormUtils().getFormJson(MotherMentorChildEidFormUtils.FORM);
                repository.prepareForm(form, motherId, id);
                form.getJSONObject("metadata").put("encounter_location",
                        Utils.getAllSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID));
                executors.mainThread().execute(() -> {
                    hideProgressDialog();
                    if (isFinishing() || isDestroyed()) return;
                    childId = id;
                    startActivityForResult(Utils.formActivityIntent(this, form.toString()), JsonFormUtils.REQUEST_CODE_GET_JSON);
                });
            } catch (Exception e) {
                fail(e, R.string.mothermentor_child_eid_unavailable);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != JsonFormUtils.REQUEST_CODE_GET_JSON || resultCode != RESULT_OK || data == null) return;
        try {
            String json = data.getStringExtra("json");
            JSONObject form = new JSONObject(json);
            String eventType = form.optString("encounter_type");
            if (CoreConstants.EventType.FAMILY_REGISTRATION.equals(eventType)) {
                RegisterParams params = new RegisterParams();
                params.setEditMode(false);
                params.setFormTag(OpdJsonFormUtils.formTag(Utils.getAllSharedPreferences()));
                showProgressDialog(org.smartregister.chw.core.R.string.saving_dialog_title);
                registerPresenter.saveForm(json, params);
            } else if (MotherMentorChildEidFormUtils.EVENT.equals(eventType) && childId != null) {
                showProgressDialog(org.smartregister.chw.core.R.string.saving_dialog_title);
                final String selectedId = childId;
                executors.diskIO().execute(() -> {
                    try {
                        repository.save(form, motherId, selectedId, Utils.getAllSharedPreferences());
                        executors.mainThread().execute(() -> {
                            hideProgressDialog();
                            if (isFinishing() || isDestroyed()) return;
                            displayToast(R.string.mothermentor_child_eid_linked);
                            setResult(RESULT_OK);
                            finish();
                        });
                    } catch (Exception e) {
                        fail(e, org.smartregister.chw.core.R.string.error_unable_to_save_form);
                    }
                });
            }
        } catch (Exception e) {
            fail(e, org.smartregister.chw.core.R.string.error_unable_to_save_form);
        }
    }

    @Override public void onChildRegistered(String id) {
        hideProgressDialog();
        if (isFinishing() || isDestroyed()) return;
        if (id == null) displayToast(org.smartregister.chw.core.R.string.error_unable_to_save_form);
        else openChildForm(id); // Repository verifies that registration was actually projected.
    }

    private void fail(Exception error, int message) {
        Timber.e(error, "Unable to link Mother Mentor child");
        executors.mainThread().execute(() -> {
            hideProgressDialog();
            if (!isFinishing() && !isDestroyed()) displayToast(message);
        });
    }

    @Override public Context getContext() { return this; }
    @Override public void displaySyncNotification() { }
    @Override public void displayToast(int id) { displayToast(getString(id)); }
    @Override public void displayToast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    @Override public void displayShortToast(int id) { Toast.makeText(this, id, Toast.LENGTH_SHORT).show(); }
    @Override public void refreshList(FetchStatus status) { }
    @Override public void updateInitialsText(String initials) { }
    @Override public OpdRegisterActivityContract.Presenter presenter() { return registerPresenter; }
    @Override public void startFormActivity(String name, String id, String metadata, HashMap<String, String> values, String table) { }
    @Override public void startFormActivity(JSONObject form) { startFormActivity(form, null); }
    @Override public void startFormActivity(@NonNull JSONObject form, @Nullable HashMap<String, String> extras) {
        try {
            MotherMentorChildEidFormUtils.configureNewClientForm(form, getString(R.string.mothermentor_child_eid_age_error));
        } catch (Exception e) {
            fail(e, org.smartregister.chw.core.R.string.error_unable_to_start_form);
            return;
        }
        Intent intent = CoreJsonFormUtils.getJsonIntent(this, form, org.smartregister.family.util.Utils.metadata().familyMemberFormActivity);
        if (extras != null) for (Map.Entry<String, String> entry : extras.entrySet()) intent.putExtra(entry.getKey(), entry.getValue());
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }
    @Override public void showProgressDialog(int title) {
        if (isFinishing() || isDestroyed()) return;
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setTitle(title);
        progressDialog.setMessage(getString(org.smartregister.chw.core.R.string.please_wait_message));
        progressDialog.show();
    }
    @Override public void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }
    @Override protected void onDestroy() {
        hideProgressDialog();
        super.onDestroy();
    }

    private interface Selection { void select(Person person); }
    private static class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.Holder> {
        private final List<Person> all;
        private final List<Person> filtered = new ArrayList<>();
        private final Selection selection;
        ChildAdapter(List<Person> people, Selection selection) {
            this.all = people;
            this.selection = selection;
            filtered.addAll(people);
        }
        void filter(String query) {
            String search = query.trim().toLowerCase(Locale.ROOT);
            filtered.clear();
            for (Person person : all) {
                if (person.name().toLowerCase(Locale.ROOT).contains(search)
                        || (person.uniqueId() != null && person.uniqueId().toLowerCase(Locale.ROOT).contains(search))) filtered.add(person);
            }
            notifyDataSetChanged();
        }
        @Override @NonNull public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tbleprosy_contact_row, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            Person person = filtered.get(position);
            holder.name.setText(person.name());
            holder.details.setText(person.uniqueId());
            holder.details.setVisibility(person.uniqueId() == null ? View.GONE : View.VISIBLE);
            holder.itemView.setOnClickListener(view -> selection.select(person));
        }
        @Override public int getItemCount() { return filtered.size(); }
        static class Holder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView details;
            Holder(View view) {
                super(view);
                name = view.findViewById(R.id.contact_name);
                details = view.findViewById(R.id.contact_details);
            }
        }
    }
}
