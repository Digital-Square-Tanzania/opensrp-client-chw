package org.smartregister.chw.due;

import android.database.Cursor;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONObject;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorVisitCompletion;
import org.smartregister.chw.util.MotherMentorDailyVisitUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Recorded appointments for active primary Mother Mentor clients; no inferred clinical intervals. */
public class MotherMentorDueServiceProvider implements ModuleDueServiceProvider {
    public static final String SERVICE_CODE = "mother_mentor.followup";
    private static final Set<String> TERMINAL = new HashSet<>(Arrays.asList(
            "deceased", "relocated", "transferred_to_another_facility", "stopped_services"));
    private final SQLiteDatabase database;

    public MotherMentorDueServiceProvider() {
        this(null);
    }

    MotherMentorDueServiceProvider(SQLiteDatabase database) {
        this.database = database;
    }

    @Override
    public String getModuleId() {
        return LandingDueServiceRepository.MOTHER_MENTOR;
    }

    @Override
    public ModuleDueResult load(DueQuery query) {
        SQLiteDatabase db = database != null ? database
                : ChwApplication.getInstance().getRepository().getReadableDatabase();
        Map<String, Appointment> latest = new HashMap<>();
        Set<String> uncertain = new HashSet<>();
        Set<String> deletedForms = deletedForms(db, query, uncertain);
        List<StoredVisit> records = new ArrayList<>();
        Set<String> localDays = new HashSet<>();
        Set<String> localForms = new HashSet<>();
        Set<String> incompleteDays = new HashSet<>();
        Map<String, Long> deletedDays = new HashMap<>();
        String eligibility = " INNER JOIN ec_mothermentor_enrollment e ON e.base_entity_id = %s COLLATE NOCASE "
                + "WHERE e.is_closed = 0 AND (e.status IS NULL OR e.status = 'client') "
                + "AND (e.screening_status IS NULL OR e.screening_status != '-') ";
        String local = "SELECT v.base_entity_id, v.visit_id, v.visit_date, v.visit_json, v.updated_at, "
                + "v.form_submission_id, v.processed, 0 imported FROM visits v"
                + String.format(java.util.Locale.ROOT, eligibility, "v.base_entity_id") + "AND v.visit_type = ?";
        // Synced history is not always materialized in visits. Read it without mutating clinical
        // data; current local visits take precedence over historical events for the same visit day.
        String imported = "SELECT h.baseEntityId, h.eventId, 0, h.json, h.updatedAt, "
                + "h.formSubmissionId, 1, CASE WHEN lower(h.syncStatus) = 'synced' THEN 1 ELSE 2 END FROM event h"
                + String.format(java.util.Locale.ROOT, eligibility, "h.baseEntityId")
                + "AND h.eventType = ? AND h.validationStatus = 'Valid'";
        try (Cursor cursor = db.rawQuery(local + " UNION ALL " + imported,
                new String[]{Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES, Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES})) {
            while (cursor.moveToNext()) {
                String clientId = string(cursor, "base_entity_id");
                if (!query.getClientNames().containsKey(clientId)) continue;
                String form = string(cursor, "form_submission_id");
                boolean fromEvent = number(cursor, "imported") > 0;
                if (!fromEvent && form != null) localForms.add(form);
                boolean deleted = form != null && deletedForms.contains(form);
                // Unsynced events only provide the source date of an explicit deletion.
                if (number(cursor, "imported") == 2 && !deleted) continue;
                try {
                    JSONObject event = new JSONObject(string(cursor, "visit_json"));
                    Map<String, String> values = MotherMentorVisitCompletion.observationValues(event.optJSONArray("obs"));
                    Visit visit = new Visit();
                    long storedDate = number(cursor, "visit_date");
                    if (storedDate > 0) visit.setDate(new Date(storedDate));
                    Date clinicalDate = MotherMentorDailyVisitUtils.getVisitDate(visit, event, values);
                    if (clinicalDate == null || new LocalDate(clinicalDate).isAfter(query.getDate())) {
                        uncertain.add(clientId);
                        continue;
                    }
                    String dayKey = clientId + ":" + new LocalDate(clinicalDate);
                    if (deleted || event.optBoolean("voided", false)) {
                        // Editing changes the form ID while leaving the previous synced event.
                        // A deleted edit must also suppress its older same-day event versions.
                        deletedDays.put(dayKey, Math.max(deletedDays.getOrDefault(dayKey, Long.MIN_VALUE), eventTime(event)));
                        continue;
                    }
                    if (!fromEvent) {
                        localDays.add(dayKey);
                        if (number(cursor, "processed") != 1
                                || !MotherMentorVisitCompletion.isComplete(event.optJSONArray("obs"))) incompleteDays.add(dayKey);
                    }
                    records.add(new StoredVisit(clientId, dayKey, form, fromEvent,
                            number(cursor, "processed") == 1, event, values,
                            new Appointment(string(cursor, "visit_id"), clinicalDate.getTime(),
                                    eventTime(event), number(cursor, "updated_at"),
                                    values.get(DBConstants.KEY.NEXT_APPOINTMENT_DATE))));
                } catch (Exception malformedRecord) {
                    uncertain.add(clientId);
                }
            }
        }
        for (StoredVisit record : records) {
            Long deletedThrough = deletedDays.get(record.dayKey);
            if (record.imported && deletedThrough != null && record.appointment.eventTime <= deletedThrough) continue;
            if (record.imported && (localDays.contains(record.dayKey) || localForms.contains(record.form))) {
                if (incompleteDays.contains(record.dayKey)) uncertain.add(record.clientId);
                continue;
            }
            if (!record.processed || !MotherMentorVisitCompletion.isComplete(record.event.optJSONArray("obs"))) continue;
            String attendance = record.values.get(DBConstants.KEY.ATTENDANCE_TYPE);
            String outcome = record.values.get(DBConstants.KEY.FOLLOW_UP_STATUS);
            boolean fullVisit = MotherMentorVisitCompletion.isFullVisit(attendance, outcome);
            // A missed contact does not satisfy or cancel the outstanding appointment.
            if (!fullVisit && "not_available".equals(outcome)) continue;
            if (!fullVisit && !TERMINAL.contains(outcome)) {
                uncertain.add(record.clientId);
                continue;
            }
            Appointment candidate = record.appointment;
            if (!fullVisit) candidate = new Appointment(candidate.visitId, candidate.clinicalDate,
                    candidate.eventTime, candidate.updatedAt, null);
            Appointment previous = latest.get(record.clientId);
            if (previous == null || candidate.compareTo(previous) > 0) latest.put(record.clientId, candidate);
        }
        List<DueServiceItem> items = new ArrayList<>();
        for (Map.Entry<String, Appointment> entry : latest.entrySet()) {
            String clientId = entry.getKey();
            Appointment appointment = entry.getValue();
            if (uncertain.contains(clientId) || appointment.nextDate == null) continue;
            LocalDate due = parseAppointmentDate(appointment.nextDate);
            if (due == null || due.isBefore(new LocalDate(appointment.clinicalDate))) {
                uncertain.add(clientId);
                continue;
            }
            if (due.isAfter(query.getDate())) continue;
            items.add(new DueServiceItem(getModuleId(), clientId, clientId, SERVICE_CODE, SERVICE_CODE,
                    due, due.plusDays(1), due.isEqual(query.getDate())
                    ? DueServiceItem.Status.DUE : DueServiceItem.Status.OVERDUE));
        }
        return new ModuleDueResult(getModuleId(), uncertain.isEmpty()
                ? ModuleDueResult.Coverage.COMPLETE : ModuleDueResult.Coverage.PARTIAL,
                items, Collections.singleton(SERVICE_CODE));
    }

