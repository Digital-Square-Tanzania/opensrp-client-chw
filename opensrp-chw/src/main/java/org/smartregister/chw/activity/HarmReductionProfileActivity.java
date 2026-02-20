package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CoreHarmReductionProfileActivity;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.fp.dao.FpDao;
import org.smartregister.chw.fp.domain.Visit;
import org.smartregister.chw.fp.util.FamilyPlanningConstants;
import org.smartregister.chw.harmreduction.R;
import org.smartregister.chw.harmreduction.dao.HarmReductionDao;
import org.smartregister.chw.harmreduction.util.Constants;
import org.smartregister.chw.harmreduction.util.HarmReductionVisitsUtil;
import org.smartregister.dao.AbstractDao;
import org.smartregister.chw.domain.SortableVisit;
import org.smartregister.chw.interactor.HarmReductionVisitHistoryInteractor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class HarmReductionProfileActivity extends CoreHarmReductionProfileActivity {
    private static final int MIN_PRE_MAT_SESSIONS_FOR_MAT_START = 3;
    private static final String YES = "yes";

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, HarmReductionProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.HARM_REDUCTION_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    protected void setupButtons() {
        if (textViewRecordHarmReductionVisit == null) {
            return;
        }

        if (hasStartedMat()) {
            textViewRecordHarmReductionVisit.setVisibility(View.GONE);
            return;
        }

        textViewRecordHarmReductionVisit.setVisibility(View.VISIBLE);
        if (StringUtils.equalsIgnoreCase(HarmReductionDao.getRocConsentForJoiningMatServices(memberObject.getBaseEntityId()), YES)) {
            textViewRecordHarmReductionVisit.setText(R.string.record_pre_mat_session);
        } else {
            textViewRecordHarmReductionVisit.setText(R.string.record_harm_reduction_community_visit);
        }
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        setupPreMatSessionsHistoryLayout();
        setupMarkClientStartedMatVisibility();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String baseEntityId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID);
        if (StringUtils.isBlank(baseEntityId)) {
            finish();
            return;
        }
        if (memberObject == null) {
            memberObject = HarmReductionDao.getMember(baseEntityId);
            if (memberObject == null) {
                memberObject = HarmReductionDao.getContact(baseEntityId);
            }
        }
        super.onCreate(savedInstanceState);
        try {
            HarmReductionVisitsUtil.processVisits();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void openClientObservationResults() {
        // no-op
    }

    @Override
    public void observationResults() {
        // no-op
    }

    @Override
    public void openRecordClientVisit() {
        HarmReductionVisitActivity.startHarmReductionVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openFollowupVisit() {
        HarmReductionVisitActivity.startHarmReductionVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openRecordTbContactVisit() {
        HarmReductionVisitActivity.startHarmReductionVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openMedicalHistory() {
        HarmReductionVisitHistoryActivity.startMe(this, memberObject);
    }

    public void openPreMatSessionsHistory() {
        HarmReductionPreMatSessionsHistoryActivity.startMe(this, memberObject);
    }

    @Override
    public void openObservationResults() {
        // no-op
    }

    @Override
    public void openHarmReductionContactRegister() {
        // no-op
    }

    @Override
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass() {
        return null;
    }

    @Override
    protected void removeMember() {
        // no-op
    }

    @NonNull
    @Override
    public CoreFamilyOtherMemberActivityPresenter presenter() {
        return null;
    }

    @Override
    public void setProfileImage(String s, String s1) {
        // no-op
    }

    @Override
    public void setProfileDetailThree(String s) {
        // no-op
    }

    @Override
    public void toggleFamilyHead(boolean b) {
        // no-op
    }

    @Override
    public void togglePrimaryCaregiver(boolean b) {
        // no-op
    }

    @Override
    public void startServiceForm() {
        HarmReductionVisitActivity.startHarmReductionVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void continueService() {
        HarmReductionVisitActivity.startHarmReductionVisitActivity(this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void continueContactVisit() {
        HarmReductionVisitActivity.startHarmReductionVisitActivity(this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void refreshList() {
        // no-op
    }

    @Override
    public void updateHasPhone(boolean b) {
        // no-op
    }

    @Override
    public void setFamilyServiceStatus(String s) {
        // no-op
    }

    @Override
    public void verifyHasPhone() {
        // no-op
    }

    @Override
    public void notifyHasPhone(boolean b) {
        // no-op
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMedicalHistory(true);
        delayRefreshSetupViews();
    }

    private void delayRefreshSetupViews() {
        try {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                setupButtons();
                setupViews();
                refreshMedicalHistory(true);
            }, 500);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void setupPreMatSessionsHistoryLayout() {
        boolean showPreMatHistory = hasPreMatSessions();
        if (rlPreMatSessionHistory != null) {
            rlPreMatSessionHistory.setVisibility(showPreMatHistory ? View.VISIBLE : View.GONE);
            rlPreMatSessionHistory.setOnClickListener(view -> openPreMatSessionsHistory());
        }

        if (preMatSessionRowDivider != null) {
            preMatSessionRowDivider.setVisibility(showPreMatHistory ? View.VISIBLE : View.GONE);
        }
    }

    private void setupMarkClientStartedMatVisibility() {
        if (hasStartedMat()) {
            if (textViewMarkClientStartedMat != null) {
                textViewMarkClientStartedMat.setVisibility(View.GONE);
            }
            return;
        }

        boolean showMarkClientStartedMat = hasMinimumPreMatSessions(MIN_PRE_MAT_SESSIONS_FOR_MAT_START);
        if (textViewMarkClientStartedMat != null) {
            textViewMarkClientStartedMat.setVisibility(showMarkClientStartedMat ? View.VISIBLE : View.GONE);
        }
    }

    private boolean hasPreMatSessions() {
        return hasMinimumPreMatSessions(1);
    }

    private boolean hasMinimumPreMatSessions(int minimumVisits) {
        if (minimumVisits <= 0) {
            return true;
        }
        if (memberObject == null) {
            return false;
        }

        if (!hasRocConsentForMat()) {
            return false;
        }

        Date consentDate = getMatConsentDate();

        try {
            List<SortableVisit> visits = HarmReductionVisitHistoryInteractor.getVisits(
                    memberObject.getBaseEntityId(),
                    Constants.EVENT_TYPE.HARM_REDUCTION_FOLLOW_UP_VISIT
            );

            int visitCount = 0;
            for (SortableVisit visit : visits) {
                Date visitDate = visit.getDate();
                if (visitDate != null && (consentDate == null || visitDate.after(consentDate))) {
                    visitCount++;
                    if (visitCount >= minimumVisits) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return false;
    }

    private boolean hasRocConsentForMat() {
        try {
            return StringUtils.equalsIgnoreCase(
                    YES,
                    HarmReductionDao.getRocConsentForJoiningMatServices(memberObject.getBaseEntityId())
            );
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    private boolean hasStartedMat() {
        if (memberObject == null || StringUtils.isBlank(memberObject.getBaseEntityId())) {
            return false;
        }

        try {
            String baseEntityId = memberObject.getBaseEntityId().replace("'", "''");
            String sql = "SELECT client_started_mat FROM " + Constants.TABLES.HARM_REDUCTION_RISK_ASSESSMENT +
                    " WHERE base_entity_id = '" + baseEntityId + "' AND is_closed = 0 " +
                    "ORDER BY last_interacted_with DESC LIMIT 1";
            List<Map<String, Object>> records = AbstractDao.readData(sql, new String[]{"client_started_mat"});

            if (records == null || records.isEmpty() || records.get(0) == null) {
                return false;
            }

            Object clientStartedMat = records.get(0).get("client_started_mat");
            return StringUtils.equalsIgnoreCase(YES, clientStartedMat == null ? null : String.valueOf(clientStartedMat));
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    private Date getMatConsentDate() {
        try {
            String consentDateString = HarmReductionDao.getVisitDateForRocConsentForJoiningMatServices(memberObject.getBaseEntityId());
            return parseConsentDate(consentDateString);
        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }

    private Date parseConsentDate(String consentDateString) {
        if (StringUtils.isBlank(consentDateString)) {
            return null;
        }

        String trimmedDate = consentDateString.trim();
        if (StringUtils.isBlank(trimmedDate)) {
            return null;
        }
        if (StringUtils.isNumeric(trimmedDate)) {
            try {
                long timestamp = Long.parseLong(trimmedDate);
                if (trimmedDate.length() == 10) {
                    timestamp *= 1000;
                }
                return new Date(timestamp);
            } catch (NumberFormatException e) {
                Timber.d(e);
            }
        }

        try {
            return ISODateTimeFormat.dateOptionalTimeParser().parseDateTime(trimmedDate).toDate();
        } catch (IllegalArgumentException e) {
            Timber.d(e);
        }

        try {
            return Date.from(OffsetDateTime.parse(trimmedDate).toInstant());
        } catch (DateTimeParseException e) {
            Timber.d(e);
        }

        String[] patterns = new String[]{
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd",
                "dd-MM-yyyy"
        };

        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern, Locale.getDefault()).parse(trimmedDate);
            } catch (ParseException e) {
                Timber.d(e);
            }
        }

        try {
            return new DateTime(trimmedDate).toDate();
        } catch (IllegalArgumentException e) {
            Timber.e(e);
        }
        return null;
    }
}
