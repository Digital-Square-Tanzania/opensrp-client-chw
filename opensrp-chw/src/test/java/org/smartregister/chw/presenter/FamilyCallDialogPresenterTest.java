package org.smartregister.chw.presenter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.core.contract.FamilyCallDialogContract;
import org.smartregister.chw.core.interactor.FamilyCallDialogInteractor;
import org.smartregister.chw.core.model.FamilyCallDialogModel;
import org.smartregister.chw.core.presenter.FamilyCallDialogPresenter;

public class FamilyCallDialogPresenterTest {


    private FamilyCallDialogPresenter presenter;

    @Mock
    private FamilyCallDialogContract.View view;

    @Mock
    private FamilyCallDialogInteractor mInteractor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        String familyBaseEntityId = "adawrfvsr553";
        presenter = new FamilyCallDialogPresenter(view, familyBaseEntityId);
        ReflectionHelpers.setField(presenter, "mInteractor", mInteractor);
    }

    @Test
    public void testUpdateHeadOfFamily() {
        FamilyCallDialogContract.Model model = new FamilyCallDialogModel();
        presenter.updateHeadOfFamily(model);
        Mockito.verify(view).refreshHeadOfFamilyView(model);
    }

    @Test
    public void testUpdateCareGiver() {
        FamilyCallDialogContract.Model model = new FamilyCallDialogModel();
        presenter.updateHeadOfFamily(model);
        Mockito.verify(view).refreshHeadOfFamilyView(model);
    }

    @Test
    public void testInitalize() {
        // should have been called
        Mockito.verify(view).refreshHeadOfFamilyView(null);
        Mockito.verify(view).refreshCareGiverView(null);

        // second call for stubbed interactor
        presenter.initalize();
        Mockito.verify(mInteractor).getHeadOfFamily(presenter, view.getCurrentContext());
    }

}