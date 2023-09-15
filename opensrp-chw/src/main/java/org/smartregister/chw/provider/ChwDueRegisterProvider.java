package org.smartregister.chw.provider;

import static org.smartregister.chw.util.Utils.getClientName;

import android.content.Context;
import android.database.Cursor;
import android.util.TypedValue;
import android.view.View;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.util.Utils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.fragment.BaseFamilyProfileMemberFragment;
import org.smartregister.family.provider.FamilyDueRegisterProvider;
import org.smartregister.family.util.DBConstants;
import org.smartregister.view.contract.SmartRegisterClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

public class ChwDueRegisterProvider extends FamilyDueRegisterProvider {

    private final Context context;

    private final View.OnClickListener onClickListener;

    public ChwDueRegisterProvider(Context context, CommonRepository commonRepository, Set visibleColumns, View.OnClickListener onClickListener, View.OnClickListener paginationClickListener) {
        super(context, commonRepository, visibleColumns, onClickListener, paginationClickListener);
        this.context = context;
        this.onClickListener = onClickListener;
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, RegisterViewHolder viewHolder) {
        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;
        populatePatientColumn(pc, client, viewHolder);
    }

    private void populatePatientColumn(CommonPersonObjectClient pc, SmartRegisterClient client, final RegisterViewHolder viewHolder) {

        String firstName = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true);
        String middleName = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.MIDDLE_NAME, true);
        String lastName = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.LAST_NAME, true);
        String familyName = Utils.getValue(pc.getColumnmaps(), ChildDBConstants.KEY.FAMILY_FIRST_NAME, true);
        String scheduleName = Utils.getValue(pc.getColumnmaps(), ChildDBConstants.KEY.SCHEDULE_NAME, false);
        String dueDate = Utils.getValue(pc.getColumnmaps(), ChildDBConstants.KEY.DUE_DATE, false);
        String overDueDate = Utils.getValue(pc.getColumnmaps(), ChildDBConstants.KEY.OVER_DUE_DATE, false);


        String dob = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.DOB, false);
        String dobString = Utils.getDuration(dob);
        dobString = dobString.contains("y") ? dobString.substring(0, dobString.indexOf("y")) : dobString;

        if (StringUtils.isNotBlank(firstName) || StringUtils.isNotBlank(middleName) || StringUtils.isNotBlank(lastName)) {
            String patientName = getClientName(firstName, middleName, lastName);
            patientName = patientName + ", " + dobString + " " + getVisitType(scheduleName);
            fillValue(viewHolder.patientNameAge, patientName);
        } else {
            String title = context.getString(R.string.family, familyName) + " " + getVisitType(scheduleName);
            fillValue(viewHolder.patientNameAge, title);
        }

        // Update UI cutoffs
        viewHolder.patientNameAge.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimensionPixelSize(R.dimen.member_due_list_title_size));

        viewHolder.nextArrow.setVisibility(View.VISIBLE);

        String lastVisit = Utils.getValue(pc.getColumnmaps(), ChildDBConstants.KEY.LAST_HOME_VISIT, false);
        if (StringUtils.isNotBlank(lastVisit)) {
            // String lastVisitString = Utils.actualDuration(context, Utils.getDuration(lastVisit));
            String lastVisitString = org.smartregister.chw.core.utils.Utils.actualDaysBetweenDateAndNow(context, lastVisit);
            viewHolder.lastVisit.setText(String.format(context.getString(R.string.last_visit_prefix), lastVisitString));
            viewHolder.lastVisit.setVisibility(View.VISIBLE);
        } else {
            viewHolder.lastVisit.setVisibility(View.GONE);
        }

        viewHolder.nextArrowColumn.setOnClickListener(v -> viewHolder.nextArrow.performClick());

        viewHolder.statusColumn.setOnClickListener(v -> viewHolder.patientColumn.performClick());

        attachPatientOnclickListener(viewHolder.patientColumn, client);

        attachNextArrowOnclickListener(viewHolder.nextArrow, client);
        updateDueColumn(viewHolder, dueDate, overDueDate);
    }

    private String getVisitType(String scheduleName) {
        switch (scheduleName) {
            case CoreConstants.SCHEDULE_TYPES.ANC_VISIT:
                return context.getString(R.string.anc_visit_suffix);
            case CoreConstants.SCHEDULE_TYPES.PNC_VISIT:
                return context.getString(R.string.pnc_visit_suffix);
            case CoreConstants.SCHEDULE_TYPES.MALARIA_VISIT:
                return context.getString(R.string.malaria_visit_suffix);
            case CoreConstants.SCHEDULE_TYPES.WASH_CHECK:
                return " · " + context.getString(R.string.wash_check);
            case CoreConstants.SCHEDULE_TYPES.FAMILY_KIT:
                return " · " + context.getString(R.string.family_kit);
            case CoreConstants.SCHEDULE_TYPES.ROUTINE_HOUSEHOLD_VISIT:
                return " · " + context.getString(R.string.routine_household_visit);
            case CoreConstants.SCHEDULE_TYPES.HIV_VISIT:
                return context.getString(R.string.routine_hiv_visit);
            case CoreConstants.SCHEDULE_TYPES.TB_VISIT:
                return context.getString(R.string.routine_tb_visit);
            default:
                return context.getString(R.string.home_visit_suffix);
        }
    }

    private void attachPatientOnclickListener(View view, SmartRegisterClient client) {
        view.setOnClickListener(onClickListener);
        view.setTag(client);
        view.setTag(R.id.VIEW_ID, BaseFamilyProfileMemberFragment.CLICK_VIEW_NORMAL);
    }

    private void attachNextArrowOnclickListener(View view, SmartRegisterClient client) {
        view.setOnClickListener(onClickListener);
        view.setTag(client);
        view.setTag(R.id.VIEW_ID, BaseFamilyProfileMemberFragment.CLICK_VIEW_NEXT_ARROW);
    }

    private void updateDueColumn(RegisterViewHolder viewHolder, String dueDate, String overDueDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date _dueDate = sdf.parse(dueDate);
            Date _overDueDate = sdf.parse(overDueDate);
            Date now = new Date();

            viewHolder.status.setVisibility(View.VISIBLE);
            if (_overDueDate.getTime() < now.getTime()) {
                viewHolder.status.setImageResource(Utils.getOverDueProfileImageResourceIDentifier());
            } else if (_dueDate.getTime() < now.getTime()) {
                viewHolder.status.setImageResource(Utils.getDueProfileImageResourceIDentifier());
            }
        } catch (ParseException e) {
            Timber.e(e);
        }
    }

}
