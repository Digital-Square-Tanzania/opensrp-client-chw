package org.smartregister.chw.sync;


import static org.smartregister.chw.anc.util.Constants.EVENT_TYPE.DELETE_EVENT;
import static org.smartregister.chw.harmreduction.util.Constants.EVENT_TYPE.HARM_REDUCTION_FOLLOW_UP_VISIT;
import static org.smartregister.chw.harmreduction.util.Constants.EVENT_TYPE.HARM_REDUCTION_MAT_CLIENTS_FOLLOWUP;
import static org.smartregister.chw.harmreduction.util.Constants.EVENT_TYPE.HARM_REDUCTION_SOBER_HOUSE_VISIT;
import static org.smartregister.chw.harmreduction.util.Constants.EVENT_TYPE.HARM_REDUCTION_USED_NEEDLES_AND_SYRINGES_COLLECTION;
import static org.smartregister.chw.hivst.util.Constants.EVENT_TYPE.HIVST_MOBILIZATION;
import static org.smartregister.chw.tbleprosy.util.Constants.EVENT_TYPE.TB_LEPROSY_MOBILIZATION;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.dao.EventDao;
import org.smartregister.chw.core.sync.CoreClientProcessor;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.harmreduction.dao.HarmReductionDao;
import org.smartregister.chw.dao.PmtctDao;
import org.smartregister.chw.domain.AypInSchoolGroupDetails;
import org.smartregister.chw.fp.util.FamilyPlanningConstants;
import org.smartregister.chw.repository.AypInSchoolGroupDetailsRepository;
import org.smartregister.chw.repository.AypInSchoolGroupMembersRepository;
import org.smartregister.chw.repository.AypOutSchoolGroupDetailsRepository;
import org.smartregister.chw.repository.AypOutSchoolGroupMembersRepository;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.chw.service.ChildAlertService;
import org.smartregister.chw.util.Constants;
import org.smartregister.domain.Event;
import org.smartregister.domain.Obs;
import org.smartregister.domain.db.EventClient;
import org.smartregister.domain.jsonmapping.ClientClassification;
import org.smartregister.domain.jsonmapping.Table;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.service.intent.VaccineIntentService;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.sync.ClientProcessorForJava;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class ChwClientProcessor extends CoreClientProcessor {

    private static final String METHADONE_TREATMENT_STATUS_FIELD = "methadone_treatment_status";

    private String currentEventType;

    private ChwClientProcessor(Context context) {
        super(context);
    }

    public static ClientProcessorForJava getInstance(Context context) {
        if (instance == null) {
            instance = new ChwClientProcessor(context);
        }
        return instance;
    }

    @Override
    public void processEvents(ClientClassification clientClassification, Table vaccineTable, Table serviceTable, EventClient eventClient, Event event, String eventType) throws Exception {
        currentEventType = eventType;

        if (VaccineIntentService.EVENT_TYPE.equals(eventType)
                || VaccineIntentService.EVENT_TYPE_OUT_OF_CATCHMENT.equals(eventType)) {
            processVaccinationEvent(eventClient, VaccineIntentService.EVENT_TYPE_OUT_OF_CATCHMENT.equals(eventType));
            return;
        }

        if (eventClient != null && eventClient.getEvent() != null) {
            String baseEntityID = eventClient.getEvent().getBaseEntityId();
            switch (eventType) {
                case CoreConstants.EventType.REMOVE_FAMILY:
                    ChwApplication.getInstance().getScheduleRepository().deleteSchedulesByFamilyEntityID(baseEntityID);
                case CoreConstants.EventType.REMOVE_MEMBER:
                    ChwApplication.getInstance().getScheduleRepository().deleteSchedulesByEntityID(baseEntityID);
                case CoreConstants.EventType.REMOVE_CHILD:
                    if (!CoreLibrary.getInstance().isPeerToPeerProcessing() && !SyncStatusBroadcastReceiver.getInstance().isSyncing()) {
                        ChwApplication.getInstance().getScheduleRepository().deleteSchedulesByEntityID(baseEntityID);
                    }
                    break;
                default:
                    break;
            }
        }

        if (eventType.equals(org.smartregister.chw.tbleprosy.util.Constants.EVENT_TYPE.TB_LEPROSY_SCREENING)) {
            Timber.e("TB_LEPROSY_SCREENING");
        }

        // Intercept head-person creation when an existing head was selected: skip non-family entityType
        try {
            if (CoreConstants.EventType.FAMILY_REGISTRATION.equals(eventType) && eventClient != null && eventClient.getEvent() != null) {
                String existingHeadIdPre = getFormValue(eventClient.getEvent(), "existing_head");
                String entityType = eventClient.getEvent().getEntityType();
                if (StringUtils.isNotBlank(existingHeadIdPre) && StringUtils.isNotBlank(entityType)) {
                    // Only allow the ec_family event to go through; skip any person/independent-client event
                    if (!CoreConstants.TABLE_NAME.FAMILY.equalsIgnoreCase(entityType)) {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            Timber.w(e);
        }

        super.processEvents(clientClassification, vaccineTable, serviceTable, eventClient, event, eventType);
        if (eventClient != null && eventClient.getEvent() != null) {
            String baseEntityID = eventClient.getEvent().getBaseEntityId();
            switch (eventType) {
                case CoreConstants.EventType.FAMILY_REGISTRATION:
                    // Ensure the new family points to the chosen existing head (if provided)
                    try {
                        String chosenHead = getFormValue(eventClient.getEvent(), "family_head");
                        if (StringUtils.isBlank(chosenHead)) {
                            chosenHead = getFormValue(eventClient.getEvent(), "existing_head");
                        }
                        if (StringUtils.isNotBlank(chosenHead)) {
                            SQLiteDatabase db = ChwApplication.getInstance().getRepository().getWritableDatabase();
                            if (db != null) {
                                db.execSQL("UPDATE ec_family SET family_head = ? WHERE base_entity_id = ?",
                                        new Object[]{chosenHead, baseEntityID});
                                // Default caregiver to the head only when not explicitly set
                                db.execSQL("UPDATE ec_family SET primary_caregiver = ? WHERE base_entity_id = ? AND (primary_caregiver IS NULL OR TRIM(primary_caregiver) = '' )",
                                        new Object[]{chosenHead, baseEntityID});
                            }
                        }
                    } catch (Exception e) {
                        Timber.w(e);
                    }
                    // Post-process: if user selected an existing head during Family Registration,
                    // revert any membership move by restoring the head's original relational_id.
                    try {
                        String existingHeadId = getFormValue(eventClient.getEvent(), "existing_head");
                        String originalRelId = getFormValue(eventClient.getEvent(), "original_relational_id");
                        if (StringUtils.isNotBlank(existingHeadId) && StringUtils.isNotBlank(originalRelId)) {
                            SQLiteDatabase db = ChwApplication.getInstance().getRepository().getWritableDatabase();
                            if (db != null) {
                                db.execSQL(
                                        "UPDATE ec_family_member SET relational_id = ? WHERE base_entity_id = ? AND relational_id != ?",
                                        new Object[]{originalRelId, existingHeadId, originalRelId}
                                );
                            }
                        }
                    } catch (Exception e) {
                        Timber.w(e);
                    }

                    // Ensure the household's unique_id is set from the family_unique_id field
                    // to avoid collisions with the head's unique_id.
                    try {
                        String familyUniqueId = getFormValue(eventClient.getEvent(), "family_unique_id");
                        if (StringUtils.isBlank(familyUniqueId)) {
                            // Fallback: try the generic unique_id only if it looks like a family value
                            String maybe = getFormValue(eventClient.getEvent(), "unique_id");
                            if (StringUtils.isNotBlank(maybe) && (maybe.endsWith("_family") || maybe.endsWith("_Family"))) {
                                familyUniqueId = maybe;
                            }
                        }

                        if (StringUtils.isNotBlank(familyUniqueId)) {
                            // Normalize: if missing the suffix, append lowercase to align with DB usage
                            if (!(familyUniqueId.endsWith("_family") || familyUniqueId.endsWith("_Family"))) {
                                familyUniqueId = familyUniqueId + "_family";
                            }

                            String familyBaseEntityId = eventClient.getEvent().getBaseEntityId();
                            if (StringUtils.isNotBlank(familyBaseEntityId)) {
                                SQLiteDatabase db = ChwApplication.getInstance().getRepository().getWritableDatabase();
                                if (db != null) {
                                    db.execSQL("UPDATE ec_family SET unique_id = ? WHERE base_entity_id = ?",
                                            new Object[]{familyUniqueId, familyBaseEntityId});
                                }
                            }
                        }
                    } catch (Exception e) {
                        Timber.w(e);
                    }
                    break;
                case CoreConstants.EventType.CHILD_HOME_VISIT:
                case CoreConstants.EventType.CHILD_VISIT_NOT_DONE:
                case CoreConstants.EventType.CHILD_REGISTRATION:
                case CoreConstants.EventType.UPDATE_CHILD_REGISTRATION:
                    if (!CoreLibrary.getInstance().isPeerToPeerProcessing() && !SyncStatusBroadcastReceiver.getInstance().isSyncing()) {
                        ChildAlertService.updateAlerts(baseEntityID);
                    }
                    break;
                case Constants.Events.CBHS_FOLLOWUP:
                case Constants.Events.MOTHER_CHAMPION_FOLLOWUP:
                case Constants.Events.ANC_FIRST_FACILITY_VISIT:
                case Constants.Events.ANC_RECURRING_FACILITY_VISIT:
                case Constants.Events.AGYW_STRUCTURAL_SERVICES:
                case Constants.Events.AGYW_BEHAVIORAL_SERVICES:
                case Constants.Events.AGYW_BIO_MEDICAL_SERVICES:
                case Constants.Events.KVP_PREP_FOLLOWUP_VISIT:
                case Constants.Events.MOTHER_CHAMPION_SBCC_SESSIONS:
                case HIVST_MOBILIZATION:
                case TB_LEPROSY_MOBILIZATION:
                case HARM_REDUCTION_USED_NEEDLES_AND_SYRINGES_COLLECTION:
                case HARM_REDUCTION_MAT_CLIENTS_FOLLOWUP:
                case HARM_REDUCTION_FOLLOW_UP_VISIT:
                case HARM_REDUCTION_SOBER_HOUSE_VISIT:
                case org.smartregister.chw.malaria.util.Constants.EVENT_TYPE.ICCM_SERVICES_VISIT:
                case org.smartregister.chw.sbc.util.Constants.EVENT_TYPE.SBC_FOLLOW_UP_VISIT:
                case org.smartregister.chw.sbc.util.Constants.EVENT_TYPE.SBC_HEALTH_EDUCATION_MOBILIZATION:
                case org.smartregister.chw.sbc.util.Constants.EVENT_TYPE.SBC_MONTHLY_SOCIAL_MEDIA_REPORT:
                case FamilyPlanningConstants.EVENT_TYPE.FP_CBD_FOLLOW_UP_VISIT:
                case org.smartregister.chw.cecap.util.Constants.EVENT_TYPE.CECAP_HOME_VISIT:
                case org.smartregister.chw.cecap.util.Constants.EVENT_TYPE.CECAP_HEALTH_EDUCATION_MOBILIZATION:
                case org.smartregister.chw.asrh.util.Constants.EVENT_TYPE.ASRH_FOLLOW_UP_VISIT:
                case org.smartregister.chw.hps.util.Constants.EVENT_TYPE.HPS_HOUSEHOLD_VISIT:
                case org.smartregister.chw.hps.util.Constants.EVENT_TYPE.HPS_CLIENT_FOLLOW_UP_VISIT:
                case org.smartregister.chw.hps.util.Constants.EVENT_TYPE.HPS_MOBILIZATION:
                case org.smartregister.chw.hps.util.Constants.EVENT_TYPE.HPS_ADVERTISEMENT_FEEDBACK:
                case org.smartregister.chw.hps.util.Constants.EVENT_TYPE.HPS_DEATH_REGISTRATION:
                case org.smartregister.chw.hps.util.Constants.EVENT_TYPE.HPS_ANNUAL_CENSUS:
                case org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_FOLLOW_UP_VISIT:
                case org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_IN_SCHOOL_FOLLOW_UP_VISIT:
                case org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_SERVICES:
                case Constants.Events.AYP_OUT_SCHOOL_FOLLOW_UP_VISIT:
                case org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_PARENTAL_SERVICES:
                case org.smartregister.chw.tbleprosy.util.Constants.EVENT_TYPE.TB_LEPROSY_CLIENT_OBSERVATION:
                case org.smartregister.chw.tbleprosy.util.Constants.EVENT_TYPE.TB_LEPROSY_RECORD_VISIT:
                case org.smartregister.chw.tbleprosy.util.Constants.EVENT_TYPE.TB_LEPROSY_FOLLOW_UP_VISIT:
                case org.smartregister.chw.tbleprosy.util.Constants.EVENT_TYPE.RECORD_LEPROSY_TREATMENT_START_DATE:
                    if (eventClient.getEvent() == null) {
                        return;
                    }
                    processVisitEvent(eventClient);
                    processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                    if (HARM_REDUCTION_MAT_CLIENTS_FOLLOWUP.equals(eventType)) {
                        processMatFollowupTreatmentStatus(eventClient.getEvent());
                    }
                    break;
                case org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_GROUP_DETAILS:
                    // AYP In-school group creation/edit event
                    processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                    try {
                        saveAypGroupDetails(eventClient.getEvent());
                    } catch (Exception e) {
                        Timber.e(e, "Error saving AYP group details");
                    }
                    break;
                case org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_GROUP_MEMBERSHIP:
                    // Persist selected members to group membership table
                    processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                    try {
                        saveAypGroupMembership(eventClient.getEvent());
                    } catch (Exception e) {
                        Timber.e(e, "Error saving AYP group membership");
                    }
                    break;
                case org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_OUT_GROUP_DETAILS:
                    // AYP In-school group creation/edit event
//                    processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                    try {
                        saveAypOutGroupDetails(eventClient.getEvent());
                    } catch (Exception e) {
                        Timber.e(e, "Error saving AYP Out group details");
                    }
                    break;
                case org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_OUT_GROUP_MEMBERSHIP:
                    // Persist selected members to group membership table
//                    processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                    try {
                        saveAypOutGroupMembership(eventClient.getEvent());
                    } catch (Exception e) {
                        Timber.e(e, "Error saving AYP Out group membership");
                    }
                    break;
                case org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_OUT_SCHOOL_GROUP_FOLLOW_UP_VISIT:
                    // Persist selected members to group membership table
                    processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                    try {
                        saveAypOutGroupFollowUpVisit(eventClient.getEvent());
                    } catch (Exception e) {
                        Timber.e(e, "Error saving AYP Out group membership");
                    }
                    break;
                case CoreConstants.EventType.REMOVE_MEMBER:
                    if (eventClient.getClient() == null) {
                        return;
                    }
                    processVisitEvent(eventClient);
                    processRemoveMember(eventClient.getClient().getBaseEntityId(), event);
                    processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                    break;
                case CoreConstants.EventType.REMOVE_CHILD:
                    if (eventClient.getClient() == null) {
                        return;
                    }
                    processVisitEvent(eventClient);
                    processRemoveChild(eventClient.getClient().getBaseEntityId(), event);
                    processRemoveMember(eventClient.getClient().getBaseEntityId(), event);
                    processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                    break;
                case org.smartregister.chw.ld.util.Constants.EVENT_TYPE.VOID_EVENT:
                case DELETE_EVENT:
                    processDeleteEvent(eventClient.getEvent());
                default:
                    break;
            }
        }

        if (!CoreLibrary.getInstance().isPeerToPeerProcessing() && !SyncStatusBroadcastReceiver.getInstance().isSyncing()) {
            try {
                ChwScheduleTaskExecutor.getInstance().execute(event.getBaseEntityId(), event.getEventType(), event.getEventDate().toDate());
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    @VisibleForTesting
    protected void processVaccinationEvent(EventClient eventClient, boolean outOfCatchment) {
        try {
            org.smartregister.immunization.domain.Vaccine vaccine = buildVaccineFromEvent(eventClient, outOfCatchment);
            if (vaccine == null) {
                return;
            }

            VaccineRepository vaccineRepository = ChwApplication.getInstance().vaccineRepository();
            addVaccine(vaccineRepository, vaccine);
        } catch (Exception e) {
            Timber.e(e, "Process Vaccine Error");
        }
    }

    @VisibleForTesting
    protected org.smartregister.immunization.domain.Vaccine buildVaccineFromEvent(EventClient eventClient, boolean outOfCatchment) {
        if (eventClient == null || eventClient.getEvent() == null) {
            return null;
        }

        Event event = eventClient.getEvent();
        Obs vaccineDateObs = findVaccineDateObs(event.getObs());
        if (vaccineDateObs == null || StringUtils.isBlank(vaccineDateObs.getFormSubmissionField())) {
            return null;
        }

        Date administeredDate = parseVaccineDate(vaccineDateObs.getValue());
        if (administeredDate == null) {
            return null;
        }

        org.smartregister.immunization.domain.Vaccine vaccine = new org.smartregister.immunization.domain.Vaccine();
        vaccine.setBaseEntityId(event.getBaseEntityId());
        vaccine.setName(vaccineDateObs.getFormSubmissionField());
        vaccine.setCalculation(resolveVaccineCalculation(event.getObs(), vaccine.getName()));
        vaccine.setDate(administeredDate);
        vaccine.setAnmId(event.getProviderId());
        vaccine.setLocationId(event.getLocationId());
        vaccine.setSyncStatus(VaccineRepository.TYPE_Synced);
        vaccine.setFormSubmissionId(event.getFormSubmissionId());
        vaccine.setEventId(event.getEventId());
        vaccine.setOutOfCatchment(outOfCatchment ? 1 : 0);
        vaccine.setProgramClientId(getVaccineProgramClient(eventClient));
        vaccine.setCreatedAt(event.getDateCreated() != null ? event.getDateCreated().toDate() : administeredDate);
        vaccine.setTeam(event.getTeam());
        vaccine.setTeamId(event.getTeamId());
        vaccine.setChildLocationId(event.getChildLocationId());
        return vaccine;
    }

    private Obs findVaccineDateObs(List<Obs> observations) {
        if (observations == null) {
            return null;
        }

        for (Obs observation : observations) {
            if (observation == null || StringUtils.isBlank(observation.getFormSubmissionField())) {
                continue;
            }

            if ("date".equalsIgnoreCase(observation.getFieldDataType())) {
                return observation;
            }
        }

        return null;
    }

    private int resolveVaccineCalculation(List<Obs> observations, String vaccineName) {
        if (observations != null) {
            String expectedDoseField = StringUtils.isBlank(vaccineName) ? null : vaccineName + "_dose";
            for (Obs observation : observations) {
                if (observation == null) {
                    continue;
                }

                boolean isDoseObs = "calculate".equalsIgnoreCase(observation.getFieldDataType())
                        || StringUtils.equalsIgnoreCase(expectedDoseField, observation.getFormSubmissionField());
                if (!isDoseObs) {
                    continue;
                }

                Integer calculation = parseInteger(observation.getValue());
                if (calculation != null) {
                    return calculation;
                }
            }
        }

        return getDoseFromVaccineName(vaccineName);
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int getDoseFromVaccineName(String vaccineName) {
        if (StringUtils.isBlank(vaccineName)) {
            return 0;
        }

        for (int index = vaccineName.length() - 1; index >= 0; index--) {
            if (!Character.isDigit(vaccineName.charAt(index))) {
                if (index == vaccineName.length() - 1) {
                    return 0;
                }

                return Integer.parseInt(vaccineName.substring(index + 1));
            }
        }

        return Integer.parseInt(vaccineName);
    }

    private Date parseVaccineDate(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(String.valueOf(value));
        } catch (ParseException e) {
            Timber.e(e);
            return null;
        }
    }

    private String getVaccineProgramClient(EventClient eventClient) {
        if (eventClient == null || eventClient.getEvent() == null || eventClient.getEvent().getDetails() == null) {
            return null;
        }

        return eventClient.getEvent().getDetails().get("program_client_id");
    }

    private String getFormValue(Event event, String key) {
        try {
            if (event == null || event.getObs() == null) return "";
            for (Obs obs : event.getObs()) {
                try {
                    String field = obs.getFieldCode();
                    if (StringUtils.isBlank(field)) {
                        field = obs.getFormSubmissionField();
                    }
                    if (key.equalsIgnoreCase(field)) {
                        Object val = obs.getValue();
                        return val != null ? String.valueOf(val) : "";
                    }
                } catch (Exception e) {
                    // continue
                }
            }
        } catch (Exception e) {
            Timber.w(e);
        }
        return "";
    }

    private void processVisitEvent(EventClient eventClient) {
        try {
            NCUtils.processHomeVisit(eventClient);
        } catch (Exception e) {
            String formID = (eventClient != null && eventClient.getEvent() != null) ? eventClient.getEvent().getFormSubmissionId() : "no form id";
            Timber.e("Form id " + formID + ". " + e.toString());
        }
    }

    private void processMatFollowupTreatmentStatus(Event event) {
        if (event == null) {
            return;
        }

        String methadoneTreatmentStatus = getFormValue(event, METHADONE_TREATMENT_STATUS_FIELD);
        if (StringUtils.isBlank(event.getBaseEntityId())) {
            return;
        }

        try {
            if (HarmReductionDao.isCompletedMethadoneTreatment(methadoneTreatmentStatus)) {
                HarmReductionDao.closeCompletedMethadoneTreatmentRiskAssessment(event.getBaseEntityId());
            } else if (HarmReductionDao.isStoppedUsingMethadone(methadoneTreatmentStatus)) {
                HarmReductionDao.reassignStoppedMethadoneTreatmentRiskAssessment(event.getBaseEntityId());
            }
        } catch (Exception e) {
            Timber.w(e);
        }
    }


    @Override
    protected String getHumanReadableConceptResponse(String value, Object object) {
        try {
            if (StringUtils.isBlank(value) || (object != null && !(object instanceof Obs))) {
                return value;
            }
            // Default behavior in this flavor is to use coded values for concept responses.
            final String VALUES = "values";
            final String HUMAN_READABLE_VALUES = "humanReadableValues";
            final String FORM_SUBMISSION_FIELD = "formSubmissionField";
            List values = new ArrayList();

            Object valueObject = getValue(object, VALUES);
            if (valueObject instanceof List) {
                values = (List) valueObject;
            }

            // For close referral feedback, persist what the user selected/readable text instead of concept code.
            // This is required for ec_close_referral.servicesProvided.
            String formSubmissionField = String.valueOf(getValue(object, FORM_SUBMISSION_FIELD));

            if (CoreConstants.EventType.CLOSE_REFERRAL.equalsIgnoreCase(currentEventType))
            {
                if ("servicesProvided".equalsIgnoreCase(formSubmissionField)) {
                    List humanReadableValues = new ArrayList();
                    Object humanReadableValueObject = getValue(object, HUMAN_READABLE_VALUES);
                    if (humanReadableValueObject instanceof List) {
                        humanReadableValues = (List) humanReadableValueObject;
                    }
                    if (!humanReadableValues.isEmpty()) {
                        return humanReadableValues.size() == 1
                                ? humanReadableValues.get(0).toString()
                                : humanReadableValues.toString();
                    }
                }

                if ("prescriptions".equalsIgnoreCase(formSubmissionField)) {
                    List humanReadableValues = new ArrayList();
                    Object humanReadableValueObject = getValue(object, HUMAN_READABLE_VALUES);
                    if (humanReadableValueObject instanceof List) {
                        humanReadableValues = (List) humanReadableValueObject;
                    }
                    if (!humanReadableValues.isEmpty()) {
                        return humanReadableValues.size() == 1
                                ? humanReadableValues.get(0).toString()
                                : humanReadableValues.toString();
                    }
                }

                if ("outcomes".equalsIgnoreCase(formSubmissionField)) {
                    List humanReadableValues = new ArrayList();
                    Object humanReadableValueObject = getValue(object, HUMAN_READABLE_VALUES);
                    if (humanReadableValueObject instanceof List) {
                        humanReadableValues = (List) humanReadableValueObject;
                    }
                    if (!humanReadableValues.isEmpty()) {
                        return humanReadableValues.size() == 1
                                ? humanReadableValues.get(0).toString()
                                : humanReadableValues.toString();
                    }
                }
            }

            if (object == null || values.isEmpty()) {
                return value;
            }

            return values.size() == 1 ? values.get(0).toString() : values.toString();

        } catch (Exception e) {
            Timber.e(e);
        }
        return value;
    }

    @Override
    public void processDeleteEvent(Event event) {
        try {
            List<String> followupTables = Arrays.asList("ec_cecap_visit");
            if (event.getDetails().containsKey(org.smartregister.chw.anc.util.Constants.JSON_FORM_EXTRA.DELETE_FORM_SUBMISSION_ID)) {
                // delete from vaccine table
                EventDao.deleteVaccineByFormSubmissionId(event.getDetails().get(org.smartregister.chw.anc.util.Constants.JSON_FORM_EXTRA.DELETE_FORM_SUBMISSION_ID));
                // delete from visit table
                EventDao.deleteVisitByFormSubmissionId(event.getDetails().get(org.smartregister.chw.anc.util.Constants.JSON_FORM_EXTRA.DELETE_FORM_SUBMISSION_ID));
                // delete from recurring service table
                EventDao.deleteServiceByFormSubmissionId(event.getDetails().get(org.smartregister.chw.anc.util.Constants.JSON_FORM_EXTRA.DELETE_FORM_SUBMISSION_ID));

                //delete from all  Case Based Management tables that use formSubmissionIds as primaryKeys
                for (String tableName : followupTables) {
                    try {
                        PmtctDao.deleteEntryFromTableByFormSubmissionId(tableName, event.getDetails().get(org.smartregister.chw.anc.util.Constants.JSON_FORM_EXTRA.DELETE_FORM_SUBMISSION_ID));
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
            } else {
                super.processDeleteEvent(event);
                //delete from all PMTCT Case Based Management tables that use formSubmissionIds as primaryKeys
                for (String tableName : followupTables) {
                    try {
                        PmtctDao.deleteEntryFromTableByFormSubmissionId(tableName, event.getFormSubmissionId());
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
            }

            Timber.d("Ending processDeleteEvent: %s", event.getEventId());
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void saveAypGroupDetails(Event event) {
        try {
            if (event == null) return;
            AypInSchoolGroupDetails record = new AypInSchoolGroupDetails();
            record.setBaseEntityId(event.getBaseEntityId());
            record.setProviderId(event.getProviderId());
            record.setGroupName(getObsStringValue(event, "group_name"));
            record.setGroupType(getObsStringValue(event, "group_type"));
            record.setAgeBand(getObsStringValue(event, "age_band"));
            record.setLastInteractedWith(System.currentTimeMillis());
            new AypInSchoolGroupDetailsRepository().save(record);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void saveAypOutGroupDetails(Event event) {
        try {
            if (event == null) return;
            AypInSchoolGroupDetails record = new AypInSchoolGroupDetails();
            record.setBaseEntityId(event.getBaseEntityId());
            record.setProviderId(event.getProviderId());
            record.setGroupName(getObsStringValue(event, "group_name"));
            record.setGroupType(getObsStringValue(event, "group_type"));
            record.setAgeBand(getObsStringValue(event, "age_band"));
            record.setLastInteractedWith(System.currentTimeMillis());
            new AypOutSchoolGroupDetailsRepository().save(record);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void saveAypGroupMembership(Event event) {
        try {
            if (event == null) return;
            // Extract values from Obs to match how AypInSchoolGroupProfileActivity.saveMembershipByEvent creates the event
            String groupId = getObsStringValue(event, "group_id");
            String membersCsv = getObsStringValue(event, "members");
            if (groupId == null || membersCsv == null) return;
            String providerId = event.getProviderId();
            List<String> ids = new ArrayList<>();
            for (String s : membersCsv.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) ids.add(t);
            }
            if (!ids.isEmpty()) {
                new AypInSchoolGroupMembersRepository().addMembers(groupId, ids, providerId);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void saveAypOutGroupMembership(Event event) {
        try {
            if (event == null) return;
            // Extract values from Obs to match how AypInSchoolGroupProfileActivity.saveMembershipByEvent creates the event
            String groupId = getObsStringValue(event, "group_id");
            String membersCsv = getObsStringValue(event, "members");
            if (groupId == null || membersCsv == null) return;
            String providerId = event.getProviderId();
            java.util.List<String> ids = new java.util.ArrayList<>();
            for (String s : membersCsv.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) ids.add(t);
            }
            if (!ids.isEmpty()) {
                new AypOutSchoolGroupMembersRepository().addMembers(groupId, ids, providerId);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void saveAypOutGroupFollowUpVisit(Event event) {
        try {
            if (event == null) return;
            // Extract values from Obs to match how AypInSchoolGroupProfileActivity.saveMembershipByEvent creates the event
            String groupId = getObsStringValue(event, "group_id");
            List<String> membersCsv = getObsArrayValue(event, "members_present");
            String providedSbcService = getObsStringValue(event, "provided_sbc_service");
            String nextAppointmentDate = getObsStringValue(event, "next_appointment_date");
            List<String> chooseSbcServiceProvided = getObsArrayValue(event, "choose_sbc_service_provided");
            List<String> economicEmpowermentServices = getObsArrayValue(event, "choose_economic_empowerment_services");

            //to be removed, on live
            if(groupId == null){
                groupId = "ffa4b7e9-d4f8-414a-8e0d-7a589486dd29";
            }

            if (groupId == null || membersCsv == null) return;
            String providerId = event.getProviderId();
            java.util.List<String> ids = membersCsv;
            if (!ids.isEmpty()) {
                new AypOutSchoolGroupMembersRepository().addFollowUpForMembers(groupId, ids, providerId, providedSbcService, chooseSbcServiceProvided, economicEmpowermentServices,nextAppointmentDate);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private String getObsStringValue(Event event, String field) {
        try {
            if (event == null || event.getObs() == null) return null;
            for (Obs o : event.getObs()) {
                String key = o.getFormSubmissionField() != null ? o.getFormSubmissionField() : o.getFieldCode();
                if (key != null && key.equalsIgnoreCase(field)) {
                    List<Object> vals = o.getValues();
                    if (vals != null && !vals.isEmpty()) {
                        Object v = vals.get(0);
                        return v != null ? String.valueOf(v) : null;
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }

    private List<String> getObsArrayValue(Event event, String field) {
        List<String> result = new ArrayList<>();

        try {
            if (event == null || event.getObs() == null) return result;

            for (Obs o : event.getObs()) {
                String key = o.getFormSubmissionField() != null
                        ? o.getFormSubmissionField()
                        : o.getFieldCode();

                if (key != null && key.equalsIgnoreCase(field)) {
                    List<Object> vals = o.getValues();
                    if (vals != null) {
                        for (Object v : vals) {
                            if (v != null) {
                                result.add(String.valueOf(v));
                            }
                        }
                    }
                    break; // field found, stop looping
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return result;
    }

}
