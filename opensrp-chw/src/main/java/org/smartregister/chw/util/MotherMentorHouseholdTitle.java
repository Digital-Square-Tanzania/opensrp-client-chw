package org.smartregister.chw.util;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.R;
import org.smartregister.util.Utils;

public final class MotherMentorHouseholdTitle {
    private MotherMentorHouseholdTitle() {}

    public static String format(Context context, String householdName) {
        String name = StringUtils.trimToNull(householdName);
        return name == null ? context.getString(R.string.mothermentor_household_title_unnamed)
                : context.getString(R.string.family_profile_title, Utils.formatValue(name, true));
    }
}
