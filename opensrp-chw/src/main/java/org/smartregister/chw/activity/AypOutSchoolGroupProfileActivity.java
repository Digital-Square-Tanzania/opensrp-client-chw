package org.smartregister.chw.activity;

import static org.smartregister.chw.core.utils.CoreJsonFormUtils.toList;
import static org.smartregister.util.JsonFormUtils.ENTITY_ID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.ayp.AypLibrary;
import org.smartregister.chw.ayp.activity.BaseAypGroupProfileActivity;
import org.smartregister.chw.ayp.activity.BaseAypOutGroupProfileActivity;
import org.smartregister.chw.ayp.dao.AypDao;
import org.smartregister.chw.ayp.domain.GroupObject;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.domain.Visit;
import org.smartregister.chw.ayp.util.AypJsonFormUtils;
import org.smartregister.chw.ayp.util.AypVisitsUtil;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.ayp.util.JsonFormUtils;
import org.smartregister.chw.ayp.util.NCUtils;
import org.smartregister.chw.domain.AypInSchoolGroupDetails;
import org.smartregister.chw.repository.AypOutSchoolGroupDetailsRepository;
import org.smartregister.chw.repository.AypOutSchoolGroupMembersRepository;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import timber.log.Timber;

public class AypOutSchoolGroupProfileActivity extends BaseAypOutGroupProfileActivity {

    public static void start(Context context, String groupId, String groupName) {
        Intent intent = new Intent(context, AypOutSchoolGroupProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.GROUP_ID, groupId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.GROUP_NAME, groupName);
        context.startActivity(intent);
    }

