package org.smartregister.chw.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.LocalDate;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.Answers;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.Entry;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.PendingTest;
import org.smartregister.chw.mothermentor.util.AppExecutors;
import org.smartregister.chw.mothermentor.util.MotherMentorHeiSchedule;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherClosure;
import org.smartregister.chw.util.MotherMentorMotherClosureDialog;
import org.smartregister.view.activity.SecuredActivity;

import java.util.List;

import timber.log.Timber;

public class MotherMentorTestResultsActivity extends SecuredActivity {
    static final int EDIT_FROM_HISTORY = 1207;
    private static final String MOTHER_ID = "mother_id";
    private static final String SAMPLE_ID = "sample_id";
    private static final String REVISION = "revision";
    private static final String RESULT = "result";
    private static final String DATE = "date";
    private static final String CONFIRMATION_DONE = "confirmation_done";
    private static final String CONFIRMATION = "confirmation";

    private final AppExecutors executors = new AppExecutors();
    private String motherId;
    private String sampleId;
    private Bundle restoredForm;
    private LinearLayout content;
    private Entry entry;
    private RadioGroup result;
    private RadioGroup confirmationDone;
    private RadioGroup confirmation;
    private Button dateButton;
    private LocalDate receivedDate;
    private boolean saving;

    static void startMe(Activity activity, String motherId) {
        activity.startActivity(new Intent(activity, MotherMentorTestResultsActivity.class)
                .putExtra(MOTHER_ID, motherId));
    }

    static void editFromHistory(Activity activity, String motherId, String id) {
        activity.startActivityForResult(formIntent(activity, motherId, id), EDIT_FROM_HISTORY);
    }

