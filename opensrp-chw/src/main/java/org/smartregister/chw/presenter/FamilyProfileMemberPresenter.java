package org.smartregister.chw.presenter;

import org.smartregister.chw.core.presenter.CoreFamilyProfileMemberPresenter;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.family.contract.FamilyProfileMemberContract;
import org.smartregister.family.util.DBConstants;

public class FamilyProfileMemberPresenter extends CoreFamilyProfileMemberPresenter {

    public FamilyProfileMemberPresenter(FamilyProfileMemberContract.View view, FamilyProfileMemberContract.Model model, String viewConfigurationIdentifier, String familyBaseEntityId, String familyHead, String primaryCaregiver) {
        super(view, model, viewConfigurationIdentifier, familyBaseEntityId, familyHead, primaryCaregiver);
    }

    @Override
    public String getDefaultSortQuery() {
        return CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.DOD + ", " + CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.DOB + " ASC ";
    }

    public String getMainCondition() {
        // Include: members linked to this family via relational_id OR the household head referenced by ec_family.family_head
        return String.format(
                " ( %1$s.%2$s = '%3$s' OR %1$s.%4$s = (SELECT family_head FROM ec_family WHERE base_entity_id = '%3$s') ) " +
                        " and ( %1$s.%5$s is null or %1$s.%6$s is not null ) " +
                        " and ( %7$s.%5$s is null or %7$s.%6$s is not null ) ",
                CoreConstants.TABLE_NAME.FAMILY_MEMBER,              // %1$s
                DBConstants.KEY.RELATIONAL_ID,                       // %2$s
                this.familyBaseEntityId,                              // %3$s
                DBConstants.KEY.BASE_ENTITY_ID,                      // %4$s
                DBConstants.KEY.DATE_REMOVED,                        // %5$s
                DBConstants.KEY.DOD,                                 // %6$s
                CoreConstants.TABLE_NAME.CHILD                        // %7$s
        );
    }

    public String getChildFilter() {
        return  " and (( ifnull(" + CoreConstants.TABLE_NAME.CHILD + "." + ChildDBConstants.KEY.ENTRY_POINT + ",'') <> 'PNC' ) or (ifnull(" + CoreConstants.TABLE_NAME.CHILD + "." + ChildDBConstants.KEY.ENTRY_POINT + ",'') = 'PNC' and ( date(" + CoreConstants.TABLE_NAME.CHILD + "." +  DBConstants.KEY.DOB + ", '+42 days') <= date() and ((SELECT is_closed FROM ec_family_member WHERE base_entity_id = " + CoreConstants.TABLE_NAME.CHILD + "." + ChildDBConstants.KEY.MOTHER_ENTITY_ID + " ) = 0)))  or (ifnull(ec_child.entry_point,'') = 'PNC'  and (SELECT is_closed FROM ec_family_member WHERE base_entity_id = ec_child.mother_entity_id ) = 1)) " ;
    }
}
