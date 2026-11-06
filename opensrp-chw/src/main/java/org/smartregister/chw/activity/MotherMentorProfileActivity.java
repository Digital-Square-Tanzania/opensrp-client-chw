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

import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreMotherMentorProfileActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.dao.MotherMentorChildEidDao;
import org.smartregister.chw.mothermentor.dao.MotherMentorMotherVisitDao;
import org.smartregister.chw.mothermentor.dao.MotherMentorPartnerFollowUpDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherClosureRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorIitRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorChildEidRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherChildRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorPartnerClosureRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorChildEidFollowUpVisitUtils;
import org.smartregister.chw.mothermentor.util.MotherMentorChildEidProfileUi;
import org.smartregister.chw.mothermentor.util.MotherMentorChildEidStatusUtils;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorIitState;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherClosure;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherClosureLabels;
import org.smartregister.chw.mothermentor.util.MotherMentorPartnerFollowUpVisitUtils;
import org.smartregister.chw.mothermentor.util.MotherMentorPartnerClosure;
import org.smartregister.chw.mothermentor.util.MotherMentorIitText;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitCompletion;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitsUtil;
import org.smartregister.chw.mothermentor.util.VisitUtils;
import org.smartregister.chw.util.MarkClientAsDeceasedHelper;
import org.smartregister.chw.util.MotherMentorDailyVisitUtils;
import org.smartregister.chw.util.MotherMentorFollowUpVisitUtils;
import org.smartregister.chw.util.MotherMentorFormWizardUtils;
import org.smartregister.chw.util.MotherMentorIitClosureDialog;
import org.smartregister.chw.util.MotherMentorMotherClosureDialog;
import org.smartregister.chw.util.MotherMentorPartnerClosureText;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.util.AppExecutors;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

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
    private static final Object DEATH_RECORD_LOCK = new Object();
    private final AppExecutors deathRecordExecutors = new AppExecutors();
    private boolean deathRecordInProgress;
    private TextView iitRiskTag;
    private TextView iitDetailsView;
    private TextView iitClosureBanner;
    private TextView pregnancyWeekView;
    private TextView followUpRoleView;
    private TextView motherClosureBanner;
    private boolean manualVisitCompletionInProgress;
    private TextView partnerClosureBanner;
    private LinearLayout iitProfileImageRow;
    private final MotherMentorChildEidProfileUi childProfileUi = new MotherMentorChildEidProfileUi();
    private final AppExecutors childProfileExecutors = new AppExecutors();
    private volatile int childProfileLoadVersion;
    private final Handler visitAvailabilityHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshVisitAvailability =
            () -> {
                if (memberObject != null && !isFinishing()) {
                    setupButtons();
                    if (!isFinishing()) scheduleVisitAvailabilityRefresh();
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
        if (isIitProfile()) {
            return MotherMentorDao.getIitHistoryMember(baseEntityId);
        }
        if (!isFollowUpProfile()) {
            MemberObject mother = MotherMentorDao.getPrimaryMemberForHistory(baseEntityId);
            if (mother != null) return mother;
        }
        MemberObject memberObject = super.getMemberObject(baseEntityId);
        if (memberObject != null) {
            return memberObject;
        }

        memberObject = getHouseholdMember(baseEntityId);
        if (memberObject != null) {
            return memberObject;
        }

        memberObject = getSecondaryEnrollmentMember(baseEntityId);
        if (memberObject == null && isFollowUpProfile()
                && getFollowUpType() == MotherMentorFollowUpType.PARTNER) {
            memberObject = MotherMentorDao.getPartnerHistoryMember(baseEntityId);
        }
        if (memberObject == null && isChildEidProfile()) {
            Map<String, String> values = loadChildEidCardValues(baseEntityId);
            if (!values.isEmpty()) {
                memberObject = new MemberObject();
                memberObject.setBaseEntityId(baseEntityId);
                memberObject.setFirstName(childValue(values, DBConstants.KEY.FIRST_NAME));
                memberObject.setMiddleName(childValue(values, DBConstants.KEY.MIDDLE_NAME));
                memberObject.setLastName(childValue(values, DBConstants.KEY.LAST_NAME));
                memberObject.setDob(childValue(values, DBConstants.KEY.CHILD_DOB));
                memberObject.setGender(childValue(values, DBConstants.KEY.GENDER));
                memberObject.setPhoneNumber(childValue(values, DBConstants.KEY.PHONE_NUMBER));
                memberObject.setFamilyBaseEntityId(childValue(values, DBConstants.KEY.RELATIONAL_ID));
                memberObject.setRelationalId(childValue(values, DBConstants.KEY.RELATIONAL_ID));
                memberObject.setAddress(childValue(values, DBConstants.KEY.VILLAGE_TOWN));
                memberObject.setUniqueId(childValue(values, DBConstants.KEY.UNIQUE_ID));
            }
        }
        return memberObject;
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
    public void initializeFloatingMenu() {
        super.initializeFloatingMenu();
        if (baseMotherMentorFloatingMenu != null) {
            baseMotherMentorFloatingMenu.setReferralClickListener(
                    !getReferralTypeModels().isEmpty()
                            ? view -> openClientReferrals()
                            : null);
        }
    }

    private void openClientReferrals() {
        if (memberObject == null || TextUtils.isEmpty(memberObject.getBaseEntityId())) {
            return;
        }
        List<ReferralTypeModel> referralTypes = getReferralTypeModels();
        if (referralTypes.isEmpty()) {
            return;
        }
        org.smartregister.chw.util.Utils.launchClientReferralActivity(
                this, referralTypes, memberObject.getBaseEntityId());
    }

    public List<ReferralTypeModel> getReferralTypeModels() {
        List<ReferralTypeModel> referralTypes = new ArrayList<>();
        if (!ChwApplication.getApplicationFlavor().hasReferrals()) {
            return referralTypes;
        }
        if (isFollowUpProfile() && getFollowUpType() == MotherMentorFollowUpType.CHILD_EID) {
            referralTypes.add(
                    new ReferralTypeModel(
                            getString(R.string.sick_child),
                            BuildConfig.USE_UNIFIED_REFERRAL_APPROACH
                                    ? CoreConstants.JSON_FORM.getChildUnifiedReferralForm()
                                    : CoreConstants.JSON_FORM.getChildReferralForm(),
                            CoreConstants.TASKS_FOCUS.SICK_CHILD));
            if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
                referralTypes.add(
                        new ReferralTypeModel(
                                getString(R.string.child_gbv_referral),
                                CoreConstants.JSON_FORM.getChildGbvReferralForm(),
                                CoreConstants.TASKS_FOCUS.SUSPECTED_CHILD_GBV));
            }
        } else if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH
                && isFollowUpProfile()
                && getFollowUpType() == MotherMentorFollowUpType.PARTNER) {
            referralTypes.add(
                    new ReferralTypeModel(
                            getString(R.string.hiv_referral),
                            CoreConstants.JSON_FORM.getHivReferralForm(),
                            CoreConstants.TASKS_FOCUS.SICK_HIV));
            referralTypes.add(
                    new ReferralTypeModel(
                            getString(R.string.hts_referral),
                            CoreConstants.JSON_FORM.getHtsReferralForm(),
                            CoreConstants.TASKS_FOCUS.CONVENTIONAL_HIV_TEST));
            referralTypes.add(
                    new ReferralTypeModel(
                            getString(R.string.tb_leprosy_referral),
                            CoreConstants.JSON_FORM.getTbLeprosyReferralForm(),
                            CoreConstants.TASKS_FOCUS.TBLEPROSY));
            referralTypes.add(
                    new ReferralTypeModel(
                            getString(R.string.gbv_referral),
                            CoreConstants.JSON_FORM.getGbvReferralForm(),
                            CoreConstants.TASKS_FOCUS.SUSPECTED_GBV));
        } else if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            referralTypes.add(
                    new ReferralTypeModel(
                            getString(R.string.pmtct_referral),
                            CoreConstants.JSON_FORM.getPmtctReferralForm(),
                            CoreConstants.TASKS_FOCUS.PMTCT));
        }
        return referralTypes;
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
        positionChildEidPill();
        refreshChildEidDetails();
        updateIitHighRiskTag();
        updatePregnancyWeek();
        applyMotherMentorDeceasedHandling();
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

    private boolean isIitProfile() {
        return isFollowUpProfile() && getFollowUpType() == MotherMentorFollowUpType.MOTHER_IIT;
    }

    private boolean isChildEidProfile() {
        return isFollowUpProfile() && getFollowUpType() == MotherMentorFollowUpType.CHILD_EID;
    }

    private void positionChildEidPill() {
        if (!isChildEidProfile() || followUpRoleView == null || imageView == null
                || !(imageView.getParent() instanceof ViewGroup)) return;
        ensureProfileImageTagRow();
        if (followUpRoleView.getParent() != iitProfileImageRow) {
            ((ViewGroup) followUpRoleView.getParent()).removeView(followUpRoleView);
            iitProfileImageRow.addView(followUpRoleView);
        }
    }

    protected MotherMentorChildEidRepository.State loadChildEidState(String childId) {
        return MotherMentorChildEidRepository.getState(
                ChwApplication.getInstance().getRepository().getReadableDatabase(), childId, null);
    }

    protected Map<String, String> loadChildEidCardValues(String childId) {
        return MotherMentorChildEidDao.cardValues(
                ChwApplication.getInstance().getRepository().getReadableDatabase(), childId);
    }

    protected void refreshChildEidDetails() {
        if (!isChildEidProfile() || memberObject == null
                || findViewById(org.smartregister.chw.mothermentor.R.id.profile_name_layout) == null) {
            childProfileUi.hide();
            return;
        }
        final String childId = memberObject.getBaseEntityId();
        final int request = ++childProfileLoadVersion;
        childProfileExecutors.diskIO().execute(() -> {
            if (request != childProfileLoadVersion) return;
            try {
                MotherMentorChildEidRepository.State child = loadChildEidState(childId);
                Map<String, String> identity = loadChildEidCardValues(childId);
                childProfileExecutors.mainThread().execute(() -> {
                    if (request != childProfileLoadVersion || isFinishing() || isDestroyed()
                            || !isChildEidProfile() || memberObject == null
                            || !childId.equals(memberObject.getBaseEntityId())) return;
                    childProfileUi.bind(this, child, identity,
                            new org.joda.time.LocalDate(getVisitAvailabilityDate()), followUpRoleView);
                });
            } catch (Exception e) {
                Timber.e(e, "Unable to load child EID tracing details");
            }
        });
    }

    @Override
    protected void onDestroy() {
        childProfileLoadVersion++;
        super.onDestroy();
    }

    private static String childValue(Map<String, String> values, String key) {
        String value = values.get(key);
        return value == null ? "" : value.trim();
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
        if (isIitProfile()) return;
        if (isFollowUpProfile()) {
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
            iitRiskTag.setAllCaps(!isIitProfile());
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
        if (!isFollowUpProfile() && !isClientDeceased() && !isServiceCardClosed()) {
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
        updateDeceasedMenuItems(menu);
        return result;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean result = super.onPrepareOptionsMenu(menu);
        updateDeceasedMenuItems(menu);
        return result;
    }

    private void updateDeceasedMenuItems(Menu menu) {
        boolean unavailable = isClientDeceased() || isServiceCardClosed();
        for (int itemId : new int[] {R.id.action_mothermentor_enroll_iit,
                R.id.action_mothermentor_enroll_partner, R.id.action_mothermentor_enroll_child_eid}) {
            MenuItem item = menu.findItem(itemId);
            if (item != null && unavailable) item.setVisible(false);
        }
        MenuItem remove = menu.findItem(R.id.action_remove_member);
        if (remove != null) {
            remove.setVisible(memberObject != null
                    && !TextUtils.equals(memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId())
                    && !hasRecordedDeath() && !isServiceCardClosed());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_remove_member) {
            if (!hasRecordedDeath() && !isServiceCardClosed()) openRemoveMember();
            return true;
        }
        if ((itemId == R.id.action_mothermentor_enroll_iit
                || itemId == R.id.action_mothermentor_enroll_partner
                || itemId == R.id.action_mothermentor_enroll_child_eid)
                && (isClientDeceased() || isServiceCardClosed())) {
            return true;
        }
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
        if (memberObject == null || TextUtils.isEmpty(memberObject.getBaseEntityId())
                || isClientDeceased() || isServiceCardClosed()) return false;
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
        if (isFinishing()) return;
        super.setupButtons();
        refreshChildEidDetails();
        MotherMentorPartnerClosure closure = loadPartnerClosure();
        updatePartnerClosureBanner(closure);
        updateIitCard();
        boolean deceased = isClientDeceased();
        updateDeceasedClientStatusTag(deceased);
        updateMotherClosureBanner();
        if (isServiceCardClosed()) {
            if (isIitProfile() && textViewRecordMotherMentor != null) {
                textViewRecordMotherMentor.setText(R.string.mothermentor_record_follow_up);
            }
            hideDeceasedClientActionViews();
            refreshMedicalHistory(true);
            return;
        }
        if (deceased) {
            hideDeceasedClientActionViews();
            refreshMedicalHistory(true);
            return;
        }
        if (textViewRecordMotherMentor != null) {
            textViewRecordMotherMentor.setVisibility(View.VISIBLE);
            boolean canStartVisit =
                    !closure.isClosed() && (isFollowUpProfile()
                            ? !hasCompletedFollowUpVisitToday()
                            : !hasCompletedServiceVisitToday());
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
        enforceProcessVisitVisibility(closure.isClosed());
        refreshMedicalHistory(true);
    }

    protected boolean isMotherCardClosed() {
        if (isFollowUpProfile() || memberObject == null
                || TextUtils.isEmpty(memberObject.getBaseEntityId())) return false;
        try {
            return MotherMentorMotherClosureRepository.isClosed(
                    ChwApplication.getInstance().getRepository().getReadableDatabase(),
                    memberObject.getBaseEntityId());
        } catch (Exception e) {
            Timber.e(e, "Unable to check the mother card closure state");
            return false;
        }
    }

    protected boolean isLinkedFollowUpCardClosed() {
        if (!isFollowUpProfile() || memberObject == null
                || TextUtils.isEmpty(memberObject.getBaseEntityId())) return false;
        if (isIitProfile()) return loadIitState(null).isClosed();
        return MotherMentorMotherClosureRepository.isFollowUpEnrollmentClosed(
                ChwApplication.getInstance().getRepository().getReadableDatabase(),
                memberObject.getBaseEntityId(), getFollowUpType());
    }

    private boolean isServiceCardClosed() {
        return isMotherCardClosed() || isLinkedFollowUpCardClosed();
    }

    protected MotherMentorMotherClosure getMotherClosureState() {
        return MotherMentorMotherClosureRepository.getState(
                ChwApplication.getInstance().getRepository().getReadableDatabase(),
                memberObject.getBaseEntityId(), null);
    }

    void updateMotherClosureBanner() {
        if (isChildEidProfile() || isIitProfile()) {
            if (motherClosureBanner != null) motherClosureBanner.setVisibility(View.GONE);
            return;
        }
        ViewGroup profile = textViewRecordMotherMentor != null
                        && textViewRecordMotherMentor.getParent() instanceof LinearLayout
                ? (ViewGroup) textViewRecordMotherMentor.getParent()
                : findViewById(org.smartregister.chw.mothermentor.R.id.profile_name_layout);
        if (profile == null) return;
        if (motherClosureBanner == null) {
            motherClosureBanner = new TextView(this);
            motherClosureBanner.setTextSize(14);
            motherClosureBanner.setTypeface(Typeface.DEFAULT_BOLD);
            motherClosureBanner.setTextColor(getResources().getColor(R.color.alert_urgent_red));
            motherClosureBanner.setGravity(android.view.Gravity.CENTER);
            int afterButton = profile.indexOfChild(textViewRecordMotherMentor) + 1;
            profile.addView(motherClosureBanner, afterButton);
        }
        boolean closed = isServiceCardClosed();
        motherClosureBanner.setVisibility(closed ? View.VISIBLE : View.GONE);
        if (!closed) return;
        MotherMentorMotherClosure state = getMotherClosureState();
        if (TextUtils.isEmpty(state.getClosedDate())) {
            motherClosureBanner.setText(org.smartregister.chw.mothermentor.R.string
                    .mothermentor_mother_closed_message);
            return;
        }
        motherClosureBanner.setText(getString(
                org.smartregister.chw.mothermentor.R.string.mothermentor_mother_closed_banner,
                MotherMentorMotherClosureLabels.label(this, state.isClosed() ? state.getStatus() : null),
                state.getClosedDate()));
    }

    protected MotherMentorIitState loadIitState(String excludedVisitId) {
        if (memberObject == null) return MotherMentorIitState.active();
        try {
            return MotherMentorIitRepository.getState(
                    ChwApplication.getInstance().getRepository().getReadableDatabase(),
                    memberObject.getBaseEntityId(), excludedVisitId);
        } catch (Exception e) {
            Timber.e(e, "Unable to load the IIT card state");
            return MotherMentorIitState.active();
        }
    }

    private void updateIitCard() {
        if (!isIitProfile() || memberObject == null) return;
        LinearLayout profile = findViewById(org.smartregister.chw.mothermentor.R.id.profile_name_layout);
        if (profile == null) return;
        MotherMentorIitState state = loadIitState(null);
        updateIitStatusPill(state);
        updateIitDetails(profile, state);
        updateIitClosureBanner(profile, state);
    }

    private void updateIitStatusPill(MotherMentorIitState state) {
        int label = org.smartregister.chw.mothermentor.R.string.mothermentor_iit_badge;
        int color = R.color.mothermentor_risk_high;
        if (state.isClosed()) {
            label = org.smartregister.chw.mothermentor.R.string.mothermentor_iit_closed;
        } else if (MotherMentorIitState.REFERRED.equals(state.getReturnState())) {
            label = org.smartregister.chw.mothermentor.R.string.mothermentor_iit_agreed;
            color = org.smartregister.chw.mothermentor.R.color.mothermentor_iit_amber;
        }
        showIitRiskTag(getString(label), getResources().getColor(color));
    }

    private void updateIitDetails(LinearLayout profile, MotherMentorIitState state) {
        if (iitDetailsView == null) {
            iitDetailsView = new TextView(this);
            iitDetailsView.setTextSize(14);
            iitDetailsView.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
            iitDetailsView.setTextColor(Color.DKGRAY);
            iitDetailsView.setBackground(createRiskTagBackground(0xffeef3f8));
            profile.addView(iitDetailsView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        String missed = MotherMentorIitText.missedAppointmentDetails(this, state.getMissedAppointmentDate(),
                new LocalDate(getVisitAvailabilityDate()));
        String counters = MotherMentorIitText.counters(this, state);
        String details = missed + "\n"
                + getString(org.smartregister.chw.mothermentor.R.string.mothermentor_iit_return_status)
                + " · " + (state.isClosed() ? MotherMentorIitText.reason(this, state.getStatus())
                        : MotherMentorIitText.returnState(this, state)) + "\n"
                + getString(org.smartregister.chw.mothermentor.R.string.mothermentor_iit_counter_label)
                + " · " + (counters.isEmpty()
                        ? getString(org.smartregister.chw.mothermentor.R.string.mothermentor_iit_no_counter)
                        : counters);
        if (state.isMamaKinaraLinked()) {
            details += "\n" + getString(
                    org.smartregister.chw.mothermentor.R.string.mothermentor_iit_linked_card_note);
        }
        iitDetailsView.setText(details);
    }

    private void updateIitClosureBanner(LinearLayout profile, MotherMentorIitState state) {
        if (!state.isClosed()) {
            if (iitClosureBanner != null) iitClosureBanner.setVisibility(View.GONE);
            return;
        }
        if (iitClosureBanner == null) {
            iitClosureBanner = new TextView(this);
            iitClosureBanner.setTextColor(Color.WHITE);
            iitClosureBanner.setTextSize(14);
            iitClosureBanner.setTypeface(Typeface.DEFAULT_BOLD);
            iitClosureBanner.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
            profile.addView(iitClosureBanner, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        iitClosureBanner.setVisibility(View.VISIBLE);
        int color = MotherMentorIitState.RETURNED_TO_CARE.equals(state.getStatus())
                ? R.color.mothermentor_risk_low : R.color.mothermentor_risk_high;
        iitClosureBanner.setBackground(createRiskTagBackground(getResources().getColor(color)));
        String reason = MotherMentorIitText.reason(this, state.getStatus());
        iitClosureBanner.setText(state.getClosedDate().isEmpty() ? reason
                : getString(org.smartregister.chw.mothermentor.R.string.mothermentor_iit_closed_banner,
                        reason, state.getClosedDate()));
    }

    protected MotherMentorPartnerClosure loadPartnerClosure() {
        if (!isFollowUpProfile() || getFollowUpType() != MotherMentorFollowUpType.PARTNER
                || memberObject == null) return MotherMentorPartnerClosure.active();
        return MotherMentorPartnerClosureRepository.getState(
                ChwApplication.getInstance().getRepository().getReadableDatabase(),
                memberObject.getBaseEntityId(), null);
    }

    private boolean isPartnerFollowUpClosed() {
        if (isFinishing()) return true;
        if (!loadPartnerClosure().isClosed()) return false;
        setupButtons();
        return true;
    }

    private void updatePartnerClosureBanner(MotherMentorPartnerClosure closure) {
        if (!closure.isClosed()) {
            if (partnerClosureBanner != null) partnerClosureBanner.setVisibility(View.GONE);
            return;
        }
        if (partnerClosureBanner == null && textViewRecordMotherMentor != null
                && textViewRecordMotherMentor.getParent() instanceof LinearLayout) {
            partnerClosureBanner = new TextView(this);
            int padding = (int) (16 * getResources().getDisplayMetrics().density);
            partnerClosureBanner.setPadding(padding, padding / 2, padding, padding / 2);
            partnerClosureBanner.setTextSize(14);
            LinearLayout bar = (LinearLayout) textViewRecordMotherMentor.getParent();
            bar.addView(partnerClosureBanner, bar.indexOfChild(textViewRecordMotherMentor) + 1,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        if (partnerClosureBanner != null) {
            partnerClosureBanner.setVisibility(View.VISIBLE);
            partnerClosureBanner.setText(getString(
                    org.smartregister.chw.mothermentor.R.string.mothermentor_partner_closed_banner,
                    MotherMentorPartnerClosureText.reason(this, closure.getClosureReason()),
                    closure.getClosedDate()));
        }
    }

    protected boolean isClientDeceased() {
        if (memberObject == null || TextUtils.isEmpty(memberObject.getBaseEntityId())) return false;
        try {
            return MotherMentorDao.isClientDeceased(memberObject.getBaseEntityId());
        } catch (Exception e) {
            Timber.e(e, "Unable to check Mother Mentor death status");
            return false;
        }
    }

    private boolean hasRecordedDeath() {
        if (memberObject == null || TextUtils.isEmpty(memberObject.getBaseEntityId())) return false;
        try {
            return MotherMentorDao.hasRecordedDeath(memberObject.getBaseEntityId());
        } catch (Exception e) {
            Timber.e(e, "Unable to check shared death record");
            return false;
        }
    }

    void updateDeceasedClientStatusTag(boolean deceased) {
        TextView status = findViewById(org.smartregister.chw.mothermentor.R.id.family_mothermentor_head);
        if (status == null) return;
        if (deceased) {
            status.setText(org.smartregister.chw.mothermentor.R.string.mothermentor_status_deceased);
        }
        status.setVisibility(deceased ? View.VISIBLE : View.GONE);
    }

    private void hideDeceasedClientActionViews() {
        if (textViewRecordMotherMentor != null) {
            boolean closed = isServiceCardClosed();
            textViewRecordMotherMentor.setVisibility(closed ? View.VISIBLE : View.GONE);
            if (closed) {
                textViewRecordMotherMentor.setEnabled(false);
                textViewRecordMotherMentor.setAlpha(0.5f);
            }
        }
        if (manualProcessVisit != null) manualProcessVisit.setVisibility(View.GONE);
        if (mothermentorServiceInProgress != null) mothermentorServiceInProgress.setVisibility(View.GONE);
        if (visitInProgress != null) visitInProgress.setVisibility(View.GONE);
        if (textViewVisitDoneEdit != null) textViewVisitDoneEdit.setVisibility(View.GONE);
    }

    void applyMotherMentorDeceasedHandling() {
        if (!isClientDeceased()) return;
        updateDeceasedClientStatusTag(true);
        hideDeceasedClientActionViews();
        if (deathRecordInProgress || !isDeathRecordSessionActive()) return;
        final String providerId = getDeathRecordProviderId();
        if (TextUtils.isEmpty(providerId)) return;
        final MemberObject client = memberObject;
        final boolean childEid = isFollowUpProfile()
                && getFollowUpType() == MotherMentorFollowUpType.CHILD_EID;
        deathRecordInProgress = true;
        deathRecordExecutors.diskIO().execute(() -> {
            try {
                synchronized (DEATH_RECORD_LOCK) {
                    if (!isDeathRecordSessionActive()
                            || !TextUtils.equals(providerId, getDeathRecordProviderId())) return;
                    if (!MotherMentorDao.hasRecordedDeath(client.getBaseEntityId())) {
                        Date date = MotherMentorDao.getClientDeathDate(client.getBaseEntityId());
                        if (date == null) {
                            throw new IllegalStateException("Recorded death/service date is unavailable");
                        }
                        String deathDate =
                                new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).format(date);
                        if (childEid) {
                            MarkClientAsDeceasedHelper.markChildAsDeceased(this,
                                    client.getBaseEntityId(), client.getFirstName(),
                                    client.getMiddleName(), client.getLastName(),
                                    client.getGender(), deathDate);
                        } else {
                            MarkClientAsDeceasedHelper.markAsDeceased(this,
                                    client.getBaseEntityId(), client.getFirstName(),
                                    client.getMiddleName(), client.getLastName(),
                                    client.getGender(), deathDate);
                        }
                    }
                    // Repairs enrollments left open by an older app after a shared death record.
                    MotherMentorDao.closeDeceasedClientEnrollments(client.getBaseEntityId());
                }
            } catch (Exception e) {
                Timber.e(e, "Unable to record Mother Mentor client death");
                deathRecordExecutors.mainThread().execute(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        displayToast(org.smartregister.chw.mothermentor.R.string.mothermentor_death_record_failed);
                    }
                });
            } finally {
                deathRecordExecutors.mainThread().execute(() -> {
                    deathRecordInProgress = false;
                    if (!isFinishing() && !isDestroyed()) invalidateOptionsMenu();
                });
            }
        });
    }

    protected boolean isDeathRecordSessionActive() {
        return !context().IsUserLoggedOut();
    }

    protected String getDeathRecordProviderId() {
        return context().allSharedPreferences().fetchRegisteredANM();
    }

    private void openRemoveMember() {
        if (memberObject == null || TextUtils.isEmpty(memberObject.getBaseEntityId())
                || TextUtils.equals(memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId())) return;
        CommonPersonObjectClient client = org.smartregister.chw.core.utils.Utils
                .getCommonPersonObjectClient(memberObject.getBaseEntityId());
        if (client == null) return;
        IndividualProfileRemoveActivity.startIndividualProfileActivity(this, client,
                memberObject.getFamilyBaseEntityId(), memberObject.getFamilyHead(),
                memberObject.getPrimaryCareGiver(), MotherMentorRegisterActivity.class.getCanonicalName());
    }

    private boolean hasCompletedServiceVisitToday() {
        return memberObject != null
                && MotherMentorDailyVisitUtils.hasCompletedServiceVisitToday(
                        VisitUtils.getVisitsOnly(
                                memberObject.getBaseEntityId(),
                                Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES),
                        getVisitAvailabilityDate());
    }

    private boolean hasCompletedFollowUpVisitToday() {
        if (memberObject == null) return false;
        if (getFollowUpType() == MotherMentorFollowUpType.PARTNER) {
            return MotherMentorDao.hasCompletedPartnerFollowUpVisitToday(
                    memberObject.getBaseEntityId(), getVisitAvailabilityDate());
        }
        return getFollowUpType() == MotherMentorFollowUpType.CHILD_EID
                && MotherMentorDao.hasCompletedChildEidFollowUpVisitToday(
                        memberObject.getBaseEntityId(), getVisitAvailabilityDate());
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
            Visit latest = MotherMentorLibrary.getInstance()
                    .visitRepository()
                    .getLatestVisit(
                            memberObject.getBaseEntityId(), getFollowUpType().getEventType());
            return isEarlierIitVisit(latest) ? null : latest;
        }
        return super.getServiceVisit();
    }

    private boolean isEarlierIitVisit(Visit visit) {
        return isIitProfile() && visit != null && !MotherMentorIitRepository.belongsToCurrentEnrollment(
                ChwApplication.getInstance().getRepository().getReadableDatabase(),
                visit.getBaseEntityId(), visit.getJson());
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

    private void enforceProcessVisitVisibility(boolean partnerClosed) {
        if (partnerClosed) {
            manualProcessVisit.setVisibility(View.GONE);
            mothermentorServiceInProgress.setVisibility(View.GONE);
            if (visitInProgress != null) visitInProgress.setVisibility(View.GONE);
            if (textViewVisitDoneEdit != null) textViewVisitDoneEdit.setVisibility(View.GONE);
            return;
        }
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
            manualProcessVisit.setOnClickListener(view -> completeSavedMotherVisit(latestVisit));
        }
    }

    void completeSavedMotherVisit(Visit visit) {
        if (manualVisitCompletionInProgress || isPartnerFollowUpClosed() || isServiceCardClosed()
                || isEarlierIitVisit(visit)) return;
        manualVisitCompletionInProgress = true;
        if (manualProcessVisit != null) manualProcessVisit.setEnabled(false);
        try {
            if (!isFollowUpProfile()) {
                MotherMentorMotherClosure preview = getSavedMotherClosurePreview(visit);
                if (preview.isClosed()) {
                    MotherMentorMotherClosureDialog.show(this, preview,
                            () -> finalizeSavedMotherVisit(visit), this::resetManualVisitCompletion);
                    return;
                }
            } else if (getFollowUpType() == MotherMentorFollowUpType.MOTHER_IIT) {
                MotherMentorIitState preview = getSavedIitClosurePreview(visit);
                if (preview.requiresConfirmation()) {
                    MotherMentorIitClosureDialog.show(this, preview,
                            () -> finalizeSavedMotherVisit(visit), this::resetManualVisitCompletion);
                    return;
                }
            }
            finalizeSavedMotherVisit(visit);
        } catch (Exception e) {
            resetManualVisitCompletion();
            Timber.e(e, "Unable to complete the saved mother visit");
        }
    }

    protected MotherMentorMotherClosure getSavedMotherClosurePreview(Visit visit) throws Exception {
        Map<String, String> values = MotherMentorVisitCompletion.observationValues(
                new JSONObject(visit.getJson()).optJSONArray("obs"));
        return MotherMentorMotherClosureRepository.preview(
                ChwApplication.getInstance().getRepository().getReadableDatabase(),
                memberObject.getBaseEntityId(), visit.getVisitId(), values,
                MotherMentorMotherVisitDao.getServiceDate(visit));
    }

    protected MotherMentorIitState getSavedIitClosurePreview(Visit visit) throws Exception {
        Map<String, String> values = MotherMentorVisitCompletion.observationValues(
                new JSONObject(visit.getJson()).optJSONArray("obs"));
        return MotherMentorIitRepository.preview(
                ChwApplication.getInstance().getRepository().getReadableDatabase(),
                memberObject.getBaseEntityId(), visit.getVisitId(), values,
                MotherMentorMotherVisitDao.getServiceDate(visit));
    }

    protected void persistSavedMotherVisit(Visit visit) throws Exception {
        MotherMentorVisitsUtil.manualProcessVisit(visit);
    }

    private void finalizeSavedMotherVisit(Visit visit) {
        try {
            if (isPartnerFollowUpClosed() || isServiceCardClosed() || isEarlierIitVisit(visit)) return;
            persistSavedMotherVisit(visit);
            displayToast(org.smartregister.chw.mothermentor.R.string.mothermentor_visit_conducted);
            setupViews();
            refreshMedicalHistory(true);
        } catch (Exception e) {
            Timber.e(e, "Unable to complete the saved mother visit");
        } finally {
            resetManualVisitCompletion();
        }
    }

    private void resetManualVisitCompletion() {
        manualVisitCompletionInProgress = false;
        if (manualProcessVisit != null) manualProcessVisit.setEnabled(true);
    }

    private boolean hasCompletedMotherMentorServiceSections(Visit visit) {
        if (visit == null || TextUtils.isEmpty(visit.getJson())) {
            return false;
        }

        try {
            JSONArray obsArray = new JSONObject(visit.getJson()).optJSONArray("obs");
            if (obsArray == null) return false;
            if (!isFollowUpProfile()) return MotherMentorVisitCompletion.isComplete(obsArray);
            Map<String, String> values = getMotherMentorServiceValues(obsArray);
            if (getFollowUpType() == MotherMentorFollowUpType.PARTNER) {
                return MotherMentorPartnerFollowUpVisitUtils.isComplete(
                        values, MotherMentorFollowUpType.PARTNER.getEventType()
                                .equals(visit.getVisitType()));
            }
            if (getFollowUpType() == MotherMentorFollowUpType.CHILD_EID) {
                if (MotherMentorChildEidStatusUtils.isSchema3(values)) {
                    MotherMentorMotherChildRepository.State clinical = getChildEidCompletionContext(visit);
                    return clinical != null && MotherMentorChildEidFollowUpVisitUtils.isComplete(values,
                            MotherMentorFollowUpType.CHILD_EID.getEventType().equals(visit.getVisitType()),
                            clinical.getChildDob(), MotherMentorMotherVisitDao.getServiceDate(visit),
                            clinical.getSamplingHistory());
                }
                return MotherMentorChildEidFollowUpVisitUtils.isComplete(
                        values, MotherMentorFollowUpType.CHILD_EID.getEventType()
                                .equals(visit.getVisitType()));
            }
            if (isIitProfile() && "2".equals(values.get(DBConstants.KEY.IIT_FOLLOW_UP_SCHEMA))) {
                return MotherMentorIitRepository.isComplete(values, loadIitState(visit.getVisitId()));
            }
            String status = values.get(FOLLOW_UP_STATUS);
            if (!TextUtils.isEmpty(status)) {
                if (!"found".equals(status)) return true;
                if (TextUtils.isEmpty(values.get(RETURNED_TO_FACILITY))) return false;
                if (TextUtils.isEmpty(values.get(SERVICE_INTERRUPTION_REASON))
                        || TextUtils.isEmpty(values.get(EXPECTED_FACILITY_VISIT_DATE))) return false;
                if (DBConstants.KEY.OTHER.equals(values.get(SERVICE_INTERRUPTION_REASON))
                        && TextUtils.isEmpty(values.get(SERVICE_INTERRUPTION_REASON_OTHER))) return false;
                return !"yes".equals(values.get(RETURNED_TO_FACILITY))
                        || !TextUtils.isEmpty(values.get(FACILITY_RETURN_DATE));
            }
            return getCompletedMotherMentorServiceFields(obsArray)
                    .containsAll(REQUIRED_MOTHER_MENTOR_SERVICE_FIELDS);
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    /** Clinical context ends before the saved visit, so later tests cannot complete an older visit. */
    protected MotherMentorMotherChildRepository.State getChildEidCompletionContext(Visit visit) {
        if (visit == null || memberObject == null
                || TextUtils.isEmpty(memberObject.getBaseEntityId())) return null;
        try {
            return MotherMentorChildEidRepository.getClinicalStateBeforeVisit(
                    ChwApplication.getInstance().getRepository().getReadableDatabase(),
                    memberObject.getBaseEntityId(), visit.getVisitId());
        } catch (Exception e) {
            Timber.e(e, "Unable to load Child EID clinical context for visit completion");
            return null;
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
        if (isServiceCardClosed()) return;
        if (isFollowUpProfile()) {
            openRecordClientVisit();
        } else {
            startForm(Constants.FORMS.MOTHERMENTOR_FOLLOWUP_VISIT);
        }
    }

    @Override
    public void openRecordTbContactVisit() {
        if (isPartnerFollowUpClosed() || isClientDeceased() || isServiceCardClosed()) return;
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
        if (isServiceCardClosed()) return;
        startForm(Constants.FORMS.MOTHERMENTOR_FOLLOWUP_VISIT);
    }

    @Override
    public void openRecordClientVisit() {
        if (isPartnerFollowUpClosed() || isClientDeceased() || isServiceCardClosed()) return;
        if (isFollowUpProfile()) {
            if (getFollowUpType() == MotherMentorFollowUpType.PARTNER
                    || getFollowUpType() == MotherMentorFollowUpType.CHILD_EID) {
                // Re-read saved records to guard stale button taps after a completed visit.
                if (hasCompletedFollowUpVisitToday()) {
                    setupButtons();
                    return;
                }
                if (isVisitOnProgress(getServiceVisit())) {
                    continueService();
                    return;
                }
            }
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
        if (visitCount > 0 || isIitProfile() && loadIitState(null).isClosed()) {
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
        if (isPartnerFollowUpClosed() || isClientDeceased() || isServiceCardClosed()) return;
        if (isFollowUpProfile()) {
            if (isIitProfile() && getServiceVisit() == null) return;
            MotherMentorVisitActivity.startMotherMentorFollowUpVisitActivity(
                    this, memberObject.getBaseEntityId(), true, getFollowUpType());
        } else {
            MotherMentorVisitActivity.startMotherMentorVisitActivity(
                    this, memberObject.getBaseEntityId(), true);
        }
    }

    @Override
    public void continueContactVisit() {
        if (isPartnerFollowUpClosed() || isClientDeceased() || isServiceCardClosed()) return;
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
        if (isPartnerFollowUpClosed() || isClientDeceased()) return;
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
        if (isServiceCardClosed()) return;
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
