package org.smartregister.chw.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChildUtilsTest extends BaseUnitTest {


    @Mock
    private ChildUtils.Flavor childUtilsFlv;

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        childUtilsFlv = Mockito.spy(ChildUtilsFlv.class);
    }

    @Test
    public void isFullyImmunizedForEmpty() {
        String[] list = {"OPV0".toLowerCase(), "BCG".toLowerCase(), "OPV1".toLowerCase(), "OPV2".toLowerCase()};
        List<String> receivedVaccine = Arrays.asList(list);
        Assert.assertEquals("", ChildUtils.isFullyImmunized(receivedVaccine));
    }

    @Test
    public void lowerCaseVaccineName() {
        Assert.assertEquals("MenA", ChildUtils.fixVaccineCasing("MENA"));
        Assert.assertEquals("Rubella 1", ChildUtils.fixVaccineCasing("RUBELLA 1"));
        Assert.assertEquals("Rubella 2", ChildUtils.fixVaccineCasing("RUBELLA 2"));
    }

    @Test
    public void threeTextAfterNewlineSplit() {
        String str = "Developmental warning signs:no" + "\n" + "Caregiver stimulation skills:no" + "\n" + "Early learning program:yes";
        String[] strings = ChildUtils.splitStringByNewline(str);
        List<String> list = Arrays.asList(strings);
        Assert.assertEquals(3, list.size());

    }

    @Test
    public void durationWithTwoDate() {
        CommonPersonObjectClient childClient = new CommonPersonObjectClient("", null, "");
        Map<String, String> map = new HashMap<>();
        map.put(DBConstants.KEY.DOB, "2019-03-01T03:00:00.000+03:00");
        childClient.setColumnmaps(map);
        String dateOfBirth = org.smartregister.family.util.Utils.getValue(childClient.getColumnmaps(), DBConstants.KEY.DOB, false);
        Date date1 = Utils.dobStringToDate(dateOfBirth);
        Date date2 = Utils.dobStringToDate("2019-06-01T03:00:00.000+03:00");
        String str2 = ChildUtils.getDurationFromTwoDate(date1, date2);
        Assert.assertEquals("13w 1d", str2);
    }

    /*
    @Test
    public void daysAway_awayTest(){
        //2019-04-08
        //20 days away
        SpannableString spannableString = ChildUtils.daysAway("2019-04-08");
        ForegroundColorSpan[] colorSpans =spannableString.getSpans(0, spannableString.length(), ForegroundColorSpan.class);
        Assert.assertTrue(colorSpans[0].getForegroundColor() == Color.GRAY);
    }
    @Test
    public void daysAway_overdueTest(){
        //2015-03-20
        //1460 days overdue color code= -1030586
        SpannableString spannableString = ChildUtils.daysAway("2015-03-20");
        ForegroundColorSpan[] colorSpans =spannableString.getSpans(0, spannableString.length(), ForegroundColorSpan.class);
        Assert.assertTrue(colorSpans[0].getForegroundColor() == -1030586);
    }
*/
}
