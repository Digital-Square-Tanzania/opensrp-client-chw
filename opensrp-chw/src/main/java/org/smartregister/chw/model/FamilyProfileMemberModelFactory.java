package org.smartregister.chw.model;

import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.core.model.CoreFamilyProfileMemberModel;

public class FamilyProfileMemberModelFactory {
    public static CoreFamilyProfileMemberModel create() {
        if ("nacp".equalsIgnoreCase(BuildConfig.FLAVOR)) {
            try {
                Class<?> clazz = Class.forName("org.smartregister.chw.model.FamilyProfileMemberModelNacp");
                return (CoreFamilyProfileMemberModel) clazz.newInstance();
            } catch (Exception ignored) {
                // fall through to default
            }
        }
        return new FamilyProfileMemberModel();
    }
}
