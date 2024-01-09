package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.widget.LinearLayout;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.custom_view.GbvFloatingMenu;
import org.smartregister.chw.gbv.GbvLibrary;
import org.smartregister.chw.gbv.activity.BaseGbvProfileActivity;
import org.smartregister.chw.gbv.domain.MemberObject;
import org.smartregister.chw.gbv.util.Constants;
import org.smartregister.chw.gbv.util.VisitUtils;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.Utils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class GbvMemberProfileActivity extends BaseGbvProfileActivity {
    private final List<ReferralTypeModel> referralTypeModels = new ArrayList<>();

    public static void startMe(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, GbvMemberProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    public void recordGbv(MemberObject memberObject) {

    }

    @Override
    protected void setupViews() {
        super.setupViews();
        try {
            VisitUtils.processVisits(GbvLibrary.getInstance().visitRepository(), GbvLibrary.getInstance().visitDetailsRepository(), GbvMemberProfileActivity.this);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void openMedicalHistory() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        setupViews();
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
    }

    private void addReferralTypes() {
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {

            //HIV Testing referrals will only be issued to non positive clients
            if (memberObject.getCtcNumber().isEmpty()) {
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.hts_referral), CoreConstants.JSON_FORM.getHtsReferralForm(), CoreConstants.TASKS_FOCUS.CONVENTIONAL_HIV_TEST));
            } else { //HIV Treatment and care referrals will be issued to HIV Positive clients
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.hiv_referral), CoreConstants.JSON_FORM.getHivReferralForm(), CoreConstants.TASKS_FOCUS.SICK_HIV));
            }

            referralTypeModels.add(new ReferralTypeModel(getString(R.string.tb_referral), CoreConstants.JSON_FORM.getTbReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_TB));

            if (isClientEligibleForAnc(memberObject)) {
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.anc_danger_signs), CoreConstants.JSON_FORM.getAncUnifiedReferralForm(), CoreConstants.TASKS_FOCUS.ANC_DANGER_SIGNS));
                referralTypeModels.add(new ReferralTypeModel(getString(R.string.pnc_referral), CoreConstants.JSON_FORM.getPncUnifiedReferralForm(), CoreConstants.TASKS_FOCUS.PNC_DANGER_SIGNS));
                if (!AncDao.isANCMember(memberObject.getBaseEntityId())) {
                    referralTypeModels.add(new ReferralTypeModel(getString(R.string.pregnancy_confirmation), CoreConstants.JSON_FORM.getPregnancyConfirmationReferralForm(), CoreConstants.TASKS_FOCUS.PREGNANCY_CONFIRMATION));
                }
            }
            referralTypeModels.add(new ReferralTypeModel(getString(R.string.gbv_referral), CoreConstants.JSON_FORM.getGbvReferralForm(), CoreConstants.TASKS_FOCUS.SUSPECTED_GBV));
        }

    }

    public List<ReferralTypeModel> getReferralTypeModels() {
        return referralTypeModels;
    }

    @Override
    public void initializeFloatingMenu() {
        baseGbvFloatingMenu = new GbvFloatingMenu(this, memberObject);
        baseGbvFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.END);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(baseGbvFloatingMenu, linearLayoutParams);


        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            if (viewId == R.id.gbv_fab) {
                checkPhoneNumberProvided();
                ((GbvFloatingMenu) baseGbvFloatingMenu).animateFAB();
            } else if (viewId == R.id.gbv_call_layout) {
                ((GbvFloatingMenu) baseGbvFloatingMenu).launchCallWidget();
                ((GbvFloatingMenu) baseGbvFloatingMenu).animateFAB();
            } else if (viewId == R.id.gbv_refer_to_facility_layout) {
                org.smartregister.chw.util.Utils.launchClientReferralActivity(GbvMemberProfileActivity.this, getReferralTypeModels(), memberObject.getBaseEntityId());
                ((GbvFloatingMenu) baseGbvFloatingMenu).animateFAB();
            } else {
                Timber.d("Unknown fab action");
            }
        };

        ((GbvFloatingMenu) baseGbvFloatingMenu).setFloatMenuClickListener(onClickFloatingMenu);
    }

    private void checkPhoneNumberProvided() {
        boolean phoneNumberAvailable = (StringUtils.isNotBlank(memberObject.getPhoneNumber()));
        ((GbvFloatingMenu) baseGbvFloatingMenu).redraw(phoneNumberAvailable);
    }

    protected boolean isClientEligibleForAnc(MemberObject hivMemberObject) {
        if (hivMemberObject.getGender().equalsIgnoreCase("Female")) {
            //Obtaining the clients CommonPersonObjectClient used for checking is the client is Of Reproductive Age
            CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);

            final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(hivMemberObject.getBaseEntityId());
            final CommonPersonObjectClient client = new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
            client.setColumnmaps(commonPersonObject.getColumnmaps());

            return org.smartregister.chw.core.utils.Utils.isMemberOfReproductiveAge(client, 15, 49);
        }
        return false;
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        addReferralTypes();
    }
}
