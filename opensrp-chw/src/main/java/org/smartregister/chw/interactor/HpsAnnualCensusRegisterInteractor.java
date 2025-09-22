package org.smartregister.chw.interactor;

import org.smartregister.chw.dao.HpsAnnualCensusRegisterDao;
import org.smartregister.chw.domain.HpsAnnualCensusListItem;
import org.smartregister.family.util.AppExecutors;

import java.util.List;

/**
 * Loads HPS Annual Census register items from visits/events.
 * Fetches latest HPS_ANNUAL_CENSUS event per baseEntityId and exposes year + entity id.
 */
public class HpsAnnualCensusRegisterInteractor {

    public interface Callback {
        void onData(List<HpsAnnualCensusListItem> items);
    }

    private final AppExecutors appExecutors = new AppExecutors();

    public void fetchItems(Callback callback) {
        Runnable runnable = () -> {
            List<HpsAnnualCensusListItem> items = HpsAnnualCensusRegisterDao.getLatestAnnualCensusItems();
            appExecutors.mainThread().execute(() -> callback.onData(items));
        };

        appExecutors.diskIO().execute(runnable);
    }
}
