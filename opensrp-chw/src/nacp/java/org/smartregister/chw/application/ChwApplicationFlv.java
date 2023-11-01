package org.smartregister.chw.application;

import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.util.ChwDBConstants;
import org.smartregister.family.util.DBConstants;

import java.util.HashMap;
import java.util.Map;

public class ChwApplicationFlv extends DefaultChwApplicationFlv {
    @Override
    public boolean hasP2P() {
        return true;
    }

    @Override
    public boolean hasReferrals() {
        return true;
    }

    @Override
    public boolean flvSetFamilyLocation() {
        return true;
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
        return true;
    }

    @Override
    public boolean hasFamilyPlanning() {
        return false;
    }

    @Override
    public boolean hasWashCheck() {
        return false;
    }

    @Override
    public boolean hasMalaria() {
        return false;
    }

    @Override
    public boolean hasServiceReport() {
        return false;
    }

    public boolean hasQR() {
        return false;
    }

    @Override
    public boolean hasJobAids() {
        return false;
    }

    @Override
    public boolean hasTasks() {
        return true;
    }

    @Override
    public boolean hasStockUsageReport() {
        return false;
    }

    @Override
    public boolean hasHIV() {
        return true;
    }

    @Override
    public boolean hasFamilyLocationRow() {
        return true;
    }

    @Override
    public boolean hasTB() {
        return false;
    }

    @Override
    public boolean hasPmtct() {
        return true;
    }

    @Override
    public boolean usesPregnancyRiskProfileLayout() {
        return true;
    }

    public boolean getChildFlavorUtil() {
        return true;
    }

    @Override
    public boolean includeCurrentChild() {
        return true;
    }

    @Override
    public boolean hasMap() {
        return true;
    }

    @Override
    public boolean hasEventDateOnFamilyProfile() {
        return true;
    }

    @Override
    public boolean hasCdp() {
        return true;
    }

    @Override
    public boolean hasHIVST() {
        return true;
    }

    @Override
    public boolean hasAGYW() {
        return false;
    }

    @Override
    public boolean hasKvp() {
        return true;
    }

    @Override
    public boolean hasICCM() {
        return true;
    }

    @Override
    public Map<String, String[]> getFTSSearchMap() {
        Map<String, String[]> map = new HashMap<>();
        map.put(CoreConstants.TABLE_NAME.FAMILY, new String[]{
                DBConstants.KEY.BASE_ENTITY_ID, DBConstants.KEY.VILLAGE_TOWN, DBConstants.KEY.FIRST_NAME,
                DBConstants.KEY.LAST_NAME, DBConstants.KEY.UNIQUE_ID, ChwDBConstants.NEAREST_HEALTH_FACILITY
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
    public boolean hasReports() {
        return false;
    }


    @Override
    public boolean showMyCommunityActivityReport() {
        return true;
    }
}
