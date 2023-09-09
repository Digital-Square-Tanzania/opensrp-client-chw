package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.core.fragment.FamilyCallDialogFragment;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.presenter.ChildProfilePresenter;
import org.smartregister.chw.util.Utils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.DBConstants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public abstract class DefaultChildProfileActivityFlv implements ChildProfileActivity.Flavor {

    @Override
    public boolean isChildOverTwoMonths(CommonPersonObjectClient client) {
        String dobStr = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.DOB, false);
        Date dobDate = null;
        try {
            dobDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(dobStr);
        } catch (ParseException pe) {
            Timber.e(pe);
        }

        if (dobDate != null) {
            LocalDate date1 = LocalDate.fromDateFields(dobDate);
            LocalDate date2 = LocalDate.now();
            return Months.monthsBetween(date1, date2).getMonths() >= 2;
        }
        return false;
    }

    @Override
    public OnClickFloatingMenu getOnClickFloatingMenu(final Activity activity, final ChildProfilePresenter presenter) {
        return viewId -> {
            if (viewId == R.id.fab) {
                FamilyCallDialogFragment.launchDialog(activity, presenter.getFamilyId());
            }
        };
    }

    @Override
    public Intent getSickChildFormActivityIntent(JSONObject jsonObject, Context context) {
        Intent intent = new Intent(context, SickChildJsonFormActivity.class);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonObject.toString());
        Form form = new Form();
        form.setName(context.getString(R.string.sick_child));
        form.setActionBarBackground(R.color.family_actionbar);
        form.setNavigationBackground(R.color.family_navigation);
        form.setWizard(true);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        return intent;
    }

    @Override
    public String getFormattedDateForVisual(String dueDate, String inputFormat) {
        return dueDate;
    }

    @Override
    public void setLastVisitRowView (String days, RelativeLayout layoutLastVisitRow, View viewLastVisitRow, TextView textViewLastVisit, Context context){
        if (TextUtils.isEmpty(days)) {
            layoutLastVisitRow.setVisibility(View.GONE);
            viewLastVisitRow.setVisibility(View.GONE);
        } else {
            layoutLastVisitRow.setVisibility(View.VISIBLE);
            textViewLastVisit.setText(context.getString(org.smartregister.chw.core.R.string.last_visit_40_days_ago, days));
            viewLastVisitRow.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setVaccineHistoryView(String days, RelativeLayout layoutVaccineHistoryRow, View viewVaccineHistoryRow, Context context){
            layoutVaccineHistoryRow.setVisibility(View.GONE);
            viewVaccineHistoryRow.setVisibility(View.GONE);
    }

    @Override
    public String getToolbarTitleName(MemberObject memberObject) {
        return memberObject.getFirstName();
    }
}
