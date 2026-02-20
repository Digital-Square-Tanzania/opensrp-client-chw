package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import org.smartregister.chw.harmreduction.HarmReductionLibrary;
import org.smartregister.chw.harmreduction.util.Constants;

public class HarmReductionMatClientsProfileActivity extends HarmReductionProfileActivity {
    private static final String MAT_CLIENTS_FOLLOWUP_EVENT = "Harm Reduction MAT Clients Followup";

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, HarmReductionMatClientsProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.HARM_REDUCTION_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    protected void setupButtons() {
        textViewRecordHarmReductionVisit.setVisibility(View.VISIBLE);
        textViewRecordHarmReductionVisit.setText(org.smartregister.chw.R.string.record_services_provided_to_mat_clients);
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        if (rlPreMatSessionHistory != null) {
            rlPreMatSessionHistory.setVisibility(View.GONE);
            rlPreMatSessionHistory.setOnClickListener(null);
        }
        if (preMatSessionRowDivider != null) {
            preMatSessionRowDivider.setVisibility(View.GONE);
        }
        if (textViewMarkClientStartedMat != null) {
            textViewMarkClientStartedMat.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == org.smartregister.chw.harmreduction.R.id.textview_record_harm_reduction_community_visit) {
            openRecordClientVisit();
            return;
        }
        super.onClick(view);
    }

    @Override
    public void openRecordClientVisit() {
        HarmReductionMatClientsVisitActivity.startHarmReductionMatClientsVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openFollowupVisit() {
        HarmReductionMatClientsVisitActivity.startHarmReductionMatClientsVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openRecordTbContactVisit() {
        HarmReductionMatClientsVisitActivity.startHarmReductionMatClientsVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openMedicalHistory() {
        HarmReductionMatClientsVisitHistoryActivity.startMe(this, memberObject);
    }

    @Override
    public void startServiceForm() {
        HarmReductionMatClientsVisitActivity.startHarmReductionMatClientsVisitActivity(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void continueService() {
        HarmReductionMatClientsVisitActivity.startHarmReductionMatClientsVisitActivity(this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void continueContactVisit() {
        HarmReductionMatClientsVisitActivity.startHarmReductionMatClientsVisitActivity(this, memberObject.getBaseEntityId(), true);
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        showProgressBar(false);
        org.smartregister.chw.harmreduction.domain.Visit lastVisit = HarmReductionLibrary.getInstance()
                .visitRepository()
                .getLatestVisit(memberObject.getBaseEntityId(), MAT_CLIENTS_FOLLOWUP_EVENT);
        rlLastVisit.setVisibility(lastVisit != null ? View.VISIBLE : View.GONE);
    }
}
