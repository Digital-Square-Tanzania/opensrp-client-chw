package org.smartregister.chw.fragment;

import android.view.View;

import org.smartregister.chw.activity.HarmReductionSoberHouseProfileActivity;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.fragment.CoreHarmReductionRegisterFragment;
import org.smartregister.chw.core.model.CoreHarmReductionRegisterFragmentModel;
import org.smartregister.chw.presenter.HarmReductionSoberHouseRegisterFragmentPresenter;
import org.smartregister.view.customcontrols.CustomFontTextView;

public class HarmReductionSoberHouseRegisterFragment extends CoreHarmReductionRegisterFragment {

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        CustomFontTextView titleView = view.findViewById(R.id.txt_title_label);
        if (titleView != null) {
            titleView.setText(org.smartregister.chw.harmreduction.R.string.record_sober_house_services);
        }
    }

    @Override
    protected void openProfile(String baseEntityId) {
        HarmReductionSoberHouseProfileActivity.startProfileActivity(requireActivity(), baseEntityId);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new HarmReductionSoberHouseRegisterFragmentPresenter(this, new CoreHarmReductionRegisterFragmentModel(), null);
    }
}
