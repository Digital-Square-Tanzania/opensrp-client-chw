package org.smartregister.chw.fragment;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Bundle;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.ayp.dao.AypDao;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.mothermentor.dao.MotherMentorPartnerFollowUpDao;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import java.util.Collections;
import java.util.Date;

public class AllClientsExpiredMotherMentorPartnerTest extends BaseUnitTest {
    @Test
    public void stalePartnerTagRefreshesItsExistingQueryInsteadOfOpeningAnExpiredProfile() {
        CommonPersonObjectClient client = mock(CommonPersonObjectClient.class);
        when(client.getCaseId()).thenReturn("partner");
        when(client.getDetails()).thenReturn(Collections.singletonMap(
                AllClientsRegisterFragment.REGISTER_TYPE,
                CoreConstants.REGISTER_TYPE.MOTHER_MENTOR_PARTNER));
        try (MockedStatic<MotherMentorPartnerFollowUpDao> dao =
                Mockito.mockStatic(MotherMentorPartnerFollowUpDao.class);
                MockedStatic<AypDao> ayp = Mockito.mockStatic(AypDao.class)) {
            dao.when(() -> MotherMentorPartnerFollowUpDao.getContext(eq("partner"), isNull(), any(Date.class)))
                    .thenReturn(new MotherMentorPartnerFollowUpDao.Context(
                            "old-link", "mother", "episode", "yes", false, false));
            StalePartnerFragment fragment = new StalePartnerFragment();
            fragment.goToClientDetailActivity(client);
            assertEquals(1, fragment.refreshes);
        }
    }

    private static class StalePartnerFragment extends AllClientsRegisterFragment {
        private int refreshes;

        @Override
        public Bundle countBundle() {
            return new Bundle();
        }

        @Override
        public void filterandSortExecute(Bundle args) {
            refreshes++;
        }
    }
}
