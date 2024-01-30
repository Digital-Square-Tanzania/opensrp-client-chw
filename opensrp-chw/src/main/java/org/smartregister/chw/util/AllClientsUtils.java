package org.smartregister.chw.util;

import static org.smartregister.chw.core.utils.CoreConstants.INTENT_KEY.CLIENT;
import static org.smartregister.chw.core.utils.Utils.getDuration;
import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;
import static org.smartregister.opd.utils.OpdDbConstants.KEY.REGISTER_TYPE;
import static org.smartregister.util.Utils.showShortToast;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.AboveFiveChildProfileActivity;
import org.smartregister.chw.activity.AgywProfileActivity;
import org.smartregister.chw.activity.AllClientsMemberProfileActivity;
import org.smartregister.chw.activity.AncMemberProfileActivity;
import org.smartregister.chw.activity.ChildProfileActivity;
import org.smartregister.chw.activity.FamilyOtherMemberProfileActivity;
import org.smartregister.chw.activity.FPMemberProfileActivity;
import org.smartregister.chw.activity.GbvMemberProfileActivity;
import org.smartregister.chw.activity.HivProfileActivity;
import org.smartregister.chw.activity.IccmProfileActivity;
import org.smartregister.chw.activity.KvpPrEPProfileActivity;
import org.smartregister.chw.activity.MalariaProfileActivity;
import org.smartregister.chw.activity.PncMemberProfileActivity;
import org.smartregister.chw.activity.SbcMemberProfileActivity;
import org.smartregister.chw.activity.TbProfileActivity;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.utils.CoreChildUtils;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fp.dao.FpDao;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.tb.dao.TbDao;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.domain.FamilyEventClient;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.opd.pojo.OpdEventClient;
import org.smartregister.opd.utils.OpdDbConstants;

import java.util.ArrayList;
import java.util.List;

public class AllClientsUtils {

    public static void goToChildProfile(Activity activity, CommonPersonObjectClient patient, Bundle bundle) {
        String dobString = getDuration(Utils.getValue(patient.getColumnmaps(), DBConstants.KEY.DOB, false));
        Integer yearOfBirth = CoreChildUtils.dobStringToYear(dobString);
        Intent intent;
        if (yearOfBirth != null && yearOfBirth >= 5) {
            intent = new Intent(activity, AboveFiveChildProfileActivity.class);
        } else {
            intent = new Intent(activity, ChildProfileActivity.class);
        }
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        passToolbarTitle(activity, intent);
        intent.putExtra(Constants.INTENT_KEY.BASE_ENTITY_ID, patient.getCaseId());
        intent.putExtra(org.smartregister.chw.anc.util.Constants.ANC_MEMBER_OBJECTS.MEMBER_PROFILE_OBJECT, new MemberObject(patient));
        activity.startActivity(intent);
    }

    public static void gotToPncProfile(Activity activity, CommonPersonObjectClient patient, Bundle bundle) {
        patient.getColumnmaps().putAll(CoreChwApplication.pncRegisterRepository().getPncCommonPersonObject(patient.entityId()).getColumnmaps());
        activity.startActivity(initProfileActivityIntent(activity, patient, bundle, PncMemberProfileActivity.class));
    }

    public static void goToAncProfile(Activity activity, CommonPersonObjectClient patient) {
        AncMemberProfileActivity.startMe(activity, patient.getCaseId());
    }

    public static void gotToMalariaProfile(Activity activity, CommonPersonObjectClient patient) {
        MalariaProfileActivity.startMalariaActivity(activity, patient.getCaseId());
    }

    public static void gotToIccmProfile(Activity activity, CommonPersonObjectClient patient) {
        IccmProfileActivity.startMalariaActivity(activity, patient.getColumnmaps().get("base_entity_id"));
    }

    public static void goToFamilyPlanningProfile(Activity activity, CommonPersonObjectClient patient) {
        FPMemberProfileActivity.startFpMemberProfileActivity(activity, patient.getCaseId());
    }

    public static void goToHivProfile(Activity activity, CommonPersonObjectClient patient) {
        HivProfileActivity.startHivProfileActivity(activity, HivDao.getMember(patient.getCaseId()));
    }

    public static void goToTbProfile(Activity activity, CommonPersonObjectClient patient) {
        TbProfileActivity.startTbProfileActivity(activity, TbDao.getMember(patient.getCaseId()));
    }

    public static void goToAgywProfile(Activity activity, CommonPersonObjectClient client) {
        AgywProfileActivity.startProfile(activity, client.getCaseId());
    }

    public static void goToKvpPrepProfile(Activity activity, CommonPersonObjectClient client) {
        KvpPrEPProfileActivity.startProfileActivity(activity, client.getCaseId());
    }

