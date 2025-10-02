package org.smartregister.chw.custom_view;

import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.smartregister.chw.core.custom_views.CoreTbLeprosyFloatingMenu;
import org.smartregister.chw.R;
import org.smartregister.chw.tbleprosy.domain.MemberObject;

public class TbLeprosyFloatingMenu extends CoreTbLeprosyFloatingMenu {
    public TbLeprosyFloatingMenu(Context context, MemberObject memberObject) {
        super(context, memberObject);
        RelativeLayout referToFacilityLayout = findViewById(R.id.refer_to_facility_layout);
        TextView referTextView = (TextView) referToFacilityLayout.getChildAt(0);
        referTextView.setText(R.string.refer_to_facility);
    }
}
