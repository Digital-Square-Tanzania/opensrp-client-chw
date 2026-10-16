package org.smartregister.chw.interactor;

import android.content.Context;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.mothermentor.contract.BaseMotherMentorVisitContract;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.VisitDetail;
import org.smartregister.chw.mothermentor.interactor.BaseMotherMentorServiceVisitInteractor;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.JsonFormUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import timber.log.Timber;

/** Four-section follow-up workflow; storage and sync use the existing Mother Mentor visit pipeline. */
public class MotherMentorFollowUpVisitInteractor extends BaseMotherMentorServiceVisitInteractor {
    private static final String STATUS = "status", REASON = "reason", RETURN = "return", COMMENTS = "comments";
    private static final String F_STATUS = "follow_up_status", F_REASON = "service_interruption_reason";
    private static final String F_OTHER = "service_interruption_reason_other", F_EXPECTED = "expected_facility_visit_date";
    private static final String F_RETURNED = "returned_to_facility", F_RETURN_DATE = "facility_return_date", F_COMMENTS = "comments";
    private final LinkedHashMap<String, BaseMotherMentorVisitAction> actions = new LinkedHashMap<>();
    private final Map<String, BaseMotherMentorVisitAction> workflowActions = new LinkedHashMap<>();

    public MotherMentorFollowUpVisitInteractor(String visitType) { super(visitType); }

    @Override
    public MemberObject getMemberClient(String memberId, String profileType) {
        MemberObject memberObject = super.getMemberClient(memberId, profileType);
        return memberObject != null ? memberObject : MotherMentorDao.getSecondaryEnrollmentMember(memberId);
    }

    @Override protected void populateActionList(BaseMotherMentorVisitContract.InteractorCallBack callBack) {
        appExecutors.diskIO().execute(() -> {
            actions.clear();
            workflowActions.clear();
            try {
                add(STATUS, R.string.mothermentor_follow_up_status_section, "mothermentor_follow_up_status", new StatusHelper(), null, false);
                add(REASON, R.string.mothermentor_interruption_reason_section, "mothermentor_service_interruption_reason", new ReasonHelper(), new FoundValidator(), false);
                add(RETURN, R.string.mothermentor_return_to_facility_section, "mothermentor_return_to_facility", new ReturnHelper(), new FoundValidator(), false);
                add(COMMENTS, R.string.mothermentor_comments_section, "mothermentor_follow_up_comment", new CommentsHelper(), new AnsweredValidator(), true);
            } catch (BaseMotherMentorVisitAction.ValidationException e) { Timber.e(e); }
            appExecutors.mainThread().execute(() -> callBack.preloadActions(actions));
        });
    }

    private void add(String key, int title, String form, BaseMotherMentorVisitAction.MotherMentorVisitActionHelper helper,
                     BaseMotherMentorVisitAction.Validator validator, boolean optional) throws BaseMotherMentorVisitAction.ValidationException {
        String localizedTitle = context.getString(title);
        BaseMotherMentorVisitAction action = getBuilder(localizedTitle).withOptional(optional).withDetails(details)
                .withHelper(helper).withValidator(validator).withFormName(form).build();
        actions.put(localizedTitle, action);
        workflowActions.put(key, action);
    }

    @Override public void submitVisit(boolean edit, String memberId, Map<String, BaseMotherMentorVisitAction> map,
                                      BaseMotherMentorVisitContract.InteractorCallBack callback) {
        LinkedHashMap<String, BaseMotherMentorVisitAction> visible = new LinkedHashMap<>();
        for (Map.Entry<String, BaseMotherMentorVisitAction> entry : map.entrySet()) if (entry.getValue().isValid()) visible.put(entry.getKey(), entry.getValue());
        super.submitVisit(edit, memberId, visible, callback);
    }

    @Override protected String getEncounterType() { return Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES; }

    private String value(String actionKey, String field) {
        BaseMotherMentorVisitAction action = workflowActions.get(actionKey);
        if (action == null || StringUtils.isBlank(action.getJsonPayload())) return null;
        try { return JsonFormUtils.getValue(new JSONObject(action.getJsonPayload()), field); }
        catch (Exception e) { Timber.e(e); return null; }
    }
    private boolean answered() { return StringUtils.isNotBlank(value(STATUS, F_STATUS)); }
    private boolean found() { return "found".equalsIgnoreCase(value(STATUS, F_STATUS)); }
    private void clearHidden() {
        if (found()) return;
        for (String key : new String[]{REASON, RETURN}) {
            BaseMotherMentorVisitAction action = workflowActions.get(key);
            if (action != null) { action.setProcessedJsonPayload(null); action.setSubTitle(null); action.evaluateStatus(); }
        }
    }

