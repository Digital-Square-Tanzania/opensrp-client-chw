<!-- markdownlint-disable MD043 -->

# SPEC - Auto NCD referral emergency/supporter details

**Branch:** `add-is-emergence-on-sw-ncd-ref` **Approach:** A - enrich the
post-visit confirmation dialog (no full form, no extra Activity) **Status:**
Draft / not started

---

## 1. Background

NCD follow-up visits run through `NcdCaseManagementVisitActivity`. When a visit
produces a danger sign or medicine side effect, the interactor stages a
`PendingNcdReferral` and, on `submittedAndClose`, a confirm/skip dialog
(`NcdReferralPromptDialog`) is shown. On confirm,
`NcdReferralTaskHelper.createReferralIfNeeded(...)` **builds the Referral
Registration event programmatically** and creates the linked task.

There are two NCD referral paths today, and they are asymmetric:

- UI:
  - Manual: `ncd_referral_form.json` NeatForm
  - Auto: confirm/skip `AlertDialog`
- Event builder:
  - Manual: referral-library save path from form data
  - Auto: `NcdReferralTaskHelper.buildAndPersistReferralEvent`
- `is_emergency_case`:
  - Manual: present in form
  - Auto: missing
- `has_treatment_supporter`, `_name`, and `_phone`:
  - Manual: present in form, prefilled and editable via
    `TreatmentSupporterFormUtil`
  - Auto: missing

**Goal:** the auto path must also emit `is_emergency_case`,
`has_treatment_supporter`, `treatment_supporter_name`,
`treatment_supporter_phone` (and, for parity,
`treatment_supporter_relationship`) on its Referral Registration event. The
treatment supporter must be **editable for this referral only**; never writing
back to the client's registration record. Editing is for the case where the
client presents a different supporter at referral time.

---

## 2. Scope

### In scope

- A richer post-visit dialog that captures the emergency flag and an editable
  treatment-supporter section, prefilled from
  `TreatmentSupporterDao.getRegisteredCaregiver`.
- Threading the captured values into
  `NcdReferralTaskHelper.createReferralIfNeeded` and emitting them as obs on the
  Referral Registration event.
- English + Swahili strings for the new dialog controls.

### Out of scope

- The manual referral form path (already complete).
- Editing/persisting the client's registered caregiver record.
- Changing the alert-detection logic that stages `PendingNcdReferral`.
- Letting the CHW edit the detected problem list or referral health facility
  (that is Approach B territory).

---

## 3. Obs to emit (target event shape)

Added to the event inside `buildAndPersistReferralEvent`, mirroring the existing
single-value obs shape (`fieldType = "concept"`, `parentCode = ""`):

- `is_emergency_case`: `"Yes"` / `"No"`, always persisted.
- `has_treatment_supporter`: `"Yes"` / `"No"`, always persisted.
- `treatment_supporter_name`: edited text, persisted when gate is `Yes` and
  non-blank.
- `treatment_supporter_phone`: edited text, persisted when gate is `Yes` and
  non-blank.
- `treatment_supporter_relationship`: spinner option key, persisted when gate is
  `Yes` and non-blank.

When the gate is `No`, only `is_emergency_case` and `has_treatment_supporter` (=
`No`) are written; the three detail obs are omitted. This matches
`TreatmentSupporterFormUtil`'s "details only when gate is Yes" rule.

---

## 4. Defaults & prefill

- **`is_emergency_case`** default: `Yes` when `alertLevel == ALERT_RED`, else
  `No`. CHW-overridable.
- **Treatment supporter:** load
  `TreatmentSupporterDao.getRegisteredCaregiver(baseEntityId)` off the main
  thread _before_ showing the dialog.
  - If a caregiver `isPresent()`: gate defaults to `Yes`;
    name/phone/relationship prefilled and editable.
  - If none on record: gate defaults to `No`; detail fields empty (shown when
    gate flipped to `Yes`).
- Relationship spinner uses the same 18 options as the form's
  `treatment_supporter_relationship` field (Mother, Father, Brother, Sister,
  Grandfather, Grandmother, Friend, Uncle, Aunt, Police, Guardian, Son,
  Daughter, Work Colleague, Brother in Law, Sister in Law, Wife, Husband).

