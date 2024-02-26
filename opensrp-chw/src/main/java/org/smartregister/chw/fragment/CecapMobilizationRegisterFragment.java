package org.smartregister.chw.fragment;

import static android.view.View.GONE;
import static com.vijay.jsonwizard.constants.JsonFormConstants.COUNT;
import static com.vijay.jsonwizard.utils.FormUtils.fields;
import static com.vijay.jsonwizard.utils.FormUtils.getFieldJSONObject;
import static org.smartregister.util.JsonFormUtils.ENTITY_ID;
import static org.smartregister.util.JsonFormUtils.STEP1;
import static org.smartregister.util.JsonFormUtils.VALUE;
import static org.smartregister.util.JsonFormUtils.generateRandomUUIDString;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.adapter.CecapMobilizationRegisterAdapter;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.cecap.dao.CecapDao;
import org.smartregister.chw.cecap.fragment.BaseCecapRegisterFragment;
import org.smartregister.chw.cecap.model.CecapMobilizationSessionModel;
import org.smartregister.chw.cecap.util.Constants;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.model.CecapMobilizationSessionRegisterFragmentModel;
import org.smartregister.chw.presenter.CecapMobilizationRegisterFragmentPresenter;
import org.smartregister.chw.provider.SbccRegisterProvider;
import org.smartregister.configurableviews.model.View;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.view.activity.BaseRegisterActivity;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class CecapMobilizationRegisterFragment extends BaseCecapRegisterFragment {
    protected Toolbar toolbar;

    protected LinearLayout emptyViewLayout;

    private android.view.View view;

    private CecapMobilizationRegisterAdapter adapter;

    @Override
    public void initializeAdapter(Set<View> visibleColumns) {
        SbccRegisterProvider sbcMobilizationRegisterProvider = new SbccRegisterProvider(getActivity(), paginationViewHandler, registerActionHandler, visibleColumns);
        clientAdapter = new RecyclerViewPaginatedAdapter(null, sbcMobilizationRegisterProvider, null);
        clientAdapter.setTotalcount(0);
        clientAdapter.setCurrentlimit(20);
        setUpAdapter();
    }

    @Override
    public void setupViews(android.view.View view) {
        initializePresenter();
        super.setupViews(view);
        this.view = view;

        emptyViewLayout = view.findViewById(org.smartregister.hivst.R.id.empty_view_ll);
        emptyViewLayout.setVisibility(GONE);
        toolbar = view.findViewById(org.smartregister.R.id.register_toolbar);
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.setContentInsetsRelative(0, 0);
        toolbar.setContentInsetStartWithNavigation(0);


        android.view.View navbarContainer = view.findViewById(org.smartregister.hivst.R.id.register_nav_bar_container);
        navbarContainer.setFocusable(false);

        CustomFontTextView titleView = view.findViewById(org.smartregister.hivst.R.id.txt_title_label);
        if (titleView != null) {
            titleView.setText(getString(R.string.cecap_mobilization_sessions_title));
            titleView.setPadding(0, titleView.getTop(), titleView.getPaddingRight(), titleView.getPaddingBottom());
        }

        android.view.View searchBarLayout = view.findViewById(org.smartregister.hivst.R.id.search_bar_layout);
        searchBarLayout.setVisibility(GONE);

        android.view.View topLeftLayout = view.findViewById(org.smartregister.hivst.R.id.top_left_layout);
        topLeftLayout.setVisibility(GONE);

        android.view.View topRightLayout = view.findViewById(org.smartregister.hivst.R.id.top_right_layout);
        topRightLayout.setVisibility(android.view.View.VISIBLE);

        android.view.View sortFilterBarLayout = view.findViewById(org.smartregister.hivst.R.id.register_sort_filter_bar_layout);
        sortFilterBarLayout.setVisibility(GONE);

        android.view.View filterSortLayout = view.findViewById(org.smartregister.hivst.R.id.filter_sort_layout);
        filterSortLayout.setVisibility(GONE);

        android.view.View dueOnlyLayout = view.findViewById(org.smartregister.hivst.R.id.due_only_layout);
        dueOnlyLayout.setVisibility(GONE);
        dueOnlyLayout.setOnClickListener(registerActionHandler);
        if (getSearchView() != null) {
            getSearchView().setVisibility(GONE);
        }
    }


    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        String viewConfigurationIdentifier = null;
        try {
            viewConfigurationIdentifier = ((BaseRegisterActivity) getActivity()).getViewIdentifiers().get(0);
        } catch (NullPointerException e) {
            Timber.e(e);
        }
        presenter = new CecapMobilizationRegisterFragmentPresenter(this, new CecapMobilizationSessionRegisterFragmentModel(), viewConfigurationIdentifier);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (clientsView.getAdapter() != null) {
            clientsView.getAdapter().notifyDataSetChanged();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        Toolbar toolbar = view.findViewById(org.smartregister.R.id.register_toolbar);
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.setContentInsetsRelative(0, 0);
        toolbar.setContentInsetStartWithNavigation(0);
        NavigationMenu.getInstance(getActivity(), null, toolbar);

        try {
            new Handler(Looper.getMainLooper()).postDelayed(this::setUpAdapter, 2000);
        } catch (Exception e) {
            Timber.e(e);
        }
    }


    protected void setUpAdapter() {
        List<CecapMobilizationSessionModel> cecapMobilizationSessionModels = CecapDao.getCecapMobilizationSessions();
        if (cecapMobilizationSessionModels != null && !cecapMobilizationSessionModels.isEmpty()) {
            showEmptyState();
            adapter = new CecapMobilizationRegisterAdapter(cecapMobilizationSessionModels, requireActivity());
            clientsView.setAdapter(adapter);
        } else {
            showEmptyState();
        }
    }

    protected void showEmptyState() {
        if (emptyViewLayout != null) {
            if (adapter != null && adapter.getItemCount() >= 1) {
                emptyViewLayout.setVisibility(GONE);
            } else {
                emptyViewLayout.setVisibility(android.view.View.VISIBLE);
            }
        }
    }

    @Override
    protected int getLayout() {
        return org.smartregister.hivst.R.layout.fragment_mobilization_register;
    }

    @Override
    public void countExecute() {
        Cursor c = null;
        try {

            String query = "select count(*) from " + presenter().getMainTable() + " where " + presenter().getMainCondition();

            if (StringUtils.isNotBlank(filters)) {
                query = query + " and ( " + filters + " ) ";
            }


            c = commonRepository().rawCustomQueryForAdapter(query);
            c.moveToFirst();
            clientAdapter.setTotalcount(c.getInt(0));
            Timber.v("total count here %s", clientAdapter.getTotalcount());

            clientAdapter.setCurrentlimit(20);
            clientAdapter.setCurrentoffset(0);

        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }


    @Override
    protected void openProfile(String baseEntityId) {
        //implement when needed
    }


    @Override
    protected void refreshSyncProgressSpinner() {
        if (syncProgressBar != null) {
            syncProgressBar.setVisibility(GONE);
        }
        if (syncButton != null) {
            syncButton.setVisibility(android.view.View.VISIBLE);
            syncButton.setPadding(0, 0, 10, 0);
            syncButton.setImageDrawable(context().getDrawable(R.drawable.ic_add_white_24));
            syncButton.setOnClickListener(view -> {
                JSONObject form;
                try {
                    form = (new FormUtils()).getFormJsonFromRepositoryOrAssets(requireActivity(), Constants.FORMS.CECAP_MOBILIZATION_SESSION);
                    if (form != null) {
                        String randomId = generateRandomUUIDString();
                        form.put(ENTITY_ID, randomId);

                        JSONObject chwName = getFieldJSONObject(fields(form, STEP1), "chw_name");
                        AllSharedPreferences preferences = ChwApplication.getInstance().getContext().allSharedPreferences();
                        chwName.put(VALUE, preferences.getANMPreferredName(preferences.fetchRegisteredANM()));
                        requireActivity().startActivityForResult(getStartEditFormIntent(form, requireActivity().getString(R.string.cecap_mobilization_sessions_title), requireActivity()), JsonFormUtils.REQUEST_CODE_GET_JSON);
                    }
                } catch (JSONException e) {
                    Timber.e(e);
                }
            });
        }
    }


    public Intent getStartEditFormIntent(JSONObject jsonForm, String title, Context context) {
        Intent intent = org.smartregister.chw.core.utils.FormUtils.getStartFormActivity(jsonForm, null, context);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());

        Form form = new Form();
        form.setDatePickerDisplayFormat("dd-MM-yyyy");
        form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        form.setName(title);
        form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);

        try {
            form.setWizard(jsonForm.getInt(COUNT) > 1);
        } catch (JSONException e) {
            Timber.e(e);
            form.setWizard(false);
        }
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        return intent;
    }
}
