package org.smartregister.chw.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
import org.smartregister.chw.mothermentor.fragment.BaseMotherMentorRegisterFragment;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Binds the real follow-up row, including recycled rows whose DOB cannot be used. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class MotherMentorFollowUpRegisterAgeTest {
    private Context context;
    private MotherMentorFollowUpRegisterProvider provider;
    private View.OnClickListener clickListener;

    @Before
    public void setUp() {
        DateTimeUtils.setCurrentMillisFixed(
                new LocalDate(2026, 10, 26).toDateTimeAtStartOfDay().plusHours(12).getMillis());
        context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_AppCompat);
        clickListener = mock(View.OnClickListener.class);
        provider = new MotherMentorFollowUpRegisterProvider(context, null, clickListener);
    }

    @After
    public void restoreClock() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    @Config(qualifiers = "en")
    public void englishInfantRowShowsCompletedMonthsInsteadOfZeroYears() {
        assertEquals("Amina Jaribio Mtoto, 6 months", bind("2026-04-26").name.getText().toString());
    }

    @Test
    @Config(qualifiers = "sw")
    public void swahiliInfantRowShowsLocalizedMonths() {
        assertEquals("Amina Jaribio Mtoto, Miezi 6", bind("2026-04-26").name.getText().toString());
    }

    @Test
    @Config(qualifiers = "en")
    public void englishNewbornRowShowsDays() {
        assertEquals("Amina Jaribio Mtoto, 10 days", bind("2026-10-16").name.getText().toString());
    }

    @Test
    @Config(qualifiers = "sw")
    public void swahiliNewbornRowShowsLocalizedDays() {
        assertEquals("Amina Jaribio Mtoto, Siku 10", bind("2026-10-16").name.getText().toString());
    }

    @Test
    @Config(qualifiers = "en")
    public void birthTodayIsZeroDaysRatherThanZeroYears() {
        assertEquals("Amina Jaribio Mtoto, 0 days", bind("2026-10-26").name.getText().toString());
    }

    @Test
    @Config(qualifiers = "en")
    public void firstBirthdayAndAdultRowsKeepExistingYearDisplay() {
        assertEquals("Amina Jaribio Mtoto, 1", bind("2025-10-26").name.getText().toString());
        assertEquals("Amina Jaribio Mtoto, 30", bind("1996-10-26").name.getText().toString());
    }

    @Test
    @Config(qualifiers = "sw")
    public void isoDateWithTimeUsesItsRecordedCalendarDate() {
        assertEquals(
                "Amina Jaribio Mtoto, Miezi 6",
                bind("2026-04-26T21:00:00.000Z").name.getText().toString());
    }

    @Test
    @Config(qualifiers = "en")
    public void unusableBirthDateDoesNotLeaveStaleNameMetadataOrClickTag() {
        MotherMentorFollowUpRegisterProvider.Holder holder = bind("1996-10-26");
        for (String dob : Arrays.asList(null, "", "  ", "not-a-date", "2026-02-30", "2026-10-27")) {
            CommonPersonObjectClient replacement = client("replacement", dob);
            provider.getView(null, replacement, holder);

            assertEquals("DOB: " + dob, "Amina Jaribio Mtoto", holder.name.getText().toString());
            assertFalse(holder.name.getText().toString().contains(", 0"));
            assertEquals("Mikocheni · 1095", holder.metadata.getText().toString());
            assertSame(replacement, holder.row.getTag());
            assertEquals(
                    BaseMotherMentorRegisterFragment.CLICK_VIEW_NORMAL,
                    holder.row.getTag(R.id.VIEW_ID));
        }
        assertTrue(holder.row.performClick());
        verify(clickListener).onClick(holder.row);
    }

    private MotherMentorFollowUpRegisterProvider.Holder bind(String dob) {
        MotherMentorFollowUpRegisterProvider.Holder holder =
                provider.createViewHolder(new FrameLayout(context));
        provider.getView(null, client("child", dob), holder);
        return holder;
    }

    private CommonPersonObjectClient client(String id, String dob) {
        Map<String, String> columns = new HashMap<>();
        columns.put(DBConstants.KEY.FIRST_NAME, "Amina");
        columns.put(DBConstants.KEY.MIDDLE_NAME, "Jaribio");
        columns.put(DBConstants.KEY.LAST_NAME, "Mtoto");
        columns.put(DBConstants.KEY.DOB, dob);
        columns.put(DBConstants.KEY.VILLAGE_TOWN, "Mikocheni");
        columns.put(DBConstants.KEY.UNIQUE_ID, "1095");
        CommonPersonObjectClient person = new CommonPersonObjectClient(id, columns, "");
        person.setColumnmaps(columns);
        return person;
    }
}
