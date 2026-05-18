package org.smartregister.chw.presenter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.core.model.CoreHarmReductionRegisterFragmentModel;
import org.smartregister.chw.harmreduction.contract.HarmReductionRegisterFragmentContract;

public class HarmReductionMatClientsRegisterFragmentPresenterTest extends BaseUnitTest {

    private HarmReductionMatClientsRegisterFragmentPresenter presenter;

    @Mock
    private HarmReductionRegisterFragmentContract.View view;

    @Mock
    private CoreHarmReductionRegisterFragmentModel model;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        presenter = new HarmReductionMatClientsRegisterFragmentPresenter(view, model, "");
    }

    @Test
    public void testMainConditionFallsBackToFollowUpStatusWhenClientStartedMatIsBlank() {
        Assert.assertEquals(
                "ec_harm_reduction_risk_assessment.is_closed = 0 AND (ec_harm_reduction_risk_assessment.client_started_mat = 'yes' OR ec_harm_reduction_risk_assessment.follow_up_status = 'started_mat_services')",
                presenter.getMainCondition()
        );
    }
}
