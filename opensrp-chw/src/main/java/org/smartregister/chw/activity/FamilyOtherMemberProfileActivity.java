package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.Utils.updateToolbarTitle;
import static org.smartregister.chw.util.Utils.getClientGender;
import static org.smartregister.chw.util.Utils.updateAgeAndGender;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.text.TextUtils;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.core.activity.CoreFamilyOtherMemberProfileActivity;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.custom_view.FamilyMemberFloatingMenu;
import org.smartregister.chw.dao.FamilyDao;
import org.smartregister.chw.dataloader.FamilyMemberDataLoader;
import org.smartregister.chw.fragment.FamilyOtherMemberProfileFragment;
import org.smartregister.chw.presenter.FamilyOtherMemberActivityPresenter;
import org.smartregister.chw.util.AllClientsUtils;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.Utils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.adapter.ViewPagerAdapter;
import org.smartregister.family.fragment.BaseFamilyOtherMemberProfileFragment;
import org.smartregister.family.model.BaseFamilyOtherMemberProfileActivityModel;
import org.smartregister.family.util.DBConstants;
import org.smartregister.view.contract.BaseProfileContract;

import timber.log.Timber;

public class FamilyOtherMemberProfileActivity extends CoreFamilyOtherMemberProfileActivity {
    private FamilyMemberFloatingMenu familyFloatingMenu;
    private Flavor flavor = new FamilyOtherMemberProfileActivityFlv();
    private java.util.List<org.smartregister.chw.model.FamilyDetailsModel> headedFamilies = java.util.Collections.emptyList();

    @Override
    protected void onCreation() {
        super.onCreation();
        setIndependentClient(false);
        updateToolbarTitle(this, R.id.toolbar_title, familyName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        AllClientsUtils.updateOptionsMenu(menu, commonPersonObject);
        try {
            int count = headedFamilies != null ? headedFamilies.size() : 0;

            MenuItem householdsItem = menu.findItem(org.smartregister.chw.R.id.action_view_households);
            if (householdsItem == null) {
                householdsItem = menu.add(Menu.NONE, org.smartregister.chw.R.id.action_view_households, Menu.NONE, "");
            }

            householdsItem.setVisible(count > 0);

            // Inflate a single action view; resource qualifiers swap phone/tablet versions
            householdsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            householdsItem.setActionView(org.smartregister.chw.R.layout.action_households_action);
            View av = householdsItem.getActionView();
            if (av != null) {
                android.widget.TextView tv = av.findViewById(org.smartregister.chw.R.id.tv_households_label);
                if (tv != null) {
                    boolean shortLabel = getResources().getBoolean(org.smartregister.chw.R.bool.use_short_hh_label);
                    tv.setText(getString(shortLabel ? org.smartregister.chw.R.string.hh_with_count : org.smartregister.chw.R.string.household_with_count, count));
                }
                av.setOnClickListener(v -> {
                    if (count <= 0) return;
                    if (count == 1) openFamilyProfile(headedFamilies.get(0)); else handleViewHouseholdsClick();
                });
                String fullTitle = getString(org.smartregister.chw.R.string.view_households_with_count, count);
                av.setContentDescription(fullTitle);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return true;
    }

    @Override
    public FamilyOtherMemberActivityPresenter presenter() {
        return (FamilyOtherMemberActivityPresenter) presenter;
    }

    @Override
    protected void startAncRegister() {
        AncRegisterActivity.startAncRegistrationActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, PhoneNumber,
                Constants.JSON_FORM.getAncRegistration(), null, familyBaseEntityId, familyName);
    }

    @Override
    protected void startPncRegister() {
        PncRegisterActivity.startPncRegistrationActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, PhoneNumber,
                CoreConstants.JSON_FORM.getPregnancyOutcome(), null, familyBaseEntityId, familyName, null);
    }

    @Override
    protected void startMalariaRegister() {
        MalariaRegisterActivity.startMalariaRegistrationActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, familyBaseEntityId);
    }

