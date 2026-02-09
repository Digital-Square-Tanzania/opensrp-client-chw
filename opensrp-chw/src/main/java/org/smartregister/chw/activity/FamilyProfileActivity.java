package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Period;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.activity.BaseAncMemberProfileActivity;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.activity.CoreAboveFiveChildProfileActivity;
import org.smartregister.chw.core.activity.CoreChildProfileActivity;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CoreFamilyProfileMenuActivity;
import org.smartregister.chw.core.activity.CoreFamilyRemoveMemberActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.ChwChildDao;
import org.smartregister.chw.fragment.FamilyProfileActivityFragment;
import org.smartregister.chw.fragment.FamilyProfileDueFragment;
import org.smartregister.chw.fragment.FamilyProfileMemberFragment;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.hps.dao.HpsDao;
import org.smartregister.chw.model.FamilyProfileModel;
import org.smartregister.chw.pnc.activity.BasePncMemberProfileActivity;
import org.smartregister.chw.presenter.FamilyProfilePresenter;
import org.smartregister.chw.tb.dao.TbDao;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.adapter.ViewPagerAdapter;
import org.smartregister.family.fragment.BaseFamilyProfileDueFragment;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;
import org.smartregister.view.fragment.BaseRegisterFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import timber.log.Timber;

public class FamilyProfileActivity extends CoreFamilyProfileActivity {
    private static final int MENU_CHANGE_INDEPENDENT_CLIENT_FAMILY = Menu.FIRST + 9601;
    private static final String ENTITY_TYPE_INDEPENDENT_CLIENT = "ec_independent_client";

