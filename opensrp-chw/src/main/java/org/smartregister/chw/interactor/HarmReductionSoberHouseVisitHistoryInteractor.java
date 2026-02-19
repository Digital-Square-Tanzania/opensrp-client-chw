package org.smartregister.chw.interactor;

import static org.smartregister.chw.harmreduction.util.Constants.EVENT_TYPE.HARM_REDUCTION_SOBER_HOUSE_VISIT;

import android.content.Context;

import org.smartregister.chw.anc.contract.BaseAncMedicalHistoryContract;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.core.CoreBaseAncMedicalHistoryInteractor;
import org.smartregister.chw.domain.SortableVisit;

import java.util.ArrayList;
import java.util.List;

public class HarmReductionSoberHouseVisitHistoryInteractor extends CoreBaseAncMedicalHistoryInteractor {

    @Override
    public void getMemberHistory(final String memberID, final Context context, final BaseAncMedicalHistoryContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            String[] eventTypes = new String[]{HARM_REDUCTION_SOBER_HOUSE_VISIT};
            List<SortableVisit> visits = HarmReductionVisitHistoryInteractor.getVisits(memberID, eventTypes);
            final List<Visit> allVisits = new ArrayList<>(visits);
            appExecutors.mainThread().execute(() -> callBack.onDataFetched(allVisits));
        };

        appExecutors.diskIO().execute(runnable);
    }
}
