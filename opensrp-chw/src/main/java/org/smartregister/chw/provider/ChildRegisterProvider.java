package org.smartregister.chw.provider;

import static org.smartregister.chw.core.utils.Utils.getDuration;
import static org.smartregister.chw.util.Utils.getClientName;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;

import org.apache.commons.lang3.text.WordUtils;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.holders.RegisterViewHolder;
import org.smartregister.chw.core.provider.CoreChildRegisterProvider;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.task.ChwUpdateLastAsyncTask;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;

import java.util.Set;

/**
 * Created by keyman on 13/11/2018.
 */

public class ChildRegisterProvider extends CoreChildRegisterProvider {

    private Set<org.smartregister.configurableviews.model.View> visibleColumns;

    private View.OnClickListener onClickListener;

    private Context context;
    private CommonRepository commonRepository;

    public ChildRegisterProvider(Context context, CommonRepository commonRepository, Set visibleColumns, View.OnClickListener onClickListener, View.OnClickListener paginationClickListener) {
        super(context, visibleColumns, onClickListener, paginationClickListener);
        this.visibleColumns = visibleColumns;
        this.onClickListener = onClickListener;
        this.context = context;
        this.commonRepository = commonRepository;
    }


    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, RegisterViewHolder viewHolder) {
        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;
        if (visibleColumns.isEmpty()) {
            populatePatientColumn(pc, client, viewHolder);
            populateIdentifierColumn(pc, viewHolder);
            populateLastColumn(pc, viewHolder);

            return;
        }
    }

    private int getChildRegisterLayout() {
        return !ChwApplication.getApplicationFlavor().prioritizeChildNameOnChildRegister() ? R.layout.adapter_child_register_list_row : R.layout.adapter_prioritize_child_register_list_row;
    }

    @Override
    public RegisterViewHolder createViewHolder(ViewGroup parent) {
        View view = inflater.inflate(getChildRegisterLayout(), parent, false);
        return new RegisterViewHolder(view);
    }

    private void fillChildNameAndAge(RegisterViewHolder viewHolder, String childName, String dobString) {
        String age = context.getResources().getString(R.string.age) + ": " + WordUtils.capitalize(Utils.getTranslatedDate(dobString, context));
        if (!ChwApplication.getApplicationFlavor().prioritizeChildNameOnChildRegister()) {
            fillValue(viewHolder.textViewChildName, WordUtils.capitalize(childName) + ", " + WordUtils.capitalize(Utils.getTranslatedDate(dobString, context)));
        } else {
            fillValue(viewHolder.textViewChildName, WordUtils.capitalize(childName));
            fillValue(viewHolder.textViewChildAge, age);
        }
    }


    protected void populatePatientColumn(CommonPersonObjectClient pc, SmartRegisterClient client, RegisterViewHolder viewHolder) {
        String parentFirstName = Utils.getValue(pc.getColumnmaps(), ChildDBConstants.KEY.FAMILY_FIRST_NAME, true);
        String parentLastName = Utils.getValue(pc.getColumnmaps(), ChildDBConstants.KEY.FAMILY_LAST_NAME, true);
        String parentMiddleName = Utils.getValue(pc.getColumnmaps(), ChildDBConstants.KEY.FAMILY_MIDDLE_NAME, true);

        String parentName = context.getResources().getString(R.string.care_giver_initials) + ": " + getClientName(parentFirstName, parentMiddleName, parentLastName);
        String firstName = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true);
        String middleName = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.MIDDLE_NAME, true);
        String lastName = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.LAST_NAME, true);
        String childName = getClientName(firstName, middleName, lastName);

        fillValue(viewHolder.textViewParentName, WordUtils.capitalize(parentName));

        String dobString = getDuration(Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.DOB, false));
        //dobString = dobString.contains("y") ? dobString.substring(0, dobString.indexOf("y")) : dobString;
        fillChildNameAndAge(viewHolder, childName, dobString);
        setAddressAndGender(pc, viewHolder);

        addButtonClickListeners(client, viewHolder);

    }

    private void populateLastColumn(CommonPersonObjectClient pc, RegisterViewHolder viewHolder) {
        Utils.startAsyncTask(new ChwUpdateLastAsyncTask(context, commonRepository, viewHolder, pc.entityId(), onClickListener), null);
    }

}
