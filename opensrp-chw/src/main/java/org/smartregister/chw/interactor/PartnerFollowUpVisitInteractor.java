package org.smartregister.chw.interactor;

import org.smartregister.chw.R;
import org.smartregister.chw.util.MotherMentorFollowUpType;

/** Partner-specific follow-up workflow with an independent form and event contract. */
public class PartnerFollowUpVisitInteractor extends BaseMotherMentorFollowUpVisitInteractor {
    public PartnerFollowUpVisitInteractor(String eventType) {
        super(new FollowUpFormDefinition(eventType, MotherMentorFollowUpType.PARTNER.getValue(),
                R.string.mothermentor_follow_up_status_section,
                R.string.mothermentor_partner_non_attendance_reason_section,
                R.string.mothermentor_partner_arrival_section,
                R.string.mothermentor_comments_section,
                "mothermentor_partner_follow_up_status",
                "mothermentor_partner_non_attendance_reason",
                "mothermentor_partner_arrival",
                "mothermentor_partner_follow_up_comment",
                "partner_non_attendance_reason",
                "partner_non_attendance_reason_other",
                "partner_expected_facility_visit_date",
                "partner_arrived_at_facility",
                "partner_facility_arrival_date",
                "mothermentor_partner_reason_"));
    }
}
