package org.smartregister.chw.util;

import static com.vijay.jsonwizard.constants.JsonFormConstants.EDITABLE;
import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static com.vijay.jsonwizard.constants.JsonFormConstants.READ_ONLY;
import static java.nio.charset.StandardCharsets.UTF_8;

import static org.smartregister.client.utils.constants.JsonFormConstants.STEP1;

import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import com.google.common.reflect.TypeToken;
import com.nerdstone.neatformcore.domain.model.NFormViewData;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.chw.activity.AncJsonWizardFormActivity;
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
import org.smartregister.commonregistry.CommonRepository;
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private static final String LAST_MENSTRUAL_PERIOD = "last_menstrual_period";
    private static final String FIRST_CLINIC_VISIT_DATE = "first_clinic_visit_date";
    public static final int REQUEST_CODE_GET_JSON = 2244;
    public static final int REQUEST_CODE_GET_JSON_WASH = 22444;
    public static final int REQUEST_CODE_GET_JSON_FAMILY_KIT = 22447;
    public static final int REQUEST_CODE_GET_JSON_HOUSEHOLD = 22445;

    public static final String CURRENT_OPENSRP_ID = "current_opensrp_id";
    public static final String READ_ONLY = "read_only";
    private static Flavor flavor = new JsonFormUtilsFlv();

    /**
     * Prepares an edit form for clients without linked family records, injecting identifiers and title.
     */
    public static JSONObject prepareIndependentEditForm(Context context, String formName, String baseEntityId, String title) {
        try {
            JSONObject jsonForm = new FormUtils(context).getFormJson(formName);
            if (jsonForm == null) return null;

            jsonForm.put("entity_id", baseEntityId);
            jsonForm.put("relational_id", baseEntityId);

            if (jsonForm.has(JsonFormConstants.STEP1)) {
                jsonForm.getJSONObject(JsonFormConstants.STEP1).put("title", title);
            }
            jsonForm.put("encounter_type", title);
            return jsonForm;
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    public static JSONObject getLocalizedFormJson(Context context, String formName) {
        if (context == null) {
            return null;
        }

        try {
            FormUtils formUtils = FormUtils.getInstance(context);
            Locale locale = CoreConstants.JSON_FORM.locale;
            if (locale == null) {
                locale = context.getResources().getConfiguration().locale;
            }

            if (locale == null || Locale.ENGLISH.getLanguage().equalsIgnoreCase(locale.getLanguage())) {
                return formUtils.getFormJson(formName);
            }

            String localizedPath = "json.form-" + locale.getLanguage() + "/" + formName + AllConstants.JSON_FILE_EXTENSION;
            try (InputStream inputStream = context.getAssets().open(localizedPath);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8))) {
                StringBuilder formJson = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    formJson.append(line);
                }
                return new JSONObject(formJson.toString());
            } catch (FileNotFoundException e) {
                Timber.d("Localized form not found at %s. Falling back to default asset", localizedPath);
            }

            return formUtils.getFormJson(formName);
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    public static Intent getAncPncStartFormIntent(JSONObject jsonForm, Context context) {
        if (!isAncRegistrationDateAlignmentForm(jsonForm)) {
            return CoreJsonFormUtils.getAncPncStartFormIntent(jsonForm, context);
        }

        Intent intent = new Intent(context, AncJsonWizardFormActivity.class);
        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());

        Form form = new Form();
        form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
        form.setWizard(true);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        return intent;
    }

    private static boolean isAncRegistrationDateAlignmentForm(JSONObject jsonForm) {
        if (jsonForm == null) {
            return false;
        }

        JSONObject stepOne = jsonForm.optJSONObject(JsonFormConstants.STEP1);
        if (stepOne == null) {
            return false;
        }

        JSONArray fields = stepOne.optJSONArray(JsonFormConstants.FIELDS);
        if (fields == null) {
            return false;
        }

        boolean hasLmp = false;
        boolean hasFirstClinicVisit = false;
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field == null) {
                continue;
            }

            String key = field.optString(JsonFormConstants.KEY);
            if (LAST_MENSTRUAL_PERIOD.equals(key)) {
                hasLmp = true;
            } else if (FIRST_CLINIC_VISIT_DATE.equals(key)) {
                hasFirstClinicVisit = true;
            }
        }

        return hasLmp && hasFirstClinicVisit;
    }

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
     * After a Family Registration completes, ensure the created ec_family row points to the chosen
     * existing head. This avoids creating a duplicate head record and makes the profile header show
     * the correct person even if they don't belong to this household's member list.
     */
    public static void linkExistingHeadToLatestFamily(String headBaseEntityId) {
        if (StringUtils.isBlank(headBaseEntityId)) return;
        // No-op (reverted).
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
     * Returns a value directly from a field JSONObject (no lookup performed)
     */
    public static String getValue(JSONObject fieldObject) {
        if (fieldObject == null) {
            return "";
        }

        try {
            if (fieldObject.has(JsonFormConstants.VALUE)) {
                Object value = fieldObject.get(JsonFormConstants.VALUE);
                return value != null ? String.valueOf(value) : "";
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
                        uniqueId.put("value", entityId + "_family");
                    }

                    // Populate the dedicated family_unique_id field when present
                    JSONObject familyUniqueId = getFieldJSONObject(field, "family_unique_id");
                    if (familyUniqueId != null) {
                        familyUniqueId.remove("value");
                        familyUniqueId.put("value", entityId + "_family");
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

    public static void populateExistingHead(JSONObject form, String baseEntityId) throws Exception {
        if (form == null || StringUtils.isBlank(baseEntityId)) {
            return;
        }

        CommonRepository commonRepository = org.smartregister.family.util.Utils.context().commonrepository(org.smartregister.family.util.Utils.metadata().familyMemberRegister.tableName);
        CommonPersonObject personObject = commonRepository.findByBaseEntityId(baseEntityId);
        if (personObject == null) {
            throw new IllegalArgumentException("No registered client found for id " + baseEntityId);
        }

        CommonPersonObjectClient client = new CommonPersonObjectClient(personObject.getCaseId(), personObject.getDetails(), personObject.getCaseId());
        client.setColumnmaps(personObject.getColumnmaps());
        populateExistingHead(form, client);
    }

    public static void populateExistingHead(JSONObject form, CommonPersonObjectClient client) throws JSONException {
        // Do NOT override the Family Registration form entity_id. The form's entity_id should
        // remain the newly generated household (family) base_entity_id. Overriding this with the
        // selected existing head's base_entity_id causes ec_family.base_entity_id to equal the
        // person's base_entity_id, which breaks the intended separation between household and person.

        JSONObject stepTwo = form.getJSONObject(org.smartregister.family.util.JsonFormUtils.STEP2);
        JSONArray fields = stepTwo.getJSONArray(FIELDS);

        // Also prefill Step 1 for flavors (e.g., nacp) whose Step 2 values are computed from Step 1
        try {
            JSONObject stepOne = form.optJSONObject(org.smartregister.family.util.JsonFormUtils.STEP1);
            if (stepOne != null) {
                JSONArray stepOneFields = stepOne.optJSONArray(FIELDS);
                if (stepOneFields != null) {
                    setIfPresent(stepOneFields, "client_first_name", org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true), false);
                    setIfPresent(stepOneFields, "client_middle_name", org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), DBConstants.KEY.MIDDLE_NAME, true), false);
                    // For household name, default to the client's surname when available
                    setIfPresent(stepOneFields, "fam_name", org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), DBConstants.KEY.LAST_NAME, true), false);
                }
            }
        } catch (Exception e) {
            Timber.w(e);
        }

        // Always persist selected head id (if field exists) for downstream processing
        setValueAndLock(fields, "existing_head", client.getCaseId(), true);
        setIfPresent(fields, "family_head", client.getCaseId(), true);

        // Capture the current household membership to allow post-save correction (nacp only field)
        try {
            String originalRelId = org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), DBConstants.KEY.RELATIONAL_ID, true);
            if (StringUtils.isBlank(originalRelId)) {
                // Fallback: read from ec_family_member where the definitive relational_id lives
                CommonRepository fmRepo = org.smartregister.family.util.Utils.context().commonrepository(org.smartregister.family.util.Utils.metadata().familyMemberRegister.tableName);
                CommonPersonObject fmRow = fmRepo.findByBaseEntityId(client.getCaseId());
                if (fmRow != null && fmRow.getColumnmaps() != null) {
                    originalRelId = org.smartregister.family.util.Utils.getValue(fmRow.getColumnmaps(), DBConstants.KEY.RELATIONAL_ID, true);
                }
            }
            setIfPresent(fields, "original_relational_id", originalRelId, true);
        } catch (Exception e) {
            Timber.w(e);
        }

        // Basic identity
        setValueAndLock(fields, "first_name", org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true), true);
        setValueAndLock(fields, "middle_name", org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), DBConstants.KEY.MIDDLE_NAME, true), true);
        setValueAndLock(fields, "surname", org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), DBConstants.KEY.LAST_NAME, true), true);

        // Gender: normalize common storage variants (M/F -> Male/Female)
        String genderRaw = org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), DBConstants.KEY.GENDER, true);
        String gender = mapGenderValue(genderRaw);
        setValueAndLock(fields, "sex", gender, true);

        // DOB: convert to dd-MM-yyyy for date_picker compatibility
        String dobRaw = org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), DBConstants.KEY.DOB, false);
        String dobDisplay = formatDobForForm(dobRaw);
        setValueAndLock(fields, "dob", dobDisplay, true);

        // Age: some flavors use "age" while others use "age_calculated"
        if (StringUtils.isNotBlank(dobRaw)) {
            int ageValue = org.smartregister.chw.util.Utils.getAgeFromDate(dobRaw);
            if (!setIfPresent(fields, "age", String.valueOf(ageValue), true)) {
                setIfPresent(fields, "age_calculated", String.valueOf(ageValue), true);
            }
        }

        // Identifier (strip hyphens; fallback to client.identifiers if column map missing)
        String uniqueId = org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), DBConstants.KEY.UNIQUE_ID, true);
        if (StringUtils.isBlank(uniqueId)) {
            try {
                EventClientRepository eventClientRepository = new EventClientRepository();
                JSONObject clientJson = eventClientRepository.getClientByBaseEntityId(client.getCaseId());
                if (clientJson != null) {
                    Client baseClient = ChwApplication.getInstance().getEcSyncHelper().convert(clientJson, Client.class);
                    if (baseClient != null && baseClient.getIdentifiers() != null) {
                        uniqueId = baseClient.getIdentifiers().get(org.smartregister.family.util.Utils.metadata().uniqueIdentifierKey);
                    }
                }
            } catch (Exception e) {
                Timber.w(e);
            }
        }
        if (StringUtils.isNotBlank(uniqueId)) {
            uniqueId = uniqueId.replace("-", "");
        }
        setValueAndLock(fields, "unique_id", uniqueId, true);

        // Mirror family_head into Step 1 as well to ensure the family event carries it
        try {
            JSONObject stepOne = form.optJSONObject(org.smartregister.family.util.JsonFormUtils.STEP1);
            if (stepOne != null) {
                JSONArray stepOneFields = stepOne.optJSONArray(FIELDS);
                if (stepOneFields != null) {
                    setIfPresent(stepOneFields, "family_head", client.getCaseId(), true);
                }
            }
        } catch (Exception e) {
            Timber.w(e);
        }

        // Contacts (keep editable)
        setValueAndLock(fields, "phone_number", org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), DBConstants.KEY.PHONE_NUMBER, true), false);
        setValueAndLock(fields, "other_phone_number", org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), DBConstants.KEY.OTHER_PHONE_NUMBER, true), false);

        // Additional demographic and attributes (prefill for review; these are not persisted from this flow)
        prefillAdditionalHoHFields(form, fields, client);
    }

    private static boolean setIfPresent(JSONArray fields, String key, String value, boolean readOnly) throws JSONException {
        JSONObject field = getFieldJSONObject(fields, key);
        if (field == null) return false;
        setValueAndLock(fields, key, value, readOnly);
        return true;
    }

    private static String mapGenderValue(String genderRaw) {
        if (StringUtils.isBlank(genderRaw)) return genderRaw;
        String g = genderRaw.trim();
        if (g.equalsIgnoreCase("m")) return "Male";
        if (g.equalsIgnoreCase("f")) return "Female";
        return g; // already in display form
    }

    private static String formatDobForForm(String dobRaw) {
        try {
            if (StringUtils.isBlank(dobRaw)) return dobRaw;
            Date dob = Utils.dobStringToDate(dobRaw);
            if (dob != null) return dd_MM_yyyy.format(dob);
        } catch (Exception e) {
            Timber.e(e);
        }
        return dobRaw;
    }

    private static void setValueAndLock(JSONArray fields, String key, String value, boolean readOnly) throws JSONException {
        JSONObject field = getFieldJSONObject(fields, key);
        if (field == null) {
            return;
        }

        if (StringUtils.isNotBlank(value)) {
            field.put(JsonFormConstants.VALUE, value);
        } else {
            field.remove(JsonFormConstants.VALUE);
        }

        if (readOnly) {
            field.put(READ_ONLY, "true");
            field.put(EDITABLE, false);
        }
    }

    private static void prefillAdditionalHoHFields(JSONObject form, JSONArray stepTwoFields, CommonPersonObjectClient client) {
        try {
            // Marital status (spinner with keys matching stored values)
            safeSetSpinner(stepTwoFields, "marital_status", org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "marital_status", true));

            // Insurance fields
            String insuranceProvider = org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "insurance_provider", true);
            if (StringUtils.isBlank(insuranceProvider)) {
                // Some flavors use attributes.Health_Insurance_Type mapping; also seen as Health_Insurance_Type
                insuranceProvider = org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "Health_Insurance_Type", true);
            }
            safeSetSpinner(stepTwoFields, "insurance_provider", insuranceProvider);

            String insuranceProviderOther = org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "insurance_provider_other", true);
            if (StringUtils.isBlank(insuranceProviderOther)) {
                insuranceProviderOther = org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "Other_Health_Insurance_Type", true);
            }
            setValueAndLock(stepTwoFields, "insurance_provider_other", insuranceProviderOther, false);

            String insuranceNumber = org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "insurance_provider_number", true);
            if (StringUtils.isBlank(insuranceNumber)) {
                insuranceNumber = org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "Health_Insurance_Number", true);
            }
            setValueAndLock(stepTwoFields, "insurance_provider_number", insuranceNumber, false);

        // Disabilities (Yes/No)
        safeSetSpinner(stepTwoFields, "disabilities", org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "disabilities", true));

            // Type of disability (checkbox keys e.g., physical_impairments, other_disabilities)
            String disabilityTypes = org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "type_of_disability", false);
            prefillCheckbox(stepTwoFields, "type_of_disability", disabilityTypes);

            // If other disability previously specified
            setValueAndLock(stepTwoFields, "specify_other_disabilities",
                    org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "specify_other_disabilities", true), false);

            // Occupation (native_radio keys like chk_farmer)
            String occupation = org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "occupation", false);
            if (StringUtils.isBlank(occupation)) {
                // Fallback: derive occupation from latest relevant event obs
                occupation = findLatestObsValue(client.getCaseId(), new String[]{"occupation"});
            }
            prefillNativeRadio(stepTwoFields, "occupation", occupation);
            if ("chk_other".equalsIgnoreCase(occupation)) {
                setValueAndLock(stepTwoFields, "occupation_other",
                        org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "occupation_other", true), false);
            }

            // Leadership role (checkbox under person_attribute Community_Leader)
            String leader = org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "leader", false);
            if (StringUtils.isBlank(leader)) {
                // Some flavors persist attribute key name
                leader = org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "Community_Leader", false);
            }
            if (StringUtils.isBlank(leader)) {
                // Last resort: read from Client JSON attributes
                try {
                    EventClientRepository repo = new EventClientRepository();
                    JSONObject cj = repo.getClientByBaseEntityId(client.getCaseId());
                    if (cj != null && cj.has("attributes")) {
                        JSONObject attrs = cj.optJSONObject("attributes");
                        if (attrs != null) {
                            leader = attrs.optString("Community_Leader", leader);
                        }
                    }
                } catch (Exception e) {
                    Timber.w(e);
                }
            }
            prefillCheckbox(stepTwoFields, "leader", leader);
            setValueAndLock(stepTwoFields, "leader_other",
                    org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "leader_other", true), false);

            // Identity availability (native_radio) and dependent ID numbers
            // Prefer direct stored id_avail if present
            String idAvail = org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "id_avail", false);
            String nationalId = coalesce(
                    org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "national_id", true),
                    org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "National_ID", true)
            );
            String voterId = coalesce(
                    org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "voter_id", true),
                    org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "Voter_Registration_Number", true)
            );
            String driverLicense = coalesce(
                    org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "driver_license", true),
                    org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "Driver_License_Number", true)
            );
            String passportNum = coalesce(
                    org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "passport", true),
                    org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "Passport_Number", true),
                    org.smartregister.family.util.Utils.getValue(client.getColumnmaps(), "passport_number", true)
            );

            if (StringUtils.isBlank(idAvail)) {
                if (StringUtils.isNotBlank(nationalId)) idAvail = "chk_national_id";
                else if (StringUtils.isNotBlank(voterId)) idAvail = "chk_voters_id";
                else if (StringUtils.isNotBlank(driverLicense)) idAvail = "chk_drivers_license";
                else if (StringUtils.isNotBlank(passportNum)) idAvail = "chk_passport_number";
                else idAvail = "chk_none";
            }
            prefillNativeRadio(stepTwoFields, "id_avail", idAvail);

            // Prefill the dependent ID fields
            setValueAndLock(stepTwoFields, "national_id", nationalId, false);
            setValueAndLock(stepTwoFields, "voter_id", voterId, false);
            setValueAndLock(stepTwoFields, "driver_license", driverLicense, false);
            setValueAndLock(stepTwoFields, "passport", passportNum, false);

            // Mirror any head id changes back to Step 1 if required fields exist (already done above for family_head)
            try {
                JSONObject stepOne = form.optJSONObject(org.smartregister.family.util.JsonFormUtils.STEP1);
                if (stepOne != null) {
                    JSONArray stepOneFields = stepOne.optJSONArray(FIELDS);
                    if (stepOneFields != null) {
                        // No-op for now; hook retained for future additions
                    }
                }
            } catch (Exception e) {
                Timber.w(e);
            }
        } catch (Exception e) {
            Timber.w(e);
        }
    }

    private static void safeSetSpinner(JSONArray fields, String key, String rawValue) throws JSONException {
        if (StringUtils.isBlank(rawValue)) return;
        JSONObject field = getFieldJSONObject(fields, key);
        if (field == null) return;

        String selectedKey = null;
        String rawNorm = normalizeKey(rawValue);

        // 1) Try openmrs_choice_ids map (value -> key)
        if (field.has("openmrs_choice_ids")) {
            JSONObject choice = field.optJSONObject("openmrs_choice_ids");
            if (choice != null && choice.names() != null) {
                for (int i = 0; i < choice.names().length(); i++) {
                    String k = choice.names().getString(i);
                    String v = choice.optString(k);
                    if (equalsIgnoreCase(rawValue, v) || equalsIgnoreCase(rawValue, k) || equalsIgnoreCase(rawNorm, normalizeKey(k))) {
                        selectedKey = k;
                        break;
                    }
                }
            }
        }

        // 2) Try keys/values arrays
        if (selectedKey == null) {
            if (field.has("keys") && field.has("values")) {
                JSONArray keys = field.optJSONArray("keys");
                JSONArray values = field.optJSONArray("values");
                if (keys != null && values != null) {
                    for (int i = 0; i < keys.length(); i++) {
                        String k = keys.optString(i);
                        String v = values.optString(i);
                        if (equalsIgnoreCase(rawValue, k) || equalsIgnoreCase(rawValue, v) || equalsIgnoreCase(rawNorm, normalizeKey(k))) {
                            selectedKey = k;
                            break;
                        }
                    }
                }
            }
        }

        // 3) Try options array (objects with key/text/openmrs_entity_id)
        if (selectedKey == null && field.has(Constants.JSON_FORM_KEY.OPTIONS)) {
            JSONArray options = field.optJSONArray(Constants.JSON_FORM_KEY.OPTIONS);
            if (options != null) {
                for (int i = 0; i < options.length(); i++) {
                    JSONObject opt = options.optJSONObject(i);
                    if (opt == null) continue;
                    String k = opt.optString("key");
                    String t = opt.optString("text");
                    String oeid = opt.optString(OPENMRS_ENTITY_ID);
                    if (equalsIgnoreCase(rawValue, k) || equalsIgnoreCase(rawValue, t) || equalsIgnoreCase(rawValue, oeid) || equalsIgnoreCase(rawNorm, normalizeKey(k))) {
                        selectedKey = k;
                        break;
                    }
                }
            }
        }

        // Fallback: set rawValue as-is
        if (selectedKey == null) {
            selectedKey = rawValue;
        }

        field.put(JsonFormConstants.VALUE, selectedKey);
    }

    private static void prefillCheckbox(JSONArray fields, String key, String selectedKeysFlat) throws JSONException {
        if (StringUtils.isBlank(selectedKeysFlat)) return;
        JSONObject field = getFieldJSONObject(fields, key);
        if (field == null) return;
        JSONArray options = field.optJSONArray(Constants.JSON_FORM_KEY.OPTIONS);
        if (options == null) {
            // fallback to generic processor
            processValueWithChoiceIds(field, selectedKeysFlat);
            return;
        }

        // Normalize incoming selections into a set (split on commas, spaces, semicolons, pipes) and parse JSON array strings
        java.util.Set<String> tokens = new java.util.HashSet<>();
        java.util.Set<String> normTokens = new java.util.HashSet<>();
        boolean parsedArray = false;
        try {
            String trimmed = selectedKeysFlat.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                org.json.JSONArray arr = new org.json.JSONArray(trimmed);
                for (int i = 0; i < arr.length(); i++) {
                    String v = String.valueOf(arr.get(i));
                    if (StringUtils.isNotBlank(v)) {
                        String cleaned = v.trim().replace("\"", "");
                        if (!cleaned.isEmpty()) {
                            tokens.add(cleaned);
                            normTokens.add(normalizeKey(cleaned));
                        }
                    }
                }
                parsedArray = true;
            }
        } catch (Exception ignore) { }

        if (!parsedArray) {
            for (String part : selectedKeysFlat.split("[\\s,;|]+")) {
                if (StringUtils.isNotBlank(part)) {
                    String cleaned = part.trim().replace("\"", "");
                    if (!cleaned.isEmpty()) {
                        tokens.add(cleaned);
                        normTokens.add(normalizeKey(cleaned));
                    }
                }
            }
        }

        for (int i = 0; i < options.length(); i++) {
            JSONObject opt = options.getJSONObject(i);
            String k = opt.optString("key");
            String t = opt.optString("text");
            String oeid = opt.optString(OPENMRS_ENTITY_ID);
            boolean selected = containsIgnoreCase(tokens, k) || containsIgnoreCase(tokens, t) || containsIgnoreCase(tokens, oeid)
                    || containsIgnoreCase(normTokens, normalizeKey(k)) || containsIgnoreCase(normTokens, normalizeKey(oeid));
            if (selected) {
                opt.put(JsonFormConstants.VALUE, true);
            }
        }
    }

    private static void prefillNativeRadio(JSONArray fields, String key, String rawValue) throws JSONException {
        if (StringUtils.isBlank(rawValue)) return;
        JSONObject field = getFieldJSONObject(fields, key);
        if (field == null) return;

        String selectedKey = null;
        String rawNorm = normalizeKey(rawValue);

        // Only options array is expected for native_radio
        JSONArray options = field.optJSONArray(Constants.JSON_FORM_KEY.OPTIONS);
        if (options != null) {
            for (int i = 0; i < options.length(); i++) {
                JSONObject opt = options.optJSONObject(i);
                if (opt == null) continue;
                String k = opt.optString("key");
                String t = opt.optString("text");
                String oeid = opt.optString(OPENMRS_ENTITY_ID);
                if (equalsIgnoreCase(rawValue, k) || equalsIgnoreCase(rawValue, t) || equalsIgnoreCase(rawValue, oeid)
                        || equalsIgnoreCase(rawNorm, normalizeKey(k)) || equalsIgnoreCase(rawNorm, normalizeKey(oeid))) {
                    selectedKey = k;
                    // mark the matching option selected so the UI toggles
                    opt.put(JsonFormConstants.VALUE, true);
                    break;
                }
            }
        }

        if (selectedKey == null) selectedKey = rawValue;
        setValueAndLock(fields, key, selectedKey, false);
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private static boolean containsIgnoreCase(java.util.Set<String> set, String value) {
        if (set == null || set.isEmpty() || StringUtils.isBlank(value)) return false;
        for (String s : set) {
            if (s.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    private static String normalizeKey(String s) {
        if (s == null) return null;
        String t = s.trim().toLowerCase(java.util.Locale.ROOT);
        // replace all non-alphanumeric with underscore, collapse repeats
        t = t.replaceAll("[^a-z0-9]+", "_");
        // trim leading/trailing underscores
        t = t.replaceAll("^_+|_+$", "");
        return t;
    }

    private static String findLatestObsValue(String baseEntityId, String[] fields) {
        try {
            java.util.List<String> eventTypes = new java.util.ArrayList<>();
            eventTypes.add(org.smartregister.chw.core.utils.CoreConstants.EventType.FAMILY_REGISTRATION);
            eventTypes.add("Update Family Registration");

            org.smartregister.clientandeventmodel.Event ev = org.smartregister.chw.dao.EventDao.getLatestEvent(baseEntityId, eventTypes);
            if (ev == null || ev.getObs() == null) return null;

            for (Obs o : ev.getObs()) {
                String key = o.getFormSubmissionField() != null ? o.getFormSubmissionField() : o.getFieldCode();
                if (key == null) continue;
                for (String f : fields) {
                    if (f.equalsIgnoreCase(key)) {
                        java.util.List<Object> vals = o.getValues();
                        if (vals != null && !vals.isEmpty()) {
                            // For multi-select, return a comma-separated string of raw values
                            if (vals.size() == 1) return String.valueOf(vals.get(0));
                            StringBuilder sb = new StringBuilder();
                            for (Object v : vals) {
                                if (v != null) {
                                    if (sb.length() > 0) sb.append(",");
                                    sb.append(String.valueOf(v));
                                }
                            }
                            return sb.toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Timber.w(e);
        }
        return null;
    }

    

    private static String coalesce(String... values) {
        for (String v : values) {
            if (StringUtils.isNotBlank(v)) return v;
        }
        return null;
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
