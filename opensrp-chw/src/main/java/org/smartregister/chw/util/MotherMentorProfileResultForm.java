package org.smartregister.chw.util;

import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.MTOTO_CONFIRMATORY_RESULT;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.MTOTO_CONFIRMATORY_TEST_DONE;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.MTOTO_PENDING_TEST;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.MTOTO_PENDING_TEST_DATE;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.MTOTO_RECEIVED_RESULT;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.MTOTO_RESULT_RECEIVED_DATE;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.MTOTO_RUFAA_STATE_BEFORE;
import static org.smartregister.chw.mothermentor.util.MotherMentorMotherChildUtils.field;
import static org.smartregister.chw.mothermentor.util.MotherMentorMotherChildUtils.put;
import static org.smartregister.util.JsonFormUtils.ENTITY_ID;

import android.content.Context;

import org.joda.time.LocalDate;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.Answers;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent.PendingTest;
import org.smartregister.chw.mothermentor.util.MotherMentorChildEidLabels;
import org.smartregister.chw.mothermentor.util.MotherMentorChildEidResultForm;
import org.smartregister.chw.mothermentor.util.MotherMentorChildEidReturnVisit;
import org.smartregister.chw.mothermentor.util.MotherMentorHeiSchedule;
import org.smartregister.chw.mothermentor.util.MotherMentorHeiSchedule.Stage;

import java.util.Map;

/** Adapts the shared native result form to a linked result from the mother's profile. */
public final class MotherMentorProfileResultForm {
    private MotherMentorProfileResultForm() {}

    public static void prepare(JSONObject form, Context context, String motherId, Stage stage,
            LocalDate sampleDate, String state, Answers saved) throws JSONException {
        MotherMentorChildEidResultForm.prepare(form, context, stage.getCode(), sampleDate, state);
        form.put(ENTITY_ID, motherId);
        form.put("encounter_type", MotherMentorMotherResultEvent.EVENT_TYPE);
        form.getJSONObject("step1").put("title",
                context.getString(saved == null ? R.string.mama_results_record : R.string.mama_results_edit));
        boolean confirmation = MotherMentorChildEidReturnVisit.AWAITING_CONFIRM.equals(state);
        if (!confirmation) {
            field(form, MTOTO_RECEIVED_RESULT).put("label", context.getString(R.string.mama_results_question,
                    MotherMentorChildEidLabels.test(context, stage.getCode())));
        }
        if (saved == null) return;
        if (confirmation) {
            String done = saved.getConfirmationDone();
            put(form, MTOTO_CONFIRMATORY_TEST_DONE, done.isEmpty() ? "yes" : done);
            put(form, MTOTO_CONFIRMATORY_RESULT, done.isEmpty() ? saved.getResult() : saved.getConfirmationResult());
        } else {
            put(form, MTOTO_RECEIVED_RESULT, saved.getResult());
            put(form, MTOTO_CONFIRMATORY_TEST_DONE, saved.getConfirmationDone());
            put(form, MTOTO_CONFIRMATORY_RESULT, saved.getConfirmationResult());
        }
        if (saved.getReceivedDate() != null) {
            put(form, MTOTO_RESULT_RECEIVED_DATE, saved.getReceivedDate().toString("dd-MM-yyyy"));
        }
    }

    public static boolean matches(JSONObject form, Map<String, String> values, String motherId, PendingTest sample) {
        return motherId.equals(form.optString(ENTITY_ID))
                && MotherMentorMotherResultEvent.EVENT_TYPE.equals(form.optString("encounter_type"))
                && sample.getStage() == Stage.fromCode(values.get(MTOTO_PENDING_TEST))
                && sample.getTestDate().equals(MotherMentorHeiSchedule.parseDate(values.get(MTOTO_PENDING_TEST_DATE)))
                && sample.getState().equals(values.get(MTOTO_RUFAA_STATE_BEFORE));
    }

    public static Answers readAnswers(Map<String, String> values, boolean confirmation) {
        LocalDate receipt = MotherMentorHeiSchedule.parseDate(values.get(MTOTO_RESULT_RECEIVED_DATE));
        if (confirmation) return Answers.confirmation(values.get(MTOTO_CONFIRMATORY_TEST_DONE),
                values.get(MTOTO_CONFIRMATORY_RESULT), receipt);
        return new Answers(values.get(MTOTO_RECEIVED_RESULT), receipt,
                values.get(MTOTO_CONFIRMATORY_TEST_DONE), values.get(MTOTO_CONFIRMATORY_RESULT));
    }
}
