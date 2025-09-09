package org.smartregister.chw.domain;

/**
 * Lightweight model for displaying AYP in-school group rows on the register list.
 */
public class AypInSchoolGroupListItem {
    private final String baseEntityId;
    private final String groupName;
    private final String groupType;
    private final String ageBand;

    public AypInSchoolGroupListItem(String baseEntityId, String groupName, String groupType, String ageBand) {
        this.baseEntityId = baseEntityId;
        this.groupName = groupName;
        this.groupType = groupType;
        this.ageBand = ageBand;
    }

    public String getBaseEntityId() { return baseEntityId; }

    public String getGroupName() { return groupName; }

    public String getGroupType() { return groupType; }

    public String getAgeBand() { return ageBand; }
}

