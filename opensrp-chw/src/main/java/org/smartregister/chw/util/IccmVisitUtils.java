package org.smartregister.chw.util;

import static org.smartregister.chw.malaria.util.Constants.EVENT_TYPE.ICCM_SERVICES_VISIT;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.kvp.util.TimeUtils;
import org.smartregister.chw.malaria.MalariaLibrary;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.malaria.domain.Visit;
import org.smartregister.chw.malaria.repository.VisitDetailsRepository;
import org.smartregister.chw.malaria.repository.VisitRepository;
import org.smartregister.chw.malaria.util.VisitUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    String isMalariaSuspect = getFieldValue(obs, "is_malaria_suspect_after_physical_examination");
                    if (isMalariaSuspect != null && isMalariaSuspect.equalsIgnoreCase("true")) {
                        completionObject.put("isMalariadDiagnosisComplete", computeCompletionStatusForAction(obs, "malaria_completion_status"));
                    }
                }

                if (Utils.getAgeFromDate(IccmDao.getMemberByBaseEntityId(visit.getBaseEntityId()).getAge()) < 5) {
                    String isDiarrheaSuspect = getFieldValue(obs, "is_diarrhea_suspect");
                    if (isDiarrheaSuspect != null && isDiarrheaSuspect.equalsIgnoreCase("true")) {
                        completionObject.put("isDiarrheaDiagnosisComplete", computeCompletionStatusForAction(obs, "diarrhea_completion_status"));
                    }

                    String isPneumoniaSuspect = getFieldValue(obs, "is_pneumonia_suspect");
                    if (isPneumoniaSuspect != null && isPneumoniaSuspect.equalsIgnoreCase("true")) {
                        completionObject.put("isPneumoniaDiagnosisComplete", computeCompletionStatusForAction(obs, "pneumonia_completion_status"));
                    }
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

    public static void manualProcessVisit(Visit visit) throws Exception {
        List<Visit> manualProcessedVisits = new ArrayList<>();
        VisitDetailsRepository visitDetailsRepository = MalariaLibrary.getInstance().visitDetailsRepository();
        VisitRepository visitRepository = MalariaLibrary.getInstance().visitRepository();
        manualProcessedVisits.add(visit);
        processVisits(manualProcessedVisits, visitRepository, visitDetailsRepository);
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
}
