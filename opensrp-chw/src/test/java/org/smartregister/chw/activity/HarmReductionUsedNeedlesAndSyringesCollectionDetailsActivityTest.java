package org.smartregister.chw.activity;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

public class HarmReductionUsedNeedlesAndSyringesCollectionDetailsActivityTest {

    @Test
    public void collectionTimestampDoesNotAddDeviceTimezoneOffset() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Africa/Dar_es_Salaam"));
            Date storedWallClock = new Date(1784547290000L);

            Assert.assertEquals(
                    "20-07-2026 11:34:50",
                    HarmReductionUsedNeedlesAndSyringesCollectionDetailsActivity
                            .formatCollectionVisitTimestamp(storedWallClock)
            );
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
