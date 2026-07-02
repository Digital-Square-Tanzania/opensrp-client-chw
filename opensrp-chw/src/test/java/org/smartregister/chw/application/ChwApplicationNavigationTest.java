package org.smartregister.chw.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.smartregister.chw.activity.NcdRegisterActivity;
import org.smartregister.chw.core.utils.CoreConstants;

import java.util.Map;

public class ChwApplicationNavigationTest {

    @Test
    public void ncdRegisterActivityIsRegistered() {
        ChwApplication chwApplication = new ChwApplication();
        Map<String, Class> activities = chwApplication.getRegisteredActivities();
        assertTrue(activities.containsKey(CoreConstants.REGISTERED_ACTIVITIES.NCD_REGISTER_ACTIVITY));
        assertEquals(NcdRegisterActivity.class, activities.get(CoreConstants.REGISTERED_ACTIVITIES.NCD_REGISTER_ACTIVITY));
    }
}
