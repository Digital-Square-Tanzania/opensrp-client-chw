package org.smartregister.chw.repository;

import android.database.Cursor;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.json.JSONObject;
import org.smartregister.chw.mothermentor.util.MotherMentorUtil;
import org.smartregister.chw.util.MotherMentorChildEidFormUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.repository.AllSharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The child keeps their own client/family identity; the mother is a separate relationship. */
public class MotherMentorChildEidRepository {
    public static final String TABLE = "ec_mothermentor_enroll_child_eid";
    public static final String[] LINK_COLUMNS = {"mother_base_entity_id", "child_name", "child_sex",
            "child_dob"};
    private final SQLiteDatabase database;

    public MotherMentorChildEidRepository(SQLiteDatabase database) {
        this.database = database;
    }

    public static void ensureColumns(SQLiteDatabase database) {
        Set<String> columns = new HashSet<>();
        try (Cursor cursor = database.rawQuery("PRAGMA table_info(" + TABLE + ")", null)) {
            while (cursor.moveToNext()) columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        }
        if (columns.isEmpty()) return;
        for (String column : LINK_COLUMNS) {
            if (!columns.contains(column)) database.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN " + column + " VARCHAR");
        }
    }

    public Person findMother(String id) {
        return first("SELECT m.*, e.ctc_number FROM ec_family_member m JOIN ec_family f ON f.base_entity_id = m.relational_id "
                + "JOIN ec_mothermentor_enrollment e "
                + "ON e.base_entity_id = m.base_entity_id WHERE m.base_entity_id = ? "
                + "AND m.is_closed = 0 AND e.is_closed = 0 AND f.is_closed = 0", new String[]{id});
    }

    public Person findClient(String id) {
        return first("SELECT m.* FROM ec_family_member m JOIN ec_family f ON f.base_entity_id = m.relational_id "
                + "WHERE m.base_entity_id = ? AND m.is_closed = 0 AND f.is_closed = 0", new String[]{id});
    }

    public boolean canEnrollChild(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        Person client = findClient(id);
        return client != null && MotherMentorChildEidFormUtils.isEligibleChild(
                client.values.get("dob"), client.values.get("dob_unknown")) && existingEnrollment(id).isEmpty();
    }

    public List<Person> candidates(String motherId, String familyId, boolean familyOnly) {
        List<Person> people = new ArrayList<>();
        String sql = candidateSql(familyOnly);
        try (Cursor cursor = database.rawQuery(sql, familyOnly
                ? new String[]{motherId, familyId} : new String[]{motherId})) {
            while (cursor.moveToNext()) {
                Person candidate = person(cursor);
                if (MotherMentorChildEidFormUtils.isEligibleChild(candidate.values.get("dob"), candidate.values.get("dob_unknown"))) {
                    people.add(candidate);
                }
            }
        }
        return people;
    }

    public static String candidateSql(boolean familyOnly) {
        // Historical enrollments without a mother ID can be linked without creating another client.
        return "SELECT m.* FROM ec_family_member m JOIN ec_family f ON f.base_entity_id = m.relational_id "
                + "WHERE m.is_closed = 0 AND f.is_closed = 0 AND m.base_entity_id <> ? "
                + (familyOnly ? "AND m.relational_id = ? " : "")
                + "AND NOT EXISTS (SELECT 1 FROM " + TABLE + " p WHERE p.base_entity_id = m.base_entity_id "
                + "AND p.is_closed = 0 AND trim(coalesce(p.mother_base_entity_id, '')) <> '') "
                + "ORDER BY m.first_name COLLATE NOCASE, m.last_name COLLATE NOCASE, m.base_entity_id";
    }

    public Map<String, String> existingEnrollment(String childId) {
        try (Cursor cursor = database.rawQuery("SELECT * FROM " + TABLE
                + " WHERE base_entity_id = ? AND is_closed = 0", new String[]{childId})) {
            return cursor.moveToFirst() ? values(cursor) : new HashMap<>();
        }
    }

    public JSONObject prepareForm(JSONObject form, String motherId, String childId) throws Exception {
        Person mother = findMother(motherId);
        Person child = findClient(childId);
        Map<String, String> previous = existingEnrollment(childId);
        validateLink(mother, child, previous.get("mother_base_entity_id"));
        return MotherMentorChildEidFormUtils.prepare(form, mother.values, child.values, previous);
    }

    public void save(JSONObject form, String motherId, String childId, AllSharedPreferences preferences) throws Exception {
        // Recheck eligibility and restore trusted IDs when the form returns, including after recreation.
        Person mother = findMother(motherId);
        Person child = findClient(childId);
        validateLink(mother, child, existingEnrollment(childId).get("mother_base_entity_id"));
        MotherMentorChildEidFormUtils.bindIdentity(form, mother.values, child.values);
        Event event = org.smartregister.chw.mothermentor.util.JsonFormUtils.processJsonForm(
                preferences, form.toString(), TABLE);
        if (event == null || !childId.equals(event.getBaseEntityId())) throw new IllegalStateException("Invalid child event");
        MotherMentorUtil.processEvent(preferences, event);
        if (!motherId.equals(existingEnrollment(childId).get("mother_base_entity_id"))) {
            throw new IllegalStateException("Child enrollment was not projected");
        }
    }

    private static void validateLink(Person mother, Person child, String linkedMother) {
        if (mother == null || child == null || mother.id().equals(child.id())
                || !MotherMentorChildEidFormUtils.isEligibleChild(child.values.get("dob"), child.values.get("dob_unknown"))
                || (linkedMother != null && !linkedMother.trim().isEmpty())) {
            throw new IllegalArgumentException("Mother or child is no longer eligible for linking");
        }
    }

    private Person first(String sql, String[] arguments) {
        try (Cursor cursor = database.rawQuery(sql, arguments)) {
            return cursor.moveToFirst() ? person(cursor) : null;
        }
    }

    private static Person person(Cursor cursor) { return new Person(values(cursor)); }

    private static Map<String, String> values(Cursor cursor) {
        Map<String, String> values = new HashMap<>();
        for (String column : cursor.getColumnNames()) values.put(column, cursor.getString(cursor.getColumnIndexOrThrow(column)));
        return values;
    }

    public static class Person {
        public final Map<String, String> values;
        public Person(Map<String, String> values) { this.values = values; }
        public String id() { return values.get("base_entity_id"); }
        public String familyId() { return values.get("relational_id"); }
        public String name() { return MotherMentorChildEidFormUtils.name(values); }
        public String uniqueId() { return values.get("unique_id"); }
    }
}
