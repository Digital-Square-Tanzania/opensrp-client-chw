package org.smartregister.chw.intent;

import org.smartregister.chw.anc.intent.HomeVisitIntentService;
import org.smartregister.chw.util.IccmVisitUtils;

import timber.log.Timber;

public class ChwHomeVisitIntentService extends HomeVisitIntentService {

    @Override
    protected void processVisits() throws Exception {
        // Process iCCM visit first while visits are still unprocessed by the global flow.
        try {
            IccmVisitUtils.processVisits();
        } catch (Exception e) {
            Timber.e(e);
        }

        super.processVisits();
    }
}
