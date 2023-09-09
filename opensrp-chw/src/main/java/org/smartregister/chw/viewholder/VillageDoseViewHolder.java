package org.smartregister.chw.viewholder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.smartregister.chw.R;
import org.smartregister.chw.contract.ListContract;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.chw.domain.VillageDose;

import java.util.Map;

public class VillageDoseViewHolder extends ListableViewHolder<VillageDose> {

    private View currentView;
    private TextView tvName;
    private LinearLayout linearLayout;
    private Context context;

    public VillageDoseViewHolder(@NonNull View itemView, Context context) {
        super(itemView);
        this.currentView = itemView;
        tvName = itemView.findViewById(R.id.tvName);
        linearLayout = itemView.findViewById(R.id.linearLayout);
        this.context = context;
    }

    @Override
    public void bindView(VillageDose villageDose, ListContract.View<VillageDose> view) {
        tvName.setText(villageDose.getVillageName());

        for (Map.Entry<String, Integer> entry : villageDose.getRecurringServices().entrySet()) {
            View viewVillage = LayoutInflater.from(currentView.getContext()).inflate(R.layout.village_dose, null, false);

            TextView tvName = viewVillage.findViewById(R.id.tvName);
             String val = entry.getKey().toLowerCase().replace(" ", "_").trim();
            String value = Utils.getStringResourceByName(val, context).trim();

            tvName.setText(value);

            TextView tvAmount = viewVillage.findViewById(R.id.tvAmount);
            tvAmount.setText(entry.getValue().toString());

            linearLayout.addView(viewVillage);
        }

        currentView.setOnClickListener(v -> view.onListItemClicked(villageDose, v.getId()));
    }

    @Override
    public void resetView() {
        tvName.setText("");
        linearLayout.removeAllViews();
    }
}
