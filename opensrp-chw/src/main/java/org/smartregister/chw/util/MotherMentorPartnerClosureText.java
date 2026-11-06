package org.smartregister.chw.util;

import android.content.Context;
import android.text.TextUtils;

/** Resolves the library's localized partner closure reasons. */
public final class MotherMentorPartnerClosureText {
    private MotherMentorPartnerClosureText() {}

    public static String reason(Context context, String status) {
        if (TextUtils.isEmpty(status)) return "";
        int resource = context.getResources().getIdentifier(
                "mothermentor_partner_closure_" + status, "string", context.getPackageName());
        return resource == 0 ? status : context.getString(resource);
    }
}
