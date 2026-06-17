package org.smartregister.chw.util;

import static org.smartregister.chw.malaria.util.Constants.EVENT_TYPE.ICCM_SERVICES_VISIT;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.kvp.util.TimeUtils;
import org.smartregister.chw.malaria.MalariaLibrary;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.malaria.domain.Visit;
import org.smartregister.chw.malaria.domain.VisitDetail;
import org.smartregister.chw.malaria.repository.VisitDetailsRepository;
import org.smartregister.chw.malaria.repository.VisitRepository;
import org.smartregister.chw.malaria.util.VisitUtils;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.util.FormUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import timber.log.Timber;

public class IccmVisitUtils extends VisitUtils {
    public static String Complete = "complete";

    public static String Pending = "pending";

    public static String Ongoing = "ongoing";

    /**
     * To be invoked for manual processing
     *
     * @param baseEntityID of the client
     * @throws Exception that occured
     */
    public static void processVisits(String baseEntityID) throws Exception {
        processVisits(MalariaLibrary.getInstance().visitRepository(), MalariaLibrary.getInstance().visitDetailsRepository(), baseEntityID);
    }

    public static void processVisits() throws Exception {
        processIccmVisits(MalariaLibrary.getInstance().visitRepository(), MalariaLibrary.getInstance().visitDetailsRepository());
    }

