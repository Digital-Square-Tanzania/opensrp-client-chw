package org.smartregister.chw.interactor;

import android.content.Context;

import com.google.gson.Gson;

import org.smartregister.chw.anc.contract.BaseAncMedicalHistoryContract;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.core.CoreBaseAncMedicalHistoryInteractor;
import org.smartregister.chw.domain.SortableVisit;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.VisitUtils;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.util.MotherMentorFollowUpVisitUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MotherMentorMedicalHistoryInteractor extends CoreBaseAncMedicalHistoryInteractor {
    private static final Gson GSON = new Gson();
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
            List<org.smartregister.chw.mothermentor.domain.VisitDetail> detailList = VisitUtils.getVisitDetailsOnly(visit.getVisitId());
            visit.setVisitDetails(VisitUtils.getVisitGroups(detailList));
        }

        List<SortableVisit> sortableVisits = new ArrayList<>();
        for (org.smartregister.chw.mothermentor.domain.Visit visit : motherMentorVisits) {
            SortableVisit sortableVisit = GSON.fromJson(GSON.toJson(visit), SortableVisit.class);
            sortableVisits.add(sortableVisit);
        }

        Collections.sort(sortableVisits);
        return sortableVisits;
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
