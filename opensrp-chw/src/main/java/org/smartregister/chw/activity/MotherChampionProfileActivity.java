package org.smartregister.chw.activity;

import static com.vijay.jsonwizard.utils.FormUtils.fields;
import static com.vijay.jsonwizard.utils.FormUtils.getFieldJSONObject;
import static org.smartregister.util.JsonFormUtils.STEP1;
import static org.smartregister.util.JsonFormUtils.VALUE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CorePmtctProfileActivity;
import org.smartregister.chw.core.interactor.CorePmtctProfileInteractor;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.model.CoreAllClientsMemberModel;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.custom_view.MotherChampionFloatingMenu;
import org.smartregister.chw.dao.MotherChampionDao;
import org.smartregister.chw.model.FamilyProfileModel;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.pmtct.PmtctLibrary;
import org.smartregister.chw.pmtct.domain.Visit;
import org.smartregister.chw.pmtct.util.Constants;
import org.smartregister.chw.pmtct.util.NCUtils;
import org.smartregister.chw.presenter.PmtctMemberProfilePresenter;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.domain.AlertStatus;
import org.smartregister.family.domain.FamilyEventClient;
import org.smartregister.family.interactor.FamilyProfileInteractor;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.repository.AllSharedPreferences;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import timber.log.Timber;

public class MotherChampionProfileActivity extends CorePmtctProfileActivity {
    private static String baseEntityId;
    private List<ReferralTypeModel> referralTypeModels = new ArrayList<>();

    public static void startProfile(Activity activity, String baseEntityId) {
        MotherChampionProfileActivity.baseEntityId = baseEntityId;
        Intent intent = new Intent(activity, MotherChampionProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        activity.startActivity(intent);
    }

    @Override
    protected void initializePresenter() {
        showProgressBar(true);
        String baseEntityId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID);
        memberObject = MotherChampionDao.getMember(baseEntityId);
        profilePresenter = new PmtctMemberProfilePresenter(this, new CorePmtctProfileInteractor(), memberObject);
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        int id = view.getId();
        if (id == R.id.textview_record_pmtct) {
            JSONObject formJsonObject = null;
            try {
                formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, org.smartregister.chw.util.Constants.JsonForm.getMotherChampionFollowupForm());
                AllSharedPreferences preferences = ChwApplication.getInstance().getContext().allSharedPreferences();
                JSONObject chwName = getFieldJSONObject(fields(formJsonObject, STEP1), "chw_name");
                chwName.put(VALUE, preferences.getANMPreferredName(preferences.fetchRegisteredANM()));

                JSONObject visitNumber = getFieldJSONObject(fields(formJsonObject, STEP1), "visit_number");
                visitNumber.put(VALUE, MotherChampionDao.getVisitNumber(memberObject.getBaseEntityId()));

            } catch (JSONException e) {
                e.printStackTrace();
            }
            startFormActivity(formJsonObject);
        }
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        addPmtctReferralTypes();
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        try {

            Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
            intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());

            Form form = new Form();
            form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
            form.setWizard(false);
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

            if (jsonForm.getString("encounter_type").equals(org.smartregister.chw.util.Constants.EncounterType.MOTHER_CHAMPION_FOLLOWUP)) {
                form.setWizard(true);
                form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
                form.setName(getString(R.string.record_followup_visit_mother_champion));
                form.setNextLabel(this.getResources().getString(org.smartregister.chw.core.R.string.next));
                form.setPreviousLabel(this.getResources().getString(org.smartregister.chw.core.R.string.back));
            }

            startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(R.string.return_to_mother_champion_clients);

