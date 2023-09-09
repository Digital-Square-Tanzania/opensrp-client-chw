package org.smartregister.chw.activity;

import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static com.vijay.jsonwizard.constants.JsonFormConstants.STEP1;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUE;
import static com.vijay.jsonwizard.utils.FormUtils.fields;
import static com.vijay.jsonwizard.utils.FormUtils.getFieldJSONObject;
import static org.smartregister.chw.cdp.util.Constants.FORMS.EDIT_CDP_OUTLET;
import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.cdp.CdpLibrary;
import org.smartregister.chw.cdp.dao.CdpDao;
import org.smartregister.chw.cdp.domain.Visit;
import org.smartregister.chw.cdp.pojo.RegisterParams;
import org.smartregister.chw.cdp.util.CdpJsonFormUtils;
import org.smartregister.chw.cdp.util.Constants;
import org.smartregister.chw.cdp.util.VisitUtils;
import org.smartregister.chw.core.activity.CoreCdpProfileActivity;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import timber.log.Timber;

public class CdpProfileActivity extends CoreCdpProfileActivity {

    public static void startProfile(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, CdpProfileActivity.class);
        passToolbarTitle(activity, intent);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        activity.startActivity(intent);
    }

    public static void disableOrEnableOutlet(AllSharedPreferences allSharedPreferences, String baseEntityId, String eventType) {
        Event event = (Event) new Event().withBaseEntityId(baseEntityId).withEventDate(new Date()).withEventType(eventType).withLocationId(org.smartregister.chw.anc.util.JsonFormUtils.locationId(allSharedPreferences)).withProviderId(allSharedPreferences.fetchRegisteredANM()).withEntityType(Constants.TABLES.CDP_OUTLET).withFormSubmissionId(UUID.randomUUID().toString()).withDateCreated(new Date());
        try {
            NCUtils.processEvent(event.getBaseEntityId(), new JSONObject(org.smartregister.chw.anc.util.JsonFormUtils.gson.toJson(event)));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    protected void startRestockingHistory() {
        RestockingVisitHistoryActivity.startMe(this, outletObject);
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        tvLastVisitTitle.setText(R.string.restock_history);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupViews();
        outletObject = CdpDao.getOutlet(outletObject.getBaseEntityId());
        initializePresenter();
        updateFollowupButton();

        try {
            List<Visit> restockVisits = VisitUtils.getVisits(outletObject.getBaseEntityId());
            if (restockVisits.size() > 0) {
                Date lastRestockDate = restockVisits.get(restockVisits.size() - 1).getDate();
                tvLastVisitSub.setText(String.format(this.getString(R.string.last_restock_date), new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(lastRestockDate)));
            } else {
                tvLastVisitSub.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Timber.e(e);
            tvLastVisitSub.setVisibility(View.GONE);
        }

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.cdp_outlet_profile_menu, menu);

        if (outletObject.isClosed()) {
            menu.findItem(R.id.action_disable_outlet).setVisible(false);
            menu.findItem(R.id.action_edit_outlet).setVisible(false);
            menu.findItem(R.id.action_enable_outlet).setVisible(true);
        } else {
            menu.findItem(R.id.action_edit_outlet).setVisible(true);
            menu.findItem(R.id.action_disable_outlet).setVisible(true);
            menu.findItem(R.id.action_enable_outlet).setVisible(false);
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_edit_outlet) {
            this.startFormForEdit(R.string.registration_info, EDIT_CDP_OUTLET);
            return true;
        } else if (itemId == R.id.action_disable_outlet) {
            AllSharedPreferences allSharedPreferences = Utils.getAllSharedPreferences();
            disableOrEnableOutlet(allSharedPreferences, outletObject.getBaseEntityId(), Constants.EVENT_TYPE.DISABLE_CDP_OUTLET);
            onResume();
            invalidateOptionsMenu();
            return true;
        } else if (itemId == R.id.action_enable_outlet) {
            AllSharedPreferences allSharedPreferences = Utils.getAllSharedPreferences();
            disableOrEnableOutlet(allSharedPreferences, outletObject.getBaseEntityId(), Constants.EVENT_TYPE.ENABLE_CDP_OUTLET);
            onResume();
            invalidateOptionsMenu();
            return true;
        }
        return false;
    }

    /**
     * This method starts the form for edit with the given title resource and form name.
     *
     * @param title_resource The resource ID of the form's title
     * @param formName       The name of the form
     */
    public void startFormForEdit(Integer title_resource, String formName) {
        try {
            JSONObject form = null;
            if (formName.equals(EDIT_CDP_OUTLET)) {
                form = CoreJsonFormUtils.getAutoJsonEditAncFormString(outletObject.getBaseEntityId(), CdpProfileActivity.this, formName, Constants.EVENT_TYPE.UPDATE_CDP_OUTLET_REGISTRATION, getResources().getString(title_resource));

                JSONArray fields = form.getJSONObject(STEP1).getJSONArray(FIELDS);

                // Populate the form fields with the outlet data
                JSONObject uniqueIdObj = JsonFormUtils.getFieldJSONObject(fields, "unique_id");
                uniqueIdObj.put(VALUE, outletObject.getOutletId());

                JSONObject outletNameObj = JsonFormUtils.getFieldJSONObject(fields, "outlet_name");
                outletNameObj.put(VALUE, outletObject.getOutletName());

                JSONObject outletWardNameObj = JsonFormUtils.getFieldJSONObject(fields, "outlet_ward_name");
                outletWardNameObj.put(VALUE, outletObject.getOutletWardName());

                JSONObject outletVillageStreetNameObj = JsonFormUtils.getFieldJSONObject(fields, "outlet_village_street_name");
                outletVillageStreetNameObj.put(VALUE, outletObject.getOutletWardName());

                JSONObject focalPersonNameObj = JsonFormUtils.getFieldJSONObject(fields, "focal_person_name");
                focalPersonNameObj.put(VALUE, outletObject.getFocalPersonName());

                JSONObject focalPersonPhoneObj = JsonFormUtils.getFieldJSONObject(fields, "focal_person_phone");
                focalPersonPhoneObj.put(VALUE, outletObject.getFocalPersonNumber());

                JSONObject outletTypeObj = JsonFormUtils.getFieldJSONObject(fields, "outlet_type");
                outletTypeObj.put(VALUE, outletObject.getOutletType());

                JSONObject otherOutletTypeObj = JsonFormUtils.getFieldJSONObject(fields, "other_outlet_type");
                otherOutletTypeObj.put(VALUE, outletObject.getOtherOutletType());

                // Start the form activity
                startActivityForResult(org.smartregister.chw.util.JsonFormUtils.getAncPncStartFormIntent(form, this), JsonFormUtils.REQUEST_CODE_GET_JSON);

            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE_GET_JSON && resultCode == RESULT_OK) {
            String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                String encounter_type = jsonObject.getString("encounter_type");
                if (encounter_type.equalsIgnoreCase(Constants.EVENT_TYPE.UPDATE_CDP_OUTLET_REGISTRATION)) {
                    RegisterParams registerParam = new RegisterParams();
                    registerParam.setEditMode(true);
                    registerParam.setFormTag(CdpJsonFormUtils.formTag(CdpLibrary.getInstance().context().allSharedPreferences()));
                    showProgressDialog(org.smartregister.cdp.R.string.saving_dialog_title);
                    profilePresenter.saveForm(jsonString, registerParam);
                    finish();
                } else if (encounter_type.equalsIgnoreCase(Constants.EVENT_TYPE.CDP_OUTLET_VISIT)) {
                    JSONObject restockTheOutlet = getFieldJSONObject(fields(jsonObject, org.smartregister.util.JsonFormUtils.STEP1), "restock_the_outlet");
                    if (restockTheOutlet != null && restockTheOutlet.has(VALUE) && restockTheOutlet.getString(VALUE).equalsIgnoreCase("yes")) {
                        startRestockingHistory();
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

}
