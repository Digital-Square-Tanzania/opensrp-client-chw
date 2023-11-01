package org.smartregister.chw.presenter;

import static org.smartregister.chw.util.Utils.getClientName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.smartregister.chw.activity.AllClientsMemberProfileActivity;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.core.activity.CoreAllClientsMemberProfileActivity;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.presenter.CoreAllClientsMemberPresenter;
import org.smartregister.chw.dao.FamilyDao;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;

import java.text.MessageFormat;

public class AllClientsMemberPresenter extends CoreAllClientsMemberPresenter {
    public AllClientsMemberPresenter(CoreAllClientsMemberProfileActivity allClientsMemberProfileActivity, String baseEntityId) {
        super(allClientsMemberProfileActivity, baseEntityId);
    }

    @Override
    public void refreshProfileTopSection(CommonPersonObjectClient client) {
        if (client != null && client.getColumnmaps() != null) {
            String firstName = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true);
            String middleName = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.MIDDLE_NAME, true);
            String lastName = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.LAST_NAME, true);

            String dob = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.DOB, true);
            int age = StringUtils.isNotBlank(dob) ? Utils.getAgeFromDate(dob) : 0;

            AllClientsMemberProfileActivity currentView = (AllClientsMemberProfileActivity) getView();
            currentView.setProfileName(MessageFormat.format("{0}, {1}", getClientName(firstName, middleName, lastName), age));
            String gestationAge = CoreChwApplication.ancRegisterRepository().getGaIfAncWoman(client.getCaseId());
            if (gestationAge != null) {
                currentView.setProfileDetailOne(NCUtils.gestationAgeString(gestationAge, currentView.getContext(), true));
            }
            String gender = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.GENDER, true);
            currentView.setProfileDetailOne(gender);
            String villageTown = FamilyDao.getFamilyDetail(client.getCaseId()).getVillageTown();
            currentView.setProfileDetailTwo(villageTown);
            String uniqueId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.UNIQUE_ID, false);
            currentView.setProfileDetailThree(String.format(currentView.getString(org.smartregister.chw.core.R.string.id_with_value), uniqueId));
            String entityType = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.ENTITY_TYPE, false);
            currentView.setProfileImage(client.getCaseId(), entityType);
        }
    }

    @Override
    public void onUniqueIdFetched(Triple<String, String, String> triple, String entityId) {
        //Overridden: Not Needed
    }

    @Override
    public void onNoUniqueId() {
        //Overridden: Not Needed
    }
}
