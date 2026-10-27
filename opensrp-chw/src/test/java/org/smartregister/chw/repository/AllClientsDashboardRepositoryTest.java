package org.smartregister.chw.repository;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.util.ChwQueryConstant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class AllClientsDashboardRepositoryTest {
    private static final String ROSTER = "SELECT * FROM roster_fixture WHERE base_entity_id IN (%s);";
    private SQLiteDatabase db;
    private net.zetetic.database.sqlcipher.SQLiteDatabase cipher;
    private TimeZone originalZone;
    private long now;

    @Before
    public void setUp() {
        originalZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Dar_es_Salaam"));
        now = time("2026-10-27T12:00:00+03:00");
        db = SQLiteDatabase.create(null);
        db.execSQL("CREATE TABLE ec_family_member_search (object_id TEXT, date_removed TEXT)");
        db.execSQL("CREATE TABLE event (baseEntityId TEXT, eventType TEXT, eventDate TEXT)");
        db.execSQL("CREATE TABLE client (baseEntityId TEXT PRIMARY KEY, json TEXT)");
        db.execSQL("CREATE TABLE roster_fixture (base_entity_id TEXT, first_name TEXT, middle_name TEXT, "
                + "last_name TEXT, home_address TEXT, register_type TEXT, last_interacted_with INTEGER)");
        cipher = mock(net.zetetic.database.sqlcipher.SQLiteDatabase.class);
        when(cipher.rawQuery(anyString(), any(String[].class)))
                .thenAnswer(call -> db.rawQuery(call.getArgument(0), call.getArgument(1)));
        // Mockito's any(Class) excludes the null selection arguments used by enrollment queries.
        when(cipher.rawQuery(anyString(), org.mockito.ArgumentMatchers.isNull()))
                .thenAnswer(call -> db.rawQuery(call.getArgument(0), null));
    }

    @After
    public void tearDown() {
        db.close();
        TimeZone.setDefault(originalZone);
    }

    @Test
    public void emptyCohortHasZeroCountsAndNoInventedActivity() {
        AllClientsDashboardRepository.Snapshot result = repository().load(now);
        assertEquals(0, result.getTotalClients());
        assertEquals(0, result.getActiveModules());
        assertEquals(0, result.getIndependentClients());
        assertEquals(0, result.getNewThisWeek());
        assertTrue(result.getBreakdown().isEmpty());
        assertTrue(result.getRecentClients().isEmpty());
    }

    @Test
    public void clientsAreUniqueAndRelatedRegistersAreOneModule() {
        add("mother", "Mother Mentor");
        row("mother", "MOTHER MENTOR PARTNER");
        row("mother", "Independent");
        row("mother", "ANC");
        add("partner", "Mother Mentor IIT");
        add("child", "MOTHER MENTOR CHILD EID");
        add("ayp", "AYP out of school");
        add("sober", "Harm Reduction Sober House");
        row("sober", "HARM REDUCTION");
        add("free", "Independent");
        add("household", null);

        AllClientsDashboardRepository.Snapshot result = repository().load(now);
        assertEquals(7, result.getTotalClients());
        assertEquals(2, result.getIndependentClients());
        assertEquals(4, result.getActiveModules());
        assertEquals(Integer.valueOf(3), breakdown(result).get("Mother Mentor"));
        assertEquals(Integer.valueOf(1), breakdown(result).get("HARM REDUCTION"));
        assertEquals(Integer.valueOf(1), breakdown(result).get("AYP"));
        assertEquals(Integer.valueOf(2), breakdown(result).get("Independent"));
    }

    @Test
    public void registrationDatesUseFirstRegistrationAndMondayBoundaryNotRecentEdits() {
        add("old-edited", "Independent");
        registration("old-edited", "2026-09-01T09:00:00+03:00");
        registration("old-edited", "2026-10-27T11:00:00+03:00");
        event("old-edited", "Update Family Member Registration", "2026-10-27T12:00:00+03:00");
        add("sunday", null);
        registration("sunday", "2026-10-25T23:59:59+03:00");
        add("monday", null);
        registration("monday", "2026-10-26T00:00:00+03:00");
        add("today", null);
        registration("today", "2026-10-27T12:00:00+03:00");
        add("future", null);
        registration("future", "2026-10-27T12:00:01+03:00");
        add("unknown", null);

        AllClientsDashboardRepository.Snapshot result = repository().load(now);
        assertEquals(2, result.getNewThisWeek());
        assertEquals(3, result.getRecentClients().size());
        assertEquals("today", result.getRecentClients().get(0).getBaseEntityId());
        assertEquals("monday", result.getRecentClients().get(1).getBaseEntityId());
        assertEquals("sunday", result.getRecentClients().get(2).getBaseEntityId());
    }

    @Test
    public void missingRegistrationUsesOriginalClientCreationAndInvalidDatesStayUnknown() {
        add("imported", "Independent");
        db.execSQL("INSERT INTO client VALUES (?, ?)", new Object[]{"imported",
                "{\"dateCreated\":\"2026-10-26T15:00:00+03:00\"}"});
        add("invalid", "Independent");
        db.execSQL("INSERT INTO client VALUES ('invalid', '{\"dateCreated\":\"not a date\"}')");
        AllClientsDashboardRepository.Snapshot result = repository().load(now);
        assertEquals(1, result.getNewThisWeek());
        assertEquals(1, result.getRecentClients().size());
        assertEquals(time("2026-10-26T15:00:00+03:00"), result.getRecentClients().get(0).getRegisteredAt());
        assertEquals("imported Test", result.getRecentClients().get(0).getName());
        assertEquals("Saranga", result.getRecentClients().get(0).getAddress());
    }

    @Test
    public void persistedLocalRegistrationTimestampsRespectLocalWeekAndCurrentTime() {
        add("sunday", null);
        registration("sunday", "2026-10-25 23:59:59");
        add("monday", null);
        registration("monday", "2026-10-26 00:00:00");
        add("recent", null);
        registration("recent", "2026-10-27 11:59:00");
        add("future", null);
        registration("future", "2026-10-27 12:00:01");
        AllClientsDashboardRepository.Snapshot result = repository().load(now);
        assertEquals(2, result.getNewThisWeek());
        assertEquals(3, result.getRecentClients().size());
        assertEquals("recent", result.getRecentClients().get(0).getBaseEntityId());
        assertEquals(time("2026-10-27T11:59:00+03:00"), result.getRecentClients().get(0).getRegisteredAt());
        assertEquals("sunday", result.getRecentClients().get(2).getBaseEntityId());
    }

    @Test
    public void removedClientsAndClientsOutsideTheRoleCohortStayExcluded() {
        add("visible", null);
        add("removed", "Mother Mentor");
        db.execSQL("UPDATE ec_family_member_search SET date_removed = '2026-10-27' WHERE object_id = 'removed'");
        row("not-in-search", "ANC");
        assertEquals(1, repository().load(now).getTotalClients());
        String roleQuery = ROSTER.replace(";", " AND base_entity_id = 'visible';");
        add("not-in-role", "ANC");
        assertEquals(1, new AllClientsDashboardRepository(cipher, roleQuery, Collections.emptySet())
                .load(now).getTotalClients());
    }

    @Test
    public void additionalActiveEnrollmentsPreventFalseIndependentCountsWithinAllowedModules() {
        for (String id : Arrays.asList("school", "parent", "self-test", "closed", "unlinked")) add(id, null);
        db.execSQL("CREATE TABLE ec_ayp_in_school_enrollment (base_entity_id TEXT, is_closed INTEGER)");
        db.execSQL("CREATE TABLE ec_ayp_parental_enrollment (base_entity_id TEXT, is_closed INTEGER)");
        db.execSQL("CREATE TABLE ec_hivst_register (base_entity_id TEXT, is_closed INTEGER)");
        db.execSQL("INSERT INTO ec_ayp_in_school_enrollment VALUES ('school', 0), ('closed', 1), ('outside', 0)");
        db.execSQL("INSERT INTO ec_ayp_parental_enrollment VALUES ('parent', 0), ('school', 0)");
        db.execSQL("INSERT INTO ec_hivst_register VALUES ('self-test', 0), ('closed', 1)");
        Set<String> menus = new HashSet<>(Arrays.asList(CoreConstants.DrawerMenu.AYP_IN_SCHOOL,
                CoreConstants.DrawerMenu.AYP_PARENTAL, CoreConstants.DrawerMenu.HIV_SELF_TESTING));
        AllClientsDashboardRepository.Snapshot result = new AllClientsDashboardRepository(cipher, ROSTER, menus).load(now);
        assertEquals(5, result.getTotalClients());
        assertEquals(2, result.getIndependentClients());
        assertEquals(2, result.getActiveModules());
        assertEquals(Integer.valueOf(2), breakdown(result).get("AYP"));
        assertEquals(Integer.valueOf(1), breakdown(result).get("HIVST"));
        assertEquals(0, repository().load(now).getActiveModules());
    }

    @Test
    public void ncdUsesActiveEnrollmentOrLatestPositiveConfirmation() {
        for (String id : Arrays.asList("active", "confirmed", "negative-now", "closed")) add(id, null);
        String enrollment = org.smartregister.chw.ncd.util.Constants.TABLES.NCD_ENROLLMENT;
        String confirmation = org.smartregister.chw.ncd.util.Constants.TABLES.DIABETES_HYPERTENSION_CONFIRMATION;
        db.execSQL("CREATE TABLE " + enrollment + " (base_entity_id TEXT, is_closed INTEGER)");
        db.execSQL("CREATE TABLE " + confirmation + " (base_entity_id TEXT, diabetes_result TEXT, "
                + "hypertension_result TEXT, last_interacted_with INTEGER, visit_date TEXT)");
        db.execSQL("INSERT INTO " + enrollment + " VALUES ('active', NULL), ('closed', 1)");
        db.execSQL("INSERT INTO " + confirmation + " VALUES ('confirmed', 'positive', NULL, 2, NULL), "
                + "('negative-now', 'yes', NULL, 1, NULL), ('negative-now', 'negative', NULL, 2, NULL)");
        AllClientsDashboardRepository.Snapshot result = new AllClientsDashboardRepository(cipher, ROSTER,
                Collections.singleton(CoreConstants.DrawerMenu.NCD)).load(now);
        assertEquals(2, result.getIndependentClients());
        assertEquals(Integer.valueOf(2), breakdown(result).get("NCD"));
    }

    @Test
    public void actualAllClientsQueryKeepsMotherMentorClosureAndScreeningRules() {
        createRegisterTables();
        actualClient("mother"); actualClient("closed"); actualClient("screening"); actualClient("partner");
        db.execSQL("INSERT INTO ec_mothermentor_enrollment (base_entity_id,is_closed,status,screening_status) "
                + "VALUES ('mother',0,'client','positive'), ('closed',1,'client','positive'), ('screening',0,'client','-')");
        db.execSQL("INSERT INTO ec_mothermentor_enroll_partner (base_entity_id,is_closed) VALUES ('partner',0)");
        AllClientsDashboardRepository.Snapshot result = new AllClientsDashboardRepository(cipher,
                ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY, Collections.emptySet()).load(now);
        assertEquals(4, result.getTotalClients());
        assertEquals(2, result.getIndependentClients());
        assertEquals(Integer.valueOf(2), breakdown(result).get("Mother Mentor"));
        // Exercise the separately selected iCCM provider template as well.
        result = new AllClientsDashboardRepository(cipher,
                ChwQueryConstant.ALL_ICCM_CLIENTS_SELECT_QUERY, Collections.emptySet()).load(now);
        assertEquals(4, result.getTotalClients());
    }

    @Test
    public void actualMembershipsIncludeMotherMentorAlongsideHigherPriorityAncAndChildTags() {
        createRegisterTables();
        actualClient("mother");
        actualClient("baby");
        db.execSQL("INSERT INTO ec_mothermentor_enrollment (base_entity_id,is_closed,status) VALUES ('mother',0,'client')");
        db.execSQL("INSERT INTO ec_anc_register (base_entity_id,is_closed) VALUES ('mother',0)");
        db.execSQL("INSERT INTO ec_mothermentor_enroll_child_eid (base_entity_id,is_closed) VALUES ('baby',0)");
        db.execSQL("INSERT INTO ec_child (base_entity_id,relational_id,dob) VALUES ('baby','baby-family','2026-01-01')");
        db.execSQL("UPDATE ec_family SET primary_caregiver='mother' WHERE base_entity_id='baby-family'");
        Set<String> menus = new HashSet<>(Arrays.asList(CoreConstants.DrawerMenu.MOTHER_MENTOR,
                CoreConstants.DrawerMenu.ANC, CoreConstants.DrawerMenu.CHILD_CLIENTS));
        AllClientsDashboardRepository.Snapshot result = new AllClientsDashboardRepository(cipher,
                ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY, menus).load(now);
        assertEquals(2, result.getTotalClients());
        assertEquals(0, result.getIndependentClients());
        assertEquals(Integer.valueOf(2), breakdown(result).get("Mother Mentor"));
        assertEquals(Integer.valueOf(1), breakdown(result).get("ANC"));
        assertEquals(Integer.valueOf(1), breakdown(result).get("Child"));
        assertEquals(3, result.getActiveModules());
    }

    @Test
    public void activeMembershipFiltersKeepConsentClosureAndEncounterIdentity() {
        createRegisterTables();
        for (String id : Arrays.asList("active", "closed", "no-consent", "expired")) {
            add(id, "Independent");
            db.execSQL("INSERT INTO ec_family_member (base_entity_id) VALUES (?)", new Object[]{id});
        }
        db.execSQL("INSERT INTO ec_hps_client_register (base_entity_id,is_closed,does_the_client_consent_to_be_enrolled_in_hps_services) "
                + "VALUES ('active',0,'yes'), ('closed',1,'yes'), ('no-consent',0,'no')");
        db.execSQL("INSERT INTO ec_tb_register (base_entity_id,is_closed,tb_case_closure_date) "
                + "VALUES ('active',0,NULL), ('closed',1,NULL), ('expired',0,'2026-01-01')");
        db.execSQL("INSERT INTO ec_pregnancy_outcome (base_entity_id,is_closed,delivery_date) "
                + "VALUES ('active',0,'2026-01-01'), ('closed',1,'2026-01-01'), ('no-consent',0,NULL)");
        db.execSQL("INSERT INTO ec_iccm_enrollment (base_entity_id,entity_id,is_closed,last_interacted_with) "
                + "VALUES ('encounter-active','active',0,strftime('%s','now')*1000), "
                + "('encounter-old','expired',0,1), ('encounter-closed','closed',1,strftime('%s','now')*1000)");
        db.execSQL("INSERT INTO ec_malaria_confirmation (base_entity_id,is_closed,malaria,last_interacted_with) "
                + "VALUES ('active',0,1,strftime('%s','now')*1000), ('expired',0,1,1), "
                + "('closed',1,1,strftime('%s','now')*1000), ('no-consent',0,0,strftime('%s','now')*1000)");
        Set<String> menus = new HashSet<>(Arrays.asList(CoreConstants.DrawerMenu.HPS, CoreConstants.DrawerMenu.TB_CLIENTS,
                CoreConstants.DrawerMenu.PNC, CoreConstants.DrawerMenu.ICCM, CoreConstants.DrawerMenu.MALARIA));
        AllClientsDashboardRepository.Snapshot result = new AllClientsDashboardRepository(cipher, ROSTER, menus).load(now);
        assertEquals(4, result.getTotalClients());
        assertEquals(3, result.getIndependentClients());
        assertEquals(5, result.getActiveModules());
        for (String type : Arrays.asList("iCCHW", "TB", "PNC", "iCCM", "Malaria")) {
            assertEquals(type, Integer.valueOf(1), breakdown(result).get(type));
        }
    }

    @Test
    public void actualIccmEpisodesCountOnePersonAndUseTheirRegistrationDate() {
        createRegisterTables();
        actualClient("person");
        registration("person", "2026-10-26T10:00:00+03:00");
        db.execSQL("INSERT INTO ec_iccm_enrollment (base_entity_id,entity_id,is_closed,last_interacted_with) "
                + "VALUES ('episode-one','person',0,strftime('%s','now')*1000), "
                + "('episode-two','person',0,strftime('%s','now')*1000)");
        for (String template : Arrays.asList(ChwQueryConstant.ALL_CLIENTS_SELECT_QUERY,
                ChwQueryConstant.ALL_ICCM_CLIENTS_SELECT_QUERY)) {
            AllClientsDashboardRepository.Snapshot result = new AllClientsDashboardRepository(cipher,
                    template, Collections.singleton(CoreConstants.DrawerMenu.ICCM)).load(now);
            assertEquals(1, result.getTotalClients());
            assertEquals(1, result.getNewThisWeek());
            assertEquals(Integer.valueOf(1), breakdown(result).get("iCCM"));
            assertEquals("person", result.getRecentClients().get(0).getBaseEntityId());
            assertEquals(time("2026-10-26T10:00:00+03:00"), result.getRecentClients().get(0).getRegisteredAt());
        }
    }

    private void createRegisterTables() {
        String columns = "base_entity_id TEXT, date_removed TEXT, delivery_date TEXT, detoxification_done TEXT, "
                + "dob TEXT, dod TEXT, entity_id TEXT, entity_type TEXT, first_name TEXT, gender TEXT, id INTEGER, "
                + "is_closed INTEGER DEFAULT 0, last_interacted_with INTEGER, last_name TEXT, malaria INTEGER, "
                + "middle_name TEXT, mother_first_name TEXT, mother_last_name TEXT, mother_middle_name TEXT, "
                + "primary_caregiver TEXT, relational_id TEXT, screening_status TEXT, should_enroll TEXT, status TEXT, "
                + "tb_case_closure_date TEXT, village_town TEXT";
        for (String table : Arrays.asList("ec_agyw_register", "ec_anc_register", "ec_asrh_register",
                "ec_ayp_out_school_enrollment", "ec_cbhs_register", "ec_cecap_register", "ec_child", "ec_family",
                "ec_family_member", "ec_family_planning", "ec_harm_reduction_risk_assessment",
                "ec_harm_reduction_sober_house_enrollment", "ec_hiv_register", "ec_hps_client_register",
                "ec_iccm_enrollment", "ec_kvp_prep_register", "ec_malaria_confirmation",
                "ec_mothermentor_enroll_child_eid", "ec_mothermentor_enroll_it", "ec_mothermentor_enroll_partner",
                "ec_mothermentor_enrollment", "ec_pregnancy_outcome", "ec_sbc_register", "ec_tb_register",
                "ec_tbleprosy_screening")) db.execSQL("CREATE TABLE " + table + " (" + columns + ")");
        db.execSQL("ALTER TABLE ec_hps_client_register ADD COLUMN does_the_client_consent_to_be_enrolled_in_hps_services TEXT");
    }

    private void actualClient(String id) {
        db.execSQL("INSERT INTO ec_family_member_search VALUES (?, NULL)", new Object[]{id});
        db.execSQL("INSERT INTO ec_family (base_entity_id,entity_type) VALUES (?, 'ec_independent_client')", new Object[]{id + "-family"});
        db.execSQL("INSERT INTO ec_family_member (base_entity_id,relational_id,first_name) VALUES (?, ?, ?)",
                new Object[]{id, id + "-family", id});
    }

    private AllClientsDashboardRepository repository() {
        return new AllClientsDashboardRepository(cipher, ROSTER, Collections.emptySet());
    }

    private void add(String id, String module) {
        db.execSQL("INSERT INTO ec_family_member_search VALUES (?, NULL)", new Object[]{id});
        row(id, module);
    }

    private void row(String id, String module) {
        db.execSQL("INSERT INTO roster_fixture VALUES (?, ?, NULL, 'Test', 'Saranga', ?, ?)",
                new Object[]{id, id, module, now});
    }

    private void registration(String id, String date) {
        event(id, CoreConstants.EventType.FAMILY_MEMBER_REGISTRATION, date);
    }

    private void event(String id, String type, String date) {
        db.execSQL("INSERT INTO event VALUES (?, ?, ?)", new Object[]{id, type, date});
    }

    private static long time(String date) { return DateTime.parse(date).getMillis(); }

    private Map<String, Integer> breakdown(AllClientsDashboardRepository.Snapshot result) {
        Map<String, Integer> counts = new HashMap<>();
        for (AllClientsDashboardRepository.ModuleCount row : result.getBreakdown()) counts.put(row.getRegisterType(), row.getCount());
        return counts;
    }
}
