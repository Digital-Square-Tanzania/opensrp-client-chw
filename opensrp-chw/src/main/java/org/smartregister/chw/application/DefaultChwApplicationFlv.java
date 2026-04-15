package org.smartregister.chw.application;

import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.family.util.DBConstants;

import java.util.HashMap;
import java.util.Map;

public abstract class DefaultChwApplicationFlv implements ChwApplication.Flavor {
    @Override
    public boolean hasP2P() {
        return true;
    }

    @Override
    public boolean syncUsingPost() {
        return true;
    }

    @Override
    public boolean hasReferrals() {
        return false;
    }

    @Override
    public boolean flvSetFamilyLocation() {
        return false;
    }

    @Override
    public boolean hasANC() {
        return true;
    }

    @Override
    public boolean hasPNC() {
        return true;
    }

    @Override
    public boolean hasChildSickForm() {
        return false;
    }

    @Override
    public boolean hasFamilyPlanning() {
        return false;
    }

    @Override
    public boolean hasMalaria() {
        return false;
    }

    @Override
    public boolean hasWashCheck() {
        return true;
    }

    @Override
    public boolean hasFamilyKitCheck() {
        return false;
    }

    @Override
    public boolean hasRoutineVisit() {
        return false;
    }

    @Override
    public boolean hasServiceReport() {
        return true;
    }

    @Override
    public boolean hasStockUsageReport() {
        return true;
    }

    @Override
    public boolean hasPinLogin() {
        return false;
    }

    @Override
    public boolean hasReports() {
        return true;
    }

    @Override
    public boolean hasJobAids() {
        return true;
    }

    @Override
    public boolean hasQR() {
        return false;
    }

    @Override
    public boolean hasTasks() {
        return false;
    }

    @Override
    public boolean hasDefaultDueFilterForChildClient() {
        return false;
    }

    public boolean hasJobAidsVitaminAGraph() {
        return true;
    }

    @Override
    public boolean hasJobAidsDewormingGraph() {
        return true;
    }

    @Override
    public boolean hasChildrenMNPSupplementationGraph() {
        return true;
    }

    @Override
    public boolean hasJobAidsBreastfeedingGraph() {
        return true;
    }

    @Override
    public boolean hasJobAidsBirthCertificationGraph() {
        return true;
    }

    @Override
    public boolean hasSurname() {
        return true;
    }

    @Override
    public boolean showMyCommunityActivityReport() {
        return true;
    }

    @Override
    public boolean showChildrenUnder5() {
        return true;
    }

    @Override
    public boolean launchChildClientsAtLogin() {
        return false;
    }

    @Override
    public boolean showNoDueVaccineView() {
        return false;
    }

    @Override
    public boolean useThinkMd() {
        return false;
    }

    @Override
    public boolean hasHIV() {
        return false;
    }

    @Override
    public boolean hasFamilyLocationRow() {
        return false;
    }

    @Override
    public boolean hasTB() {
        return false;
    }

    @Override
    public boolean usesPregnancyRiskProfileLayout() {
        return false;
    }

    @Override
    public boolean getChildFlavorUtil() {
        return false;
    }

    @Override
    public boolean prioritizeChildNameOnChildRegister() {
        return false;
    }

    @Override
    public boolean splitUpcomingServicesView() {
        return false;
    }

    @Override
    public boolean showChildrenUnderFiveAndGirlsAgeNineToEleven() {
        return false;
    }

    @Override
    public boolean dueVaccinesFilterInChildRegister() {
        return false;
    }

    @Override
    public boolean includeCurrentChild() {
        return true;
    }

    @Override
    public boolean saveOnSubmission() {
        return false;
    }

    @Override
    public boolean relaxVisitDateRestrictions() {
        return false;
    }

    @Override
    public boolean showLastNameOnChildProfile() {
        return false;
    }

    @Override
    public boolean showChildrenAboveTwoDueStatus() {
        return true;
    }

    @Override
    public boolean showFamilyServicesScheduleWithChildrenAboveTwo() {
        return true;
    }

    @Override
    public boolean hasForeignData() {
        return false;
    }

    @Override
    public boolean showIconsForChildrenUnderTwoAndGirlsAgeNineToEleven() {
        return false;
    }

    @Override
    public boolean hasMap() {
        return false;
    }

    @Override
    public boolean hasEventDateOnFamilyProfile() {
        return false;
    }

    @Override
    public boolean hasADDO() {
        return true;
    }

    @Override
    public String[] getFTSTables() {
        return new String[]{CoreConstants.TABLE_NAME.FAMILY, CoreConstants.TABLE_NAME.FAMILY_MEMBER, CoreConstants.TABLE_NAME.CHILD};
    }

    @Override
    public Map<String, String[]> getFTSSearchMap() {
        Map<String, String[]> map = new HashMap<>();
        map.put(CoreConstants.TABLE_NAME.FAMILY, new String[]{
                DBConstants.KEY.BASE_ENTITY_ID, DBConstants.KEY.VILLAGE_TOWN, DBConstants.KEY.FIRST_NAME,
                DBConstants.KEY.LAST_NAME, DBConstants.KEY.UNIQUE_ID
        });

        map.put(CoreConstants.TABLE_NAME.FAMILY_MEMBER, new String[]{
                DBConstants.KEY.BASE_ENTITY_ID, DBConstants.KEY.FIRST_NAME, DBConstants.KEY.MIDDLE_NAME,
                DBConstants.KEY.LAST_NAME, DBConstants.KEY.UNIQUE_ID, ChildDBConstants.KEY.ENTRY_POINT, DBConstants.KEY.DOB, DBConstants.KEY.DATE_REMOVED
        });

        map.put(CoreConstants.TABLE_NAME.CHILD, new String[]{
                DBConstants.KEY.BASE_ENTITY_ID, DBConstants.KEY.FIRST_NAME, DBConstants.KEY.MIDDLE_NAME,
                DBConstants.KEY.LAST_NAME, DBConstants.KEY.UNIQUE_ID, ChildDBConstants.KEY.ENTRY_POINT, DBConstants.KEY.DOB, DBConstants.KEY.DATE_REMOVED
        });
        return map;
    }

    @Override
    public Map<String, String[]> getFTSSortMap() {
        Map<String, String[]> map = new HashMap<>();
        map.put(CoreConstants.TABLE_NAME.FAMILY, new String[]{DBConstants.KEY.LAST_INTERACTED_WITH, DBConstants.KEY.DATE_REMOVED,
                DBConstants.KEY.FAMILY_HEAD, DBConstants.KEY.PRIMARY_CAREGIVER, DBConstants.KEY.ENTITY_TYPE,
                CoreConstants.DB_CONSTANTS.DETAILS
        });

        map.put(CoreConstants.TABLE_NAME.FAMILY_MEMBER, new String[]{DBConstants.KEY.DOB, DBConstants.KEY.DOD,
                DBConstants.KEY.LAST_INTERACTED_WITH, DBConstants.KEY.DATE_REMOVED, DBConstants.KEY.RELATIONAL_ID
        });

        map.put(CoreConstants.TABLE_NAME.CHILD, new String[]{ChildDBConstants.KEY.LAST_HOME_VISIT, ChildDBConstants.KEY.VISIT_NOT_DONE, DBConstants.KEY
                .LAST_INTERACTED_WITH, ChildDBConstants.KEY.DATE_CREATED, DBConstants.KEY.DATE_REMOVED, DBConstants.KEY.DOB, ChildDBConstants.KEY.ENTRY_POINT
        });
        return map;
    }
}
