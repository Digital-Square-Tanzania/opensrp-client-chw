package org.smartregister.chw.adapter;


import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.HpsAnnualCensusRegistrationDetailsActivity;
import org.smartregister.chw.hps.domain.HpsAnnualCensusRegisterModel;

import java.util.List;

public class HpsAnnualCensusRegisterAdapter extends RecyclerView.Adapter<HpsAnnualCensusRegisterAdapter.HpsAnnualCensusViewHolder> {
    private static final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);

    private final Context context;

    private final List<HpsAnnualCensusRegisterModel> hpsAnnualCensusRegisterModels;


    public HpsAnnualCensusRegisterAdapter(List<HpsAnnualCensusRegisterModel> hpsAnnualCensusRegisterModels, Context context) {
        this.hpsAnnualCensusRegisterModels = hpsAnnualCensusRegisterModels;
        this.context = context;
    }

    @NonNull
    @Override
    public HpsAnnualCensusViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View followupLayout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.hps_death_register_card_view, viewGroup, false);
        return new HpsAnnualCensusViewHolder(followupLayout, context);
    }

    @Override
    public void onBindViewHolder(@NonNull HpsAnnualCensusViewHolder holder, int position) {
        HpsAnnualCensusRegisterModel hpsSessionModel = hpsAnnualCensusRegisterModels.get(position);
        holder.bindData(hpsSessionModel);
    }

    @Override
    public int getItemCount() {
        return hpsAnnualCensusRegisterModels.size();
    }

    protected static class HpsAnnualCensusViewHolder extends RecyclerView.ViewHolder {
        public TextView dateOfDeath;

        public TextView nameOfClient;
        public TextView causeOfDeath;

        private Context context;

        public HpsAnnualCensusViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            this.context = context;
        }

        public void bindData(HpsAnnualCensusRegisterModel hpsAnnualCensusRegisterModel) {
            dateOfDeath = itemView.findViewById(R.id.hps_death_date);
            nameOfClient = itemView.findViewById(R.id.client_name);
            causeOfDeath = itemView.findViewById(R.id.cause_of_death);

            nameOfClient.setVisibility(View.GONE);
            causeOfDeath.setVisibility(View.GONE);

            dateOfDeath.setText(Html.fromHtml(context.getString(R.string.hps_annual_census_year, hpsAnnualCensusRegisterModel.getYear())));

            itemView.setOnClickListener(view -> HpsAnnualCensusRegistrationDetailsActivity.startMe(((Activity) context), hpsAnnualCensusRegisterModel.getBaseEntityId()));
        }
    }
}
