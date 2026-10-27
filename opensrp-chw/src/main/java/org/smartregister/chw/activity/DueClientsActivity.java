package org.smartregister.chw.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.LocalDate;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.joda.time.format.DateTimeFormat;
import org.smartregister.chw.R;
import org.smartregister.chw.due.DueClient;
import org.smartregister.chw.due.DueServicesChangedEvent;
import org.smartregister.chw.due.DueServiceItem;
import org.smartregister.chw.due.DueServicesSnapshot;
import org.smartregister.chw.due.LandingDueServiceRepository;
import org.smartregister.chw.util.DueServiceNavigation;
import org.smartregister.chw.util.DueServicesUi;
import org.smartregister.domain.FetchStatus;
import org.smartregister.family.util.AppExecutors;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.view.activity.SecuredActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/** Read-only worklist derived by the same repository as the landing-page counts. */
public class DueClientsActivity extends SecuredActivity
        implements SyncStatusBroadcastReceiver.SyncStatusListener {
    public static final String EXTRA_STATUS = "due_service_status";
    private final AppExecutors executors = new AppExecutors();
    private final LandingDueServiceRepository repository = new LandingDueServiceRepository();
    private final ClientAdapter adapter = new ClientAdapter();
    private DueServicesSnapshot snapshot;
    private DueServiceItem.Status status = DueServiceItem.Status.DUE;
    private int requestVersion;
    private boolean started;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable serviceChangedRefresh = this::refresh;
    private final BroadcastReceiver dateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refresh(); }
    };

    public static void start(Context context, DueServiceItem.Status status) {
        context.startActivity(new Intent(context, DueClientsActivity.class)
                .putExtra(EXTRA_STATUS, status.name()));
    }

    @Override protected void onCreate(Bundle state) {
        String requested = state == null ? getIntent().getStringExtra(EXTRA_STATUS) : state.getString(EXTRA_STATUS);
        status = "OVERDUE".equals(requested) ? DueServiceItem.Status.OVERDUE : DueServiceItem.Status.DUE;
        super.onCreate(state);
    }

    @Override protected void onCreation() {
        // SecuredActivity skips this hook when the user has logged out.
        setContentView(R.layout.activity_due_clients);
        Toolbar toolbar = findViewById(R.id.due_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
        RecyclerView list = findViewById(R.id.due_register_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        RadioGroup filters = findViewById(R.id.due_register_filters);
        filters.check(status == DueServiceItem.Status.OVERDUE ? R.id.due_filter_overdue : R.id.due_filter_due);
        filters.setOnCheckedChangeListener((group, id) -> {
            status = id == R.id.due_filter_overdue ? DueServiceItem.Status.OVERDUE : DueServiceItem.Status.DUE;
            renderClients();
        });
        ((EditText) findViewById(R.id.due_register_search)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { renderClients(); }
            @Override public void afterTextChanged(Editable text) { }
        });
        findViewById(R.id.due_register_coverage).setOnClickListener(v -> refresh());
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        state.putString(EXTRA_STATUS, status.name());
        super.onSaveInstanceState(state);
    }

    @Override protected void onResumption() {
        // Authentication is checked by SecuredActivity before starting any data reads/listeners.
        started = true;
        SyncStatusBroadcastReceiver.getInstance().addSyncStatusListener(this);
        EventBus.getDefault().register(this);
        IntentFilter filter = new IntentFilter(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(dateReceiver, filter);
        refresh();
    }

    @Override protected void onPause() {
        refreshHandler.removeCallbacks(serviceChangedRefresh);
        if (started) {
            unregisterReceiver(dateReceiver);
            EventBus.getDefault().unregister(this);
            SyncStatusBroadcastReceiver.getInstance().removeSyncStatusListener(this);
        }
        started = false;
        requestVersion++;
        snapshot = null;
        adapter.clients.clear();
        adapter.notifyDataSetChanged();
        super.onPause();
    }

    private void refresh() {
        if (!started || context().IsUserLoggedOut()) return;
        int version = ++requestVersion;
        snapshot = null;
        adapter.clients.clear();
        adapter.notifyDataSetChanged();
        findViewById(R.id.due_register_empty).setVisibility(View.GONE);
        findViewById(R.id.due_register_loading).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.due_register_coverage)).setText(R.string.due_services_loading);
        bindFilterCounts();
        executors.diskIO().execute(() -> {
            DueServicesSnapshot loaded = null;
            try {
                loaded = repository.load(System.currentTimeMillis());
            } catch (Exception e) {
                Timber.e(e, "Unable to load due-service register");
            }
            DueServicesSnapshot result = loaded;
            executors.mainThread().execute(() -> {
                if (!started || version != requestVersion || isFinishing() || isDestroyed()
                        || context().IsUserLoggedOut()) return;
                snapshot = result;
                findViewById(R.id.due_register_loading).setVisibility(View.GONE);
                TextView coverage = findViewById(R.id.due_register_coverage);
                coverage.setText(DueServicesUi.coverageText(this, snapshot));
                coverage.setFocusable(!DueServicesUi.isComplete(snapshot));
                ((TextView) findViewById(R.id.due_register_date)).setText(snapshot == null ? ""
                        : getString(R.string.due_services_evaluated, date(snapshot.getDate())));
                bindFilterCounts();
                renderClients();
            });
        });
    }

    private void bindFilterCounts() {
        ((RadioButton) findViewById(R.id.due_filter_due)).setText(getString(R.string.due_services_filter_count,
                getString(R.string.due_services_due), DueServicesUi.countLabel(this, snapshot, DueServiceItem.Status.DUE)));
        ((RadioButton) findViewById(R.id.due_filter_overdue)).setText(getString(R.string.due_services_filter_count,
                getString(R.string.due_services_overdue), DueServicesUi.countLabel(this, snapshot, DueServiceItem.Status.OVERDUE)));
    }

    private void renderClients() {
        adapter.clients.clear();
        String search = ((EditText) findViewById(R.id.due_register_search)).getText().toString().trim().toLowerCase(Locale.ROOT);
        if (snapshot != null) for (DueClient client : snapshot.getClients(status)) {
            if (client.getName().toLowerCase(Locale.ROOT).contains(search)) adapter.clients.add(client);
        }
        adapter.notifyDataSetChanged();
        TextView empty = findViewById(R.id.due_register_empty);
        boolean loading = findViewById(R.id.due_register_loading).getVisibility() == View.VISIBLE;
        empty.setVisibility(adapter.clients.isEmpty() && !loading ? View.VISIBLE : View.GONE);
        if (!DueServicesUi.isComplete(snapshot)) empty.setText(DueServicesUi.coverageText(this, snapshot));
        else empty.setText(!search.isEmpty() ? R.string.due_services_empty
                : status == DueServiceItem.Status.OVERDUE ? R.string.due_services_overdue_empty : R.string.due_services_due_empty);
    }

    private String date(LocalDate date) {
        return date == null ? getString(R.string.due_services_unknown_count)
                : DateTimeFormat.mediumDate().withLocale(getResources().getConfiguration().locale).print(date);
    }

    private String serviceDetail(DueServiceItem item) {
        boolean overdue = item.getStatus() == DueServiceItem.Status.OVERDUE;
        return getString(overdue ? R.string.due_services_overdue_detail : R.string.due_services_due_detail,
                DueServiceNavigation.moduleLabel(this, item.getModuleId()), DueServiceNavigation.serviceLabel(this, item),
                date(overdue ? item.getOverdueFrom() : item.getDueDate()));
    }

    private void openClient(DueClient client) {
        if (context().IsUserLoggedOut()) return;
        if (client.getItems().size() == 1) {
            DueServiceNavigation.open(this, client.getItems().get(0));
            return;
        }
        String[] labels = new String[client.getItems().size()];
        for (int i = 0; i < labels.length; i++) labels[i] = serviceDetail(client.getItems().get(i));
        new AlertDialog.Builder(this).setTitle(R.string.due_services_choose_service)
                .setItems(labels, (dialog, which) -> DueServiceNavigation.open(this, client.getItems().get(which))).show();
    }

    @Override public void onSyncStart() { }
    @Override public void onSyncInProgress(FetchStatus fetchStatus) { }
    @Override public void onSyncComplete(FetchStatus fetchStatus) { executors.mainThread().execute(this::refresh); }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDueServicesChanged(DueServicesChangedEvent event) {
        refreshHandler.removeCallbacks(serviceChangedRefresh);
        refreshHandler.postDelayed(serviceChangedRefresh, 250);
    }

    private final class ClientAdapter extends RecyclerView.Adapter<ClientHolder> {
        private final List<DueClient> clients = new ArrayList<>();

        @NonNull @Override public ClientHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new ClientHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_due_client, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull ClientHolder holder, int position) {
            DueClient client = clients.get(position);
            holder.name.setText(client.getName());
            boolean overdue = client.getStatus() == DueServiceItem.Status.OVERDUE;
            holder.status.setText(overdue ? R.string.due_services_overdue : R.string.due_services_due);
            holder.status.setTextColor(ContextCompat.getColor(DueClientsActivity.this,
                    overdue ? R.color.all_clients_dashboard_amber : R.color.all_clients_dashboard_blue_text));
            List<String> details = new ArrayList<>();
            for (DueServiceItem item : client.getItems()) details.add(serviceDetail(item));
            holder.services.setText(android.text.TextUtils.join("\n\n", details));
            holder.itemView.setOnClickListener(v -> openClient(client));
        }

        @Override public int getItemCount() { return clients.size(); }
    }

    private static final class ClientHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView status;
        final TextView services;
        ClientHolder(View view) {
            super(view);
            name = view.findViewById(R.id.due_client_name);
            status = view.findViewById(R.id.due_client_status);
            services = view.findViewById(R.id.due_client_services);
        }
    }
}