    @Override
    protected void startVmmcRegister() {
        // Not required
    }



    @Override
    protected void startIntegratedCommunityCaseManagementEnrollment() {
        IccmRegisterActivity.startIccmRegistrationActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, familyBaseEntityId);
    }

    @Override
    protected void startFpRegister() {
        String dob = org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        String gender = org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);

        FpRegisterActivity.startFpRegistrationActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, CoreConstants.JSON_FORM.getFpRegistrationForm(gender));
    }

    @Override
    protected void startFpEcpScreening() {
        //DO Nothing. Not Required in CHW
    }

    @Override
    protected void removeIndividualProfile() {
        IndividualProfileRemoveActivity.startIndividualProfileActivity(FamilyOtherMemberProfileActivity.this,
                commonPersonObject, familyBaseEntityId, familyHead, primaryCaregiver, FamilyRegisterActivity.class.getCanonicalName());
    }

    @Override
    protected void startHivRegister() {
        String gender = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);
        String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = Utils.getAgeFromDate(dob);

        try {
            String formName = Constants.JsonForm.getCbhsRegistrationForm();
            JSONObject formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(FamilyOtherMemberProfileActivity.this, formName);
            JSONArray steps = formJsonObject.getJSONArray("steps");
            JSONObject step = steps.getJSONObject(0);
            JSONArray fields = step.getJSONArray("fields");

            updateAgeAndGender(fields, age, gender);

            HivRegisterActivity.startHIVFormActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, formName, formJsonObject.toString());
        } catch (JSONException e) {
            Timber.e(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void startTbRegister() {
        try {
            TbRegisterActivity.startTbFormActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, Constants.JSON_FORM.getTbRegistration(), (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, Constants.JSON_FORM.getTbRegistration()).toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    protected void startEditMemberJsonForm(Integer title_resource, CommonPersonObjectClient client) {
        String titleString = title_resource != null ? getResources().getString(title_resource) : null;
        boolean isPrimaryCareGiver = commonPersonObject.getCaseId().equalsIgnoreCase(primaryCaregiver);
        String eventName = Utils.metadata().familyMemberRegister.updateEventType;
        String everSchool = client.getColumnmaps().get(CoreConstants.JsonAssets.FAMILY_MEMBER.EVER_SCHOOL);
        String schoolLevel = client.getColumnmaps().get(CoreConstants.JsonAssets.FAMILY_MEMBER.SCHOOL_LEVEL);

        String uniqueID = commonPersonObject.getColumnmaps().get(DBConstants.KEY.UNIQUE_ID);

        NativeFormsDataBinder binder = new NativeFormsDataBinder(getContext(), client.getCaseId());
        binder.setDataLoader(new FamilyMemberDataLoader(familyName, isPrimaryCareGiver, everSchool, schoolLevel, titleString, eventName, uniqueID));
        JSONObject jsonObject = binder.getPrePopulatedForm(CoreConstants.JSON_FORM.getFamilyMemberRegister());

        try {
            if (jsonObject != null)
                startFormActivity(jsonObject);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    protected BaseProfileContract.Presenter getFamilyOtherMemberActivityPresenter(
            String familyBaseEntityId, String baseEntityId, String familyHead, String primaryCaregiver, String villageTown, String familyName) {
        return new FamilyOtherMemberActivityPresenter(this, new BaseFamilyOtherMemberProfileActivityModel(),
                null, familyBaseEntityId, baseEntityId, familyHead, primaryCaregiver, villageTown, familyName);
    }

    @Override
    protected FamilyMemberFloatingMenu getFamilyMemberFloatingMenu() {
        if (familyFloatingMenu == null) {
            familyFloatingMenu = new FamilyMemberFloatingMenu(this);
        }
        return familyFloatingMenu;
    }

    @Override
    protected Context getFamilyOtherMemberProfileActivity() {
        return FamilyOtherMemberProfileActivity.this;
    }

    @Override
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivity() {
        return FamilyProfileActivity.class;
    }

    @Override
    protected void initializePresenter() {
        super.initializePresenter();
        onClickFloatingMenu = flavor.getOnClickFloatingMenu(this, familyBaseEntityId, baseEntityId);
        try {
            headedFamilies = FamilyDao.getFamiliesByHead(baseEntityId);
        } catch (Exception e) {
            Timber.e(e);
            headedFamilies = java.util.Collections.emptyList();
        }
    }

    @Override
    protected ViewPager setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        BaseFamilyOtherMemberProfileFragment profileOtherMemberFragment = FamilyOtherMemberProfileFragment.newInstance(this.getIntent().getExtras());
        adapter.addFragment(profileOtherMemberFragment, "");

        viewPager.setAdapter(adapter);

        return viewPager;
    }

    @Override
    protected BaseFamilyOtherMemberProfileFragment getFamilyOtherMemberProfileFragment() {
        return FamilyOtherMemberProfileFragment.newInstance(getIntent().getExtras());
    }

    @Override
    protected void startMalariaFollowUpVisit() {
        MalariaFollowUpVisitActivity.startMalariaFollowUpActivity(this, baseEntityId);
    }

    @Override
    protected void setIndependentClient(boolean isIndependentClient) {
        super.isIndependent = isIndependentClient;
    }

    @Override
    protected void startHfMalariaFollowupForm() {
        //Implements from super
    }

    @Override
    protected void startPmtctRegisration() {
        //do nothing - implementation in hf
    }

    @Override
    protected void startLDRegistration() {
        //do nothing - implementation in hf
    }

    @Override
    protected void startHivstRegistration() {
        String gender = org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);
        HivstRegisterActivity.startHivstRegistrationActivity(FamilyOtherMemberProfileActivity.this, baseEntityId, gender);
    }

    @Override
    protected void startAgywScreening() {
        String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = Utils.getAgeFromDate(dob);
        AgywRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId, age);
    }

    @Override
    protected void startSbcRegistration() {
        SbcRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startGbvRegistration() {
        //Implement
    }

    @Override
    protected void startCancerPreventiveServicesRegistration() {
        CecapRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startAsrhRegistration() {
        AsrhRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startHtsScreening() {
        //Not required in WAJA
    }

    @Override
    protected void startHpsEnrollment() {
        HpsRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId, org.smartregister.chw.hps.util.Constants.FORMS.HPS_CLIENT_ENROLLMENT, null);
    }

    @Override
    protected void startAypFacilityScreening() {
        // Not required in community build
    }

    @Override
    protected void startAypInSchoolEnrollment() {
        AypInSchoolRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startAypParentalEnrollment() {
        AypParentalRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startTbLeprosyScreening() {
        TbLeprosyRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId);
    }

    @Override
    protected void startAypOutSchoolEnrollment() {
        String gender = AllClientsUtils.getClientGender(baseEntityId);
        String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = Utils.getAgeFromDate(dob);
        AypOutSchoolRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId,gender,age);
    }

    @Override
    protected void startKvpPrEPRegistration() {
        String gender = getClientGender(baseEntityId);
        String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = Utils.getAgeFromDate(dob);
        KvpPrEPRegisterActivity.startRegistration(FamilyOtherMemberProfileActivity.this, baseEntityId, gender, age);
    }

    @Override
    protected void startKvpRegistration() {
        //do nothing
    }

    @Override
    protected void startPrEPRegistration() {
        //do nothing
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        delayInvalidateOptionsMenu();
    }

    private void delayInvalidateOptionsMenu() {
        try {
            new Handler(Looper.getMainLooper()).postDelayed(this::invalidateOptionsMenu, 2000);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null && item.getItemId() == org.smartregister.chw.R.id.action_view_households) {
            handleViewHouseholdsClick();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleViewHouseholdsClick() {
        if (headedFamilies == null || headedFamilies.isEmpty()) return;
        if (headedFamilies.size() == 1) {
            openFamilyProfile(headedFamilies.get(0));
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        android.view.View dialogView = inflater.inflate(org.smartregister.chw.R.layout.dialog_households_list, null, false);
        ListView listView = dialogView.findViewById(org.smartregister.chw.R.id.list_households);
        HouseholdsAdapter adapter = new HouseholdsAdapter(this, headedFamilies);
        listView.setAdapter(adapter);
        android.widget.TextView btnCancel = dialogView.findViewById(org.smartregister.chw.R.id.btn_cancel);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < headedFamilies.size()) {
                openFamilyProfile(headedFamilies.get(position));
                dialog.dismiss();
            }
        });
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    private String getHouseholdDisplay(org.smartregister.chw.model.FamilyDetailsModel family) {
        String name = family != null ? family.getFamilyName() : "";
        String village = family != null ? family.getVillageTown() : "";
        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(village)) {
            return name + " • " + village;
        } else if (!TextUtils.isEmpty(name)) {
            return name;
        } else if (!TextUtils.isEmpty(village)) {
            return village;
        } else {
            return getString(org.smartregister.chw.R.string.family_profile_title, "");
        }
    }

    private void openFamilyProfile(org.smartregister.chw.model.FamilyDetailsModel family) {
        try {
            Intent intent = new Intent(this, FamilyProfileActivity.class);
            intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_BASE_ENTITY_ID, family.getBaseEntityId());
            intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_HEAD, family.getFamilyHead());
            intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.PRIMARY_CAREGIVER, family.getPrimaryCareGiver());
            intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_NAME, family.getFamilyName());
            intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.VILLAGE_TOWN, family.getVillageTown());
            startActivity(intent);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private static class HouseholdsAdapter extends android.widget.BaseAdapter {
        private final java.util.List<org.smartregister.chw.model.FamilyDetailsModel> data;
        private final android.view.LayoutInflater inflater;
        private final android.content.Context context;

        HouseholdsAdapter(android.content.Context context, java.util.List<org.smartregister.chw.model.FamilyDetailsModel> data) {
            this.context = context;
            this.inflater = android.view.LayoutInflater.from(context);
            this.data = data != null ? data : java.util.Collections.emptyList();
        }

        @Override
        public int getCount() { return data.size(); }

        @Override
        public Object getItem(int position) { return data.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(org.smartregister.chw.R.layout.item_household_row, parent, false);
                holder = new ViewHolder();
                holder.title = convertView.findViewById(org.smartregister.chw.R.id.tv_title);
                holder.subtitle = convertView.findViewById(org.smartregister.chw.R.id.tv_subtitle);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            org.smartregister.chw.model.FamilyDetailsModel item = data.get(position);
            String name = item != null ? item.getFamilyName() : "";
            String village = item != null ? item.getVillageTown() : "";

            holder.title.setText(!android.text.TextUtils.isEmpty(name) ? name : context.getString(org.smartregister.chw.R.string.family_profile_title, ""));
            holder.subtitle.setText(village);
            convertView.setContentDescription(name + ", " + village);
            return convertView;
        }

        static class ViewHolder {
            android.widget.TextView title;
            android.widget.TextView subtitle;
        }
    }

    /**
     * build implementation differences file
     */
    public interface Flavor {
        OnClickFloatingMenu getOnClickFloatingMenu(final Activity activity, final String familyBaseEntityId, final String baseEntityId);

        boolean isOfReproductiveAge(CommonPersonObjectClient commonPersonObject, String gender);

        void updateFpMenuItems(@Nullable String baseEntityId, @Nullable Menu menu);

        void updateMalariaMenuItems(@Nullable String baseEntityId, @Nullable Menu menu);

        void updateMaleFpMenuItems(@Nullable String baseEntityId, @Nullable Menu menu);

        void updateHivMenuItems(@Nullable String baseEntityId, @Nullable Menu menu);

        void updateTbMenuItems(@Nullable String baseEntityId, @Nullable Menu menu);
    }
}