    private Set<String> deletedForms(SQLiteDatabase db, DueQuery query, Set<String> uncertain) {
        Set<String> deleted = new HashSet<>();
        try (Cursor cursor = db.rawQuery("SELECT baseEntityId, json, formSubmissionId FROM event "
                + "WHERE eventType = ? AND validationStatus = 'Valid'",
                new String[]{org.smartregister.chw.anc.util.Constants.EVENT_TYPE.DELETE_EVENT})) {
            while (cursor.moveToNext()) {
                String client = string(cursor, "baseEntityId");
                if (!query.getClientNames().containsKey(client)) continue;
                try {
                    JSONObject event = new JSONObject(string(cursor, "json"));
                    JSONObject details = event.optJSONObject("details");
                    String key = org.smartregister.chw.anc.util.Constants.JSON_FORM_EXTRA.DELETE_FORM_SUBMISSION_ID;
                    String target = details != null && details.has(key) ? details.optString(key)
                            : string(cursor, "formSubmissionId");
                    if (target != null && !target.isEmpty()) deleted.add(target);
                } catch (Exception invalidTombstone) {
                    uncertain.add(client);
                }
            }
        }
        return deleted;
    }

    private static final class StoredVisit {
        final String clientId;
        final String dayKey;
        final String form;
        final boolean imported;
        final boolean processed;
        final JSONObject event;
        final Map<String, String> values;
        final Appointment appointment;
        StoredVisit(String clientId, String dayKey, String form, boolean imported, boolean processed,
                    JSONObject event, Map<String, String> values, Appointment appointment) {
            this.clientId = clientId;
            this.dayKey = dayKey;
            this.form = form;
            this.imported = imported;
            this.processed = processed;
            this.event = event;
            this.values = values;
            this.appointment = appointment;
        }
    }

    static LocalDate parseAppointmentDate(String value) {
        if (value == null) return null;
        String text = value.trim();
        String format;
        if (text.matches("[0-9]{2}-[0-9]{2}-[0-9]{4}")) format = "dd-MM-yyyy";
        else if (text.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) format = "yyyy-MM-dd";
        else return null;
        try {
            return LocalDate.parse(text, DateTimeFormat.forPattern(format));
        } catch (IllegalArgumentException invalidDate) {
            return null;
        }
    }

    private static String string(Cursor cursor, String column) {
        return cursor.getString(cursor.getColumnIndexOrThrow(column));
    }

    private static long number(Cursor cursor, String column) {
        String value = string(cursor, column);
        try {
            return value == null ? 0 : Long.parseLong(value);
        } catch (NumberFormatException invalid) {
            return 0;
        }
    }

    private static long eventTime(JSONObject event) {
        Object value = event.opt("eventDate");
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try {
                return new DateTime(value).getMillis();
            } catch (IllegalArgumentException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static final class Appointment implements Comparable<Appointment> {
        final String visitId;
        final long clinicalDate;
        final long eventTime;
        final long updatedAt;
        final String nextDate;

        Appointment(String visitId, long clinicalDate, long eventTime, long updatedAt, String nextDate) {
            this.visitId = visitId == null ? "" : visitId;
            this.clinicalDate = clinicalDate;
            this.eventTime = eventTime;
            this.updatedAt = updatedAt;
            this.nextDate = nextDate;
        }

        @Override
        public int compareTo(Appointment other) {
            int compared = Long.compare(clinicalDate, other.clinicalDate);
            if (compared == 0) compared = Long.compare(eventTime, other.eventTime);
            if (compared == 0) compared = Long.compare(updatedAt, other.updatedAt);
            return compared == 0 ? visitId.compareTo(other.visitId) : compared;
        }
    }
}