    private class FoundValidator implements BaseMotherMentorVisitAction.Validator {
        @Override public boolean isValid(String key) { return found(); }
        @Override public boolean isEnabled(String key) { return found(); }
        @Override public void onChanged(String key) { }
    }
    private class AnsweredValidator implements BaseMotherMentorVisitAction.Validator {
        @Override public boolean isValid(String key) { return answered(); }
        @Override public boolean isEnabled(String key) { return answered(); }
        @Override public void onChanged(String key) { }
    }

    private abstract static class Helper implements BaseMotherMentorVisitAction.MotherMentorVisitActionHelper {
        Context context; JSONObject json;
        @Override public void onJsonFormLoaded(String s, Context c, Map<String, List<VisitDetail>> d) { context = c; }
        @Override public void onPayloadReceived(String s) { try { json = new JSONObject(s); } catch (Exception e) { json = null; Timber.e(e); } }
        String get(String key) { return json == null ? null : JsonFormUtils.getValue(json, key); }
        String translate(String prefix, String value) {
            if (StringUtils.isBlank(value)) return null;
            int id = context.getResources().getIdentifier(prefix + value, "string", context.getPackageName());
            return id == 0 ? value : context.getString(id);
        }
        BaseMotherMentorVisitAction.Status complete(boolean yes) { return yes ? BaseMotherMentorVisitAction.Status.COMPLETED : BaseMotherMentorVisitAction.Status.PENDING; }
        @Override public String getPreProcessed() { return null; }
        @Override public BaseMotherMentorVisitAction.ScheduleStatus getPreProcessedStatus() { return null; }
        @Override public String getPreProcessedSubTitle() { return null; }
        @Override public String postProcess(String s) { return null; }
        @Override public void onPayloadReceived(BaseMotherMentorVisitAction action) { }
    }

    private class StatusHelper extends Helper {
        @Override public void onPayloadReceived(String s) { super.onPayloadReceived(s); clearHidden(); }
        @Override public String evaluateSubTitle() { return translate("mothermentor_status_", get(F_STATUS)); }
        @Override public BaseMotherMentorVisitAction.Status evaluateStatusOnPayload() { return complete(StringUtils.isNotBlank(get(F_STATUS))); }
    }
    private static class ReasonHelper extends Helper {
        @Override public String evaluateSubTitle() { String v = translate("mothermentor_reason_", get(F_REASON)), d = get(F_EXPECTED); return StringUtils.isBlank(v) ? null : v + (StringUtils.isBlank(d) ? "" : " · " + d); }
        @Override public BaseMotherMentorVisitAction.Status evaluateStatusOnPayload() { String r = get(F_REASON); return complete(StringUtils.isNotBlank(r) && StringUtils.isNotBlank(get(F_EXPECTED)) && (!"other".equals(r) || StringUtils.isNotBlank(get(F_OTHER)))); }
        @Override public String postProcess(String source) { return "other".equals(get(F_REASON)) ? null : clearField(source, F_OTHER); }
    }
    private static class ReturnHelper extends Helper {
        @Override public String evaluateSubTitle() { String v = translate("mothermentor_answer_", get(F_RETURNED)), d = get(F_RETURN_DATE); return StringUtils.isBlank(v) ? null : v + (StringUtils.isBlank(d) ? "" : " · " + d); }
        @Override public BaseMotherMentorVisitAction.Status evaluateStatusOnPayload() { String r = get(F_RETURNED); return complete(StringUtils.isNotBlank(r) && (!"yes".equals(r) || StringUtils.isNotBlank(get(F_RETURN_DATE)))); }
        @Override public String postProcess(String source) { return "yes".equals(get(F_RETURNED)) ? null : clearField(source, F_RETURN_DATE); }
    }
    private static class CommentsHelper extends Helper {
        @Override public String evaluateSubTitle() { return get(F_COMMENTS); }
        @Override public BaseMotherMentorVisitAction.Status evaluateStatusOnPayload() { return complete(StringUtils.isNotBlank(get(F_COMMENTS))); }
    }

    private static String clearField(String source, String key) {
        try {
            JSONObject form = new JSONObject(source);
            JSONObject field = com.vijay.jsonwizard.utils.FormUtils.getFieldFromForm(form, key);
            if (field != null) field.put("value", "");
            return form.toString();
        } catch (Exception e) {
            Timber.e(e);
            return source;
        }
    }
}
