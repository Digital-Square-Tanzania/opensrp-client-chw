package org.smartregister.chw.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Application;
import android.content.Context;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import org.joda.time.DateTimeUtils;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.chw.R;
import org.smartregister.chw.model.MotherMentorClientRegisterModel;
import org.smartregister.chw.mothermentor.fragment.BaseMotherMentorRegisterFragment;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class MotherMentorClientRegisterProviderTest {
    private Context context;
    private MotherMentorClientRegisterProvider provider;
    private View.OnClickListener rowListener;

    @Before
    public void setUp() {
        DateTimeUtils.setCurrentMillisFixed(
                new LocalDate(2026, 10, 23).toDateTimeAtStartOfDay().plusHours(12).getMillis());
        context =
                new ContextThemeWrapper(
                        RuntimeEnvironment.getApplication(), R.style.Theme_AppCompat);
        rowListener = mock(View.OnClickListener.class);
        provider = new MotherMentorClientRegisterProvider(context, null, rowListener);
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    @Config(qualifiers = "en")
    public void englishRowBindsLocalizedStatusSexCtcAndCompletedVisits() {
        Map<String, String> columns = clientColumns("pregnant", "6");
        MotherMentorClientRegisterProvider.ClientViewHolder holder =
                bind(client("mother-en", columns));

        assertEquals("Halima Amina Suleiman, 32", holder.name.getText().toString());
        assertEquals("Pregnant · Female · Mikocheni", holder.metadata.getText().toString());
        assertEquals("CTC 12-34-567890-123456 · Visits 6", holder.ctcVisits.getText().toString());
        Spanned metadata = (Spanned) holder.metadata.getText();
        ForegroundColorSpan[] spans =
                metadata.getSpans(0, metadata.length(), ForegroundColorSpan.class);
        assertEquals(1, spans.length);
        assertEquals(0, metadata.getSpanStart(spans[0]));
        assertEquals("Pregnant".length(), metadata.getSpanEnd(spans[0]));
        assertEquals(
                ContextCompat.getColor(context, R.color.mama_kinara_blue),
                spans[0].getForegroundColor());
    }

    @Test
    @Config(qualifiers = "sw")
    public void swahiliRowUsesSwahiliStatusSexAndVisitLabel() {
        MotherMentorClientRegisterProvider.ClientViewHolder holder =
                bind(client("mother-sw", clientColumns("breast_feeding", "12")));

        assertEquals("Halima Amina Suleiman, 32", holder.name.getText().toString());
        assertEquals("Ananyonyesha · Mwanamke · Mikocheni", holder.metadata.getText().toString());
        assertEquals("CTC 12-34-567890-123456 · Ziara 12", holder.ctcVisits.getText().toString());
        assertEquals(
                "Halima Amina Suleiman, 32. Ananyonyesha · Mwanamke · Mikocheni. "
                        + "CTC 12-34-567890-123456 · Ziara 12",
                holder.itemView.getContentDescription().toString());
    }

    @Test
    @Config(qualifiers = "sw")
    public void notPregnantAndMaleLabelsAreLocalizedAndZeroCompletedVisitsIsValid() {
        Map<String, String> columns = clientColumns("not_pregnant", "0");
        columns.put(DBConstants.KEY.GENDER, " MALE ");
        MotherMentorClientRegisterProvider.ClientViewHolder holder =
                bind(client("mother-zero", columns));

        assertEquals("Si mjamzito · Mwanaume · Mikocheni", holder.metadata.getText().toString());
        assertEquals("CTC 12-34-567890-123456 · Ziara 0", holder.ctcVisits.getText().toString());
    }

    @Test
    @Config(qualifiers = "en")
    public void missingMalformedAndFutureBirthDatesNeverInventZeroAge() {
        for (String dob :
                Arrays.asList(null, "", "   ", "not-a-date", "2026-02-30", "2026-10-24")) {
            Map<String, String> columns = clientColumns("pregnant", "1");
            columns.put(DBConstants.KEY.DOB, dob);
            MotherMentorClientRegisterProvider.ClientViewHolder holder =
                    bind(client("mother-no-age", columns));

            assertEquals("DOB: " + dob, "Halima Amina Suleiman", holder.name.getText().toString());
            assertFalse(holder.name.getText().toString().contains(", 0"));
        }
    }

    @Test
    @Config(qualifiers = "en")
    public void missingIdentityAndUnknownStatusUseHonestEnglishFallbacks() {
        Map<String, String> columns = new HashMap<>();
        columns.put(DBConstants.KEY.FIRST_NAME, "   ");
        columns.put(DBConstants.KEY.CTC_NUMBER, "   ");
        columns.put(MotherMentorClientRegisterModel.CURRENT_STATUS, "legacy-unknown-status");
        MotherMentorClientRegisterProvider.ClientViewHolder holder =
                bind(client("unknown-en", columns));

        assertEquals("Unnamed client", holder.name.getText().toString());
        assertEquals("Status not recorded", holder.metadata.getText().toString());
        assertEquals("CTC — · Visits —", holder.ctcVisits.getText().toString());
        assertFalse(holder.metadata.getText().toString().contains("·"));
    }

    @Test
    @Config(qualifiers = "sw")
    public void missingStatusAndUnknownSexUseSwahiliFallbackWithoutRawCodes() {
        Map<String, String> columns = new HashMap<>();
        columns.put(DBConstants.KEY.GENDER, "not-recorded");
        MotherMentorClientRegisterProvider.ClientViewHolder holder =
                bind(client("unknown-sw", columns));

        assertEquals("Mteja asiye na jina", holder.name.getText().toString());
        assertEquals("Hali haijarekodiwa", holder.metadata.getText().toString());
        assertEquals("CTC — · Ziara —", holder.ctcVisits.getText().toString());
        assertFalse(holder.metadata.getText().toString().contains("not-recorded"));
    }

    @Test
    @Config(qualifiers = "en")
    public void invalidCompletedVisitCountsUseDashInsteadOfFalseZero() {
        for (String count : Arrays.asList(null, "", "NaN", "-1", "1.5")) {
            MotherMentorClientRegisterProvider.ClientViewHolder holder =
                    bind(client("invalid-count", clientColumns("pregnant", count)));

            assertEquals(
                    "Count: " + count,
                    "CTC 12-34-567890-123456 · Visits —",
                    holder.ctcVisits.getText().toString());
        }
    }

    @Test
    @Config(qualifiers = "en")
    public void recycledRowReplacesMetadataClickTagAndAccessibilityDescription() {
        MotherMentorClientRegisterProvider.ClientViewHolder holder =
                bind(client("first-mother", clientColumns("pregnant", "6")));
        Map<String, String> columns = new HashMap<>();
        columns.put(DBConstants.KEY.FIRST_NAME, "Nuru");
        columns.put(DBConstants.KEY.GENDER, "male");
        columns.put(MotherMentorClientRegisterModel.CURRENT_STATUS, "not_pregnant");
        columns.put(MotherMentorClientRegisterModel.COMPLETED_VISITS, "0");
        CommonPersonObjectClient replacement = client("replacement-mother", columns);

        provider.getView(null, replacement, holder);

        assertEquals("Nuru", holder.name.getText().toString());
        assertEquals("Not pregnant · Male", holder.metadata.getText().toString());
        assertEquals("CTC — · Visits 0", holder.ctcVisits.getText().toString());
        assertSame(replacement, holder.itemView.getTag());
        assertEquals(
                BaseMotherMentorRegisterFragment.CLICK_VIEW_NORMAL,
                holder.itemView.getTag(org.smartregister.chw.mothermentor.R.id.VIEW_ID));
        assertEquals(
                "Nuru. Not pregnant · Male. CTC — · Visits 0",
                holder.itemView.getContentDescription().toString());
        assertTrue(holder.itemView.performClick());
        verify(rowListener).onClick(holder.itemView);
        Spanned metadata = (Spanned) holder.metadata.getText();
        ForegroundColorSpan[] spans =
                metadata.getSpans(0, metadata.length(), ForegroundColorSpan.class);
        assertEquals(1, spans.length);
        assertEquals("Not pregnant".length(), metadata.getSpanEnd(spans[0]));
    }

    @Test
    public void rowIsSingleFocusableTapTargetAndChevronIsDecorative() {
        MotherMentorClientRegisterProvider.ClientViewHolder holder =
                bind(client("accessible-mother", clientColumns("pregnant", "6")));
        List<ImageView> images = new ArrayList<>();
        collectImages(holder.itemView, images);

        assertTrue(holder.itemView.isClickable());
        assertTrue(holder.itemView.isFocusable());
        assertEquals(1, images.size());
        ImageView chevron = images.get(0);
        assertFalse(chevron.isClickable());
        assertFalse(chevron.isFocusable());
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, chevron.getImportantForAccessibility());
        assertNull(chevron.getContentDescription());
        assertNotNull(chevron.getDrawable());
        assertTrue(holder.name.getMaxLines() > 1);
    }

    private MotherMentorClientRegisterProvider.ClientViewHolder bind(
            CommonPersonObjectClient client) {
        MotherMentorClientRegisterProvider.ClientViewHolder holder =
                provider.createViewHolder(new FrameLayout(context));
        provider.getView(null, client, holder);
        return holder;
    }

    private static Map<String, String> clientColumns(String status, String visits) {
        Map<String, String> columns = new HashMap<>();
        columns.put(DBConstants.KEY.FIRST_NAME, " Halima ");
        columns.put(DBConstants.KEY.MIDDLE_NAME, " Amina ");
        columns.put(DBConstants.KEY.LAST_NAME, " Suleiman ");
        columns.put(DBConstants.KEY.DOB, "1994-10-23");
        columns.put(DBConstants.KEY.GENDER, "female");
        columns.put(DBConstants.KEY.VILLAGE_TOWN, " Mikocheni ");
        columns.put(DBConstants.KEY.CTC_NUMBER, " 12-34-567890-123456 ");
        columns.put(MotherMentorClientRegisterModel.CURRENT_STATUS, status);
        columns.put(MotherMentorClientRegisterModel.COMPLETED_VISITS, visits);
        return columns;
    }

    private static CommonPersonObjectClient client(String id, Map<String, String> columns) {
        CommonPersonObjectClient client = new CommonPersonObjectClient(id, columns, "");
        client.setColumnmaps(columns);
        return client;
    }

    private static void collectImages(View view, List<ImageView> images) {
        if (view instanceof ImageView) {
            images.add((ImageView) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                collectImages(group.getChildAt(index), images);
            }
        }
    }
}
