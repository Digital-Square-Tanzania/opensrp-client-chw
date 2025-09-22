package org.smartregister.chw.provider;

import android.content.Context;
import android.database.Cursor;
import android.view.View;

import org.jeasy.rules.api.Rules;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.model.ChildVisit;
import org.smartregister.chw.core.provider.CoreRegisterProvider;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.view.contract.SmartRegisterClient;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HpsHouseholdRegisterProvider is a concrete implementation of {@link CoreRegisterProvider}
 * specifically tailored for displaying household register data within a HpsHousehold Register Fragment,
 * It extends the base functionality to include specific behaviors and customizations required for
 * managing and displaying household information.
 *
 * <p>
 * This provider is responsible for:
 * <ul>
 *   <li>Initializing with application context, common repository, visible columns, and click listeners.</li>
 *   <li>Customizing the view of each row in the household register ({@link #getView}).</li>
 *   <li>Handling logic related to due status columns ({@link #updateDueColumn}).
 *   <li>Retrieving and managing child visit data ({@link #retrieveChildVisitList} and {@link #mergeChildVisits}).</li>
 *   <li>Defining age-based filters for children ({@link #getChildAgeLimitFilter}).</li>
 * </ul>
 * </p>
 */
public class HpsHouseholdRegisterProvider extends CoreRegisterProvider {
    protected final Context context;

    public HpsHouseholdRegisterProvider(Context context, CommonRepository commonRepository, Set visibleColumns, View.OnClickListener onClickListener, View.OnClickListener paginationClickListener) {
        super(context, commonRepository, visibleColumns, onClickListener, paginationClickListener);
        this.context = context;
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, RegisterViewHolder viewHolder) {
        super.getView(cursor, client, viewHolder);
        viewHolder.dueWrapper.setVisibility(View.GONE);
    }

    @Override
    public void updateDueColumn(Context context, RegisterViewHolder viewHolder, ChildVisit childVisit) {

    }

    @Override
    public List<ChildVisit> retrieveChildVisitList(Rules rules, List<Map<String, String>> list) {
        return null;
    }

    @Override
    public ChildVisit mergeChildVisits(List<ChildVisit> childVisitList) {
        return null;
    }

    @Override
    public String getChildAgeLimitFilter() {
        if (ChwApplication.getApplicationFlavor().showIconsForChildrenUnderTwoAndGirlsAgeNineToEleven()) {
            return org.smartregister.chw.util.ChildDBConstants.childDueVaccinesFilterForChildrenBelowTwoAndGirlsAgeNineToEleven();
        } else {
            return ChildDBConstants.childAgeLimitFilter();
        }
    }

}
