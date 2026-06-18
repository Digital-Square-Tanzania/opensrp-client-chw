package org.smartregister.chw.interactor;

import org.smartregister.chw.mothermentor.contract.BaseMotherMentorVisitContract;
import org.smartregister.chw.mothermentor.interactor.BaseMotherMentorServiceVisitInteractor;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.util.Constants;

import java.util.LinkedHashMap;

import timber.log.Timber;

public class MotherMentorServiceVisitInteractor extends BaseMotherMentorServiceVisitInteractor {
    private static final String FORM_VISIT_TYPE = "mothermentor_visit_type";
    private static final String FORM_GROUP_LINKAGE = "mothermentor_visit_group_linkage";
    private static final String FORM_EDUCATION_PROVIDED = "mothermentor_visit_education_provided";
    private static final String FORM_REFERRAL_PROVIDED = "mothermentor_visit_referal";
    private static final String FORM_NEXT_VISIT_DATE = "mothermentor_next_visit_date";
    private static final String FORM_COMMENT = "mothermentor_visit_comment";

    private final LinkedHashMap<String, BaseMotherMentorVisitAction> motherMentorVisitActions = new LinkedHashMap<>();

    public MotherMentorServiceVisitInteractor(String visitType) {
        super(visitType);
    }

    @Override
    protected void populateActionList(BaseMotherMentorVisitContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            motherMentorVisitActions.clear();
            try {
                addVisitAction("Visit Type", FORM_VISIT_TYPE);
                addVisitAction("Group Linkage", FORM_GROUP_LINKAGE);
                addVisitAction("Education Provided", FORM_EDUCATION_PROVIDED);
                addVisitAction("Referral Provided", FORM_REFERRAL_PROVIDED);
                addVisitAction("Next Visit Date", FORM_NEXT_VISIT_DATE);
                addVisitAction("Comment", FORM_COMMENT);
            } catch (BaseMotherMentorVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(motherMentorVisitActions));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void addVisitAction(String title, String formName) throws BaseMotherMentorVisitAction.ValidationException {
        BaseMotherMentorVisitAction action = getBuilder(title)
                .withOptional(false)
                .withDetails(details)
                .withFormName(formName)
                .build();
        motherMentorVisitActions.put(title, action);
    }

    @Override
    protected String getEncounterType() {
        return Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES;
    }
}
