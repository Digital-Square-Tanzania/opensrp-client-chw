package org.smartregister.chw.dao;

import org.smartregister.dao.AbstractDao;

import java.util.List;

public class ChwIndexDao extends AbstractDao {
    public static String getIndexContactRegGuid(String baseEntityID) {
        String sql = String.format("SELECT rec_guid from ec_hiv_index\n" +
                "where base_entity_id = '%s'", baseEntityID);

        DataMap<String> dataMap = c -> getCursorValue(c, "rec_guid");

        List<String> values = AbstractDao.readData(sql, dataMap);
        if (values == null || values.isEmpty())
            return null;

        return values.get(0) == null ? null : values.get(0); // Return a default value of Low
    }


}