    public static void processVisits(VisitRepository visitRepository, VisitDetailsRepository visitDetailsRepository, String baseEntityID) throws Exception {
        Calendar calendar = Calendar.getInstance();

        List<Visit> visits = StringUtils.isNotBlank(baseEntityID) ? visitRepository.getAllUnSynced(calendar.getTime().getTime(), baseEntityID) : visitRepository.getAllUnSynced(calendar.getTime().getTime());
        List<Visit> iccmServicesVisits = new ArrayList<>();

        for (Visit v : visits) {
            Date visitDate = new Date(v.getDate().getTime());
            int daysDiff = TimeUtils.getElapsedDays(visitDate);
            if (daysDiff >= 1 && v.getVisitType().equalsIgnoreCase(ICCM_SERVICES_VISIT) && isIccmVisitComplete(v)) {
                try {
                    iccmServicesVisits.add(v);
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
        }
        if (iccmServicesVisits.size() > 0) {
            processVisits(iccmServicesVisits, visitRepository, visitDetailsRepository);
            processIccmReferrals(iccmServicesVisits);
        }
    }

    public static void processIccmVisits(VisitRepository visitRepository, VisitDetailsRepository visitDetailsRepository) throws Exception {

        List<Visit> visits = visitRepository.getAllUnSynced();
        List<Visit> iccmServicesVisits = new ArrayList<>();

        for (Visit v : visits) {
            Date visitDate = new Date(v.getDate().getTime());
            int daysDiff = TimeUtils.getElapsedDays(visitDate);
            if (daysDiff >= 1 && v.getVisitType().equalsIgnoreCase(ICCM_SERVICES_VISIT) && isIccmVisitComplete(v)) {
                try {
                    iccmServicesVisits.add(v);
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
        }

        if (iccmServicesVisits.size() > 0) {
            processVisits(iccmServicesVisits, visitRepository, visitDetailsRepository);
            processIccmReferrals(iccmServicesVisits);
        }
    }


    public static boolean isIccmVisitComplete(Visit visit) {
        boolean isComplete = false;
        if (visit.getVisitType().equalsIgnoreCase(ICCM_SERVICES_VISIT)) {
            try {
                JSONObject jsonObject = new JSONObject(visit.getJson());
                JSONArray obs = jsonObject.getJSONArray("obs");
                HashMap<String, Boolean> completionObject = new HashMap<>();
                completionObject.put("isMedicalHistoryDone", computeCompletionStatusForAction(obs, "medical_history_completion_status"));

                String clientPastMalariaTreatmentHistory = getFieldValue(obs, "client_past_malaria_treatment_history");

                if (StringUtils.isBlank(clientPastMalariaTreatmentHistory) || clientPastMalariaTreatmentHistory.equalsIgnoreCase("no")) {
                    completionObject.put("isPhysicalExaminationComplete", computeCompletionStatusForAction(obs, "physical_examination_completion_status"));
                    String isMalariaSuspect = getFieldValue(obs, "is_malaria_suspect");
                    String physicalExamination = getFieldValue(obs, "physical_examination");
                    String isMalariaSuspectAfterPhysicalExamination = getFieldValue(obs, "is_malaria_suspect_after_physical_examination");
                    if (shouldPopulateMalariaActionAfterPhysicalExamination(
                            isMalariaSuspect,
                            physicalExamination,
                            isMalariaSuspectAfterPhysicalExamination,
                            clientPastMalariaTreatmentHistory)) {
                        completionObject.put("isMalariadDiagnosisComplete", computeCompletionStatusForAction(obs, "malaria_completion_status"));
                    }
                }

                if (IccmDao.getMemberByBaseEntityId(visit.getBaseEntityId()).getAge() < 5) {
                    String isDiarrheaSuspect = getFieldValue(obs, "is_diarrhea_suspect");
                    if (isDiarrheaSuspect != null && isDiarrheaSuspect.equalsIgnoreCase("true")) {
                        completionObject.put("isDiarrheaDiagnosisComplete", computeCompletionStatusForAction(obs, "diarrhea_completion_status"));
                    }
                }

                if (isIccmReferralRequired(obs)) {
                    completionObject.put("isReferralComplete", hasRequiredReferralValues(obs));
                }

                if (!completionObject.containsValue(false)) {
                    isComplete = true;
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        return isComplete;
    }

    public static boolean computeCompletionStatusForAction(JSONArray obs, String checkString) throws JSONException {
        int size = obs.length();
        for (int i = 0; i < size; i++) {
            JSONObject checkObj = obs.getJSONObject(i);
            if (checkObj.getString("fieldCode").equalsIgnoreCase(checkString)) {
                String status = checkObj.getJSONArray("values").getString(0);
                return status.equalsIgnoreCase("complete");
            }
        }
        return false;
    }

    public static String getActionStatus(Map<String, Boolean> checkObject) {
        for (Map.Entry<String, Boolean> entry : checkObject.entrySet()) {
            if (entry.getValue()) {
                if (checkObject.containsValue(false)) {
                    return Ongoing;
                }
                return Complete;
            }
        }
        return Pending;
    }

    public static boolean shouldPopulateMalariaActionAfterPhysicalExamination(String isMalariaSuspect,
                                                                              String physicalExamination,
                                                                              String isMalariaSuspectAfterPhysicalExamination,
                                                                              String clientPastMalariaTreatmentHistory) {
        if (IccmReferralActionUtils.isYes(clientPastMalariaTreatmentHistory)) {
            return false;
        }

        return IccmReferralActionUtils.isTrue(isMalariaSuspect)
                || isMalariaSuspectAfterPhysicalExamination(isMalariaSuspect, physicalExamination, isMalariaSuspectAfterPhysicalExamination);
    }

    public static boolean isMalariaSuspectAfterPhysicalExamination(String isMalariaSuspect,
                                                                   String physicalExamination,
                                                                   String isMalariaSuspectAfterPhysicalExamination) {
        return IccmReferralActionUtils.isTrue(isMalariaSuspect)
                || IccmReferralActionUtils.isTrue(isMalariaSuspectAfterPhysicalExamination)
                || hasMalariaPhysicalExaminationFinding(physicalExamination);
    }

    public static boolean hasMalariaPhysicalExaminationFinding(String physicalExamination) {
        String normalizedValue = StringUtils.trimToEmpty(physicalExamination);
        if (StringUtils.isBlank(normalizedValue)
                || "[]".equals(normalizedValue)
                || "{}".equals(normalizedValue)
                || "null".equalsIgnoreCase(normalizedValue)) {
            return false;
        }

        return !StringUtils.containsIgnoreCase(normalizedValue, "none");
    }

    public static boolean manualProcessVisit(Visit visit) throws Exception {
        if (visit == null) {
            return false;
        }

        List<Visit> manualProcessedVisits = new ArrayList<>();
        VisitDetailsRepository visitDetailsRepository = MalariaLibrary.getInstance().visitDetailsRepository();
        VisitRepository visitRepository = MalariaLibrary.getInstance().visitRepository();
        manualProcessedVisits.add(visit);
        processVisits(manualProcessedVisits, visitRepository, visitDetailsRepository);

        return processIccmReferral(visit);
    }

    public static String getFieldValue(JSONArray obs, String checkString) throws JSONException {
        int size = obs.length();
        for (int i = 0; i < size; i++) {
            JSONObject jsonObject = obs.getJSONObject(i);
            if (jsonObject.getString("fieldCode").equalsIgnoreCase(checkString)) {
                JSONArray values = jsonObject.getJSONArray("values");
                return values.getString(0);
            }
        }
        return null;
    }

    public static boolean processIccmReferral(Visit manualProcessedVisits) throws Exception {
        if (manualProcessedVisits == null
                || StringUtils.isBlank(manualProcessedVisits.getVisitId())
                || StringUtils.isBlank(manualProcessedVisits.getBaseEntityId())) {
            return false;
        }

        VisitDetailsRepository detailsRepository = MalariaLibrary.getInstance().visitDetailsRepository();
        List<VisitDetail> visitDetailList = detailsRepository.getVisits(manualProcessedVisits.getVisitId());
        if (visitDetailList == null || visitDetailList.isEmpty()) {
            return false;
        }

        Map<String, List<VisitDetail>> visitDetails = getVisitGroups(visitDetailList);
        if (visitDetails == null || visitDetails.isEmpty()) {
            return false;
        }

        JSONObject referralFormJson = FormUtils.getInstance(MalariaLibrary.getInstance().context().applicationContext())
                .getFormJson(Constants.JsonForm.getIccmReferral());
        if (referralFormJson == null) {
            return false;
        }
        org.smartregister.chw.malaria.util.JsonFormUtils.populateForm(referralFormJson, visitDetails);

        if (!hasRequiredReferralValues(referralFormJson)) {
            return false;
        }

        JSONObject appointmentDateJsonObject = CoreJsonFormUtils.getJsonField(referralFormJson, "step1", "referral_appointment_date");
        String appointmentDateFieldValue = CoreJsonFormUtils.getValue(referralFormJson, "referral_appointment_date");
        String normalizedDateValue = appointmentDateFieldValue != null ? appointmentDateFieldValue.trim() : "";
        Long epochMillis = normalizedDateValue.isEmpty() ? null : parseDateToEpochMillis(normalizedDateValue);
        if (appointmentDateJsonObject != null && epochMillis != null) {
            appointmentDateJsonObject.put("value", String.valueOf(epochMillis));
        }

        String referralProblems = JsonFormUtils.getCheckBoxValue(referralFormJson, "problem");
        ReferralUtils.processReferral(referralFormJson.toString(), manualProcessedVisits.getBaseEntityId(),
                CoreConstants.TASKS_FOCUS.ICCM_REFERRAL, referralProblems);

        return true;
    }

    private static void processIccmReferrals(List<Visit> visits) {
        if (visits == null || visits.isEmpty()) {
            return;
        }

        for (Visit visit : visits) {
            try {
                processIccmReferral(visit);
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    private static boolean hasRequiredReferralValues(JSONObject referralFormJson) {
        String problem = CoreJsonFormUtils.getValue(referralFormJson, "problem");
        String problemOther = CoreJsonFormUtils.getValue(referralFormJson, "problem_other");
        String serviceBeforeReferral = CoreJsonFormUtils.getValue(referralFormJson, "service_before_referral");
        String referralFacility = CoreJsonFormUtils.getValue(referralFormJson, "chw_referral_hf");
        String referralAppointmentDate = CoreJsonFormUtils.getValue(referralFormJson, "referral_appointment_date");

        if (!hasValue(problem) || !hasValue(serviceBeforeReferral) || !hasValue(referralFacility) || !hasValue(referralAppointmentDate)) {
            return false;
        }
        if (containsOption(problem, "other_reasons")) {
            return hasValue(problemOther);
        }

        return true;
    }

    private static boolean isIccmReferralRequired(JSONArray obs) throws JSONException {
        String clientPastMalariaTreatmentHistory = getFieldValue(obs, "client_past_malaria_treatment_history");
        if ("yes".equalsIgnoreCase(StringUtils.trimToEmpty(clientPastMalariaTreatmentHistory))) {
            return true;
        }

        String isPneumoniaSuspect = getFieldValue(obs, "is_pneumonia_suspect");
        if ("true".equalsIgnoreCase(StringUtils.trimToEmpty(isPneumoniaSuspect))) {
            return true;
        }

        String diarrheaSigns = getFieldValue(obs, "diarrhea_signs");
        if (hasValue(diarrheaSigns) && !containsOption(diarrheaSigns, "none")) {
            return true;
        }

        String interpretationForMrdtTwo = getFieldValue(obs, "interpretation_for_mrdt_two");
        if (hasValue(interpretationForMrdtTwo) && !containsOption(interpretationForMrdtTwo, "control")) {
            return true;
        }

        return hasValue(getFieldValue(obs, "problem"))
                || hasValue(getFieldValue(obs, "service_before_referral"))
                || hasValue(getFieldValue(obs, "chw_referral_hf"))
                || hasValue(getFieldValue(obs, "referral_appointment_date"));
    }

    private static boolean hasRequiredReferralValues(JSONArray obs) throws JSONException {
        String problem = getFieldValue(obs, "problem");
        String problemOther = getFieldValue(obs, "problem_other");
        String serviceBeforeReferral = getFieldValue(obs, "service_before_referral");
        String referralFacility = getFieldValue(obs, "chw_referral_hf");
        String referralAppointmentDate = getFieldValue(obs, "referral_appointment_date");

        if (!hasValue(problem) || !hasValue(serviceBeforeReferral) || !hasValue(referralFacility) || !hasValue(referralAppointmentDate)) {
            return false;
        }

        if (containsOption(problem, "other_reasons")) {
            return hasValue(problemOther);
        }

        return true;
    }

    private static boolean hasValue(String value) {
        String normalizedValue = StringUtils.trimToEmpty(value);
        return StringUtils.isNotBlank(normalizedValue)
                && !"[]".equals(normalizedValue)
                && !"{}".equals(normalizedValue)
                && !"null".equalsIgnoreCase(normalizedValue);
    }

    private static boolean containsOption(String value, String optionKey) {
        if (!hasValue(value) || !hasValue(optionKey)) {
            return false;
        }

        return StringUtils.containsIgnoreCase(value, optionKey);
    }

    private static Long parseDateToEpochMillis(String dateValue) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            format.setLenient(false);
            Date date = format.parse(dateValue);
            return date != null ? date.getTime() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
