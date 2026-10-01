package org.smartregister.chw.fragment;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.domain.AypInSchoolGroupListItem;
import org.smartregister.chw.interactor.AypOutSchoolGroupsRegisterInteractor;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Config(sdk = 28)
public class AypGroupsCallbackTest extends BaseUnitTest {
    private AypOutSchoolGroupsRegisterFragment fragment;
    private RecyclerView clientsView;
    private LinearLayout emptyView;
    private Activity activity;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).get();
        fragment = mock(AypOutSchoolGroupsRegisterFragment.class, CALLS_REAL_METHODS);
        clientsView = new RecyclerView(activity);
        emptyView = new LinearLayout(activity);
        ReflectionHelpers.setField(fragment, "clientsView", clientsView);
        ReflectionHelpers.setField(fragment, "emptyViewLayout", emptyView);
        when(fragment.isAdded()).thenReturn(true);
        when(fragment.getView()).thenReturn(new View(activity));
        when(fragment.getActivity()).thenReturn(new androidx.fragment.app.FragmentActivity());
    }

    @Test
    public void populatedCallbackAfterDetachDoesNotTouchOldView() {
        try (MockedConstruction<AypOutSchoolGroupsRegisterInteractor> construction =
                     mockConstruction(AypOutSchoolGroupsRegisterInteractor.class)) {
            fragment.setUpAdapter();
            AypOutSchoolGroupsRegisterInteractor.Callback callback = captureCallback(construction, false);
            when(fragment.isAdded()).thenReturn(false);
            when(fragment.getActivity()).thenReturn(null);
            when(fragment.getView()).thenReturn(null);
            callback.onData(Collections.singletonList(new AypInSchoolGroupListItem("test-group", "Test group", "age_band", "age_10_14")));
            assertNull(clientsView.getAdapter());
        }
    }

    @Test
    public void filteredCallbackAfterViewReplacementDoesNotTouchNewView() {
        ReflectionHelpers.setField(fragment, "currentGroupTypeFilter", "age_band");
        try (MockedConstruction<AypOutSchoolGroupsRegisterInteractor> construction =
                     mockConstruction(AypOutSchoolGroupsRegisterInteractor.class)) {
            fragment.setUpAdapter();
            AypOutSchoolGroupsRegisterInteractor.Callback callback = captureCallback(construction, true);
            when(fragment.getView()).thenReturn(new View(activity));
            callback.onData(Collections.singletonList(new AypInSchoolGroupListItem("test-group", "Test group", "age_band", "age_10_14")));
            assertNull(clientsView.getAdapter());
        }
    }

    @Test
    public void emptyCallbackAfterViewDestroyedDoesNotChangeEmptyState() {
        emptyView.setVisibility(View.GONE);
        try (MockedConstruction<AypOutSchoolGroupsRegisterInteractor> construction =
                     mockConstruction(AypOutSchoolGroupsRegisterInteractor.class)) {
            fragment.setUpAdapter();
            AypOutSchoolGroupsRegisterInteractor.Callback callback = captureCallback(construction, false);
            when(fragment.getView()).thenReturn(null);
            callback.onData(Collections.emptyList());
            assertEquals(View.GONE, emptyView.getVisibility());
        }
    }

    @Test
    public void attachedCallbackShowsResultsThenEmptyState() {
        try (MockedConstruction<AypOutSchoolGroupsRegisterInteractor> construction =
                     mockConstruction(AypOutSchoolGroupsRegisterInteractor.class)) {
            fragment.setUpAdapter();
            AypOutSchoolGroupsRegisterInteractor.Callback callback = captureCallback(construction, false);
            callback.onData(Collections.singletonList(new AypInSchoolGroupListItem("test-group", "Test group", "age_band", "age_10_14")));
            assertEquals(1, clientsView.getAdapter().getItemCount());
            assertEquals(View.GONE, emptyView.getVisibility());
            callback.onData(Collections.emptyList());
            assertNull(clientsView.getAdapter());
            assertEquals(View.VISIBLE, emptyView.getVisibility());
        }
    }

    @Test
    public void delayedRefreshAfterDetachDoesNotStartFetch() {
        when(fragment.isAdded()).thenReturn(false);
        when(fragment.getActivity()).thenReturn(null);
        when(fragment.getView()).thenReturn(null);
        try (MockedConstruction<AypOutSchoolGroupsRegisterInteractor> construction =
                     mockConstruction(AypOutSchoolGroupsRegisterInteractor.class)) {
            fragment.setUpAdapter();
            assertEquals(0, construction.constructed().size());
        }
    }

    private AypOutSchoolGroupsRegisterInteractor.Callback captureCallback(
            MockedConstruction<AypOutSchoolGroupsRegisterInteractor> construction, boolean filtered) {
        ArgumentCaptor<AypOutSchoolGroupsRegisterInteractor.Callback> callback =
                ArgumentCaptor.forClass(AypOutSchoolGroupsRegisterInteractor.Callback.class);
        if (filtered) {
            verify(construction.constructed().get(0)).fetchItemsByType(
                    org.mockito.ArgumentMatchers.eq("age_band"), callback.capture());
        } else {
            verify(construction.constructed().get(0)).fetchItems(callback.capture());
        }
        return callback.getValue();
    }
}
