package org.smartregister.chw.custom_views;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.smartregister.chw.R;
import org.smartregister.chw.dao.TreatmentSupporterDao;
import org.smartregister.chw.interactor.NcdCaseManagementInteractor;
import org.smartregister.chw.interactor.NcdCaseManagementInteractor.PendingNcdReferral;
import org.smartregister.chw.model.NcdReferralInputs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Confirmation prompt shown after an NCD Monthly Follow-Up visit is submitted and the
 * interactor has staged a {@link PendingNcdReferral}. The CHW sees the triggering reasons,
 * confirms whether the case is an emergency, and reviews/edits the treatment supporter before
 * the referral is created. Only the Confirm path invokes
 * {@code NcdReferralTaskHelper.createReferralIfNeeded(...)}; Skip leaves no event or task
 * behind (the visit itself is already persisted by the time this dialog is shown).
 *
 * <p>The supporter is prefilled from the registered caregiver but edited <em>for this referral
 * only</em>; the dialog never writes back to the registration record.
 */
public final class NcdReferralPromptDialog {

    /**
     * Relationship spinner options. Kept in sync with the
     * {@code treatment_supporter_relationship} field on {@code ncd_referral_form.json} so the
     * option keys emitted as obs match the manual referral path.
     */
    private static final String[] RELATIONSHIP_OPTIONS = {
            "Mother", "Father", "Brother", "Sister", "Grandfather", "Grandmother", "Friend",
            "Uncle", "Aunt", "Police", "Guardian", "Son", "Daughter", "Work Colleague",
            "Brother in Law", "Sister in Law", "Wife", "Husband"
    };
    private static final String YES = "Yes";
    private static final String NO = "No";

    private NcdReferralPromptDialog() {
        // utility
    }

    public interface Callbacks {
        void onConfirm(NcdReferralInputs inputs);

        void onSkip();
    }

    public static void show(Activity activity, PendingNcdReferral pending,
                            TreatmentSupporterDao.Caregiver caregiver,
                            Map<String, String> facilities, Callbacks callbacks) {
        if (activity == null || activity.isFinishing() || pending == null || callbacks == null) {
            if (callbacks != null) {
                callbacks.onSkip();
            }
            return;
        }

        boolean isRed = NcdCaseManagementInteractor.ALERT_RED.equals(pending.alertLevel);
        int titleRes = isRed
                ? R.string.ncd_referral_prompt_title_urgent
                : R.string.ncd_referral_prompt_title_non_emergency;

        View view = LayoutInflater.from(activity).inflate(
                R.layout.dialog_ncd_referral_prompt, null);

        TextView reasonsView = view.findViewById(R.id.ncd_referral_reasons);
        reasonsView.setText(buildMessage(activity, pending.reasons));

        // Emergency defaults: Yes for red alerts, No for yellow. CHW can override.
        RadioGroup emergencyGroup = view.findViewById(R.id.ncd_referral_emergency_group);
        emergencyGroup.check(isRed
                ? R.id.ncd_referral_emergency_yes
                : R.id.ncd_referral_emergency_no);

        // Treatment supporter: gate defaults to Yes when a caregiver is on record.
        boolean hasCaregiver = caregiver != null && caregiver.isPresent();
        RadioGroup supporterGroup = view.findViewById(R.id.ncd_referral_supporter_group);
        View supporterDetails = view.findViewById(R.id.ncd_referral_supporter_details);
        EditText nameView = view.findViewById(R.id.ncd_referral_supporter_name);
        EditText phoneView = view.findViewById(R.id.ncd_referral_supporter_phone);
        Spinner relationshipView = view.findViewById(R.id.ncd_referral_supporter_relationship);

        ArrayAdapter<String> relationshipAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, RELATIONSHIP_OPTIONS);
        relationshipAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        relationshipView.setAdapter(relationshipAdapter);

