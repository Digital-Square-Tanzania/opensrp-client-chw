package org.smartregister.chw.presenter;

import org.junit.Test;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.opd.pojo.OpdEventClient;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MotherMentorChildEidRegisterPresenterTest {
    @Test public void newClientLinkUsesMemberIdInsteadOfNewHouseholdId() {
        OpdEventClient household = client(CoreConstants.EventType.FAMILY_REGISTRATION, "new-household");
        OpdEventClient person = client(CoreConstants.EventType.FAMILY_MEMBER_REGISTRATION, "new-person");
        assertEquals("new-person", MotherMentorChildEidRegisterPresenter.registeredMemberId(Arrays.asList(household, person)));
    }
    @Test public void incompleteRegistrationCannotLinkHouseholdAsChild() {
        assertNull(MotherMentorChildEidRegisterPresenter.registeredMemberId(Collections.singletonList(client(CoreConstants.EventType.FAMILY_REGISTRATION, "household"))));
        assertNull(MotherMentorChildEidRegisterPresenter.registeredMemberId(null));
    }
    @Test public void registrationEligibilityIsCheckedBeforePersistingClient() {
        OpdEventClient member = client(CoreConstants.EventType.FAMILY_MEMBER_REGISTRATION, "person");
        org.smartregister.clientandeventmodel.Client person = mock(org.smartregister.clientandeventmodel.Client.class);
        when(member.getClient()).thenReturn(person);
        when(person.getGender()).thenReturn("Male");
        when(person.getBirthdate()).thenReturn(org.joda.time.LocalDate.now().minusMonths(6).toDate());
        assertTrue(MotherMentorChildEidRegisterPresenter.eligibleRegisteredMember(Collections.singletonList(member)));
        when(person.getBirthdate()).thenReturn(org.joda.time.LocalDate.now().minusMonths(18).toDate());
        assertFalse(MotherMentorChildEidRegisterPresenter.eligibleRegisteredMember(Collections.singletonList(member)));
        when(person.getGender()).thenReturn("Female");
        when(person.getBirthdate()).thenReturn(org.joda.time.LocalDate.now().minusMonths(6).toDate());
        assertTrue(MotherMentorChildEidRegisterPresenter.eligibleRegisteredMember(Collections.singletonList(member)));
        when(person.getBirthdateApprox()).thenReturn(true);
        assertFalse(MotherMentorChildEidRegisterPresenter.eligibleRegisteredMember(Collections.singletonList(member)));
    }
    private OpdEventClient client(String type, String id) {
        OpdEventClient client = mock(OpdEventClient.class);
        Event event = mock(Event.class);
        when(client.getEvent()).thenReturn(event);
        when(event.getEventType()).thenReturn(type);
        when(event.getBaseEntityId()).thenReturn(id);
        return client;
    }
}
