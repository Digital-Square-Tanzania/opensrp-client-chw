package org.smartregister.chw.task;

import static org.smartregister.chw.core.utils.CoreReferralUtils.getCommonRepository;
import static org.smartregister.opd.utils.OpdDbConstants.KEY.REGISTER_TYPE;

import android.app.Activity;
import android.os.Bundle;

import org.smartregister.chw.activity.AboveFiveChildProfileActivity;
import org.smartregister.chw.activity.AncMemberProfileActivity;
import org.smartregister.chw.activity.ChildProfileActivity;
import org.smartregister.chw.activity.FPMemberProfileActivity;
import org.smartregister.chw.activity.HivIndexContactProfileActivity;
import org.smartregister.chw.activity.HivProfileActivity;
import org.smartregister.chw.activity.MalariaProfileActivity;
import org.smartregister.chw.activity.PncMemberProfileActivity;
import org.smartregister.chw.activity.TbProfileActivity;
import org.smartregister.chw.anc.activity.BaseAncMemberProfileActivity;
import org.smartregister.chw.core.activity.CoreAboveFiveChildProfileActivity;
import org.smartregister.chw.core.activity.CoreChildProfileActivity;
import org.smartregister.chw.core.task.CoreChwNotificationGoToMemberProfileTask;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.UpdateDetailsUtil;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.chw.dao.FamilyDao;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.hiv.dao.HivIndexDao;
import org.smartregister.chw.malaria.activity.BaseMalariaProfileActivity;
import org.smartregister.chw.model.FamilyDetailsModel;
import org.smartregister.chw.pnc.activity.BasePncMemberProfileActivity;
import org.smartregister.chw.tb.dao.TbDao;
import org.smartregister.chw.util.AllClientsUtils;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.Constants;
import org.smartregister.opd.utils.OpdDbConstants;

public class ChwGoToMemberProfileBasedOnRegisterTask extends CoreChwNotificationGoToMemberProfileTask {

    public ChwGoToMemberProfileBasedOnRegisterTask(CommonPersonObjectClient commonPersonObjectClient, Bundle bundle, String notificationType, Activity activity) {
        super(commonPersonObjectClient, bundle, notificationType, activity);
    }

    @Override
    protected void goToFpProfile(String baseEntityId, Activity activity) {
        FPMemberProfileActivity.startFpMemberProfileActivity(activity, baseEntityId);
    }

    @Override
    protected void goToHivProfile(String baseEntityId, Activity activity) {
        HivProfileActivity.startHivProfileActivity(activity, HivDao.getMember(baseEntityId));
    }

    @Override
    protected void goToHivIndexContactProfile(String baseEntityId, Activity activity) {
        HivIndexContactProfileActivity.startHivIndexContactProfileActivity(activity, HivIndexDao.getMember(baseEntityId));
    }

    @Override
    protected void goToTbProfile(String baseEntityId, Activity activity) {
        TbProfileActivity.startTbProfileActivity(activity, TbDao.getMember(baseEntityId));
    }

    @Override
    protected void goToOtherMemberProfile(String baseEntityId, CommonPersonObjectClient commonPersonObjectClient, Activity activity) {
        Bundle bundle = new Bundle();
        FamilyDetailsModel familyDetailsModel = FamilyDao.getFamilyDetail(baseEntityId);
        if (commonPersonObjectClient == null || commonPersonObjectClient.getDetails() == null) {
            commonPersonObjectClient = Utils.getCommonPersonObjectClient(baseEntityId);
            final CommonPersonObject personObject = getCommonRepository(org.smartregister.chw.util.Utils.metadata().familyMemberRegister.tableName)
                    .findByBaseEntityId(baseEntityId);
            commonPersonObjectClient.setDetails(personObject.getColumnmaps());
        }

        if (familyDetailsModel != null) {
            bundle.putString(Constants.INTENT_KEY.FAMILY_BASE_ENTITY_ID, familyDetailsModel.getBaseEntityId());
            bundle.putString(Constants.INTENT_KEY.FAMILY_HEAD, familyDetailsModel.getFamilyHead());
            bundle.putString(Constants.INTENT_KEY.PRIMARY_CAREGIVER, familyDetailsModel.getPrimaryCareGiver());
            bundle.putString(Constants.INTENT_KEY.FAMILY_NAME, familyDetailsModel.getFamilyName());
            bundle.putString(Constants.INTENT_KEY.VILLAGE_TOWN, familyDetailsModel.getVillageTown());
            commonPersonObjectClient.getDetails().put(OpdDbConstants.KEY.HOME_ADDRESS, familyDetailsModel.getVillageTown());
        }

        assert familyDetailsModel != null;

        if (UpdateDetailsUtil.isIndependentClient(baseEntityId)) {
            commonPersonObjectClient.getDetails().put(REGISTER_TYPE, CoreConstants.REGISTER_TYPE.INDEPENDENT);
        }

        AllClientsUtils.goToOtherMemberProfile(activity, commonPersonObjectClient, bundle,
                familyDetailsModel.getFamilyHead(), familyDetailsModel.getPrimaryCareGiver());

    }

    @Override
    protected Class<? extends CoreAboveFiveChildProfileActivity> getAboveFiveChildProfileActivityClass() {
        return AboveFiveChildProfileActivity.class;
    }

    @Override
    protected Class<? extends CoreChildProfileActivity> getChildProfileActivityClass() {
        return ChildProfileActivity.class;
    }

    @Override
    protected Class<? extends BaseAncMemberProfileActivity> getAncMemberProfileActivityClass() {
        return AncMemberProfileActivity.class;
    }

    @Override
    protected Class<? extends BasePncMemberProfileActivity> getPncMemberProfileActivityClass() {
        return PncMemberProfileActivity.class;
    }

    @Override
    protected Class<? extends BaseMalariaProfileActivity> getMalariaProfileActivityClass() {
        return MalariaProfileActivity.class;
    }
}
