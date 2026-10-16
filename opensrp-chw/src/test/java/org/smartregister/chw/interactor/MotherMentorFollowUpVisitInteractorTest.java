package org.smartregister.chw.interactor;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

public class MotherMentorFollowUpVisitInteractorTest {

    @Test
    public void getMemberClientFallsBackToSecondaryEnrollmentMember() {
        String baseEntityId = "secondary-client-id";
        MemberObject secondaryMember = new MemberObject();

        try (MockedStatic<MotherMentorLibrary> library = Mockito.mockStatic(MotherMentorLibrary.class);
             MockedStatic<MotherMentorDao> dao = Mockito.mockStatic(MotherMentorDao.class)) {
            MotherMentorLibrary motherMentorLibrary = mock(MotherMentorLibrary.class);
            library.when(MotherMentorLibrary::getInstance).thenReturn(motherMentorLibrary);
            stubApplicationContext(motherMentorLibrary);
            dao.when(() -> MotherMentorDao.getVisitMember(baseEntityId)).thenReturn(null);
            dao.when(() -> MotherMentorDao.getSecondaryEnrollmentMember(baseEntityId)).thenReturn(secondaryMember);

            MotherMentorFollowUpVisitInteractor interactor =
                    new MotherMentorFollowUpVisitInteractor("Mother Mentor Services");

            assertSame(secondaryMember, interactor.getMemberClient(baseEntityId, null));
        }
    }

    @Test
    public void getMemberClientKeepsPrimaryMemberLookup() {
        String baseEntityId = "primary-client-id";
        MemberObject primaryMember = new MemberObject();

        try (MockedStatic<MotherMentorLibrary> library = Mockito.mockStatic(MotherMentorLibrary.class);
             MockedStatic<MotherMentorDao> dao = Mockito.mockStatic(MotherMentorDao.class)) {
            MotherMentorLibrary motherMentorLibrary = mock(MotherMentorLibrary.class);
            library.when(MotherMentorLibrary::getInstance).thenReturn(motherMentorLibrary);
            stubApplicationContext(motherMentorLibrary);
            dao.when(() -> MotherMentorDao.getVisitMember(baseEntityId)).thenReturn(primaryMember);

            MotherMentorFollowUpVisitInteractor interactor =
                    new MotherMentorFollowUpVisitInteractor("Mother Mentor Services");

            assertSame(primaryMember, interactor.getMemberClient(baseEntityId, null));
            dao.verify(() -> MotherMentorDao.getSecondaryEnrollmentMember(baseEntityId), Mockito.never());
        }
    }

    private void stubApplicationContext(MotherMentorLibrary motherMentorLibrary) {
        org.smartregister.Context opensrpContext = mock(org.smartregister.Context.class);
        Mockito.when(motherMentorLibrary.context()).thenReturn(opensrpContext);
        Mockito.when(opensrpContext.applicationContext()).thenReturn(mock(android.content.Context.class));
    }
}
