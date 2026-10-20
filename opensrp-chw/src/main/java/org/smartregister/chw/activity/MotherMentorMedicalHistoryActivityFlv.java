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
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.core.activity.DefaultAncMedicalHistoryActivityFlv;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;

import timber.log.Timber;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MotherMentorMedicalHistoryActivityFlv extends DefaultAncMedicalHistoryActivityFlv {
    private static final String[] VISIT_PARAMS = {
        "step1:" + DBConstants.KEY.PURPOSE_OF_VISIT,
        "step1:" + DBConstants.KEY.PARTICIPANTS,
        "step1:" + DBConstants.KEY.TOPICS_TAUGHT,
        "step1:" + DBConstants.KEY.COMMENTS,
        DBConstants.KEY.PURPOSE_OF_VISIT,
        DBConstants.KEY.PARTICIPANTS,
        DBConstants.KEY.TOPICS_TAUGHT,
        DBConstants.KEY.COMMENTS,
        DBConstants.KEY.ATTENDANCE_TYPE,
        DBConstants.KEY.FOLLOW_UP_STATUS,
        DBConstants.KEY.SERVICE_INTERRUPTION_REASON,
        DBConstants.KEY.SERVICE_INTERRUPTION_REASON_OTHER,
        DBConstants.KEY.EXPECTED_FACILITY_VISIT_DATE,
        DBConstants.KEY.RETURNED_TO_FACILITY,
        DBConstants.KEY.FACILITY_RETURN_DATE,
        DBConstants.KEY.PARTNER_NON_ATTENDANCE_REASON,
        DBConstants.KEY.PARTNER_NON_ATTENDANCE_REASON_OTHER,
        DBConstants.KEY.PARTNER_EXPECTED_FACILITY_VISIT_DATE,
        DBConstants.KEY.PARTNER_ARRIVED_AT_FACILITY,
        DBConstants.KEY.PARTNER_FACILITY_ARRIVAL_DATE,
        DBConstants.KEY.CHILD_EID_TEST_TYPE,
        DBConstants.KEY.CHILD_EID_NON_ATTENDANCE_REASON,
        DBConstants.KEY.CHILD_EID_NON_ATTENDANCE_REASON_OTHER,
        DBConstants.KEY.CHILD_EID_EXPECTED_FACILITY_VISIT_DATE,
        DBConstants.KEY.CHILD_EID_ARRIVED_AT_FACILITY,
        DBConstants.KEY.CHILD_EID_FACILITY_ARRIVAL_DATE,
        DBConstants.KEY.HAS_BEEN_LINKED_TO_PSYCHOSOCIAL_SUPPORT_GROUP,
        DBConstants.KEY.HAS_BEEN_LINKED_TO_IGA_GROUP,
        DBConstants.KEY.EDUCATION_PROVIDED,
        DBConstants.KEY.WAY_EDUCATION_PROVIDED,
        DBConstants.KEY.TOPICS_BEING_PROVIDED,
        DBConstants.KEY.REFERRAL_GIVEN,
        DBConstants.KEY.TYPE_OF_REFERRAL_GIVEN,
        DBConstants.KEY.NEXT_APPOINTMENT_DATE
    };

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
    protected void processHealthFacilityVisit(List<Map<String, String>> hfVisits, Context context) {
        // Mother Mentor history is rendered from the visit action details below.
    }

    @Override
    public void processViewData(List<Visit> visits, Context context) {
        if (visits.isEmpty()) {
            return;
        }

        int days =
                Days.daysBetween(
                                new DateTime(visits.get(visits.size() - 1).getDate()),
                                new DateTime())
                        .getDays();
        List<LinkedHashMap<String, String>> visitDetails = new ArrayList<>();

        int x = 0;
        while (x < visits.size()) {
            LinkedHashMap<String, String> detailsMap = new LinkedHashMap<>();
            extractVisitDetails(visits, VISIT_PARAMS, detailsMap, x, context);
            detailsMap.put(DBConstants.KEY.VISIT_NUMBER, String.valueOf(x + 1));
            detailsMap.put(
                    "visit_completion_state",
                    Boolean.TRUE.equals(visits.get(x).getProcessed()) ? "completed" : "saved");
            visitDetails.add(detailsMap);
            x++;
        }

        processLastVisit(days, context);
        processVisit(visitDetails, context, visits);
    }

    private void extractVisitDetails(
            List<Visit> sourceVisits,
            String[] params,
            LinkedHashMap<String, String> visitDetailsMap,
            int iteration,
            Context context) {
        for (String param : params) {
            try {
                List<VisitDetail> details =
                        sourceVisits.get(iteration).getVisitDetails().get(param);
                visitDetailsMap.put(param, getTexts(context, details));
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    private void processLastVisit(int days, Context context) {
        linearLayoutLastVisit.setVisibility(View.VISIBLE);
        if (days < 1) {
            customFontTextViewLastVisit.setText(
                    org.smartregister.chw.core.R.string.less_than_twenty_four);
        } else {
            customFontTextViewLastVisit.setText(
                    StringUtils.capitalize(
                            MessageFormat.format(
                                    context.getString(org.smartregister.chw.core.R.string.days_ago),
                                    String.valueOf(days))));
        }
    }

    protected void processVisit(
            List<LinkedHashMap<String, String>> motherMentorVisits,
            Context context,
            List<Visit> visits) {
        final SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        if (motherMentorVisits == null || motherMentorVisits.isEmpty()) {
            return;
        }

        linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);
        int x = 0;
        for (LinkedHashMap<String, String> vals : motherMentorVisits) {
            View view = inflater.inflate(R.layout.medical_history_visit, null);
            view.findViewById(R.id.title).setVisibility(View.GONE);
            TextView tvTypeOfService = view.findViewById(R.id.type_of_service);
            LinearLayout visitDetailsLayout = view.findViewById(R.id.visit_details_layout);
            TextView tvEdit = view.findViewById(R.id.textview_edit);

            Visit visit = visits.get(x);
            boolean legacyFollowUp =
                    followUpType != null
                            && Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES.equals(
                                    visit.getVisitType());
            if (x == visits.size() - 1 && !legacyFollowUp) {
                tvEdit.setVisibility(View.VISIBLE);
            } else {
                tvEdit.setVisibility(View.GONE);
            }

            tvEdit.setOnClickListener(
                    view1 -> {
                        if (visit.getBaseEntityId() != null) {
                            ((Activity) context).finish();
                            if (StringUtils.isNotBlank(
                                    vals.get(DBConstants.KEY.FOLLOW_UP_STATUS))) {
                                MotherMentorFollowUpType editType =
                                        MotherMentorFollowUpType.PARTNER
                                                        .getEventType()
                                                        .equals(visit.getVisitType())
                                                ? MotherMentorFollowUpType.PARTNER
                                                : MotherMentorFollowUpType.CHILD_EID
                                                                .getEventType()
                                                                .equals(visit.getVisitType())
                                                        ? MotherMentorFollowUpType.CHILD_EID
                                                        : followUpType == null
                                                                ? MotherMentorFollowUpType
                                                                        .MOTHER_IIT
                                                                : followUpType;
                                MotherMentorVisitActivity.startMotherMentorFollowUpVisitActivity(
                                        (Activity) context,
                                        visit.getBaseEntityId(),
                                        true,
                                        editType);
                            } else {
                                MotherMentorVisitActivity.startMotherMentorVisitActivity(
                                        (Activity) context, visit.getBaseEntityId(), true);
                            }
                        }
                    });

            String visitType = getVisitType(context, visits.get(x).getVisitType());
            tvTypeOfService.setText(
                    String.format(
                            "%s - %s",
                            visitType, simpleDateFormat.format(visits.get(x).getDate())));

            for (LinkedHashMap.Entry<String, String> entry : vals.entrySet()) {
                TextView visitDetailTv = new TextView(context);
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                visitDetailTv.setLayoutParams(params);
                float scale = context.getResources().getDisplayMetrics().density;
                int dpAsPixels = (int) (10 * scale + 0.5f);
                visitDetailTv.setPadding(dpAsPixels, 0, 0, 0);
                visitDetailsLayout.addView(visitDetailTv);

                int titleResource =
                        context.getResources()
                                .getIdentifier(
                                        "mothermentor_history_"
                                                + normalizeHistoryKey(entry.getKey()),
                                        "string",
                                        context.getPackageName());
                evaluateView(context, vals, visitDetailTv, entry.getKey(), titleResource);
            }
            linearLayoutHealthFacilityVisitDetails.addView(view, 0);
            x++;
        }
    }

    private String getVisitType(Context context, String visitType) {
        if (Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES.equals(visitType)
                || Constants.EVENT_TYPE.MOTHERMENTOR_CONTACT_VISIT.equals(visitType)
                || Constants.EVENT_TYPE.MOTHER_MENTOR_FOLLOW_UP_VISIT.equals(visitType)
                || MotherMentorFollowUpType.PARTNER.getEventType().equals(visitType)
                || MotherMentorFollowUpType.CHILD_EID.getEventType().equals(visitType)) {
            return context.getString(R.string.mothermentor_visit_history_title);
        }
        return visitType;
    }

    private void evaluateView(
            Context context,
            Map<String, String> vals,
            TextView tv,
            String valueKey,
            int viewTitleStringResource) {
        if (StringUtils.isBlank(getMapValue(vals, valueKey))) {
            tv.setVisibility(View.GONE);
            return;
        }

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder
                .append(
                        getStringResource(context, viewTitleStringResource, valueKey),
                        boldSpan,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .append("\n");

        String[] values = getMapValue(vals, valueKey).split(",");
        if (values.length > 1) {
            for (String value : values) {
                spannableStringBuilder.append(
                        getStringResource(context, value.trim()) + "\n",
                        new BulletSpan(10),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else {
            spannableStringBuilder
                    .append(getStringResource(context, getMapValue(vals, valueKey)))
                    .append("\n");
        }
        tv.setText(spannableStringBuilder);
    }

    private String getMapValue(Map<String, String> map, String key) {
        if (map.containsKey(key)) {
            return map.get(key);
        }
        return "";
    }

    private String getStringResource(Context context, int resourceId, String fallbackKey) {
        try {
            return context.getString(resourceId);
        } catch (Exception e) {
            Timber.e(e);
            return fallbackKey.replace("_", " ");
        }
    }

    private String getStringResource(Context context, String rawValue) {
        String value = rawValue.trim();
        int resourceId =
                context.getResources()
                        .getIdentifier(
                                "mothermentor_history_" + normalizeResourceName(value),
                                "string",
                                context.getPackageName());
        try {
            return context.getString(resourceId);
        } catch (Exception e) {
            Timber.e(e);
            return value;
        }
    }

    private String normalizeHistoryKey(String key) {
        if (key == null) {
            return "";
        }
        return key.replace("step1:", "");
    }

    private String normalizeResourceName(String value) {
        return value.toLowerCase(Locale.getDefault())
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
