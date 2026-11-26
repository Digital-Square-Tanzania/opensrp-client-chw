package org.smartregister.chw.fragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.*;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.model.MotherMentorRegisterFragmentModel;
import org.smartregister.chw.mothermentor.contract.MotherMentorRegisterFragmentContract;
import org.smartregister.chw.mothermentor.repository.MotherMentorPartnerClosureRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorPartnerClosure;
import org.smartregister.chw.presenter.MotherMentorSecondaryEnrollmentRegisterFragmentPresenter;
import org.smartregister.chw.provider.MotherMentorFollowUpRegisterProvider;
import org.smartregister.chw.repository.ChwRepository;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.cursoradapter.RecyclerViewProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorClosedPartnerRegisterTest {
    private static final String PARTNERS = Constants.TABLES.MOTHERMENTOR_ENROLL_PARTNER;
    private static final String FOLLOWUPS = Constants.TABLES.MOTHERMENTOR_FOLLOWUP_VISIT;
    private static final String PARTNER = "synthetic-partner";
    private static final String CONTROL = "synthetic-active";
    private SQLiteDatabase database;
    private net.zetetic.database.sqlcipher.SQLiteDatabase cipher;
    private MockedStatic<CoreChwApplication> application;

    @Before
    public void setUp() {
        database = SQLiteDatabase.create(null);
        cipher = mock(net.zetetic.database.sqlcipher.SQLiteDatabase.class);
        when(cipher.rawQuery(anyString(), any(String[].class)))
                .thenAnswer(call -> database.rawQuery(call.getArgument(0), call.getArgument(1)));
        ChwApplication app = mock(ChwApplication.class);
        ChwRepository repository = mock(ChwRepository.class);
        application = mockStatic(CoreChwApplication.class);
        application.when(CoreChwApplication::getInstance).thenReturn(app);
        when(app.getRepository()).thenReturn(repository);
        when(repository.getReadableDatabase()).thenReturn(cipher);

        create("ec_family_member", BASE_ENTITY_ID, RELATIONAL_ID, FIRST_NAME, MIDDLE_NAME, LAST_NAME,
                DOB, GENDER, UNIQUE_ID, PHONE_NUMBER, OTHER_PHONE_NUMBER, DOD);
        create("ec_family", BASE_ENTITY_ID, PRIMARY_CARE_GIVER, FAMILY_HEAD, VILLAGE_TOWN, FIRST_NAME);
        database.execSQL("CREATE TABLE ec_anc_register (base_entity_id TEXT, is_closed INTEGER, edd TEXT)");
        create(PARTNERS, "id", BASE_ENTITY_ID, PARTNER_LINKAGE_ID, LAST_INTERACTED_WITH,
                PARTNER_NEXT_FOLLOW_UP_PHASE, MWENZI_STATUS, MWENZI_TAREHE_YA_KUFUNGWA,
                MWENZI_SABABU_YA_KUFUNGWA);
        database.execSQL("ALTER TABLE " + PARTNERS + " ADD COLUMN is_closed INTEGER DEFAULT 0");
        create(FOLLOWUPS, BASE_ENTITY_ID, ENTITY_ID, FOLLOW_UP_CATEGORY, FOLLOW_UP_STATUS,
                PARTNER_FOLLOW_UP_SCHEMA, PARTNER_FOLLOW_UP_PHASE, PARTNER_LINKAGE_ID,
                MOTHERMENTOR_VISIT_DATE, PARTNER_FOLLOW_UP_END, PARTNER_NOT_FOUND_REASON,
                PARTNER_ARRIVED_AT_FACILITY, PARTNER_FACILITY_ARRIVAL_DATE, PARTNER_EDUCATION_TOPICS,
                PARTNER_EDUCATION_PROVIDED, PARTNER_HIV_TESTED, PARTNER_HIV_RESULT,
                PARTNER_HIV_NOT_TESTED_REASON, PARTNER_HIV_NOT_TESTED_REASON_OTHER,
                PARTNER_SYPHILIS_TESTED, PARTNER_SYPHILIS_RESULT, PARTNER_SYPHILIS_NOT_TESTED_REASON,
                PARTNER_SYPHILIS_NOT_TESTED_REASON_OTHER, PARTNER_HEPATITIS_B_TESTED, PARTNER_HEPATITIS_B_RESULT,
                PARTNER_HEPATITIS_B_NOT_TESTED_REASON, PARTNER_HEPATITIS_B_NOT_TESTED_REASON_OTHER, MWENZI_STATUS);
        create("visits", VISIT_ID, BASE_ENTITY_ID, FORM_SUBMISSION_ID, VISIT_TYPE, VISIT_DATE, VISIT_JSON);
        database.execSQL("ALTER TABLE visits ADD COLUMN processed INTEGER");
        database.execSQL("CREATE INDEX visits_partner ON visits (base_entity_id)");

        insert("ec_family", BASE_ENTITY_ID, "family", VILLAGE_TOWN, "Mapera");
        partner(PARTNER, "Juma", "Jaribio", "1001", "1");
        partner(CONTROL, "Baraka", "Jaribio", "1002", "2");
    }

    @After
    public void tearDown() {
        application.close();
        database.close();
    }

    @Test
    @Config(qualifiers = "en")
    public void closedPartnerAppearsOnceOnClosureDayWithAStaleSummary() throws Exception {
        assertClosedRegister(0, "Attended the facility", true);
    }

    @Test
    @Config(qualifiers = "sw")
    public void closedPartnerAppearsOnceNextDayWithoutAnIndexedFollowUpRow() throws Exception {
        assertClosedRegister(1, "Amehudhuria kituoni", false);
    }

    @Test
    @Config(qualifiers = "en")
    public void englishClosedPartnerAppearsOnceNextDayWithoutAnIndexedFollowUpRow() throws Exception {
        assertClosedRegister(1, "Attended the facility", false);
    }

    @Test
    @Config(qualifiers = "sw")
    public void kiswahiliClosedPartnerAppearsOnceOnClosureDayWithAStaleSummary() throws Exception {
        assertClosedRegister(0, "Amehudhuria kituoni", true);
    }

    private void assertClosedRegister(int daysSinceClosing, String label, boolean indexed) throws Exception {
        LocalDate closureDay = LocalDate.now().minusDays(daysSinceClosing);
        LocalDate attendanceDay = closureDay.minusDays(1);
        visit(PARTNER, "earlier", closureDay.minusDays(2), true, "referral", "tracing",
                FOLLOW_UP_STATUS, "found", PARTNER_FACILITY_AGREEMENT, "agreed",
                PARTNER_TESTING_REFERRAL_GIVEN, "yes", PARTNER_EDUCATION_PROVIDED, "no");
        visit(PARTNER, "middle", attendanceDay, true, "referral", "referral",
                PARTNER_TESTING_REFERRAL_GIVEN, "yes");
        visit(PARTNER, "closed", closureDay, true, "completed", "attendance",
                FOLLOW_UP_STATUS, "found", PARTNER_ARRIVED_AT_FACILITY, "yes",
                PARTNER_FACILITY_ARRIVAL_DATE, attendanceDay.toString("dd-MM-yyyy"));
        if (indexed) {
            insert(FOLLOWUPS, BASE_ENTITY_ID, "form-closed", ENTITY_ID, PARTNER, FOLLOW_UP_CATEGORY,
                    "PARTNER", PARTNER_LINKAGE_ID, PARTNER, PARTNER_FOLLOW_UP_SCHEMA, "3",
                    PARTNER_FOLLOW_UP_PHASE, "attendance", PARTNER_FOLLOW_UP_END, "completed",
                    MOTHERMENTOR_VISIT_DATE, closureDay.toString(), PARTNER_ARRIVED_AT_FACILITY, "yes",
                    PARTNER_FACILITY_ARRIVAL_DATE, attendanceDay.toString());
        } else {
            database.execSQL("UPDATE " + PARTNERS + " SET " + MWENZI_STATUS
                    + " = '' WHERE " + BASE_ENTITY_ID + " = ?", new String[]{PARTNER});
        }
        visit(CONTROL, "open", attendanceDay, true, "referral", "referral",
                PARTNER_TESTING_REFERRAL_GIVEN, "yes");
        visit(CONTROL, "draft", closureDay, false, "completed", "attendance",
                PARTNER_ARRIVED_AT_FACILITY, "yes", PARTNER_FACILITY_ARRIVAL_DATE, attendanceDay.toString());

        assertProfileClosure(closureDay);
        List<String> historyBefore = savedHistory();
        assertEquals(3, historyBefore.size());
        assertTrue(historyBefore.get(0).startsWith("closed:"));
        assertTrue(historyBefore.get(1).startsWith("middle:"));
        assertTrue(historyBefore.get(2).startsWith("earlier:"));
        assertTrue(historyBefore.get(0).contains(attendanceDay.toString("dd-MM-yyyy")));

        for (String term : new String[]{"Juma", "Jaribio"}) {
            MotherMentorSecondaryEnrollmentRegisterFragment closed = fragment(true, term);
            try (Cursor row = rows(closed); Cursor active = rows(fragment(false, "Juma"))) {
                assertEquals("Active/Closed counts for " + term,
                        "0/1", active.getCount() + "/" + row.getCount());
                assertTrue(row.moveToFirst());
                assertEquals(PARTNER, row.getString(row.getColumnIndexOrThrow(BASE_ENTITY_ID)));
                assertEquals(label + " · " + closureDay.toString("dd-MM-yyyy") + " · Mapera · 1001",
                        metadata(row));
            }
            closed.countExecute();
            verify(closed.clientAdapter).setTotalcount(1);
        }
        try (Cursor active = rows(fragment(false, "Jaribio"))) {
            assertEquals(1, active.getCount());
            assertTrue(active.moveToFirst());
            assertEquals(CONTROL, active.getString(active.getColumnIndexOrThrow(BASE_ENTITY_ID)));
        }
        assertFalse(MotherMentorPartnerClosureRepository.getState(cipher, CONTROL, null).isClosed());
        assertProfileClosure(closureDay);
        assertEquals(historyBefore, savedHistory());
        try (Cursor enrollment = database.rawQuery("SELECT " + MWENZI_STATUS + ", "
                + MWENZI_TAREHE_YA_KUFUNGWA + " FROM " + PARTNERS + " WHERE " + BASE_ENTITY_ID + " = ?",
                new String[]{PARTNER})) {
            assertTrue(enrollment.moveToFirst());
            assertEquals(indexed ? "active" : "", enrollment.getString(0));
            assertEquals("", enrollment.getString(1));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String metadata(Cursor row) {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_AppCompat);
        RecyclerViewProvider provider = new MotherMentorFollowUpRegisterProvider(context, null, null);
        RecyclerView.ViewHolder holder = provider.createViewHolder(new FrameLayout(context));
        Map<String, String> columns = new HashMap<>();
        for (int i = 0; i < row.getColumnCount(); i++) columns.put(row.getColumnName(i), row.getString(i));
        CommonPersonObjectClient client = new CommonPersonObjectClient(PARTNER, columns, "");
        client.setColumnmaps(columns);
        provider.getView(row, client, holder);
        return ((TextView) holder.itemView.findViewById(R.id.client_metadata)).getText().toString();
    }

    private void assertProfileClosure(LocalDate expectedDay) {
        MotherMentorPartnerClosure profile = MotherMentorPartnerClosureRepository.getState(cipher, PARTNER, null);
        assertTrue(profile.isClosed());
        assertEquals("attended", profile.getStatus());
        assertEquals(expectedDay.toString(), profile.getClosedDate());
    }

    private Cursor rows(MotherMentorSecondaryEnrollmentRegisterFragment fragment) {
        return ((CursorLoader) fragment.onCreateLoader(0, null)).loadInBackground();
    }

    private MotherMentorSecondaryEnrollmentRegisterFragment fragment(boolean closed, String search) {
        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).get();
        MotherMentorSecondaryEnrollmentRegisterFragment fragment = mock(
                MotherMentorSecondaryEnrollmentRegisterFragment.class, CALLS_REAL_METHODS);
        MotherMentorRegisterFragmentModel model = new MotherMentorRegisterFragmentModel();
        MotherMentorSecondaryEnrollmentRegisterFragmentPresenter presenter =
                new MotherMentorSecondaryEnrollmentRegisterFragmentPresenter(fragment,
                        mock(MotherMentorRegisterFragmentContract.Model.class), null, PARTNERS);
        CommonRepository repository = mock(CommonRepository.class);
        Bundle arguments = MotherMentorSecondaryEnrollmentRegisterFragment
                .newInstance(MotherMentorFollowUpType.PARTNER).getArguments();
        fragment.clientAdapter = mock(RecyclerViewPaginatedAdapter.class);
        fragment.Sortqueries = "ec_family_member.first_name ASC";
        ReflectionHelpers.setField(fragment, "registerModel", model);
        ReflectionHelpers.setField(fragment, "presenter", presenter);
        doReturn(arguments).when(fragment).getArguments();
        doReturn(activity).when(fragment).getActivity();
        doReturn(presenter).when(fragment).presenter();
        doReturn(repository).when(fragment).commonRepository();
        doNothing().when(fragment).filterandSortExecute(any(Bundle.class));
        when(fragment.clientAdapter.getCurrentlimit()).thenReturn(20);
        when(repository.rawCustomQueryForAdapter(anyString()))
                .thenAnswer(call -> database.rawQuery(call.getArgument(0), null));
        fragment.setSharedSearchText(search);
        fragment.setHistoryMode(closed);
        return fragment;
    }

    private List<String> savedHistory() {
        List<String> history = new ArrayList<>();
        try (Cursor cursor = database.rawQuery("SELECT " + VISIT_ID + ", " + VISIT_DATE + ", " + VISIT_JSON
                + " FROM visits WHERE " + BASE_ENTITY_ID + " = ? AND processed = 1 ORDER BY CAST("
                + VISIT_DATE + " AS INTEGER) DESC", new String[]{PARTNER})) {
            while (cursor.moveToNext()) {
                history.add(cursor.getString(0) + ":" + cursor.getString(1) + ":" + cursor.getString(2));
            }
        }
        return history;
    }

    private void partner(String id, String first, String last, String identifier, String row) {
        insert("ec_family_member", BASE_ENTITY_ID, id, RELATIONAL_ID, "family",
                FIRST_NAME, first, LAST_NAME, last, UNIQUE_ID, identifier);
        insert(PARTNERS, "id", row, BASE_ENTITY_ID, id, PARTNER_LINKAGE_ID, id,
                LAST_INTERACTED_WITH, row, PARTNER_NEXT_FOLLOW_UP_PHASE, "attendance",
                MWENZI_STATUS, "active", MWENZI_TAREHE_YA_KUFUNGWA, "");
    }

    private void visit(String partner, String id, LocalDate day, boolean processed, String end,
            String phase, String... answers) throws Exception {
        JSONArray observations = new JSONArray();
        String[] context = {FOLLOW_UP_CATEGORY, "PARTNER", PARTNER_LINKAGE_ID, partner,
                PARTNER_FOLLOW_UP_SCHEMA, "3", PARTNER_FOLLOW_UP_PHASE, phase,
                PARTNER_FOLLOW_UP_END, end, MOTHERMENTOR_VISIT_DATE, day.toString()};
        for (String[] fields : new String[][]{context, answers}) {
            for (int i = 0; i < fields.length; i += 2) {
                observations.put(new JSONObject().put("fieldCode", fields[i])
                        .put("values", new JSONArray(Collections.singletonList(fields[i + 1]))));
            }
        }
        insert("visits", VISIT_ID, id, FORM_SUBMISSION_ID, "form-" + id, BASE_ENTITY_ID, partner,
                VISIT_TYPE, MotherMentorFollowUpType.PARTNER.getEventType(), "processed", processed ? "1" : "0",
                VISIT_DATE, Long.toString(day.toDateTimeAtStartOfDay().getMillis()),
                VISIT_JSON, new JSONObject().put("obs", observations).toString());
    }

    private void create(String table, String... columns) {
        database.execSQL("CREATE TABLE " + table + " (" + String.join(" TEXT, ", columns) + " TEXT)");
    }

    private void insert(String table, String... pairs) {
        ContentValues values = new ContentValues();
        for (int i = 0; i < pairs.length; i += 2) values.put(pairs[i], pairs[i + 1]);
        database.insertOrThrow(table, null, values);
    }
}