---

## 5. Design

### 5.1 New dialog (`NcdReferralPromptDialog` to custom layout)

Replace the plain `AlertDialog` message with a custom view
(`dialog_ncd_referral_prompt.xml`) containing, top to bottom:

1. Title (existing urgent / non-emergency string).
2. Body + bulleted reasons (existing behavior).
3. **Emergency case** - labeled Yes/No control (RadioGroup), seeded per
   section 4.
4. **Treatment supporter**:
   - Has-supporter gate (Yes/No). Toggling controls visibility of the detail
     block.
   - Name (EditText), Phone (EditText, `inputType=phone`), Relationship
     (Spinner).
5. Skip / Create buttons (existing strings).

The dialog collects values into a small immutable result and hands them to the
confirm callback.

### 5.2 Inputs object

Introduce `NcdReferralInputs` (plain data holder): `isEmergencyCase`
(bool/`"Yes"`/`"No"`), `hasTreatmentSupporter`, `supporterName`,
`supporterPhone`, `supporterRelationship`. Prefer this over expanding the
`createReferralIfNeeded` argument list further.

### 5.3 Callback / helper signature

`NcdReferralPromptDialog.Callbacks.onConfirm()` becomes
`onConfirm(NcdReferralInputs inputs)`. `NcdCaseManagementVisitActivity` passes
`inputs` to a new overload:

```text
createReferralIfNeeded(
    baseEntityId,
    triggeringFormSubmissionId,
    alertLevel,
    description,
    problemKeys,
    reasons,
    NcdReferralInputs inputs
)
```

Keep/forward the existing signature (passing `null` inputs) so other callers are
unaffected.

### 5.4 Event building

In `buildAndPersistReferralEvent`, after the existing obs, append the section 3
obs from `inputs` (guarded for null inputs, emitting nothing and preserving
current behavior). Add concept-key constants to `Constants.NcdReferral`.

### 5.5 Threading

`submittedAndClose` currently reads the pending referral and shows the dialog on
the UI thread. Add a background fetch of the caregiver (via
`appExecutors.diskIO()`), then `runOnUiThread` to show the dialog prefilled. The
DB read must not run on the main thread.

---

## 6. Files touched

- `custom_views/NcdReferralPromptDialog.java`: custom layout,
  emergency/supporter collection, and new callback signature.
- `res/layout/dialog_ncd_referral_prompt.xml`: new dialog layout.
- `activity/NcdCaseManagementVisitActivity.java`: background caregiver fetch and
  pass `NcdReferralInputs` to the helper.
- `util/NcdReferralTaskHelper.java`: new overload, obs emission, and
  null-safety.
- `util/Constants.java` (`NcdReferral`): concept-key constants.
- `model/NcdReferralInputs.java`: new data holder.
- `res/values/strings.xml`: new labels and hints.
- `res/values-sw/strings.xml`: Swahili translations.
- `test/.../NcdReferralTaskHelperTest`: obs emission and gate-rule coverage.

---

## 7. Acceptance criteria

1. Confirming an auto NCD referral writes a Referral Registration event that
   includes `is_emergency_case` and `has_treatment_supporter` obs; when the gate
   is Yes and details are present,
   `treatment_supporter_name`/`_phone`/`_relationship` are included too.
2. `is_emergency_case` defaults to Yes for red alerts, No for yellow, and the
   CHW can change it.
3. Supporter fields prefill from the registered caregiver and are editable;
   edits appear on the referral event and the client's `ec_family_member`
   registration record is unchanged.
4. With the gate set to No, no supporter detail obs are emitted.
5. Skip still writes no event or task.
6. The caregiver DB lookup runs off the main thread.
7. New strings are present in both `values` and `values-sw`.

---

## 8. Decisions (resolved)

- **`is_emergency_case`** is CHW-**editable**: a Yes/No control seeded from
  alert level (red maps to Yes, yellow maps to No).
- **`treatment_supporter_relationship`** is **included** on the auto path
  (18-option spinner) for parity with the manual form.
- **Phone validation** is **lenient**: `inputType=phone`, any non-blank value
  accepted; matches the form's handling.
