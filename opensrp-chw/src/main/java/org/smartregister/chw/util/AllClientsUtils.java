package org.smartregister.chw.util;

import static org.smartregister.AllConstants.TEAM_ROLE_IDENTIFIER;
import static org.smartregister.chw.core.utils.CoreConstants.INTENT_KEY.CLIENT;
import static org.smartregister.chw.core.utils.Utils.getDuration;
import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;
import static org.smartregister.opd.utils.OpdDbConstants.KEY.REGISTER_TYPE;
import static org.smartregister.util.Utils.showShortToast;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.AboveFiveChildProfileActivity;
import org.smartregister.chw.activity.AgywProfileActivity;
import org.smartregister.chw.activity.AllClientsMemberProfileActivity;
import org.smartregister.chw.activity.AncMemberProfileActivity;
import org.smartregister.chw.activity.AsrhMemberProfileActivity;
import org.smartregister.chw.activity.CecapMemberProfileActivity;
import org.smartregister.chw.activity.ChildProfileActivity;
import org.smartregister.chw.activity.FamilyOtherMemberProfileActivity;
import org.smartregister.chw.activity.FPMemberProfileActivity;
import org.smartregister.chw.activity.FamilyOtherMemberProfileActivityFlv;
import org.smartregister.chw.activity.HivProfileActivity;
import org.smartregister.chw.activity.IccmProfileActivity;
import org.smartregister.chw.activity.KvpPrEPProfileActivity;
import org.smartregister.chw.activity.MalariaProfileActivity;
import org.smartregister.chw.activity.PncMemberProfileActivity;
import org.smartregister.chw.activity.SbcMemberProfileActivity;
import org.smartregister.chw.activity.TbProfileActivity;
import org.smartregister.chw.agyw.dao.AGYWDao;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.asrh.dao.AsrhDao;
import org.smartregister.chw.cecap.dao.CecapDao;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.utils.CoreChildUtils;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fp.dao.FpDao;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.sbc.dao.SbcDao;
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
import org.smartregister.repository.AllSharedPreferences;

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

    public static void goToCecapProfile(Activity activity, CommonPersonObjectClient client) {
        CecapMemberProfileActivity.startMe(activity, client.getCaseId());
    }

    public static void goToAsrhProfile(Activity activity, CommonPersonObjectClient client) {
        AsrhMemberProfileActivity.startMe(activity, client.getCaseId());
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

    public static void updateOptionsMenu(Menu menu, CommonPersonObjectClient commonPersonObject) {
        String baseEntityId = commonPersonObject.entityId();
        FamilyOtherMemberProfileActivity.Flavor flavor = new FamilyOtherMemberProfileActivityFlv();

        String gender = org.smartregister.chw.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);

        // Cache menu items to avoid multiple lookups
        MenuItem locationInfo = menu.findItem(R.id.action_location_info);
        MenuItem tbRegistration = menu.findItem(R.id.action_tb_registration);
        MenuItem sickChildFollowUp = menu.findItem(R.id.action_sick_child_follow_up);
        MenuItem malariaDiagnosis = menu.findItem(R.id.action_malaria_diagnosis);
        MenuItem removeMember = menu.findItem(R.id.action_remove_member);

        // Set visibility for the common items
        if (locationInfo != null) locationInfo.setVisible(true);
        if (tbRegistration != null) tbRegistration.setVisible(false);
        if (sickChildFollowUp != null) sickChildFollowUp.setVisible(false);
        if (malariaDiagnosis != null) malariaDiagnosis.setVisible(false);
        if (removeMember != null) removeMember.setVisible(false);

        // Get shared preferences once
        AllSharedPreferences allSharedPreferences = org.smartregister.util.Utils.getAllSharedPreferences();
        SharedPreferences preferences = allSharedPreferences.getPreferences();
        String teamRoleIdentifier = preferences != null ? preferences.getString(TEAM_ROLE_IDENTIFIER, "") : "";

        // Helper methods for commonly repeated logic
        int age = getPersonAge(commonPersonObject);
        boolean isFemaleOfReproductiveAge = gender.equalsIgnoreCase("Female") && flavor.isOfReproductiveAge(commonPersonObject, "Female");

        // Handle team role logic
        if (!teamRoleIdentifier.isEmpty()) {
            switch (teamRoleIdentifier) {
                case "cbhs_provider":
                    flavor.updateHivMenuItems(baseEntityId, menu);
                    break;
                case "iccm_provider":
                    updateIccmMenu(menu, baseEntityId, flavor);
                    break;
                default:
                    updateDefaultMenu(menu, baseEntityId, commonPersonObject, flavor, gender, age, isFemaleOfReproductiveAge);
                    break;
            }
        } else {
            updateDefaultMenu(menu, baseEntityId, commonPersonObject, flavor, gender, age, isFemaleOfReproductiveAge);
        }
    }

    private static void updateIccmMenu(Menu menu, String baseEntityId, FamilyOtherMemberProfileActivity.Flavor flavor) {
        MenuItem iccmRegistration = menu.findItem(R.id.action_iccm_registration);
        if (!IccmDao.isRegisteredForIccm(baseEntityId) && iccmRegistration != null) {
            iccmRegistration.setVisible(true);
        }
        setMenuItemVisibility(menu, R.id.action_anc_registration, false);
        setMenuItemVisibility(menu, R.id.action_cbhs_registration, false);
        setMenuItemVisibility(menu, R.id.action_pregnancy_out_come, false);
    }

    private static void updateDefaultMenu(Menu menu, String baseEntityId, CommonPersonObjectClient commonPersonObject,
                                          FamilyOtherMemberProfileActivity.Flavor flavor, String gender, int age, boolean isFemaleOfReproductiveAge) {

        // Handle HIV Menu items
        if (!ChwApplication.getApplicationFlavor().hasHIV()) {
            setMenuItemVisibility(menu, R.id.action_cbhs_registration, false);
        } else {
            flavor.updateHivMenuItems(baseEntityId, menu);
        }

        // Handle Family Planning Menu items
        if (ChwApplication.getApplicationFlavor().hasFamilyPlanning() && flavor.isOfReproductiveAge(commonPersonObject, gender)) {
            flavor.updateFpMenuItems(baseEntityId, menu);
        } else {
            setMenuItemVisibility(menu, R.id.action_fp_initiation, false);
        }

        // Handle ANC related items
        if (ChwApplication.getApplicationFlavor().hasANC()) {
            setMenuItemVisibility(menu, R.id.action_anc_registration, !AncDao.isANCMember(baseEntityId) && isFemaleOfReproductiveAge);
            setMenuItemVisibility(menu, R.id.action_pregnancy_out_come, isFemaleOfReproductiveAge);
        }

        // Handle Malaria Menu items
        if (ChwApplication.getApplicationFlavor().hasMalaria()) {
            flavor.updateMalariaMenuItems(baseEntityId, menu);
        } else {
            setMenuItemVisibility(menu, R.id.action_malaria_registration, false);
        }

        // Handle HIVST menu items
        if (ChwApplication.getApplicationFlavor().hasHIVST()) {
            setMenuItemVisibility(menu, R.id.action_hivst_registration, !HivstDao.isRegisteredForHivst(baseEntityId) && age >= 15);
        }

        // Handle AGYW menu items
        if (ChwApplication.getApplicationFlavor().hasAGYW() && gender.equalsIgnoreCase("Female") && age >= 10 && age <= 24 && !AGYWDao.isRegisteredForAgyw(baseEntityId)) {
            setMenuItemVisibility(menu, R.id.action_agyw_screening, true);
        }

        // Handle Kvp menu items
        if (ChwApplication.getApplicationFlavor().hasKvp()) {
            setMenuItemVisibility(menu, R.id.action_kvp_prep_registration, !KvpDao.isRegisteredForKvpPrEP(baseEntityId) && age >= 15);
        }

        // Handle SBC menu items
        if (ChwApplication.getApplicationFlavor().hasSbc()) {
            setMenuItemVisibility(menu, R.id.action_sbc_registration, !SbcDao.isRegisteredForSbc(baseEntityId) && age >= 10);
        }

        // Handle Cecap menu items
        if (ChwApplication.getApplicationFlavor().hasCecap()) {
            setMenuItemVisibility(menu, R.id.action_cancer_preventive_services_registration, !CecapDao.isRegisteredForCecap(baseEntityId) && age >= 14);
        }

        // Handle Asrh menu items
        if (ChwApplication.getApplicationFlavor().hasAsrh()) {
            setMenuItemVisibility(menu, R.id.action_asrh_registration, !AsrhDao.isRegisteredForAsrh(baseEntityId) && age >= 10 && age < 25);
        }
    }

    private static void setMenuItemVisibility(Menu menu, int itemId, boolean visible) {
        MenuItem item = menu.findItem(itemId);
        if (item != null) {
            item.setVisible(visible);
        }
    }

    private static int getPersonAge(CommonPersonObjectClient commonPersonObject) {
        String dob = org.smartregister.chw.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        return org.smartregister.chw.util.Utils.getAgeFromDate(dob);
    }

}
