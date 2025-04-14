package org.smartregister.chw.activity;

import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static org.hl7.fhir.r4.model.codesystems.VariantState.NEGATIVE;
import static org.hl7.fhir.r4.model.codesystems.VariantState.POSITIVE;
import static org.smartregister.chw.util.Constants.JsonFormConstants.STEP1;
import static org.smartregister.family.util.JsonFormUtils.ENCOUNTER_TYPE;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreReferralUtils;
import org.smartregister.chw.dao.ChwHivOutcomeDao;
import org.smartregister.chw.dao.ReferralDao;
import org.smartregister.chw.pmtct.util.NCUtils;
import org.smartregister.chw.referral.activity.ReferralDetailsViewActivity;
import org.smartregister.chw.referral.domain.MemberObject;
import org.smartregister.chw.referral.util.Constants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Location;
import org.smartregister.domain.Task;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.LocationRepository;
import org.smartregister.view.customcontrols.CustomFontTextView;

import timber.log.Timber;

public class ChwReferralDetailsViewActivity extends ReferralDetailsViewActivity {
    TextView tvActionTaken;
    TextView tvComments;
    TextView tvTestResult;
    TextView tvEnrolledClinic;
    TextView tvClinicNumber;
    LinearLayout commentSection;
    LinearLayout actionTakenGroup;
    LinearLayout enrolledClinicGroup;
    LinearLayout feedBackViewGroup;

    public static void startChwReferralDetailsViewActivity(Activity activity, MemberObject memberObject, CommonPersonObjectClient client) {
        Intent intent = new Intent(activity, ChwReferralDetailsViewActivity.class);
        intent.putExtra(Constants.ReferralMemberObject.MEMBER_OBJECT, memberObject);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        tvActionTaken = findViewById(R.id.referral_action_taken_value);
        tvTestResult = findViewById(R.id.referral_feedback_test_result);
        tvEnrolledClinic = findViewById(R.id.referral_feedback_enrolled_clinic_value);
        tvClinicNumber = findViewById(R.id.referral_feedback_clinic_number);
        tvComments = findViewById(R.id.referral_feedback_comments);
        commentSection = findViewById(R.id.referral_feedback_comments_section);
        actionTakenGroup = findViewById(R.id.referral_feedback_action_taken_group);
        enrolledClinicGroup = findViewById(R.id.referral_feedback_clinic_enrolled);
        feedBackViewGroup = findViewById(R.id.referral_details_feedback);

        setupViews();
    }

    private void createCancelReferral(Task task) {
        LinearLayout referralVisitBar = findViewById(R.id.record_visit_bar);
        referralVisitBar.setVisibility(View.VISIBLE);

        CustomFontTextView markAsDone = findViewById(R.id.mark_ask_done);
        markAsDone.setText(R.string.cancel_referral);

        View viewReferralRow = findViewById(R.id.view_referal_row);
        viewReferralRow.setVisibility(View.GONE);

        markAsDone.setOnClickListener(view -> {
            closeReferralDialog(task);
        });

    }

    private void createManualUpdateOfReferral(Task task) throws JSONException {
        JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, org.smartregister.chw.util.Constants.JsonForm.REFERRAL_FOLLOWUP);

        LinearLayout referralVisitBar = findViewById(R.id.record_visit_bar);
        referralVisitBar.setVisibility(View.VISIBLE);

        CustomFontTextView viewProfile = findViewById(R.id.view_profile);
        viewProfile.setText(R.string.referral_followup);

        View viewReferralRow = findViewById(R.id.view_referal_row);
        viewReferralRow.setVisibility(View.GONE);

