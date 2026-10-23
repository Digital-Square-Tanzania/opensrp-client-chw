package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.core.activity.DefaultAncMedicalHistoryActivityFlv;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.util.MotherMentorHistoryMapper;
import org.smartregister.chw.util.MotherMentorHistoryMapper.Row;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MotherMentorMedicalHistoryActivityFlv extends DefaultAncMedicalHistoryActivityFlv {
    private final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
    private final MotherMentorFollowUpType followUpType;

    public MotherMentorMedicalHistoryActivityFlv() {
        this(null);
    }

    public MotherMentorMedicalHistoryActivityFlv(MotherMentorFollowUpType followUpType) {
        this.followUpType = followUpType;
    }

    @Override
    protected void processAncCard(String hasCard, Context context) {
        linearLayoutAncCard.setVisibility(View.GONE);
    }

    @Override
    protected void processHealthFacilityVisit(
            List<Map<String, String>> hfVisits, Context context) {
        // Mother Mentor history is rendered from the visit action details below.
    }

    @Override
    public void processViewData(List<Visit> visits, Context context) {
        if (visits == null || visits.isEmpty()) return;

        int days = Days.daysBetween(
                new DateTime(visits.get(visits.size() - 1).getDate()), new DateTime()).getDays();
        MotherMentorHistoryMapper mapper = new MotherMentorHistoryMapper(context);
        List<List<Row>> visitDetails = new ArrayList<>();
        for (int i = 0; i < visits.size(); i++) {
            Visit visit = visits.get(i);
            List<Row> rows = new ArrayList<>(mapper.map(visit));
            rows.add(new Row(DBConstants.KEY.VISIT_NUMBER,
                    context.getString(R.string.mothermentor_history_visit_number),
                    Collections.singletonList(String.valueOf(i + 1)), false));
            rows.add(new Row("visit_completion_state",
                    context.getString(R.string.mothermentor_history_visit_completion_state),
                    Collections.singletonList(context.getString(Boolean.TRUE.equals(visit.getProcessed())
                            ? R.string.mothermentor_history_completed
                            : R.string.mothermentor_history_saved)), false));
            visitDetails.add(rows);
        }
        processLastVisit(days, context);
        processVisit(visitDetails, context, visits);
    }

    private void processLastVisit(int days, Context context) {
        linearLayoutLastVisit.setVisibility(View.VISIBLE);
        if (days < 1) {
            customFontTextViewLastVisit.setText(
                    org.smartregister.chw.core.R.string.less_than_twenty_four);
        } else {
            customFontTextViewLastVisit.setText(StringUtils.capitalize(MessageFormat.format(
                    context.getString(org.smartregister.chw.core.R.string.days_ago),
                    String.valueOf(days))));
        }
    }

    protected void processVisit(List<List<Row>> history, Context context, List<Visit> visits) {
        if (history == null || history.isEmpty()) return;
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);
        for (int i = 0; i < history.size(); i++) {
            List<Row> rows = history.get(i);
            Visit visit = visits.get(i);
            View view = inflater.inflate(R.layout.medical_history_visit, null);
            view.findViewById(R.id.title).setVisibility(View.GONE);
            TextView service = view.findViewById(R.id.type_of_service);
            LinearLayout details = view.findViewById(R.id.visit_details_layout);
            TextView edit = view.findViewById(R.id.textview_edit);
            boolean legacyFollowUp = followUpType != null
                    && Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES.equals(visit.getVisitType());
            edit.setVisibility(
                    i == visits.size() - 1 && !legacyFollowUp ? View.VISIBLE : View.GONE);
            edit.setOnClickListener(ignored -> editVisit((Activity) context, visit));
            service.setText(String.format("%s - %s", getVisitType(context, visit.getVisitType()),
                    dateFormat.format(visit.getDate())));
            for (Row row : rows) {
                if (row.getValues().isEmpty()) continue;
                TextView text = new TextView(context);
                text.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                int padding = (int) (10 * context.getResources().getDisplayMetrics().density + 0.5f);
                text.setPadding(padding, 0, 0, 0);
                text.setTag(row.getKey());
                SpannableStringBuilder content = new SpannableStringBuilder();
                content.append(row.getLabel(), boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        .append("\n");
                for (String value : row.getValues()) {
                    if (row.isMultiValue()) {
                        content.append(value + "\n", new BulletSpan(10),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        content.append(value).append("\n");
                    }
                }
                text.setText(content);
                details.addView(text);
            }
            linearLayoutHealthFacilityVisitDetails.addView(view, 0);
        }
    }

    private void editVisit(Activity activity, Visit visit) {
        if (StringUtils.isBlank(visit.getBaseEntityId())) return;
        activity.finish();
        if (MotherMentorHistoryMapper.isHouseholdVisit(visit)) {
            MotherMentorHouseholdVisitActivity.startMotherMentorHouseholdVisitActivity(
                    activity, visit.getBaseEntityId(), true);
        } else if (isFollowUpVisit(visit.getVisitType())) {
            MotherMentorFollowUpType type = MotherMentorFollowUpType.MOTHER_IIT;
            for (MotherMentorFollowUpType candidate : MotherMentorFollowUpType.values()) {
                if (candidate.getEventType().equals(visit.getVisitType())) type = candidate;
            }
            MotherMentorVisitActivity.startMotherMentorFollowUpVisitActivity(
                    activity, visit.getBaseEntityId(), true, type);
        } else {
            MotherMentorVisitActivity.startMotherMentorVisitActivity(
                    activity, visit.getBaseEntityId(), true);
        }
    }

    private boolean isFollowUpVisit(String eventType) {
        for (MotherMentorFollowUpType type : MotherMentorFollowUpType.values()) {
            if (type.getEventType().equals(eventType)) return true;
        }
        return false;
    }

    private String getVisitType(Context context, String visitType) {
        if (Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES.equals(visitType)
                || Constants.EVENT_TYPE.MOTHERMENTOR_CONTACT_VISIT.equals(visitType)
                || isFollowUpVisit(visitType)) {
            return context.getString(R.string.mothermentor_visit_history_title);
        }
        return visitType;
    }
}
