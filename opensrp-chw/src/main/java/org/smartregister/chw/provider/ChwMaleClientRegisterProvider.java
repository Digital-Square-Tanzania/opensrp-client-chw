package org.smartregister.chw.provider;

import static org.smartregister.chw.util.Constants.PartnerRegistrationConstants.INTENT_BASE_ENTITY_ID;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;

import org.apache.commons.text.WordUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.smartregister.chw.R;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;
import org.smartregister.opd.OpdLibrary;
import org.smartregister.opd.configuration.OpdRegisterProviderMetadata;
import org.smartregister.opd.holders.OpdRegisterViewHolder;
import org.smartregister.opd.utils.ConfigurationInstancesHelper;

import java.util.Map;

public class ChwMaleClientRegisterProvider extends OpdRegisterProvider {
    private final Context context;

    private OpdRegisterProviderMetadata opdRegisterProviderMetadata;

    public ChwMaleClientRegisterProvider(@NonNull Context context, @NonNull View.OnClickListener onClickListener, @NonNull View.OnClickListener paginationClickListener) {
        super(context, onClickListener, paginationClickListener);
        this.context = context;
        this.opdRegisterProviderMetadata = ConfigurationInstancesHelper
                .newInstance(OpdLibrary.getInstance()
                        .getOpdConfiguration()
                        .getOpdRegisterProviderMetadata());
    }

    @Override
    public void populatePatientColumn(CommonPersonObjectClient commonPersonObjectClient, OpdRegisterViewHolder viewHolder) {
        int age_val = new Period(new DateTime(opdRegisterProviderMetadata.getDob(commonPersonObjectClient.getColumnmaps())), new DateTime()).getYears();

        Map<String, String> patientColumnMaps = commonPersonObjectClient.getColumnmaps();

        viewHolder.hideRegisterType();
        viewHolder.removeCareGiverName();
        String firstName = opdRegisterProviderMetadata.getClientFirstName(patientColumnMaps);
        String middleName = opdRegisterProviderMetadata.getClientMiddleName(patientColumnMaps);
        String lastName = opdRegisterProviderMetadata.getClientLastName(patientColumnMaps);
        String fullName = org.smartregister.util.Utils.getName(firstName, middleName + " " + lastName);
        String baseEntityId = commonPersonObjectClient.entityId();
        String age = String.valueOf(age_val);

        fillValue(viewHolder.textViewChildName, WordUtils.capitalize(fullName) + ", " +
                WordUtils.capitalize(age));
        setAddressAndGender(commonPersonObjectClient, viewHolder);
        viewHolder.itemView.findViewById(R.id.go_to_profile_image_view).setVisibility(View.GONE);
        viewHolder.itemView.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle(R.string.register_partner_dialog_title);
            builder.setMessage(WordUtils.capitalize(fullName) + ", " + WordUtils.capitalize(age));
            builder.setCancelable(true);
            builder.setPositiveButton(v.getContext().getString(R.string.yes), (dialog, id) -> {
                Activity parentActivity = (Activity) context;
                Intent intent = new Intent();
                intent.putExtra(INTENT_BASE_ENTITY_ID, baseEntityId);
                parentActivity.setResult(Activity.RESULT_OK, intent);
                parentActivity.finish();
            });
            builder.setNegativeButton(v.getContext().getString(R.string.cancel), ((dialog, id) -> dialog.cancel()));

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        });
    }


    @Override
    public void setAddressAndGender(CommonPersonObjectClient pc, OpdRegisterViewHolder viewHolder) {
        super.setAddressAndGender(pc, viewHolder);
        String gender_key = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.GENDER, true);
        String gender = "";
        if (gender_key.equalsIgnoreCase("Male")) {
            gender = context.getString(org.smartregister.chw.core.R.string.male);
        }
        fillValue(viewHolder.textViewGender, gender);
    }
}
