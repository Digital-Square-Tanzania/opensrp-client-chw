package org.smartregister.chw.fragment;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.content.CursorLoader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.model.MotherMentorRegisterFragmentModel;
import org.smartregister.chw.mothermentor.contract.MotherMentorRegisterFragmentContract;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;

/** Runs the register's actual row and count SQL against a small SQLite cohort. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorFollowUpSearchTest {
    private static final String IIT = "ec_mothermentor_enroll_it";
    private static final String PARTNER = "ec_mothermentor_enroll_partner";
    private static final String CHILD = "ec_mothermentor_enroll_child_eid";
    private static final String[] TABLES = {IIT, PARTNER, CHILD};
    private SQLiteDatabase database;

    @Before
    public void setUp() {
        database = SQLiteDatabase.create(null);
        database.execSQL("CREATE TABLE ec_family_member (base_entity_id TEXT, relational_id TEXT, "
                + "first_name TEXT, middle_name TEXT, last_name TEXT, dob TEXT, gender TEXT, "
                + "unique_id TEXT, phone_number TEXT, other_phone_number TEXT)");
        database.execSQL("CREATE TABLE ec_family (base_entity_id TEXT, primary_caregiver TEXT, "
                + "family_head TEXT, village_town TEXT, first_name TEXT)");
        database.execSQL("CREATE TABLE ec_anc_register (base_entity_id TEXT, is_closed INTEGER, edd TEXT)");
        database.execSQL("INSERT INTO ec_family VALUES "
                + "('family-a', 'client-a', 'client-a', 'Mapera', 'Test'), "
                + "('family-b', 'client-b', 'client-b', 'Kibaoni', 'Test')");
        database.execSQL("INSERT INTO ec_family_member VALUES "
                + "('client-a', 'family-a', 'Neema', 'Uhakiki', 'O''Neil', '1990-01-01', 'Female', '123456789', '', ''), "
                + "('client-b', 'family-b', 'Juma', 'Other', 'Test', '1990-01-01', 'Male', '987654321', '', ''), "
                + "('closed', 'family-a', 'Neema', 'Closed', 'Test', '1990-01-01', 'Female', '000000000', '', '')");
        for (String table : TABLES) {
            database.execSQL("CREATE TABLE " + table + " (id INTEGER, base_entity_id TEXT, "
                    + "is_closed INTEGER, weeks_lost TEXT, expected_return_date TEXT)");
            database.execSQL("INSERT INTO " + table + " VALUES "
                    + "(1, 'client-a', 0, '3', ''), (2, 'client-b', 0, '4', ''), (3, 'closed', 1, '5', '')");
        }
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void searchRestrictsIitRowsAndCountToOpenMatches() {
        verifySearch(IIT, "Neema", 1);
    }

    @Test
    public void searchRestrictsPartnerRowsAndCountToOpenMatches() {
        verifySearch(PARTNER, "Neema", 1);
    }

    @Test
    public void searchRestrictsChildEidRowsAndCountToOpenMatches() {
        verifySearch(CHILD, "Neema", 1);
    }

    @Test
    public void sharedFieldsAndApostrophesSearchCorrectlyOnEveryTab() {
        for (String table : TABLES) {
            for (String term : new String[]{"Neema", "Uhakiki", "O'Neil", "123456789", "Mapera"}) {
                verifySearch(table, term, 1);
            }
            verifySearch(table, "not-present", 0);
        }
    }

    @Test
    public void clearingSharedSearchRestoresOpenClientsOnEveryTab() {
        for (String table : TABLES) {
            MotherMentorSecondaryEnrollmentRegisterFragment fragment = fragment(table);
            fragment.setSharedSearchText("Neema");
            assertRowsAndCount(fragment, 1);
            fragment.setSharedSearchText("");
            assertRowsAndCount(fragment, 2);
        }
    }

    private void verifySearch(String table, String term, int expected) {
        MotherMentorSecondaryEnrollmentRegisterFragment fragment = fragment(table);
        fragment.setSharedSearchText(term);
        assertRowsAndCount(fragment, expected);
    }

    private void assertRowsAndCount(MotherMentorSecondaryEnrollmentRegisterFragment fragment, int expected) {
        try (Cursor rows = ((CursorLoader) fragment.onCreateLoader(0, null)).loadInBackground()) {
            assertEquals(expected, rows.getCount());
        }
        fragment.countExecute();
        verify(fragment.clientAdapter).setTotalcount(expected);
    }

    private MotherMentorSecondaryEnrollmentRegisterFragment fragment(String table) {
        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).get();
        MotherMentorSecondaryEnrollmentRegisterFragment fragment = mock(
                MotherMentorSecondaryEnrollmentRegisterFragment.class, CALLS_REAL_METHODS);
        MotherMentorRegisterFragmentContract.Presenter presenter = mock(
                MotherMentorRegisterFragmentContract.Presenter.class);
        CommonRepository repository = mock(CommonRepository.class);
        fragment.clientAdapter = mock(RecyclerViewPaginatedAdapter.class);
        fragment.Sortqueries = "ec_family_member.first_name ASC";
        ReflectionHelpers.setField(fragment, "registerModel", new MotherMentorRegisterFragmentModel());
        ReflectionHelpers.setField(fragment, "presenter", presenter);
        doReturn(activity).when(fragment).getActivity();
        doReturn(presenter).when(fragment).presenter();
        doReturn(repository).when(fragment).commonRepository();
        doNothing().when(fragment).filterandSortExecute(any(Bundle.class));
        when(fragment.clientAdapter.getCurrentlimit()).thenReturn(20);
        when(fragment.clientAdapter.getCurrentoffset()).thenReturn(0);
        when(presenter.getMainTable()).thenReturn(table);
        when(presenter.getMainCondition()).thenReturn(table + ".is_closed = 0");
        when(repository.rawCustomQueryForAdapter(anyString())).thenAnswer(
                call -> database.rawQuery(call.getArgument(0), null));
        return fragment;
    }
}
