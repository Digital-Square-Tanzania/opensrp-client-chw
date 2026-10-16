package org.smartregister.chw.interactor;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ChildEidFollowUpVisitInteractorTest {
    private final DateTime now = new DateTime(2026, 10, 16, 12, 0);

    @Test public void doesNotGuessForNewbornWithoutAnApplicableChoice() {
        assertNull(ChildEidFollowUpVisitInteractor.recommendTest("2026-10-01", null, null, now));
    }

    @Test public void followsVerifiedHeiAgeThresholds() {
        assertEquals(ChildEidFollowUpVisitInteractor.TEST_6_WEEKS,
                ChildEidFollowUpVisitInteractor.recommendTest("2026-08-01", null, null, now));
        assertEquals(ChildEidFollowUpVisitInteractor.TEST_9_MONTHS,
                ChildEidFollowUpVisitInteractor.recommendTest("2025-12-01", null, null, now));
        assertEquals(ChildEidFollowUpVisitInteractor.TEST_18_MONTHS,
                ChildEidFollowUpVisitInteractor.recommendTest("2025-03-01", "2025-12-01", null, now));
    }

    @Test public void postWeaningChoiceRequiresRecordedDateAndThreeElapsedMonths() {
        assertEquals(ChildEidFollowUpVisitInteractor.TEST_POST_WEANING,
                ChildEidFollowUpVisitInteractor.recommendTest("2025-08-01", "2026-06-01", null, now));
        assertEquals(ChildEidFollowUpVisitInteractor.TEST_9_MONTHS,
                ChildEidFollowUpVisitInteractor.recommendTest("2025-08-01", "2026-09-01", null, now));
    }

    @Test public void invalidOrFutureBirthDateNeverProducesARecommendation() {
        assertNull(ChildEidFollowUpVisitInteractor.recommendTest("not-a-date", null, null, now));
        assertNull(ChildEidFollowUpVisitInteractor.recommendTest("2027-01-01", null, null, now));
    }

    @Test public void doesNotRecommendARecordedCompletedAgeTestAgain() {
        assertNull(ChildEidFollowUpVisitInteractor.recommendTest("2026-08-01", null, "dbs_6", now));
        assertNull(ChildEidFollowUpVisitInteractor.recommendTest("2025-12-01", null, "dbs_9", now));
    }
}
