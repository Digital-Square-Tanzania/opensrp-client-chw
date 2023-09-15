package org.smartregister.chw.interactor;

import static org.smartregister.chw.anc.util.VisitUtils.getVisitDetailsOnly;
import static org.smartregister.chw.anc.util.VisitUtils.getVisitGroups;
import static org.smartregister.chw.anc.util.VisitUtils.getVisitsOnly;

import android.content.Context;

import org.smartregister.chw.anc.contract.BaseAncMedicalHistoryContract;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.util.VisitUtils;
import org.smartregister.chw.core.CoreBaseAncMedicalHistoryInteractor;
import org.smartregister.chw.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class AncHfMedicalHistoryInteractor extends CoreBaseAncMedicalHistoryInteractor {
    @Override
    public void getMemberHistory(final String memberID, final Context context, final BaseAncMedicalHistoryContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {

            String[] eventTypes = new String[2];
            eventTypes[0] = Constants.Events.ANC_FIRST_FACILITY_VISIT;
            eventTypes[1] = Constants.Events.ANC_RECURRING_FACILITY_VISIT;
            List<Visit> visits = getVisits(memberID, eventTypes);
            final List<Visit> all_visits = new ArrayList<>(visits);

            for (Visit visit : visits) {
                List<Visit> child_visits = VisitUtils.getChildVisits(visit.getVisitId());
                all_visits.addAll(child_visits);
            }
            appExecutors.mainThread().execute(() -> callBack.onDataFetched(all_visits));
        };

        appExecutors.diskIO().execute(runnable);
    }

    public static List<Visit> getVisits(String memberID, String... eventTypes) {

        List<Visit> visits = new ArrayList<>();
        if(eventTypes != null && eventTypes.length > 0) {
            for (String eventType : eventTypes) {
                List<Visit>  visit= getVisitsOnly(memberID, eventType);
                visits.addAll(visit);
            }
        } else{
            getVisitsOnly(memberID, Constants.Events.ANC_FIRST_FACILITY_VISIT);
        }

        int x = 0;
        while (visits.size() > x) {
            Visit visit = visits.get(x);
            List<VisitDetail> detailList = getVisitDetailsOnly(visit.getVisitId());
            visits.get(x).setVisitDetails(getVisitGroups(detailList));
            x++;
        }

        return visits;
    }
}