    public static void goToSbcProfile(Activity activity, CommonPersonObjectClient client) {
        SbcMemberProfileActivity.startMe(activity, client.getCaseId());
    }

    public static void goToGbvProfile(Activity activity, CommonPersonObjectClient client) {
        GbvMemberProfileActivity.startMe(activity, client.getCaseId());
    }

    private static Intent initProfileActivityIntent(Activity activity, CommonPersonObjectClient patient, Bundle bundle, Class clazz) {
        Intent intent = new Intent(activity, clazz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        intent.putExtra(org.smartregister.chw.anc.util.Constants.ANC_MEMBER_OBJECTS.BASE_ENTITY_ID, patient.entityId());
        intent.putExtra(CLIENT, patient);
        passToolbarTitle(activity, intent);
        return intent;
    }

    public static void goToOtherMemberProfile(Activity activity, CommonPersonObjectClient patient,
                                              Bundle bundle, String familyHead, String primaryCaregiver) {

        if (StringUtils.isBlank(familyHead) && StringUtils.isBlank(primaryCaregiver)) {
            showShortToast(activity, activity.getString(R.string.error_opening_profile));
        } else {
            String registerType = patient.getDetails().get(REGISTER_TYPE);
            Intent intent;
            if (CoreConstants.REGISTER_TYPE.INDEPENDENT.equals(registerType)) {
                intent = new Intent(activity, AllClientsMemberProfileActivity.class);
            } else {
                intent = new Intent(activity, FamilyOtherMemberProfileActivity.class);
            }
            intent.putExtras(bundle != null ? bundle : new Bundle());
            intent.putExtra(Constants.INTENT_KEY.BASE_ENTITY_ID, patient.getCaseId());
            intent.putExtra(CoreConstants.INTENT_KEY.CHILD_COMMON_PERSON, patient);
            intent.putExtra(Constants.INTENT_KEY.FAMILY_HEAD, familyHead);
            intent.putExtra(Constants.INTENT_KEY.PRIMARY_CAREGIVER, primaryCaregiver);
            intent.putExtra(Constants.INTENT_KEY.VILLAGE_TOWN, patient.getDetails().get(OpdDbConstants.KEY.HOME_ADDRESS));
            passToolbarTitle(activity, intent);
            activity.startActivity(intent);
        }
    }

    @NotNull
    public static List<OpdEventClient> getOpdEventClients(String jsonString) {
        List<OpdEventClient> allClientMemberEvents = new ArrayList<>();

        FamilyEventClient locationDetailsEvent = JsonFormUtils.processFamilyUpdateForm(
                Utils.context().allSharedPreferences(), jsonString);
        if (locationDetailsEvent == null) {
            return allClientMemberEvents;
        }

        FamilyEventClient clientDetailsEvent = JsonFormUtils.processFamilyHeadRegistrationForm(
                Utils.context().allSharedPreferences(), jsonString, locationDetailsEvent.getClient().getBaseEntityId());
        if (clientDetailsEvent == null) {
            return allClientMemberEvents;
        }

        if (clientDetailsEvent.getClient() != null && locationDetailsEvent.getClient() != null) {
            String headUniqueId = clientDetailsEvent.getClient().getIdentifier(Utils.metadata().uniqueIdentifierKey);
            if (StringUtils.isNotBlank(headUniqueId)) {
                String familyUniqueId = headUniqueId + Constants.IDENTIFIER.FAMILY_SUFFIX;
                locationDetailsEvent.getClient().addIdentifier(Utils.metadata().uniqueIdentifierKey, familyUniqueId);
            }
        }

        // Update the family head and primary caregiver
        Client familyClient = locationDetailsEvent.getClient();
        familyClient.addRelationship(Utils.metadata().familyRegister.familyHeadRelationKey, clientDetailsEvent.getClient().getBaseEntityId());
        familyClient.addRelationship(Utils.metadata().familyRegister.familyCareGiverRelationKey, clientDetailsEvent.getClient().getBaseEntityId());

        //Use different entity type for independent members
        locationDetailsEvent.getEvent().setEntityType(CoreConstants.TABLE_NAME.INDEPENDENT_CLIENT);
        clientDetailsEvent.getEvent().setEntityType(CoreConstants.TABLE_NAME.INDEPENDENT_CLIENT);

        allClientMemberEvents.add(new OpdEventClient(locationDetailsEvent.getClient(), locationDetailsEvent.getEvent()));
        allClientMemberEvents.add(new OpdEventClient(clientDetailsEvent.getClient(), clientDetailsEvent.getEvent()));
        return allClientMemberEvents;
    }
}
