package org.smartregister.chw.fragment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.mothermentor.dao.MotherMentorChildEidDao;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorChildEidRegisterLoaderTest {
    private MotherMentorSecondaryEnrollmentRegisterFragment fragment;
    private CommonRepository repository;
    private RecyclerViewPaginatedAdapter adapter;

    @Before
    public void setUp() {
        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).get();
        fragment = mock(MotherMentorSecondaryEnrollmentRegisterFragment.class, CALLS_REAL_METHODS);
        Bundle arguments = new Bundle();
        arguments.putString("follow_up_type", MotherMentorFollowUpType.CHILD_EID.getValue());
        doReturn(arguments).when(fragment).getArguments();
        doReturn(activity).when(fragment).getActivity();
        doNothing().when(fragment).hideProgressView();
        repository = mock(CommonRepository.class);
        doReturn(repository).when(fragment).commonRepository();
        adapter = mock(RecyclerViewPaginatedAdapter.class);
        fragment.clientAdapter = adapter;
        when(adapter.getCurrentlimit()).thenReturn(20);
    }

    @Test
    public void replacedSearchKeepsItsSnapshotAndCannotUpdateRowsCountOrCloseItsOwnedCursor() {
        when(adapter.getCurrentoffset()).thenReturn(40);
        ReflectionHelpers.setField(fragment, "sharedSearchText", "old search");
        Loader<Cursor> oldLoader = loader(true);
        MatrixCursor oldRows = rows("old child");
        stub("old search", false, 40, 2, oldRows);

        when(adapter.getCurrentoffset()).thenReturn(0);
        ReflectionHelpers.setField(fragment, "sharedSearchText", "new search");
        ReflectionHelpers.setField(fragment, "historyMode", true);
        Loader<Cursor> currentLoader = loader(true);
        MatrixCursor currentRows = rows("current child");
        stub("new search", true, 0, 5, currentRows);

        assertSame(oldRows, ((CursorLoader) oldLoader).loadInBackground());
        verify(repository).rawCustomQueryForAdapter(MotherMentorChildEidDao.pageQuery("old search", false, 20, 40));
        verify(fragment, never()).countExecute();
        verify(adapter, never()).setCurrentoffset(anyInt());
        fragment.onLoadFinished(oldLoader, oldRows);
        fragment.onLoaderReset(oldLoader);
        verify(adapter, never()).swapCursor(oldRows);
        verify(adapter, never()).swapCursor(null);
        verify(adapter, never()).setTotalcount(anyInt());
        assertFalse(oldRows.isClosed());

        assertSame(currentRows, ((CursorLoader) currentLoader).loadInBackground());
        verify(adapter, never()).setTotalcount(anyInt());
        fragment.onLoadFinished(currentLoader, currentRows);
        verify(adapter).setTotalcount(5);
        verify(adapter).swapCursor(currentRows);
    }

    @Test
    public void destroyedViewsRejectTheirLoaderDeliveryAndResetBeforeAndAfterRecreation() {
        ReflectionHelpers.setField(fragment, "sharedSearchText", "child");
        Loader<Cursor> oldLoader = loader(true);
        MatrixCursor oldRows = rows("old child");
        stub("child", false, 0, 2, oldRows);
        assertSame(oldRows, ((CursorLoader) oldLoader).loadInBackground());
        fragment.onDestroyView();
        fragment.onLoadFinished(oldLoader, oldRows);
        fragment.onLoaderReset(oldLoader);
        verify(adapter, never()).swapCursor(oldRows);
        verify(adapter, never()).swapCursor(null);
        verify(adapter, never()).setTotalcount(anyInt());
        assertFalse(oldRows.isClosed());

        RecyclerViewPaginatedAdapter replacement = mock(RecyclerViewPaginatedAdapter.class);
        fragment.clientAdapter = replacement;
        when(replacement.getCurrentlimit()).thenReturn(20);
        Loader<Cursor> newLoader = loader(true);
        MatrixCursor newRows = rows("new child");
        stub("child", false, 0, 3, newRows);
        fragment.onLoadFinished(oldLoader, oldRows);
        fragment.onLoaderReset(oldLoader);
        verify(replacement, never()).swapCursor(oldRows);
        verify(replacement, never()).swapCursor(null);
        verify(replacement, never()).setTotalcount(anyInt());
        assertSame(newRows, ((CursorLoader) newLoader).loadInBackground());
        fragment.onLoadFinished(newLoader, newRows);
        verify(replacement).setTotalcount(3);
        verify(replacement).swapCursor(newRows);
    }

    @Test
    public void pagingWithoutRecountUsesItsCapturedOffsetAndLeavesTheCountUntouched() {
        ReflectionHelpers.setField(fragment, "sharedSearchText", "child");
        when(adapter.getCurrentoffset()).thenReturn(20);
        Loader<Cursor> pageLoader = loader(false);
        MatrixCursor page = rows("second page");
        when(repository.rawCustomQueryForAdapter(MotherMentorChildEidDao.pageQuery("child", false, 20, 20)))
                .thenReturn(page);
        when(adapter.getCurrentoffset()).thenReturn(0);
        assertSame(page, ((CursorLoader) pageLoader).loadInBackground());
        fragment.onLoadFinished(pageLoader, page);
        verify(adapter).swapCursor(page);
        verify(adapter, never()).setTotalcount(anyInt());
        verify(repository, never()).rawCustomQueryForAdapter(MotherMentorChildEidDao.countQuery("child", false));
    }

    private Loader<Cursor> loader(boolean count) {
        Bundle args = new Bundle();
        args.putBoolean("count_execute", count);
        return fragment.onCreateLoader(0, args);
    }

    private void stub(String search, boolean closed, int offset, int total, MatrixCursor page) {
        when(repository.rawCustomQueryForAdapter(MotherMentorChildEidDao.countQuery(search, closed)))
                .thenReturn(count(total));
        when(repository.rawCustomQueryForAdapter(MotherMentorChildEidDao.pageQuery(search, closed, 20, offset)))
                .thenReturn(page);
    }

    private MatrixCursor count(int total) {
        MatrixCursor cursor = new MatrixCursor(new String[] {"count"});
        cursor.addRow(new Object[] {total});
        return cursor;
    }

    private MatrixCursor rows(String id) {
        MatrixCursor cursor = new MatrixCursor(new String[] {"base_entity_id"});
        cursor.addRow(new Object[] {id});
        return cursor;
    }
}
