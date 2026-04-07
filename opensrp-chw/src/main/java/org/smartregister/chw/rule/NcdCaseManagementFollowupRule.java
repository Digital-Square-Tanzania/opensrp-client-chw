package org.smartregister.chw.rule;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.smartregister.chw.core.utils.CoreConstants;

import java.util.Date;

/**
 * Computes due/overdue/expiry dates for NCD Case Management monthly follow-up visits.
 * Follows the CbhsFollowupRule pattern — self-contained date computation, no rules engine needed.
 *
 * Schedule: due at +30d, overdue at +37d (7-day grace), expiry at +365d
 * Reference: lastVisitDate if available, otherwise confirmationDate.
 */
public class NcdCaseManagementFollowupRule {

    private static final int DUE_DAYS = 30;
    private static final int OVERDUE_DAYS = 37;
    private static final int EXPIRY_DAYS = 365;

    private final DateTime confirmationDate;
    private final DateTime lastVisitDate;
    private DateTime dueDate;
    private DateTime overDueDate;
    private DateTime expiryDate;

    public NcdCaseManagementFollowupRule(Date confirmationDate, Date lastVisitDate) {
        this.confirmationDate = confirmationDate != null ? new DateTime(confirmationDate) : null;
        this.lastVisitDate = lastVisitDate != null ? new DateTime(lastVisitDate) : null;
        computeDates();
    }

    private void computeDates() {
        DateTime referenceDate = lastVisitDate != null ? lastVisitDate : confirmationDate;
        if (referenceDate == null) {
            return;
        }

        this.dueDate = referenceDate.plusDays(DUE_DAYS);
        this.overDueDate = referenceDate.plusDays(OVERDUE_DAYS);
        this.expiryDate = referenceDate.plusDays(EXPIRY_DAYS);
    }

    public Date getDueDate() {
        return dueDate != null ? dueDate.toDate() : null;
    }

    public Date getOverDueDate() {
        return overDueDate != null ? overDueDate.toDate() : null;
    }

    public Date getExpiryDate() {
        return expiryDate != null ? expiryDate.toDate() : null;
    }

    public int getDaysDifference() {
        if (dueDate == null) {
            return 0;
        }
        return Days.daysBetween(new DateTime(), dueDate).getDays();
    }

    public String getButtonStatus() {
        if (dueDate == null || expiryDate == null || overDueDate == null) {
            return CoreConstants.VISIT_STATE.NOT_DUE_YET;
        }

        DateTime currentDate = new DateTime(new LocalDate().toDate());

        if (currentDate.isBefore(expiryDate)) {
            if (currentDate.isAfter(overDueDate) || currentDate.isEqual(overDueDate)) {
                return CoreConstants.VISIT_STATE.OVERDUE;
            }
            if ((currentDate.isAfter(dueDate) || currentDate.isEqual(dueDate))
                    && currentDate.isBefore(overDueDate)) {
                return CoreConstants.VISIT_STATE.DUE;
            }
            if (lastVisitDate != null && currentDate.isEqual(lastVisitDate)) {
                return CoreConstants.VISIT_STATE.VISIT_DONE;
            }
            return CoreConstants.VISIT_STATE.NOT_DUE_YET;
        }
        return CoreConstants.VISIT_STATE.EXPIRED;
    }
}
