package org.smartregister.chw.fragment;

import android.view.View;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.NcdProfileActivity;
import org.smartregister.chw.core.fragment.CoreNcdRegisterFragment;
import org.smartregister.chw.model.NcdConfirmedRegisterFragmentModel;
import org.smartregister.chw.presenter.NcdConfirmedRegisterFragmentPresenter;
import org.smartregister.view.customcontrols.CustomFontTextView;

public class NcdConfirmedRegisterFragment extends CoreNcdRegisterFragment {

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        CustomFontTextView titleView = view.findViewById(R.id.txt_title_label);
        if (titleView != null) {
            titleView.setPadding(0, titleView.getTop(), titleView.getPaddingRight(), titleView.getPaddingBottom());
            titleView.setText(this.getString(R.string.ncd_confirmed_register_title));
        }
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new NcdConfirmedRegisterFragmentPresenter(this, new NcdConfirmedRegisterFragmentModel(), null);
    }

    @Override
    public void countExecute() {
        super.countExecute();
    }

    @Override
    protected void openProfile(String baseEntityId) {
        NcdProfileActivity.startProfileActivity(getActivity(), baseEntityId, true);
    }
}
