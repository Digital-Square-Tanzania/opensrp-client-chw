package org.smartregister.chw.interactor;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.chw.mothermentor.contract.BaseMotherMentorVisitContract;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.domain.VisitDetail;
import org.smartregister.chw.mothermentor.interactor.BaseMotherMentorServiceVisitInteractor;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.util.JsonFormUtils;
import org.smartregister.chw.util.MotherMentorFollowUpType;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/** Shared section UI and validation plumbing for category-specific follow-up forms. */
public abstract class BaseMotherMentorFollowUpVisitInteractor extends BaseMotherMentorServiceVisitInteractor {
    private static final String STATUS = "status";
    private static final String REASON = "reason";
    private static final String ARRIVAL = "arrival";
    private static final String COMMENTS = "comments";
    protected static final String FOLLOW_UP_STATUS = "follow_up_status";
    protected static final String COMMENTS_FIELD = "comments";

    private final FollowUpFormDefinition definition;
    private final LinkedHashMap<String, BaseMotherMentorVisitAction> actions = new LinkedHashMap<>();
    private final Map<String, BaseMotherMentorVisitAction> workflowActions = new LinkedHashMap<>();

    protected BaseMotherMentorFollowUpVisitInteractor(FollowUpFormDefinition definition) {
        super(definition.eventType);
        this.definition = definition;
    }

    @Override
    public MemberObject getMemberClient(String memberId, String profileType) {
        MemberObject memberObject = super.getMemberClient(memberId, profileType);
        return memberObject != null ? memberObject : MotherMentorDao.getSecondaryEnrollmentMember(memberId);
    }

    @Override
    protected void populateActionList(BaseMotherMentorVisitContract.InteractorCallBack callBack) {
        appExecutors.diskIO().execute(() -> {
            actions.clear();
            workflowActions.clear();
            try {
                add(STATUS, definition.statusTitle, definition.statusForm, new StatusHelper(), null, false);
                add(REASON, definition.reasonTitle, definition.reasonForm, new ReasonHelper(), new FoundValidator(), false);
                add(ARRIVAL, definition.arrivalTitle, definition.arrivalForm, new ArrivalHelper(), new FoundValidator(), false);
                add(COMMENTS, definition.commentsTitle, definition.commentsForm, new CommentsHelper(), new AnsweredValidator(), true);
            } catch (BaseMotherMentorVisitAction.ValidationException e) {
                Timber.e(e);
            }
            appExecutors.mainThread().execute(() -> callBack.preloadActions(actions));
        });
    }

    private void add(String key, int title, String form,
                     BaseMotherMentorVisitAction.MotherMentorVisitActionHelper helper,
                     BaseMotherMentorVisitAction.Validator validator, boolean optional)
            throws BaseMotherMentorVisitAction.ValidationException {
        String localizedTitle = context.getString(title);
        BaseMotherMentorVisitAction action = getBuilder(localizedTitle).withOptional(optional).withDetails(details)
                .withHelper(helper).withValidator(validator).withFormName(form).build();
        actions.put(localizedTitle, action);
        workflowActions.put(key, action);
    }

    @Override
    public void submitVisit(boolean edit, String memberId, Map<String, BaseMotherMentorVisitAction> map,
                            BaseMotherMentorVisitContract.InteractorCallBack callback) {
        LinkedHashMap<String, BaseMotherMentorVisitAction> visible = new LinkedHashMap<>();
        for (Map.Entry<String, BaseMotherMentorVisitAction> entry : map.entrySet()) {
            if (entry.getValue().isValid()) visible.put(entry.getKey(), entry.getValue());
        }
        super.submitVisit(edit, memberId, visible, callback);
    }

    @Override protected String getEncounterType() { return definition.eventType; }
    @Override protected String getTableName() { return MotherMentorFollowUpType.FOLLOW_UP_TABLE; }

    @Override
    protected void prepareEvent(Event event) {
        super.prepareEvent(event);
        if (event != null) {
            List<Object> values = new ArrayList<>();
            values.add(definition.category);
            event.addObs(new Obs("concept", "text", "follow_up_category", "", values,
                    new ArrayList<>(), null, "follow_up_category"));
        }
    }

