package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.NcdCaseManagementVisitActivity;
import org.smartregister.chw.rule.NcdCaseManagementFollowupRule;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.ncd.NcdLibrary;
import org.smartregister.chw.ncd.domain.Visit;
import org.smartregister.chw.dao.NcdCaseManagementDao;
import org.smartregister.chw.dao.NcdDao;
import org.smartregister.chw.ncd.activity.BaseNcdProfileActivity;
import org.smartregister.chw.ncd.util.Constants;
import org.smartregister.chw.rule.NcdCaseManagementFollowupRule;
import org.smartregister.chw.util.Utils;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.util.AppExecutors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sqlcipher.database.SQLiteDatabase;

import timber.log.Timber;

/**
 * Concrete implementation of BaseNcdProfileActivity.
 * Register this activity in AndroidManifest.xml and use it to launch NCD profile screens.
 */
public class NcdProfileActivity extends BaseNcdProfileActivity {
    private static final String EXTRA_IS_CONFIRMED_NCD = "extra_is_confirmed_ncd";
    private static final String CONFIRMATION_TABLE = "ec_diabetes_hypertension_confirmation";
    private static final String COLUMN_DIABETES_RESULT = "diabetes_result";
    private static final String COLUMN_HYPERTENSION_RESULT = "hypertension_result";
    private static final String COLUMN_LAST_INTERACTED_WITH = "last_interacted_with";
    private static final String COLUMN_VISIT_DATE = "visit_date";
    private static final int FOLLOW_UP_WAIT_PERIOD_DAYS = 3;
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    private boolean isConfirmedNcd = false;
    private int overdueByDays = 0;
    private Date lastDiabetesScreeningDate;
    private boolean followUpButtonHiddenByWaitPeriod;
    private Integer originalRecordVisitRowVisibility;
    private Integer originalRecordVisitButtonVisibility;
    private final AppExecutors appExecutors = new AppExecutors();

    /**
     * Use this method to start the NcdProfileActivity.
     * @param activity The calling activity
     * @param baseEntityId The base entity id to pass
     */
    public static void startProfileActivity(Activity activity, String baseEntityId, boolean isConfirmedNcd) {
        Intent intent = new Intent(activity, NcdProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(EXTRA_IS_CONFIRMED_NCD, isConfirmedNcd);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isConfirmedNcd = getIntent().getBooleanExtra(EXTRA_IS_CONFIRMED_NCD, false);
    }

    @Override
    public void initializeFloatingMenu() {
        super.initializeFloatingMenu();
        if (baseNcdFloatingMenu != null) {
            baseNcdFloatingMenu.setReferralListener(this::openNcdReferralForm);
        }
    }

    private void openNcdReferralForm() {
        if (memberObject == null) return;
        List<ReferralTypeModel> referralTypes = new ArrayList<>();
        referralTypes.add(new ReferralTypeModel(
                getString(R.string.refer_to_facility),
                org.smartregister.chw.util.Constants.JsonForm.getNcdReferralForm(),
                org.smartregister.chw.util.Constants.NcdReferral.FOCUS_NCD_DANGER_SIGNS));
        Utils.launchClientReferralActivity(this, referralTypes, memberObject.getBaseEntityId());
    }

    @Override
    public void startServiceForm() {

    }

    @Override
    public void continueService() {

    }

    @Override
    public void continueDischarge() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        enforceFollowUpWaitPeriod();
        if (isConfirmedNcd && memberObject != null) {
            loadCaseSummary();
        }
    }

    @Override
    protected String getVisitButtonStatus(String baseEntityId) {
        if (!isConfirmedNcd) return CoreConstants.VISIT_STATE.NOT_DUE_YET;
        Date confirmationDate = NcdCaseManagementDao.getConfirmationDate(baseEntityId);
        if (confirmationDate == null) return CoreConstants.VISIT_STATE.NOT_DUE_YET;
        Date lastVisitDate = NcdCaseManagementDao.getLastFollowUpDate(baseEntityId);
        NcdCaseManagementFollowupRule rule = new NcdCaseManagementFollowupRule(confirmationDate, lastVisitDate);
        String status = rule.getButtonStatus();
        if (CoreConstants.VISIT_STATE.OVERDUE.equals(status) && rule.getOverDueDate() != null) {
            overdueByDays = Days.daysBetween(
                    new DateTime(rule.getOverDueDate()).toLocalDate(),
                    DateTime.now().toLocalDate()).getDays();
        }
        return status;
    }

