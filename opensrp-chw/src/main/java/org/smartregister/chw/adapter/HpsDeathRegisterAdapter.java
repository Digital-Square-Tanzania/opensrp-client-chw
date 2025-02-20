package org.smartregister.chw.adapter;


import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
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

import org.smartregister.chw.R;
import org.smartregister.chw.hps.domain.HpsDeathRegisterModel;

import java.util.List;

import timber.log.Timber;

public class HpsDeathRegisterAdapter extends RecyclerView.Adapter<HpsDeathRegisterAdapter.HpsMobilizationViewHolder> {
    private static final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);

    private final Context context;

    private final List<HpsDeathRegisterModel> hpsDeathRegisterModels;


    public HpsDeathRegisterAdapter(List<HpsDeathRegisterModel> hpsDeathRegisterModels, Context context) {
        this.hpsDeathRegisterModels = hpsDeathRegisterModels;
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
        View followupLayout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.hps_death_register_card_view, viewGroup, false);
        return new HpsMobilizationViewHolder(followupLayout, context);
    }

    @Override
    public void onBindViewHolder(@NonNull HpsMobilizationViewHolder holder, int position) {
        HpsDeathRegisterModel hpsSessionModel = hpsDeathRegisterModels.get(position);
        holder.bindData(hpsSessionModel);
    }

    @Override
    public int getItemCount() {
        return hpsDeathRegisterModels.size();
    }

    protected static class HpsMobilizationViewHolder extends RecyclerView.ViewHolder {
        public TextView dateOfDeath;

        public TextView nameOfClient;
        public TextView causeOfDeath;

        private Context context;

        public HpsMobilizationViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            this.context = context;
        }

        public void bindData(HpsDeathRegisterModel hpsMobilizationSessionModel) {
            dateOfDeath = itemView.findViewById(R.id.hps_death_date);
            nameOfClient = itemView.findViewById(R.id.client_name);
            causeOfDeath = itemView.findViewById(R.id.cause_of_death);

            dateOfDeath.setText(context.getString(R.string.hps_death_register_title, hpsMobilizationSessionModel.getDod()));
            nameOfClient.setText(context.getString(R.string.hps_death_register_name_of_the_client, hpsMobilizationSessionModel.getFullName()));
            causeOfDeath.setText(context.getString(R.string.hps_death_register_cause_of_death, hpsMobilizationSessionModel.getCauseOfDeath()));

//            itemView.setOnClickListener(view -> HpsMobilizationSessionDetailsActivity.startMe(((Activity) context), hpsMobilizationSessionModel.getSessionId()));
        }
    }
}
