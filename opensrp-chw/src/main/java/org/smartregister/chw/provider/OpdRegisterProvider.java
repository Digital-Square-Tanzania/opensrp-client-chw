package org.smartregister.chw.provider;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import org.apache.commons.text.WordUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;
import org.smartregister.opd.OpdLibrary;
import org.smartregister.opd.configuration.OpdRegisterProviderMetadata;
import org.smartregister.opd.holders.OpdRegisterViewHolder;
import org.smartregister.opd.utils.ConfigurationInstancesHelper;
import org.smartregister.opd.utils.OpdDbConstants;

import java.util.Map;

import timber.log.Timber;

public class OpdRegisterProvider extends org.smartregister.opd.provider.OpdRegisterProvider {
    private final Context context;

    private OpdRegisterProviderMetadata opdRegisterProviderMetadata;

    public OpdRegisterProvider(@NonNull Context context, @NonNull View.OnClickListener onClickListener, @NonNull View.OnClickListener paginationClickListener) {
        super(context, onClickListener, paginationClickListener);
        this.context = context;
        this.opdRegisterProviderMetadata = ConfigurationInstancesHelper
                .newInstance(OpdLibrary.getInstance()
                        .getOpdConfiguration()
                        .getOpdRegisterProviderMetadata());
    }

    @Override
    public void populatePatientColumn(CommonPersonObjectClient commonPersonObjectClient, OpdRegisterViewHolder viewHolder) {
        super.populatePatientColumn(commonPersonObjectClient, viewHolder);
        Map<String, String> patientColumnMaps = commonPersonObjectClient.getColumnmaps();
        String registerType = org.smartregister.util.Utils.getValue(commonPersonObjectClient.getColumnmaps(),
                OpdDbConstants.KEY.REGISTER_TYPE, true);

        if (!TextUtils.isEmpty(registerType)) {
            viewHolder.showRegisterType();
            String type = getTranslatedRegisterType(registerType);
            fillValue(viewHolder.tvRegisterType, type);
        } else {
            viewHolder.hideRegisterType();
        }
        String firstName = opdRegisterProviderMetadata.getClientFirstName(patientColumnMaps);
        String middleName = opdRegisterProviderMetadata.getClientMiddleName(patientColumnMaps);
        String lastName = opdRegisterProviderMetadata.getClientLastName(patientColumnMaps);
        String fullName = org.smartregister.util.Utils.getName(firstName, middleName + " " + lastName);

        try {
            String age = String.valueOf(new Period(new DateTime(opdRegisterProviderMetadata.getDob(patientColumnMaps)), new DateTime()).getYears());
            fillValue(viewHolder.textViewChildName, WordUtils.capitalize(fullName) + ", " +
                    WordUtils.capitalize(age));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private String getTranslatedRegisterType(String registerType) {
        if (registerType.equalsIgnoreCase(CoreConstants.REGISTER_TYPE.CHILD)) {
            return context.getString(R.string.menu_child);
        } else if (registerType.equalsIgnoreCase(CoreConstants.REGISTER_TYPE.ANC)) {
            return context.getString(R.string.menu_anc);
        } else if (registerType.equalsIgnoreCase(CoreConstants.REGISTER_TYPE.PNC)) {
            return context.getString(R.string.menu_pnc);
        } else if (registerType.equalsIgnoreCase(CoreConstants.REGISTER_TYPE.FAMILY_PLANNING)) {
            return context.getString(R.string.menu_family_planning);
        } else if (registerType.equalsIgnoreCase(CoreConstants.REGISTER_TYPE.MALARIA)) {
            return context.getString(R.string.menu_malaria);
        } else if (registerType.equalsIgnoreCase(CoreConstants.REGISTER_TYPE.INDEPENDENT)) {
            return context.getString(R.string.menu_independent);
        } else if (registerType.equalsIgnoreCase(CoreConstants.REGISTER_TYPE.CECAP)) {
            return context.getString(R.string.menu_cecap);
        }
        return registerType;
    }

    @Override
    public void setAddressAndGender(CommonPersonObjectClient pc, OpdRegisterViewHolder viewHolder) {
        super.setAddressAndGender(pc, viewHolder);
        String gender_key = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.GENDER, true);
        String gender = "";
        if (gender_key.equalsIgnoreCase("Male")) {
            gender = context.getString(org.smartregister.chw.core.R.string.male);
        } else if (gender_key.equalsIgnoreCase("Female")) {
            gender = context.getString(org.smartregister.chw.core.R.string.female);
        }
        fillValue(viewHolder.textViewGender, gender);
    }
}
