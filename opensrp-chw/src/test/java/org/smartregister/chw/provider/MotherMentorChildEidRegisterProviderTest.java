package org.smartregister.chw.provider;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.Application;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.FrameLayout;

import org.joda.time.DateTimeUtils;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.chw.R;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class MotherMentorChildEidRegisterProviderTest {
    private Context context;
    private MotherMentorChildEidRegisterProvider provider;
    private View.OnClickListener listener;

    @Before
    public void setUp() {
        DateTimeUtils.setCurrentMillisFixed(new LocalDate(2026, 11, 4).toDateTimeAtStartOfDay().getMillis());
        context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_AppCompat);
        listener = mock(View.OnClickListener.class);
        provider = new MotherMentorChildEidRegisterProvider(context, null, listener);
    }

    @After
    public void restoreClock() { DateTimeUtils.setCurrentMillisSystem(); }

    @Test
    @Config(qualifiers = "en")
    public void englishRowUsesMonthsFullHeiIdentifierAndProjectedVisitCount() {
        MotherMentorChildEidRegisterProvider.ChildHolder holder = bind(client("child", values()));
        assertEquals("Baraka John, 19 months", holder.name.getText().toString());
        assertEquals("To be referred · Kibiti", holder.metadata.getText().toString());
        assertEquals("Missed 9 months — due 2026-10-04", holder.missed.getText().toString());
        assertEquals("Mother: Neema John · HEI-0412-C01 · Visit 3", holder.summary.getText().toString());
        assertTrue(holder.row.getContentDescription().toString().contains("HEI-0412-C01"));
    }

    @Test
    @Config(qualifiers = "sw")
    public void swahiliReferralAndMissedTestUseTheSameClinicalStage() {
        Map<String, String> values = values();
        values.put(DBConstants.KEY.MTOTO_RUFAA_STATE, "referred");
        values.put(DBConstants.KEY.MTOTO_RUFAA_TEST, "miezi9");
        MotherMentorChildEidRegisterProvider.ChildHolder holder = bind(client("child", values));
        assertEquals("Baraka John, Miezi 19", holder.name.getText().toString());
        assertEquals("Amepewa rufaa: Miezi 9 · Kibiti", holder.metadata.getText().toString());
        assertEquals("Amekosa Miezi 9 — kilitakiwa 2026-10-04", holder.missed.getText().toString());
        assertEquals("Mama: Neema John · HEI-0412-C01 · Ziara 3", holder.summary.getText().toString());
    }

    @Test
    public void closedHistoryRemainsClickableAndRecycledRowClearsPriorChildDetails() {
        Map<String, String> values = values();
        values.put(DBConstants.KEY.MTOTO_IS_CLOSED, "1");
        CommonPersonObjectClient closed = client("closed", values);
        MotherMentorChildEidRegisterProvider.ChildHolder holder = bind(closed);
        assertEquals(View.VISIBLE, holder.closedTag.getVisibility());
        assertEquals(0.55f, holder.row.getAlpha(), 0f);
        assertTrue(holder.row.performClick());
        verify(listener).onClick(holder.row);
        assertSame(closed, holder.row.getTag());
        Map<String, String> empty = new HashMap<>();
        empty.put(DBConstants.KEY.FIRST_NAME, "Second");
        empty.put(DBConstants.KEY.IS_CLOSED, "1"); // A member flag is not the EID enrollment flag.
        CommonPersonObjectClient active = client("active", empty);
        provider.getView(null, active, holder);
        assertEquals(View.GONE, holder.closedTag.getVisibility());
        assertEquals(View.GONE, holder.missed.getVisibility());
        assertEquals(1f, holder.row.getAlpha(), 0f);
        assertEquals("Second", holder.name.getText().toString());
        assertFalse(holder.summary.getText().toString().contains("HEI-0412-C01"));
        assertSame(active, holder.row.getTag());
    }

    private MotherMentorChildEidRegisterProvider.ChildHolder bind(CommonPersonObjectClient client) {
        MotherMentorChildEidRegisterProvider.ChildHolder holder =
                (MotherMentorChildEidRegisterProvider.ChildHolder) provider.createViewHolder(new FrameLayout(context));
        provider.getView(null, client, holder);
        return holder;
    }

    private Map<String, String> values() {
        Map<String, String> values = new HashMap<>();
        values.put(DBConstants.KEY.FIRST_NAME, "Baraka");
        values.put(DBConstants.KEY.LAST_NAME, "John");
        values.put(DBConstants.KEY.CHILD_DOB, "2025-04-04");
        values.put(DBConstants.KEY.MOTHER_NAME, "Neema John");
        values.put(DBConstants.KEY.VILLAGE_TOWN, "Kibiti");
        values.put(DBConstants.KEY.HEI_NUMBER, "HEI-0412-C01");
        values.put(DBConstants.KEY.HEI_FOLLOW_UP_NUMBER, "1");
        values.put(DBConstants.KEY.MTOTO_MISSED_TEST, "miezi9");
        values.put(DBConstants.KEY.MTOTO_MISSED_DUE, "2026-10-04");
        values.put(DBConstants.KEY.MTOTO_RUFAA_STATE, "none");
        values.put(DBConstants.KEY.MTOTO_VISIT_COUNT, "3");
        return values;
    }

    private CommonPersonObjectClient client(String id, Map<String, String> values) {
        CommonPersonObjectClient client = new CommonPersonObjectClient(id, values, "");
        client.setColumnmaps(values);
        return client;
    }
}
