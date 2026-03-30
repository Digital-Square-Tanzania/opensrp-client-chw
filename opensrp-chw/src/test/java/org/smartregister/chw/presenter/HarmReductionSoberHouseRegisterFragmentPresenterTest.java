package org.smartregister.chw.presenter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.harmreduction.contract.HarmReductionRegisterFragmentContract;

public class HarmReductionSoberHouseRegisterFragmentPresenterTest extends BaseUnitTest {

    private HarmReductionSoberHouseRegisterFragmentPresenter presenter;

    @Mock
    private HarmReductionRegisterFragmentContract.View view;

    @Mock
    private HarmReductionRegisterFragmentContract.Model model;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        presenter = new HarmReductionSoberHouseRegisterFragmentPresenter(view, model, "");
    }

    @Test
    public void testMainConditionRequiresDetoxification() {
        Assert.assertEquals(
                "ec_harm_reduction_sober_house_enrollment.is_closed = 0 AND ec_harm_reduction_sober_house_enrollment.detoxification_done = 'yes'",
                presenter.getMainCondition()
        );
    }
}
