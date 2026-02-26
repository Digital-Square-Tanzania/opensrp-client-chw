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
import org.smartregister.chw.activity.TbLeprosyMobilizationDetailsActivity;
import org.smartregister.chw.tbleprosy.model.TbLeprosyMobilizationModel;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class TbLeprosyMobilizationSessionsRegisterAdapter extends RecyclerView.Adapter<TbLeprosyMobilizationSessionsRegisterAdapter.TbLeprosyMobilizationViewHolder> {

    private final Context context;
    private final List<TbLeprosyMobilizationModel> mobilizationSessionModels;

    public TbLeprosyMobilizationSessionsRegisterAdapter(List<TbLeprosyMobilizationModel> mobilizationSessionModels, Context context) {
        this.mobilizationSessionModels = mobilizationSessionModels;
        this.context = context;
    }

    @NonNull
    @Override
    public TbLeprosyMobilizationViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View followupLayout = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.tbleprosy_mobilization_session_card_view, viewGroup, false);
        return new TbLeprosyMobilizationViewHolder(followupLayout);
    }

    @Override
    public void onBindViewHolder(@NonNull TbLeprosyMobilizationViewHolder holder, int position) {
        TbLeprosyMobilizationModel sessionModel = mobilizationSessionModels.get(position);
        holder.bindData(sessionModel);
    }

    @Override
    public int getItemCount() {
        return mobilizationSessionModels.size();
    }

    protected class TbLeprosyMobilizationViewHolder extends RecyclerView.ViewHolder {

        private final TextView mobilizationSessionDate;
        private final TextView mobilizationSessionParticipants;
        private final View sessionContainer;

        TbLeprosyMobilizationViewHolder(@NonNull View itemView) {
            super(itemView);
            mobilizationSessionDate = itemView.findViewById(R.id.mobilization_session_date);
            mobilizationSessionParticipants = itemView.findViewById(R.id.mobilization_session_participants);
            sessionContainer = itemView.findViewById(R.id.rlMobilizationSession);
        }

        void bindData(TbLeprosyMobilizationModel sessionModel) {
            mobilizationSessionDate.setText(context.getString(R.string.mobilziation_session_date, sessionModel.getSessionDate()));
            mobilizationSessionParticipants.setText(context.getString(R.string.mobilization_session_participants, sessionModel.getSessionParticipants()));

            View.OnClickListener openDetails = view -> {
                if (context instanceof Activity) {
                    String sessionId = sessionModel.getSessionId();
                    if (StringUtils.isBlank(sessionId)) {
                        sessionId = sessionModel.getSessionDate();
                    }
                    TbLeprosyMobilizationDetailsActivity.startMe((Activity) context, sessionId);
                }
            };

            itemView.setOnClickListener(openDetails);
            if (sessionContainer != null) {
                sessionContainer.setOnClickListener(openDetails);
            }
        }
    }
}
