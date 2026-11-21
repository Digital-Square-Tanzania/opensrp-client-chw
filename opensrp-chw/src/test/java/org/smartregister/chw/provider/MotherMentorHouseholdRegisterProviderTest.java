package org.smartregister.chw.provider;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.chw.R;
import org.smartregister.chw.mothermentor.provider.MotherMentorRegisterProvider.FooterViewHolder;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class MotherMentorHouseholdRegisterProviderTest {
    private MotherMentorHouseholdRegisterProvider provider;
    private FooterViewHolder footer;

    @Before
    public void setUp() {
        Context context = new ContextThemeWrapper(
                RuntimeEnvironment.getApplication(), R.style.Theme_AppCompat);
        provider = new MotherMentorHouseholdRegisterProvider(
                context, null, null, Collections.emptySet());
        footer = (FooterViewHolder) provider.createFooterHolder(new FrameLayout(context));
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
