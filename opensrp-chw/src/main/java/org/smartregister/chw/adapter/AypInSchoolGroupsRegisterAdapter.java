package org.smartregister.chw.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.chw.R;
import org.smartregister.chw.domain.AypInSchoolGroupListItem;

import java.util.List;

public class AypInSchoolGroupsRegisterAdapter extends RecyclerView.Adapter<AypInSchoolGroupsRegisterAdapter.ViewHolder> {

    private final Context context;
    private final List<AypInSchoolGroupListItem> items;

    public AypInSchoolGroupsRegisterAdapter(List<AypInSchoolGroupListItem> items, Context context) {
        this.items = items;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.hps_death_register_card_view, parent, false);
        return new ViewHolder(row, context);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final Context context;
        private TextView title;
        private TextView subtitle;
        private TextView extra;

        ViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            this.context = context;
            title = itemView.findViewById(R.id.client_name);
            subtitle = itemView.findViewById(R.id.hps_death_date);
            extra = itemView.findViewById(R.id.cause_of_death);
        }

        void bind(AypInSchoolGroupListItem item) {
            // Show group name prominently
            title.setVisibility(View.VISIBLE);
            title.setText(item.getGroupName() == null ? "" : item.getGroupName());

            // Show group type and/or age band
            StringBuilder sb = new StringBuilder();
            if (item.getGroupType() != null && !item.getGroupType().isEmpty()) {
                sb.append(context.getString(R.string.ayp_group_type_label, item.getGroupType()));
            }
            if (item.getAgeBand() != null && !item.getAgeBand().isEmpty()) {
                if (sb.length() > 0) sb.append("  ");
                sb.append(context.getString(R.string.ayp_group_age_band_label, item.getAgeBand()));
            }
            subtitle.setText(sb.toString());

            // No extra line for now
            extra.setVisibility(View.GONE);

            // Future: navigate to group profile screen when clicked
            itemView.setOnClickListener(v -> {
                // Placeholder: hook group profile activity if/when available
            });
        }
    }
}

