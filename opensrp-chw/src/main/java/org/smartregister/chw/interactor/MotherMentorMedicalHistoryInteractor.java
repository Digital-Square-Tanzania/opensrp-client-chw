package org.smartregister.chw.interactor;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;

import org.smartregister.chw.anc.contract.BaseAncMedicalHistoryContract;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.CoreBaseAncMedicalHistoryInteractor;
import org.smartregister.chw.domain.SortableVisit;
import org.smartregister.chw.mothermentor.domain.VisitDetail;
import org.smartregister.chw.mothermentor.repository.MotherMentorChildEidResultEvent;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.VisitUtils;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.util.MotherMentorFollowUpVisitUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MotherMentorMedicalHistoryInteractor extends CoreBaseAncMedicalHistoryInteractor {
    private static final Gson GSON = new Gson();
    private static final List<String> CHILD_EID_RESULT_FIELDS = Arrays.asList(
            DBConstants.KEY.MTOTO_RECEIVED_RESULT, DBConstants.KEY.MTOTO_RESULT_RECEIVED_DATE,
            DBConstants.KEY.MTOTO_CONFIRMATORY_TEST_DONE, DBConstants.KEY.MTOTO_CONFIRMATORY_RESULT);
    private final MotherMentorFollowUpType followUpType;

    public MotherMentorMedicalHistoryInteractor() {
        this(null);
    }

    public MotherMentorMedicalHistoryInteractor(MotherMentorFollowUpType followUpType) {
        this.followUpType = followUpType;
    }

    public static List<SortableVisit> getVisits(String memberID, String... eventTypes) {
        List<org.smartregister.chw.mothermentor.domain.Visit> motherMentorVisits = new ArrayList<>();
        if (eventTypes != null) {
            for (String eventType : eventTypes) {
                motherMentorVisits.addAll(VisitUtils.getVisitsOnly(memberID, eventType));
            }
        }

        return toSortableVisits(motherMentorVisits);
    }

    private static List<SortableVisit> toSortableVisits(
            List<org.smartregister.chw.mothermentor.domain.Visit> motherMentorVisits) {
        for (org.smartregister.chw.mothermentor.domain.Visit visit : motherMentorVisits) {
            List<VisitDetail> detailList = VisitUtils.getVisitDetailsOnly(visit.getVisitId());
            Map<String, List<VisitDetail>> details = VisitUtils.getVisitGroups(detailList);
            if (MotherMentorFollowUpType.CHILD_EID.getEventType().equals(visit.getVisitType())) {
                addLinkedChildEidAnswers(visit, details);
            }
            visit.setVisitDetails(details);
        }

        List<SortableVisit> sortableVisits = new ArrayList<>();
        for (org.smartregister.chw.mothermentor.domain.Visit visit : motherMentorVisits) {
            SortableVisit sortableVisit = GSON.fromJson(GSON.toJson(visit), SortableVisit.class);
            sortableVisits.add(sortableVisit);
        }

        Collections.sort(sortableVisits);
        return sortableVisits;
    }

    private static void addLinkedChildEidAnswers(org.smartregister.chw.mothermentor.domain.Visit visit,
            Map<String, List<VisitDetail>> details) {
        Map<String, String> answers = MotherMentorChildEidResultEvent.historyAnswers(
                ChwApplication.getInstance().getRepository().getReadableDatabase(), visit);
        for (Map.Entry<String, String> answer : answers.entrySet()) {
            if (TextUtils.isEmpty(answer.getValue()) || (details.containsKey(answer.getKey())
                    && !CHILD_EID_RESULT_FIELDS.contains(answer.getKey()))) continue;
            VisitDetail detail = new VisitDetail();
            detail.setVisitKey(answer.getKey());
            detail.setDetails(answer.getValue());
            details.put(answer.getKey(), Collections.singletonList(detail));
        }
    }

    @Override
    public void getMemberHistory(final String memberID, final Context context, final BaseAncMedicalHistoryContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            List<SortableVisit> visits;
            if (followUpType == null) {
                visits = getVisits(memberID, Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES,
                        Constants.EVENT_TYPE.MOTHERMENTOR_CONTACT_VISIT);
            } else {
                visits = toSortableVisits(MotherMentorFollowUpVisitUtils.getVisits(memberID, followUpType));
            }
            final List<Visit> allVisits = new ArrayList<>(visits);
            appExecutors.mainThread().execute(() -> callBack.onDataFetched(allVisits));
        };

        appExecutors.diskIO().execute(runnable);
    }
}
