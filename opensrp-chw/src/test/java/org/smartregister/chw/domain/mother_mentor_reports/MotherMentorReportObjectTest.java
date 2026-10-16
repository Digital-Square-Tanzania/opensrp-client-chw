package org.smartregister.chw.domain.mother_mentor_reports;

import static org.junit.Assert.assertEquals;
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

import java.text.SimpleDateFormat;
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
        database.execSQL("CREATE TABLE visits (visit_id TEXT, base_entity_id TEXT, visit_type TEXT, processed INTEGER, visit_date INTEGER)");
        database.execSQL("CREATE TABLE visit_details (visit_id TEXT, visit_key TEXT, details TEXT, processed INTEGER)");
        database.execSQL("CREATE TABLE event (baseEntityId TEXT, eventType TEXT, eventDate TEXT)");
        database.execSQL("CREATE TABLE ec_mothermentor_enroll_child_eid (base_entity_id TEXT)");
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
    public void emptyMonthDistinguishesZeroFromIndicatorsNotCaptured() throws Exception {
        JSONObject counts = report.getIndicatorData();
        for (String key : new String[]{"mm-1", "mm-2", "mm-3", "mm-4", "mm-5", "mm-6", "mm-7", "mm-9", "mm-9-boys", "mm-10", "mm-10-girls", "mm-13", "mm-14", "mm-15", "mm-16", "mm-17", "mm-18", "mm-1-unclassified"}) {
            assertEquals(key, 0, counts.getInt(key));
        }
        for (String key : new String[]{"mm-8-hiv-positive", "mm-11-positive", "mm-12-syphilis", "mm-14-female-25-59", "mm-16-male-20-24"}) {
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
        assertEquals(2, counts.getInt("mm-7"));
        assertEquals(1, counts.getInt("mm-7-male-total"));
        assertEquals(1, counts.getInt("mm-7-female-total"));
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
        assertEquals(1, counts.getInt("mm-1"));
        assertEquals(1, counts.getInt("mm-2"));
        assertEquals(1, counts.getInt("mm-1-female-25-59"));
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
        assertEquals(6, counts.getInt("mm-7"));
        assertEquals(7, counts.getInt("mm-7-unclassified"));
        assertEquals(3, counts.getInt("mm-7-male-total"));
        assertEquals(3, counts.getInt("mm-7-female-total"));
        for (String[] client : clients) {
            if (!"unclassified".equals(client[3])) assertEquals(client[0], 1, counts.getInt("mm-7-" + client[3]));
        }
        assertEquals(0, counts.getInt("mm-7-female-5-9"));
        assertEquals(counts.getInt("mm-7"), counts.getInt("mm-7-male-total") + counts.getInt("mm-7-female-total"));
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
        assertEquals(1, report.getIndicatorData().getInt("mm-13"));
    }

    @Test
    public void enrollmentBoundaryExcludesVisitsBeforeProgramEntry() throws Exception {
        primary("later", "Female", "2000-01-01");
        database.execSQL("UPDATE event SET eventDate = '2026-05-01 03:00:00'");
        visit("before-enrollment", "later", "2026-04-30", 1);
        detail("before-enrollment", "education_provided", "yes");
        assertEquals(0, report.getIndicatorData().getInt("mm-7"));
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
        assertEquals(1, counts.getInt("mm-7"));
        assertEquals(1, counts.getInt("mm-7-female-25-59"));
        assertEquals(2, counts.getInt("mm-7-unclassified"));
    }

    @Test
    public void liveEnrollmentWithEarlierArtAndExplicitNoInitiationIsExcluded() throws Exception {
        report = new MotherMentorReportObject(new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2026-10-15"));
        primary("mother", "Female", "1997-01-01T03:00:00.000+03:00");
        database.execSQL("UPDATE event SET eventDate = '2026-10-15 03:00:00'");
        database.execSQL("UPDATE ec_mothermentor_enrollment SET art_status = 'yes', art_start_date = '15-09-2026'");
        visit("continuing", "mother", "2026-10-15", 1);
        detail("continuing", "new_pmtct_art_initiation", "no");
        JSONObject counts = report.getIndicatorData();
        assertEquals(report.buildNewArtQuery(), 0, counts.getInt("mm-3"));
        assertEquals(0, counts.getInt("mm-3-unclassified"));
    }

    @Test
    public void newArtRequiresBothEnrollmentAndInitiationInMonth() throws Exception {
        for (String id : new String[]{"new-art", "old-art", "old-enrollment", "visit-art"}) primary(id, "Female", "2000-01-01");
        database.execSQL("UPDATE event SET eventDate = '2026-04-01 03:00:00' WHERE baseEntityId <> 'old-enrollment'");
        database.execSQL("UPDATE ec_mothermentor_enrollment SET art_status = 'yes', art_start_date = '30-04-2026' WHERE base_entity_id <> 'visit-art'");
        database.execSQL("UPDATE ec_mothermentor_enrollment SET art_start_date = '31-03-2026' WHERE base_entity_id = 'old-art'");
        visit("initiation", "visit-art", "2026-04-30", 1);
        detail("initiation", "new_pmtct_art_initiation", "yes");
        detail("initiation", "visit_art_start_date", "2026-04-30");
        assertEquals(2, report.getIndicatorData().getInt("mm-3"));
    }

    @Test
    public void latestTracingOutcomeWinsAndPrimaryCohortIsSeparate() throws Exception {
        primary("mother", "Female", "2000-01-01");
        iit("mother");
        member("secondary", "Male", "1990-01-01");
        iit("secondary");
        tracing("earlier", "mother", "2026-04-01", "not_found", "no", null);
        tracing("later", "mother", "2026-04-30", "found", "yes", "2026-04-30");
        tracing("iit-only", "secondary", "2026-04-20", "transferred", "not_applicable", null);
        JSONObject counts = report.getIndicatorData();
        assertEquals(1, counts.getInt("mm-4"));
        assertEquals(0, counts.getInt("mm-4-not-found"));
        assertEquals(1, counts.getInt("mm-4-returned"));
        assertEquals(1, counts.getInt("mm-5"));
        assertEquals(2, counts.getInt("mm-18"));
        assertEquals(1, counts.getInt("mm-18-transferred"));
    }

    @Test
    public void foundAndContinuingNeverImplyReturnToArt() throws Exception {
        primary("mother", "Female", "2000-01-01"); iit("mother");
        tracing("found", "mother", "2026-04-15", "found", "no", null);
        detail("found", "follow_up_status", "continuing_services");
        JSONObject counts = report.getIndicatorData();
        assertEquals(0, counts.getInt("mm-5"));
        assertEquals(0, counts.getInt("mm-4-returned"));
        assertEquals(1, counts.getInt("mm-4-other-outcomes"));
    }

    @Test
    public void communityTopicsRepeatPerActivityAndCountsRemainAggregates() throws Exception {
        community("one", "01-04-2026", "[pmtct, hiv]", "2", "3", "1", "2");
        community("two", "2026-04-30", "[pmtct]", "1", "4", "0", "1");
        community("outside", "01-05-2026", "[pmtct]", "10", "10", "9", "9");
        community("non-pmtct", "15-04-2026", "[nutrition]", "8", "8", "0", "0");
        JSONObject counts = report.getIndicatorData();
        assertEquals(4, counts.getInt("mm-6"));
        assertEquals(2, counts.getInt("mm-15"));
        assertEquals(10, counts.getInt("mm-16"));
        assertEquals(3, counts.getInt("mm-16-male-total"));
        assertEquals(7, counts.getInt("mm-16-female-total"));
        assertEquals("—", counts.getString("mm-16-female-25-59"));
        assertEquals(4, counts.getInt("mm-17"));
    }

    @Test
    public void historicalMissingAggregateCountsAreNotReportedAsZero() throws Exception {
        community("old", "15-04-2026", "[pmtct]", "2", "3", null, null);
        household("family", "ndiyo"); visit("home", "family", "2026-04-15", 1);
        detail("home", "purpose_of_visit", "routine");
        JSONObject counts = report.getIndicatorData();
        assertEquals("—", counts.getString("mm-17"));
        assertEquals(1, counts.getInt("mm-17-missing-records"));
        assertEquals("—", counts.getString("mm-14"));
        assertEquals(1, counts.getInt("mm-14-missing-records"));
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
        assertEquals(1, counts.getInt("mm-13"));
        assertEquals(10, counts.getInt("mm-14"));
        detail("home2", "household_referral_given", "yes");
        assertEquals("—", report.getIndicatorData().getString("mm-17"));
    }

    @Test
    public void queryFailureCannotMasqueradeAsMeasuredZero() throws Exception {
        reportDao.when(() -> ReportDao.getReportBreakdown(Mockito.anyString(), Mockito.<String>anyList()))
                .thenReturn(new HashMap<>());
        assertEquals("—", report.getIndicatorData().getString("mm-7"));
    }

    @Test
    public void childAttendanceCountsDistinctChildrenByStageAndSex() throws Exception {
        eid("boy", "Male", "2026-03-01");
        eid("girl", "Female", "2025-07-01T03:00:00.000+03:00");
        childVisit("six-weeks", "boy", "2026-04-01", "week_6", "yes", "01-04-2026", 1);
        childVisit("six-weeks-repeat", "boy", "2026-04-30", "week_6", "yes", "2026-04-01", 1);
        childVisit("nine-months", "girl", "2026-04-30", "month_9", "yes", "2026-04-30", 1);
        JSONObject counts = report.getIndicatorData();
        assertEquals(1, counts.getInt("mm-9"));
        assertEquals(1, counts.getInt("mm-9-boys"));
        assertEquals(0, counts.getInt("mm-9-girls"));
        assertEquals(1, counts.getInt("mm-9-male-1-11m"));
        assertEquals(1, counts.getInt("mm-10"));
        assertEquals(1, counts.getInt("mm-10-girls-female-1-11m"));
        assertEquals(0, counts.getInt("mm-10-boys"));
    }

    @Test
    public void childAttendanceRequiresFinalizedEidVisitAndConfirmedInMonthArrival() throws Exception {
        for (String id : new String[]{"no", "na", "older", "later", "unfinished", "other-stage", "before-entry"})
            eid(id, "Female", "2025-07-01");
        member("not-enrolled", "Female", "2025-07-01");
        childVisit("no", "no", "2026-04-15", "month_9", "no", null, 1);
        childVisit("na", "na", "2026-04-15", "month_9", "not_applicable", null, 1);
        childVisit("older", "older", "2026-04-15", "month_9", "yes", "2026-03-31", 1);
        childVisit("later", "later", "2026-05-01", "month_9", "yes", "2026-05-01", 1);
        childVisit("unfinished", "unfinished", "2026-04-15", "month_9", "yes", "2026-04-15", 0);
        childVisit("other-stage", "other-stage", "2026-04-15", "month_18", "yes", "2026-04-15", 1);
        childVisit("not-enrolled", "not-enrolled", "2026-04-15", "month_9", "yes", "2026-04-15", 1);
        database.execSQL("UPDATE event SET eventDate = '2026-05-01' WHERE baseEntityId = 'before-entry'");
        childVisit("before-entry", "before-entry", "2026-04-15", "month_9", "yes", "2026-04-15", 1);
        assertEquals(0, report.getIndicatorData().getInt("mm-10"));
    }

    @Test
    public void childMissingStageMakesBothIndicatorsUnavailableWithoutRewritingHistory() throws Exception {
        eid("legacy", "Female", "2025-07-01");
        visit("legacy", "legacy", "2026-04-15", 1);
        JSONObject counts = report.getIndicatorData();
        for (String indicator : new String[]{"mm-9", "mm-10"}) {
            assertEquals("—", counts.getString(indicator));
            assertEquals("—", counts.getString(indicator + "-girls"));
            assertEquals(1, counts.getInt(indicator + "-missing-records"));
        }
    }

    @Test
    public void childInvalidOrMissingArrivalDateFlagsOnlyTheAffectedStage() throws Exception {
        for (String id : new String[]{"missing", "invalid", "future"}) eid(id, "Female", "2025-07-01");
        childVisit("missing", "missing", "2026-04-15", "month_9", "yes", null, 1);
        childVisit("invalid", "invalid", "2026-04-30", "month_9", "yes", "2026-04-31", 1);
        childVisit("future", "future", "2026-04-15", "month_9", "yes", "2026-04-16", 1);
        JSONObject counts = report.getIndicatorData();
        assertEquals(0, counts.getInt("mm-9"));
        assertEquals("—", counts.getString("mm-10"));
        assertEquals(3, counts.getInt("mm-10-missing-records"));
    }

    @Test
    public void childWorkbookAgeBandsExposeOutOfBandOrUnknownDemographics() throws Exception {
        eid("newborn", "Male", "2026-04-15");
        eid("one-month", "Female", "2026-03-30");
        eid("twelve-months", "Female", "2025-04-30");
        eid("unknown-sex", "", "2025-07-01");
        childVisit("newborn", "newborn", "2026-04-30", "week_6", "yes", "2026-04-30", 1);
        for (String id : new String[]{"one-month", "twelve-months", "unknown-sex"})
            childVisit(id, id, "2026-04-30", "month_9", "yes", "2026-04-30", 1);
        JSONObject counts = report.getIndicatorData();
        assertEquals(1, counts.getInt("mm-9-male-under-1m"));
        assertEquals(1, counts.getInt("mm-10"));
        assertEquals(1, counts.getInt("mm-10-female-1-11m"));
        assertEquals(2, counts.getInt("mm-10-unclassified"));
    }

    private void eid(String id, String gender, String dob) {
        member(id, gender, dob);
        database.execSQL("INSERT INTO ec_mothermentor_enroll_child_eid VALUES (?)", new Object[]{id});
        event(id, "Mother Mentor Enroll Child Eid", "2026-03-01 03:00:00");
    }

    private void childVisit(String id, String child, String date, String stage, String attendance, String arrivalDate, int processed) {
        visit(id, child, date, processed);
        detail(id, "child_followup_test_stage", stage);
        detail(id, "child_reached_facility", attendance);
        if (arrivalDate != null) detail(id, "child_facility_date", arrivalDate);
    }

    @Test
    public void laterConfirmedTracingSupersedesEarlierLegacyUnknownVisit() throws Exception {
        primary("mother", "Female", "2000-01-01"); iit("mother");
        visit("old", "mother", "2026-04-01", 1);
        tracing("latest", "mother", "2026-04-30", "found", "yes", "2026-04-30");
        JSONObject counts = report.getIndicatorData();
        assertEquals(1, counts.getInt("mm-4-returned"));
        assertEquals(1, counts.getInt("mm-18-returned"));
        assertEquals(1, counts.getInt("mm-5"));
    }

    @Test
    public void laterUnknownTracingMasksOutcomesButCannotEraseConfirmedMonthlyReturn() throws Exception {
        primary("mother", "Female", "2000-01-01"); iit("mother");
        tracing("returned", "mother", "2026-04-01", "found", "yes", "2026-04-01");
        visit("unknown", "mother", "2026-04-30", 1);
        JSONObject counts = report.getIndicatorData();
        assertEquals("—", counts.getString("mm-4"));
        assertEquals("—", counts.getString("mm-18"));
        assertEquals(1, counts.getInt("mm-5"));
    }

    @Test
    public void incompleteLatestTracingDoesNotMasqueradeAsZero() throws Exception {
        primary("mother", "Female", "2000-01-01"); iit("mother");
        visit("incomplete", "mother", "2026-04-15", 1);
        detail("incomplete", "iit_tracing_visit", "yes");
        JSONObject counts = report.getIndicatorData();
        for (String key : new String[]{"mm-4", "mm-5", "mm-18"}) assertEquals(key, "—", counts.getString(key));
    }

    @Test
    public void impossibleOrFutureReturnDateIsMissingEvidenceNotNoReturn() throws Exception {
        primary("mother", "Female", "2000-01-01"); iit("mother");
        tracing("bad-date", "mother", "2026-04-15", "found", "yes", "2026-04-30");
        JSONObject counts = report.getIndicatorData();
        for (String key : new String[]{"mm-4", "mm-5", "mm-18"}) assertEquals(key, "—", counts.getString(key));
    }

    @Test
    public void allTracingOutcomesAndSameDayTiesReconcileToTheirParentRows() throws Exception {
        String[] outcomes = {"found", "not_found", "deceased", "transferred", "refused", "still_followed"};
        for (String outcome : outcomes) {
            primary(outcome, "Female", "2000-01-01"); iit(outcome);
            tracing(outcome, outcome, "2026-04-15", outcome, "found".equals(outcome) ? "yes" : "no", "found".equals(outcome) ? "2026-04-15" : null);
        }
        // Later save time wins over the alphabetically larger visit ID, then visit ID breaks ties.
        tracing("z-early", "found", "2026-04-15", "deceased", "no", null);
        database.execSQL("UPDATE visits SET visit_date = 10 WHERE visit_id = 'found'");
        tracing("z-latest", "not_found", "2026-04-15", "refused", "no", null);
        visit("non-tracing", "found", "2026-04-30", 1); detail("non-tracing", "iit_tracing_visit", "no");
        JSONObject c = report.getIndicatorData();
        assertEquals(1, c.getInt("mm-4-returned"));
        assertEquals(0, c.getInt("mm-4-not-found"));
        assertEquals(2, c.getInt("mm-4-refused"));
        assertEquals(1, c.getInt("mm-4-died"));
        assertEquals(4, c.getInt("mm-4"));
        assertEquals(5, c.getInt("mm-18"));
        assertEquals(1, c.getInt("mm-18-transferred"));
        assertEquals(2, c.getInt("mm-4-other-outcomes"));
        assertEquals(1, c.getInt("mm-18-other-outcomes"));
        assertEquals(1, c.getInt("mm-5"));
    }

    @Test
    public void recordedEnrollmentCalendarDateDoesNotShiftAcrossMonthAtUtcMidnight() throws Exception {
        primary("mother", "Female", "2000-01-01");
        database.execSQL("UPDATE event SET eventDate = '2026-04-01T00:30:00.000+03:00'");
        database.execSQL("UPDATE ec_mothermentor_enrollment SET art_status = 'yes', art_start_date = '01-04-2026'");
        assertEquals(1, report.getIndicatorData().getInt("mm-3"));
    }

    @Test
    public void explicitArtInitiationAfterVisitDateIsNotDeliveredCare() throws Exception {
        primary("mother", "Female", "2000-01-01");
        database.execSQL("UPDATE event SET eventDate = '2026-04-01'");
        visit("early", "mother", "2026-04-10", 1);
        detail("early", "new_pmtct_art_initiation", "yes"); detail("early", "visit_art_start_date", "2026-04-20");
        assertEquals(0, report.getIndicatorData().getInt("mm-3"));
    }

    @Test
    public void failedHouseholdReferralCoverageMustNotPublishPartialCommunityTotal() throws Exception {
        community("session", "15-04-2026", "[pmtct]", "1", "2", "1", "2");
        reportDao.when(() -> ReportDao.getReportBreakdown(Mockito.anyString(), Mockito.<String>anyList()))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    return sql.contains("household_referral_given") ? new HashMap<>() : executeQuery(sql, invocation.getArgument(1));
                });
        JSONObject counts = report.getIndicatorData();
        assertEquals("—", counts.getString("mm-17"));
        assertEquals(1, counts.getInt("query-errors"));
    }

    @Test
    public void communityTokensMatchWholeCodesAndCountEachSelectionOncePerActivity() throws Exception {
        community("all", "01-04-2026", "[\"pmtct\", \"hiv\", \"syphilis\", \"hepb\", \"fp\", \"nutrition\", \"tb\", \"gbv\", \"nyingine\", \"hiv\"]", "3", "7", "2", "3");
        community("not-token", "30-04-2026", "[not_pmtct, hiv_extra]", "99", "99", "0", "0");
        JSONObject counts = report.getIndicatorData();
        assertEquals(9, counts.getInt("mm-6"));
        assertEquals(1, counts.getInt("mm-15"));
        assertEquals(10, counts.getInt("mm-16"));
        assertEquals(5, counts.getInt("mm-17"));
    }

    @Test
    public void leapDayAndYearTransitionRespectSelectedMonth() throws Exception {
        report = new MotherMentorReportObject(new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2024-02-15"));
        community("leap", "29-02-2024", "[pmtct]", "2", "3", "0", "0");
        community("march", "01-03-2024", "[pmtct]", "7", "8", "0", "0");
        assertEquals(5, report.getIndicatorData().getInt("mm-16"));
        report = new MotherMentorReportObject(new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2025-12-15"));
        community("december", "31-12-2025", "[pmtct]", "4", "5", "0", "0");
        community("january", "01-01-2026", "[pmtct]", "8", "9", "0", "0");
        assertEquals(9, report.getIndicatorData().getInt("mm-16"));
    }

    @Test
    public void malformedAggregateCountsRemainUnavailable() throws Exception {
        community("negative", "15-04-2026", "[pmtct]", "-1", "3", "0", "0");
        community("decimal", "15-04-2026", "[pmtct]", "1.5", "3", "0", "0");
        community("blank", "15-04-2026", "[pmtct]", " ", "3", "0", "0");
        assertEquals("—", report.getIndicatorData().getString("mm-16"));
        assertEquals(3, report.getIndicatorData().getInt("mm-16-missing-records"));
    }

    @Test
    public void tracingCoverageQueryFailureKeepsOutcomeTotalsUnavailable() throws Exception {
        primary("mother", "Female", "2000-01-01"); iit("mother");
        tracing("returned", "mother", "2026-04-15", "found", "yes", "2026-04-15");
        reportDao.when(() -> ReportDao.getReportBreakdown(Mockito.anyString(), Mockito.<String>anyList()))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    return sql.contains("WITH coverage_visits") ? new HashMap<>() : executeQuery(sql, invocation.getArgument(1));
                });
        JSONObject counts = report.getIndicatorData();
        assertEquals("—", counts.getString("mm-4"));
        assertEquals("—", counts.getString("mm-18"));
        assertEquals(1, counts.getInt("mm-5"));
        assertEquals(2, counts.getInt("query-errors"));
    }

    @Test
    public void nullQueryResultIsReportedAsUnavailable() throws Exception {
        reportDao.when(() -> ReportDao.getReportBreakdown(Mockito.anyString(), Mockito.<String>anyList())).thenReturn(null);
        JSONObject counts = report.getIndicatorData();
        for (int i = 1; i <= 18; i++) assertEquals("—", counts.getString("mm-" + i));
        assertTrue(counts.getInt("query-errors") > 0);
    }

    private void event(String id, String type, String date) {
        database.execSQL("INSERT INTO event VALUES (?, ?, ?)", new Object[]{id, type, date});
    }

    private void iit(String id) {
        database.execSQL("INSERT INTO ec_mothermentor_enroll_it VALUES (?)", new Object[]{id});
        event(id, "Mother Mentor Enroll IIT", "2026-03-01 03:00:00");
    }

    private void tracing(String visitId, String client, String date, String outcome, String returned, String returnDate) {
        visit(visitId, client, date, 1);
        detail(visitId, "iit_tracing_visit", "yes");
        detail(visitId, "iit_tracing_outcome", outcome);
        detail(visitId, "returned_to_art", returned);
        if (returnDate != null) detail(visitId, "art_return_date", returnDate);
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
        member(id, gender, dob);
        database.execSQL("INSERT INTO ec_mothermentor_enrollment (base_entity_id, is_closed) VALUES (?, 0)", new Object[]{id});
        event(id, "Mother Mentor Enrollment", "2026-03-01 03:00:00");
    }

    private void member(String id, String gender, String dob) {
        database.execSQL("INSERT INTO ec_family_member VALUES (?, ?, ?)", new Object[]{id, gender, dob});
    }

    private void household(String id, String consent) {
        database.execSQL("INSERT INTO ec_mothermentor_household_enrolment VALUES (?, ?)", new Object[]{id, consent});
    }

    private void visit(String visitId, String clientId, String serviceDate, int processed) {
        database.execSQL("INSERT INTO visits VALUES (?, ?, 'MotherMentor Services', ?, 0)", new Object[]{visitId, clientId, processed});
        detail(visitId, "mothermentor_visit_date", serviceDate);
    }

    private void detail(String visitId, String key, String value) {
        // Visit-detail processed flags remain zero locally even when their parent visit is finalized.
        database.execSQL("INSERT INTO visit_details VALUES (?, ?, ?, 0)", new Object[]{visitId, key, value});
    }
}
