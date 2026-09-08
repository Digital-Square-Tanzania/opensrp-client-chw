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
import org.smartregister.immunization.domain.jsonmapping.VaccineGroup;

import java.util.ArrayList;
import java.util.Date;
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
    private final List<org.smartregister.immunization.domain.Vaccine> vaccines = new ArrayList<>();

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

    @Test
    public void testConstructorNormalizesAdministeredVaccineNamesForScheduleMatching() {
        Date vaccineDate = new Date();
        org.smartregister.immunization.domain.Vaccine vaccine = new org.smartregister.immunization.domain.Vaccine();
        vaccine.setName("OPV 0");
        vaccine.setDate(vaccineDate);

        List<org.smartregister.immunization.domain.Vaccine> administered = new ArrayList<>();
        administered.add(vaccine);

        ImmunizationValidator immunizationValidator = new ImmunizationValidator(vaccinesGroups, specialVaccines, "child", administered);

        Map<String, Date> administeredVaccines = ReflectionHelpers.getField(immunizationValidator, "administeredVaccines");
        Assert.assertEquals(vaccineDate, administeredVaccines.get("opv0"));
        Assert.assertFalse(administeredVaccines.containsKey("OPV 0"));
    }
}
