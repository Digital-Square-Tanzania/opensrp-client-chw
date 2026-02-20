package org.smartregister.chw.model;

import org.smartregister.chw.core.model.CoreFamilyProfileMemberModel;

public class FamilyProfileMemberModelFactory {
    public static CoreFamilyProfileMemberModel create() {
        return new FamilyProfileMemberModelNacp();
    }
}

