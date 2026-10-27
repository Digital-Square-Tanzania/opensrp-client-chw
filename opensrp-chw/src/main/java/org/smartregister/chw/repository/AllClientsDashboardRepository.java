package org.smartregister.chw.repository;

import android.database.Cursor;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.model.NavigationOption;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.model.NavigationModelFlv;
import org.smartregister.chw.provider.ChwAllClientsRegisterQueryProvider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.TimeZone;

/** Offline overview of the same role-scoped cohort used by the All Clients register. */
public class AllClientsDashboardRepository {
    public static final String INDEPENDENT = "Independent";
    public static final String MOTHER_MENTOR = "Mother Mentor";
    public static final String AYP = "AYP";
    public static final String NCD = "NCD";
    public static final String HIVST = "HIVST";

    private final SQLiteDatabase database;
    private final String rosterTemplate;
    private final Set<String> availableMenus;

    public AllClientsDashboardRepository() {
        this.database = null;
        this.rosterTemplate = null;
        this.availableMenus = null;
    }

    AllClientsDashboardRepository(SQLiteDatabase database, String rosterTemplate,
                                  Set<String> availableMenus) {
        this.database = database;
        this.rosterTemplate = rosterTemplate;
        this.availableMenus = availableMenus;
    }

    /** Call on a worker thread. Registration dates deliberately do not use last_interacted_with. */
    public Snapshot load(long now) {
        SQLiteDatabase db = database != null ? database
                : ChwApplication.getInstance().getRepository().getReadableDatabase();
        String template = rosterTemplate != null ? rosterTemplate
                : new ChwAllClientsRegisterQueryProvider().mainSelectWhereIDsIn();
        Map<String, Long> registrations = registrationDates(db);
        Map<String, Client> clients = new LinkedHashMap<>();
        String sql = "WITH roster AS (" + rosterQuery(template) + ") "
                + "SELECT roster.base_entity_id, roster.first_name, roster.middle_name, roster.last_name, "
                + "roster.home_address, roster.register_type, client.json client_json "
                + "FROM roster "
                + "LEFT JOIN client ON client.baseEntityId = roster.base_entity_id";
        try (Cursor cursor = db.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                String id = value(cursor, "base_entity_id");
                Client client = clients.get(id);
                if (client == null) {
                    Long registration = registrations.get(id);
                    long registeredAt = registration == null
                            ? clientCreatedAt(value(cursor, "client_json")) : registration;
                    client = new Client(id, name(cursor), value(cursor, "home_address"), registeredAt);
                    clients.put(id, client);
                }
                client.addModule(value(cursor, "register_type"));
            }
        }

