package org.smartregister.chw.model;

import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.repository.AllSharedPreferences;

import timber.log.Timber;

public class FamilyKitModel {
    private String familyId;

    public FamilyKitModel(String familyId) {
        this.familyId = familyId;
    }

    public boolean saveFamilyKitEvent(String jsonString) {
        try {
            AllSharedPreferences allSharedPreferences = AncLibrary.getInstance().context().allSharedPreferences();
            Event baseEvent = JsonFormUtils.processJsonForm(allSharedPreferences, jsonString, CoreConstants.TABLE_NAME.FAMILY_KIT_LOG);
            baseEvent.setBaseEntityId(familyId);

            NCUtils.addEvent(allSharedPreferences, baseEvent);
            NCUtils.startClientProcessing();
            return true;
        } catch (Exception e) {
            Timber.e(e);
        }
        return false;
    }

}
