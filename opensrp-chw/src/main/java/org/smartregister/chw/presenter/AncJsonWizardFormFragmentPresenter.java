package org.smartregister.chw.presenter;

import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.presenters.JsonWizardFormFragmentPresenter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class AncJsonWizardFormFragmentPresenter extends JsonWizardFormFragmentPresenter {

    private static final String LMP_FIELD = "last_menstrual_period";
    private static final String FIRST_CLINIC_VISIT_FIELD = "first_clinic_visit_date";
    private static final int MINIMUM_DAYS_AFTER_LMP = 28;

    public AncJsonWizardFormFragmentPresenter(JsonFormFragment formFragment, JsonFormInteractor jsonFormInteractor) {
        super(formFragment, jsonFormInteractor);
    }

    @Override
    protected boolean moveToNextWizardStep() {
        if (JsonFormConstants.FIRST_STEP_NAME.equals(mStepName) && !validateStepOneBeforeNext()) {
            return false;
        }
        return super.moveToNextWizardStep();
    }

    private boolean validateStepOneBeforeNext() {
        String lmpDate = getFormFieldValue(LMP_FIELD);
        String firstClinicVisitDate = getFormFieldValue(FIRST_CLINIC_VISIT_FIELD);

        if (TextUtils.isEmpty(lmpDate) || TextUtils.isEmpty(firstClinicVisitDate)) {
            return true;
        }

        try {
            Date lmp = parseOpenSrpDate(lmpDate);
            Date firstVisit = parseOpenSrpDate(firstClinicVisitDate);

            long diffMillis = firstVisit.getTime() - lmp.getTime();
            long diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis);

            if (diffDays < MINIMUM_DAYS_AFTER_LMP) {
                showFieldError();
                return false;
            }

            clearFieldError();
            return true;
        } catch (Exception e) {
            Timber.e(e);
            showFieldError();
            return false;
        }
    }

    private String getFormFieldValue(String fieldKey) {
        String value = getFieldValueFromStep(fieldKey);
        if (!TextUtils.isEmpty(value)) {
            return value.trim();
        }

        View fieldView = getFieldView(fieldKey);
        if (fieldView instanceof MaterialEditText) {
            CharSequence text = ((MaterialEditText) fieldView).getText();
            return text == null ? "" : text.toString().trim();
        }

        return "";
    }

    private String getFieldValueFromStep(String fieldKey) {
        try {
            JsonApi jsonApi = getFormFragment().getJsonApi();
            if (jsonApi == null) {
                return "";
            }

            JSONObject stepOne = jsonApi.getStep(JsonFormConstants.FIRST_STEP_NAME);
            if (stepOne == null) {
                return "";
            }

            JSONArray fields = stepOne.optJSONArray(JsonFormConstants.FIELDS);
            if (fields == null) {
                return "";
            }

            JSONObject field = getField(fields, fieldKey);
            return field == null ? "" : field.optString(JsonFormConstants.VALUE, "");
        } catch (Exception e) {
            Timber.e(e);
            return "";
        }
    }

    private JSONObject getField(JSONArray fields, String fieldKey) {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (fieldKey.equals(field == null ? null : field.optString(JsonFormConstants.KEY))) {
                return field;
            }
        }
        return null;
    }

    private Date parseOpenSrpDate(String value) throws ParseException {
        List<String> formats = Arrays.asList("dd-MM-yyyy", "yyyy-MM-dd");
        ParseException parseException = null;

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                sdf.setLenient(false);
                return sdf.parse(value);
            } catch (ParseException e) {
                parseException = e;
            }
        }

        throw parseException == null ? new ParseException("Unsupported date format: " + value, 0) : parseException;
    }

    private void showFieldError() {
        String fieldError = getString(org.smartregister.chw.R.string.invalid_first_clinic_visit_date_error);
        String snackBarError = getString(org.smartregister.chw.R.string.invalid_first_clinic_visit_date_snackbar_error);
        View fieldView = getFieldView(FIRST_CLINIC_VISIT_FIELD);
        if (fieldView instanceof MaterialEditText) {
            MaterialEditText editText = (MaterialEditText) fieldView;
            editText.setError(fieldError);
            editText.requestFocus();
            getFormFragment().scrollToView(editText);
        }

        if (getFormFragment() != null) {
            getFormFragment().showSnackBar(snackBarError);
            if (!(fieldView instanceof MaterialEditText) && getFormFragment().getContext() != null) {
                Toast.makeText(getFormFragment().getContext(), snackBarError, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getString(int resId) {
        return getFormFragment() != null && getFormFragment().getContext() != null
                ? getFormFragment().getContext().getString(resId)
                : "";
    }

    private void clearFieldError() {
        View fieldView = getFieldView(FIRST_CLINIC_VISIT_FIELD);
        if (fieldView instanceof MaterialEditText) {
            ((MaterialEditText) fieldView).setError(null);
        }
    }

    private View getFieldView(String fieldKey) {
        try {
            JsonApi jsonApi = getFormFragment().getJsonApi();
            if (jsonApi == null) {
                return null;
            }

            View fieldView = jsonApi.getFormDataView(JsonFormConstants.FIRST_STEP_NAME + ":" + fieldKey);
            if (fieldView != null) {
                return fieldView;
            }

            fieldView = jsonApi.getFormDataView(mStepName + ":" + fieldKey);
            if (fieldView != null) {
                return fieldView;
            }

            fieldView = jsonApi.getFormDataView(fieldKey);
            if (fieldView != null) {
                return fieldView;
            }

            Collection<View> formDataViews = jsonApi.getFormDataViews();
            if (formDataViews == null) {
                return null;
            }

            for (View view : formDataViews) {
                if (view != null && fieldKey.equals(view.getTag(R.id.key))) {
                    return view;
                }
            }

            return null;
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }
}