    @Override
    public void setOverDueColor() {
        super.setOverDueColor();
        if (textViewOverdueAlert != null) {
            textViewOverdueAlert.setText(getString(
                    org.smartregister.chw.ncd.R.string.ncd_visit_overdue_alert, overdueByDays));
            textViewOverdueAlert.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected Visit getCaseManagementVisit() {
        if (memberObject == null) return null;
        return NcdLibrary.getInstance().visitRepository().getLatestVisit(
                memberObject.getBaseEntityId(),
                org.smartregister.chw.util.Constants.EncounterType.NCD_MONTHLY_FOLLOWUP);
    }

    @Override
    public void openFollowupVisit() {
        if (memberObject == null) {
            Timber.w("Member object is null, cannot continue with NCD visit");
            return;
        }

        if (shouldOpenNcdVisit(memberObject.getBaseEntityId())) {
            Visit unprocessed = getCaseManagementVisit();
            boolean editMode = unprocessed != null && !unprocessed.getProcessed();
            NcdCaseManagementVisitActivity.startMe(this, memberObject.getBaseEntityId(), editMode);
            return;
        }

        if (!isFollowUpWaitPeriodSatisfied()) {
            Toast.makeText(this, R.string.ncd_follow_up_wait_message, Toast.LENGTH_SHORT).show();
            enforceFollowUpWaitPeriod();
            return;
        }

        try {
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(
                    NcdProfileActivity.this,
                    org.smartregister.chw.util.Constants.JsonForm.getDiabetesFollowupForm()
            );
            if (formJsonObject != null) {
                formJsonObject.put("entity_id", memberObject.getBaseEntityId());
                startFormActivity(formJsonObject);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public void startFormActivity(JSONObject jsonForm) {

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());


        Form form = new Form();
        form.setActionBarBackground(R.color.family_actionbar);
        form.setWizard(false);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    private void enforceFollowUpWaitPeriod() {
        if (memberObject == null) {
            return;
        }

        if (isConfirmedNcd) {
            lastDiabetesScreeningDate = null;
            restoreFollowUpVisibilityIfNeeded();
            return;
        }

        Date screeningDate = NcdDao.getLastDiabetesScreeningDate(memberObject.getBaseEntityId());
        lastDiabetesScreeningDate = screeningDate;

        if (isEligibleForFollowUp(screeningDate)) {
            restoreFollowUpVisibilityIfNeeded();
        } else {
            hideFollowUpButton();
        }
    }

    private boolean isFollowUpWaitPeriodSatisfied() {
        if (isConfirmedNcd || memberObject == null) {
            return true;
        }

        Date screeningDate = lastDiabetesScreeningDate != null ? lastDiabetesScreeningDate
                : NcdDao.getLastDiabetesScreeningDate(memberObject.getBaseEntityId());
        lastDiabetesScreeningDate = screeningDate;
        return isEligibleForFollowUp(screeningDate);
    }

    private boolean isEligibleForFollowUp(Date screeningDate) {
        if (screeningDate == null) {
            return false;
        }

        DateTime screeningDay = new DateTime(screeningDate).withTimeAtStartOfDay();
        DateTime today = DateTime.now().withTimeAtStartOfDay();

        if (screeningDay.isAfter(today)) {
            return false;
        }

        return Days.daysBetween(screeningDay, today).getDays() >= FOLLOW_UP_WAIT_PERIOD_DAYS;
    }

    private void hideFollowUpButton() {
        View recordVisitRow = findViewById(R.id.record_visit_ncd);
        TextView recordFollowUpButton = findViewById(R.id.textview_record_ncd);

        boolean changed = false;

        if (!followUpButtonHiddenByWaitPeriod) {
            originalRecordVisitRowVisibility = recordVisitRow != null ? recordVisitRow.getVisibility() : null;
            originalRecordVisitButtonVisibility = recordFollowUpButton != null ? recordFollowUpButton.getVisibility() : null;
        }

        if (recordVisitRow != null && recordVisitRow.getVisibility() != View.GONE) {
            recordVisitRow.setVisibility(View.GONE);
            changed = true;
        }

        if (recordFollowUpButton != null && recordFollowUpButton.getVisibility() != View.GONE) {
            recordFollowUpButton.setVisibility(View.GONE);
            changed = true;
        }

        followUpButtonHiddenByWaitPeriod = followUpButtonHiddenByWaitPeriod || changed;
    }

    private void restoreFollowUpVisibilityIfNeeded() {
        if (!followUpButtonHiddenByWaitPeriod) {
            return;
        }

        View recordVisitRow = findViewById(R.id.record_visit_ncd);
        TextView recordFollowUpButton = findViewById(R.id.textview_record_ncd);

        if (recordVisitRow != null && originalRecordVisitRowVisibility != null) {
            recordVisitRow.setVisibility(originalRecordVisitRowVisibility);
        }

        if (recordFollowUpButton != null && originalRecordVisitButtonVisibility != null) {
            recordFollowUpButton.setVisibility(originalRecordVisitButtonVisibility);
        }

        followUpButtonHiddenByWaitPeriod = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean shouldOpenNcdVisit(String baseEntityId) {
        if (TextUtils.isEmpty(baseEntityId)) {
            return false;
        }

        if (isConfirmedNcd) {
            return true;
        }

        return hasConfirmedDiagnosis(baseEntityId);
    }

    private boolean hasConfirmedDiagnosis(String baseEntityId) {
        Cursor cursor = null;
        try {
            SQLiteDatabase database = ChwApplication.getInstance().getRepository().getReadableDatabase();
            String query = "SELECT " + COLUMN_DIABETES_RESULT + ", " + COLUMN_HYPERTENSION_RESULT +
                    " FROM " + CONFIRMATION_TABLE +
                    " WHERE base_entity_id = ? ORDER BY CASE WHEN " + COLUMN_LAST_INTERACTED_WITH +
                    " IS NOT NULL THEN " + COLUMN_LAST_INTERACTED_WITH + " ELSE " + COLUMN_VISIT_DATE +
                    " END DESC LIMIT 1";

            cursor = database.rawQuery(query, new String[]{baseEntityId});
            if (cursor != null && cursor.moveToFirst()) {
                String diabetesResult = getValue(cursor, COLUMN_DIABETES_RESULT);
                String hypertensionResult = getValue(cursor, COLUMN_HYPERTENSION_RESULT);
                return isConfirmedResult(diabetesResult) || isConfirmedResult(hypertensionResult);
            }
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return false;
    }

    private String getValue(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex >= 0 ? cursor.getString(columnIndex) : null;
    }

    private boolean isConfirmedResult(String result) {
        if (TextUtils.isEmpty(result)) {
            return false;
        }

        String normalized = result.trim().toLowerCase(Locale.US);
        return "positive".equals(normalized)
                || "confirmed".equals(normalized)
                || "yes".equals(normalized)
                || "true".equals(normalized);
    }

    private void loadCaseSummary() {
        final String baseEntityId = memberObject.getBaseEntityId();
        appExecutors.diskIO().execute(() -> {
            // All DAO calls on background thread
            String diagnosisType = NcdCaseManagementDao.getDiagnosisType(baseEntityId);
            Date confirmationDate = NcdCaseManagementDao.getConfirmationDate(baseEntityId);
            Map<String, String> lastFollowUp = NcdCaseManagementDao.getLastFollowUpEvent(baseEntityId);
            Date lastVisitDate = NcdCaseManagementDao.getLastFollowUpDate(baseEntityId);
            String referralType = NcdCaseManagementDao.getOpenReferralType(baseEntityId);
            List<Map<String, String>> visitHistory = NcdCaseManagementDao.getVisitHistory(baseEntityId, 3);

            Date nextDueDate = null;
            if (confirmationDate != null) {
                NcdCaseManagementFollowupRule rule = new NcdCaseManagementFollowupRule(confirmationDate, lastVisitDate);
                nextDueDate = rule.getDueDate();
            }

            final String fDiagnosisType = diagnosisType;
            final Date fConfirmationDate = confirmationDate;
            final Map<String, String> fLastFollowUp = lastFollowUp;
            final Date fLastVisitDate = lastVisitDate;
            final Date fNextDueDate = nextDueDate;
            final String fReferralType = referralType;
            final List<Map<String, String>> fVisitHistory = visitHistory;

            appExecutors.mainThread().execute(() ->
                    populateCaseSummary(fDiagnosisType, fConfirmationDate, fLastFollowUp,
                            fLastVisitDate, fNextDueDate, fReferralType, fVisitHistory));
        });
    }

    private void populateCaseSummary(String diagnosisType, Date confirmationDate,
                                     Map<String, String> lastFollowUp, Date lastVisitDate,
                                     Date nextDueDate, String referralType,
                                     List<Map<String, String>> visitHistory) {
        View container = findViewById(R.id.ncd_cs_container);
        if (container == null || diagnosisType == null) {
            return;
        }

        // Diagnosis section
        TextView diagnosisView = findViewById(R.id.ncd_cs_diagnosis_type);
        TextView confirmationView = findViewById(R.id.ncd_cs_confirmation_date);
        if (diagnosisView != null) {
            int diagResId;
            switch (diagnosisType) {
                case "DM_HTN":
                    diagResId = R.string.ncd_cs_diagnosis_dm_htn;
                    break;
                case "HTN":
                    diagResId = R.string.ncd_cs_diagnosis_htn;
                    break;
                default:
                    diagResId = R.string.ncd_cs_diagnosis_dm;
                    break;
            }
            diagnosisView.setText(diagResId);
        }
        if (confirmationView != null && confirmationDate != null) {
            confirmationView.setText(getString(R.string.ncd_cs_confirmed_on,
                    DISPLAY_DATE_FORMAT.format(confirmationDate)));
        }

        // Alert status section
        View alertSection = findViewById(R.id.ncd_cs_alert_section);
        View alertDivider = findViewById(R.id.ncd_cs_alert_divider);
        if (lastFollowUp != null && alertSection != null) {
            String alertStatus = lastFollowUp.get("alert_status");
            TextView alertBadge = findViewById(R.id.ncd_cs_alert_badge);
            if (alertBadge != null && !TextUtils.isEmpty(alertStatus)) {
                String normalized = alertStatus.trim().toUpperCase(Locale.US);
                int badgeColorRes;
                int labelResId;
                if ("RED".equals(normalized)) {
                    badgeColorRes = org.smartregister.R.color.alert_urgent_red;
                    labelResId = R.string.ncd_cs_alert_red;
                } else if ("YELLOW".equals(normalized)) {
                    badgeColorRes = org.smartregister.R.color.alert_in_progress_blue;
                    labelResId = R.string.ncd_cs_alert_yellow;
                } else {
                    badgeColorRes = org.smartregister.R.color.alert_complete_green;
                    labelResId = R.string.ncd_cs_alert_none;
                }
                alertBadge.setText(labelResId);
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(12f);
                bg.setColor(ContextCompat.getColor(this, badgeColorRes));
                alertBadge.setBackground(bg);
                alertSection.setVisibility(View.VISIBLE);
                if (alertDivider != null) alertDivider.setVisibility(View.VISIBLE);
            }
        }

        // Visit dates section
        View datesSection = findViewById(R.id.ncd_cs_dates_section);
        View datesDivider = findViewById(R.id.ncd_cs_dates_divider);
        if (datesSection != null && confirmationDate != null) {
            TextView lastVisitView = findViewById(R.id.ncd_cs_last_visit_date);
            TextView nextDueView = findViewById(R.id.ncd_cs_next_due_date);
            if (lastVisitView != null) {
                lastVisitView.setText(lastVisitDate != null ? DISPLAY_DATE_FORMAT.format(lastVisitDate) : "—");
            }
            if (nextDueView != null) {
                nextDueView.setText(nextDueDate != null ? DISPLAY_DATE_FORMAT.format(nextDueDate) : "—");
            }
            datesSection.setVisibility(View.VISIBLE);
            if (datesDivider != null) datesDivider.setVisibility(View.VISIBLE);
        }

        // Referral section
        View referralSection = findViewById(R.id.ncd_cs_referral_section);
        View referralDivider = findViewById(R.id.ncd_cs_referral_divider);
        if (referralSection != null && !TextUtils.isEmpty(referralType)) {
            TextView referralTypeView = findViewById(R.id.ncd_cs_referral_type);
            if (referralTypeView != null) {
                int refResId = org.smartregister.chw.util.Constants.NcdReferral.FOCUS_NCD_DANGER_SIGNS.equals(referralType)
                        ? R.string.ncd_cs_referral_urgent
                        : R.string.ncd_cs_referral_non_emergency;
                referralTypeView.setText(refResId);
            }
            referralSection.setVisibility(View.VISIBLE);
            if (referralDivider != null) referralDivider.setVisibility(View.VISIBLE);
        }

        // Visit history section
        View historySection = findViewById(R.id.ncd_cs_history_section);
        if (historySection != null && !visitHistory.isEmpty()) {
            int[][] historyViewIds = {
                    {R.id.ncd_cs_history_row_1, R.id.ncd_cs_history_dot_1, R.id.ncd_cs_history_date_1, R.id.ncd_cs_history_summary_1},
                    {R.id.ncd_cs_history_row_2, R.id.ncd_cs_history_dot_2, R.id.ncd_cs_history_date_2, R.id.ncd_cs_history_summary_2},
                    {R.id.ncd_cs_history_row_3, R.id.ncd_cs_history_dot_3, R.id.ncd_cs_history_date_3, R.id.ncd_cs_history_summary_3},
            };

            for (int i = 0; i < Math.min(visitHistory.size(), 3); i++) {
                Map<String, String> visit = visitHistory.get(i);
                View row = findViewById(historyViewIds[i][0]);
                View dot = findViewById(historyViewIds[i][1]);
                TextView dateView = findViewById(historyViewIds[i][2]);
                TextView summaryView = findViewById(historyViewIds[i][3]);

                if (row == null) continue;

                // Date
                if (dateView != null) {
                    String visitDate = visit.get("visit_date");
                    dateView.setText(!TextUtils.isEmpty(visitDate) ? visitDate : "—");
                }

                // Dot color based on alert status
                if (dot != null) {
                    String status = visit.get("alert_status");
                    int dotColor;
                    if ("RED".equalsIgnoreCase(status)) {
                        dotColor = ContextCompat.getColor(this, org.smartregister.R.color.alert_urgent_red);
                    } else if ("YELLOW".equalsIgnoreCase(status)) {
                        dotColor = ContextCompat.getColor(this, org.smartregister.R.color.alert_in_progress_blue);
                    } else {
                        dotColor = ContextCompat.getColor(this, org.smartregister.R.color.alert_complete_green);
                    }
                    GradientDrawable dotBg = new GradientDrawable();
                    dotBg.setShape(GradientDrawable.OVAL);
                    dotBg.setColor(dotColor);
                    dot.setBackground(dotBg);
                }

                // Clinical summary
                if (summaryView != null) {
                    summaryView.setText(buildClinicalSummary(visit));
                }

                row.setVisibility(View.VISIBLE);
            }
            historySection.setVisibility(View.VISIBLE);
        }

        // Update rlLastVisit row label with days-since-last-visit
        updateLastVisitRowLabel(lastVisitDate);

        // Show the master container
        container.setVisibility(View.VISIBLE);
    }

    private void updateLastVisitRowLabel(Date lastVisitDate) {
        if (textViewLastVisitRow == null) return;
        if (lastVisitDate == null) {
            textViewLastVisitRow.setText(org.smartregister.chw.ncd.R.string.view_medical_history);
            return;
        }
        int numOfDays = Days.daysBetween(
                new DateTime(lastVisitDate).toLocalDate(),
                DateTime.now().toLocalDate()).getDays();
        String timeAgo = numOfDays <= 0
                ? getString(org.smartregister.chw.ncd.R.string.ncd_last_visit_less_than_24h)
                : numOfDays + " " + getString(org.smartregister.chw.ncd.R.string.ncd_last_visit_days);
        textViewLastVisitRow.setText(
                getString(org.smartregister.chw.ncd.R.string.last_visit_40_days_ago, timeAgo));
    }

    private String buildClinicalSummary(Map<String, String> visit) {
        StringBuilder sb = new StringBuilder();
        appendFlag(sb, visit.get("clinic_attendance"), "Clinic");
        appendFlag(sb, visit.get("medication_adherence"), "Meds");
        appendFlag(sb, visit.get("non_healing_wounds"), "Wounds");
        appendFlag(sb, visit.get("neuropathy"), "Neuropathy");
        appendFlag(sb, visit.get("vision_changes"), "Vision");
        appendFlag(sb, visit.get("chest_pain"), "Chest pain");
        return sb.length() > 0 ? sb.toString() : "—";
    }

    private void appendFlag(StringBuilder sb, String value, String label) {
        if (!TextUtils.isEmpty(value) && !"no".equalsIgnoreCase(value.trim())
                && !"none".equalsIgnoreCase(value.trim())
                && !"false".equalsIgnoreCase(value.trim())) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(label);
        }
    }

}
