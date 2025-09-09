package org.smartregister.chw.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO representing a row in ec_ayp_in_school_group_details.
 * Mirrors the fields from ayp_in_school_group_creation.json.
 */
public class AypInSchoolGroupDetails {

    private String baseEntityId;
    private String providerId;
    private String groupName;   // step1.group_name
    private String groupType;   // step1.group_type (age_band | classes)
    private String ageBand;     // step1.age_band (when relevant)
    private String encounterDate; // metadata.today (stored as text)
    private Long lastInteractedWith; // epoch millis

    // Allow carrying extra simple key/value fields if needed
    private final Map<String, String> extra = new HashMap<>();

    public String getBaseEntityId() {
        return baseEntityId;
    }

    public void setBaseEntityId(String baseEntityId) {
        this.baseEntityId = baseEntityId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    public String getAgeBand() {
        return ageBand;
    }

    public void setAgeBand(String ageBand) {
        this.ageBand = ageBand;
    }

    public String getEncounterDate() {
        return encounterDate;
    }

    public void setEncounterDate(String encounterDate) {
        this.encounterDate = encounterDate;
    }

    public Long getLastInteractedWith() {
        return lastInteractedWith;
    }

    public void setLastInteractedWith(Long lastInteractedWith) {
        this.lastInteractedWith = lastInteractedWith;
    }



    public Map<String, String> getExtra() {
        return extra;
    }

    // Fluent helpers
    public AypInSchoolGroupDetails putExtra(String key, String value) {
        if (key != null && value != null) extra.put(key, value);
        return this;
    }
}
