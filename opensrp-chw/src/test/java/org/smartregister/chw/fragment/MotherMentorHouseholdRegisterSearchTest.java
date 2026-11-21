package org.smartregister.chw.fragment;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
import org.smartregister.chw.model.MotherMentorHouseholdRegisterFragmentModel;
import org.smartregister.chw.mothermentor.contract.MotherMentorRegisterFragmentContract;
import org.smartregister.chw.mothermentor.dao.MotherMentorHouseholdFollowUpDao;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorHouseholdRegisterSearchTest {
    private SQLiteDatabase database;

    @Before
    public void setUp() {
        database = SQLiteDatabase.create(null);
        database.execSQL("CREATE TABLE ec_family (id INTEGER, relationalid TEXT, "
                + "last_interacted_with TEXT, base_entity_id TEXT, first_name TEXT, last_name TEXT, "
                + "unique_id TEXT, village_town TEXT, family_head TEXT, primary_caregiver TEXT, "
                + "is_closed INTEGER)");
        database.execSQL("INSERT INTO ec_family (id, base_entity_id, first_name, unique_id, is_closed)"
                + " VALUES (1, 'family-one', 'O''Neil 100%_!', '31245', 0),"
                + " (2, 'family-two', 'Kaya Mzuri', '98765', 0),"
                + " (3, 'family-three', 'O''Neil Other', '21245', 0),"
                + " (4, 'family-four', 'O''Neil Unenrolled', '41245', 0)");
        String enrollment = Constants.TABLES.MAMA_KINARA_HOUSEHOLD_ENROLMENT;
        database.execSQL("CREATE TABLE " + enrollment + " (household_id TEXT, is_closed INTEGER, "
                + "consent TEXT, kaya_status TEXT, kaya_sababu_ya_kufungwa TEXT, "
                + "kaya_tarehe_ya_kufungwa TEXT, last_interacted_with TEXT)");
        database.execSQL("INSERT INTO " + enrollment
                + " (household_id, is_closed, consent, kaya_status) VALUES"
                + " ('family-one', 0, 'ndiyo', 'active'), ('family-two', 0, 'ndiyo', 'active'),"
                + " ('family-three', 1, 'ndiyo', 'moved')");
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void searchesEnrolledHouseholdsByPartialNameAndId() {
        assertMatches(" o'NE ", false, 1);
        assertMatches("1245", false, 1);
        assertMatches("family-one", false, 1);
        assertMatches("unmatched", false, 0);
        assertMatches("", false, 2);
    }

    @Test
    public void treatsSqlWildcardsAsLiteralAndKeepsHistorySeparate() {
        assertMatches("%_!", false, 1);
        assertMatches("%", false, 1);
        assertMatches("O'Neil", true, 1);
        assertMatches("31245", true, 0);
    }

    private void assertMatches(String search, boolean closed, int expected) {
        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).get();
        MotherMentorContactRegisterFragment fragment =
                mock(MotherMentorContactRegisterFragment.class, CALLS_REAL_METHODS);
        MotherMentorRegisterFragmentContract.Presenter presenter =
                mock(MotherMentorRegisterFragmentContract.Presenter.class);
        CommonRepository repository = mock(CommonRepository.class);
        fragment.clientAdapter = mock(RecyclerViewPaginatedAdapter.class);
        fragment.Sortqueries = "ec_family.first_name ASC";
        fragment.filters = search;
        ReflectionHelpers.setField(fragment, "model", new MotherMentorHouseholdRegisterFragmentModel());
        doReturn(activity).when(fragment).getActivity();
        doReturn(presenter).when(fragment).presenter();
        doReturn(repository).when(fragment).commonRepository();
        when(fragment.clientAdapter.getCurrentlimit()).thenReturn(20);
        when(presenter.getMainTable()).thenReturn("ec_family");
        when(presenter.getMainCondition()).thenReturn(
                MotherMentorHouseholdFollowUpDao.enrollmentCondition("ec_family", closed));
        when(repository.rawCustomQueryForAdapter(anyString())).thenAnswer(
                call -> database.rawQuery(call.getArgument(0), null));

        try (Cursor rows = ((CursorLoader) fragment.onCreateLoader(0, null)).loadInBackground()) {
            assertEquals(expected, rows.getCount());
        }
        fragment.countExecute();
        verify(fragment.clientAdapter).setTotalcount(expected);
    }
}
