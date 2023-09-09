package org.smartregister.chw.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.presenter.CoreChildRegisterFragmentPresenter;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.FetchStatus;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChildRegisterFragmentTest extends BaseUnitTest {
    @Mock
    private CommonRepository commonRepository;
    @Mock
    private Context context;
    @Mock
    private ProgressBar syncProgressBar;
    @Mock
    private ImageView syncButton;
    @Mock
    private CoreChildRegisterFragmentPresenter presenter;
    private ChildRegisterFragment fragment;
    @Mock
    private View view;

    private FragmentActivity activity;
    private ActivityController<AppCompatActivity> controller;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        fragment = Mockito.mock(ChildRegisterFragment.class, Mockito.CALLS_REAL_METHODS);
        ReflectionHelpers.setField(fragment, "presenter", presenter);
        ReflectionHelpers.setField(fragment, "view", view);
        ReflectionHelpers.setField(fragment, "dueOnlyLayout", view);
        ReflectionHelpers.setField(fragment, "dueFilterActive", true);

        CoreLibrary.init(context);
        when(context.commonrepository(anyString())).thenReturn(commonRepository);
        controller = Robolectric.buildActivity(AppCompatActivity.class).create().resume();
        activity = controller.get();
        Context.bindtypes = new ArrayList<>();
        SyncStatusBroadcastReceiver.init(activity);
    }

    @Test
    public void testInitializePresenter() {
        fragment.initializePresenter();
        assertNotNull(presenter);
    }

    @Test
    public void testSetUniqueID() {
        when(fragment.getSearchView()).thenReturn(new EditText(activity));
        fragment.setUniqueID("unique");
        assertEquals("unique", fragment.getSearchView().getText().toString());
    }

    @Test
    public void onSyncCompleteTogglesSyncVisibility() {
        FetchStatus fetchStatus = Mockito.anyObject();
        ReflectionHelpers.setField(fragment, "syncButton", syncButton);
        ReflectionHelpers.setField(fragment, "syncProgressBar", syncProgressBar);
        fragment.onSyncComplete(fetchStatus);
        verify(syncProgressBar, Mockito.times(2)).setVisibility(View.GONE);
        verify(syncButton, Mockito.times(2)).setVisibility(View.GONE);
    }

    @Test
    public void testSetupViews() {
        when(fragment.getActivity()).thenReturn(activity);
        when(fragment.getContext()).thenReturn(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.fragment_base_register, null);
        fragment.setupViews(view);

        View dueOnlyLayout = view.findViewById(R.id.due_only_layout);
        dueOnlyLayout.setVisibility(View.VISIBLE);
        assertEquals(View.VISIBLE, dueOnlyLayout.getVisibility());
    }

    @After
    public void tearDown() {
        try {
            SyncStatusBroadcastReceiver.destroy(activity);
            activity.finish();
            controller.pause().stop().destroy(); //destroy controller if we can
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.gc();
    }

}



