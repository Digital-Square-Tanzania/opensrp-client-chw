package org.smartregister.chw.fragment;

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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.model.MotherMentorClientRegisterModel;
import org.smartregister.chw.mothermentor.contract.MotherMentorRegisterFragmentContract;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorRegisterLoaderTest {
    @Test
    public void obsoleteLoaderKeepsItsSnapshotWithoutUpdatingTheActiveCountOrRows() {
        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).get();
        MotherMentorRegisterFragment fragment =
                mock(MotherMentorRegisterFragment.class, CALLS_REAL_METHODS);
        MotherMentorClientRegisterModel model = mock(MotherMentorClientRegisterModel.class);
        MotherMentorRegisterFragmentContract.Presenter presenter =
                mock(MotherMentorRegisterFragmentContract.Presenter.class);
        CommonRepository repository = mock(CommonRepository.class);
        RecyclerViewPaginatedAdapter adapter = mock(RecyclerViewPaginatedAdapter.class);
        fragment.clientAdapter = adapter;
        doReturn(activity).when(fragment).getActivity();
        doReturn(presenter).when(fragment).presenter();
        doReturn(repository).when(fragment).commonRepository();
        doNothing().when(fragment).hideProgressView();
        ReflectionHelpers.setField(fragment, "clientModel", model);
        ReflectionHelpers.setField(fragment, "searchText", "old search");
        ReflectionHelpers.setField(
                fragment,
                "filterState",
                new MotherMentorRegisterFilterState(true, true, false, false));
        when(presenter.getMainCondition()).thenReturn("old condition");
        when(adapter.getCurrentlimit()).thenReturn(20);
        when(adapter.getCurrentoffset()).thenReturn(40);
        when(model.selectClientsPage(
                        "old condition", "old search", true, true, false, false, 20, 40))
                .thenReturn("old page query");
        when(model.countClients("old condition", "old search", true, true, false, false))
                .thenReturn("old count query");
        Bundle countArgs = new Bundle();
        countArgs.putBoolean("count_execute", true);
        Loader<Cursor> oldLoader = fragment.onCreateLoader(0, countArgs);

        ReflectionHelpers.setField(fragment, "searchText", "new search");
        ReflectionHelpers.setField(
                fragment,
                "filterState",
                new MotherMentorRegisterFilterState(true, false, true, true));
        when(presenter.getMainCondition()).thenReturn("new condition");
        when(adapter.getCurrentoffset()).thenReturn(0);
        when(model.selectClientsPage("new condition", "new search", true, false, true, true, 20, 0))
                .thenReturn("new page query");
        when(model.countClients("new condition", "new search", true, false, true, true))
                .thenReturn("new count query");
        Loader<Cursor> currentLoader = fragment.onCreateLoader(0, countArgs);
        MatrixCursor oldRows = rows("old client");
        MatrixCursor currentRows = rows("current client");
        when(repository.rawCustomQueryForAdapter("old count query")).thenReturn(count(2));
        when(repository.rawCustomQueryForAdapter("new count query")).thenReturn(count(5));
        when(repository.rawCustomQueryForAdapter("old page query")).thenReturn(oldRows);
        when(repository.rawCustomQueryForAdapter("new page query")).thenReturn(currentRows);

        assertSame(oldRows, ((CursorLoader) oldLoader).loadInBackground());
        verify(repository).rawCustomQueryForAdapter("old page query");
        verify(repository).rawCustomQueryForAdapter("old count query");
        verify(fragment, never()).countExecute();
        verify(adapter, never()).setTotalcount(anyInt());
        verify(adapter, never()).setCurrentoffset(anyInt());
        fragment.onLoadFinished(oldLoader, oldRows);
        fragment.onLoaderReset(oldLoader);
        verify(adapter, never()).swapCursor(oldRows);
        verify(adapter, never()).swapCursor(null);
        verify(adapter, never()).setTotalcount(anyInt());

        assertSame(currentRows, ((CursorLoader) currentLoader).loadInBackground());
        verify(adapter, never()).setTotalcount(anyInt());
        fragment.onLoadFinished(currentLoader, currentRows);
        verify(adapter).setTotalcount(5);
        verify(adapter).swapCursor(currentRows);
        verify(model)
                .selectClientsPage("old condition", "old search", true, true, false, false, 20, 40);
        verify(model)
                .selectClientsPage("new condition", "new search", true, false, true, true, 20, 0);
    }

    private MatrixCursor count(int value) {
        MatrixCursor cursor = new MatrixCursor(new String[] {"count"});
        cursor.addRow(new Object[] {value});
        return cursor;
    }

    private MatrixCursor rows(String id) {
        MatrixCursor cursor = new MatrixCursor(new String[] {"base_entity_id"});
        cursor.addRow(new Object[] {id});
        return cursor;
    }
}
