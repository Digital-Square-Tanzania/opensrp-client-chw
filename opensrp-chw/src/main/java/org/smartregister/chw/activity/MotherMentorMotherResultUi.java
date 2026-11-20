package org.smartregister.chw.activity;

import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.MTOTO_RESULT_RECEIVED_DATE;
import static org.smartregister.util.JsonFormUtils.ENTITY_ID;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.joda.time.LocalDate;
import org.json.JSONObject;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.mothermentor.R;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.PendingTest;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorChildEidLabels;
import org.smartregister.chw.mothermentor.util.MotherMentorChildEidResultForm;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherChildUtils;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherClosure;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherPregnancyUtils;
import org.smartregister.chw.util.MotherMentorFormWizardUtils;
import org.smartregister.chw.util.MotherMentorMotherClosureDialog;
import org.smartregister.util.AppExecutors;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

/** Profile entry point for returned Mama Kadi results. */
final class MotherMentorMotherResultUi {
    private static final int REQUEST_RESULT = 7813;
    private static final String TEST_KEY = "mother_hei_test_key";

    private final MotherMentorProfileActivity profile;
    private final AppExecutors executors = new AppExecutors();
    private int loadVersion;

    MotherMentorMotherResultUi(MotherMentorProfileActivity profile) {
        this.profile = profile;
    }

    void refresh() {
        View action = profile.findViewById(R.id.mothermentor_mother_test_results);
        if (action == null) return;
        int request = ++loadVersion;
        String motherId = profile.activeMotherResultId();
        if (motherId.isEmpty()) {
            action.setVisibility(View.GONE);
            return;
        }
        LocalDate day = new LocalDate(profile.getVisitAvailabilityDate());
        executors.diskIO().execute(() -> {
            try {
                List<PendingTest> tests = MotherMentorMotherResultEvent.pending(
                        ChwApplication.getInstance().getRepository().getReadableDatabase(), motherId, day);
                executors.mainThread().execute(() -> {
                    if (request == loadVersion && isActive(motherId)) bind(action, motherId, tests, day);
                });
            } catch (Exception error) {
                Timber.e(error, "Unable to load Mama Kadi test results");
            }
        });
    }

    private void bind(View action, String motherId, List<PendingTest> tests, LocalDate day) {
        action.setVisibility(tests.isEmpty() ? View.GONE : View.VISIBLE);
        if (tests.isEmpty()) return;
        TextView title = action.findViewById(R.id.mothermentor_mother_test_results_title);
        TextView subtitle = action.findViewById(R.id.mothermentor_mother_test_results_subtitle);
        title.setText(title(tests.size()));
        int description = tests.size() == 1
                ? R.string.mothermentor_mother_result_subtitle : R.string.mothermentor_mother_results_subtitle;
        String labels = profile.getString(description);
        for (PendingTest test : tests) {
            if (test.isOverdue(day)) labels += "\n" + testLabel(test)
                    + " · " + profile.getString(R.string.mothermentor_eid_result_overdue);
        }
        subtitle.setText(labels);
        action.setOnClickListener(view -> showTests(motherId, tests, day));
    }

    private int title(int count) {
        return count == 1 ? R.string.mothermentor_mother_result_title : R.string.mothermentor_mother_results_title;
    }

    private String testLabel(PendingTest test) {
        return MotherMentorChildEidLabels.test(profile, test.getStage().getCode());
    }

