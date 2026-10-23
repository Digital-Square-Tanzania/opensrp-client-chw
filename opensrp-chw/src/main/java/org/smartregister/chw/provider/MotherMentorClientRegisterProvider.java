package org.smartregister.chw.provider;

import android.content.Context;
import android.database.Cursor;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.LocalDate;
import org.joda.time.Years;
import org.joda.time.format.ISODateTimeFormat;
import org.smartregister.chw.R;
import org.smartregister.chw.model.MotherMentorClientRegisterModel;
import org.smartregister.chw.mothermentor.fragment.BaseMotherMentorRegisterFragment;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.cursoradapter.RecyclerViewProvider;
import org.smartregister.view.contract.SmartRegisterClient;
import org.smartregister.view.contract.SmartRegisterClients;
import org.smartregister.view.dialog.FilterOption;
import org.smartregister.view.dialog.ServiceModeOption;
import org.smartregister.view.dialog.SortOption;
import org.smartregister.view.viewholder.OnClickFormLauncher;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Presentation of primary Mama Kinara clients; follow-up registers keep their own provider. */
public class MotherMentorClientRegisterProvider
        implements RecyclerViewProvider<MotherMentorClientRegisterProvider.ClientViewHolder> {
    private final Context context;
    private final LayoutInflater inflater;
    private final View.OnClickListener paginationListener;
    private final View.OnClickListener rowListener;

    public MotherMentorClientRegisterProvider(
            Context context,
            View.OnClickListener paginationListener,
            View.OnClickListener rowListener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.paginationListener = paginationListener;
        this.rowListener = rowListener;
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, ClientViewHolder holder) {
        CommonPersonObjectClient person = (CommonPersonObjectClient) client;
        Map<String, String> columns = person.getColumnmaps();
        String status = statusLabel(value(columns, MotherMentorClientRegisterModel.CURRENT_STATUS));
        List<String> metadata = new ArrayList<>();
        metadata.add(status);
        addNonEmpty(metadata, sexLabel(value(columns, DBConstants.KEY.GENDER)));
        addNonEmpty(metadata, value(columns, DBConstants.KEY.VILLAGE_TOWN));
        SpannableString details = new SpannableString(TextUtils.join(" · ", metadata));
        details.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(context, R.color.mama_kinara_blue)),
                0,
                status.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        holder.name.setText(nameAndAge(columns));
        holder.metadata.setText(details);
        holder.ctcVisits.setText(
                context.getString(
                        R.string.mama_kinara_ctc_and_visits,
                        orMissing(value(columns, DBConstants.KEY.CTC_NUMBER)),
                        visitCount(
                                value(columns, MotherMentorClientRegisterModel.COMPLETED_VISITS))));
        holder.itemView.setTag(person);
        holder.itemView.setTag(
                org.smartregister.chw.mothermentor.R.id.VIEW_ID,
                BaseMotherMentorRegisterFragment.CLICK_VIEW_NORMAL);
        holder.itemView.setOnClickListener(rowListener);
        holder.itemView.setContentDescription(
                TextUtils.join(
                        ". ",
                        new CharSequence[] {
                            holder.name.getText(),
                            holder.metadata.getText(),
                            holder.ctcVisits.getText()
                        }));
    }

    private String nameAndAge(Map<String, String> columns) {
        List<String> names = new ArrayList<>();
        addNonEmpty(names, value(columns, DBConstants.KEY.FIRST_NAME));
        addNonEmpty(names, value(columns, DBConstants.KEY.MIDDLE_NAME));
        addNonEmpty(names, value(columns, DBConstants.KEY.LAST_NAME));
        String name =
                names.isEmpty()
                        ? context.getString(R.string.mama_kinara_unknown_client)
                        : TextUtils.join(" ", names);
        String dob = value(columns, DBConstants.KEY.DOB);
        if (!dob.isEmpty()) {
            try {
                LocalDate birthDate =
                        ISODateTimeFormat.dateOptionalTimeParser().parseLocalDate(dob);
                LocalDate today = LocalDate.now();
                if (!birthDate.isAfter(today)) {
                    return name + ", " + Years.yearsBetween(birthDate, today).getYears();
                }
            } catch (IllegalArgumentException ignored) {
                // Legacy records may have no usable date of birth. Preserve their name.
                return name;
            }
        }
        return name;
    }

    private String statusLabel(String status) {
        switch (status) {
            case "pregnant":
                return context.getString(R.string.mama_kinara_status_pregnant);
            case "breast_feeding":
                return context.getString(R.string.mama_kinara_status_breastfeeding);
            case "not_pregnant":
                return context.getString(R.string.mama_kinara_status_not_pregnant);
            default:
                return context.getString(R.string.mama_kinara_status_unknown);
        }
    }

    private String visitCount(String count) {
        try {
            long visits = Long.parseLong(count);
            return visits >= 0 ? Long.toString(visits) : missingValue();
        } catch (NumberFormatException ignored) {
            return missingValue();
        }
    }

    private String sexLabel(String gender) {
        if ("female".equalsIgnoreCase(gender)) {
            return context.getString(R.string.mama_kinara_sex_female);
        }
        if ("male".equalsIgnoreCase(gender)) {
            return context.getString(R.string.mama_kinara_sex_male);
        }
        return "";
    }

    private String orMissing(String value) {
        return value.isEmpty() ? missingValue() : value;
    }

    private String missingValue() {
        return context.getString(R.string.mama_kinara_missing_value);
    }

    private static String value(Map<String, String> columns, String key) {
        String value = columns == null ? null : columns.get(key);
        return value == null ? "" : value.trim();
    }

    private static void addNonEmpty(List<String> values, String value) {
        if (!value.isEmpty()) values.add(value);
    }

    @Override
    public void getFooterView(
            RecyclerView.ViewHolder holder,
            int page,
            int pages,
            boolean hasNext,
            boolean hasPrevious) {
        PaginationViewHolder footer = (PaginationViewHolder) holder;
        footer.pageInfo.setText(
                MessageFormat.format(
                        context.getString(org.smartregister.R.string.str_page_info), page, pages));
        footer.next.setVisibility(hasNext ? View.VISIBLE : View.INVISIBLE);
        footer.previous.setVisibility(hasPrevious ? View.VISIBLE : View.INVISIBLE);
        footer.next.setOnClickListener(paginationListener);
        footer.previous.setOnClickListener(paginationListener);
    }

    @Override
    public ClientViewHolder createViewHolder(ViewGroup parent) {
        return new ClientViewHolder(
                inflater.inflate(R.layout.mama_kinara_register_list_row, parent, false));
    }

    @Override
    public RecyclerView.ViewHolder createFooterHolder(ViewGroup parent) {
        return new PaginationViewHolder(
                inflater.inflate(R.layout.smart_register_pagination, parent, false));
    }

    @Override
    public boolean isFooterViewHolder(RecyclerView.ViewHolder holder) {
        return holder instanceof PaginationViewHolder;
    }

    @Override
    public LayoutInflater inflater() {
        return inflater;
    }

    @Override
    public SmartRegisterClients updateClients(
            FilterOption filter,
            ServiceModeOption mode,
            FilterOption extraFilter,
            SortOption sort) {
        return null;
    }

    @Override
    public void onServiceModeSelected(ServiceModeOption mode) {
        // The primary register has a single service mode.
    }

    @Override
    public OnClickFormLauncher newFormLauncher(String entityId, String formName, String metadata) {
        return null;
    }

    public static class ClientViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView metadata;
        final TextView ctcVisits;

        ClientViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.patient_name_age);
            metadata = view.findViewById(R.id.mama_kinara_client_metadata);
            ctcVisits = view.findViewById(R.id.mama_kinara_ctc_visits);
        }
    }

    private static class PaginationViewHolder extends RecyclerView.ViewHolder {
        final TextView pageInfo;
        final View next;
        final View previous;

        PaginationViewHolder(View view) {
            super(view);
            pageInfo = view.findViewById(org.smartregister.R.id.txt_page_info);
            next = view.findViewById(org.smartregister.R.id.btn_next_page);
            previous = view.findViewById(org.smartregister.R.id.btn_previous_page);
        }
    }
}
