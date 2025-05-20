package org.smartregister.chw.fragment;

import org.smartregister.chw.anc.contract.BaseAncHomeVisitContract;
import org.smartregister.chw.anc.domain.VaccineDisplay;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.chw.anc.util.NCUtils;

import java.util.List;
import java.util.Map;

public class BaseHomeVisitImmunizationFragmentFlv extends DefaultBaseHomeVisitImmunizationFragment {

    public static BaseHomeVisitImmunizationFragmentFlv getInstance(final BaseAncHomeVisitContract.VisitView view, String baseEntityID, Map<String, List<VisitDetail>> details, List<VaccineDisplay> vaccineDisplays) {
        return getInstance(view, baseEntityID, details, vaccineDisplays, true);
    }

    public static BaseHomeVisitImmunizationFragmentFlv getInstance(final BaseAncHomeVisitContract.VisitView view, String baseEntityID, Map<String, List<VisitDetail>> details, List<VaccineDisplay> vaccineDisplays, boolean defaultChecked) {
        BaseHomeVisitImmunizationFragmentFlv fragment = new BaseHomeVisitImmunizationFragmentFlv();
        fragment.visitView = view;
        fragment.baseEntityID = baseEntityID;
        fragment.details = details;
        fragment.vaccinesDefaultChecked = defaultChecked;
        for (VaccineDisplay vaccineDisplay : vaccineDisplays) {
            fragment.vaccineDisplays.put(vaccineDisplay.getVaccineWrapper().getName(), vaccineDisplay);
        }

        if (details != null && !details.isEmpty()) {
            fragment.jsonObject = NCUtils.getVisitJSONFromVisitDetails(view.getMyContext(), baseEntityID, details, vaccineDisplays);
            JsonFormUtils.populateForm(fragment.jsonObject, details);
        }

        return fragment;
    }
}
