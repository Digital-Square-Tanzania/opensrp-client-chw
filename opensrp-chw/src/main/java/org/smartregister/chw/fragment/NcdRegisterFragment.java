package org.smartregister.chw.fragment;

import android.view.View;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.NcdProfileActivity;
import org.smartregister.chw.activity.NcdRegisterActivity;
import org.smartregister.chw.core.fragment.CoreNcdRegisterFragment;
import org.smartregister.chw.model.NcdRegisterAtRiskFragmentModel;
import org.smartregister.chw.presenter.NcdRegisterFragmentPresenter;

import timber.log.Timber;
import org.smartregister.view.customcontrols.CustomFontTextView;


public class NcdRegisterFragment extends CoreNcdRegisterFragment {

    @Override
    protected void openProfile(String baseEntityId) {
        if (getActivity() instanceof NcdRegisterActivity) {
            ((NcdRegisterActivity) getActivity()).openClientProfile(baseEntityId, false);
        } else {
            Timber.e("Host activity missing NcdRegisterActivity; opening profile directly");
            NcdProfileActivity.startProfileActivity(getActivity(), baseEntityId, false);
        }
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        View dueOnlyLayout = view.findViewById(R.id.due_only_layout);
        if (dueOnlyLayout != null) {
            dueOnlyLayout.setVisibility(View.GONE);
        }

        CustomFontTextView titleView = view.findViewById(R.id.txt_title_label);
        if (titleView != null) {
            titleView.setText(R.string.ncd_at_risk_register_title);
        }
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new NcdRegisterFragmentPresenter(this, new NcdRegisterAtRiskFragmentModel(), null);
    }
}