        viewProfile.setOnClickListener(view -> {
            Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
            intent.putExtra("json", formJsonObject.toString());

            Form form = new Form();
            form.setName(getString(R.string.referral_followup));
            form.setWizard(true);
            form.setActionBarBackground(R.color.family_actionbar);
            form.setNavigationBackground(R.color.family_actionbar);
            form.setHideSaveLabel(true);
            form.setNextLabel(getString(R.string.next));
            form.setPreviousLabel(getString(R.string.previous));
            form.setSaveLabel(getString(R.string.save));
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

            startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
        });
    }

    private void cancelReferral(Task task) {
        MemberObject memberObject = getMemberObject();
        assert memberObject != null;
        task.setForEntity(memberObject.getBaseEntityId());

        CoreReferralUtils.cancelTask(task);
    }

    private void completeReferral(Task task) {
        MemberObject memberObject = getMemberObject();
        assert memberObject != null;
        task.setForEntity(memberObject.getBaseEntityId());

        CoreReferralUtils.completeTask(task, true);
    }

    private void closeReferralDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.cancel_referral_title));
        builder.setMessage(getString(R.string.cancel_referral_message));
        builder.setCancelable(true);

        builder.setPositiveButton(this.getString(R.string.cancel_referral), (dialog, id) -> {
            try {
                cancelReferral(task);
                finish();
            } catch (Exception e) {
                Timber.e(e, "ReferralTaskViewActivity --> closeReferralDialog");
            }
        });
        builder.setNegativeButton(this.getString(R.string.exit), ((dialog, id) -> dialog.cancel()));

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void setupViews() {
        LocationRepository locationRepository = new LocationRepository();
        Location location = locationRepository.getLocationById(getMemberObject().getChwReferralHf());
        ((CustomFontTextView) findViewById(R.id.referral_facility)).setText(location.getProperties().getName());

        if (getMemberObject().getServicesBeforeReferral() != null && getMemberObject().getServicesBeforeReferral().equalsIgnoreCase("None"))
            ((CustomFontTextView) findViewById(R.id.pre_referral_management)).setText(getResources().getString(R.string.none));

        String taskId = ReferralDao.getTaskIdByReasonReference(getMemberObject().getBaseEntityId());
        Task task = ChwApplication.getInstance().getTaskRepository().getTaskByIdentifier(taskId);
        if (!task.getBusinessStatus().equalsIgnoreCase(CoreConstants.BUSINESS_STATUS.COMPLETE)) {
            createCancelReferral(task);
        } else {
            showFeedBackView(task);
        }

        if (task.getBusinessStatus().equalsIgnoreCase(CoreConstants.BUSINESS_STATUS.EXPIRED)) {
            try {
                createManualUpdateOfReferral(task);
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
    }

    private void showFeedBackView(Task task) {


        if (getMemberObject().getChwReferralService().equals(CoreConstants.TASKS_FOCUS.CONVENTIONAL_HIV_TEST)) {
            String servicesProvided = ChwHivOutcomeDao.servicesProvided(task.getForEntity(), task.getLastModified().getMillis());
            String hivStatus = ChwHivOutcomeDao.hivStatus(task.getForEntity(), task.getLastModified().getMillis());
            String enrolledToCTC = ChwHivOutcomeDao.hivEnrolledToCTC(task.getForEntity(), task.getLastModified().getMillis());
            String ctcNumber = ChwHivOutcomeDao.ctcNumber(task.getForEntity(), task.getLastModified().getMillis());
            String reasonsForNotEnrolling = ChwHivOutcomeDao.reasonsForNotEnrolling(task.getForEntity(), task.getLastModified().getMillis());
            String commentsFromHF = ChwHivOutcomeDao.hivCommentsFromHF(task.getForEntity(), task.getLastModified().getMillis());

            if (checkHasFeedBack(servicesProvided, enrolledToCTC, commentsFromHF)) {
                feedBackViewGroup.setVisibility(View.VISIBLE);
                if (servicesProvided != null) {
                    actionTakenGroup.setVisibility(View.VISIBLE);
                    tvActionTaken.setText(getTranslatedHivServicesProvided(servicesProvided));
                }
                if (hivStatus != null) {
                    if (hivStatus.equalsIgnoreCase(POSITIVE.toString()))
                        tvTestResult.setText(getResources().getText(R.string.cbhs_positive));
                    else if (hivStatus.equalsIgnoreCase(NEGATIVE.toString()))
                        tvTestResult.setText(getResources().getText(R.string.cbhs_negative));
                    else
                        tvTestResult.setText(hivStatus);
                } else {
                    tvTestResult.setVisibility(View.GONE);
                }
                if (enrolledToCTC != null) {
                    enrolledClinicGroup.setVisibility(View.VISIBLE);
                    tvEnrolledClinic.setText(getTranslatedEnrolment(enrolledToCTC));
                    if (enrolledToCTC.equalsIgnoreCase("Yes")) {
                        tvClinicNumber.setText(ctcNumber);
                    } else {
                        tvClinicNumber.setText(reasonsForNotEnrolling);
                    }
                }
                if (commentsFromHF != null)
                    commentSection.setVisibility(View.VISIBLE);
                tvComments.setText(commentsFromHF);
            } else {
                feedBackViewGroup.setVisibility(View.GONE);
            }

        }
    }

    private boolean checkHasFeedBack(String servicesProvided, String enrolledToCTC, String commentsFromHF) {
        return servicesProvided != null || enrolledToCTC != null && commentsFromHF != null;
    }

    private String getTranslatedHivServicesProvided(String serviceProvided) {
        switch (serviceProvided) {
            case "no_action_taken":
                return getString(R.string.no_action_taken);
            case "tested":
                return getString(R.string.tests_done);
            case "referred":
                return getString(R.string.referred);
            default:
                return serviceProvided;
        }
    }

    private String getTranslatedEnrolment(String enrolledToCTC) {
        switch (enrolledToCTC) {
            case "yes":
                return getString(R.string.yes);
            case "no":
                return getString(R.string.no);
            default:
                return enrolledToCTC;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON && resultCode == Activity.RESULT_OK) {
            String jsonString = data.getStringExtra(org.smartregister.chw.pmtct.util.Constants.JSON_FORM_EXTRA.JSON);

            JSONObject formJson;
            String encounter_type = "";
            try {
                if (jsonString != null) {
                    formJson = new JSONObject(jsonString);
                    encounter_type = formJson.getString(ENCOUNTER_TYPE);
                }
            } catch (Exception e) {
                Timber.e(e);
            }

            if (encounter_type.equalsIgnoreCase("Referral Follow-up")) {
                try {
                    JSONObject jsonForm = new JSONObject(jsonString);
                    JSONArray fields = jsonForm.getJSONObject(STEP1).getJSONArray(FIELDS);
                    JSONObject didClientAttendReferralJsonObject = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "did_client_attend_referral");

                    String didTheClientAttendReferral = didClientAttendReferralJsonObject.optString(org.smartregister.chw.pmtct.util.JsonFormUtils.VALUE);
                    if (didTheClientAttendReferral.equalsIgnoreCase("yes")) {
                        String taskId = ReferralDao.getTaskIdByReasonReference(getMemberObject().getBaseEntityId());
                        Task task = ChwApplication.getInstance().getTaskRepository().getTaskByIdentifier(taskId);
                        completeReferral(task);
                    }

                    AllSharedPreferences allSharedPreferences = org.smartregister.util.Utils.getAllSharedPreferences();
                    Event baseEvent = org.smartregister.chw.pmtct.util.JsonFormUtils.processJsonForm(allSharedPreferences, jsonString, "ec_referral");
                    org.smartregister.chw.pmtct.util.JsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
                    baseEvent.setBaseEntityId(getMemberObject().getBaseEntityId());
                    NCUtils.processEvent(getMemberObject().getBaseEntityId(), new JSONObject(org.smartregister.chw.pmtct.util.JsonFormUtils.gson.toJson(baseEvent)));
                } catch (Exception e) {
                    Timber.e(e);
                }

            }
        }
    }
}
