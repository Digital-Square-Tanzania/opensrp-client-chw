package org.smartregister.chw.provider;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.LocalDate;
import org.joda.time.Years;
import org.joda.time.format.ISODateTimeFormat;
import org.smartregister.chw.R;
import org.smartregister.chw.hps.fragment.BaseHpsRegisterFragment;
import org.smartregister.chw.hps.provider.HpsRegisterProvider;
import org.smartregister.chw.hps.util.DBConstants;
import org.smartregister.chw.hps.util.HpsUtil;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.view.contract.SmartRegisterClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compact presentation of iCCHW clients using the existing profile action and pagination. */
public class HpsClientRegisterProvider extends HpsRegisterProvider {
    private final Context context;
    private final Set<org.smartregister.configurableviews.model.View> visibleColumns;

    public HpsClientRegisterProvider(
            Context context,
            View.OnClickListener paginationListener,
            View.OnClickListener rowListener,
            Set<org.smartregister.configurableviews.model.View> visibleColumns) {
        super(context, paginationListener, rowListener, visibleColumns);
        this.context = context;
        this.visibleColumns = visibleColumns == null ? Collections.emptySet() : visibleColumns;
    }

    @Override
    public RegisterViewHolder createViewHolder(ViewGroup parent) {
        return new RegisterViewHolder(
                inflater().inflate(R.layout.icchw_client_register_list_row, parent, false));
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, RegisterViewHolder holder) {
        if (!visibleColumns.isEmpty()) return;

        CommonPersonObjectClient person = (CommonPersonObjectClient) client;
        Map<String, String> columns = person.getColumnmaps();
        String gender = genderLabel(columns);
        String village = displayValue(columns, DBConstants.KEY.VILLAGE_TOWN);

        holder.patientName.setText(nameAndAge(columns));
        holder.textViewGender.setText(gender);
        holder.textViewGender.setVisibility(gender.isEmpty() ? View.GONE : View.VISIBLE);
        holder.textViewVillage.setText(village);
        holder.textViewVillage.setVisibility(village.isEmpty() ? View.GONE : View.VISIBLE);
        holder.itemView
                .findViewById(R.id.icchw_metadata_separator)
                .setVisibility(gender.isEmpty() || village.isEmpty() ? View.GONE : View.VISIBLE);
        holder.itemView
                .findViewById(R.id.icchw_client_metadata)
                .setVisibility(gender.isEmpty() && village.isEmpty() ? View.GONE : View.VISIBLE);

        bindProfileAction(holder.registerColumns, person);
        bindProfileAction(holder.patientColumn, person);
        List<String> description = new ArrayList<>();
        addNonEmpty(description, holder.patientName.getText().toString());
        addNonEmpty(description, gender);
        addNonEmpty(description, village);
        holder.registerColumns.setContentDescription(TextUtils.join(". ", description));
    }

    private void bindProfileAction(View view, CommonPersonObjectClient person) {
        view.setTag(person);
        view.setTag(
                org.smartregister.chw.hps.R.id.VIEW_ID, BaseHpsRegisterFragment.CLICK_VIEW_NORMAL);
        view.setOnClickListener(onClickListener);
    }

    private String nameAndAge(Map<String, String> columns) {
        List<String> names = new ArrayList<>();
        addNonEmpty(names, displayValue(columns, DBConstants.KEY.FIRST_NAME));
        addNonEmpty(names, displayValue(columns, DBConstants.KEY.MIDDLE_NAME));
        addNonEmpty(names, displayValue(columns, DBConstants.KEY.LAST_NAME));
        String name =
                names.isEmpty()
                        ? context.getString(R.string.icchw_unnamed_client)
                        : TextUtils.join(" ", names);
        String dob = value(columns, DBConstants.KEY.DOB);
        if (!dob.isEmpty()) {
            try {
                LocalDate birthDate =
                        ISODateTimeFormat.dateOptionalTimeParser().parseLocalDate(dob);
                LocalDate today = LocalDate.now();
                if (!birthDate.isAfter(today)) {
                    return name + ", " + Years.yearsBetween(birthDate, today).getYears();
                }
            } catch (IllegalArgumentException ignored) {
                // A legacy client can still be opened when their date of birth is unusable.
                return name;
            }
        }
        return name;
    }

    private String genderLabel(Map<String, String> columns) {
        if ("0".equals(value(columns, DBConstants.KEY.IS_ANC_CLOSED))) {
            return context.getString(org.smartregister.chw.hps.R.string.anc_string);
        }
        if ("0".equals(value(columns, DBConstants.KEY.IS_PNC_CLOSED))) {
            return context.getString(org.smartregister.chw.hps.R.string.pnc_string);
        }
        return HpsUtil.getGenderTranslated(context, value(columns, DBConstants.KEY.GENDER));
    }

    private static String value(Map<String, String> columns, String key) {
        String value = columns == null ? null : columns.get(key);
        return value == null ? "" : value.trim();
    }

    private static String displayValue(Map<String, String> columns, String key) {
        return org.smartregister.util.Utils.formatValue(value(columns, key), true);
    }

    private static void addNonEmpty(List<String> values, String value) {
        if (!value.isEmpty()) values.add(value);
    }
}
