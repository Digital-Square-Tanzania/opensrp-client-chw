package org.smartregister.chw.domain;

public class HpsAnnualCensusListItem {
    private final String baseEntityId;
    private final String year;
    private final boolean incomplete; // true when latest visit is saved but unprocessed

    public HpsAnnualCensusListItem(String baseEntityId, String year) {
        this(baseEntityId, year, false);
    }

    public HpsAnnualCensusListItem(String baseEntityId, String year, boolean incomplete) {
        this.baseEntityId = baseEntityId;
        this.year = year;
        this.incomplete = incomplete;
    }

    public String getBaseEntityId() {
        return baseEntityId;
    }

    public String getYear() {
        return year;
    }

    public boolean isIncomplete() {
        return incomplete;
    }
}
