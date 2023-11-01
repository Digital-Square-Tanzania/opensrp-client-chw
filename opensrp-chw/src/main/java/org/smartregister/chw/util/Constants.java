package org.smartregister.chw.util;

import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.assetManager;
import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.locale;

import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.Utils;

public class Constants extends CoreConstants {
    public static final String REFERRAL_TASK_FOCUS = "referral_task_focus";
    public static final String REFERRAL_TYPES = "ReferralTypes";
    public static final String APP_VERSION = "app_version";
    public static final String DB_VERSION = "db_version";
    public static final String MALARIA_REFERRAL_FORM = "malaria_referral_form";
    public static final String ICCM_REFERRAL_FORM = "referrals/iccm_referral_form";
    public static final String ALL_CLIENT_REGISTRATION_FORM = "all_clients_registration_form";
    public static String pregnancyOutcome = "preg_outcome";
    public static String FAMILY_MEMBER_LOCATION_TABLE = "ec_family_member_location";
    public static String CHILD_OVER_5 = "child_over_5";

    public enum FamilyRegisterOptionsUtil {Miscarriage, Other}

    public enum FamilyMemberType {ANC, PNC, Other}

    public static class FORM_SUBMISSION_FIELD {
        public static String pncHfNextVisitDateFieldType = "pnc_hf_next_visit_date";

    }
    public static class JsonFormConstants{
        public static String CLIENT_MOVED_LOCATION = "client_moved_location";
        public static final String NAME_OF_HF = "name_of_hf";
        public static final String STEP1 = "step1";
    }

    public static class EncounterType {
        public static final String SICK_CHILD = "Sick Child Referral";
        public static final String PNC_REFERRAL = "PNC Referral";
        public static final String ANC_REFERRAL = "ANC Referral";
        public static final String PMTCT_COMMUNITY_FOLLOWUP_FEEDBACK = "PMTCT Community Followup Feedback";
        public static final String MOTHER_CHAMPION_FOLLOWUP = "Mother Champion Followup";
    }

    public static class ChildIllnessViewType {
        public static final int RADIO_BUTTON = 0;
        public static final int EDIT_TEXT = 1;
        public static final int CHECK_BOX = 2;
    }

    public static class ReportParameters {
        public static String COMMUNITY = "COMMUNITY";
        public static String COMMUNITY_ID = "COMMUNITY_ID";
        public static String REPORT_DATE = "REPORT_DATE";
        public static String INDICATOR_CODE = "INDICATOR_CODE";
    }

    public static class PeerToPeerUtil {
        public static String COUNTRY_ID = "COUNTRY_ID";
    }

    public static class AncHomeVisitUtil {
        private static final String DELIVERY_KIT_RECEIVED = "anc_woman_delivery_kit_received";

        public static String getDeliveryKitReceived() {
            return Utils.getLocalForm(DELIVERY_KIT_RECEIVED, JSON_FORM.locale, JSON_FORM.assetManager);
        }

    }
    public interface PartnerRegistrationConstants {
        String PARTNER_REGISTRATION_EVENT = "Partner Registration";
        int EXISTING_PARTNER_REQUEST_CODE = 12344;
        int NEW_PARTNER_REQUEST_CODE = 12345;
        String INTENT_BASE_ENTITY_ID = "BASE_ENTITY_ID";
        String PARTNER_BASE_ENTITY_ID = "partner_base_entity_id";
        String FEEDBACK_FORM_ID = "feedback_form_id";
        String FormSubmissionId = "formSubmissionId";
        String INTENT_FORM_SUBMISSION_ID = "form_submission_id";
        String REFERRAL_FORM_SUBMISSION_ID = "referral_form_submission_id";
        String ReferralFormId = "referral_form_id";
    }

    public static class CBHSJsonForms {
        private static final String CBHS_FOLLOWUP_FORM = "cbhs_followup_form";

        public static String getCbhsFollowupForm() {
            return CBHS_FOLLOWUP_FORM;
        }
    }

    public static final class JsonForm{
        private static final String PARTNER_REGISTRATION_FORM = "male_partner_registration_form";
        private static final String PMTCT_COMMUNITY_FOLLOWUP_FEEDBACK = "pmtct_community_followup_feedback";
        private static final String MOTHER_CHAMPION_FOLLOWUP_FORM = "mother_champion_followup";
        private static final String MOTHER_CHAMPION_SBCC_FORM = "mother_champion_sbcc_sessions";
        private static final String CBHS_REGISTRATION_FORM = "cbhs_registration";
        private static final String PNC_HV_LOCATION = "pnc_hv_location";
        private static final String CHILD_HV_BREASTFEEDING_FORM = "child_hv_breastfeeding_form";
        private static  final  String CHILD_HOME_VISIT_DANGER_SIGN_FORM = "child_hv_danger_sign";
        private static final String CHILD_HV_PROBLEM_SOLVING_FORM = "child_hv_problem_solving";
        private static final String CHILD_HV_NEWBORN_CARE_INTRO_FORM = "child_hv_newborn_introduction";
        private static final String ICCM_MEDICAL_HISTORY = "iccm_medical_history";
        private static final String ICCM_PHYSICAL_EXAMINATION = "iccm_physical_examination";
        private static final String ICCM_MALARIA = "iccm_malaria";
        private static final String ICCM_PNEUMONIA = "iccm_pneumonia";
        private static final String ICCM_DIARRHEA = "iccm_diarrhea";
        private static final String CHILD_HV_MALNUTRITION_SCREENING = "child_hv_malnutrition_screening";

