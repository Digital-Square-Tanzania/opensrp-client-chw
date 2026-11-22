package org.smartregister.chw.resources;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.chw.R;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MarpWordingTest {

    @Test
    public void englishUsesMarpWordingAcrossAppAndLibraryResources() {
        Context context = localizedContext(Locale.ENGLISH);

        assertEquals("MARP/PrEP", context.getString(R.string.menu_kvp_prep));
        assertEquals("MARP Clients", context.getString(R.string.kvp_clients));
        assertEquals("MARP/PrEP Enrollment",
                context.getString(org.smartregister.chw.core.R.string.kvp_prep_registration));
        assertEquals("MARP Screening",
                context.getString(org.smartregister.chw.core.R.string.kvp_registration));
        assertEquals("Return to all MARP Clients",
                context.getString(org.smartregister.chw.kvp.R.string.return_to_kvp_prep_clients));
        assertEquals("Dominant MARP Group: WHR",
                context.getString(org.smartregister.chw.kvp.R.string.dominant_kvp_group, "WHR"));
        assertEquals("Provide MARP Service",
                context.getString(org.smartregister.chw.kvp.R.string.record_kvp));
    }

    @Test
    public void swahiliUsesMarpWordingAcrossAppAndLibraryResources() {
        Context context = localizedContext(new Locale("sw"));

        assertEquals("MARP/PrEP", context.getString(R.string.menu_kvp_prep));
        assertEquals("Wateja wa MARP", context.getString(R.string.kvp_clients));
        assertEquals("Usajili wa MARP/PrEP",
                context.getString(org.smartregister.chw.core.R.string.kvp_prep_registration));
        assertEquals("Uchunguzi wa MARP",
                context.getString(org.smartregister.chw.core.R.string.kvp_registration));
        assertEquals("Rudi kwa wateja wote wa MARP",
                context.getString(org.smartregister.chw.kvp.R.string.return_to_kvp_prep_clients));
        assertEquals("Kundi kuu la MARP: WHR",
                context.getString(org.smartregister.chw.kvp.R.string.dominant_kvp_group, "WHR"));
        assertEquals("Toa huduma ya MARP",
                context.getString(org.smartregister.chw.kvp.R.string.record_kvp));
    }

    private Context localizedContext(Locale locale) {
        Context app = RuntimeEnvironment.getApplication();
        Configuration configuration = new Configuration(app.getResources().getConfiguration());
        configuration.setLocale(locale);
        return app.createConfigurationContext(configuration);
    }
}