        textViewRecordPmtct.setText(R.string.record_mother_champion_follouwp_visit);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.action_remove_member).setVisible(false);
        menu.findItem(R.id.action_issue_pmtct_followup_referral).setVisible(false);
        return true;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_GET_JSON && resultCode == RESULT_OK) {
            try {
                String jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                JSONObject form = new JSONObject(jsonString);
                String encounterType = form.getString(JsonFormUtils.ENCOUNTER_TYPE);
                if (encounterType.equals(org.smartregister.chw.util.Constants.EncounterType.MOTHER_CHAMPION_FOLLOWUP)) {
                    AllSharedPreferences allSharedPreferences = org.smartregister.util.Utils.getAllSharedPreferences();
                    Event baseEvent = org.smartregister.chw.pmtct.util.JsonFormUtils.processJsonForm(allSharedPreferences, jsonString, org.smartregister.chw.util.Constants.TableName.MOTHER_CHAMPION_FOLLOWUP);
                    org.smartregister.chw.pmtct.util.JsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
                    baseEvent.setBaseEntityId(baseEntityId);
                    try {
                        NCUtils.processEvent(baseEvent.getBaseEntityId(), new JSONObject(org.smartregister.chw.pmtct.util.JsonFormUtils.gson.toJson(baseEvent)));
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
                if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(Utils.metadata().familyMemberRegister.updateEventType)) {
                    FamilyEventClient familyEventClient =
                            new FamilyProfileModel(memberObject.getFamilyName()).processUpdateMemberRegistration(jsonString, memberObject.getBaseEntityId());
                    new FamilyProfileInteractor().saveRegistration(familyEventClient, jsonString, true, presenter());
                } else if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(Utils.metadata().familyRegister.updateEventType)) {
                    FamilyEventClient familyEventClient = new CoreAllClientsMemberModel().processJsonForm(jsonString, memberObject.getFamilyBaseEntityId());
                    familyEventClient.getEvent().setEntityType(CoreConstants.TABLE_NAME.INDEPENDENT_CLIENT);
                    new FamilyProfileInteractor().saveRegistration(familyEventClient, jsonString, true, presenter());
                }
            } catch (Exception e) {
                Timber.e(e, "MotherChampionProfileActivity -- > onActivityResult");
            }
        }
    }

    @Override
    public void refreshFamilyStatus(AlertStatus status) {
        super.refreshFamilyStatus(status);
        rlFamilyServicesDue.setVisibility(View.GONE);
    }

    @Override
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass() {
        return null;
    }

    @Override
    protected void removeMember() {
        //implement
    }

    @NonNull
    @Override
    public CoreFamilyOtherMemberActivityPresenter presenter() {
        return null;
    }

    @Override
    public void setProfileImage(String s, String s1) {
        //implement
    }

    @Override
    public void setProfileDetailThree(String s) {
        //implement
    }

    @Override
    public void toggleFamilyHead(boolean b) {
        //implement
    }

    @Override
    public void togglePrimaryCaregiver(boolean b) {
        //implement
    }

    @Override
    public void refreshList() {
        //implement
    }

    @Override
    public void updateHasPhone(boolean b) {
        //implement
    }

    @Override
    public void setFamilyServiceStatus(String s) {
        //implement
    }

    @Override
    public void verifyHasPhone() {
        //implement
    }

    @Override
    public void notifyHasPhone(boolean b) {
        //implement
    }

    @Override
    public void initializeFloatingMenu() {
        basePmtctFloatingMenu = new MotherChampionFloatingMenu(this, memberObject);

        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.anc_fab:
                    ((MotherChampionFloatingMenu) basePmtctFloatingMenu).animateFAB();
                    break;
                case R.id.call_layout:
                    ((MotherChampionFloatingMenu) basePmtctFloatingMenu).launchCallWidget();
                    ((MotherChampionFloatingMenu) basePmtctFloatingMenu).animateFAB();
                    break;
                case R.id.refer_to_facility_layout:
                    ((PmtctMemberProfilePresenter) profilePresenter).referToFacility();
                    ((MotherChampionFloatingMenu) basePmtctFloatingMenu).animateFAB();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }

        };

        ((MotherChampionFloatingMenu) basePmtctFloatingMenu).setFloatMenuClickListener(onClickFloatingMenu);
        basePmtctFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.END);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(basePmtctFloatingMenu, linearLayoutParams);
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        Visit lastFollowupVisit = getVisit(org.smartregister.chw.util.Constants.Events.MOTHER_CHAMPION_FOLLOWUP);
        if (lastFollowupVisit != null) {
            rlLastVisit.setVisibility(View.VISIBLE);
            findViewById(R.id.view_notification_and_referral_row).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.vViewHistory)).setText(R.string.visits_history_profile_title);
            ((TextView) findViewById(R.id.ivViewHistoryArrow)).setText(getString(R.string.view_visits_history));
        } else {
            rlLastVisit.setVisibility(View.GONE);
        }
    }

    private void addPmtctReferralTypes() {
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.pmtct_referral),
                    CoreConstants.JSON_FORM.getPmtctReferralForm(), CoreConstants.TASKS_FOCUS.PMTCT));

        }

    }

    public List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
    }

    @Override
    public void openMedicalHistory() {
        MotherChampionMedicalHistoryActivity.startMe(this, memberObject);
    }

    public @Nullable
    Visit getVisit(String eventType) {
        return PmtctLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), eventType);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMedicalHistory(true);
    }
}
