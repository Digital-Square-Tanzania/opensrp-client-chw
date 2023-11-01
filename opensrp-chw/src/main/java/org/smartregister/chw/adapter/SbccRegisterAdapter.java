package org.smartregister.chw.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.chw.R;
import org.smartregister.chw.model.SbccSessionModel;

import java.util.List;

public class SbccRegisterAdapter extends RecyclerView.Adapter<SbccRegisterAdapter.SbccViewHolder> {
    private static Context context;
    private final List<SbccSessionModel> sbccSessionModels;


    public SbccRegisterAdapter(List<SbccSessionModel> sbccSessionModels, Context context) {
        this.sbccSessionModels = sbccSessionModels;
        SbccRegisterAdapter.context = context;
    }

    @NonNull
    @Override
    public SbccViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View followupLayout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.sbcc_session_card_view, viewGroup, false);
        return new SbccViewHolder(followupLayout);
    }

    @Override
    public void onBindViewHolder(@NonNull SbccViewHolder holder, int position) {
        SbccSessionModel sbccSessionModel = sbccSessionModels.get(position);
        holder.bindData(sbccSessionModel);
    }


    @Override
    public int getItemCount() {
        return sbccSessionModels.size();
    }

    protected static class SbccViewHolder extends RecyclerView.ViewHolder {
        public TextView sbccSessionDate;
        public TextView sbccSessionParticipants;
        public TextView sbccSessionLocation;

        public SbccViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void bindData(SbccSessionModel sbccSessionModel) {
            sbccSessionDate = itemView.findViewById(R.id.sbcc_session_date);
            sbccSessionParticipants = itemView.findViewById(R.id.sbcc_session_participants);
            sbccSessionLocation = itemView.findViewById(R.id.sbcc_session_location);

            sbccSessionDate.setText(context.getString(R.string.sbcc_session_date, sbccSessionModel.getSessionDate()));
            sbccSessionParticipants.setText(context.getString(R.string.sbcc_participants, sbccSessionModel.getSessionParticipants()));
            if (sbccSessionModel.getSessionLocation().equalsIgnoreCase("facility")) {
                sbccSessionLocation.setText(context.getString(R.string.sbcc_location, itemView.getContext().getString(R.string.sbcc_session_location_facility)));
            } else if (sbccSessionModel.getSessionLocation().equalsIgnoreCase("community")) {
                sbccSessionLocation.setText(context.getString(R.string.sbcc_location, itemView.getContext().getString(R.string.sbcc_session_location_community)));
            } else {
                sbccSessionLocation.setText(context.getString(R.string.sbcc_location, sbccSessionModel.getSessionLocation()));
            }
        }
    }
}
