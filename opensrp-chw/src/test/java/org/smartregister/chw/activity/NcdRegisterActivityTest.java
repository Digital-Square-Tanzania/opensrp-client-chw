package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.smartregister.chw.BaseUnitTest;

public class NcdRegisterActivityTest extends BaseUnitTest {

    @Test
    public void openClientProfileShouldStartNcdProfileActivity() {
        NcdRegisterActivity activity = Robolectric.buildActivity(NcdRegisterActivity.class)
                .setup()
                .get();

        activity.openClientProfile("sample-base-entity", true);

        Intent startedIntent = Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull(startedIntent);
        assertEquals(NcdProfileActivity.class.getName(), startedIntent.getComponent().getClassName());
    }
}
