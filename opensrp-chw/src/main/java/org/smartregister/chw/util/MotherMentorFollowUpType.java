package org.smartregister.chw.util;

import android.text.TextUtils;

import org.smartregister.chw.mothermentor.util.Constants;

/** Stable routing identity for the three Mother Mentor follow-up registers. */
public enum MotherMentorFollowUpType {
    MOTHER_IIT("MOTHER_IIT", Constants.TABLES.MOTHERMENTOR_ENROLL_IIT,
            Constants.EVENT_TYPE.MOTHER_MENTOR_FOLLOW_UP_VISIT),
    PARTNER("PARTNER", Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER,
            "MotherMentor Partner Follow-up Visit"),
    CHILD_EID("CHILD_EID", Constants.TABLES.MOTHERMENTOR_ENROLL_CHILD_EID,
            "MotherMentor Child EID Follow-up Visit");

    public static final String EXTRA_FOLLOW_UP_TYPE = "mother_mentor_follow_up_type";
    public static final String FOLLOW_UP_TABLE = "ec_mothermentor_followup_visit";

    private final String value;
    private final String enrollmentTable;
    private final String eventType;

    MotherMentorFollowUpType(String value, String enrollmentTable, String eventType) {
        this.value = value;
        this.enrollmentTable = enrollmentTable;
        this.eventType = eventType;
    }

    public String getValue() { return value; }
    public String getEnrollmentTable() { return enrollmentTable; }
    public String getEventType() { return eventType; }

    public static MotherMentorFollowUpType fromValue(String value) {
        if (!TextUtils.isEmpty(value)) {
            for (MotherMentorFollowUpType type : values()) {
                if (type.value.equals(value)) return type;
            }
        }
        return MOTHER_IIT;
    }

    public static MotherMentorFollowUpType fromEnrollmentTable(String tableName) {
        if (!TextUtils.isEmpty(tableName)) {
            for (MotherMentorFollowUpType type : values()) {
                if (type.enrollmentTable.equals(tableName)) return type;
            }
        }
        return MOTHER_IIT;
    }
}
