# SPEC - Facility selection on auto-generated NCD referrals

**Branch:** `feat/ncd-auto-referral-emergency-supporter` (follows the emergency/supporter work)
**Approach:** Extend the post-visit confirmation prompt (same dialog as the emergency/supporter
feature) **Status:** Draft / not started

---

## 1. Background

The manual NCD referral (profile floating menu to `ncd_referral_form.json`) makes the CHW pick the
**referral facility** via the `chw_referral_hf` spinner. That selection drives two things
downstream:

- the **`chw_referral_hf` obs** on the Referral Registration event, and
- the **task `groupId`** (and is also surfaced in the referral register / details views).

Facility options are loaded at runtime from
`org.smartregister.chw.referral.util.LocationUtils.INSTANCE.getFacilitiesKeyAndName()`, which
returns a `Map<facilityLocationId, facilityName>`. In the spinner the option **key is the facility
location id** and the text is the display name.

The **auto** referral path (post follow-up visit to `NcdReferralPromptDialog` to
`NcdReferralTaskHelper`) builds the event programmatically and has **no facility picker**. It
hardcodes the CHW's own locality:

- `chw_referral_hf` obs (`DBConstants.Key.REFERRAL_HF`) value:
  - Today: `sharedPreferences.fetchDefaultLocalityId(providerId)`
  - Should be: selected facility location id
- Task `groupId`:
  - Today: `sharedPreferences.fetchUserLocalityId(...)`
  - Should be: selected facility location id
- Task `location`:
  - Today: `fetchUserLocalityId(...)`
  - Should stay as noted in section 8
- Event `locationId`:
  - Today: `fetchDefaultLocalityId(...)`
  - Should stay as noted in section 8

`DBConstants.Key.REFERRAL_HF` already resolves to the string `"chw_referral_hf"`, so the obs field
code is correct; only its **value** is wrong (and there is no UI to choose it).

---

## 2. Goal

Let the CHW choose the referral facility on the post-visit prompt, and use that facility's location
id as the `chw_referral_hf` obs value and the task `groupId`, matching the manual referral
behaviour.

---

## 3. Scope

### In scope

- A required facility spinner on `NcdReferralPromptDialog`, populated from
  `getFacilitiesKeyAndName()`.
- Carrying the selected facility (id + name) through `NcdReferralInputs` into
  `NcdReferralTaskHelper`.
- Using the facility id for the `chw_referral_hf` obs value and the task `groupId`.
- Loading facilities off the main thread (DB read), alongside the existing caregiver fetch.

### Out of scope

- The manual referral form path (already complete).
- Changing how facilities are sourced / the location hierarchy.
- A searchable/hierarchical facility picker (a flat spinner mirrors the form's spinner).

---

## 4. Design

### 4.1 Dialog

Add a **facility** section to `dialog_ncd_referral_prompt.xml` (above the Skip/Create buttons):

- label `@string/ncd_referral_prompt_facility_label`,
- `Spinner ncd_referral_facility` whose first entry is a non-selectable placeholder
  (`@string/ncd_referral_prompt_facility_hint`, e.g. "Choose referral facility"), followed by the
  facility names.

The dialog keeps a parallel `List<String>` of facility ids (index-aligned with the spinner) so the
selected position resolves back to an id.

Facility is **required** (the manual form marks `chw_referral_hf` required): the Create button must
not proceed until a real facility is chosen. Implement by validating on the positive click and
showing a toast / keeping the dialog open when nothing is selected (override the positive button's
click listener so the dialog isn't auto-dismissed on invalid input).

### 4.2 Data loading (off main thread)

`NcdCaseManagementVisitActivity.submittedAndClose` already resolves the caregiver on
`appExecutors.diskIO()`. Resolve the facility map there too
(`LocationUtils.INSTANCE.getFacilitiesKeyAndName()`), then pass it into
`NcdReferralPromptDialog.show(...)` so the spinner is built on the UI thread from in-memory data.

### 4.3 Inputs

Extend `NcdReferralInputs` with `referralFacilityId` and `referralFacilityName` (immutable,
nullable). Update the existing constructor or add a fuller one; the dialog populates both from the
selection.

### 4.4 Event + task (NcdReferralTaskHelper)

- `buildAndPersistReferralEvent`: set the existing `REFERRAL_HF` (`chw_referral_hf`) obs **value**
  to `inputs.referralFacilityId` when present (fallback to `locationId` to preserve current
  behaviour when inputs/facility are null). Attach the facility name as `humanReadableValues` so
  register/detail views can show it without a second lookup.
- `createTask`: set `groupId` to `inputs.referralFacilityId` when present (fallback to
  `fetchUserLocalityId(...)`).
- Keep the null-safe overload contract from the emergency/supporter work: when `inputs` is null the
  behaviour is unchanged.

---

## 5. Files touched

- `custom_views/NcdReferralPromptDialog.java`: facility spinner and id list,
  required-facility validation, and pass facility into inputs.
- `res/layout/dialog_ncd_referral_prompt.xml`: facility label and spinner.
- `activity/NcdCaseManagementVisitActivity.java`: load the facility map off the main thread and pass
  it into the dialog.
- `model/NcdReferralInputs.java`: add `referralFacilityId` and `referralFacilityName`.
- `util/NcdReferralTaskHelper.java`: use the facility id for the `chw_referral_hf` obs value and
  task `groupId`.
- `res/values/strings.xml` and `res/values-sw/strings.xml`: facility label, hint, and validation
  message.
- `test/.../NcdReferralTaskHelperTest`: cover HF value and groupId from facility, plus
  null/fallback.

---

## 6. Acceptance criteria

1. The post-visit prompt shows a facility spinner populated from `getFacilitiesKeyAndName()`.
2. Create is blocked until a facility is chosen (with a clear message); Skip is unaffected.
3. On confirm, the event's `chw_referral_hf` obs value is the selected facility's location id (with
   the facility name as humanReadableValue).
4. The created task's `groupId` is the selected facility's location id.
5. The facility map is read off the main thread.
6. With `inputs` null (legacy callers), behaviour is unchanged (CHW locality fallback).
7. New strings exist in `values` and `values-sw`.

---

## 7. Test plan

- Unit: `NcdReferralTaskHelper` obs value + (where testable) groupId derive from
  `referralFacilityId`; fallback to locality when null. (Task creation hits repositories/prefs, so
  groupId may be asserted via a thin seam or left to the obs-level assertion; decide during
  implementation.)
- Unit: `NcdReferralInputs` getters for the new fields.

---

## 8. Decisions (resolved)

- **Facility is required** - Create is blocked until a facility is chosen (mirrors the form's
  `chw_referral_hf` required_status).
- **No default selection** - the spinner opens on a non-selectable "Choose referral facility"
  placeholder, forcing an explicit choice.
- **Only `chw_referral_hf` obs + task `groupId`** take the selected facility id. `event.locationId`
  and `task.location` stay as the CHW's locality (current behaviour).
