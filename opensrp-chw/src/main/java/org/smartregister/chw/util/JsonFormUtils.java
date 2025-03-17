package org.smartregister.chw.util;

import static com.vijay.jsonwizard.constants.JsonFormConstants.EDITABLE;
import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static com.vijay.jsonwizard.constants.JsonFormConstants.READ_ONLY;

import static org.smartregister.client.utils.constants.JsonFormConstants.STEP1;

import android.content.Context;
import android.util.Pair;

import com.google.common.reflect.TypeToken;
import com.nerdstone.neatformcore.domain.model.NFormViewData;
import com.vijay.jsonwizard.constants.JsonFormConstants;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.domain.FamilyMember;
import org.smartregister.chw.core.domain.ParentClient;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Photo;
import org.smartregister.domain.form.FormLocation;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.family.FamilyLibrary;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.DBConstants;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.util.AssetHandler;
import org.smartregister.util.FormUtils;
import org.smartregister.util.ImageUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import timber.log.Timber;

/**
 * Created by keyman on 13/11/2018.
 */
public class JsonFormUtils extends CoreJsonFormUtils {
    public static final String METADATA = "metadata";
    public static final String ENCOUNTER_TYPE = "encounter_type";
    public static final int REQUEST_CODE_GET_JSON = 2244;
    public static final int REQUEST_CODE_GET_JSON_WASH = 22444;
    public static final int REQUEST_CODE_GET_JSON_FAMILY_KIT = 22447;
    public static final int REQUEST_CODE_GET_JSON_HOUSEHOLD = 22445;

    public static final String CURRENT_OPENSRP_ID = "current_opensrp_id";
    public static final String READ_ONLY = "read_only";
    private static Flavor flavor = new JsonFormUtilsFlv();

    public static Event tagSyncMetadata(AllSharedPreferences allSharedPreferences, Event event) {
        String providerId = allSharedPreferences.fetchRegisteredANM();
        event.setProviderId(providerId);
        event.setLocationId(locationId(allSharedPreferences));
        event.setChildLocationId(allSharedPreferences.fetchCurrentLocality());
        event.setTeam(allSharedPreferences.fetchDefaultTeam(providerId));
        event.setTeamId(allSharedPreferences.fetchDefaultTeamId(providerId));

        event.setClientApplicationVersion(FamilyLibrary.getInstance().getApplicationVersion());
        event.setClientDatabaseVersion(FamilyLibrary.getInstance().getDatabaseVersion());

        return event;
    }

