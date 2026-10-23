package org.smartregister.chw.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.vijay.jsonwizard.customviews.CheckBox;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.contract.BaseAncHomeVisitContract;
import org.smartregister.chw.anc.activity.BaseAncHomeVisitActivity;
import org.smartregister.chw.contract.ImmunizationSaveHost;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.VaccineDisplay;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.dao.PersonDao;
import org.smartregister.chw.fragment.BaseHomeVisitImmunizationFragmentFlv;
import org.smartregister.immunization.domain.VaccineWrapper;
import org.smartregister.util.FormUtils;
import org.smartregister.util.Session;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.chw.anc.util.Constants.ANC_MEMBER_OBJECTS.MEMBER_PROFILE_OBJECT;

@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class ImmunizationRestorationTest extends BaseUnitTest {
    protected static final String ACTION = "Immunization for Test Baby";
    protected ActivityController<? extends BaseAncHomeVisitActivity> controller;
    private MockedStatic<PersonDao> people;
    private MockedStatic<FormUtils> forms;
    protected BaseHomeVisitImmunizationFragmentFlv fragment;
    protected BaseAncHomeVisitAction action;

    @Before
    public void setUp() throws Exception {
        Session session = ReflectionHelpers.getField(CoreLibrary.getInstance().context().userService(), "session");
        session.start(session.lengthInMilliseconds()).setPassword(java.util.UUID.randomUUID().toString());
        people = mockStatic(PersonDao.class);
        people.when(() -> PersonDao.getMothersChildren(anyString())).thenReturn(Collections.emptyList());
        forms = mockStatic(FormUtils.class);
        FormUtils formUtils = mock(FormUtils.class);
        forms.when(() -> FormUtils.getInstance(any())).thenReturn(formUtils);
        when(formUtils.getFormJson("immunization_visit")).thenAnswer(invocation -> {
            try (java.io.InputStream input = org.robolectric.RuntimeEnvironment.getApplication()
                    .getAssets().open("json.form/immunization_visit.json")) {
                return new JSONObject(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
        });
        MemberObject mother = new MemberObject();
        mother.setBaseEntityId(ownerId());
        controller = newController(new Intent().putExtra(MEMBER_PROFILE_OBJECT, mother), null);
        initializeAction();
        controller.get().startFragment(action);
        idle();
    }

    protected String ownerId() { return "test-mother"; }

    protected Class<? extends BaseAncHomeVisitActivity> activityClass() { return TestPncActivity.class; }

    protected BaseAncHomeVisitAction.ProcessingMode processingMode() { return BaseAncHomeVisitAction.ProcessingMode.SEPARATE; }

    protected ImmunizationSaveHost host() { return (ImmunizationSaveHost) controller.get(); }

    private ActivityController<? extends BaseAncHomeVisitActivity> newController(Intent intent, Bundle state) {
        ActivityController<? extends BaseAncHomeVisitActivity> result = Robolectric.buildActivity(activityClass(), intent);
        result.get().setTheme(R.style.ChwTheme_NoActionBar);
        return result.create(state).start().resume().visible();
    }

    protected void initializeAction() throws Exception {
        List<VaccineDisplay> displays = new ArrayList<>();
        for (String name : new String[]{"BCG", "OPV 0", "DPT 1"}) {
            VaccineWrapper wrapper = new VaccineWrapper();
            wrapper.setName(name);
            VaccineDisplay display = new VaccineDisplay();
            display.setVaccineWrapper(wrapper);
            display.setStartDate(new Date(0));
            display.setEndDate(new Date());
            displays.add(display);
        }
        fragment = BaseHomeVisitImmunizationFragmentFlv.getInstance(controller.get(), "test-baby", null,
                displays, false, "at_birth");
        action = spy(new BaseAncHomeVisitAction.Builder(controller.get(), ACTION)
                .withBaseEntityID("test-baby")
                .withProcessingMode(processingMode())
                .withDestinationFragment(fragment).build());
        LinkedHashMap<String, BaseAncHomeVisitAction> actions = new LinkedHashMap<>();
        actions.put(ACTION, action);
        controller.get().initializeActions(actions);
    }

    @Test
    public void saveDeliversOneCompletePayload() {
        fragment.requireView().findViewById(R.id.save_btn).performClick();
        verify(action, times(1)).setJsonPayload(anyString());
        assertTrue(action.getJsonPayload().contains("reasons_no_vaccination"));
    }

    @Test
    public void missingCallbackDoesNotCrashOrLoseTheForm() {
        ReflectionHelpers.setField(fragment, "visitView", null);
        fragment.requireView().findViewById(R.id.save_btn).performClick();
        assertNotNull(action.getJsonPayload());
        assertTrue(action.getJsonPayload().contains("test-baby"));
    }

    @Test
    public void parcelledProcessStateRetainsTheVaccineDefinition() throws Exception {
        Bundle state = new Bundle();
        controller.saveInstanceState(state);
        Intent intent = controller.get().getIntent();
        controller.pause().stop().destroy();
        controller = newController(intent, roundTrip(state));
        initializeAction();
        idle();
        BaseHomeVisitImmunizationFragmentFlv restored = restoredDialog();
        assertNotNull(restored);
        assertEquals(3, restored.getVaccineDisplays().size());
        restored.requireView().findViewById(R.id.save_btn).performClick();
        verify(action, times(1)).setJsonPayload(anyString());
        assertTrue(action.getJsonPayload().contains("test-baby"));
    }

    @Test
    public void restoredFormKeepsSelectionsSeparateDatesAndMissingReasons() throws Exception {
        enterPartialVaccination();
        String reason = firstReason().getTag().toString();
        recreate(false);
        assertTrue(vaccine("BCG").isChecked());
        assertTrue(vaccine("OPV 0").isChecked());
        assertFalse(vaccine("DPT 1").isChecked());
        assertTrue(firstReason().isChecked());
        assertEquals(1, vaccineDate("BCG").getDayOfMonth());
        assertEquals(2, vaccineDate("OPV 0").getDayOfMonth());
        // The visit action loads after the restored dialog. Saving must retain the draft.
        View save = fragment.requireView().findViewById(R.id.save_btn);
        save.performClick();
        idle();
        assertTrue(fragment.isAdded());
        assertEquals(View.VISIBLE, fragment.requireView().findViewById(R.id.immunization_save_error).getVisibility());
        BaseHomeVisitImmunizationFragmentFlv restored = fragment;
        initializeAction();
        fragment = restored;
        save.performClick();
        verify(action, times(1)).setJsonPayload(anyString());
        JSONObject payload = new JSONObject(action.getJsonPayload());
        assertEquals("test-baby", payload.getString("entity_id"));
        assertEquals("2020-09-01", field(payload, "bcg").getString("value"));
        assertEquals("2020-09-02", field(payload, "opv_0").getString("value"));
        assertEquals(org.smartregister.chw.anc.util.Constants.HOME_VISIT.VACCINE_NOT_GIVEN,
                field(payload, "dpt_1").getString("value"));
        JSONObject reasons = new JSONObject(field(payload, "reasons_no_vaccination").getString("value"));
        assertEquals(reason, reasons.getJSONArray("reasons_for_missing").getString(0));
        assertEquals(1, reasons.getJSONArray("missing_vaccines").length());
        assertEquals("at_birth", reasons.getString("when_immunization_given"));
    }

    @Test
    public void unavailableOwnerKeepsEntriesAndAllowsRetryWithUpdatedInput() throws Exception {
        enterPartialVaccination();
        controller.get().getAncHomeVisitActions().clear();
        fragment.requireView().findViewById(R.id.save_btn).performClick();
        idle();
        verify(action, never()).setJsonPayload(anyString());
        assertTrue(fragment.isAdded());
        assertTrue(firstReason().isChecked());
        vaccineDate("BCG").updateDate(2020, 8, 3);
        controller.get().getAncHomeVisitActions().put(ACTION, action);
        fragment.requireView().findViewById(R.id.save_btn).performClick();
        verify(action, times(1)).setJsonPayload(anyString());
        assertEquals("2020-09-03", field(new JSONObject(action.getJsonPayload()), "bcg").getString("value"));
    }

    @Test
    public void saveUsesOriginalActionEvenWhenCurrentActionChanges() throws Exception {
        BaseAncHomeVisitAction another = spy(new BaseAncHomeVisitAction.Builder(controller.get(), "Other action")
                .withDestinationFragment(new org.smartregister.chw.anc.fragment.BaseHomeVisitFragment()).build());
        controller.get().getAncHomeVisitActions().put("Other action", another);
        ReflectionHelpers.setField(controller.get(), "current_action", "Other action");
        fragment.requireView().findViewById(R.id.save_btn).performClick();
        verify(action, times(1)).setJsonPayload(anyString());
        verify(another, never()).setJsonPayload(anyString());
    }

    @Test
    public void rejectsDifferentMotherChildActionRequestOrPayload() throws Exception {
        String request = fragment.getArguments().getString("request_id");
        String payload = draftPayload("test-baby").toString();
        assertFalse(host().saveImmunization("other-mother", ACTION, "test-baby", request, payload));
        assertFalse(host().saveImmunization(ownerId(), ACTION, "other-baby", request, payload));
        assertFalse(host().saveImmunization(ownerId(), "Other action", "test-baby", request, payload));
        assertFalse(host().saveImmunization(ownerId(), ACTION, "test-baby", "stale", payload));
        assertFalse(host().saveImmunization(ownerId(), ACTION, "test-baby", request,
                draftPayload("other-baby").toString()));
        assertFalse(host().saveImmunization(ownerId(), ACTION, "test-baby", request, "{}"));
        assertFalse(host().saveImmunization(ownerId(), ACTION, "test-baby", request, payload));
        verify(action, never()).setJsonPayload(anyString());
    }

    @Test
    public void repeatedSaveAndAcknowledgementDeliverOnlyOnce() throws Exception {
        View save = fragment.requireView().findViewById(R.id.save_btn);
        String request = fragment.getArguments().getString("request_id");
        save.performClick();
        String payload = action.getJsonPayload();
        save.performClick();
        assertTrue(host().saveImmunization(ownerId(), ACTION, "test-baby", request, payload));
        assertFalse(host().saveImmunization(ownerId(), ACTION, "test-baby", request,
                new JSONObject(payload).put("changed", true).toString()));
        verify(action, times(1)).setJsonPayload(anyString());
        JSONArray fields = new JSONObject(payload).getJSONObject("step1").getJSONArray("fields");
        int reasons = 0;
        for (int i = 0; i < fields.length(); i++) {
            if ("reasons_no_vaccination".equals(fields.getJSONObject(i).getString("key"))) reasons++;
        }
        assertEquals(1, reasons);
    }

    @Test
    public void acceptedDraftSurvivesProcessRecreationWithoutDuplicateDelivery() throws Exception {
        enterPartialVaccination();
        String request = fragment.getArguments().getString("request_id");
        fragment.requireView().findViewById(R.id.save_btn).performClick();
        String payload = action.getJsonPayload();
        idle();
        recreate(true);
        assertEquals(payload, action.getJsonPayload());
        verify(action, times(1)).setJsonPayload(payload);
        assertTrue(host().saveImmunization(ownerId(), ACTION, "test-baby", request, payload));
        verify(action, times(1)).setJsonPayload(payload);
        fragment = (BaseHomeVisitImmunizationFragmentFlv) action.getDestinationFragment();
        controller.get().startFragment(action);
        idle();
        assertTrue(firstReason().isChecked());
        assertEquals(2, vaccineDate("OPV 0").getDayOfMonth());
        assertFalse(host().saveImmunization(ownerId(), ACTION, "test-baby", request, payload));
        vaccineDate("BCG").updateDate(2020, 8, 3);
        fragment.requireView().findViewById(R.id.save_btn).performClick();
        verify(action, times(2)).setJsonPayload(anyString());
        assertEquals("2020-09-03", field(new JSONObject(action.getJsonPayload()), "bcg").getString("value"));
    }

    @Test
    public void existingPayloadRestoresSelectionsAndReasonsForEditing() throws Exception {
        enterPartialVaccination();
        fragment.requireView().findViewById(R.id.save_btn).performClick();
        String payload = action.getJsonPayload();
        idle();
        initializeAction();
        ReflectionHelpers.setField(fragment, "jsonObject", new JSONObject(payload));
        controller.get().startFragment(action);
        idle();
        assertTrue(vaccine("BCG").isChecked());
        assertFalse(vaccine("DPT 1").isChecked());
        assertTrue(firstReason().isChecked());
        assertEquals(1, vaccineDate("BCG").getDayOfMonth());
        assertEquals(2, vaccineDate("OPV 0").getDayOfMonth());
    }

    @Test
    public void changingVaccineSelectionPreservesOtherVaccineDates() {
        enterPartialVaccination();
        vaccine("DPT 1").performClick();
        assertEquals(1, vaccineDate("BCG").getDayOfMonth());
        assertEquals(2, vaccineDate("OPV 0").getDayOfMonth());
    }

    @Test
    public void missingLegacyStateShowsRecoveryAndDoesNotCompleteAction() {
        BaseHomeVisitImmunizationFragmentFlv legacy = new BaseHomeVisitImmunizationFragmentFlv();
        legacy.show(controller.get().getSupportFragmentManager(), "legacy");
        idle();
        legacy.requireView().findViewById(R.id.save_btn).performClick();
        idle();
        assertTrue(legacy.isAdded());
        TextView error = legacy.requireView().findViewById(R.id.immunization_save_error);
        assertEquals(View.VISIBLE, error.getVisibility());
        assertEquals(controller.get().getString(R.string.immunization_restore_failed), error.getText().toString());
        verify(action, never()).setJsonPayload(anyString());
    }

    protected void recreate(boolean loadActions) throws Exception {
        Bundle state = new Bundle();
        controller.saveInstanceState(state);
        Intent intent = controller.get().getIntent();
        controller.pause().stop().destroy();
        controller = newController(intent, roundTrip(state));
        if (loadActions) initializeAction();
        idle();
        fragment = restoredDialog();
    }

    private BaseHomeVisitImmunizationFragmentFlv restoredDialog() {
        // The library's show/remove transaction does not retain its tag reliably.
        for (androidx.fragment.app.Fragment restored : controller.get().getSupportFragmentManager().getFragments()) {
            if (restored instanceof BaseHomeVisitImmunizationFragmentFlv && restored.isAdded()) {
                return (BaseHomeVisitImmunizationFragmentFlv) restored;
            }
        }
        return null;
    }

    protected void enterPartialVaccination() {
        vaccine("BCG").performClick();
        vaccine("OPV 0").performClick();
        ((RadioGroup) fragment.requireView().findViewById(R.id.select_date_mode)).check(R.id.each_its_date);
        vaccineDate("BCG").updateDate(2020, 8, 1);
        vaccineDate("OPV 0").updateDate(2020, 8, 2);
        firstReason().setChecked(true);
    }

    protected CheckBox vaccine(String key) {
        return row(R.id.vaccination_name_layout, key).findViewById(R.id.select);
    }

    protected DatePicker vaccineDate(String key) {
        return row(R.id.single_vaccine_add_layout, key).findViewById(R.id.earlier_date_picker);
    }

    protected View row(int parent, String key) {
        ViewGroup rows = fragment.requireView().findViewById(parent);
        for (int i = 0; i < rows.getChildCount(); i++) {
            if (key.equals(rows.getChildAt(i).getTag())) return rows.getChildAt(i);
        }
        throw new AssertionError("Missing vaccine row: " + key);
    }

    protected CheckBox firstReason() {
        ViewGroup reasons = fragment.requireView().findViewById(R.id.reasons_no_vaccines);
        return reasons.getChildAt(0).findViewById(R.id.select);
    }

    protected JSONObject field(JSONObject payload, String key) throws Exception {
        JSONArray fields = payload.getJSONObject("step1").getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            if (key.equals(fields.getJSONObject(i).getString("key"))) return fields.getJSONObject(i);
        }
        throw new AssertionError("Missing payload field: " + key);
    }

    private JSONObject draftPayload(String childId) throws Exception {
        return new JSONObject().put("entity_id", childId).put("step1", new JSONObject().put("fields",
                new JSONArray().put(new JSONObject().put("key", "reasons_no_vaccination").put("value", "{}"))));
    }

    private Bundle roundTrip(Bundle state) {
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeBundle(state);
            parcel.setDataPosition(0);
            return parcel.readBundle(getClass().getClassLoader());
        } finally {
            parcel.recycle();
        }
    }

    protected void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));
    }

    @After
    public void tearDown() {
        if (controller != null) controller.pause().stop().destroy();
        if (people != null) people.close();
        if (forms != null) forms.close();
        CoreLibrary.getInstance().context().userService().logoutSession();
    }

    public static class TestPncActivity extends PncHomeVisitActivity {
        @Override
        protected void registerPresenter() {
            presenter = mock(BaseAncHomeVisitContract.Presenter.class);
        }

        @Override
        public void redrawVisitUI() {
            // Exercise real owner/action handling without unrelated whole-visit validation.
        }
    }
}
