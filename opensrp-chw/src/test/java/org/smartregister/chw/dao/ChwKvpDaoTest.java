package org.smartregister.chw.dao;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChwKvpDaoTest {

    @Test
    public void retestIsNotDueWithoutDatedNegativeResult() throws Exception {
        Date referenceDate = date("15-04-2026");

        assertFalse(ChwKvpDao.isHivRetestDue(Collections.emptyList(), referenceDate));
        assertFalse(ChwKvpDao.isHivRetestDue(Collections.singletonList(
                new ChwKvpDao.HivTestRecord(null, "negative")), referenceDate));
        assertFalse(ChwKvpDao.isHivRetestDue(Collections.singletonList(
                new ChwKvpDao.HivTestRecord("not-a-date", "negative")), referenceDate));
        assertFalse(ChwKvpDao.isHivRetestDue(Collections.singletonList(
                new ChwKvpDao.HivTestRecord("15-01-2026", "positive")), referenceDate));
        assertTrue(ChwKvpDao.isHivRetestDue(Collections.singletonList(
                new ChwKvpDao.HivTestRecord("15-01-2026", "hana maambukizi")), referenceDate));
    }

    @Test
    public void retestBecomesDueThreeCalendarMonthsAfterNegativeTest() throws Exception {
        ChwKvpDao.HivTestRecord record = new ChwKvpDao.HivTestRecord("15-01-2026", "negative");

        assertFalse(ChwKvpDao.isHivRetestDue(Collections.singletonList(record), date("14-04-2026")));
        assertTrue(ChwKvpDao.isHivRetestDue(Collections.singletonList(record), date("15-04-2026")));
        assertTrue(ChwKvpDao.isHivRetestDue(Collections.singletonList(record), date("16-04-2026")));
    }

    @Test
    public void latestValidTestDateControlsRetestEligibility() throws Exception {
        assertFalse(ChwKvpDao.isHivRetestDue(Arrays.asList(
                new ChwKvpDao.HivTestRecord("01-01-2026", "negative"),
                new ChwKvpDao.HivTestRecord("01-03-2026", "negative")), date("15-04-2026")));

        assertFalse(ChwKvpDao.isHivRetestDue(Arrays.asList(
                new ChwKvpDao.HivTestRecord("01-01-2026", "negative"),
                new ChwKvpDao.HivTestRecord("2026-03-01T10:30:00.000+03:00", "positive")), date("15-04-2026")));
    }

    @Test
    public void swahiliNegativeResultAndIsoDateAreSupported() throws Exception {
        assertTrue(ChwKvpDao.isHivRetestDue(Collections.singletonList(
                new ChwKvpDao.HivTestRecord("2026-01-15T10:30:00.000+03:00", "Hasi")), date("15-04-2026")));
    }

    private Date date(String value) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ROOT);
        dateFormat.setLenient(false);
        return dateFormat.parse(value);
    }
}
