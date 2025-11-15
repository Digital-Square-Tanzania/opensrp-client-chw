package org.smartregister.chw.fragment;

import static android.view.View.GONE;
import static com.vijay.jsonwizard.constants.JsonFormConstants.COUNT;
import static org.smartregister.util.JsonFormUtils.ENTITY_ID;
import static org.smartregister.util.JsonFormUtils.generateRandomUUIDString;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.gson.Gson;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.adapter.AypOutSchoolGroupsRegisterAdapter;
import org.smartregister.chw.ayp.AypLibrary;
import org.smartregister.chw.ayp.domain.Visit;
import org.smartregister.chw.ayp.model.BaseAypRegisterFragmentModel;
import org.smartregister.chw.ayp.util.AypVisitsUtil;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.fragment.CoreAypRegisterFragment;
import org.smartregister.chw.interactor.AypOutSchoolGroupsRegisterInteractor;
import org.smartregister.chw.presenter.AypOutSchoolGroupRegisterFragmentPresenter;
import org.smartregister.chw.provider.SbccRegisterProvider;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.configurableviews.model.View;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;
import org.smartregister.view.activity.BaseRegisterActivity;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import timber.log.Timber;

public class AypOutSchoolGroupsRegisterFragment extends CoreAypRegisterFragment {
    protected Toolbar toolbar;
    protected LinearLayout emptyViewLayout;
    private android.view.View view;
    private AypOutSchoolGroupsRegisterAdapter adapter;
    private String currentGroupTypeFilter = null; // null = all; else 'age_band' | 'classes'

    @Override
    public void initializeAdapter(Set<View> visibleColumns) {
        SbccRegisterProvider provider = new SbccRegisterProvider(getActivity(), paginationViewHandler, registerActionHandler, visibleColumns);
        clientAdapter = new RecyclerViewPaginatedAdapter(null, provider, null);
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
            titleView.setText(getString(R.string.ayp_out_school_groups_register_title));
            titleView.setPadding(0, titleView.getTop(), titleView.getPaddingRight(), titleView.getPaddingBottom());
        }

        android.view.View searchBarLayout = view.findViewById(org.smartregister.hivst.R.id.search_bar_layout);
        searchBarLayout.setVisibility(GONE);

        android.view.View topLeftLayout = view.findViewById(org.smartregister.hivst.R.id.top_left_layout);
        topLeftLayout.setVisibility(GONE);

        android.view.View topRightLayout = view.findViewById(org.smartregister.hivst.R.id.top_right_layout);
        topRightLayout.setVisibility(android.view.View.VISIBLE);
        topRightLayout.setOnClickListener(v -> showGroupTypeFilterDialog());

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
        // Reuse AYP base presenter/model since this is a custom list not backed by the common register query
        presenter = new AypOutSchoolGroupRegisterFragmentPresenter(this, new BaseAypRegisterFragmentModel(), viewConfigurationIdentifier);
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
        AypOutSchoolGroupsRegisterInteractor interactor = new AypOutSchoolGroupsRegisterInteractor();
        if (currentGroupTypeFilter == null || currentGroupTypeFilter.isEmpty()) {
            interactor.fetchItems(items -> {
                if (items != null && !items.isEmpty()) {
                    adapter = new AypOutSchoolGroupsRegisterAdapter(items, requireActivity());
                    clientsView.setAdapter(adapter);
                    showEmptyState();
                } else {
                    clientsView.setAdapter(null);
                    showEmptyState();
                }
            });
        } else {
            interactor.fetchItemsByType(currentGroupTypeFilter, items -> {
                if (items != null && !items.isEmpty()) {
                    adapter = new AypOutSchoolGroupsRegisterAdapter(items, requireActivity());
                    clientsView.setAdapter(adapter);
                    showEmptyState();
                } else {
                    clientsView.setAdapter(null);
                    showEmptyState();
                }
            });
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
        // Implement when group profile is available
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
                try {
                    JSONObject form = (new FormUtils()).getFormJsonFromRepositoryOrAssets(requireActivity(), "ayp_out_school_group_creation");
                    if (form != null) {
                        String randomId = generateRandomUUIDString();
                        form.put(ENTITY_ID, randomId);
                        requireActivity().startActivityForResult(getStartEditFormIntent(form, requireActivity().getString(R.string.ayp_out_school_groups_register_title), requireActivity()), org.smartregister.family.util.JsonFormUtils.REQUEST_CODE_GET_JSON);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == org.smartregister.family.util.JsonFormUtils.REQUEST_CODE_GET_JSON) {
            try {
                String json = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
                if (json == null) {
                    json = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                }
                if (json == null) return;

                JSONObject form = new JSONObject(json);
                String encounterType = form.optString("encounter_type", "");
                if (!"group_out_details".equalsIgnoreCase(encounterType)) return;

                // Build Event from the JSON form
                AllSharedPreferences prefs = Utils.getAllSharedPreferences();
                Event baseEvent = org.smartregister.chw.ayp.util.JsonFormUtils.processJsonForm(prefs, json, Constants.TABLES.AYP_OUT_SCHOOL_GROUP_DETAILS);
                if (baseEvent != null) {
                    // Convert Event to a Visit for AYP processing
                    Visit visit = new Visit();
                    visit.setVisitId(UUID.randomUUID().toString());
                    visit.setVisitType(encounterType);
                    visit.setBaseEntityId(baseEvent.getBaseEntityId());
                    Date now = new Date();
                    visit.setDate(now);
                    visit.setUpdatedAt(now);
                    visit.setProcessed(false);
                    visit.setPreProcessedJson(new Gson().toJson(baseEvent));

                    AypLibrary.getInstance().visitRepository().addVisit(visit);
                    // Immediately process this visit into an Event and trigger client processing
                    AypVisitsUtil.manualProcessVisit(visit);
                    setUpAdapter();
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    private void showGroupTypeFilterDialog() {
        try {
            final String[] display = new String[]{
                    getString(R.string.filter_all),
                    getString(R.string.filter_group_type_age_band),
                    getString(R.string.filter_group_type_classes)
            };
            final String[] values = new String[]{
                    "",
                    "age_band",
                    "classes"
            };
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.filter_by_group_type)
                    .setItems(display, (dialog, which) -> {
                        String val = values[which];
                        currentGroupTypeFilter = (val == null || val.isEmpty()) ? null : val;
                        setUpAdapter();
                    })
                    .show();
        } catch (Exception e) {
            Timber.e(e);
        }
    }
}
