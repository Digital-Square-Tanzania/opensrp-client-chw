package org.smartregister.chw.interactor;

import static org.smartregister.chw.anc.util.VisitUtils.getVisitDetailsOnly;
import static org.smartregister.chw.anc.util.VisitUtils.getVisitGroups;
import static org.smartregister.chw.anc.util.VisitUtils.getVisitsOnly;
import static org.smartregister.chw.hivst.util.Constants.EVENT_TYPE.HIVST_MOBILIZATION;
import static org.smartregister.chw.util.Constants.Events.MOTHER_CHAMPION_SBCC_SESSIONS;

import android.content.Context;

import com.google.gson.Gson;

import org.smartregister.chw.anc.contract.BaseAncMedicalHistoryContract;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.core.CoreBaseAncMedicalHistoryInteractor;
import org.smartregister.chw.domain.SortableVisit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MotherChampionSbcMobilizationSessionDetailsInteractor extends CoreBaseAncMedicalHistoryInteractor {
    public static List<SortableVisit> getVisits(String memberID, String... eventTypes) {

        List<Visit> visits = new ArrayList<>();
        if (eventTypes != null) {
            for (String eventType : eventTypes) {
                List<Visit> visit = getVisitsOnly(memberID, eventType);
                visits.addAll(visit);
            }
        }

        int x = 0;
        while (visits.size() > x) {
            Visit visit = visits.get(x);
            List<VisitDetail> detailList = getVisitDetailsOnly(visit.getVisitId());
            visits.get(x).setVisitDetails(getVisitGroups(detailList));
            x++;
        }

        List<SortableVisit> sortableVisits = new ArrayList<>();
        for (Visit visit : visits) {
            Gson gson = new Gson();
            SortableVisit sortableVisit = gson.fromJson(gson.toJson(visit), SortableVisit.class);
            sortableVisits.add(sortableVisit);
        }

        Collections.sort(sortableVisits);

        return sortableVisits;
    }

    @Override
    public void getMemberHistory(final String memberID, final Context context, final BaseAncMedicalHistoryContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {

            String[] eventTypes = new String[]{MOTHER_CHAMPION_SBCC_SESSIONS};
            List<SortableVisit> visits = getVisits(memberID, eventTypes);
            final List<Visit> all_visits = new ArrayList<>(visits);
            appExecutors.mainThread().execute(() -> callBack.onDataFetched(Collections.singletonList(all_visits.get(0))));
        };

        appExecutors.diskIO().execute(runnable);
    }
}