    private void showTests(String motherId, List<PendingTest> tests, LocalDate day) {
        CharSequence[] items = new CharSequence[tests.size()];
        for (int index = 0; index < tests.size(); index++) {
            PendingTest test = tests.get(index);
            int status = test.isOverdue(day) ? R.string.mothermentor_eid_result_overdue
                    : test.isAwaitingConfirmation() ? R.string.mothermentor_eid_awaiting_confirmation
                    : R.string.mothermentor_mother_result_record;
            items[index] = profile.getString(R.string.mothermentor_mother_result_list_item, testLabel(test),
                    MotherMentorMotherPregnancyUtils.displayDate(test.getTestDate()), profile.getString(status));
        }
        new AlertDialog.Builder(profile)
                .setTitle(title(tests.size()))
                .setItems(items, (dialog, index) -> openForm(motherId, tests.get(index)))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openForm(String motherId, PendingTest test) {
        if (!isActive(motherId)) return;
        try {
            JSONObject form = FormUtils.getFormUtils().getFormJson(MotherMentorChildEidResultForm.FORM_NAME);
            form.put(ENTITY_ID, motherId);
            form.put(TEST_KEY, test.getKey());
            form.put("encounter_type", MotherMentorMotherResultEvent.EVENT_TYPE);
            MotherMentorChildEidResultForm.prepare(form, profile, test.getStage().getCode(),
                    test.getTestDate(), test.getState());
            launch(form);
        } catch (Exception error) {
            failure(error);
        }
    }

    private void launch(JSONObject form) {
        profile.startActivityForResult(MotherMentorFormWizardUtils.getFormIntent(profile, form), REQUEST_RESULT);
    }

    boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_RESULT) return false;
        if (resultCode == Activity.RESULT_OK && data != null) {
            executors.diskIO().execute(() -> review(data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON)));
        }
        return true;
    }

    private void review(String payload) {
        try {
            JSONObject form = new JSONObject(payload);
            String motherId = form.getString(ENTITY_ID);
            LocalDate day = new LocalDate(profile.getVisitAvailabilityDate());
            PendingTest test = MotherMentorMotherResultEvent.pending(
                    ChwApplication.getInstance().getRepository().getReadableDatabase(), motherId,
                    form.getString(TEST_KEY), day);
            Map<String, String> answers = MotherMentorMotherChildUtils.values(form);
            executors.mainThread().execute(() -> review(form, motherId, test, answers, day));
        } catch (Exception error) {
            failure(error);
        }
    }

    private void review(JSONObject form, String motherId, PendingTest test, Map<String, String> answers, LocalDate day) {
        if (!isActive(motherId)) return;
        if (test == null) {
            message(R.string.mothermentor_mother_result_unavailable);
            refresh();
            return;
        }
        if (!MotherMentorMotherResultEvent.isComplete(test, answers, day)) {
            int dateError = MotherMentorChildEidResultForm.receiptDateError(answers, test.getTestDate());
            if (dateError != 0) MotherMentorMotherChildUtils.put(form, MTOTO_RESULT_RECEIVED_DATE, "");
            message(dateError == 0 ? R.string.mothermentor_eid_result_required : dateError);
            launch(form);
            return;
        }
        Runnable save = () -> save(motherId, test, answers, day);
        if (MotherMentorMotherResultEvent.closesCard(test, answers, day)) {
            MotherMentorMotherClosure closure = MotherMentorMotherClosure.active().advance(
                    MotherMentorMotherClosure.VisitOutcome.CHILD_CONFIRMED_POSITIVE, day.toString());
            MotherMentorMotherClosureDialog.show(profile, closure, save, () -> launch(form));
        } else {
            save.run();
        }
    }

    private void save(String motherId, PendingTest test, Map<String, String> answers, LocalDate day) {
        executors.diskIO().execute(() -> {
            try {
                boolean saved = MotherMentorMotherResultEvent.save(
                        ChwApplication.getInstance().getRepository().getWritableDatabase(), motherId, test, answers, day);
                executors.mainThread().execute(() -> {
                    if (profile.isFinishing() || profile.isDestroyed()) return;
                    message(saved ? R.string.mothermentor_mother_result_saved : R.string.mothermentor_mother_result_unavailable);
                    profile.setupButtons();
                });
            } catch (Exception error) {
                failure(error);
            }
        });
    }

    private boolean isActive(String motherId) {
        return !profile.isFinishing() && !profile.isDestroyed() && motherId.equals(profile.activeMotherResultId());
    }

    private void failure(Exception error) {
        Timber.e(error, "Unable to record the Mama Kadi test result");
        executors.mainThread().execute(() -> {
            if (!profile.isFinishing() && !profile.isDestroyed()) message(R.string.mothermentor_eid_result_save_failed);
        });
    }

    private void message(int resource) {
        Toast.makeText(profile, resource, Toast.LENGTH_SHORT).show();
    }
}
