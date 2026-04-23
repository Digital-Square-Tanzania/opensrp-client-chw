package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.presenter.BaseAncMedicalHistoryPresenter;
import org.smartregister.chw.core.activity.CoreAncMedicalHistoryActivity;
import org.smartregister.chw.interactor.NcdMedicalHistoryInteractor;
import org.smartregister.chw.ncd.domain.MemberObject;

import java.util.List;

/**
 * Renders the list of past NCD Monthly Follow-Up visits for a client. Entered from the
 * NCD profile's "Last visit" row via {@link NcdProfileActivity#openMedicalHistory()}.
 * Uses the shared CHW Medical History scaffold (CoreAncMedicalHistoryActivity + Flavor)
 * so we inherit layout, toolbar, and progress handling for free.
 */
public class NcdMedicalHistoryActivity extends CoreAncMedicalHistoryActivity {

    private static MemberObject ncdMemberObject;

    private final Flavor flavor = new NcdMedicalHistoryActivityFlv();

    private ProgressBar progressBar;

    public static void startMe(Activity activity, MemberObject memberObject) {
        Intent intent = new Intent(activity, NcdMedicalHistoryActivity.class);
        ncdMemberObject = memberObject;
        activity.startActivity(intent);
    }

    @Override
    public void initializePresenter() {
        presenter = new BaseAncMedicalHistoryPresenter(
                new NcdMedicalHistoryInteractor(), this, ncdMemberObject.getBaseEntityId());
    }

    @Override
    public void setUpView() {
        linearLayout = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.linearLayoutMedicalHistory);
        progressBar = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.progressBarMedicalHistory);

        TextView tvTitle = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle);
        tvTitle.setText(getString(org.smartregister.chw.opensrp_chw_anc.R.string.back_to,
                ncdMemberObject.getFirstName()));

        ((TextView) findViewById(R.id.medical_history)).setText(getString(R.string.visits_history));
    }

    @Override
    public View renderView(List<Visit> visits) {
        super.renderView(visits);
        View view = flavor.bindViews(this);
        displayLoadingState(true);
        flavor.processViewData(visits, this);
        displayLoadingState(false);
        TextView visitTitle = view.findViewById(
                org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle);
        if (visitTitle != null) {
            visitTitle.setText(R.string.visits_history);
        }
        return view;
    }

    @Override
    public void displayLoadingState(boolean state) {
        if (progressBar != null) {
            progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
        }
    }
}
