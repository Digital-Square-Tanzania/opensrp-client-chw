package org.smartregister.chw.actionhelper;

import android.content.Context;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.client.utils.constants.JsonFormConstants;
import org.smartregister.dao.AbstractDao;
import org.smartregister.domain.Alert;
import org.smartregister.domain.AlertStatus;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.VaccineWrapper;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class ImmunizationActionHelper implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {

    private Context context;
    private final List<VaccineWrapper> wrappers;
    private LocalDate dueDate;
    private AlertStatus status;

    private List<String> keys = new ArrayList<>();
    private final Map<String, List<String>> completedVaccines = new HashMap<>();
    private final List<String> notDoneVaccines = new ArrayList<>();
    private final Map<String, VaccineRepo.Vaccine> vaccineMap = new HashMap<>();

    public ImmunizationActionHelper(Context context, List<VaccineWrapper> wrappers) {
        this.context = context;
        this.wrappers = wrappers;
        List<VaccineRepo.Vaccine> repo = VaccineRepo.getVaccines(CoreConstants.SERVICE_GROUPS.CHILD);
        for (VaccineRepo.Vaccine v : repo) {
            vaccineMap.put(v.display().toLowerCase().replace(" ", "_"), v);
        }
        initialize();
    }

    private void initialize() {
        LocalDate dueDate = null;
        AlertStatus myStatus = null;

        for (VaccineWrapper vaccineWrapper : wrappers) {
            Alert alert = vaccineWrapper.getAlert();

            if (myStatus == null || (alert != null && !alert.status().equals(AlertStatus.expired))) {
                myStatus = alert.status();
            } else if (alert != null && alert.status().equals(AlertStatus.urgent)) {
                myStatus = alert.status();
            }

            if (dueDate == null) {
                dueDate = new LocalDate(alert.startDate());
            }

            keys.add(NCUtils.removeSpaces(vaccineWrapper.getName()));
        }

        this.dueDate = new LocalDate(dueDate);
        this.status = myStatus;
    }

    @Override
    public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
        this.context = context;
    }

    @Override
    public String getPreProcessed() {
        return null;
    }

    public void onPayloadReceived(String jsonPayload) {
        try {
            notDoneVaccines.clear();
            completedVaccines.clear();

            if(jsonPayload == null) return;

            JSONObject jsonObject = new JSONObject(jsonPayload);

            JSONArray jsonArray = jsonObject.getJSONObject("step1").getJSONArray("fields");
            int totalVacs = jsonArray.length();
            int x = 0;
            while (x < totalVacs) {
                JSONObject fieldObject = jsonArray.getJSONObject(x);
                String key = fieldObject.has(JsonFormConstants.KEY) ? fieldObject.getString(JsonFormConstants.KEY) : "";
                String val = fieldObject.has(JsonFormConstants.VALUE) ? fieldObject.getString(JsonFormConstants.VALUE) : "";

                if (val.equalsIgnoreCase(Constants.HOME_VISIT.VACCINE_NOT_GIVEN)) {
                    notDoneVaccines.add(key);
                } else {
                    List<String> vacs = completedVaccines.get(val);
                    if (vacs == null) {
                        vacs = new ArrayList<>();
                    }

                    vacs.add(key);

                    completedVaccines.put(val, vacs);
                }

                x++;
            }

        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
        if (status != null && status.value().equals(AlertStatus.urgent.value())) {
            return BaseAncHomeVisitAction.ScheduleStatus.OVERDUE;
        }

        return BaseAncHomeVisitAction.ScheduleStatus.DUE;
    }

    @Override
    public String getPreProcessedSubTitle() {
        String due = context.getString(R.string.due);
        if (status != null && status.name().equals(AlertStatus.urgent.name())) {
            due = context.getString(R.string.overdue);
        }

        return MessageFormat.format("{0} {1}", due, DateTimeFormat.forPattern("dd MMM yyyy").print(dueDate));
    }

    /**
     * update all the vaccine wrappers with the dates required
     *
     * @param s
     * @return
     */
    @Override
    public String postProcess(String s) {
        return null;
    }

    @Override
    public String evaluateSubTitle() {
        SimpleDateFormat native_date = AbstractDao.getDobDateFormat();
        SimpleDateFormat new_date = new SimpleDateFormat(org.smartregister.chw.util.Constants.DATE_FORMATS.HOME_VISIT_DISPLAY, Locale.getDefault());

        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, List<String>> entry : completedVaccines.entrySet()) {
            StringBuilder completedBuilder = new StringBuilder();
            for (String vac : entry.getValue()) {
                if (completedBuilder.length() > 0) {
                    completedBuilder.append(", ");
                }

                completedBuilder.append(getTranslatedValue(vac.toUpperCase()));
            }

            if(!entry.getKey().matches("\\d{4}-\\d{2}-\\d{2}")){
                continue;
            }

            if (completedBuilder.length() > 0) {
                try {
                    if (builder.length() > 0) {
                        builder.append(" · ");
                    }

                    builder.append(MessageFormat.format("{0} {1} {2}",
                            completedBuilder.toString(),
                            context.getString(R.string.given_on_with_spaces),
                            new_date.format(native_date.parse(entry.getKey()))
                    ));
                } catch (ParseException e) {
                    Timber.e(e);
                }
            }
        }

        StringBuilder pendingBuilder = new StringBuilder();
        for (String vac : notDoneVaccines) {
            if (pendingBuilder.length() > 0) {
                pendingBuilder.append(", ");
            }

            pendingBuilder.append(getTranslatedValue(vac));
        }

        if (pendingBuilder.length() > 0) {

            if (builder.length() > 0) {
                builder.append(" · ");
            }

            builder.append(MessageFormat.format("{0} {1}",
                    pendingBuilder.toString().toUpperCase(),
                    context.getString(R.string.not_given_with_spaces)
            ));
        }

        return builder.toString();
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (!notDoneVaccines.isEmpty()) {
            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        }

        if (!completedVaccines.isEmpty()) {
            return BaseAncHomeVisitAction.Status.COMPLETED;
        }

        return BaseAncHomeVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
        Timber.v("onPayloadReceived");
    }

    private String getTranslatedValue(String name) {
        VaccineRepo.Vaccine res = vaccineMap.get(name.toLowerCase());
        if (res == null) {
            return name;
        }

        String val = res.display().toLowerCase().replace(" ", "_");
        return Utils.getStringResourceByName(val, context);
    }
}