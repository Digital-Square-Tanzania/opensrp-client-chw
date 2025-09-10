package org.smartregister.chw.activity;

import static org.smartregister.util.JsonFormUtils.ENTITY_ID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;
import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONObject;
import org.smartregister.chw.ayp.AypLibrary;
import org.smartregister.chw.ayp.activity.BaseAypGroupProfileActivity;
import org.smartregister.chw.ayp.dao.AypDao;
import org.smartregister.chw.ayp.domain.GroupObject;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.domain.Visit;
import org.smartregister.chw.ayp.util.AypVisitsUtil;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.ayp.util.JsonFormUtils;
import org.smartregister.chw.domain.AypInSchoolGroupDetails;
import org.smartregister.chw.repository.AypInSchoolGroupDetailsRepository;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.Utils;
import org.smartregister.view.activity.FormActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AypInSchoolGroupProfileActivity extends BaseAypGroupProfileActivity {

    public static void start(Context context, String groupId, String groupName) {
        Intent intent = new Intent(context, AypInSchoolGroupProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.GROUP_ID, groupId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.GROUP_NAME, groupName);
        context.startActivity(intent);
    }

    @Override
    public void openGroupDetailsForm() {
        try {
            JSONObject form = new FormUtils().getFormJsonFromRepositoryOrAssets(this, "ayp_in_school_group_creation");
            if (form != null) {
                // Use group id as entity id for this event
                String groupId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.GROUP_ID);
                if (groupId == null) groupId = UUID.randomUUID().toString();
                form.put(ENTITY_ID, groupId);

                // Configure form (non-wizard)
                Form cfg = new Form();
                cfg.setWizard(false);
                Intent intent = new Intent(this, FormActivity.class);
                intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, form.toString());
                intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, cfg);
                startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onAddMember() {
        try {
            String groupId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.GROUP_ID);
            String groupName = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.GROUP_NAME);
            if (groupId == null) return;

            // Members already in this group
            List<Visit> groupVisits = AypLibrary.getInstance().visitRepository().getVisitsByGroup(groupId);
            Set<String> existing = new HashSet<>();
            for (Visit v : groupVisits) {
                if (v.getBaseEntityId() != null) existing.add(v.getBaseEntityId());
            }

            // All in-school members
            List<MemberObject> all = AypDao.getInSchoolMembers();
            List<MemberObject> eligible = new ArrayList<>();
            for (MemberObject m : all) {
                if (!existing.contains(m.getBaseEntityId())) eligible.add(m);
            }

            if (eligible.isEmpty()) {
                Toast.makeText(this, org.smartregister.chw.R.string.no, Toast.LENGTH_SHORT).show();
                return;
            }

            // Build selection list
            List<String> labels = new ArrayList<>();
            for (MemberObject m : eligible) {
                String name = (m.getFirstName() + " " + (m.getMiddleName() != null ? m.getMiddleName() + " " : "") + m.getLastName()).trim();
                labels.add(name);
            }
            final String[] items = labels.toArray(new String[0]);
            new AlertDialog.Builder(this)
                    .setTitle(org.smartregister.chw.R.string.add_eligible_child)
                    .setItems(items, (dialog, which) -> {
                        MemberObject selected = eligible.get(which);
                        AypInSchoolGroupVisitActivity.startAypInSchoolGroupVisitActivity(this, selected.getBaseEntityId(), false, groupId, groupName);
                    })
                    .show();
        } catch (Exception ignored) { }
    }

    @Override
    public void openMemberProfile(MemberObject member) {
        AypInSchoolMemberProfileActivity.startProfileActivity(this, member.getBaseEntityId());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE_GET_JSON && resultCode == Activity.RESULT_OK) {
            try {
                String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
                if (jsonString == null) jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                if (jsonString == null) return;

                // Convert form JSON to Event and process via AYP visit pipeline
                AllSharedPreferences prefs = Utils.getAllSharedPreferences();
                Event baseEvent = JsonFormUtils.processJsonForm(prefs, jsonString, org.smartregister.chw.ayp.util.Constants.TABLES.AYP_IN_SCHOOL_GROUP_DETAILS);
                if (baseEvent != null) {
                    Visit visit = new Visit();
                    visit.setVisitId(UUID.randomUUID().toString());
                    visit.setVisitType(org.smartregister.chw.ayp.util.Constants.EVENT_TYPE.AYP_GROUP_DETAILS);
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
            TextView tvName = findViewById(org.smartregister.chw.ayp.R.id.textview_group_name);
            TextView tvType = findViewById(org.smartregister.chw.ayp.R.id.textview_group_type);
            TextView tvAge = findViewById(org.smartregister.chw.ayp.R.id.textview_group_age_band);

            String groupId = groupObject.getGroupId();
            AypInSchoolGroupDetailsRepository repo = new AypInSchoolGroupDetailsRepository();
            AypInSchoolGroupDetails rec = repo.getByBaseEntityId(groupId);
            if (rec != null) {
                if (tvName != null && rec.getGroupName() != null && !rec.getGroupName().isEmpty()) {
                    tvName.setText(rec.getGroupName());
                }
                if (tvType != null) tvType.setText(localizeGroupType(rec.getGroupType()));
                if (tvAge != null) tvAge.setText(localizeAgeBand(rec.getAgeBand()));
            }
        } catch (Exception ignored) { }
    }

    private String localizeGroupType(String raw) {
        if (raw == null) return null;
        switch (raw) {
            case "age_band": return getString(org.smartregister.chw.R.string.ayp_group_type_age_band);
            case "classes": return getString(org.smartregister.chw.R.string.ayp_group_type_classes);
            default: return raw;
        }
    }

    private String localizeAgeBand(String raw) {
        if (raw == null) return null;
        switch (raw) {
            case "age_10_14": return getString(org.smartregister.chw.R.string.ayp_age_band_age_10_14);
            case "age_10_19_enabling_dreams": return getString(org.smartregister.chw.R.string.ayp_age_band_age_10_19_enabling_dreams);
            case "age_15_19": return getString(org.smartregister.chw.R.string.ayp_age_band_age_15_19);
            case "age_20_24": return getString(org.smartregister.chw.R.string.ayp_age_band_age_20_24);
            default: return raw;
        }
    }
}
