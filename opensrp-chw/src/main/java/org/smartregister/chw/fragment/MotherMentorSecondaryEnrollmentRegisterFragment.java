package org.smartregister.chw.fragment;

import android.os.Bundle;

import org.smartregister.chw.model.MotherMentorRegisterFragmentModel;
import org.smartregister.chw.presenter.MotherMentorSecondaryEnrollmentRegisterFragmentPresenter;

public class MotherMentorSecondaryEnrollmentRegisterFragment extends MotherMentorRegisterFragment {

    private static final String ARG_TABLE_NAME = "table_name";

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

    private String getTableName() {
        Bundle arguments = getArguments();
        return arguments == null ? null : arguments.getString(ARG_TABLE_NAME);
    }
}
