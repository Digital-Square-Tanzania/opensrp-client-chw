package org.smartregister.chw.provider;

import static org.smartregister.chw.mothermentor.util.MotherMentorHeiSchedule.parseDate;
import static org.smartregister.chw.mothermentor.util.MotherMentorMotherPregnancyUtils.displayDate;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.chw.R;
import org.smartregister.chw.mothermentor.fragment.BaseMotherMentorRegisterFragment;
import org.smartregister.chw.mothermentor.provider.MotherMentorRegisterProvider;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorHouseholdClosureLabels;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;

import java.util.Set;

public class MotherMentorHouseholdRegisterProvider extends MotherMentorRegisterProvider {
    private final Context context;

    public MotherMentorHouseholdRegisterProvider(
            Context context,
            View.OnClickListener paginationClickListener,
            View.OnClickListener onClickListener,
            Set visibleColumns) {
        super(context, paginationClickListener, onClickListener, visibleColumns);
        this.context = context;
    }

    @Override
    public void getFooterView(
            RecyclerView.ViewHolder viewHolder,
            int currentPageCount,
            int totalPageCount,
            boolean hasNext,
            boolean hasPrevious) {
        boolean hasPages = totalPageCount > 0;
        super.getFooterView(
                viewHolder, currentPageCount, totalPageCount,
                hasPages && hasNext, hasPages && hasPrevious);
        if (!hasPages) {
            ((FooterViewHolder) viewHolder).pageInfoView.setText(R.string.mothermentor_household_no_results);
        }
    }

    @Override
    public void getView(
            Cursor cursor, SmartRegisterClient smartRegisterClient, RegisterViewHolder viewHolder) {
        CommonPersonObjectClient pc = (CommonPersonObjectClient) smartRegisterClient;
        String householdName = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true);
        if (!TextUtils.isEmpty(householdName)) {
            viewHolder.patientName.setText(
                    context.getString(R.string.household_register_row_title, householdName));
        }
        String reason = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.KAYA_SABABU_YA_KUFUNGWA, true);
        String closedDate = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.KAYA_TAREHE_YA_KUFUNGWA, true);
        boolean closed = !TextUtils.isEmpty(reason);
        viewHolder.textViewGender.setText(closed ? displayDate(parseDate(closedDate)) : "");
        viewHolder.textViewVillage.setText(closed
                ? MotherMentorHouseholdClosureLabels.reason(context, reason)
                : Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.VILLAGE_TOWN, true));

        viewHolder.dueWrapper.setVisibility(View.GONE);
        viewHolder.patientColumn.setOnClickListener(onClickListener);
        viewHolder.patientColumn.setTag(pc);
        viewHolder.patientColumn.setTag(
                org.smartregister.chw.mothermentor.R.id.VIEW_ID,
                BaseMotherMentorRegisterFragment.CLICK_VIEW_NORMAL);
        viewHolder.registerColumns.setOnClickListener(v -> viewHolder.patientColumn.performClick());
    }
}
