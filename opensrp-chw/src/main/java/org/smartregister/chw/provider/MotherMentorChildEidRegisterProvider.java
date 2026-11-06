package org.smartregister.chw.provider;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joda.time.LocalDate;
import org.smartregister.chw.R;
import org.smartregister.chw.mothermentor.fragment.BaseMotherMentorRegisterFragment;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorChildEidLabels;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.view.contract.SmartRegisterClient;

import java.util.Map;

/** Child tracing uses its own metadata while retaining the existing register click/paging flow. */
public class MotherMentorChildEidRegisterProvider extends MotherMentorFollowUpRegisterProvider {
    private final Context context;
    private final View.OnClickListener clickListener;

    public MotherMentorChildEidRegisterProvider(Context context,
            View.OnClickListener paginationListener, View.OnClickListener clickListener) {
        super(context, paginationListener, clickListener);
        this.context = context;
        this.clickListener = clickListener;
    }

    @Override
    public Holder createViewHolder(ViewGroup parent) {
        return new ChildHolder(LayoutInflater.from(context).inflate(
                org.smartregister.chw.mothermentor.R.layout.mothermentor_child_eid_follow_up_list_row,
                parent, false));
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, Holder baseHolder) {
        ChildHolder holder = (ChildHolder) baseHolder;
        CommonPersonObjectClient person = (CommonPersonObjectClient) client;
        Map<String, String> values = person.getColumnmaps();
        String name = (value(values, DBConstants.KEY.FIRST_NAME) + " "
                + value(values, DBConstants.KEY.MIDDLE_NAME) + " "
                + value(values, DBConstants.KEY.LAST_NAME)).trim().replaceAll("\\s+", " ");
        String dob = value(values, DBConstants.KEY.CHILD_DOB);
        if (dob.isEmpty()) dob = value(values, DBConstants.KEY.DOB);
        String age = MotherMentorChildEidLabels.age(context, dob, LocalDate.now());
        holder.name.setText(age.isEmpty() ? name : name + ", " + age);

        boolean closed = "1".equals(value(values, DBConstants.KEY.MTOTO_IS_CLOSED));
        String state = value(values, DBConstants.KEY.MTOTO_RUFAA_STATE);
        String referredTest = value(values, DBConstants.KEY.MTOTO_RUFAA_TEST);
        if (referredTest.isEmpty()) referredTest = value(values, DBConstants.KEY.MTOTO_MISSED_TEST);
        String status = closed
                ? MotherMentorChildEidLabels.closedReason(context, value(values, DBConstants.KEY.MTOTO_STATUS))
                : MotherMentorChildEidLabels.referral(context, state, referredTest);
        holder.metadata.setText(join(status, value(values, DBConstants.KEY.VILLAGE_TOWN)));
        holder.closedTag.setVisibility(closed ? View.VISIBLE : View.GONE);
        holder.row.setAlpha(closed ? 0.55f : 1f);

        String missed = MotherMentorChildEidLabels.missed(context,
                value(values, DBConstants.KEY.MTOTO_MISSED_TEST),
                value(values, DBConstants.KEY.MTOTO_MISSED_DUE));
        holder.missed.setText(missed);
        holder.missed.setVisibility(missed.isEmpty() ? View.GONE : View.VISIBLE);
        int visits = -1;
        try {
            visits = Integer.parseInt(value(values, DBConstants.KEY.MTOTO_VISIT_COUNT));
        } catch (NumberFormatException ignored) {
            // An unavailable count is omitted rather than inventing a visit total.
        }
        holder.summary.setText(MotherMentorChildEidLabels.summary(context,
                value(values, DBConstants.KEY.MOTHER_NAME),
                value(values, DBConstants.KEY.HEI_NUMBER), visits));
        holder.row.setContentDescription(join(holder.name.getText().toString(),
                holder.metadata.getText().toString(), missed, holder.summary.getText().toString()));
        holder.row.setTag(person);
        holder.row.setTag(R.id.VIEW_ID, BaseMotherMentorRegisterFragment.CLICK_VIEW_NORMAL);
        holder.row.setEnabled(true);
        holder.row.setOnClickListener(clickListener);
    }

    private static String value(Map<String, String> values, String key) {
        String value = values == null ? null : values.get(key);
        return value == null ? "" : value.trim();
    }

    private static String join(String... values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (TextUtils.isEmpty(value)) continue;
            if (result.length() > 0) result.append(" · ");
            result.append(value);
        }
        return result.toString();
    }

    static class ChildHolder extends Holder {
        final TextView missed;
        final TextView summary;
        final View closedTag;

        ChildHolder(View view) {
            super(view);
            missed = view.findViewById(org.smartregister.chw.mothermentor.R.id.mothermentor_eid_missed_due);
            summary = view.findViewById(org.smartregister.chw.mothermentor.R.id.mothermentor_eid_mother_summary);
            closedTag = view.findViewById(org.smartregister.chw.mothermentor.R.id.mothermentor_eid_closed_tag);
        }
    }
}
