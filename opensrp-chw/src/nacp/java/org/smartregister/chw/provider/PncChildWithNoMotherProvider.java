package org.smartregister.chw.provider;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.view.View;

import org.apache.commons.lang3.StringUtils;
import org.jeasy.rules.api.Rules;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.util.DBConstants;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.rule.PncVisitAlertRule;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.HomeVisitUtil;
import org.smartregister.chw.core.provider.ChwPncRegisterProvider;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

public class PncChildWithNoMotherProvider extends ChwPncRegisterProvider {

    private Set<org.smartregister.configurableviews.model.View> visibleColumns;
    private Context context;
    private View.OnClickListener onClickListener;
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private CommonRepository commonRepository;

    public PncChildWithNoMotherProvider(Context context, CommonRepository commonRepository, Set visibleColumns, View.OnClickListener onClickListener, View.OnClickListener paginationClickListener) {
        super(context, commonRepository, visibleColumns, onClickListener, paginationClickListener);
        this.visibleColumns = visibleColumns;
        this.onClickListener = onClickListener;
        this.context = context;
        this.commonRepository = commonRepository;
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, RegisterViewHolder viewHolder) {

        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;

        if (this.visibleColumns.isEmpty()) {
            populatePatientColumn(pc, client, viewHolder);
            populateLastColumn(pc, viewHolder);
        }

        Utils.startAsyncTask(new UpdateAsyncTask(context, viewHolder, pc), null);

    }

    private void populatePatientColumn(CommonPersonObjectClient pc, SmartRegisterClient client, RegisterViewHolder viewHolder) {

        viewHolder.villageTown.setText(Utils.getValue(pc.getColumnmaps(), "village_town", true));
        String firstName = Utils.getValue(pc.getColumnmaps(), "first_name", true);
        String middleName = Utils.getValue(pc.getColumnmaps(), "middle_name", true);
        String fname = Utils.getName(firstName, middleName);
        String patientName = Utils.getName(fname, Utils.getValue(pc.getColumnmaps(), "last_name", true));
        String dobString = Utils.getValue(pc.getColumnmaps(), "dob", false);
        String dayPnc;
        if (StringUtils.isNotBlank(dobString)) {
            int age = Years.yearsBetween(new DateTime(dobString), new DateTime()).getYears();
            dayPnc = MessageFormat.format("{0}, {1}", patientName, age);
            viewHolder.patientNameAndAge.setText(dayPnc);
        } else {
            viewHolder.patientNameAndAge.setText(patientName);
        }

        DateTimeFormatter formatter = DateTimeFormat.forPattern(DATE_FORMAT_PATTERN);
        dayPnc = Utils.getValue(pc.getColumnmaps(), "delivery_date", true);
        if (StringUtils.isNotBlank(dayPnc)) {
            int period = Days.daysBetween(new DateTime(formatter.parseDateTime(dayPnc)), new DateTime()).getDays();
            String pncDay = MessageFormat.format("{0} {1}", context.getString(R.string.pnc_day), period);

            viewHolder.pncDay.setText(pncDay);
        }

        viewHolder.patientColumn.setOnClickListener(this.onClickListener);
        viewHolder.patientColumn.setTag(client);
        viewHolder.patientColumn.setTag(R.id.VIEW_ID, "click_view_normal");
        viewHolder.dueButton.setOnClickListener(this.onClickListener);
        viewHolder.dueButton.setTag(client);
        viewHolder.dueButton.setTag(R.id.VIEW_ID, "click_view_dosage_status");
        viewHolder.registerColumns.setOnClickListener(var1 -> viewHolder.patientColumn.performClick());

        viewHolder.dueWrapper.setOnClickListener(var1 -> viewHolder.dueButton.performClick());
    }

    private void populateLastColumn(CommonPersonObjectClient pc, RegisterViewHolder viewHolder) {
        if (commonRepository != null) {
            CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(pc.entityId());
            if (commonPersonObject != null) {
                viewHolder.dueButton.setVisibility(android.view.View.VISIBLE);
                viewHolder.dueButton.setText(context.getString(org.smartregister.chw.opensrp_chw_anc.R.string.anc_home_visit));
                viewHolder.dueButton.setAllCaps(true);
            } else {
                viewHolder.dueButton.setVisibility(android.view.View.GONE);
            }
        }
    }

    private class UpdateAsyncTask extends AsyncTask<Void, Void, Void> {
        private final RegisterViewHolder viewHolder;
        private final CommonPersonObjectClient pc;
        private final Context context;

        private final Rules rules;
        private PncVisitAlertRule pncVisitAlertRule;

        private UpdateAsyncTask(Context context, RegisterViewHolder viewHolder, CommonPersonObjectClient pc) {
            this.context = context;
            this.viewHolder = viewHolder;
            this.pc = pc;
            this.rules = CoreChwApplication.getInstance().getRulesEngineHelper().rules(CoreConstants.RULE_FILE.PNC_HOME_VISIT);
        }

        @Override
        protected Void doInBackground(Void... params) {
            //map = getChildDetails(pc.getCaseId());
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault());
            String baseEntityID = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.BASE_ENTITY_ID, false);
            String dayPnc = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.DELIVERY_DATE, true);
            Date deliveryDate = null;
            Date lastVisitDate = null;
            try {
                deliveryDate = sdf.parse(dayPnc);
            } catch (ParseException e) {
                Timber.e(e);
            }

            Visit lastVisit = AncLibrary.getInstance().visitRepository().getLatestVisit(baseEntityID, org.smartregister.chw.anc.util.Constants.EVENT_TYPE.PNC_HOME_VISIT);
            if (lastVisit != null) {
                lastVisitDate = lastVisit.getDate();
            }

            pncVisitAlertRule = HomeVisitUtil.getPncVisitStatus(rules, lastVisitDate, deliveryDate);
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            // Update status column
            if (pncVisitAlertRule == null || StringUtils.isBlank(pncVisitAlertRule.getVisitID())) {
                return;
            }

            if (pncVisitAlertRule != null
                    && StringUtils.isNotBlank(pncVisitAlertRule.getVisitID())
                    && !pncVisitAlertRule.getButtonStatus().equalsIgnoreCase(CoreConstants.VISIT_STATE.EXPIRED)
            ) {
                updateDueColumn(context, viewHolder, pncVisitAlertRule);
            }
        }
    }
}
