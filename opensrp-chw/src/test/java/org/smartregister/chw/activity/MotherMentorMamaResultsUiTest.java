package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.R;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.Answers;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.Entry;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.PendingTest;
import org.smartregister.chw.mothermentor.util.MotherMentorHeiSchedule.Stage;
import org.smartregister.util.LangUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, qualifiers = "sw")
public class MotherMentorMamaResultsUiTest {
    @Test
    public void profileAndResultListShowOnlyTheOverdueSampleAndUpdateAfterSave() {
        LangUtils.saveLanguage(RuntimeEnvironment.getApplication(), "sw");
        LocalDate today = LocalDate.now();
        Entry birth = entry(Stage.AT_BIRTH, today.minusDays(8), true, false);
        Entry routine = entry(Stage.SIX_WEEKS, today.minusDays(14), false, false);
        Entry savedBirth = entry(Stage.AT_BIRTH, today.minusDays(8), true, true);
        List<Entry> initial = Arrays.asList(birth, routine);
        List<Entry> afterSave = Arrays.asList(savedBirth, routine);

        MotherMentorProfileActivity profile = Robolectric.buildActivity(MotherMentorProfileActivity.class).get();
        MotherMentorMotherResultUi profileUi = new MotherMentorMotherResultUi(profile);
        LinearLayout profileRow = new LinearLayout(profile);
        TextView profileTitle = addLabel(profileRow,
                org.smartregister.chw.mothermentor.R.id.mothermentor_mother_test_results_title);
        TextView profileSubtitle = addLabel(profileRow,
                org.smartregister.chw.mothermentor.R.id.mothermentor_mother_test_results_subtitle);
        bind(profileUi, profileRow, initial, today);
        assertEquals("Majibu ya Vipimo", profileTitle.getText().toString());
        assertTrue(profileSubtitle.getText().toString().contains("Wakati wa kuzaliwa · ⚠ Majibu yamechelewa"));
        assertFalse(profileSubtitle.getText().toString().contains("Wiki 6"));
        bind(profileUi, profileRow, afterSave, today);
        assertFalse(profileSubtitle.getText().toString().contains("Majibu yamechelewa"));
        bind(profileUi, profileRow, Collections.singletonList(routine), today);
        assertEquals("Majibu ya Kipimo", profileTitle.getText().toString());
        bind(profileUi, profileRow, Collections.emptyList(), today);
        assertEquals(View.GONE, profileRow.getVisibility());

        Activity host = Robolectric.buildActivity(Activity.class).setup().get();
        View root = host.getLayoutInflater().inflate(R.layout.activity_mothermentor_test_results, null);
        MotherMentorTestResultsActivity results = spy(Robolectric.buildActivity(MotherMentorTestResultsActivity.class).get());
        doAnswer(call -> root.findViewById(call.getArgument(0))).when(results).findViewById(anyInt());
        LinearLayout content = root.findViewById(R.id.mama_results_content);
        ReflectionHelpers.setField(results, "content", content);
        render(results, initial);
        TextView heading = root.findViewById(R.id.mama_results_heading);
        assertEquals("Majibu ya Vipimo", heading.getText().toString());
        assertEquals("⚠ Majibu yamechelewa", note(content, 0).getText().toString());
        assertEquals(View.GONE, note(content, 1).getVisibility());
        render(results, afterSave);
        assertEquals(results.getString(R.string.mama_results_edit_today), note(content, 0).getText().toString());
        assertEquals(View.GONE, note(content, 1).getVisibility());
        render(results, Collections.singletonList(routine));
        assertEquals("Majibu ya Kipimo", heading.getText().toString());
        render(results, Collections.emptyList());
        assertEquals(results.getString(R.string.mama_results_no_entries),
                ((TextView) content.getChildAt(0)).getText().toString());
    }

    private static Entry entry(Stage stage, LocalDate date, boolean overdue, boolean recorded) {
        Entry entry = mock(Entry.class);
        PendingTest sample = mock(PendingTest.class);
        when(sample.getStage()).thenReturn(stage);
        when(sample.getTestDate()).thenReturn(date);
        when(sample.isOverdue(LocalDate.now())).thenReturn(overdue);
        when(entry.getSample()).thenReturn(sample);
        when(entry.isRecorded()).thenReturn(recorded);
        if (recorded) when(entry.getAnswers()).thenReturn(new Answers("negative", date, "", ""));
        return entry;
    }

    private static TextView addLabel(LinearLayout row, int id) {
        TextView label = new TextView(row.getContext());
        label.setId(id);
        row.addView(label);
        return label;
    }

    private static void bind(MotherMentorMotherResultUi ui, View row, List<Entry> entries, LocalDate today) {
        ReflectionHelpers.callInstanceMethod(ui, "bind",
                ReflectionHelpers.ClassParameter.from(View.class, row),
                ReflectionHelpers.ClassParameter.from(String.class, "synthetic-mother"),
                ReflectionHelpers.ClassParameter.from(List.class, entries),
                ReflectionHelpers.ClassParameter.from(LocalDate.class, today));
    }

    private static void render(MotherMentorTestResultsActivity activity, List<Entry> entries) {
        ReflectionHelpers.callInstanceMethod(activity, "renderList",
                ReflectionHelpers.ClassParameter.from(List.class, entries));
    }

    private static TextView note(LinearLayout content, int row) {
        return content.getChildAt(row).findViewById(R.id.mama_results_note);
    }
}
