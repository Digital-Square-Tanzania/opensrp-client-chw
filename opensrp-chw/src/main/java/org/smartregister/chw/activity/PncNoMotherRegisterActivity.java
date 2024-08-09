package org.smartregister.chw.activity;

import static com.vijay.jsonwizard.utils.FormUtils.fields;
import static com.vijay.jsonwizard.utils.FormUtils.getFieldJSONObject;
import static org.smartregister.chw.util.Constants.Events.PNC_NO_MOTHER_CHILD_REGISTRATION;
import static org.smartregister.chw.pmtct.util.NCUtils.getClientProcessorForJava;
import static org.smartregister.chw.pmtct.util.NCUtils.getSyncHelper;
import static org.smartregister.family.util.JsonFormUtils.STEP2;
import static org.smartregister.util.JsonFormUtils.STEP1;
import static org.smartregister.util.Utils.getAllSharedPreferences;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.core.utils.CoreReferralUtils;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.R;
import org.smartregister.chw.util.AllClientsUtils;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.Utils;
import org.smartregister.opd.OpdLibrary;
import org.smartregister.opd.pojo.OpdEventClient;
import org.smartregister.opd.pojo.RegisterParams;
import org.smartregister.opd.utils.OpdConstants;
import org.smartregister.opd.utils.OpdJsonFormUtils;
import org.smartregister.opd.utils.OpdUtils;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.UniqueIdRepository;
import org.smartregister.util.JsonFormUtils;
import org.smartregister.view.activity.SecuredActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

public class PncNoMotherRegisterActivity extends SecuredActivity {
    private static String formName;

    public static void startPncNoMotherRegistrationActivity(Activity activity, String formName) {
        Intent intent = new Intent(activity, PncNoMotherRegisterActivity.class);
        PncNoMotherRegisterActivity.formName = formName;
        activity.startActivity(intent);
    }

    @Override
    protected void onCreation() {
        startFormActivity(FormUtils.getFormUtils().getFormJson(formName));
    }

    @Override
    protected void onResumption() {
        //do nothing
    }

    public UniqueIdRepository getUniqueIdRepository() {
        return OpdLibrary.getInstance().getUniqueIdRepository();
    }

    public void startFormActivity(JSONObject jsonForm) {
        try {
            String entityId = getUniqueIdRepository().getNextUniqueId().getOpenmrsId();
            String newEntityId = entityId;
            if (StringUtils.isNotBlank(entityId)) {
                newEntityId = entityId.replace("-", "");
            }

            JSONObject stepOneUniqueId = getFieldJSONObject(fields(jsonForm, STEP1), Constants.JSON_FORM_KEY.UNIQUE_ID);

            if (stepOneUniqueId != null) {
                stepOneUniqueId.remove(org.smartregister.family.util.JsonFormUtils.VALUE);
                stepOneUniqueId.put(org.smartregister.family.util.JsonFormUtils.VALUE, newEntityId + "_Family");
            }

            JSONObject stepTwoUniqueId = getFieldJSONObject(fields(jsonForm, STEP2), Constants.JSON_FORM_KEY.UNIQUE_ID);
            if (stepTwoUniqueId != null) {
                stepTwoUniqueId.remove(org.smartregister.family.util.JsonFormUtils.VALUE);
                stepTwoUniqueId.put(org.smartregister.family.util.JsonFormUtils.VALUE, newEntityId);
            }

            Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
            intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
            Form form = new Form();
            form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

            form.setWizard(true);
            form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
            form.setName(getString(R.string.no_mother_child_reg));
            form.setNextLabel(this.getResources().getString(org.smartregister.chw.core.R.string.next));
            form.setPreviousLabel(this.getResources().getString(org.smartregister.chw.core.R.string.back));


            startActivityForResult(intent, org.smartregister.family.util.JsonFormUtils.REQUEST_CODE_GET_JSON);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            Intent intent = new Intent(this, PncRegisterActivity.class);
            startActivity(intent);
        }
        if (requestCode == org.smartregister.family.util.JsonFormUtils.REQUEST_CODE_GET_JSON && resultCode == RESULT_OK) {
            String jsonString = data.getStringExtra(OpdConstants.JSON_FORM_EXTRA.JSON);
            Timber.d("JSONResult : %s", jsonString);

            try {
                RegisterParams registerParam = new RegisterParams();
                registerParam.setEditMode(false);
                registerParam.setFormTag(OpdJsonFormUtils.formTag(OpdUtils.context().allSharedPreferences()));
                saveForm(jsonString, registerParam);
                startActivity(new Intent(this, PncRegisterActivity.class));
            } catch (Exception e) {
                Timber.e(e);
            }
        }

    }

