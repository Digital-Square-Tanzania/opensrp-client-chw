package org.smartregister.chw.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import org.smartregister.chw.model.MotherMentorRegisterFragmentModel;
import org.smartregister.chw.presenter.MotherMentorSecondaryEnrollmentRegisterFragmentPresenter;

public class MotherMentorSecondaryEnrollmentRegisterFragment extends MotherMentorRegisterFragment {

    private static final String ARG_TABLE_NAME = "table_name";
    private String sharedSearchText = "";

    public static MotherMentorSecondaryEnrollmentRegisterFragment newInstance(String tableName) {
        MotherMentorSecondaryEnrollmentRegisterFragment fragment = new MotherMentorSecondaryEnrollmentRegisterFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TABLE_NAME, tableName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new MotherMentorSecondaryEnrollmentRegisterFragmentPresenter(
                this,
                new MotherMentorRegisterFragmentModel(),
                null,
                getTableName());
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        hideRegisterHeader(view);
        applySharedSearchText();
    }

    public void setSharedSearchText(String searchText) {
        sharedSearchText = searchText == null ? "" : searchText;
        applySharedSearchText();
    }

    private void applySharedSearchText() {
        EditText searchView = getSearchView();
        if (searchView != null && !TextUtils.equals(searchView.getText(), sharedSearchText)) {
            searchView.setText(sharedSearchText);
            searchView.setSelection(searchView.getText().length());
        }
    }

    private void hideRegisterHeader(View view) {
        setGone(view.findViewById(org.smartregister.R.id.toolbar_parent_layout));
        setGone(view.findViewById(org.smartregister.R.id.search_bar_layout));
        setGone(view.findViewById(org.smartregister.R.id.register_sort_filter_bar_layout));
        setGone(view.findViewById(org.smartregister.R.id.clients_header_layout));
    }

    private void setGone(View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    private String getTableName() {
        Bundle arguments = getArguments();
        return arguments == null ? null : arguments.getString(ARG_TABLE_NAME);
    }
}