    @Override
    public void openGroupDetailsForm() {
        try {
            String groupId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.GROUP_ID);
            String groupName = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.GROUP_NAME);
            AypOutSchoolGroupVisitActivity.startAypOutSchoolGroupVisitActivity(this, UUID.randomUUID().toString(), false, groupId, groupName);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void renderMembers(List<MemberObject> members) {
        adapter.setItems(members);

        int count = (members != null) ? members.size() : 0;

        if (count >= 5) {
            // Normal display when group has 5 or more members
            tvGroupMemberCount.setText(getString(R.string.group_members_count, count));
            tvGroupMemberCount.setTextColor(Color.BLACK);
            tvGroupMemberCount.setBackgroundColor(Color.TRANSPARENT);

            if (btnProvideDetails != null) {
                btnProvideDetails.setVisibility(View.VISIBLE);
            }

        } else {
            // Warning style for few members
            tvGroupMemberCount.setTextColor(Color.WHITE);
            tvGroupMemberCount.setBackgroundColor(Color.RED);
            tvGroupMemberCount.setText(getString(R.string.group_few_members_count, count));

            if (btnProvideDetails != null) {
                btnProvideDetails.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onAddMember() {
        try {
            String groupId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.GROUP_ID);
            String groupName = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.GROUP_NAME);
            if (groupId == null) return;

            // Members already in this group
            Set<String> existing = new HashSet<>();
            List<MemberObject> existingMembers = AypDao.getOutSchoolGroupMembers(groupId);
            for (MemberObject memberObject : existingMembers) {
                existing.add(memberObject.getBaseEntityId());
            }

            int existingCount = existing.size();
            int maxGroupSize = 10;

            // Check if group already full
            if (existingCount >= maxGroupSize) {
                Toast.makeText(this,
                        "This group already has 10 members. You cannot add more.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // All members eligible to join
            List<MemberObject> all = AypDao.getOutSchoolMembers();
            List<MemberObject> eligible = new ArrayList<>();
            for (MemberObject m : all) {
                if (!existing.contains(m.getBaseEntityId())) eligible.add(m);
            }

            if (eligible.isEmpty()) {
                Toast.makeText(this, org.smartregister.chw.R.string.no, Toast.LENGTH_SHORT).show();
                return;
            }

            // Build multi-choice selection list
            final String[] items = new String[eligible.size()];
            final boolean[] checked = new boolean[eligible.size()];
            for (int i = 0; i < eligible.size(); i++) {
                MemberObject m = eligible.get(i);
                String name = (m.getFirstName() + " " +
                        (m.getMiddleName() != null ? m.getMiddleName() + " " : "") +
                        m.getLastName()).trim();
                items[i] = name;
                checked[i] = false;
            }

            new AlertDialog.Builder(this)
                    .setTitle(org.smartregister.chw.R.string.add_eligible_child)
                    .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> {
                        int selectedCount = 0;
                        for (boolean b : checked) if (b) selectedCount++;

                        // Prevent selecting more than 10 total (including existing)
                        if (isChecked && (existingCount + selectedCount) > maxGroupSize) {
                            ((AlertDialog) dialog).getListView().setItemChecked(which, false);
                            checked[which] = false;
                            Toast.makeText(this,
                                    "Each group can have up to 10 members only",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            checked[which] = isChecked;
                        }
                    })
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        List<String> selectedIds = collectSelectedIds(eligible, checked);
                        int newCount = existingCount + selectedIds.size();

                        if (newCount > maxGroupSize) {
                            Toast.makeText(this,
                                    "Adding these members would exceed the 10-member limit",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        saveMembershipByEvent(groupId, selectedIds);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();

        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private List<String> collectSelectedIds(List<MemberObject> eligible, boolean[] checked) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < eligible.size(); i++)
            if (checked[i]) ids.add(eligible.get(i).getBaseEntityId());
        return ids;
    }

    private void saveMembershipByEvent(String groupId, List<String> memberIds) {
        try {
            if (memberIds == null || memberIds.isEmpty()) return;
            // Build Event tagged to groupId with membership list in details
            Event baseEvent = JsonFormUtils.createUntaggedEvent(groupId,
                    org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_OUT_GROUP_MEMBERSHIP,
                    "ec_ayp_out_school_group_members");
            baseEvent.setFormSubmissionId(AypJsonFormUtils.generateRandomUUIDString());
            JsonFormUtils.tagEvent(Utils.getAllSharedPreferences(), baseEvent);

            org.smartregister.chw.util.JsonFormUtils.tagSyncMetadata(org.smartregister.chw.util.Utils.context().allSharedPreferences(), baseEvent);

            baseEvent.addObs(new Obs("concept", "text", "group_id", "",
                    toList(groupId), new ArrayList<>(), null, "group_id"));

            baseEvent.addObs(new Obs("concept", "text", "members", "",
                    toList(TextUtils.join(",", memberIds)), new ArrayList<>(), null, "members"));


            AllSharedPreferences allSharedPreferences = AypLibrary.getInstance().context().allSharedPreferences();

            try {
                NCUtils.addEvent(allSharedPreferences,baseEvent);
                NCUtils.startClientProcessing();
            } catch (Exception e) {
                Timber.e(e);
            }

            refreshMembersFromSources(groupId);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void refreshMembersFromSources(String groupId) {
        try {
            // From visits
            List<Visit> groupVisits = AypLibrary.getInstance().visitRepository().getVisitsByGroup(groupId);
            Set<String> ids = new HashSet<>();
            for (Visit v : groupVisits)
                if (v.getBaseEntityId() != null) ids.add(v.getBaseEntityId());
            // From membership table
            List<String> extra = new AypOutSchoolGroupMembersRepository().getMemberIds(groupId);
            ids.addAll(extra);
            // Build MemberObjects
            List<MemberObject> members = new ArrayList<>();
            for (String id : ids) {
                MemberObject m = AypDao.getOutSchoolMember(id);
                if (m != null) members.add(m);
            }
            renderMembers(members);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void openMemberProfile(MemberObject member) {
        AypOutSchoolMemberProfileActivity.startProfileActivity(this, member.getBaseEntityId());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE_GET_JSON && resultCode == Activity.RESULT_OK) {
            try {
                String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
                if (jsonString == null)
                    jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                if (jsonString == null) return;

                // Convert form JSON to Event and process via AYP visit pipeline
                AllSharedPreferences prefs = Utils.getAllSharedPreferences();
                Event baseEvent = JsonFormUtils.processJsonForm(prefs, jsonString, org.smartregister.chw.ayp.util.Constants.TABLES.AYP_OUT_SCHOOL_GROUP_DETAILS);
                if (baseEvent != null) {
                    Visit visit = new Visit();
                    visit.setVisitId(UUID.randomUUID().toString());
                    visit.setVisitType(org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_OUT_GROUP_DETAILS);
                    visit.setBaseEntityId(baseEvent.getBaseEntityId());
                    Date now = new Date();
                    visit.setDate(now);
                    visit.setUpdatedAt(now);
                    visit.setProcessed(false);
                    visit.setPreProcessedJson(new Gson().toJson(baseEvent));

                    AypLibrary.getInstance().visitRepository().addVisit(visit);
                    AypVisitsUtil.manualProcessVisit(visit);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void setGroupViewWithData(GroupObject groupObject) {
        super.onGroupLoaded(groupObject);
        // Populate group name, type and age-band from repository
        try {
            TextView tvName = findViewById(R.id.textview_group_name);
            TextView tvType = findViewById(R.id.textview_group_type);
            TextView tvAge = findViewById(R.id.textview_group_age_band);

            String groupId = groupObject.getGroupId();
            AypOutSchoolGroupDetailsRepository repo = new AypOutSchoolGroupDetailsRepository();
            AypInSchoolGroupDetails rec = repo.getByBaseEntityId(groupId);
            if (rec != null) {
                if (tvName != null && rec.getGroupName() != null && !rec.getGroupName().isEmpty()) {
                    tvName.setText(rec.getGroupName());
                }
                if (tvType != null) tvType.setText(localizeGroupType(rec.getGroupType()));
                if (tvAge != null) tvAge.setText(localizeAgeBand(rec.getAgeBand()));
            }
        } catch (Exception ignored) {
        }
    }

    private String localizeGroupType(String raw) {
        if (raw == null) return null;
        switch (raw) {
            case "age_band":
                return getString(org.smartregister.chw.R.string.ayp_group_type_age_band);
            case "classes":
                return getString(org.smartregister.chw.R.string.ayp_group_type_classes);
            default:
                return raw;
        }
    }

    private String localizeAgeBand(String raw) {
        if (raw == null) return null;
        switch (raw) {
            case "age_10_14":
                return getString(org.smartregister.chw.R.string.ayp_age_band_age_10_14);
            case "age_15_19":
                return getString(org.smartregister.chw.R.string.ayp_age_band_age_15_19);
            case "age_20_24":
                return getString(org.smartregister.chw.R.string.ayp_age_band_age_20_24);
            default:
                return raw;
        }
    }
}
