package org.smartregister.chw.fragment;

import android.view.View;

import org.smartregister.chw.R;
import org.smartregister.view.customcontrols.CustomFontTextView;

/**
 * Author issyzac on 15/05/2024
 */
public class LinkageFollowUpFragment extends MalariaRegisterFragment{

    @Override
    public void setupViews(View view) {
        super.setupViews(view);

        CustomFontTextView titleView = view.findViewById(R.id.txt_title_label);
        if (titleView != null) {
            titleView.setPadding(0, titleView.getTop(), titleView.getPaddingRight(), titleView.getPaddingBottom());
            titleView.setText("Linkage Follow Up");
        }

    }
}
