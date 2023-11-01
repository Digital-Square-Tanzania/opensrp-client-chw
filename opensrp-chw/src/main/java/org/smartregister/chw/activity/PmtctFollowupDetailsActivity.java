package org.smartregister.chw.activity;

import static com.vijay.jsonwizard.utils.FormUtils.fields;
import static com.vijay.jsonwizard.utils.FormUtils.getFieldJSONObject;
import static org.smartregister.util.JsonFormUtils.STEP1;
import static org.smartregister.util.JsonFormUtils.VALUE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.appbar.AppBarLayout;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.dao.PmtctDao;
import org.smartregister.chw.domain.PmtctReferralMemberObject;
import org.smartregister.chw.pmtct.util.NCUtils;
import org.smartregister.chw.util.Constants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;
import org.smartregister.view.activity.SecuredActivity;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import timber.log.Timber;

public class PmtctFollowupDetailsActivity extends SecuredActivity implements View.OnClickListener {
    public static final String PMTCT_MEMBER_OBJECT = "PMTCT_MEMBER_OBJECT";
    private static PmtctReferralMemberObject memberObject;
    protected AppBarLayout appBarLayout;
    private CustomFontTextView clientName;
    private CustomFontTextView careGiverName;
    private CustomFontTextView careGiverPhone;
    private CustomFontTextView comments;
    private CustomFontTextView referralDate;
    private CustomFontTextView lastFacilityVisitDate;
    private CustomFontTextView referralType;
    private CustomFontTextView locationName;
    private CustomFontTextView markAsDone;
    private String baseEntityId;