        if (hasCaregiver) {
            if (!TextUtils.isEmpty(caregiver.getName())) {
                nameView.setText(caregiver.getName().trim());
            }
            if (!TextUtils.isEmpty(caregiver.getPhone())) {
                phoneView.setText(caregiver.getPhone().trim());
            }
            int relIndex = relationshipIndex(caregiver.getRelationship());
            if (relIndex >= 0) {
                relationshipView.setSelection(relIndex);
            }
        }
        supporterGroup.check(hasCaregiver
                ? R.id.ncd_referral_supporter_yes
                : R.id.ncd_referral_supporter_no);
        supporterDetails.setVisibility(hasCaregiver ? View.VISIBLE : View.GONE);
        supporterGroup.setOnCheckedChangeListener((group, checkedId) ->
                supporterDetails.setVisibility(
                        checkedId == R.id.ncd_referral_supporter_yes ? View.VISIBLE : View.GONE));

        // Referral facility: index 0 is a non-selectable placeholder, then one entry per facility.
        // facilityIds is index-aligned with the spinner (id is null at the placeholder position).
        Spinner facilityView = view.findViewById(R.id.ncd_referral_facility);
        List<String> facilityNames = new ArrayList<>();
        List<String> facilityIds = new ArrayList<>();
        facilityNames.add(activity.getString(R.string.ncd_referral_prompt_facility_hint));
        facilityIds.add(null);
        if (facilities != null) {
            for (Map.Entry<String, String> entry : facilities.entrySet()) {
                facilityIds.add(entry.getKey());
                facilityNames.add(entry.getValue());
            }
        }
        ArrayAdapter<String> facilityAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, facilityNames);
        facilityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        facilityView.setAdapter(facilityAdapter);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(titleRes)
                .setView(view)
                .setCancelable(false)
                .setNegativeButton(R.string.ncd_referral_prompt_button_skip,
                        (d, w) -> callbacks.onSkip())
                .setPositiveButton(R.string.ncd_referral_prompt_button_create, null)
                .create();

        // Override the positive button after show() so an unselected facility keeps the dialog
        // open (the default listener would auto-dismiss).
        dialog.setOnShowListener(d -> {
            Button create = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            create.setOnClickListener(v -> {
                int pos = facilityView.getSelectedItemPosition();
                if (pos <= 0) {
                    Toast.makeText(activity, R.string.ncd_referral_prompt_facility_required,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                callbacks.onConfirm(collectInputs(emergencyGroup, supporterGroup, nameView,
                        phoneView, relationshipView, facilityIds.get(pos),
                        facilityNames.get(pos)));
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private static NcdReferralInputs collectInputs(RadioGroup emergencyGroup,
                                                   RadioGroup supporterGroup,
                                                   EditText nameView, EditText phoneView,
                                                   Spinner relationshipView,
                                                   String facilityId, String facilityName) {
        String isEmergency = emergencyGroup.getCheckedRadioButtonId()
                == R.id.ncd_referral_emergency_yes
                ? YES : NO;
        boolean gateYes = supporterGroup.getCheckedRadioButtonId()
                == R.id.ncd_referral_supporter_yes;
        if (!gateYes) {
            return new NcdReferralInputs(isEmergency, NO, null, null, null, facilityId,
                    facilityName);
        }
        String name = textOf(nameView);
        String phone = textOf(phoneView);
        String relationship = relationshipView.getSelectedItem() != null
                ? relationshipView.getSelectedItem().toString() : null;
        return new NcdReferralInputs(isEmergency, YES, name, phone, relationship, facilityId,
                facilityName);
    }

    private static String textOf(EditText editText) {
        String value = editText.getText() != null ? editText.getText().toString().trim() : "";
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        return value;
    }

    private static int relationshipIndex(String relationship) {
        if (TextUtils.isEmpty(relationship)) {
            return -1;
        }
        for (int i = 0; i < RELATIONSHIP_OPTIONS.length; i++) {
            if (RELATIONSHIP_OPTIONS[i].equalsIgnoreCase(relationship.trim())) {
                return i;
            }
        }
        return -1;
    }

    private static CharSequence buildMessage(Activity activity, List<String> reasons) {
        StringBuilder sb = new StringBuilder();
        sb.append(activity.getString(R.string.ncd_referral_prompt_body));
        if (reasons != null) {
            for (String reason : reasons) {
                sb.append("\n• ").append(reason);
            }
        }
        return sb.toString();
    }
}
