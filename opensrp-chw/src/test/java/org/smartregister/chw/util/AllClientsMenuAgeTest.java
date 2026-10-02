package org.smartregister.chw.util;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@Config(sdk = 28)
public class AllClientsMenuAgeTest extends BaseUnitTest {
    @Test
    public void missingOrEmptyBirthDateHasUnknownAge() {
        assertEquals(-1, age(null));
        assertEquals(-1, age(""));
        assertEquals(-1, age("   "));
    }

    @Test
    public void malformedBirthDateHasUnknownAge() {
        assertEquals(-1, age("not-a-date"));
        assertEquals(-1, age("2026-02-30"));
    }

    @Test
    public void validBirthDateKeepsAgeBasedEligibility() {
        assertEquals(20, age(LocalDate.now().minusYears(20).toString()));
        assertEquals(0, age(LocalDate.now().toString()));
    }

    private int age(String dob) {
        Map<String, String> columns = new HashMap<>();
        if (dob != null) {
            columns.put("dob", dob);
        }
        CommonPersonObjectClient client = new CommonPersonObjectClient("test-client", columns, "");
        client.setColumnmaps(columns);
        return ReflectionHelpers.callStaticMethod(AllClientsUtils.class, "getPersonAge",
                ReflectionHelpers.ClassParameter.from(CommonPersonObjectClient.class, client));
    }
}
