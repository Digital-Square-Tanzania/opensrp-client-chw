package org.smartregister.chw.fragment;

import static org.smartregister.chw.util.FnInterfaces.KeyValue;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vijay.jsonwizard.customviews.CheckBox;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.contract.ImmunizationSaveHost;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.VaccineWrapper;
import org.smartregister.chw.anc.contract.BaseAncHomeVisitContract;
import org.smartregister.chw.anc.domain.VaccineDisplay;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.util.FnList;
import org.smartregister.chw.util.UtilsFlv;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.smartregister.chw.R.id.*;


public class BaseHomeVisitImmunizationFragmentFlv extends DefaultBaseHomeVisitImmunizationFragment
        implements ImmunizationSaveHost.Dialog {
    private static final String STATE = "immunization_dialog";
    private String visitId;
    private String actionId;
    private String requestId;
    private Bundle restoredInputs;
    private Set<String> initialReasons = new HashSet<>();
    private boolean restoringInputs;
    private boolean delivered;
    private boolean saving;
    private boolean invalidRestoration;

    private View root;
    private String whenImmunizationGiven;

    public static BaseHomeVisitImmunizationFragmentFlv getInstance(final BaseAncHomeVisitContract.VisitView view, String baseEntityID, Map<String, List<VisitDetail>> details, List<VaccineDisplay> vaccineDisplays) {
        return getInstance(view, baseEntityID, details, vaccineDisplays, true);
    }

    public static BaseHomeVisitImmunizationFragmentFlv getInstance(final BaseAncHomeVisitContract.VisitView view, String baseEntityID, Map<String, List<VisitDetail>> details, List<VaccineDisplay> vaccineDisplays, boolean defaultChecked) {
        BaseHomeVisitImmunizationFragmentFlv fragment = new BaseHomeVisitImmunizationFragmentFlv();
        fragment.visitView = view;
        fragment.baseEntityID = baseEntityID;
        fragment.details = details;
        fragment.vaccinesDefaultChecked = defaultChecked;
        for (VaccineDisplay vaccineDisplay : vaccineDisplays) {
            fragment.vaccineDisplays.put(vaccineDisplay.getVaccineWrapper().getName(), vaccineDisplay);
        }

        if (details != null && !details.isEmpty()) {
            fragment.jsonObject = NCUtils.getVisitJSONFromVisitDetails(view.getMyContext(), baseEntityID, details, vaccineDisplays);
            JsonFormUtils.populateForm(fragment.jsonObject, details);
        }
        fragment.setArguments(fragment.saveDialogState());
        return fragment;
    }

    public static BaseHomeVisitImmunizationFragmentFlv getInstance(final BaseAncHomeVisitContract.VisitView view, String baseEntityID, Map<String, List<VisitDetail>> details, List<VaccineDisplay> vaccineDisplays, boolean defaultChecked, String whenImmunizationGiven) {
        BaseHomeVisitImmunizationFragmentFlv fragment = new BaseHomeVisitImmunizationFragmentFlv();
        fragment.visitView = view;
        fragment.baseEntityID = baseEntityID;
        fragment.details = details;
        fragment.whenImmunizationGiven = getWhenImmunizationGivenInEnglish(whenImmunizationGiven, view.getMyContext());
        fragment.vaccinesDefaultChecked = defaultChecked;
        for (VaccineDisplay vaccineDisplay : vaccineDisplays) {
            fragment.vaccineDisplays.put(vaccineDisplay.getVaccineWrapper().getName(), vaccineDisplay);
        }

        if (details != null && !details.isEmpty()) {
            fragment.jsonObject = NCUtils.getVisitJSONFromVisitDetails(view.getMyContext(), baseEntityID, details, vaccineDisplays);
            JsonFormUtils.populateForm(fragment.jsonObject, details);
        }
        fragment.setArguments(fragment.saveDialogState());
        return fragment;
    }


    public static String getWhenImmunizationGivenInEnglish(String name, Context context) {
        return FnList.from(R.string.at_birth, R.string.date_weeks, R.string.date_months).reduce(name, (n, id) ->
                n.replace(context.getString(id), UtilsFlv.getEnglishString(context, id))
        ).trim().replaceAll("\\W+", "_");
    }

    @Override
    public void bindToVisitAction(String visitId, String actionId, String requestId, String acceptedPayload) {
        this.visitId = visitId;
        this.actionId = actionId;
        this.requestId = requestId;
        delivered = false;
        if (!TextUtils.isEmpty(acceptedPayload)) {
            try {
                jsonObject = new JSONObject(acceptedPayload);
                restoredInputs = null;
            } catch (JSONException e) {
                invalidRestoration = true;
            }
        }
        setArguments(saveDialogState());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle state = savedInstanceState == null ? getArguments() : savedInstanceState.getBundle(STATE);
        if (state == null) {
            invalidRestoration = true;
            return;
        }
        visitId = state.getString("visit_id");
        actionId = state.getString("action_id");
        requestId = state.getString("request_id");
        baseEntityID = state.getString("child_id");
        whenImmunizationGiven = state.getString("when_given");
        delivered = state.getBoolean("delivered");
        invalidRestoration = state.getBoolean("invalid");
        vaccinesDefaultChecked = state.getBoolean("default_checked");
        datePickerHelper.relaxedDates = state.getBoolean("relaxed_dates");
        datePickerHelper.minimumDate = new Date(state.getLong("minimum_date", System.currentTimeMillis()));
        super.setRelaxedDates(datePickerHelper.relaxedDates);
        super.setMinimumDate(datePickerHelper.minimumDate);
        restoredInputs = state.getBundle("inputs");
        ArrayList<String> reasons = state.getStringArrayList("initial_reasons");
        if (reasons != null) initialReasons = new HashSet<>(reasons);
        ArrayList<Bundle> definitions = state.getParcelableArrayList("vaccines");
        vaccineDisplays.clear();
        if (definitions != null) {
            for (Bundle definition : definitions) {
                VaccineWrapper wrapper = new VaccineWrapper();
                wrapper.setName(definition.getString("name"));
                wrapper.setVaccine((VaccineRepo.Vaccine) definition.getSerializable("vaccine"));
                VaccineDisplay display = new VaccineDisplay();
                display.setVaccineWrapper(wrapper);
                display.setStartDate(new Date(definition.getLong("start_date")));
                if (definition.containsKey("end_date")) display.setEndDate(new Date(definition.getLong("end_date")));
                vaccineDisplays.put(wrapper.getName(), display);
            }
        }
        String payload = state.getString("payload");
        if (!TextUtils.isEmpty(payload)) {
            try {
                jsonObject = new JSONObject(payload);
            } catch (JSONException e) {
                invalidRestoration = true;
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBundle(STATE, saveDialogState());
        super.onSaveInstanceState(outState);
    }

    private Bundle saveDialogState() {
        Bundle state = new Bundle();
        state.putString("visit_id", visitId);
        state.putString("action_id", actionId);
        state.putString("request_id", requestId);
        state.putString("child_id", baseEntityID);
        state.putString("when_given", whenImmunizationGiven);
        state.putBoolean("delivered", delivered);
        state.putBoolean("invalid", invalidRestoration);
        state.putBoolean("default_checked", vaccinesDefaultChecked);
        state.putBoolean("relaxed_dates", datePickerHelper.relaxedDates);
        state.putLong("minimum_date", datePickerHelper.minimumDate.getTime());
        if (jsonObject != null) state.putString("payload", jsonObject.toString());
        state.putBundle("inputs", root == null ? restoredInputs : captureInputs());
        try {
            state.putStringArrayList("initial_reasons", new ArrayList<>(getPrevMissingReasonsForEdit()));
        } catch (JSONException e) {
            state.putBoolean("invalid", true);
        }
        ArrayList<Bundle> definitions = new ArrayList<>();
        for (VaccineDisplay display : vaccineDisplays.values()) {
            Bundle definition = new Bundle();
            definition.putString("name", display.getVaccineWrapper().getName());
            definition.putSerializable("vaccine", display.getVaccineWrapper().getVaccine());
            definition.putLong("start_date", display.getStartDate().getTime());
            if (display.getEndDate() != null) definition.putLong("end_date", display.getEndDate().getTime());
            definitions.add(definition);
        }
        state.putParcelableArrayList("vaccines", definitions);
        return state;
    }

    @Override
    protected void setCheckBoxState(@Nullable CheckBox checkBox, boolean state) {
        // Keep initialization synchronous so delayed defaults cannot overwrite restored selections.
        if (checkBox != null) checkBox.setChecked(state);
    }

    @Override
    public void updateSelectedVaccines(Map<String, String> selectedVaccines, boolean variedMode) {
        // NACP restores vaccine dates and missing-vaccine reasons together after its controls exist.
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        restoreInputs(restoredInputs == null ? inputsFromPayload() : restoredInputs);
        if (invalidRestoration || vaccineDisplays.isEmpty() || TextUtils.isEmpty(baseEntityID)) {
            showSaveError(R.string.immunization_restore_failed);
        }
    }

    @Override
    public void onDestroyView() {
        if (root != null) restoredInputs = captureInputs();
        root = null;
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        visitView = null;
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (delivered) closeAfterSave();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
    }

    private void init(View view) {
        root = view;
        root.findViewById(R.id.save_btn).setOnClickListener(this::save);
        RadioGroup radioGroup = root.findViewById(R.id.select_date_mode);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (!restoringInputs) datePickerHelper.toggleMultiMode(group, checkedId);
        });

        CheckBox noVaccine = root.findViewById(R.id.checkbox_no_vaccination).findViewById(R.id.select);
        noVaccine.setOnClickListener(ignore -> {
        });
        noVaccine.setOnCheckedChangeListener((checkbox, checked) -> {
            if (!restoringInputs) onNoVaccineSelected(checked);
        });

        try {
            createViewOptionForNoVaccines();
            listenForVaccineSelection();
        } catch (JSONException e) {
            invalidRestoration = true;
        }

    }

    private Set<String> getPrevMissingReasonsForEdit() throws JSONException {
        if (details != null) {
            JSONArray visitJSON = FnList.from(details.get("reasons_no_vaccination"))
                    .map(vd -> new JSONObject(vd.getDetails()))
                    .filter(json -> TextUtils.equals(whenImmunizationGiven, json.optString("when_immunization_given")))
                    .map(json -> json.getJSONArray("reasons_for_missing"))
                    .first(new JSONArray());
            return FnList.from(visitJSON).map(Object::toString).toSet();
        } else if (this.jsonObject != null) {
            JSONArray fields = this.jsonObject.getJSONObject("step1").getJSONArray("fields");
            JSONObject outerJsonObject;
            for (int index = 0; index < fields.length(); index++) {
                JSONObject field = fields.getJSONObject(index);
                if (field.getString("key").equals("reasons_no_vaccination")) {
                    if (!field.getString("value").isEmpty()) {
                        outerJsonObject = new JSONObject(field.getString("value"));
                        return FnList.from(outerJsonObject.getJSONArray("reasons_for_missing")).map(Object::toString).toSet();
                    }
                }
            }
        }
        return initialReasons;
    }

    private void createViewOptionForNoVaccines() throws JSONException {
        ViewGroup parent = root.findViewById(R.id.reasons_no_vaccines);
        Set<String> reasonsEdit = getPrevMissingReasonsForEdit();
        hide(reasonsEdit.isEmpty(), reasons_no_vaccines, why_no_vaccine);

        FnList.from(root.getResources().getStringArray(R.array.reason_no_vaccine))
                .map(KeyValue::create)
                .forEachItem(reason -> {
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_vaccine_name_check, parent, false);
                    CustomFontTextView label = view.findViewById(R.id.vaccine);
                    CheckBox checkBox = view.findViewById(R.id.select);
                    checkBox.setTag(reason.key);
                    checkBox.setChecked(reasonsEdit.contains(reason.key));
                    label.setText(reason.value);
                    parent.addView(view);
                });
    }

    private void onVaccineSelectStatusChange(View ignore) {
        int allVaccineCount = ((ViewGroup) root.findViewById(R.id.vaccination_name_layout)).getChildCount();
        int selectedVaccineCount = FnList.from(getVaccineValues()).filter(v -> v.selected).list().size();

        boolean allSelected = allVaccineCount == selectedVaccineCount && selectedVaccineCount > 0;
        boolean fewSelected = !allSelected && selectedVaccineCount > 0;
        boolean noneSelected = selectedVaccineCount == 0;

        if (allSelected) onAllVaccineSelected();
        if (fewSelected) onFewVaccineSelected();
        if (noneSelected) onNoVaccineSelected(false);
    }

    private void onAllVaccineSelected() {
        ViewGroup reasonsView = root.findViewById(R.id.reasons_no_vaccines);
        FnList.from(reasonsView)
                .forEachItem(v -> ((CheckBox) v.findViewById(R.id.select)).setChecked(false));
        hide(reasons_no_vaccines, why_no_vaccine);
        show(congratulate_has_all_vaccine, select_date_mode, select_date_mode_label, multiple_vaccine_date_pickerview, single_vaccine_add_layout, vaccination_name_layout);
        datePickerHelper.showDateForSelectedVaccines();
    }

    private void onFewVaccineSelected() {
        show(reasons_no_vaccines, vaccination_name_layout, why_no_vaccine, select_date_mode, select_date_mode_label, multiple_vaccine_date_pickerview, single_vaccine_add_layout);
        hide(congratulate_has_all_vaccine);
        datePickerHelper.showDateForSelectedVaccines();
    }

    private void onNoVaccineSelected(boolean showReasons) {
        hide(true, congratulate_has_all_vaccine);
        hide(showReasons, multiple_vaccine_date_pickerview, select_date_mode, select_date_mode_label, single_vaccine_add_layout, vaccination_name_layout);
        show(showReasons, reasons_no_vaccines, why_no_vaccine);

        datePickerHelper.clearDates();
        ViewGroup reasonsView = root.findViewById(R.id.reasons_no_vaccines);
        FnList.from(reasonsView)
                .forEachItem(v -> ((CheckBox) v.findViewById(R.id.select)).setChecked(false));
        FnList.from(root, R.id.vaccination_name_layout)
                .map(v -> (CheckBox) v.findViewById(R.id.select))
                .forEachItem(ch -> ch.setChecked(false));
    }

    private void show(boolean show, Integer... ids) {
        FnList.from(ids).forEachItem(id -> root.findViewById(id).setVisibility(show ? View.VISIBLE : View.GONE));
    }

    private void hide(boolean hide, Integer... ids) {
        show(!hide, ids);
    }

    private void show(Integer... ids) {
        show(true, ids);
    }

    private void hide(Integer... ids) {
        show(false, ids);
    }

    private void listenForVaccineSelection() {
        FnList.from(root, R.id.vaccination_name_layout)
                .map(v -> (CheckBox) v.findViewById(R.id.select))
                .forEachItem(this::onVaccineSelectStatusChange);
    }

    private List<VaccineValue> getVaccineValues() {
        ViewGroup datesView = root.findViewById(R.id.single_vaccine_add_layout);
        ViewGroup vaccineView = root.findViewById(R.id.vaccination_name_layout);
        List<VaccineValue> vaccineV = FnList.from(vaccineView).map(v -> new VaccineValue(v, this)).list();

        if (datesView.getVisibility() == View.VISIBLE) {
            FnList.from(datesView)
                    .forEachItem(view -> FnList.from(vaccineV).forEachItem(vc -> vc.setDateFromMultiMode(view)));
        }
        return vaccineV;
    }

    protected FnList<String> getSelectedReasonsNoVaccines() {
        return FnList.from(root, R.id.reasons_no_vaccines)
                .map(v -> (CheckBox) v.findViewById(R.id.select))
                .filter(CompoundButton::isChecked)
                .map(ch -> ch.getTag().toString());
    }

    private void save(View view) {
        if (saving || delivered) return;
        if (invalidRestoration || vaccineDisplays.isEmpty() || TextUtils.isEmpty(baseEntityID)) {
            showSaveError(R.string.immunization_restore_failed);
            return;
        }
        saving = true;
        try {
            JSONObject payload = buildPayload();
            if (payload == null || !(getActivity() instanceof ImmunizationSaveHost)
                    || TextUtils.isEmpty(visitId) || TextUtils.isEmpty(actionId) || TextUtils.isEmpty(requestId)
                    || !((ImmunizationSaveHost) getActivity()).saveImmunization(
                    visitId, actionId, baseEntityID, requestId, payload.toString())) {
                showSaveError(R.string.immunization_save_retry);
                return;
            }
            jsonObject = payload;
            delivered = true;
            root.findViewById(R.id.save_btn).setEnabled(false);
            closeAfterSave();
        } catch (JSONException e) {
            showSaveError(R.string.immunization_save_retry);
        } finally {
            saving = false;
        }
    }

    private JSONObject buildPayload() throws JSONException {
        Map<VaccineWrapper, String> dates = new LinkedHashMap<>();
        JSONArray missingVaccines = new JSONArray();
        boolean noneGiven = ((CheckBox) root.findViewById(R.id.checkbox_no_vaccination)
                .findViewById(R.id.select)).isChecked();
        SimpleDateFormat format = new SimpleDateFormat(org.smartregister.chw.anc.util.Constants.DATE_FORMATS.DOB, Locale.getDefault());
        for (VaccineValue value : getVaccineValues()) {
            VaccineWrapper wrapper = vaccineDisplays.get(value.key).getVaccineWrapper();
            boolean selected = value.selected && !noneGiven;
            dates.put(wrapper, selected ? format.format(value.date)
                    : org.smartregister.chw.anc.util.Constants.HOME_VISIT.VACCINE_NOT_GIVEN);
            if (!selected) missingVaccines.put(value.name);
        }
        JSONObject payload = NCUtils.getVisitJSONFromWrapper(getContext(), baseEntityID, dates);
        if (payload == null) return null;
        JSONArray reasons = new JSONArray();
        getSelectedReasonsNoVaccines().forEachItem(reasons::put);
        JSONObject value = new JSONObject().put("reasons_for_missing", reasons)
                .put("missing_vaccines", missingVaccines).put("when_immunization_given", whenImmunizationGiven);
        JSONObject field = new JSONObject().put("key", "reasons_no_vaccination")
                .put("openmrs_entity_parent", "vaccine").put("openmrs_entity", "concept")
                .put("type", "text").put("openmrs_entity_id", "reasons_no_vaccination")
                .put("value", value.toString());
        payload.getJSONObject("step1").getJSONArray("fields").put(field);
        return payload;
    }

    private void showSaveError(int message) {
        TextView error = root.findViewById(R.id.immunization_save_error);
        error.setText(message);
        error.setVisibility(View.VISIBLE);
    }

    private void closeAfterSave() {
        if (isAdded() && !getParentFragmentManager().isStateSaved()) {
            getParentFragmentManager().beginTransaction().remove(this).commit();
        }
    }

    private Bundle captureInputs() {
        Bundle inputs = new Bundle();
        inputs.putBoolean("none_given", ((CheckBox) root.findViewById(R.id.checkbox_no_vaccination)
                .findViewById(R.id.select)).isChecked());
        inputs.putBoolean("varied_dates", ((RadioGroup) root.findViewById(R.id.select_date_mode))
                .getCheckedRadioButtonId() == R.id.each_its_date);
        inputs.putLong("common_date", UtilsFlv.getDateFromDatePicker(root.findViewById(R.id.earlier_date_picker)).getTime());
        for (VaccineValue value : getVaccineValues()) {
            Bundle vaccine = new Bundle();
            vaccine.putBoolean("selected", value.selected);
            vaccine.putLong("date", value.date.getTime());
            inputs.putBundle(value.key, vaccine);
        }
        inputs.putStringArrayList("reasons", new ArrayList<>(getSelectedReasonsNoVaccines().list()));
        return inputs;
    }

    private Bundle inputsFromPayload() {
        Bundle inputs = new Bundle();
        Set<String> reasons = initialReasons;
        String previousDate = null;
        boolean anySelected = false;
        if (jsonObject != null) {
            JSONArray fields = jsonObject.optJSONObject("step1") == null ? null
                    : jsonObject.optJSONObject("step1").optJSONArray("fields");
            if (fields != null) {
                for (int index = 0; index < fields.length(); index++) {
                    JSONObject field = fields.optJSONObject(index);
                    if (field == null) continue;
                    if ("reasons_no_vaccination".equals(field.optString("key"))) {
                        try {
                            reasons = FnList.from(new JSONObject(field.optString("value"))
                                    .optJSONArray("reasons_for_missing")).map(Object::toString).toSet();
                        } catch (JSONException e) {
                            invalidRestoration = true;
                        }
                    }
                    for (String key : vaccineDisplays.keySet()) {
                        if (!NCUtils.removeSpaces(key).equals(field.optString("key"))) continue;
                        String value = field.optString("value");
                        boolean selected = !org.smartregister.chw.anc.util.Constants.HOME_VISIT.VACCINE_NOT_GIVEN.equals(value);
                        Bundle vaccine = new Bundle();
                        vaccine.putBoolean("selected", selected);
                        if (selected) {
                            try {
                                SimpleDateFormat format = new SimpleDateFormat(org.smartregister.chw.anc.util.Constants.DATE_FORMATS.DOB, Locale.getDefault());
                                format.setLenient(false);
                                long date = format.parse(value).getTime();
                                vaccine.putLong("date", date);
                                inputs.putLong("common_date", date);
                                if (previousDate != null && !previousDate.equals(value)) inputs.putBoolean("varied_dates", true);
                                previousDate = value;
                                anySelected = true;
                            } catch (java.text.ParseException e) {
                                invalidRestoration = true;
                            }
                        }
                        inputs.putBundle(key, vaccine);
                    }
                }
                inputs.putBoolean("none_given", !anySelected);
            }
        }
        inputs.putStringArrayList("reasons", new ArrayList<>(reasons));
        return inputs;
    }

    private void restoreInputs(Bundle inputs) {
        restoringInputs = true;
        CheckBox none = root.findViewById(R.id.checkbox_no_vaccination).findViewById(R.id.select);
        none.setChecked(inputs.getBoolean("none_given"));
        RadioGroup mode = root.findViewById(R.id.select_date_mode);
        if (inputs.getBoolean("varied_dates")) mode.check(R.id.each_its_date);
        else mode.check(R.id.shared_date);
        if (inputs.containsKey("common_date")) setPickerDate(root.findViewById(R.id.earlier_date_picker), inputs.getLong("common_date"));
        FnList.from(root, R.id.vaccination_name_layout).forEachItem(row -> {
            Bundle vaccine = inputs.getBundle((String) row.getTag());
            ((CheckBox) row.findViewById(R.id.select)).setChecked(vaccine != null && vaccine.getBoolean("selected"));
        });
        restoringInputs = false;
        if (none.isChecked()) onNoVaccineSelected(true);
        else onVaccineSelectStatusChange(null);
        FnList.from(root, R.id.single_vaccine_add_layout).forEachItem(row -> {
            Bundle vaccine = inputs.getBundle((String) row.getTag());
            if (vaccine != null && vaccine.containsKey("date")) setPickerDate(row.findViewById(R.id.earlier_date_picker), vaccine.getLong("date"));
        });
        ArrayList<String> reasons = inputs.getStringArrayList("reasons");
        FnList.from(root, R.id.reasons_no_vaccines).forEachItem(row -> {
            CheckBox choice = row.findViewById(R.id.select);
            choice.setChecked(reasons != null && reasons.contains(choice.getTag().toString()));
        });
    }

    private static void setPickerDate(DatePicker picker, long millis) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(millis);
        picker.updateDate(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
    }

    private static class VaccineValue {
        String key;
        String name;
        Date date;
        boolean selected;

        VaccineValue(View view, BaseHomeVisitImmunizationFragmentFlv fg) {
            CustomFontTextView tv = view.findViewById(R.id.vaccine);
            DatePicker dp = fg.root.findViewById(R.id.earlier_date_picker);
            CheckBox cb = view.findViewById(R.id.select);
            cb.setOnClickListener(fg::onVaccineSelectStatusChange);

            this.key = (String) view.getTag();
            this.name = tv.getText().toString();
            this.selected = cb.isChecked();
            this.date = dp == null ? new Date() : UtilsFlv.getDateFromDatePicker(dp);
        }

        void setDateFromMultiMode(View view) {
            DatePicker dt = view.findViewById(R.id.earlier_date_picker);
            if (key.equals(view.getTag())) {
                this.date = UtilsFlv.getDateFromDatePicker(dt);
            }
        }
    }

    private final DatePickerHelper datePickerHelper = new DatePickerHelper(this);

    @Override
    public void setRelaxedDates(boolean relaxedDates) {
        super.setRelaxedDates(relaxedDates);
        datePickerHelper.relaxedDates = relaxedDates;
    }

    @Override
    public void setMinimumDate(Date minimumDate) {
        super.setMinimumDate(minimumDate);
        datePickerHelper.minimumDate = minimumDate;
    }

    private static class DatePickerHelper {
        private DatePickerHelper(BaseHomeVisitImmunizationFragmentFlv b) {
            base = b;
        }

        BaseHomeVisitImmunizationFragmentFlv base;
        private Date minimumDate = new Date();
        private boolean relaxedDates = false;

        private void toggleMultiMode(RadioGroup group, int checkedId) {
            showDateForSelectedVaccines();
        }

        private void showDateForSelectedVaccines() {
            RadioGroup radioGroup = base.root.findViewById(R.id.select_date_mode);
            boolean sharedMode = radioGroup.getCheckedRadioButtonId() == R.id.each_its_date;

            base.hide(sharedMode, multiple_vaccine_date_pickerview);
            base.show(sharedMode, single_vaccine_add_layout);

            if (!sharedMode) return;
            View root = base.root;
            ViewGroup parent = root.findViewById(R.id.single_vaccine_add_layout);
            List<VaccineValue> values = base.getVaccineValues();
            parent.removeAllViews();
            FnList.from(values)
                    .filter(v -> v.selected)
                    .forEachItem(vaccineView -> {
                        View layout = LayoutInflater.from(root.getContext()).inflate(R.layout.custom_single_vaccine_view, parent, false);
                        TextView question = layout.findViewById(R.id.vaccines_given_when_title_question);
                        DatePicker datePicker = layout.findViewById(R.id.earlier_date_picker);
                        String translatedVaccineName = NCUtils.getStringResourceByName(vaccineView.name.toLowerCase().replace(" ", "_"), root.getContext());
                        question.setText(root.getContext().getString(R.string.when_vaccine, translatedVaccineName));

                        layout.setTag(vaccineView.key);
                        VaccineDisplay vaccineDisplay = base.vaccineDisplays.get(vaccineView.key);
                        if (vaccineDisplay != null)
                            initializeDatePicker(datePicker, vaccineDisplay);
                        setPickerDate(datePicker, vaccineView.date.getTime());
                        parent.addView(layout);
                    });
        }

        private void clearDates() {
            ViewGroup parent = base.root.findViewById(R.id.single_vaccine_add_layout);
            parent.removeAllViews();
        }

        private void initializeDatePicker(@NotNull DatePicker datePicker, @NotNull VaccineDisplay vaccineDisplay) {
            Date startDate = vaccineDisplay.getStartDate();
            Date endDate = (vaccineDisplay.getEndDate() != null && vaccineDisplay.getEndDate().before(new Date())) ?
                    vaccineDisplay.getEndDate() : new Date();

            Date minDate = (startDate.after(endDate) ? endDate : startDate);
            datePicker.setMinDate(relaxedDates ? minimumDate.getTime() : minDate.getTime());
            datePicker.setMaxDate((relaxedDates ? new Date() : endDate).getTime());
        }
    }
}