        public static final String SKIN_TO_SKIN = "child_skin_to_skin";

        public static String getCbhsRegistrationForm() {
            return CBHS_REGISTRATION_FORM;
        }

        public static String getMotherChampionFollowupForm() {
            return MOTHER_CHAMPION_FOLLOWUP_FORM;
        }

        public static String getPmtctCommunityFollowupFeedback() {
            return PMTCT_COMMUNITY_FOLLOWUP_FEEDBACK;
        }

        public static String getPartnerRegistrationForm() {
            return Utils.getLocalForm(PARTNER_REGISTRATION_FORM, locale, assetManager);
        }

        public static String getMotherChampionSbccForm() {
            return MOTHER_CHAMPION_SBCC_FORM;
        }

        public static String getPncHvLocation() {
            return PNC_HV_LOCATION;
        }
      
        public static String getChildHvBreastfeedingForm() {
            return CHILD_HV_BREASTFEEDING_FORM;
        }
      
        public static String getChildHomeVisitDangerSignForm() {
            return CHILD_HOME_VISIT_DANGER_SIGN_FORM;
        }
      
        public static String getChildHvProblemSolvingForm() {
            return CHILD_HV_PROBLEM_SOLVING_FORM;
        }

        public static String getChildHvNewBornCareIntroForm() {
            return CHILD_HV_NEWBORN_CARE_INTRO_FORM;
        }

        public static String getIccmMedicalHistory() {
            return ICCM_MEDICAL_HISTORY;
        }
        public static String getIccmPhysicalExamination() {
            return ICCM_PHYSICAL_EXAMINATION;
        }
        public static String getIccmMalaria() {
            return ICCM_MALARIA;
        }
        public static String getIccmPneumonia() {
            return ICCM_PNEUMONIA;
        }
        public static String getIccmDiarrhea() {
            return ICCM_DIARRHEA;
        }

        public static String getChildHvMalnutritionScreening() {
            return CHILD_HV_MALNUTRITION_SCREENING;
        }

        public static String getSkinToSkin() { return SKIN_TO_SKIN; }

    }

    public static final class Events {
        public static final String UPDATE_MALARIA_CONFIGURATION = "Update Malaria Confirmation";
        public static final String MALARIA_CONFIRMATION = "malaria_confirmation";
        public static final String ANC_FIRST_FACILITY_VISIT = "ANC First Facility Visit";
        public static final String ANC_RECURRING_FACILITY_VISIT = "ANC Recurring Facility Visit";
        public static final String MOTHER_CHAMPION_FOLLOWUP = "Mother Champion Followup";
        public static final String CBHS_FOLLOWUP = "CBHS Followup";
        public static final String CBHS_CLOSE_VISITS = "CBHS Close Visits";
        public static final String AGYW_STRUCTURAL_SERVICES = "AGYW Structural Services";
        public static final String AGYW_BEHAVIORAL_SERVICES = "AGYW Behavioral Services";
        public static final String AGYW_BIO_MEDICAL_SERVICES = "AGYW Bio Medical Services";
        public static final String KVP_PREP_FOLLOWUP_VISIT = "Kvp PrEP Follow-up Visit";
    }

    public static final class ActionList {
        public static final String PMTCT_FOLLOWUP_FEEDBACK = "Pmtct_followup_action";
    }

    public static class TableName {
        public static final String MOTHER_CHAMPION_FOLLOWUP = "ec_mother_champion_followup";
        public static final String SBCC = "ec_sbcc";
        public static final String CBHS_REGISTER = "ec_cbhs_register";
    }

    public static class DBConstants{
        public static final String SBCC_DATE = "sbcc_date";
    }

    public interface PmtctFollowupFeedbackConstants {
        String referralFormId = "community_referral_form_id";
    }
    public static final class ReportConstants {

        public interface ReportTypes {
           String CBHS_REPORT = "cbhs_report";
           String MOTHER_CHAMPION_REPORT = "mother_champion_report";
           String CONDOM_DISTRIBUTION_REPORT = "condom_distribution_report";
           String AGYW_REPORT = "agyw_report";
           String ICCM_REPORT = "iccm_report";
        }

        public interface CDPReportKeys {
            String ISSUING_REPORTS = "issuing_reports";
            String RECEIVING_REPORTS = "receiving_reports";
        }

        public interface ICCMReportKeys {
            String CLIENTS_MONTHLY_REPORT = "iccm-clients-monthly-report";
            String DISPENSING_SUMMARY = "iccm-dispensing-summary";

            String MALARIA_MONTHLY_REPORT = "iccm-malaria-monthly-report";
        }

        public interface ReportPaths {
            String CBHS_REPORT_PATH = "cbhs-taarifa-ya-mwezi";
            String MOTHER_CHAMPION_REPORT_PATH = "mother-champion-report";
            String CONDOM_DISTRIBUTION_ISSUING_REPORT_PATH = "condom-distribution-issuing-report";
            String CONDOM_DISTRIBUTION_RECEIVING_REPORT_PATH = "condom-distribution-receiving-report";
            String ICCM_CLIENTS_REPORT_PATH = "iccm_reports/iccm-clients-monthly-report";
            String ICCM_DISPENSING_SUMMARY_PATH = "iccm_reports/iccm-dispensing-summary";
            String MALARIA_MONTHLY_REPORT_PATH = "iccm_reports/iccm-malaria-monthly-report";
        }
    }
}
