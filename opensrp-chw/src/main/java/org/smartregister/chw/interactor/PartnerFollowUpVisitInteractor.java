package org.smartregister.chw.interactor;

import org.smartregister.chw.R;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.util.MotherMentorFollowUpType;

/** Partner-specific follow-up workflow with an independent form and event contract. */
public class PartnerFollowUpVisitInteractor extends BaseMotherMentorFollowUpVisitInteractor {
    public PartnerFollowUpVisitInteractor(String eventType) {
        super(
                new FollowUpFormDefinition(
                        eventType,
                        MotherMentorFollowUpType.PARTNER.getValue(),
                        R.string.mothermentor_follow_up_status_section,
                        R.string.mothermentor_partner_non_attendance_reason_section,
                        R.string.mothermentor_partner_arrival_section,
                        R.string.mothermentor_comments_section,
                        "mothermentor_partner_follow_up_status",
                        "mothermentor_partner_non_attendance_reason",
                        "mothermentor_partner_arrival",
                        "mothermentor_partner_follow_up_comment",
                        DBConstants.KEY.PARTNER_NON_ATTENDANCE_REASON,
                        DBConstants.KEY.PARTNER_NON_ATTENDANCE_REASON_OTHER,
                        DBConstants.KEY.PARTNER_EXPECTED_FACILITY_VISIT_DATE,
                        DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY,
                        DBConstants.KEY.PARTNER_FACILITY_ARRIVAL_DATE,
                        "mothermentor_partner_reason_"));
    }
}
