package org.smartregister.chw.fragment;

import android.view.View;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.NcdProfileActivity;
import org.smartregister.chw.core.fragment.CoreNcdRegisterFragment;


public class NcdRegisterFragment extends CoreNcdRegisterFragment {

    @Override
    protected void openProfile(String baseEntityId) {
        NcdProfileActivity.startProfileActivity(getActivity(), baseEntityId);
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        View dueOnlyLayout = view.findViewById(R.id.due_only_layout);
        dueOnlyLayout.setVisibility(View.GONE);
    }
}
