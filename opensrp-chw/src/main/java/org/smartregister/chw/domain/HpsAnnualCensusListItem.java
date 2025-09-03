package org.smartregister.chw.domain;

public class HpsAnnualCensusListItem {
    private final String baseEntityId;
    private final String year;

    public HpsAnnualCensusListItem(String baseEntityId, String year) {
        this.baseEntityId = baseEntityId;
        this.year = year;
    }

    public String getBaseEntityId() {
        return baseEntityId;
    }

    public String getYear() {
        return year;
    }
}

