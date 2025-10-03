package org.smartregister.chw.interactor;

import org.smartregister.chw.dao.AypInSchoolGroupsRegisterDao;
import org.smartregister.chw.domain.AypInSchoolGroupListItem;
import org.smartregister.family.util.AppExecutors;

import java.util.List;

public class AypOutSchoolGroupsRegisterInteractor {

    public interface Callback {
        void onData(List<AypInSchoolGroupListItem> items);
    }

    private final AppExecutors appExecutors = new AppExecutors();

    public void fetchItems(Callback callback) {
        Runnable runnable = () -> {
            List<AypInSchoolGroupListItem> items = AypInSchoolGroupsRegisterDao.getOutGroups();
            appExecutors.mainThread().execute(() -> callback.onData(items));
        };
        appExecutors.diskIO().execute(runnable);
    }

    public void fetchItemsByType(String groupType, Callback callback) {
        Runnable runnable = () -> {
            List<AypInSchoolGroupListItem> items = AypInSchoolGroupsRegisterDao.getAypOutSchoolGroupsByType(groupType);
            appExecutors.mainThread().execute(() -> callback.onData(items));
        };
        appExecutors.diskIO().execute(runnable);
    }
}
