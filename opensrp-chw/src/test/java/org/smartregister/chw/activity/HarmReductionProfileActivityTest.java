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

public class HarmReductionProfileActivityTest extends BaseUnitTest {

    @Test
    public void applyHarmReductionDeceasedHandlingShouldShowStatusTagWhenClientIsDeceased() {
        HarmReductionProfileActivity activity = Mockito.mock(HarmReductionProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        MemberObject memberObject = Mockito.mock(MemberObject.class);
        TextView statusTag = Mockito.mock(TextView.class);
        Mockito.doReturn("base-id").when(memberObject).getBaseEntityId();
        ReflectionHelpers.setField(activity, "memberObject", memberObject);
        Mockito.doReturn(statusTag).when(activity).findViewById(org.smartregister.chw.harmreduction.R.id.family_tbleprosy_head);

        try (MockedStatic<HarmReductionDao> harmReductionDaoStatic = Mockito.mockStatic(HarmReductionDao.class)) {
            harmReductionDaoStatic.when(() -> HarmReductionDao.isCommunityClientDeceased("base-id")).thenReturn(true);

            activity.applyHarmReductionDeceasedHandling();

            Mockito.verify(statusTag).setText(org.smartregister.chw.R.string.harm_reduction_followup_visit_client_deceased);
            Mockito.verify(statusTag).setVisibility(View.VISIBLE);
        }
    }

    @Test
    public void applyHarmReductionDeceasedHandlingShouldHideStatusTagWhenClientIsNotDeceased() {
        HarmReductionProfileActivity activity = Mockito.mock(HarmReductionProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        MemberObject memberObject = Mockito.mock(MemberObject.class);
        TextView statusTag = Mockito.mock(TextView.class);
        Mockito.doReturn("base-id").when(memberObject).getBaseEntityId();
        ReflectionHelpers.setField(activity, "memberObject", memberObject);
        Mockito.doReturn(statusTag).when(activity).findViewById(org.smartregister.chw.harmreduction.R.id.family_tbleprosy_head);

        try (MockedStatic<HarmReductionDao> harmReductionDaoStatic = Mockito.mockStatic(HarmReductionDao.class)) {
            harmReductionDaoStatic.when(() -> HarmReductionDao.isCommunityClientDeceased("base-id")).thenReturn(false);

            activity.applyHarmReductionDeceasedHandling();

            Mockito.verify(statusTag).setVisibility(View.GONE);
            Mockito.verify(statusTag, Mockito.never()).setText(Mockito.anyInt());
        }
    }

    @Test
    public void setupButtonsShouldHideActionViewsWhenClientIsDeceased() {
        HarmReductionProfileActivity activity = Mockito.mock(HarmReductionProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        MemberObject memberObject = Mockito.mock(MemberObject.class);
        TextView recordVisitButton = Mockito.mock(TextView.class);
        TextView markClientStartedMatButton = Mockito.mock(TextView.class);
        Mockito.doReturn("base-id").when(memberObject).getBaseEntityId();

        ReflectionHelpers.setField(activity, "memberObject", memberObject);
        ReflectionHelpers.setField(activity, "textViewRecordHarmReductionVisit", recordVisitButton);
        ReflectionHelpers.setField(activity, "textViewMarkClientStartedMat", markClientStartedMatButton);

        try (MockedStatic<HarmReductionDao> harmReductionDaoStatic = Mockito.mockStatic(HarmReductionDao.class)) {
            harmReductionDaoStatic.when(() -> HarmReductionDao.isCommunityClientDeceased("base-id")).thenReturn(true);

            activity.setupButtons();

            Mockito.verify(recordVisitButton).setVisibility(View.GONE);
            Mockito.verify(markClientStartedMatButton).setVisibility(View.GONE);
            harmReductionDaoStatic.verify(() -> HarmReductionDao.isCommunityClientDeceased("base-id"));
        }
    }

    @Test
    public void setupButtonsShouldHideActionViewsWhenMemberObjectIsNull() {
        HarmReductionProfileActivity activity = Mockito.mock(HarmReductionProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        TextView recordVisitButton = Mockito.mock(TextView.class);
        TextView markClientStartedMatButton = Mockito.mock(TextView.class);

        ReflectionHelpers.setField(activity, "memberObject", null);
        ReflectionHelpers.setField(activity, "textViewRecordHarmReductionVisit", recordVisitButton);
        ReflectionHelpers.setField(activity, "textViewMarkClientStartedMat", markClientStartedMatButton);

        activity.setupButtons();

        Mockito.verify(recordVisitButton).setVisibility(View.GONE);
        Mockito.verify(markClientStartedMatButton).setVisibility(View.GONE);
    }

    @Test
    public void applyHarmReductionDeceasedHandlingShouldNotHideActionViewsWhenClientIsNotDeceased() {
        HarmReductionProfileActivity activity = Mockito.mock(HarmReductionProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        MemberObject memberObject = Mockito.mock(MemberObject.class);
        TextView recordVisitButton = Mockito.mock(TextView.class);
        TextView markClientStartedMatButton = Mockito.mock(TextView.class);
        Mockito.doReturn("base-id").when(memberObject).getBaseEntityId();

        ReflectionHelpers.setField(activity, "memberObject", memberObject);
        ReflectionHelpers.setField(activity, "textViewRecordHarmReductionVisit", recordVisitButton);
        ReflectionHelpers.setField(activity, "textViewMarkClientStartedMat", markClientStartedMatButton);

        try (MockedStatic<HarmReductionDao> harmReductionDaoStatic = Mockito.mockStatic(HarmReductionDao.class)) {
            harmReductionDaoStatic.when(() -> HarmReductionDao.isCommunityClientDeceased("base-id")).thenReturn(false);

            activity.applyHarmReductionDeceasedHandling();

            Mockito.verify(recordVisitButton, Mockito.never()).setVisibility(View.GONE);
            Mockito.verify(markClientStartedMatButton, Mockito.never()).setVisibility(View.GONE);
        }
    }
}
