# NCD Case Management — Implementation Tasks

**Spec:** [TECHSPEC v1.1](file:///home/gosso/.claude/MEMORY/WORK/20260401-120000_ncd-case-management-techspec/TECHSPEC.md)
**Branch:** `feature-ncd-case-management`
**Repo:** `Digital-Square-Tanzania/opensrp-client-chw`
**Last Updated:** 2026-04-16

---

## Legend

- [ ] Not started
- [~] In progress
- [x] Complete
- [!] Blocked

---

## Phase 1: Database & Foundation (no UI dependency)

These must land first — everything else reads/writes to these tables.

- [x] **1.1** Add `ec_ncd_case_management_followup` to `ec_client_classification.json` (§12.2)
  - Event type: `NCD Monthly Follow-Up`
  - Fields: `visit_date`, `clinic_attendance`, `medication_adherence`, `no_adherence_reason`, `non_healing_wounds`, `neuropathy`, `vision_changes`, `chest_pain`, `salt_intake_target_met`, `sugar_intake_target_met`, `physical_activity_target_met`, `peer_support_linked`, `alert_status`, `is_red_alert`, `is_yellow_alert`, `is_side_effects_alert`, `is_missed_clinic_alert`
- [x] **1.2** Add `ec_ncd_case_management_followup` fields to `ec_client_fields.json` (§12.3)
- [x] **1.3** Add database upgrade method in `ChwRepositoryFlv.java` to create `ec_ncd_case_management_followup` table
- [x] **1.4** Create `NcdCaseManagementDao.java` (§11)
  - `getLastFollowUpEvent(baseEntityId)`
  - `getVisitHistory(baseEntityId, limit)`
  - `hasOpenReferral(baseEntityId)`

## Phase 2: Monthly Follow-Up Forms (core deliverable)

Each section is a **separate action** with its own JSON form and ActionHelper, following the
existing pattern in `NcdVisitInteractor` (Vitals action + Client Education action).
All forms are single-step (`"count": "1"`) with `ProcessingMode.SEPARATE`.

### 2A — Section A: Clinical Adherence Form + Action

- [x] **2.1** Create `ncd_followup_clinical_adherence.json` (EN) — single-step form (§8.2)
  - `clinic_attendance` (native_radio: yes/no)
  - `medication_adherence` (native_radio: yes/no)
  - `no_adherence_reason` (spinner, conditional on adherence = no)
  - `counseling_felt_better` (toaster_notes, conditional on reason = felt_better)
  - `yellow_alert_toaster` (toaster_notes, conditional on YELLOW conditions)
  - Hidden calculated fields: `is_yellow_alert`, `is_side_effects_alert`, `is_missed_clinic_alert`
- [x] **2.2** Create `ncd_followup_clinical_adherence.json` (SW) in `json.form-sw/`
- [x] **2.3** Create `NcdClinicalAdherenceActionHelper.java` implementing `NcdVisitActionHelper`
  - Extract `clinic_attendance` + `medication_adherence` for subtitle
  - `evaluateStatusOnPayload()`: COMPLETED when both fields answered

### 2B — Section B: Danger Signs Form + Action

- [x] **2.4** Create `ncd_followup_danger_signs.json` (EN) — single-step form (§8.3)
  - `non_healing_wounds` (native_radio: yes/no)
  - `neuropathy` (native_radio: yes/no)
  - `vision_changes` (native_radio: yes/no)
  - `chest_pain` (native_radio: yes/no)
  - `red_alert_toaster` (toaster_notes, conditional on any danger sign = yes)
  - Hidden calculated fields: `is_red_alert`, `alert_status`
- [x] **2.5** Create `ncd_followup_danger_signs.json` (SW) in `json.form-sw/`
- [x] **2.6** Create `NcdDangerSignsActionHelper.java` implementing `NcdVisitActionHelper`
  - Extract danger sign responses for subtitle (e.g. "2 danger signs detected")
  - `evaluateStatusOnPayload()`: COMPLETED when all 4 fields answered
  - `postProcess()`: evaluate `alert_status`, trigger referral task if RED
  - Referral dedup: check `NcdCaseManagementDao.hasOpenReferral()` before creating
  - When RED + YELLOW both true: include YELLOW flags in referral payload
  - Constraint: max 1 active referral per client (§9.4)

### 2C — Section C: Lifestyle Modifications Form + Action

- [x] **2.7** Create `ncd_followup_lifestyle.json` (EN) — single-step form (§8.4)
  - `salt_intake_target_met` (native_radio: yes/no)
  - `sugar_intake_target_met` (native_radio: yes/no)
  - `physical_activity_target_met` (native_radio: yes/no)
  - Educational prompt toasters: `salt_prompt`, `sugar_prompt`, `activity_prompt`
- [x] **2.8** Create `ncd_followup_lifestyle.json` (SW) in `json.form-sw/`
- [x] **2.9** Create `NcdLifestyleActionHelper.java` implementing `NcdVisitActionHelper`
  - Extract target-met counts for subtitle (e.g. "2/3 targets met")
  - `evaluateStatusOnPayload()`: COMPLETED when all 3 fields answered

### 2D — Section D: Psychosocial Support Form + Action

- [x] **2.10** Create `ncd_followup_psychosocial.json` (EN) — single-step form (§8.5)
  - `peer_support_linked` (native_radio: yes/no)
  - `peer_support_prompt` (toaster_notes, conditional on linked = no)
- [x] **2.11** Create `ncd_followup_psychosocial.json` (SW) in `json.form-sw/`
- [x] **2.12** Create `NcdPsychosocialActionHelper.java` implementing `NcdVisitActionHelper`
  - Extract linked status for subtitle
  - `evaluateStatusOnPayload()`: COMPLETED when field answered

### 2E — Rules & Decision Support Content

- [x] **2.13** Create `ncd_followup_clinical_adherence_relevance.yml` (§9.1)
  - Rules: `no_adherence_reason`, `counseling_felt_better`, `yellow_alert_toaster`
- [x] **2.14** Create `ncd_followup_clinical_adherence_calculation.yml` (§9.2)
  - Rules: `is_side_effects_alert`, `is_missed_clinic_alert`, `is_yellow_alert`
- [x] **2.15** Create `ncd_followup_danger_signs_relevance.yml` (§9.1)
  - Rules: `red_alert_toaster`
- [x] **2.16** Create `ncd_followup_danger_signs_calculation.yml` (§9.2)
  - Rules: `is_red_alert`, `alert_status`
  - Note: `alert_status` only sets RED locally; YELLOW precedence handled in Phase 3 Java Interactor
- [x] **2.17** Create `ncd_followup_lifestyle_relevance.yml` (§9.1)
  - Rules: `salt_prompt`, `sugar_prompt`, `activity_prompt`
- [x] **2.18** Create `ncd_followup_psychosocial_relevance.yml` (§9.1)
  - Rules: `peer_support_prompt`
- [x] **2.19** Add decision support content text to form toasters (§10)
  - Silent Killer counseling script (§10.1) — EN + SW
  - YELLOW ALERT message (§10.2)
  - RED ALERT message with transport instructions (§10.3)
  - Salt reduction prompt with local spice alternatives (§10.4)
  - Sugar reduction prompt (§10.5)
  - Physical activity prompt (§10.6)
  - Peer support linkage prompt (§10.7)

## Phase 3: Visit Workflow (Java — wires actions to visit screen)

Depends on: Phase 1 (DAO), Phase 2 (forms + action helpers exist)

- [x] **3.1** Create `NcdCaseManagementInteractor.java` extending `BaseNcdVisitInteractor`
  - Override `getEncounterType()` → `"NCD Monthly Follow-Up"`
  - Override `getTableName()` → `"ec_ncd_case_management_followup"`
  - Override `populateActionList()` → build 4 actions in order:
    1. Clinical Adherence → `ncd_followup_clinical_adherence` + `NcdClinicalAdherenceActionHelper`
    2. Danger Signs → `ncd_followup_danger_signs` + `NcdDangerSignsActionHelper`
    3. Lifestyle → `ncd_followup_lifestyle` + `NcdLifestyleActionHelper`
    4. Psychosocial → `ncd_followup_psychosocial` + `NcdPsychosocialActionHelper`
  - Note: Pre-fill logic for unresolved alerts (§9.5) deferred to Phase 5 (profile screen integration)
- [x] **3.2** Create `NcdCaseManagementVisitPresenter.java` extending `BaseNcdVisitPresenter`
  - Wire to `NcdCaseManagementInteractor`
- [x] **3.3** Create `NcdCaseManagementVisitActivity.java` extending `NcdVisitActivity`
  - Override `registerPresenter()` → use `NcdCaseManagementVisitPresenter`
  - `startMe(activity, baseEntityId, isEditMode)` static launcher
- [x] **3.4** Add `NCD Monthly Follow-Up` encounter type constant to `Constants.java`
- [x] **3.5** Add form file name constants to `Constants.JsonForm` for all 4 forms
- [x] **3.6** Add string resources for visit title (EN + SW) — action name strings added in Phase 2

## Phase 4: Task Generation (recurring 30-day schedule)

Depends on: Phase 3 (visit activity exists to open from task)

- [x] **4.1** Create `NcdCaseManagementFollowupRule` + DAO methods for scheduling
  - `getConfirmationDate()` queries `ec_diabetes_hypertension_confirmation.visit_date`
  - `getLastFollowUpDate()` queries `ec_ncd_case_management_followup.visit_date`
  - Rule computes: due=+30d, overdue=+37d, expiry=+365d from last visit (or confirmation date)
  - Note: Uses `BaseTaskExecutor` pattern (like TB/HIV), NOT `services.json` (child health only)
- [x] **4.2** Create `NcdCaseManagementVisitScheduler` extending `BaseTaskExecutor`
  - Registered in `ChwScheduleTaskExecutor` for NCD Monthly Follow-Up and confirmation events
  - Schedule type: `NCD_CASE_MANAGEMENT_VISIT`
- [x] **4.3** Wire NcdProfileActivity to launch `NcdCaseManagementVisitActivity` for confirmed NCD clients
  - Modified `openFollowupVisit()` routing: confirmed → NcdCaseManagementVisitActivity

## Phase 5: Client Case Summary UI (Profile Screen)

Depends on: Phase 1 (DAO for queries), Phase 3 (visit can be launched from profile)

- [x] **5.1** ~~Create `NcdCaseManagementProfileActivity`~~ — implemented directly in `NcdProfileActivity` (simpler, avoids unnecessary subclass)
- [x] **5.2** Implement header: diagnosis badge (DM/HTN/DM+HTN), confirmed date — `populateCaseSummary()` + `content_ncd_profile.xml`
- [x] **5.3** Implement alert status banner — RED/YELLOW/NONE colored badge via `GradientDrawable` on `ncd_cs_alert_badge`
- [x] **5.4** Implement visit summary panel — last follow-up date, next due date, open referral status via `ncd_cs_dates_section` + `ncd_cs_referral_section`
- [x] **5.5** Implement visit history list — last 3 follow-ups with date, colored dot, clinical flags via `buildClinicalSummary()`. Also: full Medical History screen via `NcdMedicalHistoryActivity` (accessed from `rlLastVisit` row)
- [x] **5.6** Wire "Start Visit" button — `openFollowupVisit()` routes confirmed NCD → `NcdCaseManagementVisitActivity`
- [x] **5.7** `NcdProfileActivity` already registered in `AndroidManifest.xml`

## Phase 6: Register Lifecycle

Depends on: Phase 5 (profile screen hosts the close action — done, lives in `NcdProfileActivity`)

- [x] **6.1** Add "Close Record" menu action to `NcdProfileActivity` — `ncd_profile_menu.xml` with `action_close_ncd_case`, wired via `onCreateOptionsMenu` + `onOptionsItemSelected`
- [x] **6.2** Implement close event — `ncd_case_management_close.json` (EN + SW) form with `close_reason` spinner (deceased, relocated, transferred, ltfu, data_error) + conditional `transfer_facility` field. Encounter type `NCD Case Management Close` added to `ec_client_classification.json` with `closes_case: ["ec_ncd_register"]`
- [x] **6.3** Cancel all DUE/OVERDUE tasks and void open referrals on close — `NcdCaseManagementDao.cancelOpenTasks()` + `voidOpenReferrals()`, called from `handleNcdCaseClosure()` after form event is saved
- [x] **6.4** Filter closed records from active register query — pre-existing: `BaseNcdRegisterFragmentPresenter.getMainCondition()` returns `is_closed = 0`

## Phase 7: Sync Status (polish)

Depends on: Phase 1 (data exists to sync)

- [ ] **7.1** Add per-record sync status chip to profile view — pending/synced/failed (§7, ISC-83)

---

## Open Blockers

| # | Question | Owner | Status |
|---|----------|-------|--------|
| OQ-4 | CHW org unit assignments confirmed for task scoping? | Admin | Unconfirmed |
| OQ-6 | Referral closure propagation from facility back to CHW app? | Backend team | Unconfirmed |
| OQ-7 | Swahili counseling script translations clinically reviewed? | Translator | Unconfirmed |
| OQ-8 | Visit history — last 3 visits hardcoded or configurable? | Design | Decision needed |
| OQ-9 | "Felt Better" counseling toaster — dismissible or always expanded? | Design | Decision needed |

---

## Branch Maintenance

- [ ] Rebase/merge `feature-ncd-case-management` onto latest `development` (30 days stale as of 2026-04-02, includes SDK 35 upgrade, HPS fixes, NACP merges)
