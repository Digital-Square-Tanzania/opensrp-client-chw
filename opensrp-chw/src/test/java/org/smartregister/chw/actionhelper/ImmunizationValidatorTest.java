package org.smartregister.chw.actionhelper;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.anc.domain.VaccineDisplay;
import org.smartregister.chw.fragment.BaseHomeVisitImmunizationFragmentFlv;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.domain.jsonmapping.VaccineGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImmunizationValidatorTest extends BaseUnitTest {

    @Mock
    private BaseHomeVisitImmunizationFragmentFlv fragmentFlv;

    @Mock
    private VaccineGroup vaccineGroup;

    private final List<VaccineGroup> vaccinesGroups = new ArrayList<>();
    private final List<org.smartregister.immunization.domain.jsonmapping.Vaccine> specialVaccines = new ArrayList<>();

    private final List<Vaccine> vaccines = new ArrayList<>();

    private ImmunizationValidator validator;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        String vaccineCategory = "child";
        validator = new ImmunizationValidator(vaccinesGroups, specialVaccines, vaccineCategory, vaccines);
    }

    @Test
    public void testAddFragment() {
        DateTime anchorDate = new DateTime();

        String key = "sample";
        validator.addFragment(key, fragmentFlv, vaccineGroup, anchorDate);

        Map<String, BaseHomeVisitImmunizationFragmentFlv> fragments = ReflectionHelpers.getField(validator, "fragments");
        Assert.assertEquals(fragmentFlv, fragments.get(key));
    }

    @Test
    public void testIsValid() {
        DateTime anchorDate = new DateTime();

        String key = "sample";
        Map<String, VaccineDisplay> displayMap = new HashMap<>();
        displayMap.put("a", new VaccineDisplay());

        Mockito.doReturn(displayMap).when(fragmentFlv).getVaccineDisplays();
        validator.addFragment(key, fragmentFlv, vaccineGroup, anchorDate);
    }
}
