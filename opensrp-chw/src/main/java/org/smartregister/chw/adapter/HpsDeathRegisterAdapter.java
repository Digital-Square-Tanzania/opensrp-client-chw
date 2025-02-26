package org.smartregister.chw.adapter;


import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.HpsDeathRegistrationDetailsActivity;
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

            dateOfDeath.setText(context.getString(R.string.hps_death_register_death_date, hpsMobilizationSessionModel.getDod()));

            if (hpsMobilizationSessionModel.getAge() > 0) {
                nameOfClient.setText(context.getString(R.string.hps_death_register_name_of_the_client, hpsMobilizationSessionModel.getFullName() + ", " + hpsMobilizationSessionModel.getAge()));
            } else {
                nameOfClient.setText(context.getString(R.string.hps_death_register_name_of_the_client, hpsMobilizationSessionModel.getFullName()));
            }
            causeOfDeath.setText(context.getString(R.string.hps_death_register_cause_of_death, getStringResource(context, "hps_", hpsMobilizationSessionModel.getCauseOfDeath())));

            itemView.setOnClickListener(view -> HpsDeathRegistrationDetailsActivity.startMe(((Activity) context), hpsMobilizationSessionModel.getDeathId()));
        }
    }
}
