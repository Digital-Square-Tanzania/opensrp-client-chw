package org.smartregister.chw.adapter;


import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.HpsMobilizationSessionDetailsActivity;
import org.smartregister.chw.hps.domain.HpsMobilizationSessionModel;

import java.util.List;

import timber.log.Timber;

public class HpsMobilizationSessionsRegisterAdapter extends RecyclerView.Adapter<HpsMobilizationSessionsRegisterAdapter.HpsMobilizationViewHolder> {
    private static final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);

    private final Context context;

    private final List<HpsMobilizationSessionModel> mobilizationSessionModels;


    public HpsMobilizationSessionsRegisterAdapter(List<HpsMobilizationSessionModel> mobilizationSessionModels, Context context) {
        this.mobilizationSessionModels = mobilizationSessionModels;
        this.context = context;
    }

    private static void evaluateView(TextView tv, Context context, String stringValue) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder.append(context.getString(R.string.cecap_health_education_provided), boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append("\n");

        String[] stringValueArray;
        if (stringValue.contains(",")) {
            stringValueArray = stringValue.substring(1, stringValue.length() - 1).split(",");
            for (String value : stringValueArray) {
                spannableStringBuilder.append(getStringResource(context, "hps_", value.trim()) + "\n", new BulletSpan(10), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else if (stringValue.charAt(0) == '[' && stringValue.charAt(stringValue.length() - 1) == ']') {
            spannableStringBuilder.append(getStringResource(context, "hps_", stringValue.substring(1, stringValue.length() - 1))).append("\n");
        } else {
            spannableStringBuilder.append(getStringResource(context, "hps_", stringValue)).append("\n");
        }
        tv.setText(spannableStringBuilder);
    }

    private static String getStringResource(Context context, String prefix, String resourceName) {
        int resourceId = context.getResources().
                getIdentifier(prefix + resourceName.trim(), "string", context.getPackageName());
        try {
            return context.getString(resourceId);
        } catch (Exception e) {
            Timber.e(e);
            return resourceName;
        }
    }

    @NonNull
    @Override
    public HpsMobilizationViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View followupLayout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.sbc_mobilization_session_card_view, viewGroup, false);
        return new HpsMobilizationViewHolder(followupLayout, context);
    }

    @Override
    public void onBindViewHolder(@NonNull HpsMobilizationViewHolder holder, int position) {
        HpsMobilizationSessionModel hpsSessionModel = mobilizationSessionModels.get(position);
        holder.bindData(hpsSessionModel);
    }

    @Override
    public int getItemCount() {
        return mobilizationSessionModels.size();
    }

    protected static class HpsMobilizationViewHolder extends RecyclerView.ViewHolder {
        public TextView sbccSessionDate;
        public TextView clientsReached;

        public TextView typeOfCommunitySbcActivity;

        private Context context;

        public HpsMobilizationViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            this.context = context;
        }

        public void bindData(HpsMobilizationSessionModel hpsMobilizationSessionModel) {
            sbccSessionDate = itemView.findViewById(R.id.sbc_session_date);
            typeOfCommunitySbcActivity = itemView.findViewById(R.id.sbc_activity_provided);
            clientsReached = itemView.findViewById(R.id.sbc_clients_reached);
            clientsReached.setVisibility(View.VISIBLE);

            sbccSessionDate.setText(Html.fromHtml(context.getString(R.string.hps_session_date, hpsMobilizationSessionModel.getDateOfGathering())));

            int malesAttended = Integer.parseInt(hpsMobilizationSessionModel.getNumberOfMalesWhoAttended());
            int femalesAttended = Integer.parseInt(hpsMobilizationSessionModel.getNumberOfFemalesWhoAttended());
            int totalClientsReached = malesAttended + femalesAttended;
            clientsReached.setText(Html.fromHtml(context.getString(R.string.hps_clients_reached, totalClientsReached)));

            if (StringUtils.isNotBlank(hpsMobilizationSessionModel.getEducationProvided()) && !hpsMobilizationSessionModel.getEducationProvided().equalsIgnoreCase("null")) {
                evaluateView(typeOfCommunitySbcActivity, context, hpsMobilizationSessionModel.getEducationProvided());
            } else {
                evaluateView(typeOfCommunitySbcActivity, context, "none");
            }

            itemView.setOnClickListener(view -> HpsMobilizationSessionDetailsActivity.startMe(((Activity) context), hpsMobilizationSessionModel.getSessionId()));
        }
    }
}
