package org.smartregister.chw.sync.helper;

import org.smartregister.CoreLibrary;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.domain.Task;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.repository.TaskRepository;
import org.smartregister.sync.helper.TaskServiceHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChwTaskServiceHelper extends TaskServiceHelper {
    private static ChwTaskServiceHelper instance;

    private ChwTaskServiceHelper(TaskRepository taskRepository) {
        super(taskRepository);
    }

    public static ChwTaskServiceHelper getInstance() {
        if (instance == null) {
            instance = new ChwTaskServiceHelper(CoreLibrary.getInstance().context().getTaskRepository());
        }
        return instance;
    }

    @Override
    protected List<String> getLocationIds() {
        LocationHelper locationHelper = LocationHelper.getInstance();
        ArrayList<String> locations = new ArrayList<>();
        if (locationHelper != null) {
            String defaultLocation = locationHelper.getDefaultLocation();
            List<String> locationsFromHierarchy = locationHelper.locationsFromHierarchy(true, defaultLocation);
            locations.addAll(locationsFromHierarchy);
        }
        return locations;
    }

    @Override
    protected Set<String> getPlanDefinitionIds() {
        Set<String> planDefinitionIds = new HashSet<>();
        planDefinitionIds.add(CoreConstants.REFERRAL_PLAN_ID);
        planDefinitionIds.add(CoreConstants.ADDO_LINKAGE_PLAN_ID);
        return planDefinitionIds;
    }

    @Override
    public List<Task> fetchTasksFromServer() {
        return super.fetchTasksFromServer();
    }
}
