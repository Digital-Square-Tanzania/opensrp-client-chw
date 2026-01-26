package org.smartregister.chw.interactor;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.anc.contract.BaseAncMedicalHistoryContract;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.interactor.BaseAncMedicalHistoryInteractor;
import org.smartregister.chw.ayp.AypLibrary;
import org.smartregister.chw.ayp.repository.VisitDetailsRepository;
import org.smartregister.chw.ayp.repository.VisitRepository;
import org.smartregister.chw.ayp.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class AypInSchoolMedicalHistoryInteractor extends BaseAncMedicalHistoryInteractor {

    @Override
    public void getMemberHistory(final String memberID, final Context context, final BaseAncMedicalHistoryContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            List<Visit> visits = new ArrayList<>();
            try {
                AypLibrary library = AypLibrary.getInstance();
                if (library == null) {
                    Timber.w("AypLibrary instance not initialised when loading medical history");
                } else {
                    VisitRepository visitRepository = library.visitRepository();
                    VisitDetailsRepository detailsRepository = library.visitDetailsRepository();
                    appendVisits(visits, visitRepository.getVisits(memberID, Constants.EVENT_TYPE.AYP_SERVICES), detailsRepository);
                    appendVisits(visits, visitRepository.getVisits(memberID, Constants.EVENT_TYPE.AYP_IN_SCHOOL_FOLLOW_UP_VISIT), detailsRepository);
                }
            } catch (Exception e) {
                Timber.e(e);
            }

            if (!visits.isEmpty()) {
                visits.sort(new VisitDateComparator());
            }

            final List<Visit> finalVisits = new ArrayList<>(visits);
            appExecutors.mainThread().execute(() -> callBack.onDataFetched(finalVisits));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void appendVisits(List<Visit> destination,
                              List<org.smartregister.chw.ayp.domain.Visit> source,
                              VisitDetailsRepository detailsRepository) {
        if (source == null || source.isEmpty()) {
            return;
        }

        for (org.smartregister.chw.ayp.domain.Visit srcVisit : source) {
            Visit converted = convertVisit(srcVisit, detailsRepository);
            destination.add(converted);
        }
    }

    private Visit convertVisit(org.smartregister.chw.ayp.domain.Visit source,
                               VisitDetailsRepository detailsRepository) {
        Visit visit = new Visit();
        visit.setVisitId(source.getVisitId());
        visit.setVisitType(source.getVisitType());
        visit.setVisitGroup(source.getVisitGroup());
        visit.setParentVisitID(source.getParentVisitID());
        visit.setBaseEntityId(source.getBaseEntityId());
        visit.setDate(source.getDate());
        visit.setUpdatedAt(source.getUpdatedAt());
        visit.setEventId(source.getEventId());
        visit.setFormSubmissionId(source.getFormSubmissionId());
        visit.setPreProcessedJson(source.getPreProcessedJson());
        visit.setJson(source.getJson());
        visit.setProcessed(source.getProcessed());
        visit.setCreatedAt(source.getCreatedAt());

        List<org.smartregister.chw.ayp.domain.VisitDetail> visitDetails = detailsRepository.getVisits(source.getVisitId());
        List<VisitDetail> convertedDetails = convertVisitDetails(visitDetails);
        visit.setVisitDetails(groupByVisitKey(convertedDetails));
        return visit;
    }

    private List<VisitDetail> convertVisitDetails(List<org.smartregister.chw.ayp.domain.VisitDetail> details) {
        if (details == null || details.isEmpty()) {
            return Collections.emptyList();
        }

        List<VisitDetail> converted = new ArrayList<>(details.size());
        for (org.smartregister.chw.ayp.domain.VisitDetail detail : details) {
            VisitDetail mapped = new VisitDetail();
            mapped.setVisitDetailsId(detail.getVisitDetailsId());
            mapped.setVisitId(detail.getVisitId());
            mapped.setBaseEntityId(detail.getBaseEntityId());
            mapped.setVisitKey(detail.getVisitKey());
            mapped.setParentCode(detail.getParentCode());
            mapped.setDetails(detail.getDetails());
            mapped.setHumanReadable(detail.getHumanReadable());
            mapped.setJsonDetails(detail.getJsonDetails());
            mapped.setPreProcessedJson(detail.getPreProcessedJson());
            mapped.setPreProcessedType(detail.getPreProcessedType());
            mapped.setProcessed(detail.getProcessed());
            mapped.setUpdatedAt(detail.getUpdatedAt());
            mapped.setCreatedAt(detail.getCreatedAt());
            converted.add(mapped);
        }
        return converted;
    }

    private Map<String, List<VisitDetail>> groupByVisitKey(List<VisitDetail> details) {
        if (details == null || details.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<VisitDetail>> grouped = new HashMap<>();
        for (VisitDetail detail : details) {
            if (detail == null || StringUtils.isBlank(detail.getVisitKey())) {
                continue;
            }
            grouped.computeIfAbsent(detail.getVisitKey(), key -> new ArrayList<>()).add(detail);
        }
        return grouped;
    }

    private static class VisitDateComparator implements Comparator<Visit> {
        @Override
        public int compare(Visit first, Visit second) {
            if (first == null && second == null) {
                return 0;
            } else if (first == null) {
                return 1;
            } else if (second == null) {
                return -1;
            }

            // Prefer visit date, fallback to updatedAt, then createdAt
            long firstMillis = extractComparableMillis(first);
            long secondMillis = extractComparableMillis(second);
            return Long.compare(secondMillis, firstMillis);
        }

        private long extractComparableMillis(Visit visit) {
            if (visit.getDate() != null) {
                return visit.getDate().getTime();
            } else if (visit.getUpdatedAt() != null) {
                return visit.getUpdatedAt().getTime();
            } else if (visit.getCreatedAt() != null) {
                return visit.getCreatedAt().getTime();
            }
            return 0L;
        }
    }
}
