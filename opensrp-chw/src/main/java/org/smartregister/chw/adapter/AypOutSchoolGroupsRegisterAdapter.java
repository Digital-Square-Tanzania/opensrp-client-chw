package org.smartregister.chw.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.chw.R;
import org.smartregister.chw.domain.AypInSchoolGroupListItem;

import java.util.List;

public class AypOutSchoolGroupsRegisterAdapter extends RecyclerView.Adapter<AypOutSchoolGroupsRegisterAdapter.ViewHolder> {

    private final Context context;
    private final List<AypInSchoolGroupListItem> items;

    public AypOutSchoolGroupsRegisterAdapter(List<AypInSchoolGroupListItem> items, Context context) {
        this.items = items;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ayp_group_list, parent, false);
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

        ViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            this.context = context;
            title = itemView.findViewById(R.id.tvGroupName);
            subtitle = itemView.findViewById(R.id.tvGroupMeta);
        }

        void bind(AypInSchoolGroupListItem item) {
            // Show group name prominently
            title.setVisibility(View.VISIBLE);
            title.setText(item.getGroupName() == null ? "" : item.getGroupName());

            // Localize values and style with bold labels and age band value
            String typeValue = localizeGroupType(item.getGroupType());
            String ageBandValue = localizeAgeBand(item.getAgeBand());

            SpannableStringBuilder ssb = new SpannableStringBuilder();
            // Type: <value>
            String typeLabel = context.getString(R.string.ayp_group_type_title);
            ssb.append(typeLabel, new StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
               .append(" ")
               .append(typeValue == null ? "" : typeValue)
               .append("\n");

            // Age-band: <value> (value bold)
            String ageLabel = context.getString(R.string.ayp_group_age_band_title);
            ssb.append(ageLabel, new StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
               .append(" ");
            if (ageBandValue != null && !ageBandValue.isEmpty()) {
                int start = ssb.length();
                ssb.append(ageBandValue);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), start, start + ageBandValue.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            subtitle.setText(ssb);

            // Navigate to group profile on click
            itemView.setOnClickListener(v -> {
                org.smartregister.chw.activity.AypOutSchoolGroupProfileActivity.start(context, item.getBaseEntityId(), item.getGroupName());
            });
        }

        private String localizeGroupType(String raw) {
            if (raw == null) return null;
            switch (raw) {
                case "age_band":
                    return context.getString(R.string.ayp_group_type_age_band);
                case "classes":
                    return context.getString(R.string.ayp_group_type_classes);
                default:
                    return raw;
            }
        }

        private String localizeAgeBand(String raw) {
            if (raw == null) return null;
            switch (raw) {
                case "age_10_14":
                    return context.getString(R.string.ayp_age_band_age_10_14);
                case "age_10_19_enabling_dreams":
                    return context.getString(R.string.ayp_age_band_age_10_19_enabling_dreams);
                case "age_15_19":
                    return context.getString(R.string.ayp_age_band_age_15_19);
                case "age_20_24":
                    return context.getString(R.string.ayp_age_band_age_20_24);
                default:
                    return raw;
            }
        }
    }
}
