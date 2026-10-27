package org.smartregister.chw.due;

/** Adapt existing persisted module data; implementations run on a worker thread. */
public interface ModuleDueServiceProvider {
    String getModuleId();

    ModuleDueResult load(DueQuery query);
}