    public static Pair<Client, Event> processChildRegistrationForm(AllSharedPreferences allSharedPreferences, String jsonString) {

        try {
            Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(jsonString);

            if (!registrationFormParams.getLeft()) {
                return null;
            }

            JSONObject jsonForm = registrationFormParams.getMiddle();
            JSONArray fields = registrationFormParams.getRight();

            String entityId = getString(jsonForm, ENTITY_ID);
            if (StringUtils.isBlank(entityId)) {
                entityId = generateRandomUUIDString();
            }

            lastInteractedWith(fields);

            dobUnknownUpdateFromAge(fields);

            processChildEnrollMent(jsonForm, fields);

            Client baseClient = org.smartregister.util.JsonFormUtils.createBaseClient(fields, formTag(allSharedPreferences), entityId);

            Event baseEvent = org.smartregister.util.JsonFormUtils.createEvent(fields, getJSONObject(jsonForm, METADATA), formTag(allSharedPreferences), entityId, getString(jsonForm, ENCOUNTER_TYPE), CoreConstants.TABLE_NAME.CHILD);
            tagSyncMetadata(allSharedPreferences, baseEvent);

            if (baseClient != null || baseEvent != null) {
                String imageLocation = org.smartregister.family.util.JsonFormUtils.getFieldValue(jsonString, Constants.KEY.PHOTO);
                org.smartregister.family.util.JsonFormUtils.saveImage(baseEvent.getProviderId(), baseClient.getBaseEntityId(), imageLocation);
            }

            JSONObject lookUpJSONObject = getJSONObject(getJSONObject(jsonForm, METADATA), "look_up");
            String lookUpEntityId = "";
            String lookUpBaseEntityId = "";
            if (lookUpJSONObject != null) {
                lookUpEntityId = getString(lookUpJSONObject, "entity_id");
                lookUpBaseEntityId = getString(lookUpJSONObject, "value");
            }
            if (lookUpEntityId.equals("family") && StringUtils.isNotBlank(lookUpBaseEntityId)) {
                ParentClient parentClient = new ParentClient(lookUpBaseEntityId);
                parentClient.setMotherBaseEntityId(motherBaseEntityId(baseClient.getBaseEntityId()));
                Context context = ChwApplication.getInstance().getContext().applicationContext();
                addRelationship(context, parentClient, baseClient);
                SQLiteDatabase db = ChwApplication.getInstance().getRepository().getReadableDatabase();
                EventClientRepository eventClientRepository = new EventClientRepository();
                JSONObject clientjson = eventClientRepository.getClient(db, lookUpBaseEntityId);
                baseClient.setAddresses(getAddressFromClientJson(clientjson));
            }


            return Pair.create(baseClient, baseEvent);
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    private static String motherBaseEntityId(String baseEntityId) {
        try {
            EventClientRepository eventClientRepository = new EventClientRepository();
            ECSyncHelper syncHelper = ChwApplication.getInstance().getEcSyncHelper();
            JSONObject object = eventClientRepository.getClientByBaseEntityId(baseEntityId);
            Client client = syncHelper.convert(object, Client.class);
            List<String> motherList = client.getRelationships().get("mother");
            if (motherList != null) {
                return motherList.get(0);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static Triple<Boolean, JSONObject, JSONArray> validateParameters(String jsonString) {

        JSONObject jsonForm = toJSONObject(jsonString);
        JSONArray fields = fields(jsonForm);

        return Triple.of(jsonForm != null && fields != null, jsonForm, fields);
    }

    private static void processChildEnrollMent(JSONObject jsonForm, JSONArray fields) {

        try {

            JSONObject surnam_familyName_SameObject = getFieldJSONObject(fields, "surname_same_as_family_name");
            JSONArray surnam_familyName_Same_options = getJSONArray(surnam_familyName_SameObject, org.smartregister.family.util.Constants.JSON_FORM_KEY.OPTIONS);
            JSONObject surnam_familyName_Same_option = getJSONObject(surnam_familyName_Same_options, 0);
            String surnam_familyName_SameString = surnam_familyName_Same_option != null ? surnam_familyName_Same_option.getString(VALUE) : null;

            if (StringUtils.isNotBlank(surnam_familyName_SameString) && Boolean.valueOf(surnam_familyName_SameString)) {
                String familyId = jsonForm.getJSONObject("metadata").getJSONObject("look_up").getString("value");
                CommonPersonObject familyObject = ChwApplication.getInstance().getContext().commonrepository("ec_family").findByCaseID(familyId);
                if (ChwApplication.getApplicationFlavor().hasSurname()) {
                    String lastname = familyObject.getColumnmaps().get(DBConstants.KEY.LAST_NAME);
                    JSONObject surname_object = getFieldJSONObject(fields, "surname");
                    surname_object.put(VALUE, lastname);
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }

    }

    private static String processValueWithChoiceIds(JSONObject jsonObject, String value) {
        try {
            //spinner
            if (jsonObject.has("openmrs_choice_ids")) {
                JSONObject choiceObject = jsonObject.getJSONObject("openmrs_choice_ids");

                for (int i = 0; i < choiceObject.names().length(); i++) {
                    if (value.equalsIgnoreCase(choiceObject.getString(choiceObject.names().getString(i)))) {
                        value = choiceObject.names().getString(i);
                    }
                }


            }//checkbox
            else if (jsonObject.has(Constants.JSON_FORM_KEY.OPTIONS)) {
                JSONArray option_array = jsonObject.getJSONArray(Constants.JSON_FORM_KEY.OPTIONS);
                for (int i = 0; i < option_array.length(); i++) {
                    JSONObject option = option_array.getJSONObject(i);
                    if (value.contains(option.getString("key"))) {
                        option.put("value", "true");
                    }
                }
            }

        } catch (Exception e) {
            Timber.e(e);
        }
        return value;
    }

    protected static void processPopulatableFields(CommonPersonObjectClient client, JSONObject jsonObject) throws JSONException {

        switch (jsonObject.getString(org.smartregister.family.util.JsonFormUtils.KEY).toLowerCase()) {
            case Constants.JSON_FORM_KEY.DOB_UNKNOWN:
                jsonObject.put(org.smartregister.family.util.JsonFormUtils.READ_ONLY, false);
                JSONObject optionsObject = jsonObject.getJSONArray(Constants.JSON_FORM_KEY.OPTIONS).getJSONObject(0);
                optionsObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), Constants.JSON_FORM_KEY.DOB_UNKNOWN, false));

                break;
            case DBConstants.KEY.DOB:

                String dobString = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.DOB, false);
                if (StringUtils.isNotBlank(dobString)) {
                    Date dob = Utils.dobStringToDate(dobString);
                    if (dob != null) {
                        jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, dd_MM_yyyy.format(dob));
                    }
                }

                break;

            case Constants.KEY.PHOTO:

                Photo photo = ImageUtils.profilePhotoByClientID(client.getCaseId(), Utils.getProfileImageResourceIDentifier());
                if (StringUtils.isNotBlank(photo.getFilePath())) {
                    jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, photo.getFilePath());
                }

                break;

            case DBConstants.KEY.UNIQUE_ID:

                String uniqueId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.UNIQUE_ID, false);
                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, uniqueId.replace("-", ""));

                break;

            case "fam_name":

                String fam_name = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.FIRST_NAME, false);
                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, fam_name);

                break;

            case DBConstants.KEY.VILLAGE_TOWN:

                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.VILLAGE_TOWN, false));

                break;

            case DBConstants.KEY.QUATER_CLAN:

                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.QUATER_CLAN, false));

                break;

            case DBConstants.KEY.STREET:

                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.STREET, false));

                break;

            case DBConstants.KEY.LANDMARK:

                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.LANDMARK, false));

                break;

            case DBConstants.KEY.FAMILY_SOURCE_INCOME:

                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.FAMILY_SOURCE_INCOME, false));

                break;

            case ChwDBConstants.NEAREST_HEALTH_FACILITY:

                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), ChwDBConstants.NEAREST_HEALTH_FACILITY, false));

                break;

            case DBConstants.KEY.GPS:

                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.GPS, false));

                break;

            case ChwDBConstants.EVENT_DATE:

                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), ChwDBConstants.EVENT_DATE, false));

                break;

            default:

                Timber.e("ERROR:: Unprocessed Form Object Key " + jsonObject.getString(org.smartregister.family.util.JsonFormUtils.KEY));

                break;

        }

        if (jsonObject.getString(org.smartregister.family.util.JsonFormUtils.KEY).equalsIgnoreCase(DBConstants.KEY.DOB)) {

            jsonObject.put(org.smartregister.family.util.JsonFormUtils.READ_ONLY, false);
            JSONObject optionsObject = jsonObject.getJSONArray(Constants.JSON_FORM_KEY.OPTIONS).getJSONObject(0);
            optionsObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.DOB, false));

        }
    }

    public static Vaccine tagSyncMetadata(AllSharedPreferences allSharedPreferences, Vaccine vaccine) {
        String providerId = allSharedPreferences.fetchRegisteredANM();
        vaccine.setAnmId(providerId);
        vaccine.setLocationId(locationId(allSharedPreferences));
        vaccine.setChildLocationId(allSharedPreferences.fetchCurrentLocality());
        vaccine.setTeam(allSharedPreferences.fetchDefaultTeam(providerId));
        vaccine.setTeamId(allSharedPreferences.fetchDefaultTeamId(providerId));
        return vaccine;
    }

    public static ServiceRecord tagSyncMetadata(AllSharedPreferences allSharedPreferences, ServiceRecord serviceRecord) {
        String providerId = allSharedPreferences.fetchRegisteredANM();
        serviceRecord.setAnmId(providerId);
        serviceRecord.setLocationId(locationId(allSharedPreferences));
        serviceRecord.setChildLocationId(allSharedPreferences.fetchCurrentLocality());
        serviceRecord.setTeam(allSharedPreferences.fetchDefaultTeam(providerId));
        serviceRecord.setTeamId(allSharedPreferences.fetchDefaultTeamId(providerId));
        return serviceRecord;
    }

    /**
     * @param familyID
     * @param allSharedPreferences
     * @param jsonObject
     * @param providerId
     * @return Returns a triple object <b>DateOfDeath as String, BaseEntityID , List of Events </b>that should be processed
     */
    public static Triple<Pair<Date, String>, String, List<Event>> processRemoveMemberEvent(String familyID, AllSharedPreferences allSharedPreferences, JSONObject jsonObject, String providerId) {

        try {

            List<Event> events = new ArrayList<>();

            Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(jsonObject.toString());

            if (!registrationFormParams.getLeft()) {
                return null;
            }

            Date dod = null;


            JSONObject metadata = getJSONObject(registrationFormParams.getMiddle(), METADATA);
            String memberID = getString(registrationFormParams.getMiddle(), ENTITY_ID);

            JSONArray fields = new JSONArray();

            int x = 0;
            while (x < registrationFormParams.getRight().length()) {
                //JSONObject obj = registrationFormParams.getRight().getJSONObject(x);
                String myKey = registrationFormParams.getRight().getJSONObject(x).getString(KEY);

                if (myKey.equalsIgnoreCase(org.smartregister.chw.util.Constants.FORM_CONSTANTS.REMOVE_MEMBER_FORM.DATE_MOVED) ||
                        myKey.equalsIgnoreCase(org.smartregister.chw.util.Constants.FORM_CONSTANTS.REMOVE_MEMBER_FORM.REASON)
                ) {
                    fields.put(registrationFormParams.getRight().get(x));
                }
                if (myKey.equalsIgnoreCase(org.smartregister.chw.util.Constants.FORM_CONSTANTS.REMOVE_MEMBER_FORM.DATE_DIED)) {
                    fields.put(registrationFormParams.getRight().get(x));
                    try {
                        dod = dd_MM_yyyy.parse(registrationFormParams.getRight().getJSONObject(x).getString(VALUE));
                    } catch (Exception e) {
                        Timber.d(e.toString());
                    }
                }
                x++;
            }

            String encounterType = getString(jsonObject, ENCOUNTER_TYPE);

            String eventType;
            String tableName;

            if (encounterType.equalsIgnoreCase(org.smartregister.chw.util.Constants.EventType.REMOVE_CHILD)) {
                eventType = org.smartregister.chw.util.Constants.EventType.REMOVE_CHILD;
                tableName = org.smartregister.chw.util.Constants.TABLE_NAME.CHILD;
            } else if (encounterType.equalsIgnoreCase(org.smartregister.chw.util.Constants.EventType.REMOVE_FAMILY)) {
                eventType = org.smartregister.chw.util.Constants.EventType.REMOVE_FAMILY;
                tableName = org.smartregister.chw.util.Constants.TABLE_NAME.FAMILY;
            } else {
                eventType = org.smartregister.chw.util.Constants.EventType.REMOVE_MEMBER;
                tableName = org.smartregister.chw.util.Constants.TABLE_NAME.FAMILY_MEMBER;
            }

            Event eventMember = JsonFormUtils.createEvent(fields, metadata, formTag(allSharedPreferences), memberID,
                    eventType,
                    tableName
            );
            JsonFormUtils.tagSyncMetadata(Utils.context().allSharedPreferences(), eventMember);
            events.add(eventMember);


            return Triple.of(Pair.create(dod, encounterType), memberID, events);
        } catch (Exception e) {
            Timber.e(e.toString());
            return null;
        }
    }

    public static FamilyMember getFamilyMemberFromRegistrationForm(String jsonString, String familyBaseEntityId, String entityID) throws JSONException {
        FamilyMember member = new FamilyMember();

        Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(jsonString);
        if (!registrationFormParams.getLeft()) {
            return null;
        }

        JSONArray fields = registrationFormParams.getRight();

        member.setFamilyID(familyBaseEntityId);
        member.setMemberID(entityID);
        member.setPhone(getJsonFieldValue(fields, org.smartregister.chw.util.Constants.JsonAssets.FAMILY_MEMBER.PHONE_NUMBER));
        member.setOtherPhone(getJsonFieldValue(fields, org.smartregister.chw.util.Constants.JsonAssets.FAMILY_MEMBER.OTHER_PHONE_NUMBER));
        member.setEduLevel(getJsonFieldValue(fields, org.smartregister.chw.util.Constants.JsonAssets.FAMILY_MEMBER.HIGHEST_EDUCATION_LEVEL));
        member.setPrimaryCareGiver(
                getJsonFieldValue(fields, org.smartregister.chw.util.Constants.JsonAssets.PRIMARY_CARE_GIVER).equalsIgnoreCase("Yes") ||
                        getJsonFieldValue(fields, org.smartregister.chw.util.Constants.JsonAssets.IS_PRIMARY_CARE_GIVER).equalsIgnoreCase("Yes")
        );
        member.setFamilyHead(false);

        return member;
    }

    public static Pair<List<Client>, List<Event>> processFamilyUpdateRelations(Context context, FamilyMember familyMember, String lastLocationId) throws Exception {
        List<Client> clients = new ArrayList<>();
        List<Event> events = new ArrayList<>();


        ECSyncHelper syncHelper = ChwApplication.getInstance().getEcSyncHelper();
        JSONObject clientObject = syncHelper.getClient(familyMember.getFamilyID());
        Client familyClient = syncHelper.convert(clientObject, Client.class);
        if (familyClient == null) {
            String birthDate = clientObject.getString("birthdate");
            if (StringUtils.isNotBlank(birthDate)) {
                birthDate = birthDate.replace("-00:44:30", getTimeZone());
                clientObject.put("birthdate", birthDate);
            }

            familyClient = syncHelper.convert(clientObject, Client.class);
        }

        Map<String, List<String>> relationships = familyClient.getRelationships();

        if (familyMember.getPrimaryCareGiver()) {
            relationships.put(org.smartregister.chw.util.Constants.RELATIONSHIP.PRIMARY_CAREGIVER, toStringList(familyMember.getMemberID()));
            familyClient.setRelationships(relationships);
        }

        if (familyMember.getFamilyHead()) {
            relationships.put(org.smartregister.chw.util.Constants.RELATIONSHIP.FAMILY_HEAD, toStringList(familyMember.getMemberID()));
            familyClient.setRelationships(relationships);
        }

        clients.add(familyClient);


        JSONObject metadata = FormUtils.getInstance(context)
                .getFormJson(Utils.metadata().familyRegister.formName)
                .getJSONObject(org.smartregister.family.util.JsonFormUtils.METADATA);

        metadata.put(org.smartregister.family.util.JsonFormUtils.ENCOUNTER_LOCATION, lastLocationId);

        FormTag formTag = new FormTag();
        formTag.providerId = Utils.context().allSharedPreferences().fetchRegisteredANM();
        formTag.appVersion = FamilyLibrary.getInstance().getApplicationVersion();
        formTag.databaseVersion = FamilyLibrary.getInstance().getDatabaseVersion();

        Event eventFamily = JsonFormUtils.createEvent(new JSONArray(), metadata, formTag, familyMember.getFamilyID(),
                org.smartregister.chw.util.Constants.EventType.UPDATE_FAMILY_RELATIONS,
                Utils.metadata().familyRegister.tableName);
        JsonFormUtils.tagSyncMetadata(Utils.context().allSharedPreferences(), eventFamily);


        Event eventMember = JsonFormUtils.createEvent(new JSONArray(), metadata, formTag, familyMember.getMemberID(), org.smartregister.chw.util.Constants.EventType.UPDATE_FAMILY_MEMBER_RELATIONS,
                Utils.metadata().familyMemberRegister.tableName);
        JsonFormUtils.tagSyncMetadata(Utils.context().allSharedPreferences(), eventMember);

        eventMember.addObs(new Obs("concept", "text", org.smartregister.chw.util.Constants.FORM_CONSTANTS.CHANGE_CARE_GIVER.PHONE_NUMBER.CODE, "",
                toList(familyMember.getPhone()), new ArrayList<>(), null, DBConstants.KEY.PHONE_NUMBER));

        eventMember.addObs(new Obs("concept", "text", org.smartregister.chw.util.Constants.FORM_CONSTANTS.CHANGE_CARE_GIVER.OTHER_PHONE_NUMBER.CODE, org.smartregister.chw.util.Constants.FORM_CONSTANTS.CHANGE_CARE_GIVER.OTHER_PHONE_NUMBER.PARENT_CODE,
                toList(familyMember.getOtherPhone()), new ArrayList<>(), null, DBConstants.KEY.OTHER_PHONE_NUMBER));

        eventMember.addObs(new Obs("concept", "text", org.smartregister.chw.util.Constants.FORM_CONSTANTS.CHANGE_CARE_GIVER.HIGHEST_EDU_LEVEL.CODE, "",
                toList(getEducationLevels(context).get(familyMember.getEduLevel())), toList(familyMember.getEduLevel()), null, DBConstants.KEY.HIGHEST_EDU_LEVEL));


        events.add(eventFamily);
        events.add(eventMember);

        return Pair.create(clients, events);
    }

    public static String getTimeZone() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"),
                Locale.getDefault());
        Date currentLocalTime = calendar.getTime();
        DateFormat date = new SimpleDateFormat("Z");
        String localTime = date.format(currentLocalTime);
        return localTime.substring(0, 3) + ":" + localTime.substring(3, 5);
    }

    /**
     * Returns a value from json form field
     *
     * @param jsonObject native forms jsonObject
     * @param key        field object key
     * @return value
     */
    public static String getValue(JSONObject jsonObject, String key) {
        try {
            JSONObject formField = com.vijay.jsonwizard.utils.FormUtils.getFieldFromForm(jsonObject, key);
            if (formField != null && formField.has(JsonFormConstants.VALUE)) {
                return formField.getString(JsonFormConstants.VALUE);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return "";
    }

    /**
     * Returns a value from a native forms checkbox field and returns an comma separated string
     *
     * @param jsonObject native forms jsonObject
     * @param key        field object key
     * @return value
     */
    public static String getCheckBoxValue(JSONObject jsonObject, String key) {
        try {
            JSONArray jsonArray = jsonObject.getJSONObject(JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);

            JSONObject jo = null;
            int x = 0;
            while (jsonArray.length() > x) {
                jo = jsonArray.getJSONObject(x);
                if (jo.getString(JsonFormConstants.KEY).equalsIgnoreCase(key)) {
                    break;
                }
                x++;
            }

            StringBuilder resBuilder = new StringBuilder();
            if (jo != null) {
                // read all the checkboxes
                JSONArray jaOptions = jo.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);
                int optionSize = jaOptions.length();
                int y = 0;
                while (optionSize > y) {
                    JSONObject options = jaOptions.getJSONObject(y);
                    if (options.has(JsonFormConstants.VALUE) && options.getBoolean(JsonFormConstants.VALUE)) {
                        resBuilder.append(options.getString(JsonFormConstants.TEXT)).append(", ");
                    }
                    y++;
                }

                String res = resBuilder.toString();
                res = (res.length() >= 2) ? res.substring(0, res.length() - 2) : "";
                return res;
            }

        } catch (Exception e) {
            Timber.e(e);
        }
        return "";
    }

    public static JSONObject getJson(Context context, String formName, String baseEntityID) throws Exception {
        String locationId = ChwApplication.getInstance().getContext().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
        JSONObject jsonObject = FormUtils.getInstance(context).getFormJson(formName);
        org.smartregister.chw.anc.util.JsonFormUtils.getRegistrationForm(jsonObject, baseEntityID, locationId);
        return jsonObject;
    }

    public static JSONObject getAutoPopulatedJsonEditMemberFormString(String title, String formName, Context context, CommonPersonObjectClient client, String eventType, String familyName, boolean isPrimaryCaregiver) {
        return flavor.getAutoJsonEditMemberFormString(title, formName, context, client, eventType, familyName, isPrimaryCaregiver);
    }

    public static void populatedJsonForm(@NotNull JSONObject jsonObject, @NotNull Map<String, String> valueMap) throws JSONException {
        Map<String, String> _valueMap = new HashMap<>(valueMap);
        int step = 1;
        while (jsonObject.has("step" + step)) {
            JSONObject jsonStepObject = jsonObject.getJSONObject("step" + step);
            JSONArray array = jsonStepObject.getJSONArray(JsonFormConstants.FIELDS);
            int position = 0;
            while (position < array.length() && _valueMap.size() > 0) {

                JSONObject object = array.getJSONObject(position);
                String key = object.getString(JsonFormConstants.KEY);

                if (_valueMap.containsKey(key)) {
                    object.put(JsonFormConstants.VALUE, _valueMap.get(key));
                    _valueMap.remove(key);
                }

                position++;
            }

            step++;
        }
    }

    public static List<Obs> getObsForNeatForm(HashMap<String, NFormViewData> detailsHashMap) {
        ArrayList<Obs> obs = new ArrayList<>();
        for (String key : detailsHashMap.keySet()) {
            NFormViewData viewData = detailsHashMap.get(key);
            Obs ob = new Obs();
            ob.setFormSubmissionField(key);
            if (viewData.getMetadata() != null) {
                if (viewData.getMetadata().containsKey(OPENMRS_ENTITY))
                    ob.setFieldType(viewData.getMetadata().get(OPENMRS_ENTITY).toString());
                if (viewData.getMetadata().containsKey(OPENMRS_ENTITY_ID))
                    ob.setFieldCode(viewData.getMetadata().get(OPENMRS_ENTITY_ID).toString());
                if (viewData.getMetadata().containsKey(OPENMRS_ENTITY_PARENT))
                    ob.setParentCode(viewData.getMetadata().get(OPENMRS_ENTITY_PARENT).toString());
            }
            if (viewData.getValue() instanceof HashMap) {
                ArrayList<Object> humanReadableValues = new ArrayList<>();
                addHumanReadableValues(ob, humanReadableValues, (HashMap<String, String>) viewData.getValue());
                if (humanReadableValues.size() > 0)
                    ob.setHumanReadableValues(humanReadableValues);
            } else if (viewData.getValue() instanceof NFormViewData) {
                ArrayList<Object> humanReadableValues = new ArrayList<>();
                saveValues((NFormViewData) viewData.getValue(), ob, humanReadableValues);
                if (humanReadableValues.size() > 0)
                    ob.setHumanReadableValues(humanReadableValues);
            } else {
                ob.setValue(viewData.getValue());
            }
            obs.add(ob);
        }
        return obs;
    }

    private static void saveValues(NFormViewData optionsNFormViewData, Obs obs, ArrayList<Object> humanReadableValues) {
        if (optionsNFormViewData.getMetadata() != null) {
            if (optionsNFormViewData.getMetadata().containsKey(OPENMRS_ENTITY_ID)) {
                obs.setValue(optionsNFormViewData.getMetadata().get(OPENMRS_ENTITY_ID).toString());
                humanReadableValues.add(optionsNFormViewData.getValue());
            } else {
                obs.setValue(optionsNFormViewData.getValue());
            }
        }
    }

    private static void addHumanReadableValues(Obs obs, ArrayList<Object> humanReadableValues, HashMap<?, ?> valuesHashMap) {
        for (Object key : valuesHashMap.keySet()) {
            Object value = valuesHashMap.get(key);
            if (value instanceof NFormViewData) {
                saveValues((NFormViewData) value, obs, humanReadableValues);
            } else {
                obs.setValue(value);
            }
        }
    }


    public static void addLocHierarchyQuestions(JSONObject form) {
        try {
            List<Pair<String, String>> locationFields = FamilyLibrary.getInstance().metadata().getLocationFields();
            ArrayList<String> allowedLevels = FamilyLibrary.getInstance().metadata().getLocationHierarchy();
            if (locationFields != null && locationFields.size() > 0) {
                for (Pair<String, String> locationPair : locationFields) {
                    List<String> defaultFacility = LocationHelper.getInstance().generateDefaultLocationHierarchy(allowedLevels);
                    List<FormLocation> upToFacilities = LocationHelper.getInstance().generateLocationHierarchyTree(false, allowedLevels);
                    String defaultFacilityString = AssetHandler.javaToJsonString(defaultFacility, (new TypeToken<List<String>>() {
                    }).getType());
                    String upToFacilitiesString = AssetHandler.javaToJsonString(upToFacilities, (new TypeToken<List<FormLocation>>() {
                    }).getType());
                    JSONArray questions = form.getJSONObject((String) locationPair.first).getJSONArray("fields");

                    // Count the number of innermost nodes.
                    DepthResult innermostCount = countInnermostNodes(new JSONArray(upToFacilitiesString));


                    JSONObject famVillage = org.smartregister.chw.hps.util.JsonFormUtils.getFieldJSONObject(form.getJSONObject(STEP1).getJSONArray(FIELDS),"fam_village");
                    if (innermostCount.count < 2) {
                        famVillage.put(VALUE, innermostCount.value);
                        famVillage.put("type", "hidden");
                    }

                    for (int i = 0; i < questions.length(); ++i) {
                        if (questions.getJSONObject(i).getString("key").equals(locationPair.second)) {


                            if (StringUtils.isNotBlank(upToFacilitiesString)) {
                                questions.getJSONObject(i).put("tree", new JSONArray(upToFacilitiesString));
                            }

                            if (StringUtils.isNotBlank(defaultFacilityString)) {
                                questions.getJSONObject(i).put("default", defaultFacilityString);
                            }

                            if (innermostCount.count < 2) {
                                questions.getJSONObject(i).put(VALUE, innermostCount.defaultValue);
                                questions.getJSONObject(i).put(EDITABLE, false);
                                questions.getJSONObject(i).put(READ_ONLY,true);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }

    }

    public static JSONObject getFormAsJson(JSONObject form, String formName, String id, String currentLocationId) throws Exception {
        if (form == null) {
            return null;
        } else {
            String entityId = id;
            form.getJSONObject("metadata").put("encounter_location", currentLocationId);
            if (!org.smartregister.family.util.Utils.metadata().familyRegister.formName.equals(formName) && !org.smartregister.family.util.Utils.metadata().familyMemberRegister.formName.equals(formName)) {
                Timber.w("Unsupported form requested for launch " + formName, new Object[0]);
            } else {
                if (StringUtils.isNotBlank(id)) {
                    entityId = id.replace("-", "");
                }

                JSONArray field = fields(form, "step1");
                JSONObject uniqueId = getFieldJSONObject(field, "unique_id");
                if (formName.equals(org.smartregister.family.util.Utils.metadata().familyRegister.formName)) {
                    if (uniqueId != null) {
                        uniqueId.remove("value");
                        uniqueId.put("value", entityId + "_Family");
                    }

                    field = fields(form, "step2");
                    uniqueId = getFieldJSONObject(field, "unique_id");
                    if (uniqueId != null) {
                        uniqueId.remove("value");
                        uniqueId.put("value", entityId);
                    }
                } else if (uniqueId != null) {
                    uniqueId.remove("value");
                    uniqueId.put("value", entityId);
                }

                addLocHierarchyQuestions(form);
            }

            Timber.d("form is " + form.toString(), new Object[0]);
            return form;
        }
    }

    /**
     * Returns the total number of nodes (leaf objects) found at the maximum depth
     * in the JSON tree.
     *
     * @param jsonArray The root JSON array.
     * @return The count of innermost nodes.
     */
    public static DepthResult countInnermostNodes(JSONArray jsonArray) throws JSONException {
        DepthResult result = new DepthResult();
        traverseNodes(jsonArray, 1, result);
        return result;
    }

    /**
     * Recursively traverses the JSON tree.
     *
     * @param nodes  The current JSON array to process.
     * @param depth  The current depth in the tree.
     * @param result The running result tracking the maximum depth and count.
     */
    private static void traverseNodes(JSONArray nodes, int depth, DepthResult result) throws JSONException {
        for (int i = 0; i < nodes.length(); i++) {
            JSONObject obj = nodes.getJSONObject(i);
            // If this object has a "nodes" array, go deeper.
            if (obj.has("nodes") && obj.get("nodes") instanceof JSONArray) {
                JSONArray childNodes = obj.getJSONArray("nodes");
                result.defaultValue.put(obj.getString(KEY));
                traverseNodes(childNodes, depth + 1, result);
            } else {
                // This is a leaf node.
                if (depth > result.maxDepth) {
                    result.maxDepth = depth;
                    result.count = 1;
                    result.defaultValue.put(obj.getString(KEY));
                    result.value = obj.getString(KEY);
                } else if (depth == result.maxDepth) {
                    result.count++;
                }
            }
        }
    }

    public interface Flavor {
        JSONObject getAutoJsonEditMemberFormString(String title, String formName, Context context, CommonPersonObjectClient client, String eventType, String familyName, boolean isPrimaryCaregiver);

        void processFieldsForMemberEdit(CommonPersonObjectClient client, JSONObject jsonObject, JSONArray jsonArray, String familyName, boolean isPrimaryCaregiver, Event ecEvent, Client ecClient) throws JSONException;
    }

    private static class DepthResult {
        int maxDepth = 0;
        int count = 0;
        String value = "";
        JSONArray defaultValue = new JSONArray();
    }

}

