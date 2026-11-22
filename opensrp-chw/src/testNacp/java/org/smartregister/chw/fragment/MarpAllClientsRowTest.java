package org.smartregister.chw.fragment;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Intent;
import android.database.Cursor;
import android.widget.FrameLayout;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.FragmentActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.KvpPrEPProfileActivity;
import org.smartregister.chw.ayp.dao.AypDao;
import org.smartregister.chw.configs.AllClientsRegisterRowOptions;
import org.smartregister.chw.dao.FamilyDao;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.provider.ChwAllClientsRegisterQueryProvider;
import org.smartregister.chw.provider.OpdRegisterProvider;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;
import org.smartregister.opd.OpdLibrary;
import org.smartregister.opd.configuration.OpdConfiguration;
import org.smartregister.opd.holders.OpdRegisterViewHolder;
import org.smartregister.opd.utils.OpdDbConstants;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MarpAllClientsRowTest {
    private static final String CLIENT_ID = "client-id";
    private static final String STORED_REGISTER_TYPE = "KVP/PrEP";

    @Test
    @Config(qualifiers = "en")
    public void englishRowShowsMarpAndOpensMarpProfile() {
        assertMarpRowAndProfileRoute();
    }

    @Test
    @Config(qualifiers = "sw")
    public void kiswahiliRowShowsMarpAndOpensMarpProfile() {
        assertMarpRowAndProfileRoute();
    }

    private void assertMarpRowAndProfileRoute() {
        Map<String, String> clientValues = new HashMap<>();
        clientValues.put(AllClientsRegisterFragment.REGISTER_TYPE, STORED_REGISTER_TYPE);
        clientValues.put(DBConstants.KEY.FIRST_NAME, "Fixture");
        clientValues.put(DBConstants.KEY.DOB, "1995-01-01");
        CommonPersonObjectClient client = new CommonPersonObjectClient(CLIENT_ID, clientValues, "");
        client.setColumnmaps(new HashMap<>(clientValues));

        ContextThemeWrapper context = new ContextThemeWrapper(
                RuntimeEnvironment.getApplication(), R.style.Theme_AppCompat);
        OpdRegisterViewHolder row;
        OpdLibrary library = mock(OpdLibrary.class);
        when(library.getOpdConfiguration()).thenReturn(
                new OpdConfiguration.Builder(ChwAllClientsRegisterQueryProvider.class)
                        .setOpdRegisterRowOptions(AllClientsRegisterRowOptions.class)
                        .build());
        CoreLibrary coreLibrary = mock(CoreLibrary.class, RETURNS_DEEP_STUBS);
        try (MockedStatic<OpdLibrary> opd = mockStatic(OpdLibrary.class);
                MockedStatic<CoreLibrary> core = mockStatic(CoreLibrary.class)) {
            opd.when(OpdLibrary::getInstance).thenReturn(library);
            core.when(CoreLibrary::getInstance).thenReturn(coreLibrary);
            OpdRegisterProvider provider = new OpdRegisterProvider(
                    context, view -> {}, view -> {});
            row = provider.createViewHolder(new FrameLayout(context));
            provider.getView(mock(Cursor.class), client, row);
        }

        assertEquals("MARP/PrEP", row.tvRegisterType.getText().toString());
        assertEquals(STORED_REGISTER_TYPE, client.getDetails().get(AllClientsRegisterFragment.REGISTER_TYPE));
        assertEquals(STORED_REGISTER_TYPE, client.getColumnmaps().get(OpdDbConstants.KEY.REGISTER_TYPE));

        FragmentActivity activity = mock(FragmentActivity.class);
        when(activity.getPackageName()).thenReturn(RuntimeEnvironment.getApplication().getPackageName());
        AllClientsRegisterFragment fragment = mock(AllClientsRegisterFragment.class, CALLS_REAL_METHODS);
        doReturn(activity).when(fragment).getActivity();
        try (MockedStatic<AypDao> ayp = mockStatic(AypDao.class);
                MockedStatic<FamilyDao> family = mockStatic(FamilyDao.class)) {
            fragment.goToClientDetailActivity(client);
        }
        ArgumentCaptor<Intent> startedActivity = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivity(startedActivity.capture());
        Intent intent = startedActivity.getValue();
        assertEquals(KvpPrEPProfileActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals(CLIENT_ID, intent.getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID));
        assertEquals(Constants.PROFILE_TYPES.KVP_PrEP_PROFILE,
                intent.getStringExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE));
    }
}
