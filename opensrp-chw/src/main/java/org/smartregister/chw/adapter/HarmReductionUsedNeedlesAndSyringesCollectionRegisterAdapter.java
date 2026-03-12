package org.smartregister.chw.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.chw.activity.HarmReductionUsedNeedlesAndSyringesCollectionDetailsActivity;
import org.smartregister.chw.harmreduction.R;
import org.smartregister.chw.harmreduction.model.HarmReductionUsedNeedlesAndSyringesCollectionModel;

import java.util.List;

public class HarmReductionUsedNeedlesAndSyringesCollectionRegisterAdapter extends RecyclerView.Adapter<HarmReductionUsedNeedlesAndSyringesCollectionRegisterAdapter.CollectionViewHolder> {

    private final Context context;
    private final List<HarmReductionUsedNeedlesAndSyringesCollectionModel> collectionModels;

    public HarmReductionUsedNeedlesAndSyringesCollectionRegisterAdapter(List<HarmReductionUsedNeedlesAndSyringesCollectionModel> collectionModels, Context context) {
        this.collectionModels = collectionModels;
        this.context = context;
    }

    @NonNull
    @Override
    public CollectionViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View followupLayout = LayoutInflater.from(viewGroup.getContext()).inflate(
                R.layout.harm_reduction_used_needles_collection_card_view,
                viewGroup,
                false
        );
        return new CollectionViewHolder(followupLayout);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionViewHolder holder, int position) {
        HarmReductionUsedNeedlesAndSyringesCollectionModel collectionModel = collectionModels.get(position);
        holder.bindData(collectionModel);
    }

    @Override
    public int getItemCount() {
        return collectionModels.size();
    }

    protected class CollectionViewHolder extends RecyclerView.ViewHolder {
        public TextView collectionDate;
        public TextView safetyBoxesCollected;

        public CollectionViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void bindData(HarmReductionUsedNeedlesAndSyringesCollectionModel collectionModel) {
            safetyBoxesCollected = itemView.findViewById(R.id.mobilization_session_participants);
            collectionDate = itemView.findViewById(R.id.mobilization_session_date);

            String totalSafetyBoxes = collectionModel.getTotalSafetyBoxesCollected();
            if (totalSafetyBoxes == null) {
                totalSafetyBoxes = "0";
            }
            String date = collectionModel.getCollectionDate();
            if (date == null) {
                date = "";
            }

            safetyBoxesCollected.setText(context.getString(R.string.harm_reduction_safety_boxes_collected, totalSafetyBoxes));
            collectionDate.setText(context.getString(R.string.harm_reduction_collection_date, date));
            itemView.setOnClickListener(view -> HarmReductionUsedNeedlesAndSyringesCollectionDetailsActivity.startMe((Activity) context, collectionModel.getCollectionId()));
        }
    }
}
