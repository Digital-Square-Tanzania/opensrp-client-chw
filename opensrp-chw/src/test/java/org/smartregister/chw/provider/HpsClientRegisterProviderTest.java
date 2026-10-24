package org.smartregister.chw.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Application;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.FrameLayout;

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
import org.smartregister.chw.hps.fragment.BaseHpsRegisterFragment;
import org.smartregister.chw.hps.provider.HpsRegisterProvider;
import org.smartregister.chw.hps.util.DBConstants;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class HpsClientRegisterProviderTest {
    private Context context;
    private HpsClientRegisterProvider provider;
    private View.OnClickListener rowListener;
    private View.OnClickListener paginationListener;

    @Before
    public void setUp() {
        DateTimeUtils.setCurrentMillisFixed(
                new LocalDate(2026, 10, 26).toDateTimeAtStartOfDay().plusHours(12).getMillis());
        context =
                new ContextThemeWrapper(
                        RuntimeEnvironment.getApplication(), R.style.Theme_AppCompat);
        rowListener = mock(View.OnClickListener.class);
        paginationListener = mock(View.OnClickListener.class);
        provider =
                new HpsClientRegisterProvider(
                        context, paginationListener, rowListener, Collections.emptySet());
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    @Config(qualifiers = "en")
    public void englishRowShowsNameAgeAndColoredSexWithVillage() {
        HpsRegisterProvider.RegisterViewHolder holder = bind(client("client-en", columns()));

        assertEquals("Halima Amina Suleiman, 32", holder.patientName.getText().toString());
        assertEquals("Female", holder.textViewGender.getText().toString());
        assertEquals("Mikocheni", holder.textViewVillage.getText().toString());
        assertEquals(
                ContextCompat.getColor(context, R.color.mama_kinara_blue),
                holder.textViewGender.getCurrentTextColor());
        assertEquals(
                ContextCompat.getColor(context, R.color.mama_kinara_secondary),
                holder.textViewVillage.getCurrentTextColor());
        assertEquals(
                View.VISIBLE,
                holder.itemView.findViewById(R.id.icchw_client_metadata).getVisibility());
        assertEquals(
                View.VISIBLE,
                holder.itemView.findViewById(R.id.icchw_metadata_separator).getVisibility());
        assertEquals(
                "Halima Amina Suleiman, 32. Female. Mikocheni",
                holder.registerColumns.getContentDescription().toString());
        assertFalse(
                holder.registerColumns.getContentDescription().toString().contains("1234567890"));
    }

    @Test
    public void legacyNameAndVillageFormattingKeepTheirExistingCapitalization() {
        Map<String, String> columns = columns();
        columns.put(DBConstants.KEY.FIRST_NAME, "halima");
        columns.put(DBConstants.KEY.MIDDLE_NAME, "amina");
        columns.put(DBConstants.KEY.LAST_NAME, "suleiman");
        columns.put(DBConstants.KEY.VILLAGE_TOWN, "mikocheni");
        columns.put(DBConstants.KEY.UNIQUE_ID, "00abc123");
        HpsRegisterProvider.RegisterViewHolder holder = bind(client("client-legacy", columns));

        assertEquals("Halima Amina Suleiman, 32", holder.patientName.getText().toString());
        assertEquals("Mikocheni", holder.textViewVillage.getText().toString());
        assertFalse(holder.registerColumns.getContentDescription().toString().contains("00abc123"));
    }

    @Test
    @Config(qualifiers = "sw")
    public void swahiliRowLocalizesSexWithNameAndVillageOnly() {
        HpsRegisterProvider.RegisterViewHolder holder = bind(client("client-sw", columns()));

        assertEquals("Halima Amina Suleiman, 32", holder.patientName.getText().toString());
        assertEquals("Mwanamke", holder.textViewGender.getText().toString());
        assertEquals(
                "Halima Amina Suleiman, 32. Mwanamke. Mikocheni",
                holder.registerColumns.getContentDescription().toString());
    }

    @Test
    @Config(qualifiers = "sw")
    public void activeAncThenPncKeepPriorityOverSex() {
        Map<String, String> columns = columns();
        columns.put(DBConstants.KEY.IS_ANC_CLOSED, "0");
        columns.put(DBConstants.KEY.IS_PNC_CLOSED, "0");
        HpsRegisterProvider.RegisterViewHolder holder = bind(client("client-status", columns));
        assertEquals(
                context.getString(org.smartregister.chw.hps.R.string.anc_string),
                holder.textViewGender.getText().toString());

        columns.put(DBConstants.KEY.IS_ANC_CLOSED, "1");
        provider.getView(null, client("client-status", columns), holder);
        assertEquals(
                context.getString(org.smartregister.chw.hps.R.string.pnc_string),
                holder.textViewGender.getText().toString());

        columns.put(DBConstants.KEY.IS_PNC_CLOSED, "1");
        columns.put(DBConstants.KEY.GENDER, " MALE ");
        provider.getView(null, client("client-status", columns), holder);
        assertEquals("Mwanaume", holder.textViewGender.getText().toString());
    }

    @Test
    public void missingMalformedAndFutureDobPreserveNameWithoutInventedAge() {
        for (String dob : Arrays.asList(null, "", " ", "invalid", "2026-02-30", "2026-10-27")) {
            Map<String, String> columns = columns();
            columns.put(DBConstants.KEY.DOB, dob);
            HpsRegisterProvider.RegisterViewHolder holder = bind(client("client-no-dob", columns));

            assertEquals(
                    "DOB: " + dob,
                    "Halima Amina Suleiman",
                    holder.patientName.getText().toString());
            assertFalse(holder.patientName.getText().toString().contains(", 0"));
            assertTrue(holder.registerColumns.performClick());
            assertEquals(
                    BaseHpsRegisterFragment.CLICK_VIEW_NORMAL,
                    holder.registerColumns.getTag(org.smartregister.chw.hps.R.id.VIEW_ID));
        }
    }

    @Test
    public void isoTimestampBirthDateIsAccepted() {
        Map<String, String> columns = columns();
        columns.put(DBConstants.KEY.DOB, "1994-10-23T01:30:00.000+03:00");
        HpsRegisterProvider.RegisterViewHolder holder = bind(client("client-timestamp", columns));

        assertEquals("Halima Amina Suleiman, 32", holder.patientName.getText().toString());
    }

    @Test
    @Config(qualifiers = "en")
    public void missingIdentityUsesEnglishFallbackAndHidesEmptyMetadata() {
        HpsRegisterProvider.RegisterViewHolder holder =
                bind(client("client-empty", new HashMap<>()));

        assertEquals("Unnamed client", holder.patientName.getText().toString());
        assertEmptyMetadata(holder);
        assertEquals("Unnamed client", holder.registerColumns.getContentDescription().toString());
    }

    @Test
    @Config(qualifiers = "sw")
    public void unknownSexIsOmittedAndUnnamedClientIsLocalized() {
        Map<String, String> columns = new HashMap<>();
        columns.put(DBConstants.KEY.GENDER, "unknown-legacy-code");
        HpsRegisterProvider.RegisterViewHolder holder = bind(client("client-unknown-sw", columns));

        assertEquals("Mteja asiye na jina", holder.patientName.getText().toString());
        assertEmptyMetadata(holder);
        assertFalse(
                holder.registerColumns
                        .getContentDescription()
                        .toString()
                        .contains("unknown-legacy-code"));
    }

    @Test
    @Config(qualifiers = "en")
    public void singleMetadataValueDoesNotLeaveAnOrphanSeparator() {
        Map<String, String> columns = columns();
        columns.remove(DBConstants.KEY.VILLAGE_TOWN);
        HpsRegisterProvider.RegisterViewHolder holder = bind(client("client-sex-only", columns));
        assertEquals(View.VISIBLE, holder.textViewGender.getVisibility());
        assertEquals(View.GONE, holder.textViewVillage.getVisibility());
        assertEquals(
                View.GONE,
                holder.itemView.findViewById(R.id.icchw_metadata_separator).getVisibility());
        assertEquals(
                View.VISIBLE,
                holder.itemView.findViewById(R.id.icchw_client_metadata).getVisibility());

        columns.remove(DBConstants.KEY.GENDER);
        columns.put(DBConstants.KEY.VILLAGE_TOWN, "Mikocheni");
        provider.getView(null, client("client-village-only", columns), holder);
        assertEquals(View.GONE, holder.textViewGender.getVisibility());
        assertEquals(View.VISIBLE, holder.textViewVillage.getVisibility());
        assertEquals(
                View.GONE,
                holder.itemView.findViewById(R.id.icchw_metadata_separator).getVisibility());
    }

    @Test
    @Config(qualifiers = "en")
    public void recycledRowClearsOldValuesAndReplacesBothProfileTags() {
        HpsRegisterProvider.RegisterViewHolder holder = bind(client("first-client", columns()));
        Map<String, String> replacementColumns = new HashMap<>();
        replacementColumns.put(DBConstants.KEY.FIRST_NAME, "Nuru");
        CommonPersonObjectClient replacement = client("next-client", replacementColumns);
        provider.getView(null, replacement, holder);

        assertEquals("Nuru", holder.patientName.getText().toString());
        assertEmptyMetadata(holder);
        assertEquals("Nuru", holder.registerColumns.getContentDescription().toString());
        assertSame(replacement, holder.registerColumns.getTag());
        assertSame(replacement, holder.patientColumn.getTag());
        assertTrue(holder.registerColumns.performClick());
        assertTrue(holder.patientColumn.performClick());
        verify(rowListener).onClick(holder.registerColumns);
        verify(rowListener).onClick(holder.patientColumn);
    }

    @Test
    public void longNameAndVillageRemainAvailableWithoutAddingTheIdentifier() {
        Map<String, String> columns = columns();
        String surname = "Mwakalinga Mollel Mwakalinga Mollel";
        String village = "Mtaa wa Mikocheni Barabara ya Mbuyuni";
        String uniqueId = "123456789012345678901234567890";
        columns.put(DBConstants.KEY.LAST_NAME, surname);
        columns.put(DBConstants.KEY.VILLAGE_TOWN, village);
        columns.put(DBConstants.KEY.UNIQUE_ID, uniqueId);
        HpsRegisterProvider.RegisterViewHolder holder = bind(client("client-long", columns));

        assertEquals("Halima Amina " + surname + ", 32", holder.patientName.getText().toString());
        assertTrue(holder.patientName.getMaxLines() > 1);
        assertEquals(
                "Mtaa Wa Mikocheni Barabara Ya Mbuyuni",
                holder.textViewVillage.getText().toString());
        assertFalse(holder.registerColumns.getContentDescription().toString().contains(uniqueId));
    }

    @Test
    public void entireRowAndPatientColumnBothLaunchTheNormalClientProfile() {
        CommonPersonObjectClient person = client("profile-client", columns());
        HpsRegisterProvider.RegisterViewHolder holder = bind(person);

        for (View target : Arrays.asList(holder.registerColumns, holder.patientColumn)) {
            assertSame(person, target.getTag());
            assertEquals(
                    BaseHpsRegisterFragment.CLICK_VIEW_NORMAL,
                    target.getTag(org.smartregister.chw.hps.R.id.VIEW_ID));
            assertTrue(target.performClick());
            verify(rowListener).onClick(target);
        }
        assertNull(holder.dueButton);
        assertNull(holder.dueWrapper);
    }

    @Test
    public void inheritedFooterKeepsPageCountsVisibilityAndPaginationActions() {
        HpsRegisterProvider.FooterViewHolder footer =
                (HpsRegisterProvider.FooterViewHolder)
                        provider.createFooterHolder(new FrameLayout(context));
        assertTrue(provider.isFooterViewHolder(footer));
        provider.getFooterView(footer, 1, 3, true, false);

        assertEquals(
                MessageFormat.format(
                        context.getString(org.smartregister.R.string.str_page_info), 1, 3),
                footer.pageInfoView.getText().toString());
        assertEquals(View.VISIBLE, footer.nextPageView.getVisibility());
        assertEquals(View.INVISIBLE, footer.previousPageView.getVisibility());
        assertTrue(footer.nextPageView.performClick());
        verify(paginationListener).onClick(footer.nextPageView);

        provider.getFooterView(footer, 3, 3, false, true);
        assertEquals(View.INVISIBLE, footer.nextPageView.getVisibility());
        assertEquals(View.VISIBLE, footer.previousPageView.getVisibility());
        assertTrue(footer.previousPageView.performClick());
        verify(paginationListener).onClick(footer.previousPageView);
    }

    @Test
    public void configuredColumnsKeepTheExistingDefaultBindingGuard() {
        HpsClientRegisterProvider configuredProvider =
                new HpsClientRegisterProvider(
                        context,
                        paginationListener,
                        rowListener,
                        Collections.singleton(
                                mock(org.smartregister.configurableviews.model.View.class)));
        HpsRegisterProvider.RegisterViewHolder holder =
                configuredProvider.createViewHolder(new FrameLayout(context));
        holder.patientName.setText("Configured content");
        configuredProvider.getView(null, client("configured-client", columns()), holder);

        assertEquals("Configured content", holder.patientName.getText().toString());
        assertNull(holder.registerColumns.getTag());
    }

    private void assertEmptyMetadata(HpsRegisterProvider.RegisterViewHolder holder) {
        assertEquals("", holder.textViewGender.getText().toString());
        assertEquals("", holder.textViewVillage.getText().toString());
        assertEquals(View.GONE, holder.textViewGender.getVisibility());
        assertEquals(View.GONE, holder.textViewVillage.getVisibility());
        assertEquals(
                View.GONE,
                holder.itemView.findViewById(R.id.icchw_client_metadata).getVisibility());
        assertEquals(
                View.GONE,
                holder.itemView.findViewById(R.id.icchw_metadata_separator).getVisibility());
    }

    private HpsRegisterProvider.RegisterViewHolder bind(CommonPersonObjectClient person) {
        HpsRegisterProvider.RegisterViewHolder holder =
                provider.createViewHolder(new FrameLayout(context));
        provider.getView(null, person, holder);
        return holder;
    }

    private static Map<String, String> columns() {
        Map<String, String> columns = new HashMap<>();
        columns.put(DBConstants.KEY.FIRST_NAME, " Halima ");
        columns.put(DBConstants.KEY.MIDDLE_NAME, " Amina ");
        columns.put(DBConstants.KEY.LAST_NAME, " Suleiman ");
        columns.put(DBConstants.KEY.DOB, "1994-10-23");
        columns.put(DBConstants.KEY.GENDER, "female");
        columns.put(DBConstants.KEY.VILLAGE_TOWN, " Mikocheni ");
        columns.put(DBConstants.KEY.UNIQUE_ID, " 1234567890 ");
        return columns;
    }

    private static CommonPersonObjectClient client(String id, Map<String, String> columns) {
        CommonPersonObjectClient person = new CommonPersonObjectClient(id, columns, "");
        person.setColumnmaps(columns);
        return person;
    }
}
