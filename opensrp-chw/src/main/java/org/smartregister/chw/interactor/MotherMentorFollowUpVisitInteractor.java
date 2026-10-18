package org.smartregister.chw.interactor;

import org.smartregister.chw.R;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.util.MotherMentorFollowUpType;

/** Mother IIT-specific follow-up workflow. */
public class MotherMentorFollowUpVisitInteractor extends BaseMotherMentorFollowUpVisitInteractor {
    public MotherMentorFollowUpVisitInteractor(String eventType) {
        this(eventType, MotherMentorFollowUpType.MOTHER_IIT.getValue());
    }

    public MotherMentorFollowUpVisitInteractor(String eventType, String category) {
        super(
                new FollowUpFormDefinition(
                        eventType,
                        category,
                        R.string.mothermentor_follow_up_status_section,
                        R.string.mothermentor_interruption_reason_section,
                        R.string.mothermentor_return_to_facility_section,
                        R.string.mothermentor_comments_section,
                        "mothermentor_follow_up_status",
                        "mothermentor_service_interruption_reason",
                        "mothermentor_return_to_facility",
                        "mothermentor_follow_up_comment",
                        DBConstants.KEY.SERVICE_INTERRUPTION_REASON,
                        DBConstants.KEY.SERVICE_INTERRUPTION_REASON_OTHER,
                        DBConstants.KEY.EXPECTED_FACILITY_VISIT_DATE,
                        DBConstants.KEY.RETURNED_TO_FACILITY,
                        DBConstants.KEY.FACILITY_RETURN_DATE,
                        "mothermentor_reason_"));
    }
}
