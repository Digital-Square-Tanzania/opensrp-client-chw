package org.smartregister.chw.activity;

import static org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_OUT_SCHOOL_FOLLOW_UP_VISIT;
import static org.smartregister.chw.ayp.util.Constants.FORMS.AYP_OUT_SCHOOL_GRADUATION;
import static org.smartregister.chw.util.Utils.getCommonReferralTypes;
import static org.smartregister.chw.util.Utils.launchClientReferralActivity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.ayp.AypLibrary;
import org.smartregister.chw.ayp.dao.AypDao;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.domain.Visit;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.ayp.util.DBConstants;
import org.smartregister.chw.core.activity.CoreAypProfileActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.AypOutSchoolDao;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class AypOutSchoolMemberProfileActivity extends CoreAypProfileActivity {

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, AypOutSchoolMemberProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(org.smartregister.chw.ayp.util.Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.ayp_PROFILE);
        activity.startActivity(intent);
    }

    private org.smartregister.chw.ayp.domain.Visit getVisit(String eventType) {
        return AypLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), eventType);
    }

    @Override
    protected boolean showReferralView() {
        return true;
    }

    @Override
    public void startReferralForm() {
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            List<ReferralTypeModel> referralTypeModels = new ArrayList<>();
            if (memberObject.getGender().equalsIgnoreCase("male")) {
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.ayp_friendly_services),
                        CoreConstants.JSON_FORM.getMaleAypFriendlyServicesReferralForm(), CoreConstants.TASKS_FOCUS.KVP_FRIENDLY_SERVICES));
            } else {
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.ayp_friendly_services),
                        CoreConstants.JSON_FORM.getFemaleAypFriendlyServicesReferralForm(), CoreConstants.TASKS_FOCUS.KVP_FRIENDLY_SERVICES));
            }
            referralTypeModels.addAll(getCommonReferralTypes(this, memberObject.getBaseEntityId()));

            launchClientReferralActivity(this, referralTypeModels, memberObject.getBaseEntityId());
        } else {
            Toast.makeText(this, "Refer to facility", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void setupViews() {
        super.setupViews();

        if (AypOutSchoolDao.wereSelfTestingKitsDistributed(memberObject.getBaseEntityId())) {
            if (HivstDao.isRegisteredForHivst(memberObject.getBaseEntityId())) {
                boolean shouldIssueHivSelfTestingKits = false;
                String lastSelfTestingFollowupDateString = HivstDao.clientLastFollowup(memberObject.getBaseEntityId());
                if (lastSelfTestingFollowupDateString == null) {
                    shouldIssueHivSelfTestingKits = true;
                } else {
                    try {
                        Date lastSelfTestingFollowupDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(lastSelfTestingFollowupDateString);
                        Visit lastVisit = getVisit(AYP_OUT_SCHOOL_FOLLOW_UP_VISIT);
                        if (truncateTimeFromDate(lastSelfTestingFollowupDate).before(truncateTimeFromDate(lastVisit.getDate())) && lastVisit.getProcessed()) {
                            shouldIssueHivSelfTestingKits = true;
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }

                if (shouldIssueHivSelfTestingKits) {
                    textViewRecordayp.setVisibility(View.GONE);
                    visitDone.setVisibility(View.VISIBLE);
                    textViewVisitDoneEdit.setText(R.string.issue_selft_testing_kits);
                    textViewVisitDone.setText(getContext().getString(R.string.pending_hivst_followup));
                    textViewVisitDone.setVisibility(View.VISIBLE);
                    textViewVisitDoneEdit.setOnClickListener(view -> HivstProfileActivity.startProfile(AypOutSchoolMemberProfileActivity.this, memberObject.getBaseEntityId(), true));
                    imageViewCross.setImageResource(org.smartregister.chw.core.R.drawable.activityrow_notvisited);
                } else {
                    textViewRecordayp.setVisibility(View.VISIBLE);
                    visitDone.setVisibility(View.GONE);
                    textViewVisitDone.setVisibility(View.GONE);
                    if (isVisitOnProgress(getAypOutSchoolVisit())) {
                        textViewRecordayp.setVisibility(View.GONE);
                        visitInProgress.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                textViewRecordayp.setVisibility(View.GONE);
                visitDone.setVisibility(View.VISIBLE);
                textViewVisitDoneEdit.setText(R.string.register_client);
                textViewVisitDone.setText(getContext().getString(R.string.pending_hivst_registration));
                textViewVisitDone.setVisibility(View.VISIBLE);
                textViewVisitDoneEdit.setOnClickListener(v -> startHivstRegistration());
                imageViewCross.setImageResource(org.smartregister.chw.core.R.drawable.activityrow_notvisited);
            }
        }
    }

    private Date truncateTimeFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }


    public void startHivstRegistration() {
        CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);

        final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
        final CommonPersonObjectClient client =
                new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());
        String gender = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);
        HivstRegisterActivity.startHivstRegistrationActivity(this, memberObject.getBaseEntityId(), gender);
    }

    @Override
    public void continueService() {
        AypOutSchoolClientServiceVisitActivity.startAypVisitActivity(this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void continueDischarge() {
    }

    @Override
    public void openFollowupVisit() {
        AypOutSchoolClientServiceVisitActivity.startAypVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void startServiceForm() {
        AypOutSchoolClientServiceVisitActivity.startAypVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void graduateForm() {
        try {
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, AYP_OUT_SCHOOL_GRADUATION);
            startFormActivity(formJsonObject);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        return AypDao.getOutSchoolMember(baseEntityId);
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        setupViews();
        refreshMedicalHistory(true);
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        org.smartregister.chw.ayp.domain.Visit lastVisit = getVisit(AYP_OUT_SCHOOL_FOLLOW_UP_VISIT);
        if (lastVisit != null) {
            rlLastVisit.setVisibility(View.VISIBLE);
            findViewById(R.id.view_notification_and_referral_row).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.vViewHistory)).setText(R.string.visits_history_profile_title);
            ((TextView) findViewById(R.id.ivViewHistoryArrow)).setText(getString(R.string.view_visits_history));
        } else {
            rlLastVisit.setVisibility(View.GONE);
        }
    }

    @Override
    public void openMedicalHistory() {
        AypOutSchoolMedicalHistoryActivity.startMe(this, memberObject);
    }

}

