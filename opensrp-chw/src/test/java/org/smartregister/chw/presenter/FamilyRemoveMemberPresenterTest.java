package org.smartregister.chw.presenter;


import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.core.contract.FamilyRemoveMemberContract;
import org.smartregister.chw.interactor.FamilyRemoveMemberInteractor;
import org.smartregister.chw.util.Constants;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;

import java.util.HashMap;
import java.util.Map;

public class FamilyRemoveMemberPresenterTest {


    private FamilyRemoveMemberPresenter presenter;

    @Mock
    private FamilyRemoveMemberContract.View view;

    @Mock
    private FamilyRemoveMemberContract.Model model;

    @Mock
    private FamilyRemoveMemberInteractor interactor;

    private String familyBaseEntityId = "familyBaseEntityId";
    private String familyHead = "familyHead";
    private String primaryCaregiver = "primaryCaregiver";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        String viewConfigurationIdentifier = "viewConfigurationIdentifier";
        presenter = new FamilyRemoveMemberPresenter(view, model, viewConfigurationIdentifier, familyBaseEntityId, familyHead, primaryCaregiver);
        ReflectionHelpers.setField(presenter, "interactor", interactor);
    }

    @Test
    public void testRemoveMember() {

        CommonPersonObjectClient client = new CommonPersonObjectClient(null, null, null);

        // remove member when is not a primary care giver
        client.setColumnmaps(new HashMap<>());
        client.getColumnmaps().put(DBConstants.KEY.BASE_ENTITY_ID, "memberID");
        presenter.removeMember(client);

        Mockito.verify(model).prepareJsonForm(client, model.getForm(client));


        // remove member when is a primary care giver start fetch members for the change member dialog
        client.setColumnmaps(new HashMap<>());
        client.getColumnmaps().put(DBConstants.KEY.BASE_ENTITY_ID, primaryCaregiver);
        presenter.removeMember(client);

        Mockito.verify(interactor).processFamilyMember(familyBaseEntityId, client, presenter);
    }


    @Test
    public void testProcessMember() {
        Map<String, String> familyDetails = new HashMap<>();
        familyDetails.put(Constants.RELATIONSHIP.FAMILY_HEAD, familyHead);
        familyDetails.put(Constants.RELATIONSHIP.PRIMARY_CAREGIVER, primaryCaregiver);

        CommonPersonObjectClient client = new CommonPersonObjectClient(null, null, null);

        // when the member is family head open change head dialog
        client.setColumnmaps(new HashMap<>());
        client.getColumnmaps().put(DBConstants.KEY.BASE_ENTITY_ID, familyHead);

        presenter.processMember(familyDetails, client);
        Mockito.verify(view).displayChangeFamilyHeadDialog(client, familyHead);

        // when the member is care giver open change care giver
        client.setColumnmaps(new HashMap<String, String>());
        client.getColumnmaps().put(DBConstants.KEY.BASE_ENTITY_ID, primaryCaregiver);

        presenter.processMember(familyDetails, client);
        Mockito.verify(view).displayChangeCareGiverDialog(client, primaryCaregiver);

        // when the member has no role open form
        client.setColumnmaps(new HashMap<String, String>());
        client.getColumnmaps().put(DBConstants.KEY.BASE_ENTITY_ID, "memberID");
        presenter.removeMember(client);

        Mockito.verify(model).prepareJsonForm(client, model.getForm(client));
    }


    @Test
    public void testRemoveEveryone() {
        String familyName = "familyName";
        String details = "details";
        JSONObject form = Mockito.mock(JSONObject.class);

        ReflectionHelpers.setField(presenter, "familyBaseEntityId", familyBaseEntityId);
        Mockito.when(model.prepareFamilyRemovalForm(familyBaseEntityId, familyName, details)).thenReturn(form);

        presenter.removeEveryone(familyName, details);

        Mockito.verify(model).prepareFamilyRemovalForm(familyBaseEntityId, familyName, details);
        Mockito.verify(view).startJsonActivity(form);

    }

    @Test
    public void testOnFamilyRemoved() {
        Boolean success = true;

        presenter.onFamilyRemoved(success);

        Mockito.verify(view).onEveryoneRemoved();
    }

}
