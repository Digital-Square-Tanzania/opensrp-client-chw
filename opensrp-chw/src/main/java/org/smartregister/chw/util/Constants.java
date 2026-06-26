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
    public static final String CECAP_MALE_REFERRAL_FORM = "referrals/cecap_male_referral_form";
    public static final String CECAP_FEMALE_REFERRAL_FORM = "referrals/cecap_female_referral_form";
    public static final String ALL_CLIENT_REGISTRATION_FORM = "all_clients_registration_form";
    public static String pregnancyOutcome = "preg_outcome";
    public static String FAMILY_MEMBER_LOCATION_TABLE = "ec_family_member_location";
    public static String CHILD_OVER_5 = "child_over_5";
    public static final String EXTRA_CLIENT_PICKER_MODE = "client_picker_mode";
    public static final String EXTRA_EXISTING_HEAD_CLIENT = "existing_head_client";

    public static final String ADDO_LINKAGE_PLAN_ID = "6270285b-5a3b-4647-b772-c0b3c52e2b72";

    public static String FILTER_APPOINTMENT_DATE = "FILTER_APPOINTMENT_DATE";
    public static String FILTER_APPOINTMENT_DATE_RANGE_START_DATE = "FILTER_APPOINTMENT_DATE_RANGE_START_DATE";
    public static String FILTER_APPOINTMENT_DATE_RANGE_END_DATE = "FILTER_APPOINTMENT_DATE_RANGE_END_DATE";
    public static String FILTER_HIV_STATUS = "FILTER_HIV_STATUS";
    public static String FILTER_PREP_STATUS = "FILTER_PREP_STATUS";
    public static String FILTERS_ENABLED = "FILTERS_ENABLED";
    public static String ENABLE_HIV_STATUS_FILTER = "ENABLE_HIV_STATUS_FILTER";
    public static String ENABLE_PREP_STATUS_FILTER = "ENABLE_PREP_STATUS_FILTER";
    public static String ENABLE_DATE_RANGE_FILTER = "ENABLE_DATE_RANGE_FILTER";
    public static String ENTITY_TYPE_EC_FAMILY_MEMBER = "ec_family_member";
    public static String ENTITY_TYPE_EC_FAMILY = "ec_family";
    public static String ENTITY_TYPE_EC_INDEPENDENT_CLIENT = "ec_independent_client";

    public static int REQUEST_FILTERS = 2004;

    public enum PneumoniaStatus {ENABLED, DISABLED}

    public enum FamilyRegisterOptionsUtil {Miscarriage, Other}

    public enum FamilyMemberType {ANC, PNC, Other}

    public static class FORM_SUBMISSION_FIELD {
        public static String pncHfNextVisitDateFieldType = "pnc_hf_next_visit_date";

    }
    public static class JsonFormConstants{
        public static String CLIENT_MOVED_LOCATION = "client_moved_location";
        public static final String NAME_OF_HF = "name_of_hf";
        public static final String STEP1 = "step1";
        public static final String MOTHER_AVAILABLE = "mother_available";
        public static final String OTHER_CAREGIVER_NAME = "other_caregiver_name";
        public static final String CAREGIVER_NAME = "caregiver_name";
    }

    public static class EncounterType {
        public static final String SICK_CHILD = "Sick Child Referral";
        public static final String PNC_REFERRAL = "PNC Referral";
        public static final String ANC_REFERRAL = "ANC Referral";
        public static final String PMTCT_COMMUNITY_FOLLOWUP_FEEDBACK = "PMTCT Community Followup Feedback";
        public static final String MOTHER_CHAMPION_FOLLOWUP = "Mother Champion Followup";
        public static final String LINKAGE_FOLLOWUP = "Linkage Followup";
        public static final String NCD_MONTHLY_FOLLOWUP = "NCD Monthly Follow-Up";
        public static final String NCD_CASE_MANAGEMENT_CLOSE = "NCD Case Management Close";
    }

    public static class ScheduleType {
        public static final String NCD_CASE_MANAGEMENT_VISIT = "NCD_CASE_MANAGEMENT_VISIT";
    }

    public static class NcdReferral {
        public static final String URGENT_REFERRAL_CODE = "Referral";
        public static final String NON_EMERGENCY_REFERRAL_CODE = "ncd_non_emergency_referral";
        public static final String FOCUS_NCD_DANGER_SIGNS = "NCD Danger Signs";
        public static final String FOCUS_NCD_CLINICAL_CONCERN = "NCD Clinical Concern";
        // Coded problem keys used as a fallback when no specific reason was captured,
        // so the referral event's "problem" obs still carries a key alongside its human-readable value.
        public static final String PROBLEM_KEY_DANGER_SIGNS = "ncd_danger_signs";
        public static final String PROBLEM_KEY_CLINICAL_CONCERN = "ncd_clinical_concern";
        // Concept keys for the emergency-case and treatment-supporter obs captured on the
        // post-visit referral prompt and emitted on the auto-generated Referral Registration
        // event (mirrors the fields on ncd_referral_form.json).
        public static final String IS_EMERGENCY_CASE = "is_emergency_case";
        public static final String HAS_TREATMENT_SUPPORTER = "has_treatment_supporter";
        public static final String TREATMENT_SUPPORTER_NAME = "treatment_supporter_name";
        public static final String TREATMENT_SUPPORTER_PHONE = "treatment_supporter_phone";
        public static final String TREATMENT_SUPPORTER_RELATIONSHIP = "treatment_supporter_relationship";
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
        private static final String TBLEPROSY_REGISTRATION_FORM = "tbleprosy_record_visit";
        private static final String PNC_HV_LOCATION = "pnc_hv_location";
        private static final String CHILD_HV_BREASTFEEDING_FORM = "child_hv_breastfeeding_form";
        private static  final  String CHILD_HOME_VISIT_DANGER_SIGN_FORM = "child_hv_danger_sign";
        private static final String CHILD_HV_PROBLEM_SOLVING_FORM = "child_hv_problem_solving";
        private static final String CHILD_HV_NEWBORN_CARE_INTRO_FORM = "child_hv_newborn_introduction";
        private static final String CHILD_HV_PLAY_ASSESSMENT_COUNSELLING = "child_hv_play_assessment_counselling";
        private static final String CHILD_HV_DEVELOPMENT_SCREENING_ASSESSMENT = "child_hv_development_screening_assessment";
        private static final String CHILD_HV_CORD_CARE = "child_hv_cord_care";
        private static final String ICCM_MEDICAL_HISTORY = "iccm_medical_history";
        private static final String ICCM_PHYSICAL_EXAMINATION = "iccm_physical_examination";
        private static final String ICCM_MALARIA = "iccm_malaria";
        private static final String ICCM_PNEUMONIA = "iccm_pneumonia";
        private static final String ICCM_DIARRHEA = "iccm_diarrhea";

        private static final String ICCM_REFERRAL_NATIVE_FORM = "referrals/iccm_referral_native_form";
        private static final String CHILD_HV_MALNUTRITION_SCREENING = "child_hv_malnutrition_screening";
        private static final String CHILD_HV_COMMUNICATION_ASSESSMENT_COUNSELLING = "child_hv_communication_assessment";

        public static final String SKIN_TO_SKIN = "child_skin_to_skin";
        public static final String DIABETES_SCREENING_FORM = "diabetes_hypertension_screening_form";
        public static final String CHILD_HV_COMP_FEEDING = "child_complementary_feeding";

        public static final String DIABETES_FOLLOWUP_FORM = "diabetes_hypertension_followup_form";
        public static final String NCD_VITALS_FORM = "record_diabetes_hypertension_vital_form";
        public static final String NCD_CLIENT_EDUCATION_FORM = "ncd_client_education_form";
        public static final String NCD_FOLLOWUP_STATUS = "ncd_followup_status";
        public static final String NCD_FOLLOWUP_CLINICAL_ADHERENCE = "ncd_followup_clinical_adherence";
        public static final String NCD_FOLLOWUP_DANGER_SIGNS = "ncd_followup_danger_signs";
        public static final String NCD_FOLLOWUP_LIFESTYLE = "ncd_followup_lifestyle";
        public static final String NCD_FOLLOWUP_PSYCHOSOCIAL = "ncd_followup_psychosocial";
        public static final String NCD_REFERRAL_FORM = "ncd_referral_form";
        public static final String NCD_CASE_MANAGEMENT_CLOSE = "ncd_case_management_close";

        public static final String CHILD_SAFETY_FORM = "child_hv_child_safety";

        private static final String CHILD_HV_CCD_INTRODUCTION = "child_hv_ccd_introduction";
        private static final String CHILD_HV_PMTCT = "child_hv_pmtct";

        private static final String CHILD_HV_CCD_CARE_GIVER_RESPONSIVENESS = "child_hv_caregiver_responsiveness";

        private static final String CHILD_HV_CCD_CHILD_DISCIPLINE = "child_hv_ccd_child_discipline";
        private static final String ANC_HV_PARTNER_ENGAGEMENT = "anc_hv_partner_engagement";


        public static String getNcdReferralForm() {
            return NCD_REFERRAL_FORM;
        }

        public static String getDiabetesScreeningForm() {
            return DIABETES_SCREENING_FORM;
        }

        public static String getDiabetesFollowupForm() {
            return DIABETES_FOLLOWUP_FORM;
        }

        public static String getNcdVitalsForm() {
            return NCD_VITALS_FORM;
        }

        public static String getNcdClientEducationForm() {
            return NCD_CLIENT_EDUCATION_FORM;
        }
        public static String getCbhsRegistrationForm() {
            return CBHS_REGISTRATION_FORM;
        }

        public static String getTbLeprosyForm() {
            return TBLEPROSY_REGISTRATION_FORM;
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

        public static String getChildHvPlayAssessmentCounselling() {
            return CHILD_HV_PLAY_ASSESSMENT_COUNSELLING;
        }

        public static String getChildHvDevelopmentScreeningAssessment() {
            return CHILD_HV_DEVELOPMENT_SCREENING_ASSESSMENT;
        }

        public static String getChildHvCordCare() {
            return CHILD_HV_CORD_CARE;
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

        public static String getIccmPneumonia(){
            return ICCM_PNEUMONIA;
        }

        public static String getIccmDiarrhea(){
            return ICCM_DIARRHEA;
        }

        public static String getIccmReferral() {
            return ICCM_REFERRAL_NATIVE_FORM;
        }

        public static String getChildHvMalnutritionScreening() {
            return CHILD_HV_MALNUTRITION_SCREENING;
        }

        public static String getSkinToSkin() { return SKIN_TO_SKIN; }

        public static String getChildSafetyForm() { return CHILD_SAFETY_FORM; }

        public static String getChildHVCCDIntroduction() {
            return CHILD_HV_CCD_INTRODUCTION;
        }

        public static String getChildHvCompFeeding(){
            return CHILD_HV_COMP_FEEDING;
        }

        public static String getChildHvCcdCareGiverResponsiveness() {
            return CHILD_HV_CCD_CARE_GIVER_RESPONSIVENESS;
        }

        public static String getChildHvCcdChildDiscipline() {
            return CHILD_HV_CCD_CHILD_DISCIPLINE;
        }

        public static String getChildHvPmtct() {
            return CHILD_HV_PMTCT;
        }

        public static String getChildHvCommunicationAssessmentCounselling() {
            return CHILD_HV_COMMUNICATION_ASSESSMENT_COUNSELLING;
        }

        public static String getAncHvPartnerEngagement() {
            return ANC_HV_PARTNER_ENGAGEMENT;
        }
    }

    public static final class Events {
        public static final String UPDATE_MALARIA_CONFIGURATION = "Update Malaria Confirmation";

        public static final String MALARIA_CONFIRMATION = "malaria_confirmation";

        public static final String ANC_FIRST_FACILITY_VISIT = "ANC First Facility Visit";

        public static final String ANC_RECURRING_FACILITY_VISIT = "ANC Recurring Facility Visit";

        public static final String MOTHER_CHAMPION_FOLLOWUP = "Mother Champion Followup";

        public static final String MOTHER_CHAMPION_SBCC_SESSIONS = "Mother Champion SBCC Sessions";

        public static final String CBHS_FOLLOWUP = "CBHS Followup";

        public static final String CBHS_CLOSE_VISITS = "CBHS Close Visits";

        public static final String AGYW_STRUCTURAL_SERVICES = "AGYW Structural Services";

        public static final String AGYW_BEHAVIORAL_SERVICES = "AGYW Behavioral Services";

        public static final String AGYW_BIO_MEDICAL_SERVICES = "AGYW Bio Medical Services";

        public static final String KVP_PREP_FOLLOWUP_VISIT = "Kvp PrEP Follow-up Visit";
        public static final String PLAY_ASSESSMENT_COUNSELLING= "Play Assessment and Counselling";
        public static final String DEVELOPMENT_SCREENING_AND_ASSESSMENT = "Development Screening and Assessment";
        public static final String COMMUNICATION_ASSESSMENT_COUNSELLING= "Communication Assessment and Counselling";

        public static final String AYP_OUT_SCHOOL_FOLLOW_UP_VISIT = "Ayp Out School Client Followup Visit";

        public static final String AYP_OUT_SCHOOL_GROUP_FOLLOW_UP_VISIT = "Ayp Out School Group Followup Visit";

        public static final String PNC_NO_MOTHER_CHILD_REGISTRATION = "PNC No Mother Child Registration";
    }

    public static final class ActionList {
        public static final String PMTCT_FOLLOWUP_FEEDBACK = "Pmtct_followup_action";
    }

    public static class TableName {
        public static final String MOTHER_CHAMPION_FOLLOWUP = "ec_mother_champion_followup";

        public static final String SBCC = "ec_sbcc";

        public static final String CBHS_REGISTER = "ec_cbhs_register";
        public static final String CHILD_NO_MOTHER = "ec_child_no_mother";
        public static final String NCD_CASE_MANAGEMENT_FOLLOWUP = "ec_ncd_case_management_followup";
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
           String ECD_REPORT = "ecd_report";
           String SBC_REPORT = "sbc_report";

           String KVP_REPORT = "kvp_report";

           String AYP_OUT_SCHOOL_REPORT = "ayp_out_school_report";

            String HPS_REPORT = "hps_report";

           String ASRH_REPORT = "asrh_report";

           String HARM_REDUCTION_REPORT = "harm_reduction_report";

           String HARM_REDUCTION_SOBER_HOUSE_REPORT = "harm_reduction_sober_house_report";

           String AYP_REPORT = "ayp_report";

           String AYP_IN_SCHOOL_REPORT = "ayp_in_school_report";

           String CECAP_REPORT = "cecap_report";

           String TBLEPROSY_REPORT = "tbleprosy_report";
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

        public interface ECDReportKeys {
            String CLIENTS_MONTHLY_REPORT = "ecd-clients-monthly-report";
        }


        public interface CecapReportKeys {
            String CLIENTS_MONTHLY_REPORT = "clients-monthly-report";
            String OTHER_MONTHLY_REPORT = "other-monthly-report";
        }


        public interface AsrhReportKeys {
            String CLIENTS_MONTHLY_REPORT = "clients-monthly-report";
            String OTHER_MONTHLY_REPORT = "other-monthly-report";
        }

        public interface HarmReductionReportKeys {
            String CLIENTS_MONTHLY_REPORT = "clients-monthly-report";
        }

        public interface HarmReductionSoberHouseReportKeys {
            String CLIENTS_MONTHLY_REPORT = "clients-monthly-report";
        }

        public interface TbLeprosyReportKeys {
            String COMMUNITY_REPORT = "tbleprosy-community-report";
            String HOUSEHOLD_REPORT = "tbleprosy-household-report";
            String SPECIAL_AREAS_REPORT = "tbleprosy-special-areas-report";
            String TREATMENT_STATUS_REPORT = "tbleprosy-treatment-status-report";
            String SERVICE_CHALLENGES_REPORT = "tbleprosy-service-challenges-report";
        }

        public interface AypReportKeys {
            String AYP_IN_SCHOOL_MONTHLY_REPORT = "ayp-in-school-monthly-report";
            String AYP_PARENTAL_MONTHLY_REPORT = "ayp-parental-monthly-report";
            String AYP_OUT_SCHOOL_MONTHLY_REPORT = "ayp-out-school-monthly-report";
        }

        public interface HpsReportKeys {
            String HPS_MONTHLY_REPORT = "hps-monthly-report";
            String HPS_ANNUAL_REPORT = "hps-annual-report";
        }

        public interface ReportPaths {
            String CBHS_REPORT_PATH = "cbhs-taarifa-ya-mwezi";

            String MOTHER_CHAMPION_REPORT_PATH = "mother-champion-report";

            String CONDOM_DISTRIBUTION_ISSUING_REPORT_PATH = "condom-distribution-issuing-report";

            String CONDOM_DISTRIBUTION_RECEIVING_REPORT_PATH = "condom-distribution-receiving-report";

            String ICCM_CLIENTS_REPORT_PATH = "iccm_reports/iccm-clients-monthly-report";

            String ICCM_DISPENSING_SUMMARY_PATH = "iccm_reports/iccm-dispensing-summary";

            String MALARIA_MONTHLY_REPORT_PATH = "iccm_reports/iccm-malaria-monthly-report";

            String SBC_REPORT_PATH = "sbc-report";
            String ECD_CLIENTS_REPORT_PATH = "ecd_reports/ecd-clients-monthly-report";

            String KVP_REPORT_PATH = "kvp-report";

            String AYP_OUT_SCHOOL_REPORT_PATH = "ayp-out-school-report";

            String HPS_MONTHLY_REPORT_PATH = "hps-monthly-report";

            String HPS_ANNUAL_REPORT_PATH = "hps-annual-report";

            String ASRH_REPORT_PATH = "asrh_reports/asrh-report";

            String ASRH_OTHER_REPORT_PATH = "asrh_reports/asrh-other-report";

            String HARM_REDUCTION_REPORT_PATH = "harm_reduction_reports/harm-reduction-report";

            String HARM_REDUCTION_SOBER_HOUSE_REPORT_PATH = "harm_reduction_sober_house_reports/harm-reduction-sober-house-report";

            String AYP_IN_SCHOOL_REPORT_PATH = "ayp_in_school_reports/ayp-in-school-report";

            String AYP_PARENTAL_REPORT_PATH = "ayp_reports/ayp-parental-report";

            String CECAP_REPORT_PATH = "cecap_reports/cecap-report";

            String CECAP_OTHER_REPORT_PATH = "cecap_reports/cecap-other-report";

            String TBLEPROSY_COMMUNITY_REPORT_PATH = "tbleprosy_reports/tbleprosy-community-report";
            String TBLEPROSY_HOUSEHOLD_REPORT_PATH = "tbleprosy_reports/tbleprosy-household-report";
            String TBLEPROSY_SPECIAL_REPORT_PATH = "tbleprosy_reports/tbleprosy-special-areas-report";
            String TBLEPROSY_TREATMENT_STATUS_REPORT_PATH = "tbleprosy_reports/tbleprosy-treatment-status-report";
            String TBLEPROSY_SERVICE_CHALLENGES_REPORT_PATH = "tbleprosy_reports/tbleprosy-service-challenges-report";
            String TBLEPROSY_MERGED_REPORT_PATH = "tbleprosy_reports/tbleprosy-merged-report";
        }
    }

    public static final class HomeVisitActions {

        public interface ChildHomeVisitActions {
            String CHILD_MINOR_AILMENT = "child_minor_ailment";
        }

        public interface AncHomeVisitActions {

        }

        public interface PncHomeVisitActions {

        }
    }

    public interface AddoLinkage {
        String BUSINESS_STATUS = "Linked";
        String CODE = "Linkage";

        String CHILD_TASK_FOCUS = "Child Minor Ailments";
        String ANC_TASK_FOCUS = "ANC Minor Ailments";
        String PNC_TASK_FOCUS = "PNC Minor Ailments";
    }

    public interface iCCMTreatment{
        String FIELD_SERVICE_BEFORE_REFERRAL = "service_before_referral";
        String FIELD_DISPENSED_ANTI_PYRETIC = "dispensed_anti_pyretic";
        String FIELD_DIARRHEA_MEDICATION_DISPENSED = "diarrhea_medication_dispensed";
        String FIELD_DIARRHEA_MEDICATION_REFERRED_CLIENT = "diarrhea_medication_dispensed_for_referred_clients";
        String TREATMENT_ANTI_PYRETIC = "anti_pyretic";
        String TREATMENT_ORS = "ors";
        String TREATMENT_ZINC_SOURCE = "zinc";
        String TREATMENT_ORS_ZINC_CO_PACK = "ors_zinc_co_pack";
        String TREATMENT_ZINC_ORS_CO_PACK_SOURCE = "zinc_ors_co_pack";
    }

}
