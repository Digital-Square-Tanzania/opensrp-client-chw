package org.smartregister.chw.activity;

import android.view.View;
import android.widget.TextView;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.harmreduction.dao.HarmReductionDao;
import org.smartregister.chw.harmreduction.domain.MemberObject;

public class HarmReductionSoberHouseProfileActivityTest extends BaseUnitTest {

    @Test
    public void applyHarmReductionSoberHouseDeceasedHandlingShouldShowStatusTagWhenClientIsDeceased() {
        HarmReductionSoberHouseProfileActivity activity = Mockito.mock(HarmReductionSoberHouseProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        MemberObject memberObject = Mockito.mock(MemberObject.class);
        TextView statusTag = Mockito.mock(TextView.class);
        Mockito.doReturn("base-id").when(memberObject).getBaseEntityId();
        ReflectionHelpers.setField(activity, "memberObject", memberObject);
        Mockito.doReturn(statusTag).when(activity).findViewById(org.smartregister.chw.harmreduction.R.id.family_tbleprosy_head);

        try (MockedStatic<HarmReductionDao> harmReductionDaoStatic = Mockito.mockStatic(HarmReductionDao.class)) {
            harmReductionDaoStatic.when(() -> HarmReductionDao.isSoberHouseClientDeceased("base-id")).thenReturn(true);

            activity.applyHarmReductionSoberHouseDeceasedHandling();

            Mockito.verify(statusTag).setText(org.smartregister.chw.R.string.harm_reduction_followup_visit_client_deceased);
            Mockito.verify(statusTag).setVisibility(View.VISIBLE);
        }
    }

    @Test
    public void applyHarmReductionSoberHouseDeceasedHandlingShouldHideStatusTagWhenClientIsNotDeceased() {
        HarmReductionSoberHouseProfileActivity activity = Mockito.mock(HarmReductionSoberHouseProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        MemberObject memberObject = Mockito.mock(MemberObject.class);
        TextView statusTag = Mockito.mock(TextView.class);
        Mockito.doReturn("base-id").when(memberObject).getBaseEntityId();
        ReflectionHelpers.setField(activity, "memberObject", memberObject);
        Mockito.doReturn(statusTag).when(activity).findViewById(org.smartregister.chw.harmreduction.R.id.family_tbleprosy_head);

        try (MockedStatic<HarmReductionDao> harmReductionDaoStatic = Mockito.mockStatic(HarmReductionDao.class)) {
            harmReductionDaoStatic.when(() -> HarmReductionDao.isSoberHouseClientDeceased("base-id")).thenReturn(false);

            activity.applyHarmReductionSoberHouseDeceasedHandling();

            Mockito.verify(statusTag).setVisibility(View.GONE);
            Mockito.verify(statusTag, Mockito.never()).setText(Mockito.anyInt());
        }
    }

    @Test
    public void setupButtonsShouldHideActionViewsWhenClientIsDeceased() {
        HarmReductionSoberHouseProfileActivity activity = Mockito.mock(HarmReductionSoberHouseProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        MemberObject memberObject = Mockito.mock(MemberObject.class);
        TextView soberHouseVisitButton = Mockito.mock(TextView.class);
        Mockito.doReturn("base-id").when(memberObject).getBaseEntityId();

        ReflectionHelpers.setField(activity, "memberObject", memberObject);
        ReflectionHelpers.setField(activity, "textViewRecordSoberHouseVisit", soberHouseVisitButton);

        try (MockedStatic<HarmReductionDao> harmReductionDaoStatic = Mockito.mockStatic(HarmReductionDao.class)) {
            harmReductionDaoStatic.when(() -> HarmReductionDao.isSoberHouseClientDeceased("base-id")).thenReturn(true);

            activity.setupButtons();

            Mockito.verify(soberHouseVisitButton).setVisibility(View.GONE);
            harmReductionDaoStatic.verify(() -> HarmReductionDao.isSoberHouseClientDeceased("base-id"));
        }
    }

    @Test
    public void setupButtonsShouldHideActionViewsWhenMemberObjectIsNull() {
        HarmReductionSoberHouseProfileActivity activity = Mockito.mock(HarmReductionSoberHouseProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        TextView soberHouseVisitButton = Mockito.mock(TextView.class);

        ReflectionHelpers.setField(activity, "memberObject", null);
        ReflectionHelpers.setField(activity, "textViewRecordSoberHouseVisit", soberHouseVisitButton);

        activity.setupButtons();

        Mockito.verify(soberHouseVisitButton).setVisibility(View.GONE);
    }

    @Test
    public void applyHarmReductionSoberHouseDeceasedHandlingShouldNotHideActionViewsWhenClientIsNotDeceased() {
        HarmReductionSoberHouseProfileActivity activity = Mockito.mock(HarmReductionSoberHouseProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        MemberObject memberObject = Mockito.mock(MemberObject.class);
        TextView soberHouseVisitButton = Mockito.mock(TextView.class);
        Mockito.doReturn("base-id").when(memberObject).getBaseEntityId();

        ReflectionHelpers.setField(activity, "memberObject", memberObject);
        ReflectionHelpers.setField(activity, "textViewRecordSoberHouseVisit", soberHouseVisitButton);

        try (MockedStatic<HarmReductionDao> harmReductionDaoStatic = Mockito.mockStatic(HarmReductionDao.class)) {
            harmReductionDaoStatic.when(() -> HarmReductionDao.isSoberHouseClientDeceased("base-id")).thenReturn(false);

            activity.applyHarmReductionSoberHouseDeceasedHandling();

            Mockito.verify(soberHouseVisitButton, Mockito.never()).setVisibility(View.GONE);
        }
    }
}
