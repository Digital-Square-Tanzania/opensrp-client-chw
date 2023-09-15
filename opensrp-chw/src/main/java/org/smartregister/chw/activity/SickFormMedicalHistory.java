package org.smartregister.chw.activity;

import static org.smartregister.chw.anc.util.Constants.ANC_MEMBER_OBJECTS.MEMBER_PROFILE_OBJECT;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.chw.R;
import org.smartregister.chw.adapter.SickFormMedicalHistoryAdapter;
import org.smartregister.chw.anc.activity.BaseAncUpcomingServicesActivity;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.contract.SickFormMedicalHistoryContract;
import org.smartregister.chw.core.utils.CustomDividerItemDecoration;
import org.smartregister.chw.domain.FormDetails;
import org.smartregister.chw.fragment.FormHistoryDialogFragment;
import org.smartregister.chw.interactor.SickFormMedicalHistoryInteractor;
import org.smartregister.chw.presenter.SickFormMedicalHistoryPresenter;
import org.smartregister.view.activity.SecuredActivity;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class SickFormMedicalHistory extends SecuredActivity implements SickFormMedicalHistoryContract.View {

    protected MemberObject memberObject;
    protected SickFormMedicalHistoryContract.Presenter presenter;
    protected List<Visit> serviceList = new ArrayList<>();
    private TextView tvTitle;
    private ProgressBar progressBar;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView recyclerView;


    public static void startMe(Activity activity, MemberObject memberObject) {
        Intent intent = new Intent(activity, BaseAncUpcomingServicesActivity.class);
        intent.putExtra(MEMBER_PROFILE_OBJECT, memberObject);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sick_visit_activity);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            memberObject = (MemberObject) getIntent().getSerializableExtra(MEMBER_PROFILE_OBJECT);
        }
        setUpActionBar();
        setUpView();
        initializePresenter();
    }

    @Override
    protected void onCreation() {
        Timber.v("Empty onCreation");
    }

    @Override
    protected void onResumption() {
        Timber.v("Empty onResumption");
    }

    private void setUpActionBar() {
        Toolbar toolbar = findViewById(R.id.collapsing_toolbar);
        tvTitle = toolbar.findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            final Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);
            upArrow.setColorFilter(getResources().getColor(R.color.text_blue), PorterDuff.Mode.SRC_ATOP);
            actionBar.setHomeAsUpIndicator(upArrow);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    public void setUpView() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(false);
        progressBar = findViewById(R.id.progressBarUpcomingServices);

        tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(getString(R.string.back_to, memberObject.getFullName()));


        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new SickFormMedicalHistoryAdapter(serviceList, this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new CustomDividerItemDecoration(ContextCompat.getDrawable(getContext(), org.smartregister.chw.core.R.drawable.divider)));

    }

    @Override
    public void initializePresenter() {
        presenter = new SickFormMedicalHistoryPresenter(memberObject, new SickFormMedicalHistoryInteractor(), this);
    }

    @Override
    public SickFormMedicalHistoryContract.Presenter getPresenter() {
        return presenter;
    }

    @Override
    public void displayLoadingState(boolean state) {
        progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    @Override
    public void refreshVisits(List<Visit> visits) {
        this.serviceList.clear();
        this.serviceList.addAll(visits);

        mAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onAdapterInteraction(Visit visit) {
        FormDetails formDetails = new FormDetails();
        formDetails.setTitle(getString(R.string.sick_visit));
        formDetails.setBaseEntityID(visit.getBaseEntityId());
        formDetails.setEventDate(visit.getDate().getTime());
        formDetails.setEventType(org.smartregister.chw.util.Constants.EventType.SICK_CHILD);
        formDetails.setFormName(org.smartregister.chw.util.Constants.JSON_FORM.getChildSickForm());

        FormHistoryDialogFragment dialogFragment = FormHistoryDialogFragment.getInstance(formDetails);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        dialogFragment.show(ft, FormHistoryDialogFragment.DIALOG_TAG);
    }
}
