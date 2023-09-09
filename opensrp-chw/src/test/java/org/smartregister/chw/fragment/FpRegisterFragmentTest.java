package org.smartregister.chw.fragment;

import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.core.R;
import org.smartregister.chw.fp.contract.BaseFpRegisterFragmentContract;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class FpRegisterFragmentTest extends BaseUnitTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private Context context;
    @Mock
    private FpRegisterFragment fpRegisterFragment;

    @Mock
    private CommonRepository commonRepository;

    @Mock
    private View view;

    private FragmentActivity activity;
    private ActivityController<AppCompatActivity> activityController;

    @Mock
    private ImageView imageView;

    @Mock
    private ProgressBar clientsProgressView;

    @Mock
    private TextView textView;

    @Mock
    private RelativeLayout relativeLayout;

    @Mock
    private EditText editText;

    @Mock
    private TextWatcher textWatcher;

    @Mock
    private View.OnKeyListener hideKeyboard;

    @Mock
    private ProgressBar syncProgressBar;

    @Mock
    private BaseFpRegisterFragmentContract.Presenter presenter;

    @Mock
    private CommonPersonObjectClient commonPersonObjectClient;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        fpRegisterFragment = Mockito.mock(FpRegisterFragment.class, Mockito.CALLS_REAL_METHODS);
        ReflectionHelpers.setField(fpRegisterFragment, "presenter", presenter);
        ReflectionHelpers.setField(fpRegisterFragment, "view", view);
        ReflectionHelpers.setField(fpRegisterFragment, "dueOnlyLayout", view);
        ReflectionHelpers.setField(fpRegisterFragment, "clientsProgressView", clientsProgressView);
        ReflectionHelpers.setField(fpRegisterFragment, "dueFilterActive", true);
        ReflectionHelpers.setField(fpRegisterFragment, "qrCodeScanImageView", imageView);
        ReflectionHelpers.setField(fpRegisterFragment, "headerTextDisplay", textView);
        ReflectionHelpers.setField(fpRegisterFragment, "filterStatus", textView);
        ReflectionHelpers.setField(fpRegisterFragment, "filterRelativeLayout", relativeLayout);
        ReflectionHelpers.setField(fpRegisterFragment, "searchView", editText);
        ReflectionHelpers.setField(fpRegisterFragment, "textWatcher", textWatcher);
        ReflectionHelpers.setField(fpRegisterFragment, "hideKeyboard", hideKeyboard);
        ReflectionHelpers.setField(fpRegisterFragment, "syncProgressBar", syncProgressBar);
        ReflectionHelpers.setField(fpRegisterFragment, "syncButton", imageView);
        ReflectionHelpers.setField(fpRegisterFragment, "globalQrSearch", false);

        CoreLibrary.init(context);
        when(context.commonrepository(anyString())).thenReturn(commonRepository);
        activityController = Robolectric.buildActivity(AppCompatActivity.class).create().resume();
        activity = activityController.get();
        Context.bindtypes = new ArrayList<>();
        SyncStatusBroadcastReceiver.init(activity);
    }

    @Test
    public void testSetupViews() {
        when(fpRegisterFragment.getActivity()).thenReturn(activity);
        when(fpRegisterFragment.getContext()).thenReturn(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.fragment_base_register, null);
        fpRegisterFragment.setupViews(view);

        View dueOnlyLayout = view.findViewById(R.id.due_only_layout);
        dueOnlyLayout.setVisibility(View.VISIBLE);
        View searchBarLayout = view.findViewById(org.smartregister.R.id.search_bar_layout);
        searchBarLayout.setBackgroundResource(R.color.customAppThemeBlue);
        assertEquals(View.VISIBLE, dueOnlyLayout.getVisibility());
    }

    @Test
    public void testInitializePresenterActivityIsNull() {
        fpRegisterFragment.initializePresenter();
        TestCase.assertNotNull(fpRegisterFragment.presenter());
    }

    @Test
    public void testInitializePresenter() {
        Mockito.doReturn(activity).when(fpRegisterFragment).getActivity();

        fpRegisterFragment.initializePresenter();
        TestCase.assertNotNull(fpRegisterFragment.presenter());
    }

    @Test
    public void testOpenProfile() {
        when(fpRegisterFragment.getActivity()).thenReturn(activity);
        fpRegisterFragment.openProfile(commonPersonObjectClient);
    }

    @After
    public void tearDown() {
        try {
            SyncStatusBroadcastReceiver.destroy(activity);
            activityController.pause().stop().destroy();
            activity.finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
