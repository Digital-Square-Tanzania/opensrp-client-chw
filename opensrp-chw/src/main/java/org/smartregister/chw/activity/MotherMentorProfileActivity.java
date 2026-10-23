package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreMotherMentorProfileActivity;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitsUtil;
import org.smartregister.chw.mothermentor.util.VisitUtils;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.util.MotherMentorDailyVisitUtils;
import org.smartregister.chw.util.MotherMentorFollowUpVisitUtils;
import org.smartregister.chw.util.MotherMentorFormWizardUtils;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import timber.log.Timber;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MotherMentorProfileActivity extends CoreMotherMentorProfileActivity {
    static final String EXTRA_FOLLOW_UP_PROFILE = "extra_follow_up_profile";
    private static final String FORM_MOTHERMENTOR_ENROLL_IIT = "mothermentor_enroll_iit";
    private static final String ATTENDANCE_TYPE = DBConstants.KEY.ATTENDANCE_TYPE;
    private static final String PSYCHOSOCIAL_GROUP_LINKAGE =
            DBConstants.KEY.HAS_BEEN_LINKED_TO_PSYCHOSOCIAL_SUPPORT_GROUP;
    private static final String IGA_GROUP_LINKAGE = DBConstants.KEY.HAS_BEEN_LINKED_TO_IGA_GROUP;
    private static final String EDUCATION_PROVIDED = DBConstants.KEY.EDUCATION_PROVIDED;
    private static final String REFERRAL_GIVEN = DBConstants.KEY.REFERRAL_GIVEN;
    private static final String NEXT_APPOINTMENT_DATE = DBConstants.KEY.NEXT_APPOINTMENT_DATE;
    private static final String COMMENTS = DBConstants.KEY.COMMENTS;
    private static final String FOLLOW_UP_STATUS = DBConstants.KEY.FOLLOW_UP_STATUS;
    private static final String SERVICE_INTERRUPTION_REASON =
            DBConstants.KEY.SERVICE_INTERRUPTION_REASON;
    private static final String SERVICE_INTERRUPTION_REASON_OTHER =
            DBConstants.KEY.SERVICE_INTERRUPTION_REASON_OTHER;
    private static final String EXPECTED_FACILITY_VISIT_DATE =
            DBConstants.KEY.EXPECTED_FACILITY_VISIT_DATE;
    private static final String RETURNED_TO_FACILITY = DBConstants.KEY.RETURNED_TO_FACILITY;
    private static final String FACILITY_RETURN_DATE = DBConstants.KEY.FACILITY_RETURN_DATE;
    private static final String PARTNER_ARRIVED_AT_FACILITY =
            DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY;
    private static final String PARTNER_FACILITY_ARRIVAL_DATE =
            DBConstants.KEY.PARTNER_FACILITY_ARRIVAL_DATE;
    private static final String CHILD_EID_TEST_TYPE = DBConstants.KEY.CHILD_EID_TEST_TYPE;
    private static final String CHILD_EID_ARRIVED_AT_FACILITY =
            DBConstants.KEY.CHILD_EID_ARRIVED_AT_FACILITY;
    private static final String CHILD_EID_FACILITY_ARRIVAL_DATE =
            DBConstants.KEY.CHILD_EID_FACILITY_ARRIVAL_DATE;
    private static final int HIGH_RISK_WEEK_THRESHOLD = 8;
    private static final String[] IIT_DATE_FORMATS = new String[] {"dd-MM-yyyy", "yyyy-MM-dd"};

    private static final Set<String> REQUIRED_MOTHER_MENTOR_SERVICE_FIELDS =
            new HashSet<>(
                    Arrays.asList(
                            ATTENDANCE_TYPE,
                            PSYCHOSOCIAL_GROUP_LINKAGE,
                            IGA_GROUP_LINKAGE,
                            EDUCATION_PROVIDED,
                            REFERRAL_GIVEN,
                            NEXT_APPOINTMENT_DATE,
                            COMMENTS));
    private TextView iitRiskTag;
    private TextView pregnancyWeekView;
    private TextView followUpRoleView;
    private LinearLayout iitProfileImageRow;
    private final Handler visitAvailabilityHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshVisitAvailability =
            () -> {
                if (memberObject != null && !isFinishing()) {
                    setupButtons();
                    scheduleVisitAvailabilityRefresh();
                }
            };
    private final BroadcastReceiver visitDateChangedReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    refreshVisitAvailability.run();
                }
            };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter dateChanges = new IntentFilter(Intent.ACTION_DATE_CHANGED);
        dateChanges.addAction(Intent.ACTION_TIME_CHANGED);
        dateChanges.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(visitDateChangedReceiver, dateChanges);
        refreshVisitAvailability.run();
    }

    @Override
    protected void onPause() {
        visitAvailabilityHandler.removeCallbacks(refreshVisitAvailability);
        unregisterReceiver(visitDateChangedReceiver);
        super.onPause();
    }

    private void scheduleVisitAvailabilityRefresh() {
        visitAvailabilityHandler.removeCallbacks(refreshVisitAvailability);
        Date now = getVisitAvailabilityDate();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.setTime(now);
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);
        visitAvailabilityHandler.postDelayed(
                refreshVisitAvailability, tomorrow.getTimeInMillis() - now.getTime());
    }

    Date getVisitAvailabilityDate() {
        return new Date();
    }

    public static void startMe(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, MotherMentorProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(
                Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE,
                Constants.PROFILE_TYPES.MOTHERMENTOR_PROFILE);
        activity.startActivity(intent);
    }

    public static void startFollowUpProfile(Activity activity, String baseEntityId) {
        startFollowUpProfile(activity, baseEntityId, MotherMentorFollowUpType.MOTHER_IIT);
    }

    public static void startFollowUpProfile(
            Activity activity, String baseEntityId, MotherMentorFollowUpType followUpType) {
        Intent intent = new Intent(activity, MotherMentorProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(
                Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE,
                Constants.PROFILE_TYPES.MOTHERMENTOR_PROFILE);
        intent.putExtra(EXTRA_FOLLOW_UP_PROFILE, true);
        intent.putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE, followUpType.getValue());
        activity.startActivity(intent);
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        MemberObject memberObject = super.getMemberObject(baseEntityId);
        if (memberObject != null) {
            return memberObject;
        }

        memberObject = getHouseholdMember(baseEntityId);
        if (memberObject != null) {
            return memberObject;
        }

        return getSecondaryEnrollmentMember(baseEntityId);
    }

    private MemberObject getHouseholdMember(String baseEntityId) {
        if (TextUtils.isEmpty(baseEntityId)) {
            return null;
        }

        try {
            CommonRepository commonRepository =
                    Utils.context().commonrepository(Utils.metadata().familyRegister.tableName);
            if (commonRepository == null) {
                return null;
            }

            CommonPersonObject personObject = commonRepository.findByBaseEntityId(baseEntityId);
            if (personObject != null) {
                return commonPersonObjectToHouseholdMember(personObject);
            }
        } catch (Exception e) {
            Timber.e(e, "Unable to load Mother Mentor household profile");
        }
        return null;
    }

    private MemberObject commonPersonObjectToHouseholdMember(CommonPersonObject personObject) {
        MemberObject memberObject = new MemberObject();
        memberObject.setBaseEntityId(personObject.getCaseId());
        memberObject.setFamilyBaseEntityId(personObject.getCaseId());
        memberObject.setFirstName(
                Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.FIRST_NAME, false));
        memberObject.setLastName(
                Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.LAST_NAME, false));
        memberObject.setUniqueId(
                Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.UNIQUE_ID, false));
        memberObject.setAddress(
                Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.VILLAGE_TOWN, false));
        memberObject.setPrimaryCareGiver(
                Utils.getValue(
                        personObject.getColumnmaps(), DBConstants.KEY.PRIMARY_CARE_GIVER, false));
        memberObject.setFamilyHead(
                Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.FAMILY_HEAD, false));
        memberObject.setPhoneNumber(
                Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.PHONE_NUMBER, false));
        return memberObject;
    }

    private MemberObject getSecondaryEnrollmentMember(String baseEntityId) {
        MemberObject memberObject =
                getSecondaryEnrollmentMember(
                        baseEntityId, Constants.TABLES.MOTHERMENTOR_ENROLL_IIT);
        if (memberObject != null) {
            return memberObject;
        }

        memberObject =
                getSecondaryEnrollmentMember(
                        baseEntityId, Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER);
        if (memberObject != null) {
            return memberObject;
        }

        return getSecondaryEnrollmentMember(
                baseEntityId, Constants.TABLES.MOTHERMENTOR_ENROLL_CHILD_EID);
    }

    private MemberObject getSecondaryEnrollmentMember(String baseEntityId, String tableName) {
        if (TextUtils.isEmpty(baseEntityId)) {
            return null;
        }

        String sql =
                "select m."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + ", m."
                        + DBConstants.KEY.UNIQUE_ID
                        + ", m."
                        + DBConstants.KEY.RELATIONAL_ID
                        + ", m."
                        + DBConstants.KEY.DOB
                        + ", m."
                        + DBConstants.KEY.FIRST_NAME
                        + ", m."
                        + DBConstants.KEY.MIDDLE_NAME
                        + ", m."
                        + DBConstants.KEY.LAST_NAME
                        + ", m."
                        + DBConstants.KEY.GENDER
                        + ", m."
                        + DBConstants.KEY.MARITAL_STATUS
                        + ", m."
                        + DBConstants.KEY.PHONE_NUMBER
                        + ", f."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " as "
                        + DBConstants.KEY.FAMILY_BASE_ENTITY_ID
                        + ", f."
                        + DBConstants.KEY.FIRST_NAME
                        + " as "
                        + DBConstants.KEY.FAMILY_NAME
                        + ", f."
                        + DBConstants.KEY.PRIMARY_CARE_GIVER
                        + ", f."
                        + DBConstants.KEY.FAMILY_HEAD
                        + ", f."
                        + DBConstants.KEY.VILLAGE_TOWN
                        + ", fh."
                        + DBConstants.KEY.FIRST_NAME
                        + " as "
                        + DBConstants.KEY.FAMILY_HEAD_FIRST_NAME
                        + ", fh."
                        + DBConstants.KEY.MIDDLE_NAME
                        + " as "
                        + DBConstants.KEY.FAMILY_HEAD_MIDDLE_NAME
                        + ", fh."
                        + DBConstants.KEY.LAST_NAME
                        + " as "
                        + DBConstants.KEY.FAMILY_HEAD_LAST_NAME
                        + ", fh."
                        + DBConstants.KEY.PHONE_NUMBER
                        + " as "
                        + DBConstants.KEY.FAMILY_HEAD_PHONE_NUMBER
                        + ", pcg."
                        + DBConstants.KEY.FIRST_NAME
                        + " as "
                        + DBConstants.KEY.PCG_FIRST_NAME
                        + ", pcg."
                        + DBConstants.KEY.MIDDLE_NAME
                        + " as "
                        + DBConstants.KEY.PCG_MIDDLE_NAME
                        + ", pcg."
                        + DBConstants.KEY.LAST_NAME
                        + " as "
                        + DBConstants.KEY.PCG_LAST_NAME
                        + ", pcg."
                        + DBConstants.KEY.PHONE_NUMBER
                        + " as "
                        + DBConstants.KEY.PCG_PHONE_NUMBER
                        + " from ec_family_member m inner join ec_family f on m."
                        + DBConstants.KEY.RELATIONAL_ID
                        + " = f."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " inner join "
                        + tableName
                        + " mr on mr."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " = m."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " left join ec_family_member fh on fh."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " = f."
                        + DBConstants.KEY.FAMILY_HEAD
                        + " left join ec_family_member pcg on pcg."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " = f."
                        + DBConstants.KEY.PRIMARY_CARE_GIVER
                        + " where mr."
                        + DBConstants.KEY.IS_CLOSED
                        + " = 0 AND m."
                        + DBConstants.KEY.IS_CLOSED
                        + " = 0 AND f."
                        + DBConstants.KEY.IS_CLOSED
                        + " = 0 AND m."
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " = ? ORDER BY mr."
                        + DBConstants.KEY.LAST_INTERACTED_WITH
                        + " DESC LIMIT 1";

        try {
            SQLiteDatabase db = ChwApplication.getInstance().getRepository().getReadableDatabase();
            try (Cursor cursor = db.rawQuery(sql, new String[] {baseEntityId})) {
                if (cursor.moveToFirst()) {
                    return cursorToSecondaryEnrollmentMember(cursor);
                }
            }
        } catch (Exception e) {
            Timber.e(
                    e,
                    "Unable to load Mother Mentor secondary enrollment profile from %s",
                    tableName);
        }
        return null;
    }

    private MemberObject cursorToSecondaryEnrollmentMember(Cursor cursor) {
        MemberObject memberObject = new MemberObject();
        memberObject.setFirstName(getCursorValue(cursor, DBConstants.KEY.FIRST_NAME));
        memberObject.setMiddleName(getCursorValue(cursor, DBConstants.KEY.MIDDLE_NAME));
        memberObject.setLastName(getCursorValue(cursor, DBConstants.KEY.LAST_NAME));
        memberObject.setAddress(getCursorValue(cursor, DBConstants.KEY.VILLAGE_TOWN));
        memberObject.setGender(getCursorValue(cursor, DBConstants.KEY.GENDER));
        memberObject.setMartialStatus(getCursorValue(cursor, DBConstants.KEY.MARITAL_STATUS));
        memberObject.setUniqueId(getCursorValue(cursor, DBConstants.KEY.UNIQUE_ID));
        memberObject.setDob(getCursorValue(cursor, DBConstants.KEY.DOB));
        memberObject.setFamilyBaseEntityId(
                getCursorValue(cursor, DBConstants.KEY.FAMILY_BASE_ENTITY_ID));
        memberObject.setRelationalId(getCursorValue(cursor, DBConstants.KEY.RELATIONAL_ID));
        memberObject.setPrimaryCareGiver(
                getCursorValue(cursor, DBConstants.KEY.PRIMARY_CARE_GIVER));
        memberObject.setFamilyName(getCursorValue(cursor, DBConstants.KEY.FAMILY_NAME));
        memberObject.setPhoneNumber(getCursorValue(cursor, DBConstants.KEY.PHONE_NUMBER));
        memberObject.setBaseEntityId(getCursorValue(cursor, DBConstants.KEY.BASE_ENTITY_ID));
        memberObject.setFamilyHead(getCursorValue(cursor, DBConstants.KEY.FAMILY_HEAD));
        memberObject.setFamilyHeadPhoneNumber(
                getCursorValue(cursor, DBConstants.KEY.FAMILY_HEAD_PHONE_NUMBER));

        String familyHeadName =
                (getCursorValue(cursor, DBConstants.KEY.FAMILY_HEAD_FIRST_NAME)
                                + " "
                                + getCursorValue(cursor, DBConstants.KEY.FAMILY_HEAD_MIDDLE_NAME))
                        .trim();
        memberObject.setFamilyHeadName(
                (familyHeadName
                                + " "
                                + getCursorValue(cursor, DBConstants.KEY.FAMILY_HEAD_LAST_NAME))
                        .trim());

        String primaryCareGiverName =
                (getCursorValue(cursor, DBConstants.KEY.PCG_FIRST_NAME)
                                + " "
                                + getCursorValue(cursor, DBConstants.KEY.PCG_MIDDLE_NAME))
                        .trim();
        memberObject.setPrimaryCareGiverName(
                (primaryCareGiverName + " " + getCursorValue(cursor, DBConstants.KEY.PCG_LAST_NAME))
                        .trim());

        return memberObject;
    }

    private String getCursorValue(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex == -1 || cursor.isNull(columnIndex)) {
            return "";
        }
        return cursor.getString(columnIndex);
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        TextView toolbarTitle = findViewById(org.smartregister.chw.mothermentor.R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(
                    isFollowUpProfile()
                            ? R.string.mothermentor_back_to_follow_up_clients
                            : org.smartregister.chw.mothermentor.R.string
                                    .return_to_mothermentor_clients);
        }
        updateFollowUpRole();
        updateIitHighRiskTag();
        updatePregnancyWeek();
    }

    private void updateFollowUpRole() {
        LinearLayout profile =
                findViewById(org.smartregister.chw.mothermentor.R.id.profile_name_layout);
        if (profile == null) return;
        if (followUpRoleView == null) {
            followUpRoleView = new TextView(this);
            followUpRoleView.setTextSize(14);
            followUpRoleView.setTypeface(Typeface.DEFAULT_BOLD);
            followUpRoleView.setTextColor(getResources().getColor(R.color.customAppThemeBlue));
            followUpRoleView.setGravity(android.view.Gravity.CENTER);
            profile.addView(followUpRoleView);
        }
        boolean showSecondaryRole =
                isFollowUpProfile() && getFollowUpType() != MotherMentorFollowUpType.MOTHER_IIT;
        followUpRoleView.setVisibility(showSecondaryRole ? View.VISIBLE : View.GONE);
        if (showSecondaryRole) {
            followUpRoleView.setText(
                    getFollowUpType() == MotherMentorFollowUpType.CHILD_EID
                            ? R.string.mothermentor_child_eid
                            : R.string.mothermentor_partner);
        }
    }

    private void updatePregnancyWeek() {
        if (isFollowUpProfile() && getFollowUpType() != MotherMentorFollowUpType.MOTHER_IIT) {
            if (pregnancyWeekView != null) pregnancyWeekView.setVisibility(View.GONE);
            return;
        }
        Integer week =
                getPregnancyWeek(memberObject == null ? null : memberObject.getBaseEntityId());
        LinearLayout profile =
                findViewById(org.smartregister.chw.mothermentor.R.id.profile_name_layout);
        if (profile == null) return;
        if (pregnancyWeekView == null) {
            pregnancyWeekView = new TextView(this);
            pregnancyWeekView.setTextSize(14);
            pregnancyWeekView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            pregnancyWeekView.setGravity(android.view.Gravity.CENTER);
            profile.addView(pregnancyWeekView);
        }
        pregnancyWeekView.setVisibility(week == null ? View.GONE : View.VISIBLE);
        if (week != null)
            pregnancyWeekView.setText(getString(R.string.mothermentor_pregnancy_week, week));
    }

    private Integer getPregnancyWeek(String baseEntityId) {
        if (TextUtils.isEmpty(baseEntityId)) return null;
        try {
            SQLiteDatabase db = ChwApplication.getInstance().getRepository().getReadableDatabase();
            try (Cursor cursor =
                    db.rawQuery(
                            "SELECT edd FROM ec_anc_register WHERE "
                                    + DBConstants.KEY.BASE_ENTITY_ID
                                    + " = ? AND "
                                    + DBConstants.KEY.IS_CLOSED
                                    + " = 0 ORDER BY "
                                    + DBConstants.KEY.LAST_INTERACTED_WITH
                                    + " DESC LIMIT 1",
                            new String[] {baseEntityId})) {
                if (!cursor.moveToFirst()) return null;
                String edd = getCursorValue(cursor, "edd");
                if (TextUtils.isEmpty(edd)) return null;
                int remaining =
                        org.joda.time.Weeks.weeksBetween(
                                        new org.joda.time.DateTime().withTimeAtStartOfDay(),
                                        new org.joda.time.DateTime(edd).withTimeAtStartOfDay())
                                .getWeeks();
                return Math.max(0, Math.min(42, 40 - remaining));
            }
        } catch (Exception e) {
            Timber.e(e, "Unable to load pregnancy week for Mother Mentor profile");
            return null;
        }
    }

    private void updateIitHighRiskTag() {
        if (isFollowUpProfile() && getFollowUpType() != MotherMentorFollowUpType.MOTHER_IIT) {
            hideIitRiskTag();
            return;
        }
        if (memberObject == null || isFinishing()) {
            return;
        }

        Integer weeksLost = getLatestIitWeeksLost(memberObject.getBaseEntityId());
        if (weeksLost == null) {
            hideIitRiskTag();
        } else if (weeksLost >= HIGH_RISK_WEEK_THRESHOLD) {
            showIitRiskTag(
                    getString(R.string.mothermentor_risk_high),
                    getResources().getColor(R.color.mothermentor_risk_high));
        } else {
            showIitRiskTag(
                    getString(R.string.mothermentor_risk_low),
                    getResources().getColor(R.color.mothermentor_risk_low));
        }
    }

    private void hideIitRiskTag() {
        if (iitRiskTag != null) {
            iitRiskTag.setVisibility(View.GONE);
        }
    }

    private void showIitRiskTag(String label, int backgroundColor) {
        LinearLayout profileNameLayout =
                findViewById(org.smartregister.chw.mothermentor.R.id.profile_name_layout);
        if (profileNameLayout == null || imageView == null) {
            return;
        }

        ensureProfileImageTagRow();

        if (iitRiskTag == null) {
            iitRiskTag = new TextView(this);
            iitRiskTag.setTextColor(Color.WHITE);
            iitRiskTag.setTextSize(12);
            iitRiskTag.setTypeface(Typeface.DEFAULT_BOLD);
            iitRiskTag.setAllCaps(true);
            iitRiskTag.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4));

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            params.leftMargin = dpToPx(8);
            params.gravity = android.view.Gravity.CENTER_VERTICAL;
            iitProfileImageRow.addView(iitRiskTag, params);
        }
        iitRiskTag.setText(label);
        iitRiskTag.setBackground(createRiskTagBackground(backgroundColor));
        iitRiskTag.setVisibility(View.VISIBLE);
    }

    private GradientDrawable createRiskTagBackground(int backgroundColor) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(backgroundColor);
        background.setCornerRadius(dpToPx(12));
        return background;
    }

    private void ensureProfileImageTagRow() {
        if (iitProfileImageRow != null) {
            return;
        }

        ViewGroup currentParent = (ViewGroup) imageView.getParent();
        int imageIndex = currentParent.indexOfChild(imageView);
        ViewGroup.LayoutParams imageParams = imageView.getLayoutParams();
        currentParent.removeView(imageView);

        iitProfileImageRow = new LinearLayout(this);
        iitProfileImageRow.setGravity(android.view.Gravity.CENTER);
        iitProfileImageRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;

        currentParent.addView(iitProfileImageRow, imageIndex, rowParams);
        iitProfileImageRow.addView(imageView, imageParams);
    }

    private Integer getLatestIitWeeksLost(String baseEntityId) {
        if (TextUtils.isEmpty(baseEntityId)) {
            return null;
        }

        String sql =
                "SELECT "
                        + DBConstants.KEY.WEEKS_LOST
                        + ", "
                        + DBConstants.KEY.EXPECTED_RETURN_DATE
                        + " FROM "
                        + Constants.TABLES.MOTHERMENTOR_ENROLL_IIT
                        + " WHERE "
                        + DBConstants.KEY.IS_CLOSED
                        + " = 0 AND "
                        + DBConstants.KEY.BASE_ENTITY_ID
                        + " = ? ORDER BY "
                        + DBConstants.KEY.LAST_INTERACTED_WITH
                        + " DESC LIMIT 1";
        try {
            SQLiteDatabase db = ChwApplication.getInstance().getRepository().getReadableDatabase();
            try (Cursor cursor = db.rawQuery(sql, new String[] {baseEntityId})) {
                if (!cursor.moveToFirst()) {
                    return null;
                }

                int weeksLost = parseWeeksLost(getCursorValue(cursor, DBConstants.KEY.WEEKS_LOST));
                if (weeksLost > 0) {
                    return weeksLost;
                }

                return calculateWeeksLost(
                        getCursorValue(cursor, DBConstants.KEY.EXPECTED_RETURN_DATE));
            }
        } catch (Exception e) {
            Timber.e(e, "Unable to load Mother Mentor IIT weeks lost");
            return null;
        }
    }

    private int parseWeeksLost(String weeksLost) {
        if (TextUtils.isEmpty(weeksLost)) {
            return 0;
        }

        try {
            return (int) Double.parseDouble(weeksLost);
        } catch (NumberFormatException e) {
            Timber.e(e, "Unable to parse Mother Mentor IIT weeks lost: %s", weeksLost);
            return 0;
        }
    }

    private int calculateWeeksLost(String expectedReturnDate) {
        Date returnDate = parseIitDate(expectedReturnDate);
        if (returnDate == null) {
            return 0;
        }

        long daysLost = TimeUnit.MILLISECONDS.toDays(new Date().getTime() - returnDate.getTime());
        if (daysLost <= 0) {
            return 0;
        }
        return (int) (daysLost / 7);
    }

    private Date parseIitDate(String dateValue) {
        if (TextUtils.isEmpty(dateValue)) {
            return null;
        }

        for (String dateFormat : IIT_DATE_FORMATS) {
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
                simpleDateFormat.setLenient(false);
                return simpleDateFormat.parse(dateValue);
            } catch (ParseException e) {
                // Try the next supported date format.
            }
        }
        Timber.w("Unable to parse Mother Mentor IIT expected return date: %s", dateValue);
        return null;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        if (!isFollowUpProfile()) {
            menu.add(
                            Menu.NONE,
                            R.id.action_mothermentor_enroll_iit,
                            Menu.NONE,
                            R.string.mothermentor_enroll_iit)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        menu.add(
                        Menu.NONE,
                        R.id.action_mothermentor_enroll_partner,
                        Menu.NONE,
                        R.string.mothermentor_link_partner)
                .setVisible(canLinkMotherMentorClient())
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(
                        Menu.NONE,
                        R.id.action_mothermentor_enroll_child_eid,
                        Menu.NONE,
                        R.string.mothermentor_link_child_eid)
                .setVisible(canLinkMotherMentorClient())
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_mothermentor_enroll_iit) {
            if (!isFollowUpProfile()) {
                startSecondaryEnrollment(FORM_MOTHERMENTOR_ENROLL_IIT);
            }
            return true;
        } else if (itemId == R.id.action_mothermentor_enroll_partner) {
            if (canLinkMotherMentorClient()) {
                Intent intent = new Intent(this, MotherMentorPartnerRegisterActivity.class);
                intent.putExtra(
                        MotherMentorPartnerRegisterActivity.MOTHER_ID,
                        memberObject.getBaseEntityId());
                startActivity(intent);
            }
            return true;
        } else if (itemId == R.id.action_mothermentor_enroll_child_eid) {
            if (canLinkMotherMentorClient()) {
                Intent intent = new Intent(this, MotherMentorChildEidRegisterActivity.class);
                intent.putExtra(
                        MotherMentorChildEidRegisterActivity.MOTHER_ID,
                        memberObject.getBaseEntityId());
                startActivity(intent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean canLinkMotherMentorClient() {
        if (memberObject == null || TextUtils.isEmpty(memberObject.getBaseEntityId())) return false;
        try {
            return new org.smartregister.chw.repository.MotherMentorPartnerRepository(
                                    ChwApplication.getInstance()
                                            .getRepository()
                                            .getReadableDatabase())
                            .findMother(memberObject.getBaseEntityId())
                    != null;
        } catch (Exception e) {
            Timber.e(e, "Unable to check Mother Mentor client linking eligibility");
            return false;
        }
    }

    @Override
    protected void setupButtons() {
        super.setupButtons();
        if (textViewRecordMotherMentor != null) {
            textViewRecordMotherMentor.setVisibility(View.VISIBLE);
            boolean canStartVisit = isFollowUpProfile() || !hasCompletedServiceVisitToday();
            textViewRecordMotherMentor.setEnabled(canStartVisit);
            textViewRecordMotherMentor.setAlpha(canStartVisit ? 1f : 0.5f);
            if (isFollowUpProfile()) {
                textViewRecordMotherMentor.setText(R.string.mothermentor_record_follow_up);
            } else if (memberObject != null
                    && TextUtils.equals(
                            memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId())) {
                textViewRecordMotherMentor.setText(R.string.mothermentor_household_record);
            } else {
                textViewRecordMotherMentor.setText(
                        org.smartregister.chw.mothermentor.R.string.record_mothermentor);
            }
        }
        enforceProcessVisitVisibility();
        refreshMedicalHistory(true);
    }

    private boolean hasCompletedServiceVisitToday() {
        return memberObject != null
                && MotherMentorDailyVisitUtils.hasCompletedServiceVisitToday(
                        VisitUtils.getVisitsOnly(
                                memberObject.getBaseEntityId(),
                                Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES),
                        getVisitAvailabilityDate());
    }

    boolean isFollowUpProfile() {
        return getIntent() != null && getIntent().getBooleanExtra(EXTRA_FOLLOW_UP_PROFILE, false);
    }

    MotherMentorFollowUpType getFollowUpType() {
        return MotherMentorFollowUpType.fromValue(
                getIntent() == null
                        ? null
                        : getIntent()
                                .getStringExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE));
    }

    @Override
    protected Visit getServiceVisit() {
        if (isFollowUpProfile() && memberObject != null) {
            // Legacy sectioned follow-ups were stored as MotherMentor Services. Keep them in
            // history, but do not resume them with a different category schema.
            return MotherMentorLibrary.getInstance()
                    .visitRepository()
                    .getLatestVisit(
                            memberObject.getBaseEntityId(), getFollowUpType().getEventType());
        }
        return super.getServiceVisit();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == org.smartregister.chw.mothermentor.R.id.textview_record_mothermentor) {
            if (memberObject == null || TextUtils.isEmpty(memberObject.getBaseEntityId())) {
                Timber.w("Unable to start Mother Mentor follow-up: client identity is missing");
                Toast.makeText(
                                this,
                                org.smartregister.chw.mothermentor.R.string
                                        .error_unable_to_open_profile,
                                Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            openRecordClientVisit();
            return;
        }
        super.onClick(view);
    }

    private void enforceProcessVisitVisibility() {
        Visit latestVisit = getServiceVisit();
        if (latestVisit == null) {
            manualProcessVisit.setVisibility(View.GONE);
            mothermentorServiceInProgress.setVisibility(View.GONE);
            return;
        }

        if (isVisitOnProgress(latestVisit)) {
            textViewRecordMotherMentor.setVisibility(View.GONE);
            mothermentorServiceInProgress.setVisibility(View.VISIBLE);
        } else {
            mothermentorServiceInProgress.setVisibility(View.GONE);
        }

        boolean shouldShowProcessVisit =
                !Boolean.TRUE.equals(latestVisit.getProcessed())
                        && hasCompletedMotherMentorServiceSections(latestVisit);

        manualProcessVisit.setVisibility(shouldShowProcessVisit ? View.VISIBLE : View.GONE);
        if (shouldShowProcessVisit) {
            manualProcessVisit.setText(R.string.mothermentor_complete_visit);
            manualProcessVisit.setOnClickListener(
                    view -> {
                        try {
                            MotherMentorVisitsUtil.manualProcessVisit(latestVisit);
                            displayToast(
                                    org.smartregister.chw.mothermentor.R.string
                                            .mothermentor_visit_conducted);
                            setupViews();
                            refreshMedicalHistory(true);
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    });
        }
    }

    private boolean hasCompletedMotherMentorServiceSections(Visit visit) {
        if (visit == null || TextUtils.isEmpty(visit.getJson())) {
            return false;
        }

        try {
            JSONArray obsArray = new JSONObject(visit.getJson()).optJSONArray("obs");
            if (obsArray == null) return false;
            Map<String, String> values = getMotherMentorServiceValues(obsArray);
            String status = values.get(FOLLOW_UP_STATUS);
            if (!TextUtils.isEmpty(status)) {
                if (!"found".equals(status)) return true;
                boolean partnerSchema =
                        getFollowUpType() == MotherMentorFollowUpType.PARTNER
                                && MotherMentorFollowUpType.PARTNER
                                        .getEventType()
                                        .equals(visit.getVisitType());
                boolean childSchema =
                        getFollowUpType() == MotherMentorFollowUpType.CHILD_EID
                                && MotherMentorFollowUpType.CHILD_EID
                                        .getEventType()
                                        .equals(visit.getVisitType());
                String arrivalField =
                        childSchema
                                ? CHILD_EID_ARRIVED_AT_FACILITY
                                : partnerSchema
                                        ? PARTNER_ARRIVED_AT_FACILITY
                                        : RETURNED_TO_FACILITY;
                String arrivalDateField =
                        childSchema
                                ? CHILD_EID_FACILITY_ARRIVAL_DATE
                                : partnerSchema
                                        ? PARTNER_FACILITY_ARRIVAL_DATE
                                        : FACILITY_RETURN_DATE;
                if (childSchema && TextUtils.isEmpty(values.get(CHILD_EID_TEST_TYPE))) return false;
                if (TextUtils.isEmpty(values.get(arrivalField))) return false;
                if (!partnerSchema && !childSchema) {
                    if (TextUtils.isEmpty(values.get(SERVICE_INTERRUPTION_REASON))
                            || TextUtils.isEmpty(values.get(EXPECTED_FACILITY_VISIT_DATE))) return false;
                    if (DBConstants.KEY.OTHER.equals(values.get(SERVICE_INTERRUPTION_REASON))
                            && TextUtils.isEmpty(values.get(SERVICE_INTERRUPTION_REASON_OTHER))) return false;
                }
                return !"yes".equals(values.get(arrivalField))
                        || !TextUtils.isEmpty(values.get(arrivalDateField));
            }
            return getCompletedMotherMentorServiceFields(obsArray)
                    .containsAll(REQUIRED_MOTHER_MENTOR_SERVICE_FIELDS);
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    private Map<String, String> getMotherMentorServiceValues(JSONArray obsArray) {
        Map<String, String> valuesByField = new java.util.HashMap<>();
        for (int i = 0; i < obsArray.length(); i++) {
            JSONObject obs = obsArray.optJSONObject(i);
            if (obs == null) continue;
            JSONArray values = obs.optJSONArray("values");
            if (values != null && values.length() > 0) {
                valuesByField.put(
                        obs.optString("fieldCode").toLowerCase(Locale.US), values.optString(0));
            }
        }
        return valuesByField;
    }

    private Set<String> getCompletedMotherMentorServiceFields(JSONArray obsArray) {
        Set<String> completedFields = new HashSet<>();
        for (int i = 0; i < obsArray.length(); i++) {
            JSONObject obs = obsArray.optJSONObject(i);
            if (!hasRequiredFieldValue(obs)) {
                continue;
            }

            String fieldCode = obs.optString("fieldCode").toLowerCase(Locale.US);
            if (REQUIRED_MOTHER_MENTOR_SERVICE_FIELDS.contains(fieldCode)) {
                completedFields.add(fieldCode);
            }
        }
        return completedFields;
    }

    private boolean hasRequiredFieldValue(JSONObject obs) {
        if (obs == null) {
            return false;
        }

        JSONArray values = obs.optJSONArray("values");
        return values != null && values.length() > 0 && !TextUtils.isEmpty(values.optString(0));
    }

    @Override
    public void openFollowupVisit() {
        if (isFollowUpProfile()) {
            openRecordClientVisit();
        } else {
            startForm(Constants.FORMS.MOTHERMENTOR_FOLLOWUP_VISIT);
        }
    }

    @Override
    public void openRecordTbContactVisit() {
        MotherMentorVisitActivity.startMotherMentorVisitActivity(
                this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openClientObservationResults() {
        openObservationResults();
    }

    @Override
    public void observationResults() {
        openObservationResults();
    }

    @Override
    public void openTbContactFollowUpVisit() {
        startForm(Constants.FORMS.MOTHERMENTOR_FOLLOWUP_VISIT);
    }

    @Override
    public void openRecordClientVisit() {
        if (isFollowUpProfile()) {
            MotherMentorVisitActivity.startMotherMentorFollowUpVisitActivity(
                    this, memberObject.getBaseEntityId(), false, getFollowUpType());
        } else {
            // Re-read persisted visits so a stale profile cannot start a second visit today.
            if (hasCompletedServiceVisitToday()) {
                setupButtons();
                return;
            }
            if (isVisitOnProgress(getServiceVisit())) {
                continueService();
                return;
            }
            MotherMentorVisitActivity.startMotherMentorVisitActivity(
                    this, memberObject.getBaseEntityId(), false);
        }
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        int visitCount = getMotherMentorVisitHistoryCount();
        if (visitCount > 0) {
            rlLastVisit.setVisibility(View.VISIBLE);
            view_last_visit_row.setVisibility(View.VISIBLE);
            findViewById(R.id.view_notification_and_referral_row).setVisibility(View.VISIBLE);
            ((TextView) findViewById(org.smartregister.chw.mothermentor.R.id.ivViewHistoryArrow))
                    .setText(getString(R.string.mothermentor_view_visit_history, visitCount));
            rlLastVisit.setOnClickListener(view -> openMedicalHistory());
            ivViewHistoryArrow.setOnClickListener(view -> openMedicalHistory());
        } else {
            rlLastVisit.setVisibility(View.GONE);
            view_last_visit_row.setVisibility(View.GONE);
        }
    }

    @Override
    public void openMedicalHistory() {
        MotherMentorMedicalHistoryActivity.startMe(
                this, memberObject, isFollowUpProfile() ? getFollowUpType() : null);
    }

    private int getMotherMentorVisitHistoryCount() {
        try {
            if (isFollowUpProfile()) {
                return MotherMentorFollowUpVisitUtils.getVisits(
                                memberObject.getBaseEntityId(), getFollowUpType())
                        .size();
            }
            return VisitUtils.getVisitsOnly(
                                    memberObject.getBaseEntityId(),
                                    Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES)
                            .size()
                    + VisitUtils.getVisitsOnly(
                                    memberObject.getBaseEntityId(),
                                    Constants.EVENT_TYPE.MOTHERMENTOR_CONTACT_VISIT)
                            .size();
        } catch (Exception e) {
            Timber.e(e);
            return 0;
        }
    }

    @Override
    public void startServiceForm() {
        openRecordClientVisit();
    }

    @Override
    public void continueService() {
        if (isFollowUpProfile()) {
            MotherMentorVisitActivity.startMotherMentorFollowUpVisitActivity(
                    this, memberObject.getBaseEntityId(), true, getFollowUpType());
        } else {
            MotherMentorVisitActivity.startMotherMentorVisitActivity(
                    this, memberObject.getBaseEntityId(), true);
        }
    }

    @Override
    public void continueContactVisit() {
        MotherMentorVisitActivity.startMotherMentorVisitActivity(
                this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void openObservationResults() {
        startForm(Constants.FORMS.OBSERVATION_RESULTS);
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        startActivityForResult(
                MotherMentorFormWizardUtils.getFormIntent(this, jsonForm),
                JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    private void startForm(String formName) {
        try {
            JSONObject form = FormUtils.getFormUtils().getFormJson(formName);
            form.put(
                    org.smartregister.util.JsonFormUtils.ENTITY_ID, memberObject.getBaseEntityId());
            startFormActivity(form);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void startSecondaryEnrollment(String formName) {
        MotherMentorRegisterActivity.startRegistration(
                this,
                memberObject.getBaseEntityId(),
                memberObject.getFamilyBaseEntityId(),
                memberObject.getGender(),
                memberObject.getAge(),
                formName);
    }

    @Override
    public void startHivstRegistration() {
        // HIV self-test registration is not supported from the Mother Mentor profile.
    }
}