    private static Intent formIntent(Activity activity, String motherId, String id) {
        return new Intent(activity, MotherMentorTestResultsActivity.class)
                .putExtra(MOTHER_ID, motherId).putExtra(SAMPLE_ID, id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        restoredForm = savedInstanceState;
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onCreation() {
        motherId = getIntent().getStringExtra(MOTHER_ID);
        sampleId = getIntent().getStringExtra(SAMPLE_ID);
        if (motherId == null || motherId.isEmpty()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_mothermentor_test_results);
        content = findViewById(R.id.mama_results_content);
        ImageButton navigation = findViewById(R.id.mama_results_navigation);
        navigation.setOnClickListener(view -> finish());
        if (sampleId != null) {
            navigation.setImageResource(org.smartregister.chw.core.R.mipmap.ic_cross_white);
            navigation.setContentDescription(getText(R.string.mama_results_close));
            findViewById(R.id.mama_results_toolbar).setBackgroundColor(Color.rgb(32, 45, 61));
            View save = findViewById(R.id.mama_results_save_top);
            save.setVisibility(View.VISIBLE);
            save.setOnClickListener(view -> submit());
        }
    }

    @Override
    protected void onResumption() {
        if (content == null) return;
        if (sampleId == null) {
            executors.diskIO().execute(() -> {
                try {
                    List<Entry> entries = MotherMentorMotherResultEvent.visible(database(), motherId, LocalDate.now());
                    executors.mainThread().execute(() -> renderList(entries));
                } catch (RuntimeException error) {
                    failedToLoad(error);
                }
            });
        } else if (entry == null) {
            executors.diskIO().execute(() -> {
                try {
                    Entry editable = MotherMentorMotherResultEvent.editable(database(), motherId, sampleId, LocalDate.now());
                    executors.mainThread().execute(() -> renderForm(editable));
                } catch (RuntimeException error) {
                    failedToLoad(error);
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (entry == null || result == null) return;
        state.putLong(REVISION, entry.getRevision());
        state.putInt(RESULT, result.getCheckedRadioButtonId());
        state.putString(DATE, receivedDate == null ? "" : receivedDate.toString());
        state.putInt(CONFIRMATION_DONE, confirmationDone.getCheckedRadioButtonId());
        state.putInt(CONFIRMATION, confirmation.getCheckedRadioButtonId());
    }

    private SQLiteDatabase database() {
        return ChwApplication.getInstance().getRepository().getWritableDatabase();
    }

    private void renderList(List<Entry> entries) {
        if (isFinishing() || isDestroyed()) return;
        findViewById(R.id.mama_results_loading).setVisibility(View.GONE);
        findViewById(R.id.mama_results_footer).setVisibility(View.VISIBLE);
        TextView heading = findViewById(R.id.mama_results_heading);
        heading.setText(getResources().getQuantityText(R.plurals.mama_results_title, entries.size()));
        content.removeAllViews();
        if (entries.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.mama_results_no_entries);
            content.addView(empty);
            return;
        }
        LocalDate today = LocalDate.now();
        for (Entry item : entries) {
            View row = getLayoutInflater().inflate(R.layout.mothermentor_test_result_row, content, false);
            String type = testLabel(this, item.getSample());
            String summary = field(R.string.mama_results_date, dateLabel(item.getSample().getTestDate())) + "\n"
                    + field(R.string.mama_results_type, type);
            TextView note = row.findViewById(R.id.mama_results_note);
            if (item.isRecorded()) {
                summary += "\n" + field(R.string.mama_results_result, answerLabel(this, item.getAnswers().getResult()));
                note.setText(R.string.mama_results_edit_today);
                note.setVisibility(View.VISIBLE);
            } else if (item.getSample().isOverdue(today)) {
                note.setText(R.string.mama_results_overdue);
                note.setTextColor(getResources().getColor(R.color.alert_urgent_red));
                note.setVisibility(View.VISIBLE);
            }
            ((TextView) row.findViewById(R.id.mama_results_summary)).setText(summary);
            Button action = row.findViewById(R.id.mama_results_action);
            action.setText(item.isRecorded() ? R.string.mama_results_edit : R.string.mama_results_record);
            action.setContentDescription(action.getText() + " · " + type);
            action.setOnClickListener(view -> startActivity(formIntent(this, motherId, item.getId())));
            content.addView(row);
        }
    }

    private void renderForm(Entry editable) {
        if (isFinishing() || isDestroyed()) return;
        if (editable == null || (restoredForm != null && restoredForm.containsKey(REVISION)
                && editable.getRevision() != restoredForm.getLong(REVISION))) {
            unavailable();
            return;
        }
        if (entry != null) return;
        entry = editable;
        findViewById(R.id.mama_results_loading).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.mama_results_heading)).setText(
                entry.isRecorded() ? R.string.mama_results_edit : R.string.mama_results_record);
        getLayoutInflater().inflate(R.layout.mothermentor_test_result_form, content, true);
        ((TextView) findViewById(R.id.mama_results_question)).setText(
                getString(R.string.mama_results_question, testLabel(this, entry.getSample())));
        result = findViewById(R.id.mama_results_answers);
        confirmationDone = findViewById(R.id.mama_results_confirmation_done);
        confirmation = findViewById(R.id.mama_results_confirmation_answers);
        dateButton = findViewById(R.id.mama_results_date_picker);
        dateButton.setOnClickListener(view -> chooseDate());
        findViewById(R.id.mama_results_save_bottom).setOnClickListener(view -> submit());
        if (restoredForm != null && restoredForm.containsKey(REVISION)) {
            result.check(restoredForm.getInt(RESULT, -1));
            receivedDate = MotherMentorHeiSchedule.parseDate(restoredForm.getString(DATE));
            confirmationDone.check(restoredForm.getInt(CONFIRMATION_DONE, -1));
            confirmation.check(restoredForm.getInt(CONFIRMATION, -1));
        } else if (entry.isRecorded()) {
            Answers answers = entry.getAnswers();
            result.check("positive".equals(answers.getResult())
                    ? R.id.mama_results_answer_positive : R.id.mama_results_answer_negative);
            receivedDate = answers.getReceivedDate();
            if ("yes".equals(answers.getConfirmationDone())) confirmationDone.check(R.id.mama_results_confirmation_yes);
            if ("no".equals(answers.getConfirmationDone())) confirmationDone.check(R.id.mama_results_confirmation_no);
            if ("positive".equals(answers.getConfirmationResult())) confirmation.check(R.id.mama_results_confirmation_positive);
            if ("negative".equals(answers.getConfirmationResult())) confirmation.check(R.id.mama_results_confirmation_negative);
            if ("pending".equals(answers.getConfirmationResult())) confirmation.check(R.id.mama_results_confirmation_pending);
        }
        restoredForm = null;
        result.setOnCheckedChangeListener((group, checked) -> updateConfirmation());
        confirmationDone.setOnCheckedChangeListener((group, checked) -> updateConfirmation());
        updateConfirmation();
        updateDate();
        setSaving(false);
    }

    private void updateConfirmation() {
        boolean show = !entry.getSample().isAwaitingConfirmation()
                && result.getCheckedRadioButtonId() == R.id.mama_results_answer_positive;
        if (!show && confirmationDone.getCheckedRadioButtonId() != -1) confirmationDone.clearCheck();
        boolean showResult = show && confirmationDone.getCheckedRadioButtonId() == R.id.mama_results_confirmation_yes;
        if (!showResult && confirmation.getCheckedRadioButtonId() != -1) confirmation.clearCheck();
        findViewById(R.id.mama_results_confirmation_section).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.mama_results_confirmation_answers_section).setVisibility(showResult ? View.VISIBLE : View.GONE);
    }

    private void chooseDate() {
        LocalDate today = LocalDate.now();
        LocalDate selected = receivedDate == null ? today : receivedDate;
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            receivedDate = new LocalDate(year, month + 1, day);
            updateDate();
        }, selected.getYear(), selected.getMonthOfYear() - 1, selected.getDayOfMonth());
        picker.getDatePicker().setMinDate(entry.getSample().getTestDate().toDate().getTime());
        picker.getDatePicker().setMaxDate(today.toDate().getTime());
        picker.show();
    }

