package org.smartregister.chw.fragment;

import android.os.Bundle;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.AllClientsRegisterActivity;
import org.smartregister.chw.core.model.NavigationModel;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.repository.AllClientsDashboardRepository;
import org.smartregister.chw.repository.AllClientsDashboardRepository.ModuleCount;
import org.smartregister.chw.repository.AllClientsDashboardRepository.RecentClient;
import org.smartregister.chw.repository.AllClientsDashboardRepository.Snapshot;
import org.smartregister.domain.FetchStatus;
import org.smartregister.family.util.AppExecutors;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.chw.core.job.CoreBasePncCloseJob;
import org.smartregister.chw.core.job.HomeVisitServiceJob;
import org.smartregister.chw.core.job.VaccineRecurringServiceJob;
import org.smartregister.job.ImageUploadServiceJob;
import org.smartregister.job.PullUniqueIdsServiceJob;
import org.smartregister.job.SyncServiceJob;
import org.smartregister.job.SyncTaskServiceJob;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

/** Local, read-only summary of the same client cohort as the All Clients register. */
public class AllClientsDashboardFragment extends Fragment implements SyncStatusBroadcastReceiver.SyncStatusListener {
    private final AppExecutors executors = new AppExecutors();
    private final AllClientsDashboardRepository repository = new AllClientsDashboardRepository();
    private int requestVersion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_clients_dashboard, container, false);
        view.findViewById(R.id.dashboard_view_clients).setOnClickListener(v -> host().showAllClients(false));
        view.findViewById(R.id.dashboard_add_client).setOnClickListener(v -> host().startRegistration());
        view.findViewById(R.id.dashboard_search_clients).setOnClickListener(v -> host().showAllClients(true));
        view.findViewById(R.id.dashboard_sync).setOnClickListener(v -> startSync());
        view.findViewById(R.id.dashboard_error).setOnClickListener(v -> refreshDashboard());
        return view;
    }

    private AllClientsRegisterActivity host() {
        return (AllClientsRegisterActivity) requireActivity();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.post(() -> {
            if (getView() == view && isAdded()) {
                host().bindDashboardToolbar();
            }
        });
    }

    private void startSync() {
        // Match Core NavigationPresenter's sync jobs without depending on a portrait-only drawer.
        CoreBasePncCloseJob.scheduleJobImmediately(CoreBasePncCloseJob.TAG);
        HomeVisitServiceJob.scheduleJobImmediately(HomeVisitServiceJob.TAG);
        VaccineRecurringServiceJob.scheduleJobImmediately(VaccineRecurringServiceJob.TAG);
        ImageUploadServiceJob.scheduleJobImmediately(ImageUploadServiceJob.TAG);
        SyncServiceJob.scheduleJobImmediately(SyncServiceJob.TAG);
        PullUniqueIdsServiceJob.scheduleJobImmediately(PullUniqueIdsServiceJob.TAG);
        SyncTaskServiceJob.scheduleJobImmediately(SyncTaskServiceJob.TAG);
        Toast.makeText(requireContext(), R.string.action_start_sync, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        SyncStatusBroadcastReceiver.getInstance().addSyncStatusListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDashboard();
    }

    @Override
    public void onStop() {
        SyncStatusBroadcastReceiver.getInstance().removeSyncStatusListener(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        requestVersion++;
        super.onDestroyView();
    }

    public void refreshDashboard() {
        View view = getView();
        if (view == null) {
            return;
        }
        int version = ++requestVersion;
        bindIntroduction(view);
        updateSyncState();
        view.findViewById(R.id.dashboard_loading).setVisibility(View.VISIBLE);
        view.findViewById(R.id.dashboard_error).setVisibility(View.GONE);
        executors.diskIO().execute(() -> {
            try {
                Snapshot snapshot = repository.load(System.currentTimeMillis());
                executors.mainThread().execute(() -> {
                    if (getView() == view && version == requestVersion) {
                        bindSnapshot(view, snapshot);
                        view.findViewById(R.id.dashboard_loading).setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                Timber.e(e, "Unable to load All Clients dashboard");
                executors.mainThread().execute(() -> {
                    if (getView() == view && version == requestVersion) {
                        view.findViewById(R.id.dashboard_loading).setVisibility(View.GONE);
                        view.findViewById(R.id.dashboard_error).setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void bindIntroduction(View view) {
        String name = NavigationModel.getInstance().getCurrentUser();
        text(view, R.id.dashboard_greeting, TextUtils.isEmpty(name)
                ? getString(R.string.all_clients_dashboard_greeting_default)
                : getString(R.string.all_clients_dashboard_greeting, name));
        Locale locale = getResources().getConfiguration().locale;
        String pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEE d MMMM yyyy");
        text(view, R.id.dashboard_date, new SimpleDateFormat(pattern, locale).format(new Date()));
        AllSharedPreferences preferences = org.smartregister.util.Utils.getAllSharedPreferences();
        String location = preferences.fetchUserLocalityName(preferences.fetchRegisteredANM());
        text(view, R.id.dashboard_location, location);
        view.findViewById(R.id.dashboard_location).setVisibility(TextUtils.isEmpty(location) ? View.GONE : View.VISIBLE);
    }

    private void bindSnapshot(View view, Snapshot snapshot) {
        NumberFormat numbers = NumberFormat.getIntegerInstance(getResources().getConfiguration().locale);
        text(view, R.id.dashboard_total, numbers.format(snapshot.getTotalClients()));
        text(view, R.id.dashboard_modules, numbers.format(snapshot.getActiveModules()));
        text(view, R.id.dashboard_unlinked, numbers.format(snapshot.getIndependentClients()));
        text(view, R.id.dashboard_new_week, numbers.format(snapshot.getNewThisWeek()));
        view.findViewById(R.id.dashboard_empty).setVisibility(snapshot.getTotalClients() == 0 ? View.VISIBLE : View.GONE);
        ViewGroup breakdown = view.findViewById(R.id.dashboard_breakdown);
        breakdown.removeAllViews();
        for (ModuleCount module : snapshot.getBreakdown()) {
            View row = getLayoutInflater().inflate(R.layout.item_all_clients_dashboard_module, breakdown, false);
            String label = moduleLabel(module.getRegisterType());
            text(row, R.id.dashboard_module_name, label);
            text(row, R.id.dashboard_module_count, numbers.format(module.getCount()));
            ProgressBar bar = row.findViewById(R.id.dashboard_module_progress);
            bar.setMax(Math.max(1, snapshot.getTotalClients()));
            bar.setProgress(module.getCount());
            if (AllClientsDashboardRepository.INDEPENDENT.equals(module.getRegisterType())) {
                bar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                        R.color.all_clients_dashboard_unlinked)));
                ((TextView) row.findViewById(R.id.dashboard_module_name)).setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.all_clients_dashboard_secondary));
            }
            row.setContentDescription(getString(R.string.all_clients_dashboard_module_count, label, module.getCount()));
            breakdown.addView(row);
        }
        ViewGroup recent = view.findViewById(R.id.dashboard_recent);
        recent.removeAllViews();
        if (snapshot.getRecentClients().isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText(R.string.all_clients_dashboard_recent_empty);
            recent.addView(empty);
        }
        for (RecentClient client : snapshot.getRecentClients()) {
            View row = getLayoutInflater().inflate(R.layout.item_all_clients_dashboard_recent, recent, false);
            text(row, R.id.dashboard_recent_name, getString(R.string.all_clients_dashboard_new_client, client.getName()));
            text(row, R.id.dashboard_recent_date, relativeDate(client.getRegisteredAt()));
            String details = moduleLabel(client.getRegisterType());
            if (!TextUtils.isEmpty(client.getAddress())) {
                details += " · " + client.getAddress();
            }
            text(row, R.id.dashboard_recent_details, details);
            recent.addView(row);
        }
    }

    private String relativeDate(long timestamp) {
        Calendar today = Calendar.getInstance();
        Calendar registered = Calendar.getInstance();
        registered.setTimeInMillis(timestamp);
        if (sameDay(today, registered)) {
            return getString(R.string.all_clients_dashboard_today);
        }
        today.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(today, registered)) {
            return getString(R.string.all_clients_dashboard_yesterday);
        }
        return DateFormat.getDateInstance(DateFormat.MEDIUM, getResources().getConfiguration().locale)
                .format(new Date(timestamp));
    }

    private boolean sameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private String moduleLabel(String type) {
        if (TextUtils.isEmpty(type) || CoreConstants.REGISTER_TYPE.INDEPENDENT.equals(type)) {
            return getString(R.string.all_clients_dashboard_unlinked_label);
        }
        switch (type) {
            case CoreConstants.REGISTER_TYPE.MOTHER_MENTOR: return getString(R.string.mother_mentor);
            case CoreConstants.REGISTER_TYPE.CHILD: return getString(R.string.menu_child);
            case CoreConstants.REGISTER_TYPE.ANC: return getString(R.string.menu_anc);
            case CoreConstants.REGISTER_TYPE.PNC: return getString(R.string.menu_pnc);
            case CoreConstants.REGISTER_TYPE.MALARIA: return getString(R.string.menu_malaria);
            case CoreConstants.REGISTER_TYPE.FAMILY_PLANNING: return getString(R.string.menu_family_planing);
            case CoreConstants.REGISTER_TYPE.TB: return getString(R.string.menu_tb);
            case CoreConstants.REGISTER_TYPE.CBHS: return getString(R.string.menu_hiv);
            case CoreConstants.REGISTER_TYPE.CECAP: return getString(R.string.cecap);
            case CoreConstants.REGISTER_TYPE.AYSRH: return getString(R.string.asrh);
            case CoreConstants.REGISTER_TYPE.TBLEPROSY: return getString(R.string.menu_tbleprosy);
            case CoreConstants.REGISTER_TYPE.HARM_REDUCTION: return getString(R.string.menu_harm_reduction);
            case "NCD": return getString(R.string.nav_menu_ncd);
            case "HIVST": return getString(R.string.menu_hivst);
            default: return type;
        }
    }

    private void text(View view, int id, String value) {
        ((TextView) view.findViewById(id)).setText(value);
    }

    private void updateSyncState() {
        if (getView() != null) {
            boolean syncing = SyncStatusBroadcastReceiver.getInstance().isSyncing();
            TextView sync = getView().findViewById(R.id.dashboard_sync);
            sync.setEnabled(!syncing);
            sync.setText(syncing ? R.string.all_clients_dashboard_syncing : R.string.all_clients_dashboard_sync);
        }
    }

    @Override
    public void onSyncStart() {
        executors.mainThread().execute(this::updateSyncState);
    }

    @Override
    public void onSyncInProgress(FetchStatus fetchStatus) {
        executors.mainThread().execute(this::updateSyncState);
    }

    @Override
    public void onSyncComplete(FetchStatus fetchStatus) {
        executors.mainThread().execute(() -> {
            if (isAdded() && getView() != null) {
                refreshDashboard();
                if (fetchStatus == FetchStatus.fetchedFailed || fetchStatus == FetchStatus.noConnection) {
                    Toast.makeText(requireContext(), R.string.all_clients_dashboard_sync_failed, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