    private void saveForm(String jsonString, RegisterParams registerParam) {
        try {
            List<OpdEventClient> opdEventClientList = processRegistration(jsonString);
            if (opdEventClientList == null || opdEventClientList.isEmpty()) {
                return;
            }
            saveRegistration(opdEventClientList, jsonString, registerParam);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public List<OpdEventClient> processRegistration(String jsonString) {

        List<OpdEventClient> opdEventClients = AllClientsUtils.getOpdEventClients(jsonString);
        try {
            //set sync location the same as the facility
            for (OpdEventClient opdEventClient : opdEventClients) {
                AllSharedPreferences allSharedPreferences = getAllSharedPreferences();
                String anmIdentifier = allSharedPreferences.fetchRegisteredANM();
                opdEventClient.getEvent().setLocationId(allSharedPreferences.fetchUserLocalityId(anmIdentifier));
            }
        } catch (Exception e) {
            Timber.e(e, "Error retrieving Sync location Field");
        }
        return opdEventClients;
    }

    public void saveRegistration(@NonNull List<OpdEventClient> allClientEventList, @NonNull String jsonString,
                                 @NonNull RegisterParams params) {
        try {
            List<String> currentFormSubmissionIds = new ArrayList<>();

            for (int i = 0; i < allClientEventList.size(); i++) {
                try {

                    OpdEventClient allClientEvent = allClientEventList.get(i);
                    Client baseClient = allClientEvent.getClient();
                    Event baseEvent = allClientEvent.getEvent();
                    addClient(params, baseClient);
                    addEvent(params, currentFormSubmissionIds, baseEvent);
                    updateOpenSRPId(jsonString, params, baseClient);
                    addImageLocation(jsonString, baseClient, baseEvent);
                    if (baseEvent.getEventType().equalsIgnoreCase("Family Member Registration"))
                        createPncRegistrationEvent(baseEvent.getBaseEntityId(), jsonString);
                } catch (Exception e) {
                    Timber.e(e, "ChwAllClientRegisterInteractor --> saveRegistration");
                }
            }

            long lastSyncTimeStamp = getAllSharedPreferences().fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            getClientProcessorForJava().processClient(getSyncHelper().getEvents(currentFormSubmissionIds));
            getAllSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
        } catch (Exception e) {
            Timber.e(e, "OpdRegisterInteractor --> saveRegistration");
        }
    }

    private void addClient(@NonNull RegisterParams params, Client baseClient) throws JSONException {
        JSONObject clientJson = new JSONObject(OpdJsonFormUtils.gson.toJson(baseClient));
        if (params.isEditMode()) {
            try {
                org.smartregister.family.util.JsonFormUtils.mergeAndSaveClient(getSyncHelper(), baseClient);
            } catch (Exception e) {
                Timber.e(e, "ChwAllClientRegisterInteractor --> mergeAndSaveClient");
            }
        } else {
            getSyncHelper().addClient(baseClient.getBaseEntityId(), clientJson);
        }
    }

    private void addImageLocation(String jsonString, Client baseClient, Event baseEvent) {
        if (baseClient != null || baseEvent != null) {
            String imageLocation = OpdJsonFormUtils.getFieldValue(jsonString, Constants.KEY.PHOTO);
            if (StringUtils.isNotBlank(imageLocation)) {
                org.smartregister.family.util.JsonFormUtils.saveImage(baseEvent.getProviderId(), baseClient.getBaseEntityId(), imageLocation);
            }
        }
    }

    private void updateOpenSRPId(String jsonString, RegisterParams params, Client baseClient) {
        if (params.isEditMode()) {
            // UnAssign current OpenSRP ID
            if (baseClient != null) {
                String newOpenSrpId = baseClient.getIdentifier(Utils.metadata().uniqueIdentifierKey).replace("-", "");
                String currentOpenSrpId = org.smartregister.family.util.JsonFormUtils.getString(jsonString, org.smartregister.family.util.JsonFormUtils.CURRENT_OPENSRP_ID).replace("-", "");
                if (!newOpenSrpId.equals(currentOpenSrpId)) {
                    //OpenSRP ID was changed
                    getUniqueIdRepository().open(currentOpenSrpId);
                }
            }

        } else {
            if (baseClient != null) {
                String openSrpId = baseClient.getIdentifier(Utils.metadata().uniqueIdentifierKey);
                if (StringUtils.isNotBlank(openSrpId) && !openSrpId.contains(Constants.IDENTIFIER.FAMILY_SUFFIX)) {
                    //Mark OpenSRP ID as used
                    getUniqueIdRepository().close(openSrpId);
                }
            }
        }
    }

    private void addEvent(RegisterParams params, List<String> currentFormSubmissionIds, Event baseEvent) throws JSONException {
        if (baseEvent != null) {
            JSONObject eventJson = new JSONObject(OpdJsonFormUtils.gson.toJson(baseEvent));
            getSyncHelper().addEvent(baseEvent.getBaseEntityId(), eventJson, params.getStatus());
            currentFormSubmissionIds.add(eventJson.getString(EventClientRepository.event_column.formSubmissionId.toString()));
        }
    }

    private void createPncRegistrationEvent(String baseEntityId, String jsonString) {
        AllSharedPreferences sharedPreferences = getAllSharedPreferences();
        Event baseEvent = org.smartregister.chw.anc.util.JsonFormUtils.processJsonForm(sharedPreferences, CoreReferralUtils.setEntityId(jsonString, baseEntityId), org.smartregister.chw.util.Constants.TableName.CHILD_NO_MOTHER);

        baseEvent.setEventType(PNC_NO_MOTHER_CHILD_REGISTRATION);
        baseEvent.setFormSubmissionId(JsonFormUtils.generateRandomUUIDString());
        baseEvent.setEntityType(org.smartregister.chw.util.Constants.TableName.CHILD_NO_MOTHER);

        // tag docs
        org.smartregister.chw.util.JsonFormUtils.tagSyncMetadata(Utils.context().allSharedPreferences(), baseEvent);
        try {
            NCUtils.processEvent(baseEvent.getBaseEntityId(), new JSONObject(org.smartregister.chw.anc.util.JsonFormUtils.gson.toJson(baseEvent)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