    private BaseFamilyProfileDueFragment profileDueFragment;
    private TextView tvEventDate;
    private TextView tvInterpunct;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && profileDueFragment != null) {
            profileDueFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        tvEventDate = findViewById(R.id.textview_event_date);
        tvInterpunct = findViewById(R.id.interpunct);
    }

    @Override
    public void setEventDate(String eventDate) {
        if (ChwApplication.getApplicationFlavor().hasEventDateOnFamilyProfile()) {
            tvEventDate.setVisibility(View.VISIBLE);
            tvInterpunct.setVisibility(View.VISIBLE);
            tvEventDate.setText(String.format(this.getString(R.string.created), eventDate));
        }
    }

    @Override
    protected void refreshPresenter() {
        this.presenter = new FamilyProfilePresenter(this, new FamilyProfileModel(familyName),
                familyBaseEntityId, familyHead, primaryCaregiver, familyName);
    }

    @Override
    protected void refreshList(Fragment fragment) {
        if (fragment instanceof BaseRegisterFragment) {
            if (fragment instanceof FamilyProfileMemberFragment) {
                FamilyProfileMemberFragment familyProfileMemberFragment = ((FamilyProfileMemberFragment) fragment);
                if (familyProfileMemberFragment.presenter() != null) {
                    familyProfileMemberFragment.refreshListView();
                }
            } else if (fragment instanceof FamilyProfileDueFragment) {
                FamilyProfileDueFragment familyProfileDueFragment = ((FamilyProfileDueFragment) fragment);
                if (familyProfileDueFragment.presenter() != null) {
                    familyProfileDueFragment.refreshListView();
                }
            } else if (fragment instanceof FamilyProfileActivityFragment) {
                FamilyProfileActivityFragment familyProfileActivityFragment = ((FamilyProfileActivityFragment) fragment);
                if (familyProfileActivityFragment.presenter() != null) {
                    familyProfileActivityFragment.refreshListView();
                }
            }
        }
    }

    @Override
    protected Class<? extends CoreFamilyRemoveMemberActivity> getFamilyRemoveMemberClass() {
        return FamilyRemoveMemberActivity.class;
    }

    @Override
    protected Class<? extends CoreFamilyProfileMenuActivity> getFamilyProfileMenuClass() {
        return FamilyProfileMenuActivity.class;
    }

    @Override
    protected void initializePresenter() {
        super.initializePresenter();
        presenter = new FamilyProfilePresenter(this, new FamilyProfileModel(familyName), familyBaseEntityId, familyHead, primaryCaregiver, familyName);
    }

    public FamilyProfilePresenter getFamilyProfilePresenter() {
        return (FamilyProfilePresenter) presenter;
    }

    @Override
    protected ViewPager setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        FamilyProfileMemberFragment profileMemberFragment = (FamilyProfileMemberFragment) FamilyProfileMemberFragment.newInstance(this.getIntent().getExtras());
        profileDueFragment = FamilyProfileDueFragment.newInstance(this.getIntent().getExtras());
        FamilyProfileActivityFragment profileActivityFragment = (FamilyProfileActivityFragment) FamilyProfileActivityFragment.newInstance(this.getIntent().getExtras());

        adapter.addFragment(profileMemberFragment, this.getString(org.smartregister.family.R.string.member).toUpperCase());
        adapter.addFragment(profileDueFragment, this.getString(org.smartregister.family.R.string.due).toUpperCase());
        adapter.addFragment(profileActivityFragment, this.getString(org.smartregister.family.R.string.activity).toUpperCase());

        viewPager.setAdapter(adapter);

        if (getIntent().getBooleanExtra(CoreConstants.INTENT_KEY.SERVICE_DUE, false) || getIntent().getBooleanExtra(Constants.INTENT_KEY.GO_TO_DUE_PAGE, false)) {
            viewPager.setCurrentItem(1);
        }

        return viewPager;
    }

    @Override
    protected Class<?> getFamilyOtherMemberProfileActivityClass() {
        return FamilyOtherMemberProfileActivity.class;
    }

    @Override
    protected Class<? extends CoreAboveFiveChildProfileActivity> getAboveFiveChildProfileActivityClass() {
        return AboveFiveChildProfileActivity.class;
    }

    @Override
    protected Class<? extends CoreChildProfileActivity> getChildProfileActivityClass() {
        return ChildProfileActivity.class;
    }

    @Override
    protected Class<? extends BaseAncMemberProfileActivity> getAncMemberProfileActivityClass() {
        return AncMemberProfileActivity.class;
    }

    @Override
    public void goToAncProfileActivity(CommonPersonObjectClient patient, Bundle bundle) {
        AncMemberProfileActivity.startMe(this, patient.getCaseId());
    }

    @Override
    protected Class<? extends BasePncMemberProfileActivity> getPncMemberProfileActivityClass() {
        return PncMemberProfileActivity.class;
    }

    @Override
    protected void goToFpProfile(String baseEntityId, Activity activity) {
        FPMemberProfileActivity.startFpMemberProfileActivity(activity, baseEntityId);
    }

    @Override
    protected void goToHivProfile(String baseEntityId, Activity activity) {
        HivProfileActivity.startHivProfileActivity(this, Objects.requireNonNull(HivDao.getMember(baseEntityId)));
    }

    @Override
    protected void goToTbProfile(String baseEntityId, Activity activity) {
        TbProfileActivity.startTbProfileActivity(this, Objects.requireNonNull(TbDao.getMember(baseEntityId)));
    }


    @Override
    protected boolean isAncMember(String baseEntityId) {
        return ChwApplication.getApplicationFlavor().hasANC() && getFamilyProfilePresenter().isAncMember(baseEntityId);
    }

    @Override
    protected void startHpsHouseholdEnrollment(String s) {
        HpsRegisterActivity.startRegistration(FamilyProfileActivity.this, s, org.smartregister.chw.hps.util.Constants.FORMS.HPS_HOUSEHOLD_ENROLLMENT, 1);
    }

    @Override
    protected HashMap<String, String> getAncFamilyHeadNameAndPhone(String baseEntityId) {
        return getFamilyProfilePresenter().getAncFamilyHeadNameAndPhone(baseEntityId);
    }

    @Override
    protected CommonPersonObject getAncCommonPersonObject(String baseEntityId) {
        return getFamilyProfilePresenter().getAncCommonPersonObject(baseEntityId);
    }

    @Override
    protected boolean isPncMember(String baseEntityId) {
        return ChwApplication.getApplicationFlavor().hasPNC() && getFamilyProfilePresenter().isPncMember(baseEntityId);
    }

    @Override
    protected CommonPersonObject getPncCommonPersonObject(String baseEntityId) {
        return getFamilyProfilePresenter().getPncCommonPersonObject(baseEntityId);
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    private Intent getDefaultChildrenIntent(int age) {
        if (age < 5) {
            return new Intent(this, getChildProfileActivityClass());
        } else {
            return new Intent(this, getAboveFiveChildProfileActivityClass());
        }
    }

    private Intent getIntentForChildrenUnderFiveAndGirlsAgeNineToEleven(int age, String gender) {
        if (age < 5 || (gender.equalsIgnoreCase("Female") && (age >= 9 && age < 11))) {
            return new Intent(this, getChildProfileActivityClass());
        } else {
            return new Intent(this, getAboveFiveChildProfileActivityClass());
        }
    }

    private Intent getChildIntent(CommonPersonObjectClient patient) {
        String dobString = Utils.getValue(patient.getColumnmaps(), DBConstants.KEY.DOB, false);

        int age = (int) Math.floor(Days.daysBetween(new DateTime(dobString).toLocalDate(), new DateTime().toLocalDate()).getDays() / 365.4);

        String gender = ChwChildDao.getChildGender(patient.entityId());
        if (ChwApplication.getApplicationFlavor().showChildrenUnderFiveAndGirlsAgeNineToEleven()) {
            return getIntentForChildrenUnderFiveAndGirlsAgeNineToEleven(age, gender);
        } else {
            return getDefaultChildrenIntent(age);
        }
    }

    @Override
    public void goToChildProfileActivity(CommonPersonObjectClient patient, Bundle bundle) {
        Intent intent = getChildIntent(patient);

        if (bundle != null) {
            intent.putExtras(bundle);
        }
        MemberObject memberObject = new MemberObject(patient);
        memberObject.setFamilyName(familyName);
        passToolbarTitle(this, intent);
        intent.putExtra(Constants.INTENT_KEY.BASE_ENTITY_ID, patient.getCaseId());
        intent.putExtra(org.smartregister.chw.anc.util.Constants.ANC_MEMBER_OBJECTS.MEMBER_PROFILE_OBJECT, memberObject);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (ChwApplication.getApplicationFlavor().hasHps()) {
            menu.findItem(R.id.action_hps_enrollment).setVisible(!HpsDao.isHouseholdRegisteredForHps(familyHead));
        }
        menu.add(Menu.NONE, MENU_CHANGE_INDEPENDENT_CLIENT_FAMILY, Menu.NONE, R.string.action_change_client_family)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_CHANGE_INDEPENDENT_CLIENT_FAMILY) {
            showIndependentClientsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showIndependentClientsDialog() {
        List<IndependentClientOption> independentClients = loadIndependentClients();
        if (independentClients.isEmpty()) {
            Toast.makeText(this, getString(R.string.change_family_no_clients_available), Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tbleprosy_contact_selector, null, false);
        EditText searchInput = dialogView.findViewById(R.id.search_input);
        RecyclerView contactsRecycler = dialogView.findViewById(R.id.contact_list);
        TextView emptyView = dialogView.findViewById(R.id.empty_view);

        searchInput.setHint(R.string.change_family_search_hint);
        emptyView.setText(R.string.change_family_no_clients);

        IndependentClientSelectionAdapter adapter = new IndependentClientSelectionAdapter(independentClients);
        contactsRecycler.setLayoutManager(new LinearLayoutManager(this));
        contactsRecycler.setAdapter(adapter);
        updateEmptyViewVisibility(adapter, emptyView);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.change_family_dialog_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        adapter.setOnClientSelectedListener(client -> {
            dialog.dismiss();
            reassignClientFamily(client);
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s == null ? "" : s.toString());
                updateEmptyViewVisibility(adapter, emptyView);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });

        dialog.show();
    }

    private void updateEmptyViewVisibility(IndependentClientSelectionAdapter adapter, TextView emptyView) {
        if (emptyView != null) {
            emptyView.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void reassignClientFamily(IndependentClientOption client) {
        if (client == null || StringUtils.isBlank(client.getBaseEntityId()) || StringUtils.isBlank(familyBaseEntityId)) {
            Toast.makeText(this, getString(R.string.change_family_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            boolean updated = org.smartregister.chw.util.Utils.updateClientFamilyRelationship(
                    client.getBaseEntityId(),
                    familyBaseEntityId
            );

            runOnUiThread(() -> {
                if (updated) {
                    Toast.makeText(
                            this,
                            getString(R.string.change_family_success, client.getDisplayName()),
                            Toast.LENGTH_SHORT
                    ).show();
                    refreshMemberList(org.smartregister.domain.FetchStatus.fetched);
                } else {
                    Toast.makeText(this, getString(R.string.change_family_failed), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private List<IndependentClientOption> loadIndependentClients() {
        Cursor cursor = null;
        List<IndependentClientOption> independentClients = new ArrayList<>();
        try {
            SQLiteDatabase readableDatabase = ChwApplication.getInstance().getRepository().getReadableDatabase();
            if (readableDatabase == null) {
                return Collections.emptyList();
            }

            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT m.base_entity_id, m.first_name, m.middle_name, m.last_name, m.dob, m.unique_id ");
            sqlBuilder.append("FROM ec_family_member m ");
            sqlBuilder.append("INNER JOIN ec_family f ON f.base_entity_id = m.relational_id ");
            sqlBuilder.append("WHERE m.is_closed = 0 ");
            sqlBuilder.append("AND m.date_removed IS NULL ");
            sqlBuilder.append("AND m.dod IS NULL ");
            sqlBuilder.append("AND f.entity_type = ? ");

            List<String> args = new ArrayList<>();
            args.add(ENTITY_TYPE_INDEPENDENT_CLIENT);

            if (StringUtils.isNotBlank(familyBaseEntityId)) {
                sqlBuilder.append("AND m.relational_id <> ? ");
                args.add(familyBaseEntityId);
            }

            sqlBuilder.append("ORDER BY m.first_name, m.middle_name, m.last_name");

            cursor = readableDatabase.rawQuery(sqlBuilder.toString(), args.toArray(new String[0]));
            while (cursor != null && cursor.moveToNext()) {
                String baseEntityId = cursor.getString(0);
                if (StringUtils.isBlank(baseEntityId)) {
                    continue;
                }

                String firstName = cursor.getString(1);
                String middleName = cursor.getString(2);
                String lastName = cursor.getString(3);
                String dob = cursor.getString(4);
                String uniqueId = cursor.getString(5);

                String displayName = buildDisplayName(firstName, middleName, lastName, baseEntityId);
                String ageDisplay = buildAgeDisplay(dob);
                independentClients.add(new IndependentClientOption(baseEntityId, displayName, ageDisplay, uniqueId));
            }
        } catch (Exception e) {
            Timber.e(e, "Unable to load independent clients for family reassignment");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return independentClients;
    }

    private String buildDisplayName(String firstName, String middleName, String lastName, String fallback) {
        String clientName = org.smartregister.chw.util.Utils.getClientName(
                StringUtils.defaultString(firstName),
                StringUtils.defaultString(middleName),
                StringUtils.defaultString(lastName)
        );
        return StringUtils.isNotBlank(clientName) ? clientName : fallback;
    }

    private String buildAgeDisplay(String dob) {
        if (StringUtils.isBlank(dob)) {
            return "";
        }
        try {
            DateTime birthDate = new DateTime(dob);
            int age = Math.max(0, new Period(birthDate, DateTime.now()).getYears());
            return String.format(Locale.getDefault(), "%d", age);
        } catch (Exception e) {
            Timber.w(e, "Unable to parse date of birth while loading independent clients");
            return "";
        }
    }

    private static class IndependentClientOption {
        private final String baseEntityId;
        private final String displayName;
        private final String ageDisplay;
        private final String uniqueId;

        private IndependentClientOption(String baseEntityId, String displayName, String ageDisplay, String uniqueId) {
            this.baseEntityId = baseEntityId;
            this.displayName = displayName;
            this.ageDisplay = ageDisplay;
            this.uniqueId = uniqueId;
        }

        private String getBaseEntityId() {
            return baseEntityId;
        }

        private String getDisplayName() {
            return displayName;
        }

        private String getDetails() {
            StringBuilder detailsBuilder = new StringBuilder();
            if (StringUtils.isNotBlank(uniqueId)) {
                detailsBuilder.append(uniqueId);
            }
            if (StringUtils.isNotBlank(ageDisplay)) {
                if (detailsBuilder.length() > 0) {
                    detailsBuilder.append(" · ");
                }
                detailsBuilder.append(ageDisplay);
            }
            return detailsBuilder.toString();
        }

        private boolean matches(String query) {
            if (StringUtils.isBlank(query)) {
                return true;
            }
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            return (displayName != null && displayName.toLowerCase(Locale.getDefault()).contains(lowerQuery))
                    || (uniqueId != null && uniqueId.toLowerCase(Locale.getDefault()).contains(lowerQuery))
                    || (baseEntityId != null && baseEntityId.toLowerCase(Locale.getDefault()).contains(lowerQuery));
        }
    }

    private interface OnIndependentClientSelectedListener {
        void onClientSelected(IndependentClientOption client);
    }

    private static class IndependentClientViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final TextView detailsView;

        IndependentClientViewHolder(@NonNull View itemView) {
            super(itemView);
            this.nameView = itemView.findViewById(R.id.contact_name);
            this.detailsView = itemView.findViewById(R.id.contact_details);
        }
    }

    private class IndependentClientSelectionAdapter extends RecyclerView.Adapter<IndependentClientViewHolder> {
        private final List<IndependentClientOption> allClients;
        private final List<IndependentClientOption> filteredClients = new ArrayList<>();
        private OnIndependentClientSelectedListener onClientSelectedListener;

        IndependentClientSelectionAdapter(List<IndependentClientOption> clients) {
            this.allClients = clients == null ? Collections.emptyList() : clients;
            this.filteredClients.addAll(this.allClients);
        }

        void setOnClientSelectedListener(OnIndependentClientSelectedListener listener) {
            this.onClientSelectedListener = listener;
        }

        void filter(String query) {
            filteredClients.clear();
            for (IndependentClientOption client : allClients) {
                if (client.matches(query)) {
                    filteredClients.add(client);
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public IndependentClientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tbleprosy_contact_row, parent, false);
            return new IndependentClientViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull IndependentClientViewHolder holder, int position) {
            IndependentClientOption client = filteredClients.get(position);
            holder.nameView.setText(client.getDisplayName());
            String details = client.getDetails();
            if (StringUtils.isNotBlank(details)) {
                holder.detailsView.setText(details);
                holder.detailsView.setVisibility(View.VISIBLE);
            } else {
                holder.detailsView.setText("");
                holder.detailsView.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (onClientSelectedListener != null) {
                    onClientSelectedListener.onClientSelected(client);
                }
            });
        }

        @Override
        public int getItemCount() {
            return filteredClients.size();
        }
    }
}
