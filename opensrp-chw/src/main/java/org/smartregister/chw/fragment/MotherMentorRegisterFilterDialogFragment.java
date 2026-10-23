package org.smartregister.chw.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import org.smartregister.chw.R;

/** A draft is committed only by Save; Back and dismissal leave the applied filters alone. */
public class MotherMentorRegisterFilterDialogFragment extends DialogFragment {
    public static final String TAG = "mama_kinara_register_filter";
    private static final String STATE_FILTER = "filter_draft";
    private MotherMentorRegisterFilterState draft = MotherMentorRegisterFilterState.allClients();
    private SwitchCompat enabled;
    private CheckBox pregnant;
    private CheckBox breastfeeding;
    private CheckBox underTwo;

    public interface Listener {
        void onFilterSaved(MotherMentorRegisterFilterState state);
    }

    public static MotherMentorRegisterFilterDialogFragment newInstance(
            MotherMentorRegisterFilterState filter) {
        MotherMentorRegisterFilterDialogFragment dialog =
                new MotherMentorRegisterFilterDialogFragment();
        Bundle args = new Bundle();
        args.putBundle(STATE_FILTER, filter.toBundle());
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar);
        Bundle state = savedInstanceState == null ? getArguments() : savedInstanceState;
        draft =
                MotherMentorRegisterFilterState.fromBundle(
                        state == null ? null : state.getBundle(STATE_FILTER));
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_mama_kinara_register_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        enabled = view.findViewById(R.id.mama_kinara_filter_enabled);
        pregnant = view.findViewById(R.id.mama_kinara_filter_pregnant);
        breastfeeding = view.findViewById(R.id.mama_kinara_filter_breastfeeding);
        underTwo = view.findViewById(R.id.mama_kinara_filter_under_two);
        enabled.setChecked(draft.isEnabled());
        pregnant.setChecked(draft.isPregnant());
        breastfeeding.setChecked(draft.isBreastfeeding());
        underTwo.setChecked(draft.isUnderTwo());
        enabled.setOnCheckedChangeListener((button, checked) -> updateControls());
        view.findViewById(R.id.mama_kinara_filter_back).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.mama_kinara_filter_save).setOnClickListener(v -> save());
        updateControls();
    }

    private void updateControls() {
        enabled.setText(
                enabled.isChecked()
                        ? R.string.mama_kinara_register_filter_on
                        : R.string.mama_kinara_register_filter_off);
        getView()
                .findViewById(R.id.mama_kinara_filter_options)
                .setVisibility(enabled.isChecked() ? View.VISIBLE : View.GONE);
        for (CheckBox option : new CheckBox[] {pregnant, breastfeeding, underTwo}) {
            option.setEnabled(enabled.isChecked());
            option.setAlpha(enabled.isChecked() ? 1f : 0.45f);
        }
    }

    private MotherMentorRegisterFilterState readDraft() {
        return new MotherMentorRegisterFilterState(
                enabled.isChecked(),
                pregnant.isChecked(),
                breastfeeding.isChecked(),
                underTwo.isChecked());
    }

    private void save() {
        Fragment target = getTargetFragment();
        if (target instanceof Listener) ((Listener) target).onFilterSaved(readDraft().applied());
        dismiss();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (enabled != null) draft = readDraft();
        outState.putBundle(STATE_FILTER, draft.toBundle());
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog() == null ? null : getDialog().getWindow();
        if (window != null) {
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setStatusBarColor(getResources().getColor(R.color.mama_kinara_blue));
            window.setNavigationBarColor(getResources().getColor(R.color.mama_kinara_white));
        }
    }

    @Override
    public void onDestroyView() {
        if (enabled != null) draft = readDraft();
        enabled = null;
        pregnant = null;
        breastfeeding = null;
        underTwo = null;
        super.onDestroyView();
    }
}
