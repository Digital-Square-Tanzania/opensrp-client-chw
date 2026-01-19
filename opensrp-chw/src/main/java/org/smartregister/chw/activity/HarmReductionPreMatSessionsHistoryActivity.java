package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.ISODateTimeFormat;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.presenter.BaseAncMedicalHistoryPresenter;
import org.smartregister.chw.core.activity.CoreAncMedicalHistoryActivity;
import org.smartregister.chw.core.activity.DefaultAncMedicalHistoryActivityFlv;
import org.smartregister.chw.harmreduction.dao.HarmReductionDao;
import org.smartregister.chw.harmreduction.domain.MemberObject;
import org.smartregister.chw.interactor.HarmReductionVisitHistoryInteractor;
import org.smartregister.chw.util.ReportUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class HarmReductionPreMatSessionsHistoryActivity extends CoreAncMedicalHistoryActivity {
    private static final String REPORT_BASE_URL = "https://appassets.androidplatform.net/assets/reports/harmreduction/";
    private static final String[] VISIT_PARAMS = {
            "client_status",
            "health_education_provided",
            "health_education_other_specify"
    };

    private static MemberObject harmReductionMemberObject;

    private final HarmReductionPreMatSessionsHistoryActivityFlv flavor = new HarmReductionPreMatSessionsHistoryActivityFlv();
    private ProgressBar progressBar;
    private final List<Visit> displayedVisits = new ArrayList<>();

    public static void startMe(Activity activity, MemberObject memberObject) {
        Intent intent = new Intent(activity, HarmReductionPreMatSessionsHistoryActivity.class);
        harmReductionMemberObject = memberObject;
        activity.startActivity(intent);
    }

    @Override
    public void initializePresenter() {
        presenter = new BaseAncMedicalHistoryPresenter(new HarmReductionVisitHistoryInteractor(), this, harmReductionMemberObject.getBaseEntityId());
    }

    @Override
    public void setUpView() {
        linearLayout = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.linearLayoutMedicalHistory);
        progressBar = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.progressBarMedicalHistory);

        TextView tvTitle = findViewById(org.smartregister.chw.opensrp_chw_anc.R.id.tvTitle);
        tvTitle.setText(getString(org.smartregister.chw.opensrp_chw_anc.R.string.back_to, harmReductionMemberObject.getFullName()));

        ((TextView) findViewById(R.id.medical_history)).setText(getString(org.smartregister.chw.harmreduction.R.string.harm_reduction_pre_mat_session_history));
    }

    @Override
    public View renderView(List<Visit> visits) {
        List<Visit> filteredVisits = filterVisitsAfterMatConsent(visits);
        List<Visit> orderedVisits = sortVisitsByDateAscending(filteredVisits);
        displayedVisits.clear();
        displayedVisits.addAll(orderedVisits);
        super.renderView(orderedVisits);
        View view = flavor.bindViews(this);
        displayLoadingState(true);
        flavor.processViewData(orderedVisits, this);
        displayLoadingState(false);
        TextView visitTitle = view.findViewById(org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle);
        visitTitle.setText(org.smartregister.chw.harmreduction.R.string.harm_reduction_pre_mat_session_history);
        return view;
    }

    @Override
    public void displayLoadingState(boolean state) {
        progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_harm_reduction_pre_mat_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_download_harm_reduction_pre_mat_report) {
            generateContactReportPdf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private List<Visit> filterVisitsAfterMatConsent(List<Visit> visits) {
        Date consentDate = getMatConsentDate();
        if (consentDate == null) {
            return visits;
        }

        List<Visit> filteredVisits = new ArrayList<>();
        for (Visit visit : visits) {
            Date visitDate = visit.getDate();
            if (visitDate != null && visitDate.after(consentDate)) {
                filteredVisits.add(visit);
            }
        }
        return filteredVisits;
    }

    private List<Visit> sortVisitsByDateAscending(List<Visit> visits) {
        List<Visit> sortedVisits = new ArrayList<>(visits);
        sortedVisits.sort(Comparator.comparing(Visit::getDate, Comparator.nullsLast(Date::compareTo)));
        return sortedVisits;
    }


    private void generateContactReportPdf() {
        String templateHtml = loadReportTemplate();
        if (StringUtils.isBlank(templateHtml)) {
            return;
        }

        List<ContactInfo> contactInfos = flavor.buildContactInfoForPdf(displayedVisits, this);
        String populatedHtml = populateContactTable(templateHtml, contactInfos);
        HarmReductionPreMatPrintActivity.start(this, populatedHtml, buildPrintJobName());
    }

    private String loadReportTemplate() {
        try (InputStream inputStream = getAssets().open("reports/harmreduction/5. HR Report.htm");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            return builder.toString();
        } catch (IOException e) {
            Timber.e(e);
            return null;
        }
    }

    private String populateContactTable(String templateHtml, List<ContactInfo> contactInfos) {
        String populatedHtml = templateHtml;
        String[] datePlaceholders = {"{{CONTACT1_DATE}}", "{{CONTACT2_DATE}}", "{{CONTACT3_DATE}}", "{{CONTACT4_DATE}}", "{{CONTACT5_DATE}}"};
        String[] infoPlaceholders = {"{{CONTACT1_INFO}}", "{{CONTACT2_INFO}}", "{{CONTACT3_INFO}}", "{{CONTACT4_INFO}}", "{{CONTACT5_INFO}}"};

        for (int i = 0; i < datePlaceholders.length; i++) {
            ContactInfo contactInfo = i < contactInfos.size() ? contactInfos.get(i) : null;
            String contactDate = contactInfo != null ? contactInfo.contactDate : "";
            populatedHtml = populatedHtml.replace(datePlaceholders[i], formatCellValue(contactDate));
            populatedHtml = populatedHtml.replace(infoPlaceholders[i], buildInformationCell(contactInfo));
        }

        populatedHtml = populatedHtml.replace("{{REFERRAL_TO_MAT}}", formatCellValue(formatMatReferralDate()));
        populatedHtml = applyDemographicInfo(populatedHtml);
        return populatedHtml;
    }

    private String applyDemographicInfo(String html) {
        String rocName = harmReductionMemberObject != null ? harmReductionMemberObject.getFullName() : "";
        String rocSex = getMemberGender();
        html = html.replace("{{ROC_NAME}}", formatCellValue(rocName));
        html = html.replace("{{ROC_SEX}}", formatCellValue(rocSex));
        return html;
    }

    private String getMemberGender() {
        if (harmReductionMemberObject == null) {
            return "";
        }
        try {
            return harmReductionMemberObject.getGender();
        } catch (Exception e) {
            Timber.d(e);
        }
        try {
            // fallback if the model exposes sex instead of gender
            java.lang.reflect.Method method = harmReductionMemberObject.getClass().getMethod("getSex");
            Object value = method.invoke(harmReductionMemberObject);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            Timber.d(e);
        }
        return "";
    }

    private String buildInformationCell(ContactInfo contactInfo) {
        if (contactInfo == null || contactInfo.informationPoints.isEmpty()) {
            return "";
        }

        List<String> encodedDetails = new ArrayList<>();
        for (String detail : contactInfo.informationPoints) {
            if (StringUtils.isNotBlank(detail)) {
                encodedDetails.add(TextUtils.htmlEncode(detail));
            }
        }
        return encodedDetails.isEmpty() ? "" : StringUtils.join(encodedDetails, "<br/>");
    }

    private String formatCellValue(String value) {
        return StringUtils.isNotBlank(value) ? TextUtils.htmlEncode(value) : "";
    }

    private String formatMatReferralDate() {
        Date consentDate = getMatConsentDate();
        if (consentDate == null) {
            return "";
        }
        return new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(consentDate);
    }

    private String buildPrintJobName() {
        if (harmReductionMemberObject == null) {
            return getString(org.smartregister.chw.harmreduction.R.string.harm_reduction_pre_mat_session_history);
        }
        int age = 0;
        try {
            age = harmReductionMemberObject.getAge();
        } catch (Exception e) {
            Timber.e(e);
        }
        return String.format(Locale.getDefault(), "%s %s %s, %d",
                StringUtils.defaultString(harmReductionMemberObject.getFirstName()),
                StringUtils.defaultString(harmReductionMemberObject.getMiddleName()),
                StringUtils.defaultString(harmReductionMemberObject.getLastName()),
                age).trim();
    }

    private Date getMatConsentDate() {
        try {
            String consentDateString = HarmReductionDao.getVisitDateForRocConsentForJoiningMatServices(harmReductionMemberObject.getBaseEntityId());
            return parseConsentDate(consentDateString);
        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }

    private Date parseConsentDate(String consentDateString) {
        if (StringUtils.isBlank(consentDateString)) {
            return null;
        }

        String trimmedDate = consentDateString.trim();
        if (StringUtils.isBlank(trimmedDate)) {
            return null;
        }
        if (StringUtils.isNumeric(trimmedDate)) {
            try {
                long timestamp = Long.parseLong(trimmedDate);
                if (trimmedDate.length() == 10) {
                    timestamp *= 1000;
                }
                return new Date(timestamp);
            } catch (NumberFormatException e) {
                Timber.d(e);
            }
        }

        try {
            return ISODateTimeFormat.dateOptionalTimeParser().parseDateTime(trimmedDate).toDate();
        } catch (IllegalArgumentException e) {
            Timber.d(e);
        }

        try {
            return Date.from(OffsetDateTime.parse(trimmedDate).toInstant());
        } catch (DateTimeParseException e) {
            Timber.d(e);
        }

        String[] patterns = new String[]{
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd",
                "dd-MM-yyyy"
        };

        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern, Locale.getDefault()).parse(trimmedDate);
            } catch (ParseException e) {
                Timber.d(e);
            }
        }

        try {
            return new DateTime(trimmedDate).toDate();
        } catch (IllegalArgumentException e) {
            Timber.e(e);
        }
        return null;
    }

    private static class ContactInfo {
        String contactDate = "";
        List<String> informationPoints = new ArrayList<>();
    }

    private static class HarmReductionPreMatSessionsHistoryActivityFlv extends DefaultAncMedicalHistoryActivityFlv {

        private final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);

        @Override
        protected void processAncCard(String has_card, Context context) {
            linearLayoutAncCard.setVisibility(View.GONE);
        }

        @Override
        protected void processHealthFacilityVisit(List<Map<String, String>> hf_visits, Context context) {
            // no-op
        }

        List<ContactInfo> buildContactInfoForPdf(List<Visit> visits, Context context) {
            List<ContactInfo> contactInfos = new ArrayList<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            for (Visit visit : visits) {
                ContactInfo contactInfo = new ContactInfo();
                Date visitDate = visit.getDate();
                if (visitDate != null) {
                    contactInfo.contactDate = dateFormat.format(visitDate);
                }

                Map<String, List<VisitDetail>> visitDetails = visit.getVisitDetails();
                addInformationPoint(contactInfo, visitDetails, "client_status", context);
                addInformationPoint(contactInfo, visitDetails, "health_education_provided", context);
                addInformationPoint(contactInfo, visitDetails, "health_education_other_specify", context);

                contactInfos.add(contactInfo);
            }
            return contactInfos;
        }

        private void addInformationPoint(ContactInfo contactInfo, Map<String, List<VisitDetail>> visitDetails, String key, Context context) {
            if (contactInfo == null || visitDetails == null) {
                return;
            }

            try {
                List<VisitDetail> details = visitDetails.get(key);
                String value = getTexts(context, details);
                if (StringUtils.isNotBlank(value)) {
                    contactInfo.informationPoints.add(value);
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        @Override
        public void processViewData(List<Visit> visits, Context context) {
            if (!visits.isEmpty()) {
                int days = 0;
                List<LinkedHashMap<String, String>> followupVisits = new ArrayList<>();

                int x = 0;
                while (x < visits.size()) {
                    LinkedHashMap<String, String> visitDetails = new LinkedHashMap<>();
                    if (x == 0) {
                        days = Days.daysBetween(new DateTime(visits.get(visits.size() - 1).getDate()), new DateTime()).getDays();
                    }

                    extractVisitDetails(visits, VISIT_PARAMS, visitDetails, x, context);
                    followupVisits.add(visitDetails);
                    x++;
                }

                processLastVisit(days, context);
                processVisit(followupVisits, context, visits);
            }
        }

        private void extractVisitDetails(List<Visit> sourceVisits, String[] params, LinkedHashMap<String, String> visitDetailsMap, int iteration, Context context) {
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            for (String param : params) {
                try {
                    List<VisitDetail> details = sourceVisits.get(iteration).getVisitDetails().get(param);
                    map.put(param, getTexts(context, details));
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
            visitDetailsMap.putAll(map);
        }

        private void processLastVisit(int days, Context context) {
            linearLayoutLastVisit.setVisibility(View.VISIBLE);
            if (days < 1) {
                customFontTextViewLastVisit.setText(org.smartregister.chw.core.R.string.less_than_twenty_four);
            } else {
                customFontTextViewLastVisit.setText(StringUtils.capitalize(MessageFormat.format(context.getString(org.smartregister.chw.core.R.string.days_ago), String.valueOf(days))));
            }
        }

        protected void processVisit(List<LinkedHashMap<String, String>> followupVisits, Context context, List<Visit> visits) {
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
            if (followupVisits != null && !followupVisits.isEmpty()) {
                linearLayoutHealthFacilityVisit.setVisibility(View.VISIBLE);

                int x = 0;
                for (LinkedHashMap<String, String> vals : followupVisits) {
                    View view = inflater.inflate(R.layout.medical_history_visit, null);
                    view.findViewById(R.id.title).setVisibility(View.GONE);
                    TextView tvTypeOfService = view.findViewById(R.id.type_of_service);
                    LinearLayout visitDetailsLayout = view.findViewById(R.id.visit_details_layout);
                    TextView tvEdit = view.findViewById(R.id.textview_edit);

                    if (x == visits.size() - 1) {
                        tvEdit.setVisibility(View.VISIBLE);
                    } else {
                        tvEdit.setVisibility(View.GONE);
                    }

                    tvEdit.setOnClickListener(view1 -> {
                        Visit visit = visits.get(visits.size() - 1);
                        if (visit.getBaseEntityId() != null) {
                            ((Activity) context).finish();
                            HarmReductionVisitActivity.startHarmReductionVisitActivity((Activity) context, visit.getBaseEntityId(), true);
                        }
                    });

                    String visitDateString = simpleDateFormat.format(visits.get(x).getDate());
                    String contactLabel = getContactLabel(x);
                    tvTypeOfService.setText(String.format(Locale.getDefault(), "%s CONTACT - %s", contactLabel, visitDateString));

                    for (LinkedHashMap.Entry<String, String> entry : vals.entrySet()) {
                        TextView visitDetailTv = new TextView(context);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                        visitDetailTv.setLayoutParams(params);
                        float scale = context.getResources().getDisplayMetrics().density;
                        int dpAsPixels = (int) (10 * scale + 0.5f);
                        visitDetailTv.setPadding(dpAsPixels, 0, 0, 0);
                        visitDetailsLayout.addView(visitDetailTv);

                        try {
                            int resource = context.getResources().getIdentifier("harm_reduction_" + entry.getKey(), "string", context.getPackageName());
                            evaluateView(context, vals, visitDetailTv, entry.getKey(), resource);
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }
                    linearLayoutHealthFacilityVisitDetails.addView(view, 0);
                    x++;
                }
            }
        }

        private void evaluateView(Context context, Map<String, String> vals, TextView tv, String valueKey, int viewTitleStringResource) {
            if (StringUtils.isNotBlank(getMapValue(vals, valueKey))) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                spannableStringBuilder.append(context.getString(viewTitleStringResource), boldSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append("\n");

                String stringValue = getMapValue(vals, valueKey);
                if (stringValue.contains(",")) {
                    String[] stringValueArray = stringValue.split(",");
                    for (String value : stringValueArray) {
                        String mValue = value.trim().replaceAll("^\\[|]$", "");
                        spannableStringBuilder.append(getStringResource(context, mValue) + "\n", new BulletSpan(10), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } else {
                    spannableStringBuilder.append(getStringResource(context, stringValue)).append("\n");
                }
                tv.setText(spannableStringBuilder);
            } else {
                tv.setVisibility(View.GONE);
            }
        }

        private String getMapValue(Map<String, String> map, String key) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
            return "";
        }

        private String getStringResource(Context context, String resourceName) {
            int resourceId = context.getResources()
                    .getIdentifier("harm_reduction_" + resourceName.trim(), "string", context.getPackageName());
            try {
                return context.getString(resourceId);
            } catch (Exception e) {
                Timber.e(e);
                return resourceName;
            }
        }

        private String getContactLabel(int visitIndex) {
            String[] ordinals = {"FIRST", "SECOND", "THIRD", "FOURTH", "FIFTH", "SIXTH", "SEVENTH", "EIGHTH", "NINTH", "TENTH"};
            if (visitIndex >= 0 && visitIndex < ordinals.length) {
                return ordinals[visitIndex];
            }

            int contactNumber = visitIndex + 1;
            return String.format(Locale.getDefault(), "%d%s", contactNumber, getOrdinalSuffix(contactNumber)).toUpperCase(Locale.getDefault());
        }

        private String getOrdinalSuffix(int number) {
            int mod100 = number % 100;
            if (mod100 >= 11 && mod100 <= 13) {
                return "th";
            }

            switch (number % 10) {
                case 1:
                    return "st";
                case 2:
                    return "nd";
                case 3:
                    return "rd";
                default:
                    return "th";
            }
        }
    }
}
