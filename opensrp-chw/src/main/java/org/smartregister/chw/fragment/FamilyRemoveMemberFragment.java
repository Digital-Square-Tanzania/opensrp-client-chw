package org.smartregister.chw.fragment;

import android.app.AlertDialog;
import android.os.Bundle;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.FamilyRegisterActivity;
import org.smartregister.chw.core.activity.CoreFamilyRegisterActivity;
import org.smartregister.chw.core.fragment.CoreFamilyProfileChangeDialog;
import org.smartregister.chw.core.fragment.CoreFamilyRemoveMemberFragment;
import org.smartregister.chw.core.utils.CoreConstants;

import org.smartregister.chw.model.FamilyRemoveMemberModel;
import org.smartregister.chw.presenter.FamilyRemoveMemberPresenter;
import org.smartregister.chw.provider.FamilyRemoveMemberProvider;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.Utils;

import java.util.HashMap;
import java.util.Set;

import timber.log.Timber;

public class FamilyRemoveMemberFragment extends CoreFamilyRemoveMemberFragment {

    public static final String DIALOG_TAG = FamilyRemoveMemberFragment.class.getSimpleName();

    public static CoreFamilyRemoveMemberFragment newInstance(Bundle bundle) {
        Bundle args = bundle;
        FamilyRemoveMemberFragment fragment = new FamilyRemoveMemberFragment();
        if (args == null) {
            args = new Bundle();
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void setRemoveMemberProvider(Set visibleColumns, String familyHead, String primaryCaregiver, String familyBaseEntityId) {
        this.removeMemberProvider = new FamilyRemoveMemberProvider(familyBaseEntityId, this.getActivity(),
                this.commonRepository(), visibleColumns, new RemoveMemberListener(), new FooterListener(), familyHead, primaryCaregiver);
    }

    @Override
    public void setAdvancedSearchFormData(HashMap<String, String> hashMap) {
        Timber.v(DIALOG_TAG, "setAdvancedSearchFormData");
    }

    @Override
    protected void setPresenter(String familyHead, String primaryCareGiver) {
        this.presenter = new FamilyRemoveMemberPresenter(this, new FamilyRemoveMemberModel(), null, familyBaseEntityId, familyHead, primaryCareGiver);
    }

    @Override
    protected Class<? extends CoreFamilyRegisterActivity> getFamilyRegisterActivityClass() {
        return FamilyRegisterActivity.class;
    }

    @Override
    protected CoreFamilyProfileChangeDialog getChangeFamilyCareGiverDialog() {
        return FamilyProfileChangeDialog.newInstance(getContext(), familyBaseEntityId,
                CoreConstants.PROFILE_CHANGE_ACTION.PRIMARY_CARE_GIVER);
    }

    @Override
    protected CoreFamilyProfileChangeDialog getChangeFamilyHeadDialog() {
        return FamilyProfileChangeDialog.newInstance(getContext(), familyBaseEntityId,
                CoreConstants.PROFILE_CHANGE_ACTION.HEAD_OF_FAMILY);
    }

    @Override
    protected String getRemoveFamilyMemberDialogTag() {
        return FamilyRemoveMemberFragment.DIALOG_TAG;
    }

    @Override
    public void removeMember(CommonPersonObjectClient client) {
        showRemoveReasonSelectionDialog(client);
    }

    private void showRemoveReasonSelectionDialog(CommonPersonObjectClient client) {
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(getActivity());
        android.view.View dialogView = inflater.inflate(R.layout.dialog_member_remove_reason_selection, null, false);

        android.widget.TextView title = dialogView.findViewById(R.id.dialog_title);
        if (title != null) title.setText(org.smartregister.chw.R.string.household_member_removal_reason_title);

        android.view.View optionDeath = dialogView.findViewById(R.id.option_death);
        android.view.View optionMoveToIndependentClient = dialogView.findViewById(R.id.option_move_to_independent_client);
        android.view.View optionStartedNewFamily = dialogView.findViewById(R.id.option_start_new_family);
        android.view.View cancelBtn = dialogView.findViewById(org.smartregister.chw.R.id.btn_cancel);

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .setCancelable(true)
                .create();
        // Make dialog window background transparent so rounded content background is visible
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        optionDeath.setOnClickListener(v -> {
            dialog.dismiss();
            setReasonForRemove("Death");
            super.removeMember(client);
        });
        optionMoveToIndependentClient.setOnClickListener(v -> {
            dialog.dismiss();
            try {
                if (super.isClientEligibleForRemoval(client, "change_to_independent_client")) {
                    covertToIndependentClient(client);
                } else {
                    super.removeMember(client);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        optionStartedNewFamily.setOnClickListener(v -> {
            dialog.dismiss();
            try {
                if (super.isClientEligibleForRemoval(client, "start_new_family")) {
                    startNewFamily(client);
                } else {
                    super.removeMember(client);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        if (cancelBtn != null) {
            cancelBtn.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void startNewFamily(CommonPersonObjectClient client) throws Exception {
        String locationId = Utils.context().allSharedPreferences().getPreference("CURRENT_LOCATION_ID");
        setReasonForRemove("start_new_family");
        this.getPresenter().startForm(Utils.metadata().familyRegister.formName,client.getColumnmaps().get("unique_id"),
                client.entityId(), null, locationId, "start_new_family");
    }

    private void covertToIndependentClient(CommonPersonObjectClient client) throws Exception {
        String locationId = Utils.context().allSharedPreferences().getPreference("CURRENT_LOCATION_ID");
        setReasonForRemove("change_to_independent_client");
        this.getPresenter().startForm(Utils.metadata().familyRegister.formName,client.getColumnmaps().get("unique_id"),
                client.entityId(), null, locationId, "change_to_independent_client");
    }
}
