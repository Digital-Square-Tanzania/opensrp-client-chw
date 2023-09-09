package org.smartregister.chw.model;

import org.smartregister.chw.anc.util.DBConstants;

import java.util.HashSet;
import java.util.Set;

public abstract class DefaultAncRegisterFragmentModelFlv implements AncRegisterFragmentModel.Flavor {
    @Override
    public Set<String> mainColumns(String tableName) {
        Set<String> columnList = new HashSet<>();
        columnList.add(tableName + "." + DBConstants.KEY.HAS_ANC_CARD);
        return columnList;
    }
}
