package org.smartregister.chw.activity;

import android.view.View;
import android.widget.TextView;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.LocalDate;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherClosureRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.Entry;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherClosure;
import org.smartregister.util.AppExecutors;

import java.util.List;

import timber.log.Timber;

/** Binds the Mama Kadi profile row and the same-day closure notice. */
final class MotherMentorMotherResultUi {
    private final MotherMentorProfileActivity profile;
    private final AppExecutors executors = new AppExecutors();
    private int loadVersion;

    MotherMentorMotherResultUi(MotherMentorProfileActivity profile) {
        this.profile = profile;
    }

    void refresh() {
        View row = profile.findViewById(org.smartregister.chw.mothermentor.R.id.mothermentor_mother_test_results);
        if (row == null) return;
        int request = ++loadVersion;
        String motherId = profile.motherResultId();
        if (motherId.isEmpty()) {
            row.setVisibility(View.GONE);
            profile.showMotherClosureEditNotice(0);
            return;
        }
        LocalDate today = new LocalDate(profile.getVisitAvailabilityDate());
        executors.diskIO().execute(() -> {
            try {
                SQLiteDatabase db = ChwApplication.getInstance().getRepository().getReadableDatabase();
                List<Entry> entries = MotherMentorMotherResultEvent.visible(db, motherId, today);
                int notice = closureNotice(db, motherId, entries, today);
                executors.mainThread().execute(() -> {
                    if (request != loadVersion || profile.isFinishing() || profile.isDestroyed()
                            || !motherId.equals(profile.motherResultId())) return;
                    bind(row, motherId, entries, today);
                    profile.showMotherClosureEditNotice(notice);
                });
            } catch (RuntimeException error) {
                Timber.e(error, "Unable to load Mama Kadi test results");
            }
        });
    }

    private int closureNotice(SQLiteDatabase db, String motherId, List<Entry> entries, LocalDate today) {
        MotherMentorMotherClosure closure = MotherMentorMotherClosureRepository.getState(db, motherId, null);
        if (!today.toString().equals(closure.getClosedDate())) return 0;
        if (entries.stream().anyMatch(entry -> entry.isRecorded()
                && entry.getAnswers().closesFollowUp(entry.getSample(), today))) {
            return R.string.mama_results_closed_result_today;
        }
        return MotherMentorMotherClosureRepository.isSameDayClosingVisit(db, profile.getServiceVisit(), today)
                ? R.string.mama_results_closed_visit_today : 0;
    }

    private void bind(View row, String motherId, List<Entry> entries, LocalDate day) {
        int count = entries.size();
        row.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        if (count == 0) return;
        TextView title = row.findViewById(org.smartregister.chw.mothermentor.R.id.mothermentor_mother_test_results_title);
        TextView subtitle = row.findViewById(org.smartregister.chw.mothermentor.R.id.mothermentor_mother_test_results_subtitle);
        title.setText(profile.getResources().getQuantityText(R.plurals.mama_results_title, count));
        StringBuilder label = new StringBuilder(profile.getResources()
                .getQuantityText(R.plurals.mama_results_subtitle, count));
        for (Entry entry : entries) {
            if (!entry.isRecorded() && entry.getSample().isOverdue(day)) {
                label.append("\n").append(MotherMentorTestResultsActivity.testLabel(profile, entry.getSample()))
                        .append(" · ").append(profile.getString(R.string.mama_results_overdue));
            }
        }
        subtitle.setText(label);
        row.setOnClickListener(view -> MotherMentorTestResultsActivity.startMe(profile, motherId));
    }
}
