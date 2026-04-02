# NCD Case Management — Implementation Tasks

**Spec:** [TECHSPEC v1.1](file:///home/gosso/.claude/MEMORY/WORK/20260401-120000_ncd-case-management-techspec/TECHSPEC.md)
**Branch:** `feature-ncd-case-management`
**Repo:** `Digital-Square-Tanzania/opensrp-client-chw`
**Last Updated:** 2026-04-02

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

## Phase 2: Monthly Follow-Up Form (core deliverable)

The form + rules are the heart of the feature. No Java UI dependency — pure JSON/YAML assets.

- [ ] **2.1** Create `ncd_monthly_followup_form.json` (EN) — 4-step form (§8)
  - [ ] Step 1 — Section A: Clinical Adherence (§8.2)
    - `clinic_attendance` (native_radio)
    - `medication_adherence` (native_radio)
    - `no_adherence_reason` (spinner, conditional on adherence = no)
    - `counseling_felt_better` (toaster_notes, conditional on reason = felt_better)
    - `yellow_alert_toaster` (toaster_notes, conditional on YELLOW conditions)
    - Hidden calculated fields: `is_yellow_alert`, `is_side_effects_alert`, `is_missed_clinic_alert`
  - [ ] Step 2 — Section B: Danger Signs (§8.3)
    - `non_healing_wounds` (native_radio)
    - `neuropathy` (native_radio)
    - `vision_changes` (native_radio)
    - `chest_pain` (native_radio)
    - `red_alert_toaster` (toaster_notes, conditional on any danger sign = yes)
    - Hidden calculated fields: `is_red_alert`, `alert_status`
  - [ ] Step 3 — Section C: Lifestyle Modifications (§8.4)
    - `salt_intake_target_met` (native_radio)
    - `sugar_intake_target_met` (native_radio)
    - `physical_activity_target_met` (native_radio)
    - Educational prompt toasters: `salt_prompt`, `sugar_prompt`, `activity_prompt`
  - [ ] Step 4 — Section D: Psychosocial Support (§8.5)
    - `peer_support_linked` (native_radio)
    - `peer_support_prompt` (toaster_notes, conditional on linked = no)
- [ ] **2.2** Create `ncd_monthly_followup_form.json` (SW) — Swahili variant in `json.form-sw/`
- [ ] **2.3** Create `ncd_monthly_followup_relevance.yml` (§9.1)
  - Rules: `no_adherence_reason`, `counseling_felt_better`, `yellow_alert_toaster`, `red_alert_toaster`, `salt_prompt`, `sugar_prompt`, `activity_prompt`, `peer_support_prompt`
- [ ] **2.4** Create `ncd_monthly_followup_calculation.yml` (§9.2)
  - Rules: `is_red_alert`, `is_side_effects_alert`, `is_missed_clinic_alert`, `is_yellow_alert`, `alert_status`
  - Must implement: RED > YELLOW precedence (§9.3 truth table)
- [ ] **2.5** Add decision support content text to form toasters (§10)
  - Silent Killer counseling script (§10.1) — EN + SW
  - YELLOW ALERT message (§10.2)
  - RED ALERT message with transport instructions (§10.3)
  - Salt reduction prompt with local spice alternatives (§10.4)
  - Sugar reduction prompt (§10.5)
  - Physical activity prompt (§10.6)
  - Peer support linkage prompt (§10.7)

## Phase 3: Visit Workflow (Java — wires form to app)

Depends on: Phase 1 (DAO), Phase 2 (form exists)

- [ ] **3.1** Create `NcdMonthlyFollowUpActionHelper.java` (§11)
  - Implement `NcdVisitActionHelper` interface
  - `postProcess()`: evaluate `alert_status`, create referral task if needed
  - Referral dedup: check `NcdCaseManagementDao.hasOpenReferral()` before creating task
  - When RED + YELLOW both true: include YELLOW flags (`is_side_effects_alert`, `is_missed_clinic_alert`) in referral payload
  - Constraint: max 1 active referral per client (§9.4)
- [ ] **3.2** Create `NcdCaseManagementInteractor.java` (§11)
  - Pre-fill logic for unresolved alerts (§9.5):
    - If previous `alert_status == 'red'` + open referral → pre-fill Section B danger signs + show warning banner
    - If previous RED + YELLOW → pre-fill Section A + inject `pending_yellow_review` flag
  - Wire form opening to `ncd_monthly_followup_form`
- [ ] **3.3** Create or extend visit activity to use `NcdCaseManagementInteractor`
  - `NcdCaseManagementVisitActivity` extends `NcdVisitActivity`
  - Register encounter type `NCD Monthly Follow-Up`
- [x] **3.4** Add `NCD Monthly Follow-Up` encounter type constant to `Constants.java`
- [ ] **3.5** Add form file name constants to `Constants.java` (`FORMS.NCD_MONTHLY_FOLLOWUP`)
- [ ] **3.6** Add string resources for visit titles and action names (EN + SW)

## Phase 4: Task Generation (recurring 30-day schedule)

Depends on: Phase 3 (visit activity exists to open from task)

- [ ] **4.1** Add NCD Monthly Follow-Up entry to `services.json` (§4.1)
  - Service type: `NCD Monthly Follow-Up`
  - Due offset: `+30d` from `ncd_confirmation_date`
  - Recurrence: next task due = `lastCompletionDate + 30d`
- [ ] **4.2** Extend task executor to generate NCD follow-up tasks with correct card display (§4.2)
  - Card shows: client name, village, diagnosis type (DM/HTN/DM+HTN), due date, alert badge
- [ ] **4.3** Wire task card tap to open `NcdCaseManagementVisitActivity` with correct form

## Phase 5: Client Case Summary UI (Profile Screen)

Depends on: Phase 1 (DAO for queries), Phase 3 (visit can be launched from profile)

- [ ] **5.1** Create `NcdCaseManagementProfileActivity` extending `NcdProfileActivity` (§5)
- [ ] **5.2** Implement header: client name, age/sex, village, diagnosis badge (DM/HTN/DM+HTN), confirmed date (§5.1)
- [ ] **5.3** Implement alert status banner — color + text + icon, sourced from last follow-up `alert_status` (§5.2)
  - RED: `#D32F2F` + "RED ALERT — Urgent Referral Required"
  - YELLOW: `#F9A825` + "YELLOW ALERT — Non-Emergency Referral Pending"
  - NONE: `#388E3C` + "No Active Alert"
- [ ] **5.4** Implement visit summary panel — last follow-up date, next due date, open referral status (§5.3)
- [ ] **5.5** Implement visit history list — last 3 completed follow-ups with date, alert chip, clinical flags (§5.4)
- [ ] **5.6** Wire "Start Visit" button to `NcdCaseManagementVisitActivity`
- [ ] **5.7** Register `NcdCaseManagementProfileActivity` in `AndroidManifest.xml`

## Phase 6: Register Lifecycle

Depends on: Phase 5 (profile screen hosts the close action)

- [ ] **6.1** Add "Close Record" menu action to `NcdCaseManagementProfileActivity` (§6)
- [ ] **6.2** Implement close event (`ncd_case_management_close`) with reasons: deceased, relocated, transferred, LTFU, data_error
- [ ] **6.3** Cancel all DUE/OVERDUE tasks and void open referrals on close
- [ ] **6.4** Filter closed records from active register query (`is_closed != 1`)

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
