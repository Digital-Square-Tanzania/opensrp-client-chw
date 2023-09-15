package org.smartregister.chw.provider;

import static org.smartregister.chw.core.utils.Utils.getDuration;
import static org.smartregister.chw.util.Utils.getClientName;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.malaria.util.Constants;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.fragment.BaseFamilyProfileMemberFragment;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;
import org.smartregister.view.customcontrols.FontVariant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class FamilyActivityRegisterProvider extends org.smartregister.family.provider.FamilyActivityRegisterProvider {
    public FamilyActivityRegisterProvider(Context context, CommonRepository commonRepository, Set visibleColumns, View.OnClickListener onClickListener, View.OnClickListener paginationClickListener) {
        super(context, commonRepository, visibleColumns, onClickListener, paginationClickListener);
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, RegisterViewHolder viewHolder) {
        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;
        if (visibleColumns.isEmpty()) {
            populatePatientColumn(pc, client, viewHolder);
            populateIdentifierColumn(pc, viewHolder);

            return;
        }
    }

    protected void populatePatientColumn(CommonPersonObjectClient pc, SmartRegisterClient client, final RegisterViewHolder viewHolder) {

        String firstName = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true);
        String middleName = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.MIDDLE_NAME, true);
        String lastName = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.LAST_NAME, true);
        String visitType = Utils.getValue(pc.getColumnmaps(), ChildDBConstants.KEY.VISIT_TYPE, false);
        String familyName = org.smartregister.chw.util.Utils.getValue(pc.getColumnmaps(), ChildDBConstants.KEY.FAMILY_FIRST_NAME, true);

        String eventType = getVisitType(visitType);
        boolean notVisited = notVisited(visitType);
        long eventDate = parseLong(Utils.getValue(pc.getColumnmaps(), ChildDBConstants.KEY.VISIT_DATE, false));

        if (notVisited) {
            viewHolder.status.setImageResource(Utils.getActivityProfileImageResourceNotVistedIDentifier());
            fillValue(viewHolder.lastVisit, String.format(context.getString(R.string.profile_activity_not_visited), new SimpleDateFormat("dd MMM yyyy").format(new Date(eventDate))));
        } else {
            viewHolder.status.setImageResource(Utils.getActivityProfileImageResourceVistedIDentifier());
            fillValue(viewHolder.lastVisit, String.format(context.getString(R.string.profile_activity_completed), new SimpleDateFormat("dd MMM yyyy").format(new Date(eventDate))));
        }

        String patientName = getClientName(firstName, middleName, lastName);

        String dob = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.DOB, false);
        String dobString = getDuration(dob);
        dobString = dobString.contains("y") ? dobString.substring(0, dobString.indexOf("y")) : dobString;

        String dod = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.DOD, false);
        if (StringUtils.isNotBlank(dod)) {

            dobString = getDuration(dod, dob);
            dobString = dobString.contains("y") ? dobString.substring(0, dobString.indexOf("y")) : dobString;

            patientName = patientName + ", " + dobString + " " + eventType + context.getString(org.smartregister.family.R.string.deceased_brackets);
            viewHolder.patientNameAge.setFontVariant(FontVariant.REGULAR);
            viewHolder.patientNameAge.setTextColor(Color.GRAY);
            viewHolder.patientNameAge.setTypeface(viewHolder.patientNameAge.getTypeface(), Typeface.ITALIC);
        } else {
            if (StringUtils.isNotBlank(firstName) || StringUtils.isNotBlank(middleName) || StringUtils.isNotBlank(lastName)) {
                patientName = Utils.getName(firstName, middleName, lastName);
                patientName = patientName + ", " + dobString + " " + eventType;
            } else {
                patientName = context.getString(R.string.family, familyName) + " " + eventType;
            }

            viewHolder.patientNameAge.setFontVariant(FontVariant.REGULAR);
            viewHolder.patientNameAge.setTextColor(Color.BLACK);
            viewHolder.patientNameAge.setTypeface(viewHolder.patientNameAge.getTypeface(), Typeface.NORMAL);
        }

        fillValue(viewHolder.patientNameAge, patientName);

        // Update UI cutoffs
        viewHolder.patientNameAge.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimensionPixelSize(R.dimen.member_due_list_title_size));

        String gender = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.GENDER, true);
        fillValue(viewHolder.gender, gender);

        viewHolder.status.setOnClickListener(v -> viewHolder.patientColumn.performClick());

        View patient = viewHolder.patientColumn;
        attachPatientOnclickListener(patient, client);
    }

    private String getVisitType(String visitType) {
        switch (visitType) {
            case CoreConstants.EventType.ANC_HOME_VISIT:
                return context.getString(R.string.anc_visit_suffix);
            case CoreConstants.EventType.PNC_HOME_VISIT:
                return context.getString(R.string.pnc_visit_suffix);
            case Constants.FORMS.MALARIA_FOLLOW_UP_VISIT:
                return context.getString(R.string.malaria_visit_suffix);
            case CoreConstants.EventType.WASH_CHECK:
                return " · " + context.getString(R.string.wash_check);
            case CoreConstants.EventType.FAMILY_KIT:
                return " · " + context.getString(R.string.family_kit);
            case CoreConstants.EventType.CHILD_HOME_VISIT:
                return context.getString(R.string.home_visit_suffix);
            case CoreConstants.EventType.ROUTINE_HOUSEHOLD_VISIT:
                return " · " + context.getString(R.string.routine_household_visit);
            default:
                return "";
        }
    }

    private boolean notVisited(String visitType) {
        List<String> notVisited = new ArrayList<>();
        notVisited.add(CoreConstants.EventType.CHILD_VISIT_NOT_DONE);
        notVisited.add(CoreConstants.EventType.ANC_HOME_VISIT_NOT_DONE);

        return notVisited.contains(visitType);
    }

    private void populateIdentifierColumn(CommonPersonObjectClient pc, RegisterViewHolder viewHolder) {
        String uniqueId = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.UNIQUE_ID, false);
        //fillValue(viewHolder.ancId, String.format(context.getString(R.string.unique_id_text), uniqueId));
    }

    private long parseLong(String string) {
        long res = 0l;
        try {
            if (StringUtils.isNotBlank(string))
                res = Long.valueOf(string);
        } catch (Exception e) {
            Timber.e(e);
        }
        return res;
    }

    private void attachPatientOnclickListener(View view, SmartRegisterClient client) {
        view.setOnClickListener(onClickListener);
        view.setTag(client);
        view.setTag(org.smartregister.family.R.id.VIEW_ID, BaseFamilyProfileMemberFragment.CLICK_VIEW_NORMAL);
    }

}
