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
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.DateTime;
import org.smartregister.chw.R;
import org.smartregister.chw.mothermentor.fragment.BaseMotherMentorRegisterFragment;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorAgeUtils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.cursoradapter.RecyclerViewProvider;
import org.smartregister.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;
import org.smartregister.view.contract.SmartRegisterClients;
import org.smartregister.view.dialog.FilterOption;
import org.smartregister.view.dialog.ServiceModeOption;
import org.smartregister.view.dialog.SortOption;
import org.smartregister.view.viewholder.OnClickFormLauncher;

import timber.log.Timber;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

public class MotherMentorFollowUpRegisterProvider
        implements RecyclerViewProvider<MotherMentorFollowUpRegisterProvider.Holder> {
    private final Context context;
    private final LayoutInflater inflater;
    private final View.OnClickListener clickListener;
    private final View.OnClickListener paginationListener;

    public MotherMentorFollowUpRegisterProvider(
            Context context,
            View.OnClickListener paginationListener,
            View.OnClickListener clickListener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.paginationListener = paginationListener;
        this.clickListener = clickListener;
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, Holder holder) {
        CommonPersonObjectClient person = (CommonPersonObjectClient) client;
        try {
            String first = Utils.getValue(person.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true);
            String middle =
                    Utils.getValue(person.getColumnmaps(), DBConstants.KEY.MIDDLE_NAME, true);
            String last = Utils.getValue(person.getColumnmaps(), DBConstants.KEY.LAST_NAME, true);
            String fullName = (first + " " + middle + " " + last).trim().replaceAll("\\s+", " ");
            String age =
                    MotherMentorAgeUtils.formatAge(
                            context,
                            Utils.getValue(person.getColumnmaps(), DBConstants.KEY.DOB, false));
            holder.name.setText(TextUtils.isEmpty(age) ? fullName : fullName + ", " + age);

            String village =
                    Utils.getValue(person.getColumnmaps(), DBConstants.KEY.VILLAGE_TOWN, true);
            String id = Utils.getValue(person.getColumnmaps(), DBConstants.KEY.UNIQUE_ID, true);
            Integer weeksLost = weeksLost(person);
            String risk =
                    weeksLost == null
                            ? ""
                            : context.getString(
                                    weeksLost >= 8
                                            ? R.string.mothermentor_risk_high
                                            : R.string.mothermentor_risk_low);
            String metadata = join(risk, village, id);
            SpannableString styled = new SpannableString(metadata);
            if (!TextUtils.isEmpty(risk))
                styled.setSpan(
                        new ForegroundColorSpan(
                                context.getResources()
                                        .getColor(
                                                weeksLost >= 8
                                                        ? R.color.mothermentor_risk_high
                                                        : R.color.mothermentor_risk_low)),
                        0,
                        risk.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.metadata.setText(styled);

            Integer pregnancyWeek =
                    pregnancyWeek(Utils.getValue(person.getColumnmaps(), "pregnancy_edd", false));
            holder.pregnancy.setVisibility(pregnancyWeek == null ? View.GONE : View.VISIBLE);
            if (pregnancyWeek != null)
                holder.pregnancy.setText(
                        context.getString(R.string.mothermentor_pregnancy_week, pregnancyWeek));

            holder.row.setTag(person);
            holder.row.setTag(R.id.VIEW_ID, BaseMotherMentorRegisterFragment.CLICK_VIEW_NORMAL);
            holder.row.setOnClickListener(clickListener);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private Integer weeksLost(CommonPersonObjectClient person) {
        String raw = Utils.getValue(person.getColumnmaps(), "follow_up_weeks_lost", false);
        if (!TextUtils.isEmpty(raw))
            try {
                return (int) Double.parseDouble(raw);
            } catch (NumberFormatException ignored) {
            }
        String date =
                Utils.getValue(person.getColumnmaps(), "follow_up_expected_return_date", false);
        if (TextUtils.isEmpty(date)) return null;
        try {
            long days =
                    TimeUnit.MILLISECONDS.toDays(
                            System.currentTimeMillis() - new DateTime(date).getMillis());
            return Math.max(0, (int) days / 7);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer pregnancyWeek(String edd) {
        if (TextUtils.isEmpty(edd)) return null;
        try {
            int remaining =
                    org.joda.time.Weeks.weeksBetween(
                                    new DateTime().withTimeAtStartOfDay(),
                                    new DateTime(edd).withTimeAtStartOfDay())
                            .getWeeks();
            return Math.max(0, Math.min(42, 40 - remaining));
        } catch (Exception e) {
            return null;
        }
    }

    private String join(String... values) {
        StringBuilder b = new StringBuilder();
        for (String value : values)
            if (!TextUtils.isEmpty(value)) {
                if (b.length() > 0) b.append(" · ");
                b.append(value);
            }
        return b.toString();
    }

    @Override
    public Holder createViewHolder(ViewGroup parent) {
        return new Holder(
                inflater.inflate(R.layout.mothermentor_follow_up_register_list_row, parent, false));
    }

    @Override
    public RecyclerView.ViewHolder createFooterHolder(ViewGroup parent) {
        return new Footer(inflater.inflate(R.layout.smart_register_pagination, parent, false));
    }

    @Override
    public boolean isFooterViewHolder(RecyclerView.ViewHolder holder) {
        return holder instanceof Footer;
    }

    @Override
    public void getFooterView(
            RecyclerView.ViewHolder holder, int page, int pages, boolean next, boolean previous) {
        Footer f = (Footer) holder;
        f.info.setText(
                MessageFormat.format(
                        context.getString(org.smartregister.R.string.str_page_info), page, pages));
        f.next.setVisibility(next ? View.VISIBLE : View.INVISIBLE);
        f.previous.setVisibility(previous ? View.VISIBLE : View.INVISIBLE);
        f.next.setOnClickListener(paginationListener);
        f.previous.setOnClickListener(paginationListener);
    }

    @Override
    public SmartRegisterClients updateClients(
            FilterOption a, ServiceModeOption b, FilterOption c, SortOption d) {
        return null;
    }

    @Override
    public void onServiceModeSelected(ServiceModeOption option) {}

    @Override
    public OnClickFormLauncher newFormLauncher(String a, String b, String c) {
        return null;
    }

    @Override
    public LayoutInflater inflater() {
        return inflater;
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View row;
        final TextView name, metadata, pregnancy;

        Holder(View v) {
            super(v);
            row = v.findViewById(R.id.register_columns);
            name = v.findViewById(R.id.patient_name_age);
            metadata = v.findViewById(R.id.client_metadata);
            pregnancy = v.findViewById(R.id.pregnancy_week);
        }
    }

    static class Footer extends RecyclerView.ViewHolder {
        final TextView info;
        final Button next, previous;

        Footer(View v) {
            super(v);
            info = v.findViewById(org.smartregister.R.id.txt_page_info);
            next = v.findViewById(org.smartregister.R.id.btn_next_page);
            previous = v.findViewById(org.smartregister.R.id.btn_previous_page);
        }
    }
}