        Set<String> menus = availableMenus;
        if (menus == null) {
            menus = new HashSet<>();
            for (NavigationOption option : new NavigationModelFlv().getNavigationItems()) {
                menus.add(option.getMenuTitle());
            }
        }
        addRegisterMemberships(db, clients, menus);
        return summarize(clients, now);
    }

    private Map<String, Long> registrationDates(SQLiteDatabase db) {
        Map<String, Long> registrations = new HashMap<>();
        try (Cursor cursor = db.rawQuery("SELECT baseEntityId, eventDate FROM event WHERE eventType = ?",
                new String[]{CoreConstants.EventType.FAMILY_MEMBER_REGISTRATION})) {
            while (cursor.moveToNext()) {
                long date = parseDate(value(cursor, "eventDate"));
                if (date <= 0) continue;
                String id = value(cursor, "baseEntityId");
                Long previous = registrations.get(id);
                if (previous == null || date < previous) registrations.put(id, date);
            }
        }
        return registrations;
    }

    static String rosterQuery(String template) {
        // The provider has a separate iCCM-role query. Keep its conditions and all ID slots.
        String query = template.replace("%s",
                "SELECT object_id FROM ec_family_member_search WHERE date_removed IS NULL").trim();
        // iCCM rows use the encounter ID to open its service profile. The overview counts people
        // and joins their registration dates, so normalize only this private projection.
        query = query.replace("ec_iccm_enrollment.base_entity_id,", "ec_family_member.base_entity_id,");
        return query.endsWith(";") ? query.substring(0, query.length() - 1) : query;
    }

    private void addRegisterMemberships(SQLiteDatabase db, Map<String, Client> clients,
                                       Set<String> menus) {
        if (clients.isEmpty()) return;
        // Row tags choose a profile by priority, so they cannot describe concurrent enrollments.
        // Enrich only this cohort with active memberships in role-available programs. Related
        // registers share one module; module counts can overlap, while total clients never do.
        String[][] memberships = {
                {CoreConstants.DrawerMenu.MOTHER_MENTOR, MOTHER_MENTOR, "ec_mothermentor_enrollment", "base_entity_id",
                        "is_closed = 0 AND (status IS NULL OR status = 'client') AND (screening_status IS NULL OR screening_status != '-')"},
                {CoreConstants.DrawerMenu.MOTHER_MENTOR, MOTHER_MENTOR, "ec_mothermentor_enroll_it", "base_entity_id", "is_closed = 0"},
                {CoreConstants.DrawerMenu.MOTHER_MENTOR, MOTHER_MENTOR, "ec_mothermentor_enroll_partner", "base_entity_id", "is_closed = 0"},
                {CoreConstants.DrawerMenu.MOTHER_MENTOR, MOTHER_MENTOR, "ec_mothermentor_enroll_child_eid", "base_entity_id", "is_closed = 0"},
                {CoreConstants.DrawerMenu.ANC, "ANC", "ec_anc_register", "base_entity_id", "is_closed = 0"},
                {CoreConstants.DrawerMenu.PNC, "PNC", "ec_pregnancy_outcome", "base_entity_id", "is_closed = 0 AND delivery_date IS NOT NULL"},
                {CoreConstants.DrawerMenu.CHILD_CLIENTS, "Child", "ec_child", "base_entity_id",
                        "date(dob) <= date('now') AND EXISTS (SELECT 1 FROM ec_family_member m WHERE m.base_entity_id = ec_child.base_entity_id AND m.is_closed = 0 AND m.date_removed IS NULL)"},
                {CoreConstants.DrawerMenu.CBHS_CLIENTS, "CBHS", "ec_cbhs_register", "base_entity_id", "is_closed = 0"},
                {CoreConstants.DrawerMenu.HPS, "iCCHW", "ec_hps_client_register", "base_entity_id",
                        "is_closed = 0 AND does_the_client_consent_to_be_enrolled_in_hps_services = 'yes'"},
                {CoreConstants.DrawerMenu.ICCM, "iCCM", "ec_iccm_enrollment", "entity_id",
                        "is_closed = 0 AND date('now') <= date(last_interacted_with / 1000, 'unixepoch', 'localtime')"},
                {CoreConstants.DrawerMenu.MALARIA, "Malaria", "ec_malaria_confirmation", "base_entity_id",
                        "is_closed = 0 AND malaria = 1 AND datetime('now') <= datetime(last_interacted_with / 1000, 'unixepoch', 'localtime', '+15 days')"},
                {CoreConstants.DrawerMenu.TB_CLIENTS, "TB", "ec_tb_register", "base_entity_id", "is_closed = 0 AND tb_case_closure_date IS NULL"},
                {CoreConstants.DrawerMenu.FAMILY_PLANNING, "Family Planning", "ec_family_planning", "base_entity_id", "is_closed = 0"},
                {CoreConstants.DrawerMenu.AGYW, "AGYW", "ec_agyw_register", "base_entity_id", "is_closed = 0"},
                {CoreConstants.DrawerMenu.KVP_PrEP, "KVP/PrEP", "ec_kvp_prep_register", "base_entity_id", "is_closed = 0"},
                {CoreConstants.DrawerMenu.TBLEPROSY, "TB/Leprosy", "ec_tbleprosy_screening", "base_entity_id",
                        "is_closed = 0 AND (status IS NULL OR status = 'client') AND screening_status != '-'"},
                {CoreConstants.DrawerMenu.HARM_REDUCTION, "HARM REDUCTION", "ec_harm_reduction_risk_assessment", "base_entity_id", "is_closed = 0"},
                {CoreConstants.DrawerMenu.HARM_REDUCTION_SOBER_HOUSE, "HARM REDUCTION", "ec_harm_reduction_sober_house_enrollment", "base_entity_id",
                        "is_closed = 0 AND detoxification_done = 'yes'"},
                {CoreConstants.DrawerMenu.AYP_IN_SCHOOL, AYP, "ec_ayp_in_school_enrollment", "base_entity_id", "is_closed = 0"},
                {CoreConstants.DrawerMenu.AYP_OUT_SCHOOL, AYP, "ec_ayp_out_school_enrollment", "base_entity_id", "is_closed = 0"},
                {CoreConstants.DrawerMenu.AYP_PARENTAL, AYP, "ec_ayp_parental_enrollment", "base_entity_id", "is_closed = 0"},
                {CoreConstants.DrawerMenu.CECAP, "Reproductive Cancers", "ec_cecap_register", "base_entity_id", "is_closed = 0"},
                {CoreConstants.DrawerMenu.AYSRH, "AYSRH", "ec_asrh_register", "base_entity_id", "is_closed = 0"},
                {CoreConstants.DrawerMenu.SBC, "SBC", "ec_sbc_register", "base_entity_id", "is_closed = 0"},
                {CoreConstants.DrawerMenu.HIV_SELF_TESTING, HIVST,
                        org.smartregister.chw.hivst.util.Constants.TABLES.HIVST_REGISTER, "base_entity_id", "is_closed = 0"}
        };
        for (String[] membership : memberships) {
            if (menus.contains(membership[0])) {
                addMemberships(db, clients, membership[1], membership[2], membership[3], membership[4]);
            }
        }
        if (menus.contains(CoreConstants.DrawerMenu.NCD)) {
            addMemberships(db, clients, NCD,
                    org.smartregister.chw.ncd.util.Constants.TABLES.NCD_ENROLLMENT, "base_entity_id",
                    "IFNULL(is_closed, 0) = 0");
            String table = org.smartregister.chw.ncd.util.Constants.TABLES.DIABETES_HYPERTENSION_CONFIRMATION;
            if (hasTable(db, table)) {
                // NcdDao also recognizes the latest confirmed diagnosis without an active enrollment.
                try (Cursor cursor = db.rawQuery("SELECT base_entity_id, diabetes_result, hypertension_result "
                        + "FROM " + table + " ORDER BY COALESCE(last_interacted_with, visit_date) DESC", null)) {
                    Set<String> seen = new HashSet<>();
                    while (cursor.moveToNext()) {
                        String id = value(cursor, "base_entity_id");
                        if (seen.add(id) && clients.containsKey(id)
                                && (org.smartregister.chw.dao.NcdDao.ConfirmationStatus.isPositive(value(cursor, "diabetes_result"))
                                || org.smartregister.chw.dao.NcdDao.ConfirmationStatus.isPositive(value(cursor, "hypertension_result")))) {
                            clients.get(id).addModule(NCD);
                        }
                    }
                }
            }
        }
    }

    private void addMemberships(SQLiteDatabase db, Map<String, Client> clients, String module,
                                String table, String identity, String condition) {
        if (!hasTable(db, table)) return;
        try (Cursor cursor = db.rawQuery("SELECT DISTINCT " + identity + " FROM " + table
                + " WHERE " + condition, null)) {
            while (cursor.moveToNext()) {
                Client client = clients.get(cursor.getString(0));
                if (client != null) client.addModule(module);
            }
        }
    }

    private boolean hasTable(SQLiteDatabase db, String table) {
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
                new String[]{table})) {
            return cursor.moveToFirst();
        }
    }

    private Snapshot summarize(Map<String, Client> clients, long now) {
        Calendar monday = Calendar.getInstance();
        monday.setTimeInMillis(now);
        monday.add(Calendar.DAY_OF_MONTH, -((monday.get(Calendar.DAY_OF_WEEK) + 5) % 7));
        monday.set(Calendar.HOUR_OF_DAY, 0);
        monday.set(Calendar.MINUTE, 0);
        monday.set(Calendar.SECOND, 0);
        monday.set(Calendar.MILLISECOND, 0);
        int independent = 0;
        int newThisWeek = 0;
        Map<String, Integer> counts = new TreeMap<>();
        List<RecentClient> recent = new ArrayList<>();
        for (Client client : clients.values()) {
            if (client.modules.isEmpty()) independent++;
            for (String module : client.modules) counts.put(module, counts.getOrDefault(module, 0) + 1);
            if (client.registeredAt > 0 && client.registeredAt <= now) {
                if (client.registeredAt >= monday.getTimeInMillis()) newThisWeek++;
                recent.add(new RecentClient(client.id, client.name, client.address,
                        client.modules.isEmpty() ? INDEPENDENT : client.modules.iterator().next(), client.registeredAt));
            }
        }
        recent.sort(Comparator.comparingLong(RecentClient::getRegisteredAt).reversed()
                .thenComparing(RecentClient::getBaseEntityId));
        if (recent.size() > 3) recent = new ArrayList<>(recent.subList(0, 3));
        List<ModuleCount> breakdown = new ArrayList<>();
        for (Map.Entry<String, Integer> count : counts.entrySet()) {
            breakdown.add(new ModuleCount(count.getKey(), count.getValue()));
        }
        if (independent > 0) breakdown.add(new ModuleCount(INDEPENDENT, independent));
        return new Snapshot(clients.size(), independent, counts.size(), newThisWeek, breakdown, recent);
    }

    private static long clientCreatedAt(String json) {
        if (json.isEmpty()) return 0;
        try {
            Object date = new JSONObject(json).opt("dateCreated");
            if (date instanceof Number) return ((Number) date).longValue();
            if (date instanceof String) return parseDate((String) date);
        } catch (JSONException | IllegalArgumentException ignored) {
            // An unknown registration date must not turn a recent edit into a new registration.
        }
        return 0;
    }

    private static long parseDate(String value) {
        if (value.isEmpty()) return 0;
        try {
            DateTimeZone zone = DateTimeZone.forTimeZone(TimeZone.getDefault());
            // EventClientRepository persists dates without an offset in the device's local time.
            // SQLite strftime would interpret these as UTC, moving Sunday records into Monday.
            if (value.length() == 19 && value.charAt(10) == ' ') {
                return DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withZone(zone)
                        .parseDateTime(value).getMillis();
            }
            return ISODateTimeFormat.dateOptionalTimeParser().withZone(zone)
                    .parseDateTime(value).getMillis();
        } catch (IllegalArgumentException ignored) {
            return 0;
        }
    }

    private static String value(Cursor cursor, String column) {
        String value = cursor.getString(cursor.getColumnIndexOrThrow(column));
        return value == null ? "" : value;
    }

    private static String name(Cursor cursor) {
        StringBuilder name = new StringBuilder();
        for (String column : new String[]{"first_name", "middle_name", "last_name"}) {
            String part = value(cursor, column).trim();
            if (!part.isEmpty()) {
                if (name.length() > 0) name.append(' ');
                name.append(part);
            }
        }
        return name.toString();
    }

    private static class Client {
        final String id;
        final String name;
        final String address;
        final long registeredAt;
        final Set<String> modules = new TreeSet<>();

        Client(String id, String name, String address, long registeredAt) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.registeredAt = registeredAt;
        }

        void addModule(String type) {
            String key = type.trim().toUpperCase(Locale.ROOT);
            if (key.isEmpty() || key.equals("INDEPENDENT")) return;
            if (key.startsWith("MOTHER MENTOR")) type = MOTHER_MENTOR;
            else if (key.startsWith("AYP")) type = AYP;
            else if (key.startsWith("HARM REDUCTION")) type = "HARM REDUCTION";
            modules.add(type);
        }
    }

    public static final class Snapshot {
        private final int totalClients;
        private final int independentClients;
        private final int activeModules;
        private final int newThisWeek;
        private final List<ModuleCount> breakdown;
        private final List<RecentClient> recentClients;

        public Snapshot(int totalClients, int independentClients, int activeModules, int newThisWeek,
                        List<ModuleCount> breakdown, List<RecentClient> recentClients) {
            this.totalClients = totalClients;
            this.independentClients = independentClients;
            this.activeModules = activeModules;
            this.newThisWeek = newThisWeek;
            this.breakdown = Collections.unmodifiableList(new ArrayList<>(breakdown));
            this.recentClients = Collections.unmodifiableList(new ArrayList<>(recentClients));
        }

        public int getTotalClients() { return totalClients; }
        public int getIndependentClients() { return independentClients; }
        public int getActiveModules() { return activeModules; }
        public int getNewThisWeek() { return newThisWeek; }
        public List<ModuleCount> getBreakdown() { return breakdown; }
        public List<RecentClient> getRecentClients() { return recentClients; }
    }

    public static final class ModuleCount {
        private final String registerType;
        private final int count;

        public ModuleCount(String registerType, int count) {
            this.registerType = registerType;
            this.count = count;
        }

        public String getRegisterType() { return registerType; }
        public int getCount() { return count; }
    }

    public static final class RecentClient {
        private final String baseEntityId;
        private final String name;
        private final String address;
        private final String registerType;
        private final long registeredAt;

        public RecentClient(String baseEntityId, String name, String address, String registerType, long registeredAt) {
            this.baseEntityId = baseEntityId;
            this.name = name;
            this.address = address;
            this.registerType = registerType;
            this.registeredAt = registeredAt;
        }

        public String getBaseEntityId() { return baseEntityId; }
        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getRegisterType() { return registerType; }
        public long getRegisteredAt() { return registeredAt; }
    }
}
