package org.smartregister.chw.provider;

import android.content.Context;
import android.database.Cursor;

import org.joda.time.LocalDate;
import org.smartregister.chw.R;
import org.smartregister.chw.harmreduction.provider.HarmReductionRegisterProvider;
import org.smartregister.chw.util.HarmReductionMatAppointmentStatus;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;

import java.util.Set;

public class HarmReductionMatClientsRegisterProvider extends HarmReductionRegisterProvider {

    private static final String NEXT_APPOINTMENT_DATE = "next_appointment_date";
    private final Context context;

    public HarmReductionMatClientsRegisterProvider(Context context,
                                                   android.view.View.OnClickListener paginationClickListener,
                                                   android.view.View.OnClickListener onClickListener,
                                                   Set<org.smartregister.configurableviews.model.View> visibleColumns) {
        super(context, paginationClickListener, onClickListener, visibleColumns);
        this.context = context;
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, RegisterViewHolder viewHolder) {
        super.getView(cursor, client, viewHolder);

        CommonPersonObjectClient person = (CommonPersonObjectClient) client;
        String appointmentDate = Utils.getValue(
                person.getColumnmaps(), NEXT_APPOINTMENT_DATE, false
        );
        HarmReductionMatAppointmentStatus.Status status =
                HarmReductionMatAppointmentStatus.classify(appointmentDate, LocalDate.now());

        if (status == HarmReductionMatAppointmentStatus.Status.NONE) {
            viewHolder.dueWrapper.setVisibility(android.view.View.GONE);
            return;
        }

        viewHolder.dueWrapper.setVisibility(android.view.View.VISIBLE);
        int label;
        if (status == HarmReductionMatAppointmentStatus.Status.MISSED) {
            label = R.string.harm_reduction_mat_appointment_missed;
            viewHolder.dueButton.setBackgroundResource(org.smartregister.chw.core.R.drawable.record_btn_selector_overdue);
        } else if (status == HarmReductionMatAppointmentStatus.Status.DUE) {
            label = R.string.harm_reduction_mat_appointment_due;
            viewHolder.dueButton.setBackgroundResource(org.smartregister.chw.core.R.drawable.record_btn_selector_due);
        } else {
            label = R.string.harm_reduction_mat_appointment_upcoming;
            viewHolder.dueButton.setBackgroundResource(R.drawable.due_contact);
        }
        viewHolder.dueButton.setText(context.getString(
                R.string.harm_reduction_mat_appointment_status,
                context.getString(label),
                HarmReductionMatAppointmentStatus.displayDate(appointmentDate)
        ));
    }
}
