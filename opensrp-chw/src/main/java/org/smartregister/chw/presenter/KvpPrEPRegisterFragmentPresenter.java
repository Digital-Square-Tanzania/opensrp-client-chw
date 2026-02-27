package org.smartregister.chw.presenter;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.R;
import org.smartregister.chw.kvp.contract.KvpRegisterFragmentContract;
import org.smartregister.chw.kvp.presenter.BaseKvpRegisterFragmentPresenter;
import org.smartregister.chw.kvp.util.Constants;

import java.text.MessageFormat;

public class KvpPrEPRegisterFragmentPresenter extends BaseKvpRegisterFragmentPresenter {

    public KvpPrEPRegisterFragmentPresenter(KvpRegisterFragmentContract.View view, KvpRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainTable() {
        return Constants.TABLES.KVP_PrEP_REGISTER;
    }

    public String getDueFilterCondition(String nextAppointmentStartDate, String nextAppointmentEndDate, Context context) {
        StringBuilder customFilter = new StringBuilder();

        if (nextAppointmentStartDate != null && !nextAppointmentStartDate.equalsIgnoreCase(context.getString(R.string.none))) {
            customFilter.append(" AND date(substr(next_visit_date, 7, 4) || '-' || substr(next_visit_date, 4, 2) || '-' || substr(next_visit_date, 1, 2)) >= date(substr('" + nextAppointmentStartDate + "', 7, 4) || '-' || substr('" + nextAppointmentStartDate + "', 4, 2) || '-' || substr('" + nextAppointmentStartDate + "', 1, 2))");
        }

        if (nextAppointmentEndDate != null && !nextAppointmentEndDate.equalsIgnoreCase(context.getString(R.string.none))) {
            customFilter.append(" AND date(substr(next_visit_date, 7, 4) || '-' || substr(next_visit_date, 4, 2) || '-' || substr(next_visit_date, 1, 2)) <= date(substr('" + nextAppointmentEndDate + "', 7, 4) || '-' || substr('" + nextAppointmentEndDate + "', 4, 2) || '-' || substr('" + nextAppointmentEndDate + "', 1, 2))");
        }

        return customFilter.toString();
    }

    @Override
    public String getMainCondition() {
        return this.getMainTable() + ".is_closed IS 0 ";
    }
}
