package org.smartregister.chw.util;


import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.rule.PNCHealthFacilityVisitRule;
import org.smartregister.chw.core.utils.Utils;

import java.util.Date;

public class PNCVisitUtil {

    public static PNCHealthFacilityVisitRule getNextPNCHealthFacilityVisit(Date deliveryDate, Date lastVisitDate) {

        PNCHealthFacilityVisitRule visitRule = new PNCHealthFacilityVisitRule(deliveryDate, lastVisitDate);
        visitRule = ChwApplication.getInstance().getRulesEngineHelper().getPNCHealthFacilityRule(visitRule, Constants.RULE_FILE.PNC_HEALTH_FACILITY_VISIT);

        return visitRule;
    }

    public static String getTranslatedValue(Context context, String name) {
        if (StringUtils.isBlank(name))
            return name;
        String val = "pnc_" + name;
        return Utils.getStringResourceByName(val, context);
    }

}