    public static void startPmtctDetailsActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, PmtctFollowupDetailsActivity.class);
        intent.putExtra(PMTCT_MEMBER_OBJECT, baseEntityId);
        activity.startActivity(intent);
    }



    @Override
    protected void onCreation() {
        setContentView(R.layout.activity_pmtctc_details);
        inflateToolbar();

        baseEntityId = getIntent().getStringExtra(PMTCT_MEMBER_OBJECT);
        memberObject = PmtctDao.getPmtctReferralMemberObject(baseEntityId);
        setUpViews();
    }

    @Override
    protected void onResumption() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    private void inflateToolbar() {
        Toolbar toolbar = findViewById(R.id.back_pmtct_toolbar);
        CustomFontTextView toolBarTextView = toolbar.findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);

        ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);

        Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);

        upArrow.setColorFilter(getResources().getColor(R.color.text_blue), PorterDuff.Mode.SRC_ATOP);
        supportActionBar.setHomeAsUpIndicator(upArrow);
        supportActionBar.setElevation(0f);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setNavigationOnClickListener(view -> finish());
        }

        toolBarTextView.setText(R.string.back_to_pmtct);
        appBarLayout = findViewById(R.id.app_bar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            appBarLayout.setOutlineProvider(null);
    }

    private void setUpViews() {
        clientName = findViewById(R.id.client_name);
        careGiverName = findViewById(R.id.care_giver_name);
        careGiverPhone = findViewById(R.id.care_giver_phone);
        comments = findViewById(R.id.comments);
        referralDate = findViewById(R.id.referral_date);
        lastFacilityVisitDate = findViewById(R.id.last_facility_visit_date);
        referralType = findViewById(R.id.referral_type);
        locationName = findViewById(R.id.location_name);
        markAsDone = findViewById(R.id.mark_ask_done);
        markAsDone.setOnClickListener(this);
        obtainReferralDetails();
    }

    @SuppressLint("SetTextI18n")
    private void obtainReferralDetails() {
        clientName.setText(memberObject.getFirstName() + " " + memberObject.getMiddleName() + " " + memberObject.getLastName() + ", " + memberObject.getAge());
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        Calendar referralDateCalendar = Calendar.getInstance();
        referralDateCalendar.setTimeInMillis(memberObject.getPmtctCommunityReferralDate().getTime());
        referralDate.setText(dateFormatter.format(referralDateCalendar.getTime()));

        if (memberObject.getLastFacilityVisitDate() != null) {
            referralDateCalendar.setTimeInMillis(memberObject.getLastFacilityVisitDate().getTime());
            lastFacilityVisitDate.setText(dateFormatter.format(referralDateCalendar.getTime()));
        } else {
            findViewById(R.id.referral_facility_layout).setVisibility(View.GONE);
        }
        locationName.setText(memberObject.getAddress());

        if (StringUtils.isNotEmpty(memberObject.getChildName())) {
            careGiverName.setText(String.format(getString(R.string.child_name), memberObject.getChildName()));
            referralType.setText(context().getStringResource(getReasonsForReferralResource(true)));
        } else {
            careGiverName.setVisibility(View.GONE);
            referralType.setText(context().getStringResource(getReasonsForReferralResource(false)));
        }

        if (StringUtils.isBlank(getFamilyMemberContacts()) && StringUtils.isEmpty(getFamilyMemberContacts())) {
            careGiverPhone.setText(getString(R.string.phone_not_provided));
        } else {
            careGiverPhone.setText(getFamilyMemberContacts());
        }

        if (StringUtils.isNotBlank(memberObject.getComments()) && StringUtils.isNotEmpty(memberObject.getComments())) {
            comments.setText(memberObject.getComments());
        }
    }

    private int getReasonsForReferralResource(boolean isChildReason) {
        int resourceId;
        if (isChildReason) {
            switch (memberObject.getReasonsForIssuingCommunityFollowupReferral()) {
                case "missed_appointment":
                    resourceId = R.string.missed_appointment_child;
                    break;
                case "lost_to_followup":
                    resourceId = R.string.lost_to_followup_child;
                    break;
                default:
                    resourceId = -1;
                    break;
            }
        } else {
            switch (memberObject.getReasonsForIssuingCommunityFollowupReferral()) {
                case "missed_appointment":
                    resourceId = R.string.missed_appointment;
                    break;
                case "lost_to_followup":
                    resourceId = R.string.lost_to_followup;
                    break;
                case "mother_champion_services":
                    resourceId = R.string.mother_champion_services;
                    break;
                default:
                    resourceId = -1;
                    break;
            }
        }
        return resourceId;
    }

    private String getFamilyMemberContacts() {
        String phoneNumber = "";
        String familyPhoneNumber = memberObject.getPhoneNumber();
        String familyHeadPhoneNumber = memberObject.getFamilyHeadPhoneNumber();
        String primaryCareGiverPhoneNumber = memberObject.getFamilyHeadPhoneNumber();

        if (StringUtils.isNoneEmpty(familyPhoneNumber)) {
            phoneNumber = familyPhoneNumber;
        } else if (StringUtils.isEmpty(familyHeadPhoneNumber) && StringUtils.isNoneEmpty()) {
            phoneNumber = familyHeadPhoneNumber;
        } else if (StringUtils.isEmpty(primaryCareGiverPhoneNumber) && StringUtils.isNoneEmpty()) {
            phoneNumber = primaryCareGiverPhoneNumber;
        }
        return phoneNumber;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.mark_ask_done) {
            JSONObject form = FormUtils.getFormUtils().getFormJson(Constants.JsonForm.getPmtctCommunityFollowupFeedback());
            AllSharedPreferences preferences = ChwApplication.getInstance().getContext().allSharedPreferences();

            try {
                JSONObject chwName = getFieldJSONObject(fields(form, STEP1), "chw_name");
                chwName.put(VALUE, preferences.getANMPreferredName(preferences.fetchRegisteredANM()));
            } catch (JSONException e) {
                Timber.e(e);
            }
            startActivityForResult(FormUtils.getStartFormActivity(form, getString(R.string.pmtct_followup_feedback_title), this), JsonFormUtils.REQUEST_CODE_GET_JSON);
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == org.smartregister.chw.pmtct.util.Constants.REQUEST_CODE_GET_JSON && resultCode == RESULT_OK) {
            try {
                String jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                JSONObject form = new JSONObject(jsonString);
                String encounterType = form.getString(JsonFormUtils.ENCOUNTER_TYPE);
                if (encounterType.equals(Constants.EncounterType.PMTCT_COMMUNITY_FOLLOWUP_FEEDBACK)) {
                    AllSharedPreferences allSharedPreferences = Utils.getAllSharedPreferences();
                    Event baseEvent = org.smartregister.chw.pmtct.util.JsonFormUtils.processJsonForm(allSharedPreferences, tagReferralFormId(jsonString, baseEntityId), CoreConstants.TABLE_NAME.PMTCT_COMMUNITY_FEEDBACK);
                    org.smartregister.chw.pmtct.util.JsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
                    baseEvent.setBaseEntityId(memberObject.getBaseEntityId());
                    NCUtils.processEvent(baseEvent.getBaseEntityId(), new JSONObject(org.smartregister.chw.pmtct.util.JsonFormUtils.gson.toJson(baseEvent)));
                    Toast.makeText(this, R.string.followup_feedback_recorded, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MotherChampionRegisterActivity.class);
                    startActivity(intent);
                }
            } catch (Exception e) {
                Timber.e(e, "MotherChampionRegisterActivity -- > onActivityResult");
            }
        }
    }

    private String tagReferralFormId(String jsonString, String formSubmissionId) throws JSONException {
        JSONObject form = new JSONObject(jsonString);
        JSONArray fields = org.smartregister.util.JsonFormUtils.fields(form);
        JSONObject referralFormId = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, Constants.PmtctFollowupFeedbackConstants.referralFormId);
        assert referralFormId != null;
        referralFormId.put(JsonFormUtils.VALUE, formSubmissionId);
        return form.toString();
    }
}