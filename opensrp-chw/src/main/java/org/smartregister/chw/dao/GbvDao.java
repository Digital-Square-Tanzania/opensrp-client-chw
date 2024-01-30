package org.smartregister.chw.dao;

import org.smartregister.chw.domain.GbvRegistrationObject;
import org.smartregister.dao.AbstractDao;

import java.util.List;

public class GbvDao extends AbstractDao {
    public static GbvRegistrationObject getGbVRegistrationObject(String baseEntityId) {
        String sql = "select physical_violence, emotional_violence, sexual_violence, exploitation_violence FROM ec_gbv_register where base_entity_id ='" + baseEntityId + "' ";


        DataMap<GbvRegistrationObject> dataMap = cursor -> {
            GbvRegistrationObject gbvRegistrationObject = new GbvRegistrationObject();

            gbvRegistrationObject.setPhysicalViolence(getCursorValue(cursor, "physical_violence", null));
            gbvRegistrationObject.setEmotionalViolence(getCursorValue(cursor, "emotional_violence", null));
            gbvRegistrationObject.setSexualViolence(getCursorValue(cursor, "sexual_violence", null));
            gbvRegistrationObject.setExploitationViolence(getCursorValue(cursor, "exploitation_violence", null));


            return gbvRegistrationObject;
        };

        List<GbvRegistrationObject> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return null;

        return res.get(0);
    }
}
