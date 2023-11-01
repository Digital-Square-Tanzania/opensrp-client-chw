package org.smartregister.chw.fragment;

import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUE;
import static org.smartregister.chw.core.utils.FormUtils.getStartFormActivity;
import static org.smartregister.chw.util.Constants.JsonFormConstants.STEP1;
import static org.smartregister.util.Utils.getAllSharedPreferences;

import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.activity.OrderDetailsActivity;
import org.smartregister.chw.cdp.util.Constants;
import org.smartregister.chw.core.fragment.CoreOrdersRegisterFragment;
import org.smartregister.chw.presenter.OrdersRegisterFragmentPresenter;
import org.smartregister.chw.referral.util.JsonFormConstants;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import timber.log.Timber;

public class OrdersRegisterFragment extends CoreOrdersRegisterFragment {

    /**
     * Starts the CDP condom order form
     */
    @Override
    public void startOrderForm() {
        try {
            // Get the order form as a JSONObject
            JSONObject form = model().getOrderFormAsJson(Constants.FORMS.CDP_CONDOM_ORDER);

            // Get the user's name from shared preferences
            String userName = getAllSharedPreferences().getPreference("anmIdentifier");
            String providerFullName = getAllSharedPreferences().getPreference(userName);

            // Get the fields of the form
            JSONArray fields = form.getJSONObject(STEP1).getJSONArray(JsonFormConstants.FIELDS);

            // Get the "requester" field and set its value to the provider's full name
            JSONObject requesterField = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "requester");
            requesterField.put(VALUE, providerFullName);

            // Create an intent to start the form activity
            Intent startFormIntent = getStartFormActivity(form, null, requireActivity());

            // Start the form activity for result
            requireActivity().startActivityForResult(startFormIntent, JsonFormUtils.REQUEST_CODE_GET_JSON);
        } catch (Exception e) {
            // Print the stack trace of the exception
            Timber.e(e);
        }
    }

    @Override
    public void showDetails(CommonPersonObjectClient cp) {
        OrderDetailsActivity.startMe(requireActivity(), cp);
    }

    @Override
    protected void initializePresenter() {
        presenter = new OrdersRegisterFragmentPresenter(this, model());
    }


}
