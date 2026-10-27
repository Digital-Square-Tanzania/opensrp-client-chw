# Module due-service providers

The landing dashboard and due-client register read the same offline provider contract. Module providers own eligibility, completion, scheduling, and source identity; the UI groups their results into distinct people.

## Current coverage

The first provider reports **recorded next appointments for active primary Mother Mentor clients**. It does not report IIT tracing, partner follow-up, Child EID milestones, or other modules. The UI names this coverage explicitly.

An appointment is due on its recorded local calendar date and overdue from the next day. Future appointments are omitted. This is an appointment-reminder policy; it does not invent clinical recurrence or grace intervals.

`MotherMentorDueServiceProvider` reads current `visits` rows and valid synced `event` history for `MotherMentor Services`, intersected with the authorized All Clients cohort and active primary `ec_mothermentor_enrollment`. Synced history is necessary because these events are not always materialized into the local visit table. A qualifying local service must be processed; both sources must satisfy the existing `MotherMentorVisitCompletion` checks. The latest completed full service replaces the previous appointment. A `not_available` contact preserves it; deceased, relocated, transferred, or stopped-service outcomes suppress it. Drafts do not complete an appointment.

Appointment dates come from `next_appointment_date` in visit JSON, with strict `dd-MM-yyyy` and `yyyy-MM-dd` support. Clinical chronology shares `MotherMentorDailyVisitUtils`: explicit `mothermentor_visit_date`, original event date, then persisted visit timestamp. This avoids treating an imported old visit as a new service. Equal clinical dates use original event time, then update time and stable visit ID. Current local visit rows shadow historical event records on the same client and clinical day, and records with the same form-submission ID are deduplicated. This preserves a local edit even when the old synced event remains. A pending local edit that shadows a synced appointment yields partial coverage. Valid Delete Event tombstones and explicitly voided records are excluded on reload.

Clients without a recorded appointment are not assigned an invented deadline. Unreadable records or invalid appointment dates produce partial coverage, with affected clients excluded from confirmed totals. Missing tables/provider failures produce an error rather than zero.

## Extension points

All contracts are app-owned under `org.smartregister.chw.due`; feature libraries do not need to depend on Core or dashboard UI.

1. Implement `ModuleDueServiceProvider` for a module and its explicitly supported service codes. Read persisted local sources on the worker thread. Reuse the module's existing scheduling/completion rules, including closure, transfer, expiry and cancellation behavior.
2. Return `DueServiceItem` records containing canonical person ID, source/profile ID, stable module and service codes, a client-local occurrence key, dates and evaluated status. Keep an occurrence key stable across rescheduling; separate concurrent obligations need separate keys. Household or encounter IDs must not become person IDs.
3. Register the provider in `LandingDueServiceRepository` and map its existing authorized, nonempty dashboard membership to its stable module ID. Keep separate IDs for AYP in-school, out-of-school and parental modules.
4. Add localized service/module labels and an authorized existing-profile destination in `DueServiceNavigation`. Providers never launch activities. Future service types that require different profiles must have explicit routes.
5. Add source and boundary tests, then validate the count-to-register and profile flow on a device.

`ModuleDueResult` distinguishes COMPLETE, PARTIAL, UNSUPPORTED and ERROR coverage, and declares the service codes it supports. COMPLETE means all records for those declared services were evaluated; it never means every service in that module is implemented. An unregistered enabled provider returns UNSUPPORTED. A failing provider does not remove successful results from another module.

## Aggregation and refresh

`DueQuery` supplies one local evaluation date and the authorized canonical client-name map. The registry filters foreign module IDs and people outside this cohort. `DueServicesSnapshot` deduplicates by module, person and occurrence key, preserves all service reasons, and assigns a person with any overdue service to the overdue bucket. Thus the two client totals do not overlap. Module/service totals may overlap and must not be added to obtain a person total.

Counts and rows use the same aggregation. The register reloads on entry and displays its evaluation date, so it can reflect newer local data than the dashboard after an intervening update. No patient lists are serialized into Intent extras or persisted in a second clinical database.

Visible screens refresh after successful event processing (`DueServicesChangedEvent`), sync completion, authenticated resume, and device date/time/timezone changes. Event notifications invalidate derived results; persisted module data remains authoritative. Generation guards discard stale asynchronous responses. The register uses `SecuredActivity`, clears patient rows on pause, and checks login and module permissions before opening a profile.

## Validation checklist for another provider

- Exact due/overdue/expiry dates and local midnight; distinguish an entire due window from an appointment scheduled exactly today.
- No-client, disabled-module, unauthorized-person and inactive-enrollment cases.
- Completion, draft, missed contact, cancellation, edit, reschedule, deletion and imported-history ordering.
- Multiple services for one person, same local occurrence keys for different people, and cross-module overdue precedence.
- Unsupported/failed/partial coverage versus a successful empty result.
- Dashboard count, filtered register rows, existing profile route, session protection and offline reload.
