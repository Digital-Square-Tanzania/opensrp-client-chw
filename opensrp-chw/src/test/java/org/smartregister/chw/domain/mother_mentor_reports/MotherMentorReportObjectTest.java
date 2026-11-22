package org.smartregister.chw.domain.mother_mentor_reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorDeliveryChildren;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorReportObjectTest {
    private SQLiteDatabase database;
    private MotherMentorReportObject report;
    private MockedStatic<ReportDao> reportDao;

    @Before
    public void setUp() throws Exception {
        database = SQLiteDatabase.create(null);
        database.execSQL("CREATE TABLE ec_family_member (base_entity_id TEXT, gender TEXT, dob TEXT)");
        database.execSQL("CREATE TABLE ec_mothermentor_enrollment (base_entity_id TEXT, is_closed INTEGER, art_status TEXT, art_start_date TEXT)");
        database.execSQL("CREATE TABLE ec_mothermentor_secondary_enrollment (base_entity_id TEXT)");
        database.execSQL("CREATE TABLE ec_mothermentor_household_enrolment (base_entity_id TEXT, consent TEXT)");
        database.execSQL("CREATE TABLE visits (visit_id TEXT, base_entity_id TEXT, visit_type TEXT, processed INTEGER, visit_date INTEGER, visit_json TEXT, form_submission_id TEXT)");
        database.execSQL("CREATE TABLE visit_details (visit_id TEXT, visit_key TEXT, details TEXT, processed INTEGER)");
        database.execSQL("CREATE TABLE event (baseEntityId TEXT, eventType TEXT, eventDate TEXT)");
        database.execSQL("CREATE TABLE ec_mothermentor_enroll_child_eid (base_entity_id TEXT)");
        database.execSQL("CREATE TABLE ec_mothermentor_enroll_partner (base_entity_id TEXT, is_closed INTEGER)");
        database.execSQL("CREATE TABLE ec_mothermentor_enroll_it (base_entity_id TEXT)");
        database.execSQL("CREATE TABLE ec_mothermentor_mobilization (base_entity_id TEXT PRIMARY KEY, sh_tarehe TEXT, sh_mada TEXT, elim_m TEXT, elim_f TEXT, referred_male TEXT, referred_female TEXT, facility_referral_services TEXT)");
        report = new MotherMentorReportObject(new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2026-04-15"));
        reportDao = Mockito.mockStatic(ReportDao.class);
        reportDao.when(() -> ReportDao.getReportBreakdown(Mockito.anyString(), Mockito.<String>anyList()))
                .thenAnswer(invocation -> executeQuery(invocation.getArgument(0), invocation.getArgument(1)));
    }

    @After
    public void tearDown() {
        if (reportDao != null) reportDao.close();
        if (database != null) database.close();
    }

    @Test
    public void countsOlderLiveBornSiblingsOnceDespiteRepeatedVisits() throws Exception {
        primary("mother", "Female", "1997-03-01");
        deliveryVisit("delivery", "mother", "2026-03-21", "19-03-2026", 1);
        deliveryVisit("duplicate", "mother", "2026-03-22", "19-03-2026", 1);
        deliveryVisit("carried", "mother", "2026-04-01", "19-03-2026", 1);
        detail("carried", DBConstants.KEY.MAMA_CARRIED_DELIVERY, "true");
        for (String id : new String[]{"delivery", "duplicate", "carried"}) {
            detail(id, MotherMentorDeliveryChildren.SECTIONS, "true");
            detail(id, MotherMentorDeliveryChildren.COUNT, "3");
            child(id, 1, "deceased", "male");
            child(id, 2, "alive", "female");
            child(id, 3, "stillbirth", "female");
        }
        motherVisit("follow-up", "mother", "2026-04-30", "repeat", "");

        JSONObject counts = report.getIndicatorData();
        assertEquals(2, counts.getInt("mm-7"));
        assertEquals(1, counts.getInt("mm-7-boys-male-under-1m"));
        assertEquals(1, counts.getInt("mm-7-girls-female-under-1m"));
        assertEquals(0, counts.getInt("mm-7-unknown-sex"));
    }

    @Test
    public void lateRecordedLegacyLiveBirthWithUnknownSexCountsInTheVisitMonth() throws Exception {
        primary("mother", "Female", "1997-03-01");
        deliveryVisit("late-delivery", "mother", "2026-04-10", "19-03-2026", 1);
        detail("late-delivery", DBConstants.KEY.MAMA_CHILD_STATUS, "alive");

        JSONObject counts = report.getIndicatorData();
        assertEquals(1, counts.getInt("mm-7"));
        assertEquals(0, counts.getInt("mm-7-male-total"));
        assertEquals(0, counts.getInt("mm-7-female-total"));
        assertEquals(1, counts.getInt("mm-7-unknown-sex"));
    }

    @Test
    public void reopenedNovemberExcludesDecemberBirthAndDecemberCountsItOnce() throws Exception {
        SimpleDateFormat dates = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        primary("mother", "Female", "1997-03-01");
        motherVisit("november-visit", "mother", "2026-11-30", "repeat", "");

        assertEquals(0, new MotherMentorReportObject(dates.parse("2026-11-01"))
                .getIndicatorData().getInt("mm-7"));

        deliveryVisit("december-delivery", "mother", "2026-12-04", "01-12-2026", 1);
        detail("december-delivery", MotherMentorDeliveryChildren.SECTIONS, "true");
        detail("december-delivery", MotherMentorDeliveryChildren.COUNT, "1");
        child("december-delivery", 1, "alive", "female");
        motherVisit("december-repeat", "mother", "2026-12-18", "repeat", "");

        JSONObject reopenedNovember = new MotherMentorReportObject(dates.parse("2026-11-01")).getIndicatorData();
        JSONObject december = new MotherMentorReportObject(dates.parse("2026-12-01")).getIndicatorData();
        assertEquals(0, reopenedNovember.getInt("mm-7"));
        assertEquals(1, december.getInt("mm-7"));
        assertEquals(1, december.getInt("mm-7-female-total"));
    }

    @Test
    public void reopenedNovemberIncludesMonthEndBirthRecordedInDecember() throws Exception {
        primary("mother", "Female", "1997-03-01");
        motherVisit("november-visit", "mother", "2026-11-25", "repeat", "");
        deliveryVisit("december-entry", "mother", "2026-12-04", "30/11/2026", 1);
        detail("december-entry", DBConstants.KEY.MAMA_CHILD_STATUS, "alive");

        report = new MotherMentorReportObject(new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2026-11-01"));
        assertEquals(1, report.getIndicatorData().getInt("mm-7"));
    }

    @Test
    public void ignoresMothersWithoutAFinalizedMamaVisitInThePeriod() throws Exception {
        for (String mother : new String[]{"earlier-visit", "draft-only", "voided-only", "tracing-only"}) {
            primary(mother, "Female", "1997-03-01");
            String delivery = mother + "-delivery";
            deliveryVisit(delivery, mother, "2026-03-21", "19-03-2026", 1);
            detail(delivery, DBConstants.KEY.MAMA_CHILD_STATUS, "alive");
        }
        visit("draft", "draft-only", "2026-04-17", 0);
        detail("draft", DBConstants.KEY.ATTENDANCE_TYPE, "repeat");
        motherVisit("voided", "voided-only", "2026-04-17", "repeat", "");
        database.execSQL("UPDATE visits SET visit_json = ? WHERE visit_id = 'voided'",
                new String[]{"{\"voided\":true}"});
        visit("legacy-tracing", "tracing-only", "2026-04-17", 1);
        detail("legacy-tracing", DBConstants.KEY.IIT_TRACING_VISIT, "yes");

        JSONObject counts = report.getIndicatorData();
        assertEquals(0, counts.getInt("mm-7"));
        assertEquals(0, counts.getInt("mm-7-unknown-sex"));
    }

    @Test
    public void ignoresDraftAndVoidedDeliveriesForVisitedMothers() throws Exception {
        primary("mother", "Female", "1997-03-01");
        deliveryVisit("draft-delivery", "mother", "2026-03-20", "19-03-2026", 0);
        deliveryVisit("voided-delivery", "mother", "2026-03-21", "19-03-2026", 1);
        detail("draft-delivery", DBConstants.KEY.MAMA_CHILD_STATUS, "alive");
        detail("voided-delivery", DBConstants.KEY.MAMA_CHILD_STATUS, "alive");
        database.execSQL("UPDATE visits SET visit_json = ? WHERE visit_id = 'voided-delivery'",
                new String[]{"{\"voided\":true}"});
        motherVisit("follow-up", "mother", "2026-04-17", "repeat", "");

        assertEquals(0, report.getIndicatorData().getInt("mm-7"));
    }

    private void deliveryVisit(String id, String mother, String visitDate, String birthDate, int processed) {
        visit(id, mother, visitDate, processed);
        detail(id, DBConstants.KEY.ATTENDANCE_TYPE, "repeat");
        detail(id, DBConstants.KEY.MAMA_ENROLLMENT_ID, mother + "-enrollment");
        detail(id, DBConstants.KEY.MAMA_PREGNANCY_OUTCOME, "preterm");
        detail(id, DBConstants.KEY.MAMA_TAREHE_KUJIFUNGUA, birthDate);
    }

    private void child(String visit, int number, String status, String sex) {
        detail(visit, MotherMentorDeliveryChildren.observationKey(number, DBConstants.KEY.MAMA_CHILD_STATUS), status);
        detail(visit, MotherMentorDeliveryChildren.observationKey(number, MotherMentorDeliveryChildren.SEX), sex);
    }

    @Test
    public void emptyMonthDistinguishesZeroFromIndicatorsNotCaptured() throws Exception {
        JSONObject counts = report.getIndicatorData();
        for (int indicator = 1; indicator <= 19; indicator++) {
            String key = "mm-" + indicator;
            assertEquals(key, 0, counts.getInt(key));
        }
        assertEquals(0, counts.getInt("query-errors"));
        for (String key : new String[]{"mm-15-female-25-59", "mm-18-male-20-24"}) {
            assertEquals(key, "—", counts.getString(key));
        }
    }

    @Test
    public void serviceDateControlsMonthAndOnlyFinalizedVisitsCount() throws Exception {
        primary("first-day", "Female", "2000-01-01");
        primary("last-day", "Male", "2000-01-01");
        primary("previous-month", "Female", "2000-01-01");
        primary("next-month", "Female", "2000-01-01");
        primary("unfinished", "Female", "2000-01-01");
        primary("other-service", "Female", "2000-01-01");
        primary("missing-date", "Female", "2000-01-01");
        primary("impossible-date", "Female", "2000-01-01");
        visit("v1", "first-day", "2026-04-01", 1);
        visit("v2", "last-day", "2026-04-30", 1);
        visit("v3", "previous-month", "2026-03-31", 1);
        visit("v4", "next-month", "2026-05-01", 1);
        visit("v5", "unfinished", "2026-04-15", 0);
        visit("v6", "other-service", "2026-04-15", 1);
        database.execSQL("UPDATE visits SET visit_type = 'Other Services' WHERE visit_id = 'v6'");
        visit("v7", "missing-date", "", 1);
        visit("v8", "impossible-date", "2026-04-31", 1);
        for (int i = 1; i <= 8; i++) detail("v" + i, "education_provided", "yes");
        // Import/save times deliberately belong to a different month. Service dates above govern.
        database.execSQL("UPDATE visits SET visit_date = 1780272000000");

        JSONObject counts = report.getIndicatorData();
        assertEquals(2, counts.getInt("mm-5"));
        assertEquals(1, counts.getInt("mm-5-male-total"));
        assertEquals(1, counts.getInt("mm-5-female-total"));
    }

    @Test
    public void distinctPrimaryClientsPreserveHistoryAndDoNotCountExistingGroupMembership() throws Exception {
        primary("closed-primary", "Female", "2000-01-01");
        database.execSQL("UPDATE ec_mothermentor_enrollment SET is_closed = 1 WHERE base_entity_id = 'closed-primary'");
        primary("existing-member", "Male", "1990-01-01");
        member("secondary-only", "Female", "1990-01-01");
        database.execSQL("INSERT INTO ec_mothermentor_secondary_enrollment VALUES ('secondary-only')");
        visit("first", "closed-primary", "2026-04-01", 1);
        visit("repeat", "closed-primary", "2026-04-29", 1);
        visit("later-no", "closed-primary", "2026-04-30", 1);
        visit("existing", "existing-member", "2026-04-15", 1);
        visit("secondary", "secondary-only", "2026-04-15", 1);
        for (String key : new String[]{"has_been_linked_to_psychosocial_support_group", "has_been_linked_to_iga_group"}) {
            detail("first", key, "yes");
            detail("first", key, "yes");
            detail("repeat", key, "yes");
            detail("later-no", key, "no");
            detail("existing", key, "already_in_group");
            detail("secondary", key, "yes");
        }

        JSONObject counts = report.getIndicatorData();
        assertEquals(1, counts.getInt("mm-3"));
        assertEquals(1, counts.getInt("mm-4"));
        assertEquals(1, counts.getInt("mm-3-female-25-59"));
    }

    @Test
    public void ageBandsUseCompletedAgeAtMonthEndAndExposeUnclassifiableClients() throws Exception {
        String[][] clients = {
                {"age10", " female ", "2016-04-30", "female-10-14"},
                {"almost15", "Male", "2011-05-01", "male-10-14"},
                {"age15", "Female", "2011-04-30", "female-15-19"},
                {"age20", "Male", "2006-04-30", "male-20-24"},
                {"age25", "Female", "2001-04-30", "female-25-59"},
                {"age60", "Male", "1966-04-30", "male-60-plus"},
                {"under10", "Female", "2016-05-01", "unclassified"},
                {"invalid-dob", "Male", "invalid", "unclassified"},
                {"missing-dob", "Female", "", "unclassified"},
                {"future-dob", "Female", "2027-01-01", "unclassified"},
                {"impossible-dob", "Female", "2000-02-30", "unclassified"},
                {"julian-dob", "Male", "2451544", "unclassified"},
                {"missing-sex", "", "2000-01-01", "unclassified"}
        };
        for (String[] client : clients) {
            primary(client[0], client[1], client[2]);
            visit(client[0], client[0], "2026-04-15", 1);
            detail(client[0], "education_provided", "yes");
        }

        JSONObject counts = report.getIndicatorData();
        assertEquals(6, counts.getInt("mm-5"));
        assertEquals(7, counts.getInt("mm-5-unclassified"));
        assertEquals(3, counts.getInt("mm-5-male-total"));
        assertEquals(3, counts.getInt("mm-5-female-total"));
        for (String[] client : clients) {
            if (!"unclassified".equals(client[3])) assertEquals(client[0], 1, counts.getInt("mm-5-" + client[3]));
        }
        assertEquals(0, counts.getInt("mm-5-female-5-9"));
        assertEquals(counts.getInt("mm-5"), counts.getInt("mm-5-male-total") + counts.getInt("mm-5-female-total"));
    }

    @Test
    public void householdVisitsRequireConsentAndPurposeAndCountEachHouseholdOnce() throws Exception {
        household("consenting", "ndiyo");
        household("declined", "hapana");
        household("no-purpose", "ndiyo");
        household("unfinished", "ndiyo");
        visit("h1", "consenting", "2026-04-01", 1);
        visit("h2", "consenting", "2026-04-30", 1);
        visit("h3", "declined", "2026-04-15", 1);
        visit("h4", "no-purpose", "2026-04-15", 1);
        visit("h5", "unfinished", "2026-04-15", 0);
        visit("h6", "not-a-household", "2026-04-15", 1);
        for (String id : new String[]{"h1", "h2", "h3", "h5", "h6"}) detail(id, "purpose_of_visit", "education");
        detail("h4", "purpose_of_visit", "  ");
        assertEquals(1, report.getIndicatorData().getInt("mm-14"));
    }

    @Test
    public void enrollmentBoundaryExcludesVisitsBeforeProgramEntry() throws Exception {
        primary("later", "Female", "2000-01-01");
        database.execSQL("UPDATE event SET eventDate = '2026-05-01 03:00:00'");
        visit("before-enrollment", "later", "2026-04-30", 1);
        detail("before-enrollment", "education_provided", "yes");
        assertEquals(0, report.getIndicatorData().getInt("mm-5"));
    }

    @Test
    public void registrationIsoBirthDatesUseCalendarAgeAndRejectMalformedDates() throws Exception {
        primary("iso-date", "Female", "2001-04-30T00:00:00.000+03:00");
        primary("malformed", "Female", "2001-04-30garbage");
        primary("impossible-iso", "Female", "2001-02-30T03:00:00.000+03:00");
        for (String id : new String[]{"iso-date", "malformed", "impossible-iso"}) {
            visit(id, id, "2026-04-15", 1);
            detail(id, "education_provided", "yes");
        }
        JSONObject counts = report.getIndicatorData();
        assertEquals(1, counts.getInt("mm-5"));
        assertEquals(1, counts.getInt("mm-5-female-25-59"));
        assertEquals(2, counts.getInt("mm-5-unclassified"));
    }

    @Test
    public void communityTopicsRepeatPerActivityAndCountsRemainAggregates() throws Exception {
        community("one", "01-04-2026", "[pmtct, hiv]", "2", "3", "1", "2");
        community("two", "2026-04-30", "[pmtct]", "1", "4", "0", "1");
        community("outside", "01-05-2026", "[pmtct]", "10", "10", "9", "9");
        community("non-pmtct", "15-04-2026", "[nutrition]", "8", "8", "0", "0");
        JSONObject counts = report.getIndicatorData();
        assertEquals(4, counts.getInt("mm-16"));
        assertEquals(2, counts.getInt("mm-17"));
        assertEquals(10, counts.getInt("mm-18"));
        assertEquals(3, counts.getInt("mm-18-male-total"));
        assertEquals(7, counts.getInt("mm-18-female-total"));
        assertEquals("—", counts.getString("mm-18-female-25-59"));
        assertEquals(4, counts.getInt("mm-19"));
    }

    @Test
    public void historicalMissingAggregateCountsAreNotReportedAsZero() throws Exception {
        community("old", "15-04-2026", "[pmtct]", "2", "3", null, null);
        household("family", "ndiyo"); visit("home", "family", "2026-04-15", 1);
        detail("home", "purpose_of_visit", "routine");
        JSONObject counts = report.getIndicatorData();
        assertEquals("—", counts.getString("mm-19"));
        assertEquals(1, counts.getInt("mm-19-missing-records"));
        assertEquals("—", counts.getString("mm-15"));
        assertEquals(1, counts.getInt("mm-15-missing-records"));
    }

    @Test
    public void householdCounsellingSumsCountsOncePerVisitNotPerRoleOrDetail() throws Exception {
        household("family", "ndiyo");
        for (String id : new String[]{"home1", "home2"}) {
            visit(id, "family", "2026-04-15", 1); detail(id, "purpose_of_visit", "routine");
            detail(id, "counselled_male", "2"); detail(id, "counselled_female", "3");
            detail(id, "counselled_female", "3");
        }
        JSONObject counts = report.getIndicatorData();
        assertEquals(1, counts.getInt("mm-14"));
        assertEquals(10, counts.getInt("mm-15"));
        detail("home2", "household_referral_given", "yes");
        assertEquals(0, report.getIndicatorData().getInt("mm-19"));
    }

    @Test
    public void roleCounsellingTotalsIgnoreStaleSexAndUntickedRoleCounts() throws Exception {
        roleHouseholdVisit("roles", "[mother, partner, child, other]", "2", null, "99", "1", "Neighbour");
        detail("roles", "counselled_male", "50");
        detail("roles", "counselled_female", "60");
        detail("roles", "counselled_total", "999");
        JSONObject counts = report.getIndicatorData();
        assertEquals(5, counts.getInt("mm-15"));
        assertEquals(5, counts.getInt("mm-15-known-total"));
        assertEquals(0, counts.getInt("mm-15-missing-records"));
        assertEquals(1, counts.getInt("mm-15-role-visits"));
        assertEquals("—", counts.getString("mm-15-male-total"));
        assertEquals("—", counts.getString("mm-15-female-total"));
        assertEquals(0, counts.getInt("mm-15-legacy-male-total"));
        assertEquals(0, counts.getInt("query-errors"));
    }

    @Test
    public void mixedLegacyAndRoleVisitsShowFullTotalAndUnavailableSexSubtotals() throws Exception {
        roleHouseholdVisit("roles", "[mother, child]", "2", null, null, null, null);
        visit("legacy", "family", "2026-04-15", 1);
        detail("legacy", "purpose_of_visit", "routine");
        detail("legacy", "counselled_male", "2");
        detail("legacy", "counselled_female", "3");
        JSONObject counts = report.getIndicatorData();
        assertEquals(8, counts.getInt("mm-15"));
        assertEquals(8, counts.getInt("mm-15-known-total"));
        assertEquals(0, counts.getInt("mm-15-missing-records"));
        assertEquals(1, counts.getInt("mm-14"));
        assertEquals("—", counts.getString("mm-15-male-total"));
        assertEquals("—", counts.getString("mm-15-female-total"));
        assertEquals(2, counts.getInt("mm-15-legacy-male-total"));
        assertEquals(3, counts.getInt("mm-15-legacy-female-total"));
        assertEquals(1, counts.getInt("mm-15-role-visits"));
        assertEquals(1, counts.getInt("mm-15-legacy-visits"));
    }

    @Test
    public void roleCounsellingRequiresEverySelectedCountAndOtherIdentification() throws Exception {
        roleHouseholdVisit("incomplete", "[mother, child, other]", null, null, null, "1", null);
        JSONObject counts = report.getIndicatorData();
        assertEquals("—", counts.getString("mm-15"));
        assertEquals(1, counts.getInt("mm-15-missing-records"));
        assertEquals(0, counts.getInt("mm-15-known-total"));
        detail("incomplete", "counselled_child_count", "2");
        counts = report.getIndicatorData();
        assertEquals("—", counts.getString("mm-15"));
        detail("incomplete", "other", "Other person");
        assertEquals(4, report.getIndicatorData().getInt("mm-15"));
    }

    @Test
    public void roleCounsellingRejectsUnknownRolesEmptySelectionAndNonpositiveCounts() throws Exception {
        for (String id : new String[] {"unknown", "empty", "zero", "negative", "fraction", "large"}) {
            String roles = "unknown".equals(id) ? "[grandmother]" : "empty".equals(id) ? "[]" : "[child]";
            String number = "zero".equals(id) ? "0" : "negative".equals(id) ? "-1"
                    : "fraction".equals(id) ? "1.5" : "large".equals(id) ? "100000" : "1";
            roleHouseholdVisit(id, roles, number, null, null, null, null);
        }
        JSONObject counts = report.getIndicatorData();
        assertEquals("—", counts.getString("mm-15"));
        assertEquals(6, counts.getInt("mm-15-missing-records"));
        assertEquals(0, counts.getInt("mm-15-known-total"));
        assertEquals(0, counts.getInt("query-errors"));
    }

    @Test
    public void roleCounsellingRequiresWholeTokensAndKeepsValidSpacingAndDuplicates()
            throws Exception {
        roleHouseholdVisit("compound-child", "[mother, childchild]", null, null, null, null, null);
        roleHouseholdVisit("compound-roles", "[mother, motherpartner]", null, null, null, null, null);
        roleHouseholdVisit("split-word", "[mother, part ner]", null, null, null, null, null);
        roleHouseholdVisit("spaced", "[  mother , partner  ]", null, null, null, null, null);
        roleHouseholdVisit("duplicates", "[mother, mother, partner]", null, null, null, null, null);

        JSONObject counts = report.getIndicatorData();
        assertEquals(3, counts.getInt("mm-15-missing-records"));
        assertEquals("—", counts.getString("mm-15"));
        assertEquals(4, counts.getInt("mm-15-known-total"));
        assertEquals(5, counts.getInt("mm-15-role-visits"));
        assertEquals(0, counts.getInt("query-errors"));
    }

    @Test
    public void roleCountsAreOncePerVisitAndFinalizedConsentMonthBoundariesRemain() throws Exception {
        roleHouseholdVisit("roles", "[mother, partner, child]", "2", null, null, null, null);
        detail("roles", "counselled_child_count", "2");
        visit("draft", "family", "2026-04-15", 0);
        detail("draft", "purpose_of_visit", "routine");
        detail("draft", "household_counselling_schema", "2");
        detail("draft", "participants", "[mother]");
        visit("outside", "family", "2026-05-01", 1);
        detail("outside", "purpose_of_visit", "routine");
        detail("outside", "household_counselling_schema", "2");
        detail("outside", "participants", "[partner]");
        household("no-consent", "hapana");
        visit("refused", "no-consent", "2026-04-15", 1);
        detail("refused", "purpose_of_visit", "routine");
        detail("refused", "household_counselling_schema", "2");
        detail("refused", "participants", "[mother]");
        assertEquals(4, report.getIndicatorData().getInt("mm-15"));
    }

    private void roleHouseholdVisit(String id, String roles, String child, String relative,
            String neighbour, String other, String otherIdentification) {
        household("family", "ndiyo");
        visit(id, "family", "2026-04-15", 1);
        detail(id, "purpose_of_visit", "routine");
        detail(id, "household_counselling_schema", "2");
        detail(id, "participants", roles);
        if (child != null) detail(id, "counselled_child_count", child);
        if (relative != null) detail(id, "counselled_relative_count", relative);
        if (neighbour != null) detail(id, "counselled_neighbour_count", neighbour);
        if (other != null) detail(id, "counselled_other_count", other);
        if (otherIdentification != null) detail(id, "other", otherIdentification);
    }

    @Test
    public void queryFailureCannotMasqueradeAsMeasuredZero() throws Exception {
        reportDao.when(() -> ReportDao.getReportBreakdown(Mockito.anyString(), Mockito.<String>anyList()))
                .thenReturn(new HashMap<>());
        assertEquals("—", report.getIndicatorData().getString("mm-5"));
    }

    private void eid(String id, String gender, String dob) {
        member(id, gender, dob);
        database.execSQL("INSERT INTO ec_mothermentor_enroll_child_eid VALUES (?)", new Object[]{id});
        event(id, "Mother Mentor Enroll Child Eid", "2026-03-01 03:00:00");
    }

    @Test
    public void communityTokensMatchWholeCodesAndCountEachSelectionOncePerActivity() throws Exception {
        community("all", "01-04-2026", "[\"pmtct\", \"hiv\", \"syphilis\", \"hepb\", \"fp\", \"nutrition\", \"tb\", \"gbv\", \"nyingine\", \"hiv\"]", "3", "7", "2", "3");
        community("not-token", "30-04-2026", "[not_pmtct, hiv_extra]", "99", "99", "0", "0");
        JSONObject counts = report.getIndicatorData();
        assertEquals(9, counts.getInt("mm-16"));
        assertEquals(1, counts.getInt("mm-17"));
        assertEquals(10, counts.getInt("mm-18"));
        assertEquals(5, counts.getInt("mm-19"));
    }

    @Test
    public void leapDayAndYearTransitionRespectSelectedMonth() throws Exception {
        report = new MotherMentorReportObject(new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2024-02-15"));
        community("leap", "29-02-2024", "[pmtct]", "2", "3", "0", "0");
        community("march", "01-03-2024", "[pmtct]", "7", "8", "0", "0");
        assertEquals(5, report.getIndicatorData().getInt("mm-18"));
        report = new MotherMentorReportObject(new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2025-12-15"));
        community("december", "31-12-2025", "[pmtct]", "4", "5", "0", "0");
        community("january", "01-01-2026", "[pmtct]", "8", "9", "0", "0");
        assertEquals(9, report.getIndicatorData().getInt("mm-18"));
    }

    @Test
    public void malformedAggregateCountsRemainUnavailable() throws Exception {
        community("negative", "15-04-2026", "[pmtct]", "-1", "3", "0", "0");
        community("decimal", "15-04-2026", "[pmtct]", "1.5", "3", "0", "0");
        community("blank", "15-04-2026", "[pmtct]", " ", "3", "0", "0");
        assertEquals("—", report.getIndicatorData().getString("mm-18"));
        assertEquals(3, report.getIndicatorData().getInt("mm-18-missing-records"));
    }

    @Test
    public void nullQueryResultIsReportedAsUnavailable() throws Exception {
        reportDao.when(() -> ReportDao.getReportBreakdown(Mockito.anyString(), Mockito.<String>anyList())).thenReturn(null);
        JSONObject counts = report.getIndicatorData();
        for (int i = 1; i <= 18; i++) assertEquals("—", counts.getString("mm-" + i));
        assertTrue(counts.getInt("query-errors") > 0);
    }

    @Test
    public void referralsUseTheServiceMonthAndCountEachPartnerOnce() throws Exception {
        member("birthday", "Male", "2001-04-15");
        partnerReferral("before-birthday", "birthday", "2026-04-01", "tracing", "yes");
        partnerReferral("after-birthday", "birthday", "2026-04-20", "referral", "yes");
        member("other", "Male", "1990-01-01");
        partnerReferral("next-month", "other", "2026-05-01", "referral", "yes");
        database.execSQL("INSERT INTO ec_mothermentor_enroll_partner VALUES ('birthday', 1)");
        database.execSQL("UPDATE visits SET visit_date = 1780272000000");

        JSONObject counts = report.getIndicatorData();
        assertEquals(1, counts.getInt("mm-12"));
        assertEquals(1, counts.getInt("mm-12-male-20-24"));
        assertEquals(0, counts.getInt("mm-12-male-25-59"));
        assertEquals(0, counts.getInt("mm-12-missing-records"));
        assertFalse(counts.has("mm-12-hiv-positive"));
    }

    @Test
    public void onlyExplicitFinalizedReferralsCount() throws Exception {
        member("partner", "Male", "1990-01-01");
        partnerAttendance("historical-test", "partner", "2026-04-17", "episode", "link");
        detail("historical-test", DBConstants.KEY.PARTNER_HIV_RESULT, "positive");
        partnerReferral("refused", "partner", "2026-04-18", "tracing", "yes");
        setDetail("refused", "partner_facility_agreement", "refused");
        partnerReferral("draft", "partner", "2026-04-19", "referral", "yes");
        database.execSQL("UPDATE visits SET processed = 0 WHERE visit_id = 'draft'");
        partnerReferral("no-referral", "partner", "2026-04-20", "referral", "no");
        partnerReferral("incomplete", "partner", "2026-04-21", "referral", "");
        partnerReferral("attendance-only", "partner", "2026-04-22", "attendance", "yes");
        JSONObject counts = report.getIndicatorData();
        assertEquals(0, counts.getInt("mm-12"));
        assertEquals(1, counts.getInt("mm-12-missing-records"));

        partnerReferral("confirmed", "partner", "2026-04-23", "referral", "yes");
        setDetail("confirmed", "partner_mother_pregnant", "no");
        counts = report.getIndicatorData();
        assertEquals(1, counts.getInt("mm-12"));
        assertEquals(1, counts.getInt("mm-12-missing-records"));
    }

    @Test
    public void oldPartnerVisitsStillSignalUnknownEducationWithoutChangingReferralCounts() throws Exception {
        member("legacy", "Male", "1990-01-01");
        currentVisit("legacy", "legacy", "2026-04-20", MotherMentorFollowUpType.PARTNER, "found");
        setDetail("legacy", "partner_arrived_at_facility", "yes");
        JSONObject counts = report.getIndicatorData();
        assertEquals(0, counts.getInt("mm-12"));
        assertEquals("—", counts.getString("mm-13-hiv"));
        assertEquals(0, counts.getInt("mm-12-missing-records"));
        assertEquals(1, counts.getInt("mm-13-missing-records"));
    }

    private void partnerReferral(String id, String partner, String date, String phase, String answer) {
        currentVisit(id, partner, date, MotherMentorFollowUpType.PARTNER, "found");
        setDetail(id, "partner_follow_up_schema", "3");
        setDetail(id, "partner_follow_up_phase", phase);
        setDetail(id, "partner_facility_agreement", "agreed");
        setDetail(id, "partner_education_provided", "no");
        setDetail(id, DBConstants.KEY.PARTNER_TESTING_REFERRAL_GIVEN, answer);
    }

    @Test
    public void partnerEducationCountsDistinctPregnantPartnersAndTopicsWithinTheMonth() throws Exception {
        member("educated", "Male", "2001-04-15");
        partnerAttendance("prior", "educated", "2026-03-20", "episode", "link");
        partnerEducation("prior", "hiv");
        partnerAttendance("first", "educated", "2026-04-01", "episode", "link");
        setDetail("first", "partner_follow_up_phase", "tracing");
        setDetail("first", "partner_facility_agreement", "refused");
        partnerEducation("first", "[\"hiv\",\"syphilis\"]");
        partnerAttendance("repeat", "educated", "2026-04-20", "episode", "new-link");
        partnerEducation("repeat", "hiv,hepatitis_b");
        member("attendance-no", "Male", "1990-01-01");
        partnerAttendance("attendance-no", "attendance-no", "2026-04-20", "episode", "link");
        setDetail("attendance-no", "partner_arrived_at_facility", "no");
        partnerEducation("attendance-no", "hepatitis_b");

        JSONObject c = report.getIndicatorData();
        assertEquals(2, c.getInt("mm-13"));
        assertEquals(1, c.getInt("mm-13-hiv"));
        assertEquals(1, c.getInt("mm-13-syphilis"));
        assertEquals(2, c.getInt("mm-13-hepatitis-b"));
        assertEquals(1, c.getInt("mm-13-hiv-male-20-24"));
        assertEquals(0, c.getInt("mm-13-hiv-male-25-59"));
    }

    @Test
    public void attendanceNoWithoutEducationIsKnownZeroAndPreservesHistoricalEducation() throws Exception {
        member("not-attended", "Male", "1990-01-01");
        partnerAttendance("not-attended", "not-attended", "2026-04-20", "episode", "link");
        setDetail("not-attended", "partner_arrived_at_facility", "no");
        setDetail("not-attended", "partner_education_provided", "");
        JSONObject c = report.getIndicatorData();
        assertEquals(0, c.getInt("mm-12"));
        assertEquals(0, c.getInt("mm-12-missing-records"));
        assertEquals(0, c.getInt("mm-13"));
        assertEquals(0, c.getInt("mm-13-missing-records"));

        member("educated-before-attendance", "Male", "1990-01-01");
        partnerAttendance("historical-education", "educated-before-attendance", "2026-04-21", "episode", "link");
        setDetail("historical-education", "partner_arrived_at_facility", "no");
        partnerEducation("historical-education", "hiv");
        c = report.getIndicatorData();
        assertEquals(1, c.getInt("mm-13"));
        assertEquals(1, c.getInt("mm-13-hiv"));
        assertEquals(0, c.getInt("mm-13-missing-records"));
    }

    @Test
    public void attendanceNoWithExplicitIncompleteEducationRemainsMissing() throws Exception {
        member("not-attended", "Male", "1990-01-01");
        partnerAttendance("not-attended", "not-attended", "2026-04-20", "episode", "link");
        setDetail("not-attended", "partner_arrived_at_facility", "no");
        setDetail("not-attended", "partner_education_provided", "yes");
        JSONObject c = report.getIndicatorData();
        assertEquals("—", c.getString("mm-13"));
        assertEquals(1, c.getInt("mm-13-missing-records"));
    }

    @Test
    public void missingPartnerEducationTopicsAreNotReportedAsNoEducation() throws Exception {
        member("partner", "Male", "1990-01-01");
        partnerAttendance("missing-topics", "partner", "2026-04-20", "episode", "link");
        setDetail("missing-topics", "partner_education_provided", "yes");
        JSONObject c = report.getIndicatorData();
        assertEquals("—", c.getString("mm-13"));
        assertEquals("—", c.getString("mm-13-syphilis"));
        assertEquals(1, c.getInt("mm-13-missing-records"));
    }

    private void partnerAttendance(String id, String partner, String date, String episode, String linkage) {
        currentVisit(id, partner, date, MotherMentorFollowUpType.PARTNER, "found");
        setDetail(id, "partner_follow_up_schema", "2");
        setDetail(id, "mother_base_entity_id", "mother");
        setDetail(id, "mother_pregnancy_episode_id", episode);
        setDetail(id, "partner_linkage_id", linkage);
        setDetail(id, "partner_mother_pregnant", "yes");
        setDetail(id, "partner_follow_up_phase", "attendance");
        setDetail(id, "partner_arrived_at_facility", "yes");
        setDetail(id, "partner_facility_arrival_date", date);
        setDetail(id, "partner_education_provided", "no");
        for (String test : new String[]{"hiv", "syphilis", "hepatitis_b"}) {
            setDetail(id, "partner_" + test + "_tested", "no");
            setDetail(id, "partner_" + test + "_not_tested_reason", "stock_out");
        }
    }

    @Test
    public void newReturningAndMaternalReferralsEachCountClientsOnce() throws Exception {
        primary("new-mother", "Female", "1996-01-01");
        primary("returning", "Female", "2006-04-12");
        primary("draft", "Female", "1996-01-01");
        household("family", "ndiyo");
        motherVisit("first", "new-mother", "2026-04-01", "new", "");
        detail("first", "new_pmtct_art_initiation", "no");
        motherVisit("repeat-first", "new-mother", "2026-04-20", "repeat", "[family_planning]");
        motherVisit("return-1", "returning", "2026-04-02", "repeat", "[nutrition, legal_support]");
        motherVisit("return-2", "returning", "2026-04-25", "repeat", "[nutrition]");
        motherVisit("home", "family", "2026-04-11", "repeat", "[nutrition]");
        motherVisit("next-month", "returning", "2026-05-01", "new", "[nutrition]");
        motherVisit("unfinished", "draft", "2026-04-17", "new", "[nutrition]");
        database.execSQL("UPDATE visits SET processed = 0 WHERE visit_id = 'unfinished'");
        community("one", "2026-04-20", "pmtct", "1", "2", "3", "4");

        JSONObject counts = report.getIndicatorData();
        assertEquals(1, counts.getInt("mm-1"));
        assertEquals(1, counts.getInt("mm-2"));
        assertEquals(1, counts.getInt("mm-2-female-15-19"));
        assertEquals(2, counts.getInt("mm-6"));
        assertEquals(7, counts.getInt("mm-19"));
    }

    @Test
    public void countsNewRepeatAndReferralVisitsForEitherEnrollmentEventName() throws Exception {
        primary("new-mother", "Female", "1996-01-01", "MotherMentor Enrollment");
        event("new-mother", "Mother Mentor Enrollment", "2026-04-10 09:00:00");
        primary("returning-mother", "Female", "1997-03-01", "MotherMentor Enrollment");
        primary("current-mother", "Female", "1998-01-01");
        primary("future-mother", "Female", "1998-01-01", "Unrelated Enrollment");
        event("future-mother", "MotherMentor Enrollment", "2026-05-01 03:00:00");

        motherVisit("first", "new-mother", "2026-04-01", "new", "");
        detail("first", "new_pmtct_art_initiation", "no");
        motherVisit("repeat-new", "new-mother", "2026-04-15", "repeat", "");
        motherVisit("referral-one", "returning-mother", "2026-04-05", "repeat", "[nutrition]");
        motherVisit("referral-two", "returning-mother", "2026-04-12", "repeat", "[nutrition]");
        motherVisit("current-first", "current-mother", "2026-04-05", "new", "");
        motherVisit("too-early", "future-mother", "2026-04-05", "new", "[nutrition]");

        JSONObject counts = report.getIndicatorData();
        assertEquals(2, counts.getInt("mm-1"));
        assertEquals(1, counts.getInt("mm-2"));
        assertEquals(1, counts.getInt("mm-6"));
    }

    @Test
    public void childFollowUpUsesActualTestDateAndIgnoresMaternalVisitsAndAttendanceOnly() throws Exception {
        eid("boy", "Male", "2026-03-01");
        eid("girl", "Female", "2025-07-01");
        eid("not-tested", "Female", "2025-07-01");
        eid("maternal-only", "Male", "2025-07-01");
        followUpTest("sample", "boy", "2026-04-15", "six_weeks", "2026-04-13", "yes", "pending");
        followUpTest("duplicate", "boy", "2026-04-19", "six_weeks", "2026-04-13", "yes", "negative");
        followUpTest("late", "girl", "2026-05-01", "nine_months", "2026-04-30", "yes", "negative");
        followUpTest("facility-only", "not-tested", "2026-04-10", "nine_months", "2026-04-10", "no", "");
        followUpTest("mama", "maternal-only", "2026-04-10", "nine_months", "2026-04-10", "yes", "negative");
        database.execSQL("UPDATE visits SET visit_type = 'MotherMentor Services' WHERE visit_id = 'mama'");
        database.execSQL("DELETE FROM visit_details WHERE visit_id = 'mama' AND visit_key = 'child_eid_test_type'");
        detail("mama", "mama_hei_test_type", "nine_months");

        JSONObject counts = report.getIndicatorData();
        assertEquals(1, counts.getInt("mm-8-boys-male-1-11m"));
        assertEquals(1, counts.getInt("mm-8"));
        assertEquals(1, counts.getInt("mm-9-girls-female-1-11m"));
        assertEquals(1, counts.getInt("mm-9"));
    }

    @Test
    public void finalTestOutcomesBreakDownBothSexesAndSumToTheTotal() throws Exception {
        for (String[] item : new String[][]{
                {"a", "Male", "positive"}, {"b", "Female", "positive"},
                {"c", "Male", "negative"}, {"d", "Female", "negative"}}) {
            eid(item[0], item[1], "2024-10-01");
            finalChildTest("test-" + item[0], item[0], "2026-04-18", item[2]);
        }

        JSONObject counts = report.getIndicatorData();
        assertEquals(4, counts.getInt("mm-10"));
        for (String result : new String[]{"positive", "negative"}) {
            assertEquals(2, counts.getInt("mm-10-" + result));
            assertEquals(1, counts.getInt("mm-10-" + result + "-female-12-23m"));
            assertEquals(1, counts.getInt("mm-10-" + result + "-male-12-23m"));
        }
    }

    @Test
    public void latestIitVisitClassifiesEachMotherInExactlyOneOutcome() throws Exception {
        String[][] other = {{"agreed", "found", ""}, {"refused", "found", ""},
                {"not-found", "not_found", "travelled"}, {"died", "not_found", "deceased"},
                {"transferred", "not_found", "moved_area"}};
        for (String[] visit : other) {
            member(visit[0], "Female", "1998-02-01");
            iit(visit[0]);
            iitVisit(visit[0], visit[0], "2026-04-12", visit[1], visit[2], "none");
        }
        detail("agreed", "iit_return_agreement", "agreed");
        detail("refused", "iit_return_agreement", "refused");
        member("returned", "Female", "1998-02-01");
        iit("returned");
        iitVisit("first", "returned", "2026-04-01", "found", "", "none");
        detail("first", "iit_return_agreement", "agreed");
        iitVisit("latest", "returned", "2026-04-22", "found", "", "referred");
        detail("latest", DBConstants.KEY.RETURNED_TO_FACILITY, "yes");
        detail("latest", DBConstants.KEY.FACILITY_RETURN_DATE, "21-04-2026");
        iitVisit("later-voided", "returned", "2026-04-25", "not_found", "deceased", "referred");
        database.execSQL("UPDATE visits SET visit_json = ? WHERE visit_id = 'later-voided'",
                new String[]{"{\"" + DBConstants.KEY.EVENT_VOIDED + "\":true}"});

        JSONObject counts = report.getIndicatorData();
        assertEquals(6, counts.getInt("mm-11"));
        for (String[] visit : other) assertEquals(1, counts.getInt("mm-11-" + visit[0]));
        assertEquals(1, counts.getInt("mm-11-returned"));
        assertEquals(0, counts.getInt("mm-11-missing-records"));
    }

    @Test
    public void foundHouseholdsCountOnceAndNotFoundVisitsDoNotAddCounselling() throws Exception {
        household("family", "ndiyo");
        household("unreachable", "ndiyo");
        for (String id : new String[]{"initial", "found", "missed", "only-missed"}) {
            String family = "only-missed".equals(id) ? "unreachable" : "family";
            visit(id, family, "2026-04-15", 1);
            detail(id, DBConstants.KEY.HOUSEHOLD_FOLLOW_UP_SCHEMA, "2");
            detail(id, DBConstants.KEY.HOUSEHOLD_VISIT_TYPE, "initial".equals(id) ? "new" : "repeat");
            if (!"initial".equals(id)) {
                detail(id, DBConstants.KEY.HOUSEHOLD_FOLLOW_UP_STATUS, "found".equals(id) ? "found" : "not_found");
            }
            detail(id, "purpose_of_visit", "routine");
            detail(id, "counselled_male", "2");
            detail(id, "counselled_female", "3");
        }
        JSONObject counts = report.getIndicatorData();
        assertEquals(1, counts.getInt("mm-14"));
        assertEquals(10, counts.getInt("mm-15"));
        assertEquals(4, counts.getInt("mm-15-male-total"));
        assertEquals(6, counts.getInt("mm-15-female-total"));
    }

    private void motherVisit(String id, String client, String date, String attendance, String referrals) {
        visit(id, client, date, 1);
        detail(id, DBConstants.KEY.ATTENDANCE_TYPE, attendance);
        detail(id, DBConstants.KEY.TYPE_OF_REFERRAL_GIVEN, referrals);
    }

    private void followUpTest(String id, String child, String visitDate, String stage, String testDate,
            String tested, String result) {
        currentVisit(id, child, visitDate, MotherMentorFollowUpType.CHILD_EID, "found");
        detail(id, DBConstants.KEY.CHILD_EID_TEST_TYPE, stage);
        detail(id, DBConstants.KEY.MTOTO_RETURN_FLOW, "1");
        detail(id, DBConstants.KEY.MAMA_HEI_TEST_DONE, tested);
        detail(id, DBConstants.KEY.MAMA_HEI_TEST_RESULT, result);
        detail(id, DBConstants.KEY.MAMA_HEI_TEST_DATE, testDate);
    }

    private void iitVisit(String id, String client, String date, String status, String reason, String returnState) {
        currentVisit(id, client, date, MotherMentorFollowUpType.MOTHER_IIT, status);
        detail(id, DBConstants.KEY.IIT_NOT_FOUND_REASON, reason);
        detail(id, DBConstants.KEY.IIT_RETURN_STATE, returnState);
    }

    private void partnerEducation(String id, String topics) {
        setDetail(id, "partner_education_provided", "yes");
        setDetail(id, "partner_education_topics", topics);
    }

    private void finalChildTest(String id, String child, String date, String result) {
        currentVisit(id, child, date, MotherMentorFollowUpType.CHILD_EID, "found");
        setDetail(id, "child_eid_test_type", "eighteen_months");
        setDetail(id, "child_eid_arrived_at_facility", "yes");
        setDetail(id, "child_eid_facility_arrival_date", date);
        setDetail(id, "child_eid_final_hiv_test_done", "yes");
        setDetail(id, "child_eid_final_hiv_result", result);
        setDetail(id, "child_eid_final_hiv_test_date", date);
    }

    private void setDetail(String visitId, String key, String value) {
        database.execSQL("DELETE FROM visit_details WHERE visit_id = ? AND visit_key = ?", new Object[]{visitId, key});
        detail(visitId, key, value);
    }

    private void currentVisit(String id, String client, String date, MotherMentorFollowUpType type, String status) {
        visit(id, client, date, 1);
        database.execSQL("UPDATE visits SET visit_type = ? WHERE visit_id = ?", new Object[]{type.getEventType(), id});
        detail(id, "follow_up_category", type.getValue()); detail(id, "follow_up_status", status);
    }

    private void event(String id, String type, String date) {
        database.execSQL("INSERT INTO event VALUES (?, ?, ?)", new Object[]{id, type, date});
    }

    private void iit(String id) {
        database.execSQL("INSERT INTO ec_mothermentor_enroll_it VALUES (?)", new Object[]{id});
        event(id, "Mother Mentor Enroll IIT", "2026-03-01 03:00:00");
    }

    private void community(String id, String date, String topics, String men, String women, String referredMen, String referredWomen) {
        database.execSQL("INSERT INTO ec_mothermentor_mobilization VALUES (?, ?, ?, ?, ?, ?, ?, 'ctc')",
                new Object[]{id, date, topics, men, women, referredMen, referredWomen});
    }

    private Map<String, Integer> executeQuery(String sql, List<String> columns) {
        Map<String, Integer> result = new HashMap<>();
        try (Cursor cursor = database.rawQuery(sql, null)) {
            assertTrue(cursor.moveToFirst());
            for (String column : columns) result.put(column, cursor.getInt(cursor.getColumnIndexOrThrow(column)));
        }
        return result;
    }

    private void primary(String id, String gender, String dob) {
        primary(id, gender, dob, "Mother Mentor Enrollment");
    }

    private void primary(String id, String gender, String dob, String enrollmentEvent) {
        member(id, gender, dob);
        database.execSQL("INSERT INTO ec_mothermentor_enrollment (base_entity_id, is_closed) VALUES (?, 0)", new Object[]{id});
        event(id, enrollmentEvent, "2026-03-01 03:00:00");
    }

    private void member(String id, String gender, String dob) {
        database.execSQL("INSERT INTO ec_family_member VALUES (?, ?, ?)", new Object[]{id, gender, dob});
    }

    private void household(String id, String consent) {
        database.execSQL("INSERT INTO ec_mothermentor_household_enrolment VALUES (?, ?)", new Object[]{id, consent});
    }

    private void visit(String visitId, String clientId, String serviceDate, int processed) {
        database.execSQL("INSERT INTO visits (visit_id, base_entity_id, visit_type, processed, visit_date) VALUES (?, ?, 'MotherMentor Services', ?, 0)", new Object[]{visitId, clientId, processed});
        detail(visitId, "mothermentor_visit_date", serviceDate);
    }

    private void detail(String visitId, String key, String value) {
        // Visit-detail processed flags remain zero locally even when their parent visit is finalized.
        database.execSQL("INSERT INTO visit_details VALUES (?, ?, ?, 0)", new Object[]{visitId, key, value});
    }
}
