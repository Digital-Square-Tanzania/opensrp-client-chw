package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import org.smartregister.chw.R;
import org.smartregister.chw.ayp.AypLibrary;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.domain.Visit;
import org.smartregister.chw.ayp.domain.VisitDetail;
import org.smartregister.chw.ayp.repository.VisitDetailsRepository;
import org.smartregister.chw.ayp.repository.VisitRepository;
import org.smartregister.chw.ayp.util.AppExecutors;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

public class AypInSchoolMedicalHistoryActivity extends AppCompatActivity {

    private static MemberObject memberProfile;

    private final AppExecutors appExecutors = new AppExecutors();
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private ProgressBar progressBar;
    private LinearLayout historyContainer;
    private TextView emptyStateView;

    public static void startMe(Activity activity, MemberObject memberObject) {
        memberProfile = memberObject;
        Intent intent = new Intent(activity, AypInSchoolMedicalHistoryActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.smartregister.chw.opensrp_chw_anc.R.layout.activity_base_anc_medical_history);
        if (memberProfile == null) {
            Timber.w("AypInSchoolMedicalHistoryActivity launched without a member profile");
            finish();
            return;
        }
        setupToolbar();
        initialiseViews();
        loadHistory();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.collapsing_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView title = toolbar.findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle);
        String displayName = !TextUtils.isEmpty(memberProfile.getFullName())
                ? memberProfile.getFullName()
                : getString(R.string.ayp_client);
        title.setText(getString(org.smartregister.chw.opensrp_chw_anc.R.string.back_to, displayName));
    }

    private void initialiseViews() {
        TextView header = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.medical_history);
        header.setText(R.string.ayp_visit_history);
        progressBar = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.progressBarMedicalHistory);
        historyContainer = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.linearLayoutMedicalHistory);
        emptyStateView = buildEmptyStateView();
    }

    private TextView buildEmptyStateView() {
        CustomFontTextView textView = new CustomFontTextView(this);
        textView.setText(R.string.ayp_visit_history_empty);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        int verticalPadding = dpToPx(24);
        int horizontalPadding = dpToPx(20);
        textView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        textView.setTextColor(ContextCompat.getColor(this, R.color.medical_sub_text_inner));
        return textView;
    }

    private void loadHistory() {
        progressBar.setVisibility(View.VISIBLE);
        historyContainer.removeAllViews();
        appExecutors.diskIO().execute(() -> {
            List<VisitDisplay> items = fetchVisitHistory();
            appExecutors.mainThread().execute(() -> {
                progressBar.setVisibility(View.GONE);
                renderHistory(items);
            });
        });
    }

    private List<VisitDisplay> fetchVisitHistory() {
        List<VisitDisplay> results = new ArrayList<>();
        try {
            AypLibrary library = AypLibrary.getInstance();
            if (library == null) {
                Timber.w("AypLibrary instance not initialised when loading medical history");
                return results;
            }
            VisitRepository visitRepository = library.visitRepository();
            VisitDetailsRepository detailsRepository = library.visitDetailsRepository();

            List<Visit> combined = new ArrayList<>();
            List<Visit> serviceVisits = visitRepository.getVisits(memberProfile.getBaseEntityId(), Constants.EVENT_TYPE.AYP_SERVICES);
            if (serviceVisits != null) {
                combined.addAll(serviceVisits);
            }
            List<Visit> followUpVisits = visitRepository.getVisits(memberProfile.getBaseEntityId(), Constants.EVENT_TYPE.AYP_IN_SCHOOL_FOLLOW_UP_VISIT);
            if (followUpVisits != null) {
                combined.addAll(followUpVisits);
            }
            Collections.sort(combined, (first, second) -> compareVisitsByDate(second, first));

            for (Visit visit : combined) {
                List<VisitDetail> details = detailsRepository.getVisits(visit.getVisitId());
                results.add(new VisitDisplay(visit, extractDetailLines(details)));
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return results;
    }

    private int compareVisitsByDate(Visit newer, Visit older) {
        Date newerDate = newer != null ? newer.getDate() : null;
        Date olderDate = older != null ? older.getDate() : null;
        if (newerDate == null && newer != null) {
            newerDate = newer.getUpdatedAt();
        }
        if (olderDate == null && older != null) {
            olderDate = older.getUpdatedAt();
        }

        if (newerDate == null && olderDate == null) {
            return 0;
        }
        if (newerDate == null) {
            return -1;
        }
        if (olderDate == null) {
            return 1;
        }
        return newerDate.compareTo(olderDate);
    }

    private List<String> extractDetailLines(List<VisitDetail> details) {
        Set<String> lines = new LinkedHashSet<>();
        if (details != null) {
            for (VisitDetail detail : details) {
                String value = detail.getHumanReadable();
                if (TextUtils.isEmpty(value)) {
                    value = detail.getDetails();
                }
                if (!TextUtils.isEmpty(value)) {
                    lines.add(value.trim());
                }
            }
        }
        return new ArrayList<>(lines);
    }

    private void renderHistory(List<VisitDisplay> items) {
        historyContainer.removeAllViews();
        if (items.isEmpty()) {
            historyContainer.addView(emptyStateView);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (VisitDisplay item : items) {
            View visitView = inflater.inflate(R.layout.medical_history_visit, historyContainer, false);

            TextView titleView = visitView.findViewById(R.id.title);
            titleView.setText(formatVisitDate(item.visit));

            TextView typeView = visitView.findViewById(R.id.type_of_service);
            typeView.setText(item.visit.getVisitType());

            LinearLayout detailsLayout = visitView.findViewById(R.id.visit_details_layout);
            bindDetails(detailsLayout, item.detailLines);

            historyContainer.addView(visitView);
        }
    }

    private void bindDetails(LinearLayout container, List<String> lines) {
        container.removeAllViews();
        if (lines.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }
        container.setVisibility(View.VISIBLE);
        for (String line : lines) {
            CustomFontTextView detailView = new CustomFontTextView(this);
            detailView.setText("\u2022 " + line);
            detailView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            detailView.setTextColor(ContextCompat.getColor(this, R.color.medical_sub_text_inner));
            detailView.setPadding(dpToPx(24), dpToPx(4), dpToPx(20), dpToPx(4));
            container.addView(detailView);
        }
    }

    private String formatVisitDate(Visit visit) {
        Date date = visit != null ? visit.getDate() : null;
        if (date == null) {
            date = visit != null ? visit.getUpdatedAt() : null;
        }
        return date != null ? dateFormatter.format(date) : getString(R.string.ayp_visit_history_unknown_date);
    }

    private int dpToPx(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        memberProfile = null;
    }

    private static class VisitDisplay {
        private final Visit visit;
        private final List<String> detailLines;

        VisitDisplay(Visit visit, List<String> detailLines) {
            this.visit = visit;
            this.detailLines = detailLines;
        }
    }
}
