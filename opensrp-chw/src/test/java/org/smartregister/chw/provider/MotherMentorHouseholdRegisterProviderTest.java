package org.smartregister.chw.provider;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.R;
import org.smartregister.chw.mothermentor.provider.MotherMentorRegisterProvider.FooterViewHolder;
import org.smartregister.chw.mothermentor.provider.MotherMentorRegisterProvider.RegisterViewHolder;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Config(sdk = 28)
public class MotherMentorHouseholdRegisterProviderTest extends BaseUnitTest {
    private MotherMentorHouseholdRegisterProvider provider;
    private FooterViewHolder footer;
    private Context context;

    @Before
    public void setUp() {
        context = new ContextThemeWrapper(
                RuntimeEnvironment.getApplication(), R.style.Theme_AppCompat);
        provider = new MotherMentorHouseholdRegisterProvider(
                context, null, null, Collections.emptySet());
        footer = (FooterViewHolder) provider.createFooterHolder(new FrameLayout(context));
    }

    @Test
    @Config(qualifiers = "en")
    public void englishHouseholdRowUsesSharedTitle() {
        assertHouseholdRowTitle("Jaribio Family", "Household");
    }

    @Test
    @Config(qualifiers = "sw")
    public void swahiliHouseholdRowUsesSharedTitle() {
        assertHouseholdRowTitle("Kaya ya Jaribio", "Kaya");
    }

    private void assertHouseholdRowTitle(String expected, String fallback) {
        RegisterViewHolder holder = (RegisterViewHolder) provider.createViewHolder(new FrameLayout(context));
        Map<String, String> columns = new HashMap<>();
        columns.put(DBConstants.KEY.FIRST_NAME, "jaribio");
        columns.put(DBConstants.KEY.LAST_NAME, "Family");
        CommonPersonObjectClient client = new CommonPersonObjectClient("test-household", columns, "");
        client.setColumnmaps(columns);
        provider.getView(null, client, holder);
        assertEquals(expected, holder.patientName.getText().toString());
        assertEquals(expected, context.getString(R.string.family_profile_title, "Jaribio"));
        assertEquals(expected, context.getString(R.string.household_register_row_title, "Jaribio"));

        columns.put(DBConstants.KEY.FIRST_NAME, " ");
        provider.getView(null, client, holder);
        assertEquals(fallback, holder.patientName.getText().toString());
    }

    @Test
    @Config(qualifiers = "en")
    public void showsEmptyStateAndRestoresPaginationWhenResultsReturn() {
        provider.getFooterView(footer, 2, 3, true, true);
        assertEquals("Page 2 of 3", footer.pageInfoView.getText().toString());

        provider.getFooterView(footer, 1, 0, false, true);
        assertEquals("No households found", footer.pageInfoView.getText().toString());
        assertEquals(View.INVISIBLE, footer.previousPageView.getVisibility());
        assertEquals(View.INVISIBLE, footer.nextPageView.getVisibility());

        provider.getFooterView(footer, 2, 3, true, true);
        assertEquals("Page 2 of 3", footer.pageInfoView.getText().toString());
        assertEquals(View.VISIBLE, footer.previousPageView.getVisibility());
        assertEquals(View.VISIBLE, footer.nextPageView.getVisibility());
    }

    @Test
    @Config(qualifiers = "sw")
    public void showsLocalizedEmptyState() {
        provider.getFooterView(footer, 1, 0, false, false);
        assertEquals("Hakuna kaya zilizopatikana", footer.pageInfoView.getText().toString());
    }
}
