package org.smartregister.chw.provider;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import android.app.Application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.R;
import org.smartregister.chw.core.utils.CoreConstants;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class MotherMentorAllClientsTagTest {
    private OpdRegisterProvider provider;

    @Before
    public void setUp() {
        provider = mock(OpdRegisterProvider.class, CALLS_REAL_METHODS);
        ReflectionHelpers.setField(provider, "context", RuntimeEnvironment.getApplication());
    }

    @Test
    @Config(qualifiers = "en")
    public void englishMotherMentorTagsRetainTheirExistingNames() {
        assertEquals("Mother Mentor", translated(CoreConstants.REGISTER_TYPE.MOTHER_MENTOR));
        assertEquals(
                "Mother Mentor Partner",
                translated(CoreConstants.REGISTER_TYPE.MOTHER_MENTOR_PARTNER));
        assertEquals(
                "Mother Mentor Child IED",
                translated(CoreConstants.REGISTER_TYPE.MOTHER_MENTOR_CHILD_EID));
    }

    @Test
    @Config(qualifiers = "sw")
    public void swahiliMotherMentorTagsUseTheirLocalizedNames() {
        assertEquals("Mama Kinara", translated(CoreConstants.REGISTER_TYPE.MOTHER_MENTOR));
        assertEquals(
                "Mama Kinara: Mwenza",
                translated(CoreConstants.REGISTER_TYPE.MOTHER_MENTOR_PARTNER));
        assertEquals(
                "Mtoto wa ufuatiliaji wa ukusanyaji wa DBS",
                translated(CoreConstants.REGISTER_TYPE.MOTHER_MENTOR_CHILD_EID));
    }

    @Test
    @Config(qualifiers = "sw")
    public void primaryTagTranslationAcceptsDifferentStoredCapitalization() {
        assertEquals("Mama Kinara", translated("MOTHER MENTOR"));
    }

    @Test
    public void unrelatedUnknownTagsKeepTheirStoredNames() {
        assertEquals("Other register", translated("Other register"));
    }

    @Test
    @Config(qualifiers = "sw")
    public void swahiliPartnerTitleAndHistoryUseMwenza() {
        Application application = RuntimeEnvironment.getApplication();
        assertEquals("MWENZA", application.getString(R.string.mothermentor_partner));
        assertEquals(
                "Tarehe mwenza aliyotakiwa kituoni",
                application.getString(
                        R.string.mothermentor_history_partner_expected_facility_visit_date));
    }

    private String translated(String registerType) {
        return ReflectionHelpers.callInstanceMethod(
                provider,
                "getTranslatedRegisterType",
                ReflectionHelpers.ClassParameter.from(String.class, registerType));
    }
}