    private String value(String actionKey, String field) {
        BaseMotherMentorVisitAction action = workflowActions.get(actionKey);
        if (action == null || StringUtils.isBlank(action.getJsonPayload())) return null;
        try {
            return JsonFormUtils.getValue(new JSONObject(action.getJsonPayload()), field);
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    private boolean statusAnswered() { return StringUtils.isNotBlank(value(STATUS, FOLLOW_UP_STATUS)); }
    private boolean clientFound() { return "found".equalsIgnoreCase(value(STATUS, FOLLOW_UP_STATUS)); }

    private void clearHiddenActions() {
        if (clientFound()) return;
        clearAction(REASON);
        clearAction(ARRIVAL);
    }

    private void clearAction(String key) {
        BaseMotherMentorVisitAction action = workflowActions.get(key);
        if (action != null) {
            action.setProcessedJsonPayload(null);
            action.setSubTitle(null);
            action.evaluateStatus();
        }
    }

    private class FoundValidator implements BaseMotherMentorVisitAction.Validator {
        @Override public boolean isValid(String key) { return clientFound(); }
        @Override public boolean isEnabled(String key) { return clientFound(); }
        @Override public void onChanged(String key) { }
    }

    private class AnsweredValidator implements BaseMotherMentorVisitAction.Validator {
        @Override public boolean isValid(String key) { return statusAnswered(); }
        @Override public boolean isEnabled(String key) { return statusAnswered(); }
        @Override public void onChanged(String key) { }
    }

    private abstract static class Helper implements BaseMotherMentorVisitAction.MotherMentorVisitActionHelper {
        Context context;
        JSONObject json;

        @Override public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> details) { this.context = context; }
        @Override public void onPayloadReceived(String payload) {
            try { json = new JSONObject(payload); }
            catch (Exception e) { json = null; Timber.e(e); }
        }
        String get(String key) { return json == null ? null : JsonFormUtils.getValue(json, key); }
        String translate(String prefix, String value) {
            if (StringUtils.isBlank(value)) return null;
            int id = context.getResources().getIdentifier(prefix + value, "string", context.getPackageName());
            return id == 0 ? value : context.getString(id);
        }
        BaseMotherMentorVisitAction.Status complete(boolean complete) {
            return complete ? BaseMotherMentorVisitAction.Status.COMPLETED : BaseMotherMentorVisitAction.Status.PENDING;
        }
        @Override public String getPreProcessed() { return null; }
        @Override public BaseMotherMentorVisitAction.ScheduleStatus getPreProcessedStatus() { return null; }
        @Override public String getPreProcessedSubTitle() { return null; }
        @Override public String postProcess(String source) { return null; }
        @Override public void onPayloadReceived(BaseMotherMentorVisitAction action) { }
    }

    private class StatusHelper extends Helper {
        @Override public void onPayloadReceived(String payload) { super.onPayloadReceived(payload); clearHiddenActions(); }
        @Override public String evaluateSubTitle() { return translate("mothermentor_status_", get(FOLLOW_UP_STATUS)); }
        @Override public BaseMotherMentorVisitAction.Status evaluateStatusOnPayload() { return complete(StringUtils.isNotBlank(get(FOLLOW_UP_STATUS))); }
    }

    private class ReasonHelper extends Helper {
        @Override public String evaluateSubTitle() {
            String reason = translate(definition.reasonTranslationPrefix, get(definition.reasonField));
            String date = get(definition.expectedDateField);
            return StringUtils.isBlank(reason) ? null : reason + (StringUtils.isBlank(date) ? "" : " · " + date);
        }
        @Override public BaseMotherMentorVisitAction.Status evaluateStatusOnPayload() {
            String reason = get(definition.reasonField);
            return complete(StringUtils.isNotBlank(reason) && StringUtils.isNotBlank(get(definition.expectedDateField))
                    && (!"other".equals(reason) || StringUtils.isNotBlank(get(definition.reasonOtherField))));
        }
        @Override public String postProcess(String source) {
            return "other".equals(get(definition.reasonField)) ? null : clearField(source, definition.reasonOtherField);
        }
    }

    private class ArrivalHelper extends Helper {
        @Override public String evaluateSubTitle() {
            String answer = translate("mothermentor_answer_", get(definition.arrivalField));
            String date = get(definition.arrivalDateField);
            return StringUtils.isBlank(answer) ? null : answer + (StringUtils.isBlank(date) ? "" : " · " + date);
        }
        @Override public BaseMotherMentorVisitAction.Status evaluateStatusOnPayload() {
            String answer = get(definition.arrivalField);
            return complete(StringUtils.isNotBlank(answer)
                    && (!"yes".equals(answer) || StringUtils.isNotBlank(get(definition.arrivalDateField))));
        }
        @Override public String postProcess(String source) {
            return "yes".equals(get(definition.arrivalField)) ? null : clearField(source, definition.arrivalDateField);
        }
    }

    private static class CommentsHelper extends Helper {
        @Override public String evaluateSubTitle() { return get(COMMENTS_FIELD); }
        @Override public BaseMotherMentorVisitAction.Status evaluateStatusOnPayload() { return complete(StringUtils.isNotBlank(get(COMMENTS_FIELD))); }
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

    protected static class FollowUpFormDefinition {
        final String eventType;
        final String category;
        final int statusTitle, reasonTitle, arrivalTitle, commentsTitle;
        final String statusForm, reasonForm, arrivalForm, commentsForm;
        final String reasonField, reasonOtherField, expectedDateField, arrivalField, arrivalDateField;
        final String reasonTranslationPrefix;

        protected FollowUpFormDefinition(String eventType, String category, int statusTitle, int reasonTitle, int arrivalTitle,
                                         int commentsTitle, String statusForm, String reasonForm, String arrivalForm,
                                         String commentsForm, String reasonField, String reasonOtherField,
                                         String expectedDateField, String arrivalField, String arrivalDateField,
                                         String reasonTranslationPrefix) {
            this.eventType = eventType;
            this.category = category;
            this.statusTitle = statusTitle;
            this.reasonTitle = reasonTitle;
            this.arrivalTitle = arrivalTitle;
            this.commentsTitle = commentsTitle;
            this.statusForm = statusForm;
            this.reasonForm = reasonForm;
            this.arrivalForm = arrivalForm;
            this.commentsForm = commentsForm;
            this.reasonField = reasonField;
            this.reasonOtherField = reasonOtherField;
            this.expectedDateField = expectedDateField;
            this.arrivalField = arrivalField;
            this.arrivalDateField = arrivalDateField;
            this.reasonTranslationPrefix = reasonTranslationPrefix;
        }
    }
}
