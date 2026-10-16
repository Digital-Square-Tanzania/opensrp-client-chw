package org.smartregister.chw.interactor;

import org.smartregister.chw.R;
import org.smartregister.chw.util.MotherMentorFollowUpType;

/** Mother IIT-specific follow-up workflow. */
public class MotherMentorFollowUpVisitInteractor extends BaseMotherMentorFollowUpVisitInteractor {
    public MotherMentorFollowUpVisitInteractor(String eventType) {
        this(eventType, MotherMentorFollowUpType.MOTHER_IIT.getValue());
    }

    public MotherMentorFollowUpVisitInteractor(String eventType, String category) {
        super(new FollowUpFormDefinition(eventType, category,
                R.string.mothermentor_follow_up_status_section,
                R.string.mothermentor_interruption_reason_section,
                R.string.mothermentor_return_to_facility_section,
                R.string.mothermentor_comments_section,
                "mothermentor_follow_up_status",
                "mothermentor_service_interruption_reason",
                "mothermentor_return_to_facility",
                "mothermentor_follow_up_comment",
                "service_interruption_reason",
                "service_interruption_reason_other",
                "expected_facility_visit_date",
                "returned_to_facility",
                "facility_return_date",
                "mothermentor_reason_"));
    }
}
