package org.smartregister.chw.util;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.repository.VisitRepository;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.mockito.Mockito.when;
import static org.smartregister.chw.core.utils.CoreConstants.EventType.ANC_HOME_VISIT;

public class VisitUtilsTest extends BaseUnitTest {

    private static final String BASE_ENTITY_ID = "base-entity-id";
    private static final String LMP_DATE = "01-01-2024";
    private static final String MEMBER_CREATED_AT = "2024-01-01T00:00:00.000+0000";
    private static final String ECD_VISIT_JSON = "{\"obs\":[{\"fieldCode\":\"partner_head_of_household\"}]}";
    private static final String NON_ECD_VISIT_JSON = "{\"obs\":[{\"fieldCode\":\"other_field\"}]}";

    private final SimpleDateFormat visitDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    private AncLibrary originalAncLibrary;
    private AncLibrary ancLibrary;
    private VisitRepository visitRepository;
    private MemberObject member;

    @Before
    public void setUp() throws Exception {
        ancLibrary = Mockito.mock(AncLibrary.class);
        visitRepository = Mockito.mock(VisitRepository.class);
        when(ancLibrary.visitRepository()).thenReturn(visitRepository);

        Field instanceField = AncLibrary.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        originalAncLibrary = (AncLibrary) instanceField.get(null);
        instanceField.set(null, ancLibrary);

        member = new MemberObject();
        member.setBaseEntityId(BASE_ENTITY_ID);
        member.setLastMenstrualPeriod(LMP_DATE);
        member.setDateCreated(MEMBER_CREATED_AT);
    }

    @After
    public void tearDown() throws Exception {
        Field instanceField = AncLibrary.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, originalAncLibrary);
    }

    @Test
    public void shouldInspectLatestVisitWhenNotInEditMode() throws Exception {
        stubVisits(Arrays.asList(
                visitOn("2024-02-15", NON_ECD_VISIT_JSON),
                visitOn("2024-03-01", ECD_VISIT_JSON)
        ));

        Assert.assertFalse(invokeIsVisitInRangeWithoutECD(0, 16, false));
    }

    @Test
    public void shouldIgnoreLatestVisitWhenInEditMode() throws Exception {
        stubVisits(Arrays.asList(
                visitOn("2024-02-15", NON_ECD_VISIT_JSON),
                visitOn("2024-03-01", ECD_VISIT_JSON)
        ));

        Assert.assertTrue(invokeIsVisitInRangeWithoutECD(0, 16, true));
    }

    @Test
    public void shouldReturnTrueWhenEditModeHasOnlyCurrentVisit() throws Exception {
        stubVisits(Collections.singletonList(visitOn("2024-03-01", ECD_VISIT_JSON)));

        Assert.assertTrue(invokeIsVisitInRangeWithoutECD(0, 16, true));
    }

    @Test
    public void shouldReturnFalseWhenSecondLastVisitContainsEcdInEditMode() throws Exception {
        stubVisits(Arrays.asList(
                visitOn("2024-02-15", ECD_VISIT_JSON),
                visitOn("2024-03-01", NON_ECD_VISIT_JSON)
        ));

        Assert.assertFalse(invokeIsVisitInRangeWithoutECD(0, 16, true));
    }

    private void stubVisits(List<Visit> visits) {
        when(visitRepository.getVisits(BASE_ENTITY_ID, ANC_HOME_VISIT)).thenReturn(visits);
    }

    private boolean invokeIsVisitInRangeWithoutECD(int minWeeks, int maxWeeks, boolean editMode) throws Exception {
        Method method = VisitUtils.class.getDeclaredMethod(
                "isVisitInRangeWithoutECD",
                MemberObject.class,
                int.class,
                int.class,
                boolean.class
        );
        method.setAccessible(true);
        return (Boolean) method.invoke(null, member, minWeeks, maxWeeks, editMode);
    }

    private Visit visitOn(String date, String json) throws Exception {
        Visit visit = new Visit();
        visit.setDate(parseDate(date));
        visit.setJson(json);
        return visit;
    }

    private Date parseDate(String date) throws Exception {
        return visitDateFormat.parse(date);
    }
}
