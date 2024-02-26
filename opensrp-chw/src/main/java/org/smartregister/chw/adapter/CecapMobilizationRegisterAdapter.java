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
import org.smartregister.chw.activity.CecapMobilizationSessionDetailsActivity;
import org.smartregister.chw.activity.SbcMobilizationSessionDetailsActivity;
import org.smartregister.chw.cecap.model.CecapMobilizationSessionModel;

import java.util.List;

import timber.log.Timber;

public class CecapMobilizationRegisterAdapter extends RecyclerView.Adapter<CecapMobilizationRegisterAdapter.CecapMobilizationViewHolder> {
    private static final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);

    private final Context context;

    private final List<CecapMobilizationSessionModel> mobilizationSessionModels;


    public CecapMobilizationRegisterAdapter(List<CecapMobilizationSessionModel> mobilizationSessionModels, Context context) {
        this.mobilizationSessionModels = mobilizationSessionModels;
        this.context = context;
    }

    private static void evaluateView(TextView tv, Context context, String stringValue) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder.append(context.getString(R.string.sbc_type_of_sbc_activity), boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append("\n");

        String[] stringValueArray;
        if (stringValue.contains(",")) {
            stringValueArray = stringValue.substring(1, stringValue.length() - 1).split(",");
            for (String value : stringValueArray) {
                spannableStringBuilder.append(getStringResource(context, "cecap_", value.trim()) + "\n", new BulletSpan(10), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else if (stringValue.charAt(0) == '[' && stringValue.charAt(stringValue.length() - 1) == ']') {
            spannableStringBuilder.append(getStringResource(context, "cecap_", stringValue.substring(1, stringValue.length() - 1))).append("\n");
        } else {
            spannableStringBuilder.append(getStringResource(context, "cecap_", stringValue)).append("\n");
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
    public CecapMobilizationViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View followupLayout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.sbc_mobilization_session_card_view, viewGroup, false);
        return new CecapMobilizationViewHolder(followupLayout, context);
    }

    @Override
    public void onBindViewHolder(@NonNull CecapMobilizationViewHolder holder, int position) {
        CecapMobilizationSessionModel cecapSessionModel = mobilizationSessionModels.get(position);
        holder.bindData(cecapSessionModel);
    }

    @Override
    public int getItemCount() {
        return mobilizationSessionModels.size();
    }

    protected static class CecapMobilizationViewHolder extends RecyclerView.ViewHolder {
        public TextView sbccSessionDate;

        public TextView typeOfCommunitySbcActivity;

        private Context context;

        public CecapMobilizationViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            this.context = context;
        }

        public void bindData(CecapMobilizationSessionModel cecapSessionModel) {
            sbccSessionDate = itemView.findViewById(R.id.sbc_session_date);
            typeOfCommunitySbcActivity = itemView.findViewById(R.id.sbc_activity_provided);

            sbccSessionDate.setText(context.getString(R.string.sbcc_session_date, cecapSessionModel.getSessionDate()));

            evaluateView(typeOfCommunitySbcActivity, context, cecapSessionModel.getHealthEducation());

            itemView.setOnClickListener(view -> CecapMobilizationSessionDetailsActivity.startMe(((Activity) context), cecapSessionModel.getSessionId()));
        }
    }
}