    private void updateDate() {
        if (receivedDate == null) dateButton.setText(R.string.mama_results_select_date);
        else dateButton.setText(dateLabel(receivedDate));
    }

    private void submit() {
        if (entry == null || saving) return;
        LocalDate today = LocalDate.now();
        if (!MotherMentorHeiSchedule.validDate(receivedDate, entry.getSample().getTestDate(), today)) {
            toast(R.string.mama_results_select_date_error);
            return;
        }
        int done = confirmationDone.getCheckedRadioButtonId();
        int confirmed = confirmation.getCheckedRadioButtonId();
        Answers answers = new Answers(
                testAnswer(result.getCheckedRadioButtonId(), R.id.mama_results_answer_positive, R.id.mama_results_answer_negative),
                receivedDate, done == R.id.mama_results_confirmation_yes ? "yes"
                        : done == R.id.mama_results_confirmation_no ? "no" : "",
                confirmed == R.id.mama_results_confirmation_pending ? "pending"
                        : testAnswer(confirmed, R.id.mama_results_confirmation_positive, R.id.mama_results_confirmation_negative));
        if (!answers.isValid(entry.getSample(), today)) {
            toast(R.string.mama_results_select_answers);
            return;
        }
        setSaving(true);
        if (answers.closesFollowUp(entry.getSample(), today)) {
            MotherMentorMotherClosureDialog.show(this, MotherMentorMotherClosure.active()
                    .advance(MotherMentorMotherClosure.VisitOutcome.CHILD_CONFIRMED_POSITIVE, today.toString()),
                    () -> save(answers), () -> setSaving(false));
        } else {
            save(answers);
        }
    }

    private void save(Answers answers) {
        executors.diskIO().execute(() -> {
            try {
                boolean saved = MotherMentorMotherResultEvent.save(database(), motherId, sampleId,
                        entry.getRevision(), answers);
                executors.mainThread().execute(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (saved) {
                        setResult(RESULT_OK);
                        toast(R.string.mama_results_saved);
                        finish();
                    } else {
                        unavailable();
                    }
                });
            } catch (Exception error) {
                Timber.e(error, "Unable to save the Mama Kadi result");
                executors.mainThread().execute(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    toast(R.string.mama_results_save_error);
                    setSaving(false);
                });
            }
        });
    }

    private void setSaving(boolean saving) {
        this.saving = saving;
        findViewById(R.id.mama_results_save_top).setEnabled(!saving);
        findViewById(R.id.mama_results_save_bottom).setEnabled(!saving);
    }

    private void failedToLoad(RuntimeException error) {
        Timber.e(error, "Unable to load Mama Kadi test results");
        executors.mainThread().execute(() -> {
            if (!isFinishing() && !isDestroyed()) unavailable();
        });
    }

    private void unavailable() {
        toast(R.string.mama_results_unavailable);
        finish();
    }

    private void toast(int text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private static String testAnswer(int selected, int positive, int negative) {
        if (selected == positive) return "positive";
        if (selected == negative) return "negative";
        return "";
    }

    static String testLabel(Context context, PendingTest sample) {
        String stage = stageLabel(context, sample.getStage());
        return sample.isAwaitingConfirmation() ? context.getString(R.string.mama_results_confirmation_type, stage) : stage;
    }

    static String stageLabel(Context context, MotherMentorHeiSchedule.Stage stage) {
        switch (stage) {
            case AT_BIRTH: return context.getString(R.string.mama_results_birth);
            case SIX_WEEKS: return context.getString(R.string.mama_results_week_six);
            case NINE_MONTHS: return context.getString(R.string.mama_results_month_nine);
            case AFTER_WEANING: return context.getString(R.string.mama_results_month_fifteen);
            case EIGHTEEN_MONTHS: return context.getString(R.string.mama_results_month_eighteen);
            default: throw new IllegalArgumentException("Unknown test type");
        }
    }

    static String answerLabel(Context context, String answer) {
        if ("positive".equals(answer)) return context.getString(R.string.mama_results_positive);
        if ("negative".equals(answer)) return context.getString(R.string.mama_results_negative);
        return context.getString(R.string.mama_results_pending);
    }

    static String dateLabel(LocalDate date) {
        return date.toString("dd-MM-yyyy");
    }

    private String field(int label, String value) {
        return getString(R.string.mama_results_field_value, getString(label), value);
    }
}
