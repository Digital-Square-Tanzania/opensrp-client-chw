package org.smartregister.chw.activity;

import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.MTOTO_RESULT_RECEIVED_DATE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.LocalDate;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherClosureRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.Answers;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.Entry;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.PendingTest;
import org.smartregister.chw.mothermentor.util.AppExecutors;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorChildEidResultForm;
import org.smartregister.chw.mothermentor.util.MotherMentorHeiSchedule;
import org.smartregister.chw.mothermentor.util.MotherMentorChildEidClosureDialog;
import org.smartregister.chw.mothermentor.util.MotherMentorChildEidExit;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherClosure;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherChildUtils;
import org.smartregister.chw.util.MotherMentorFormWizardUtils;
import org.smartregister.chw.util.MotherMentorProfileResultForm;
import org.smartregister.chw.util.MotherMentorMotherClosureDialog;
import org.smartregister.view.activity.SecuredActivity;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class MotherMentorTestResultsActivity extends SecuredActivity {
    static final int EDIT_FROM_HISTORY = 1207;
    private static final String MOTHER_ID = "mother_id";
    private static final String SAMPLE_ID = "sample_id";
    private static final int REQUEST_NATIVE_RESULT = 1209;
    private static final String REVISION = "revision";
    private static final String SUBMITTED_FORM = "submitted_form";

    private final AppExecutors executors = new AppExecutors();
    private String motherId;
    private String sampleId;
    private LinearLayout content;
    private Long expectedRevision;
    private String submittedForm;
    private boolean loading;
    private boolean reviewing;

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
        if (savedInstanceState != null && savedInstanceState.containsKey(REVISION)) {
            expectedRevision = savedInstanceState.getLong(REVISION);
            submittedForm = savedInstanceState.getString(SUBMITTED_FORM);
        }
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
        } else if (submittedForm != null) {
            reviewResult();
        } else if (expectedRevision == null && !loading) {
            loading = true;
            executors.diskIO().execute(() -> {
                try {
                    Entry entry = MotherMentorMotherResultEvent.editable(database(), motherId, sampleId, LocalDate.now());
                    executors.mainThread().execute(() -> openNativeForm(entry));
                } catch (RuntimeException error) {
                    failedToLoad(error);
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (expectedRevision == null) return;
        state.putLong(REVISION, expectedRevision);
        state.putString(SUBMITTED_FORM, submittedForm);
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

    private void openNativeForm(Entry entry) {
        if (isFinishing() || isDestroyed()) return;
        if (entry == null) {
            unavailable();
            return;
        }
        try {
            expectedRevision = entry.getRevision();
            PendingTest sample = entry.getSample();
            JSONObject form = FormUtils.getFormUtils().getFormJson(MotherMentorChildEidResultForm.FORM_NAME);
            MotherMentorProfileResultForm.prepare(form, this, motherId, sample.getStage(),
                    sample.getTestDate(), sample.getState(), entry.getAnswers());
            ((TextView) findViewById(R.id.mama_results_heading)).setText(
                    entry.isRecorded() ? R.string.mama_results_edit : R.string.mama_results_record);
            launchNativeForm(form);
        } catch (Exception error) {
            failedToLoad(error);
        }
    }

    private void launchNativeForm(JSONObject form) {
        if (isFinishing() || isDestroyed()) return;
        submittedForm = null;
        reviewing = false;
        startActivityForResult(MotherMentorFormWizardUtils.getFormIntent(this, form), REQUEST_NATIVE_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_NATIVE_RESULT || isFinishing()) return;
        if (resultCode != RESULT_OK || data == null) {
            finish();
            return;
        }
        submittedForm = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
        if (submittedForm == null) {
            unavailable();
            return;
        }
        reviewResult();
    }

    private void reviewResult() {
        if (reviewing || expectedRevision == null) return;
        reviewing = true;
        String payload = submittedForm;
        long revision = expectedRevision;
        executors.diskIO().execute(() -> {
            try {
                LocalDate day = LocalDate.now();
                Entry entry = MotherMentorMotherResultEvent.editable(database(), motherId, sampleId, day);
                JSONObject form = new JSONObject(payload);
                Map<String, String> values = MotherMentorMotherChildUtils.values(form);
                if (entry == null || entry.getRevision() != revision
                        || !MotherMentorProfileResultForm.matches(form, values, motherId, entry.getSample())) {
                    executors.mainThread().execute(() -> {
                        if (!isFinishing() && !isDestroyed()) unavailable();
                    });
                    return;
                }
                boolean motherContinues = MotherMentorMotherClosureRepository.hasOpenSibling(database(), motherId, null);
                executors.mainThread().execute(() -> reviewAnswers(entry, form, values, day, motherContinues));
            } catch (Exception error) {
                failedToLoad(error);
            }
        });
    }

    private void reviewAnswers(Entry entry, JSONObject form, Map<String, String> values, LocalDate day,
            boolean motherContinues) {
        if (isFinishing() || isDestroyed()) return;
        PendingTest sample = entry.getSample();
        int dateError = MotherMentorChildEidResultForm.receiptDateError(values, sample.getTestDate());
        if (dateError != 0) {
            MotherMentorMotherChildUtils.put(form, MTOTO_RESULT_RECEIVED_DATE, "");
            retryForm(form, dateError);
            return;
        }
        Answers answers = MotherMentorProfileResultForm.readAnswers(values, sample.isAwaitingConfirmation());
        if (!answers.isValid(sample, day)) {
            retryForm(form, R.string.mama_results_select_answers);
            return;
        }
        Runnable save = () -> save(entry, answers, form);
        if (answers.closesFollowUp(sample, day) && motherContinues) {
            MotherMentorChildEidClosureDialog.show(this, MotherMentorChildEidExit.E4, false, true,
                    save, () -> launchNativeForm(form));
        } else if (answers.closesFollowUp(sample, day)) {
            MotherMentorMotherClosureDialog.show(this, MotherMentorMotherClosure.active().advance(
                    MotherMentorMotherClosure.VisitOutcome.CHILD_CONFIRMED_POSITIVE, day.toString()),
                    save, () -> launchNativeForm(form));
        } else {
            save.run();
        }
    }

    private void save(Entry entry, Answers answers, JSONObject form) {
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
                executors.mainThread().execute(() -> retryForm(form, R.string.mama_results_save_error));
            }
        });
    }

    private void retryForm(JSONObject form, int message) {
        if (isFinishing() || isDestroyed()) return;
        toast(message);
        launchNativeForm(form);
    }

    private void failedToLoad(Exception error) {
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
