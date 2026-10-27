package org.smartregister.chw.util;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import org.smartregister.chw.R;
import org.smartregister.chw.due.DueServiceItem;
import org.smartregister.chw.due.DueServicesSnapshot;
import org.smartregister.chw.due.ModuleDueResult;

import java.text.NumberFormat;
import java.util.LinkedHashSet;
import java.util.Set;

/** Shared coverage and count presentation for the dashboard and its client register. */
public final class DueServicesUi {
    public interface OnStatusClick {
        void open(DueServiceItem.Status status);
    }

    private DueServicesUi() { }

    public static boolean hasAvailableData(DueServicesSnapshot snapshot) {
        if (snapshot == null) return false;
        for (ModuleDueResult result : snapshot.getResults()) {
            if (result.getCoverage() == ModuleDueResult.Coverage.COMPLETE
                    || result.getCoverage() == ModuleDueResult.Coverage.PARTIAL) return true;
        }
        return false;
    }

    public static boolean isComplete(DueServicesSnapshot snapshot) {
        if (snapshot == null || snapshot.getResults().isEmpty()) return false;
        for (ModuleDueResult result : snapshot.getResults()) {
            if (result.getCoverage() != ModuleDueResult.Coverage.COMPLETE) return false;
        }
        return true;
    }

    public static String countLabel(Context context, DueServicesSnapshot snapshot,
                                    DueServiceItem.Status status) {
        int count = snapshot == null ? 0 : snapshot.getClients(status).size();
        if (!isComplete(snapshot) && count == 0) {
            return context.getString(R.string.due_services_unknown_count);
        }
        String number = NumberFormat.getIntegerInstance(context.getResources().getConfiguration().locale)
                .format(count);
        return isComplete(snapshot) ? number : context.getString(R.string.due_services_partial_count, number);
    }

    public static String coverageText(Context context, DueServicesSnapshot snapshot) {
        if (snapshot == null) return context.getString(R.string.due_services_error);
        if (snapshot.getResults().isEmpty()) return context.getString(R.string.due_services_no_modules);
        Set<String> covered = new LinkedHashSet<>();
        Set<String> unsupported = new LinkedHashSet<>();
        boolean failed = false;
        for (ModuleDueResult result : snapshot.getResults()) {
            String module = DueServiceNavigation.moduleLabel(context, result.getModuleId());
            if (result.getCoverage() == ModuleDueResult.Coverage.UNSUPPORTED) unsupported.add(module);
            else if (result.getCoverage() == ModuleDueResult.Coverage.ERROR) failed = true;
            else {
                Set<String> services = new LinkedHashSet<>();
                for (String code : result.getSupportedServiceCodes()) {
                    services.add(DueServiceNavigation.serviceLabel(context, result.getModuleId(), code));
                }
                covered.add(module + (services.isEmpty() ? "" : ": " + android.text.TextUtils.join(", ", services)));
            }
        }
        if (covered.isEmpty()) {
            return failed ? context.getString(R.string.due_services_error)
                    : context.getString(R.string.due_services_unsupported, android.text.TextUtils.join(", ", unsupported));
        }
        String text = context.getString(R.string.due_services_coverage, android.text.TextUtils.join(", ", covered));
        if (!isComplete(snapshot)) text += "\n" + context.getString(R.string.due_services_partial);
        return text;
    }

    public static void bindSummary(View view, DueServicesSnapshot snapshot,
                                   OnStatusClick listener, Runnable retry) {
        View section = view.findViewById(R.id.dashboard_due_summary);
        section.setVisibility(snapshot != null && snapshot.getResults().isEmpty() ? View.GONE : View.VISIBLE);
        Context context = view.getContext();
        bindCard(view, R.id.dashboard_due_card, R.id.dashboard_due_count, R.string.due_services_due,
                snapshot, DueServiceItem.Status.DUE, listener);
        bindCard(view, R.id.dashboard_overdue_card, R.id.dashboard_overdue_count, R.string.due_services_overdue,
                snapshot, DueServiceItem.Status.OVERDUE, listener);
        TextView coverage = view.findViewById(R.id.dashboard_due_coverage);
        coverage.setText(coverageText(context, snapshot));
        boolean canRetry = snapshot == null;
        if (snapshot != null) for (ModuleDueResult result : snapshot.getResults()) {
            if (result.getCoverage() == ModuleDueResult.Coverage.ERROR
                    || result.getCoverage() == ModuleDueResult.Coverage.PARTIAL) canRetry = true;
        }
        coverage.setOnClickListener(canRetry ? v -> retry.run() : null);
        coverage.setClickable(canRetry);
        coverage.setFocusable(canRetry);
    }

    private static void bindCard(View view, int cardId, int countId, int labelId,
                                 DueServicesSnapshot snapshot, DueServiceItem.Status status,
                                 OnStatusClick listener) {
        Context context = view.getContext();
        String count = countLabel(context, snapshot, status);
        ((TextView) view.findViewById(countId)).setText(count);
        View card = view.findViewById(cardId);
        boolean available = hasAvailableData(snapshot);
        card.setEnabled(available);
        card.setFocusable(available);
        card.setContentDescription(available
                ? context.getString(R.string.due_services_open_count, context.getString(labelId), count)
                : context.getString(labelId) + ": " + count);
        card.setOnClickListener(available ? v -> listener.open(status) : null);
    }
}
