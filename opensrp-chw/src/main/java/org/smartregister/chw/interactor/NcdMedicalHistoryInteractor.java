package org.smartregister.chw.interactor;

import static org.smartregister.chw.anc.util.VisitUtils.getVisitDetailsOnly;
import static org.smartregister.chw.anc.util.VisitUtils.getVisitGroups;
import static org.smartregister.chw.anc.util.VisitUtils.getVisitsOnly;

import android.content.Context;

import com.google.gson.Gson;

import org.smartregister.chw.anc.contract.BaseAncMedicalHistoryContract;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.core.CoreBaseAncMedicalHistoryInteractor;
import org.smartregister.chw.domain.SortableVisit;
import org.smartregister.chw.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads NCD Monthly Follow-Up visits from the shared visits table for the Medical History screen.
 * Filters by {@link Constants.EncounterType#NCD_MONTHLY_FOLLOWUP} so only NCD encounters render.
 */
public class NcdMedicalHistoryInteractor extends CoreBaseAncMedicalHistoryInteractor {

    @Override
    public void getMemberHistory(final String memberID, final Context context,
                                 final BaseAncMedicalHistoryContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            List<Visit> visits = new ArrayList<>(
                    getVisitsOnly(memberID, Constants.EncounterType.NCD_MONTHLY_FOLLOWUP));

            for (int i = 0; i < visits.size(); i++) {
                Visit visit = visits.get(i);
                List<VisitDetail> detailList = getVisitDetailsOnly(visit.getVisitId());
                visit.setVisitDetails(getVisitGroups(detailList));
            }

            List<SortableVisit> sortableVisits = new ArrayList<>();
            Gson gson = new Gson();
            for (Visit visit : visits) {
                SortableVisit sortableVisit = gson.fromJson(gson.toJson(visit), SortableVisit.class);
                sortableVisits.add(sortableVisit);
            }
            Collections.sort(sortableVisits);

            final List<Visit> allVisits = new ArrayList<>(sortableVisits);
            appExecutors.mainThread().execute(() -> callBack.onDataFetched(allVisits));
        };

        appExecutors.diskIO().execute(runnable);
    }
}
