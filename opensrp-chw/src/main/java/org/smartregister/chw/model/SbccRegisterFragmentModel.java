package org.smartregister.chw.model;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.pmtct.model.BasePmtctRegisterFragmentModel;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;

import java.util.HashSet;
import java.util.Set;

public class SbccRegisterFragmentModel extends BasePmtctRegisterFragmentModel {

    @NonNull
    @Override
    public String mainSelect(@NonNull String tableName, @NonNull String mainCondition) {
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(tableName, mainColumns(tableName));
        return queryBuilder.mainCondition(mainCondition);
    }

    @Override
    @NotNull
    public String[] mainColumns(String tableName) {
        Set<String> columnList = new HashSet<>();

        return columnList.toArray(new String[columnList.size()]);
    }
}
