package org.smartregister.chw.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.contract.SickFormMedicalHistoryContract;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SickFormMedicalHistoryAdapter extends RecyclerView.Adapter<SickFormMedicalHistoryAdapter.MyViewHolder> {

    private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);

    private List<Visit> serviceList;
    private SickFormMedicalHistoryContract.View view;

    public SickFormMedicalHistoryAdapter(List<Visit> serviceList, SickFormMedicalHistoryContract.View view) {
        this.serviceList = serviceList;
        this.view = view;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
        View v = layoutInflater.inflate(R.layout.sick_form_medical_history, viewGroup, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SickFormMedicalHistoryAdapter.MyViewHolder holder, int i) {
        Visit visit = serviceList.get(i);
        holder.tvDetails.setText(sdf.format(visit.getDate()));
        holder.parentView.setOnClickListener(v -> view.onAdapterInteraction(visit));
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDetails;
        private View parentView;

        private MyViewHolder(View view) {
            super(view);
            this.parentView = view;
            tvDetails = view.findViewById(R.id.tvDetails);
        }
    }
}
