package org.smartregister.chw.activity;

import android.widget.DatePicker;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.activity.BaseAncHomeVisitActivity;
import org.smartregister.chw.anc.contract.BaseAncHomeVisitContract;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.VaccineDisplay;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.core.dao.VisitDao;
import org.smartregister.chw.fragment.BaseHomeVisitImmunizationFragmentFlv;
import org.smartregister.chw.interactor.ChildHomeVisitInteractorFlv;
import org.smartregister.immunization.repository.VaccineRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/** Exercises the same contract in the child host, plus its real interactor and vaccine validator. */
public class ChildImmunizationRestorationTest extends ImmunizationRestorationTest {
    @Override
    protected String ownerId() { return "test-baby"; }

    @Override
    protected Class<? extends BaseAncHomeVisitActivity> activityClass() { return TestChildActivity.class; }

    @Override
    protected BaseAncHomeVisitAction.ProcessingMode processingMode() { return BaseAncHomeVisitAction.ProcessingMode.COMBINED; }

    @Test
    public void realChildInteractorSavesDatesBeforeRecalculatingTheNextWindow() throws Exception {
        LinkedHashMap<String, BaseAncHomeVisitAction> actions = loadRealChildActions();
        saveAll(actions, "BCG", dateOfBirth().plusDays(1));
        LocalDate given = dateOfBirth().plusDays(46);
        saveAll(actions, "OPV 1", given);
        BaseHomeVisitImmunizationFragmentFlv first = destination(actionFor(actions, "OPV 1"));
        assertTrue(first.getVaccineDisplays().get("OPV 1").getValid());
        assertEquals(given, new LocalDate(first.getVaccineDisplays().get("OPV 1").getDateGiven()));
        VaccineDisplay next = destination(actionFor(actions, "OPV 2")).getVaccineDisplays().get("OPV 2");
        assertEquals(given.plusDays(28), new LocalDate(next.getStartDate()));
        assertFalse(next.getValid());
        assertEquals("test-baby", actionFor(actions, "OPV 1").getBaseEntityID());
    }

    @Test
    public void realChildWindowsAndAcceptedDatesSurviveProcessRecreation() throws Exception {
        LinkedHashMap<String, BaseAncHomeVisitAction> actions = loadRealChildActions();
        saveAll(actions, "BCG", dateOfBirth().plusDays(1));
        LocalDate firstDate = dateOfBirth().plusDays(46);
        saveAll(actions, "OPV 1", firstDate);
        String firstPayload = actionFor(actions, "OPV 1").getJsonPayload();
        LocalDate secondDate = firstDate.plusDays(30);
        saveAll(actions, "OPV 2", secondDate);
        String secondPayload = actionFor(actions, "OPV 2").getJsonPayload();
        recreate(false);
        actions = loadRealChildActions();
        assertEquals(firstPayload, actionFor(actions, "OPV 1").getJsonPayload());
        assertEquals(secondPayload, actionFor(actions, "OPV 2").getJsonPayload());
        VaccineDisplay accepted = destination(actionFor(actions, "OPV 2")).getVaccineDisplays().get("OPV 2");
        assertTrue(accepted.getValid());
        assertEquals(secondDate, new LocalDate(accepted.getDateGiven()));
        assertEquals(secondDate.plusDays(28), new LocalDate(destination(actionFor(actions, "OPV 3"))
                .getVaccineDisplays().get("OPV 3").getStartDate()));
    }

    @Test
    public void changingEarlierChildVaccinationsDoesNotRestoreInvalidatedLaterPayloads() throws Exception {
        LinkedHashMap<String, BaseAncHomeVisitAction> actions = loadRealChildActions();
        saveAll(actions, "BCG", dateOfBirth().plusDays(1));
        saveAll(actions, "OPV 1", dateOfBirth().plusDays(46));
        saveAll(actions, "OPV 2", dateOfBirth().plusDays(76));
        BaseAncHomeVisitAction second = actionFor(actions, "OPV 2");
        assertNotNull(second.getJsonPayload());
        open(actionFor(actions, "OPV 1"));
        vaccine("OPV 1").performClick();
        fragment.requireView().findViewById(R.id.save_btn).performClick();
        idle();
        assertNull(second.getJsonPayload());
        assertFalse(destination(actionFor(actions, "OPV 1")).getVaccineDisplays().get("OPV 1").getValid());
        assertNull(destination(actionFor(actions, "OPV 1")).getVaccineDisplays().get("OPV 1").getDateGiven());
        String secondTitle = second.getTitle();
        recreate(false);
        actions = loadRealChildActions();
        assertNull(actions.get(secondTitle).getJsonPayload());
    }

    @Test
    public void pendingChildWindowRebindsToTheNewValidatorAfterProcessRecreation() throws Exception {
        LinkedHashMap<String, BaseAncHomeVisitAction> actions = loadRealChildActions();
        saveAll(actions, "BCG", dateOfBirth().plusDays(1));
        open(actionFor(actions, "OPV 1"));
        for (String key : new ArrayList<>(fragment.getVaccineDisplays().keySet())) vaccine(key).performClick();
        LocalDate given = dateOfBirth().plusDays(46);
        DatePicker picker = fragment.requireView().findViewById(R.id.earlier_date_picker);
        picker.updateDate(given.getYear(), given.getMonthOfYear() - 1, given.getDayOfMonth());
        recreate(false);
        assertNotNull(fragment);
        BaseHomeVisitImmunizationFragmentFlv restored = fragment;
        actions = loadRealChildActions(false);
        fragment = restored;
        fragment.requireView().findViewById(R.id.save_btn).performClick();
        idle();
        assertFalse(fragment.isAdded());
        VaccineDisplay accepted = destination(actionFor(actions, "OPV 1")).getVaccineDisplays().get("OPV 1");
        assertTrue(accepted.getValid());
        assertEquals(given, new LocalDate(accepted.getDateGiven()));
        assertEquals(given.plusDays(28), new LocalDate(destination(actionFor(actions, "OPV 2"))
                .getVaccineDisplays().get("OPV 2").getStartDate()));
    }

    private LinkedHashMap<String, BaseAncHomeVisitAction> loadRealChildActions() throws Exception {
        return loadRealChildActions(true);
    }

    private LinkedHashMap<String, BaseAncHomeVisitAction> loadRealChildActions(boolean closeDialog) throws Exception {
        if (closeDialog && fragment != null && fragment.isAdded()) {
            controller.get().getSupportFragmentManager().beginTransaction().remove(fragment).commit();
            idle();
        }
        MemberObject child = new MemberObject();
        child.setBaseEntityId("test-baby");
        child.setDob(dateOfBirth().toString());
        LinkedHashMap<String, BaseAncHomeVisitAction> actions;
        try (MockedStatic<VisitDao> visits = mockStatic(VisitDao.class)) {
            visits.when(() -> VisitDao.getMedicalHistory(anyString())).thenReturn(Collections.emptyMap());
            actions = new VaccineOnlyInteractor().load(controller.get(), child);
        }
        controller.get().initializeActions(actions);
        return actions;
    }

    private LocalDate dateOfBirth() { return new LocalDate().minusMonths(6); }

    private BaseHomeVisitImmunizationFragmentFlv destination(BaseAncHomeVisitAction selected) {
        return (BaseHomeVisitImmunizationFragmentFlv) selected.getDestinationFragment();
    }

    private BaseAncHomeVisitAction actionFor(LinkedHashMap<String, BaseAncHomeVisitAction> actions, String vaccine) {
        for (BaseAncHomeVisitAction candidate : actions.values()) {
            if (destination(candidate).getVaccineDisplays().containsKey(vaccine)) return candidate;
        }
        java.util.Map<String, Object> visible = new java.util.LinkedHashMap<>();
        for (BaseAncHomeVisitAction candidate : actions.values()) visible.put(candidate.getTitle(), destination(candidate).getVaccineDisplays().keySet());
        throw new AssertionError("Missing actual child vaccine action: " + vaccine + " in " + visible);
    }

    private void open(BaseAncHomeVisitAction selected) {
        fragment = destination(selected);
        controller.get().startFragment(selected);
        idle();
    }

    private void saveAll(LinkedHashMap<String, BaseAncHomeVisitAction> actions, String vaccine, LocalDate date) {
        BaseAncHomeVisitAction selected = actionFor(actions, vaccine);
        assertTrue(selected.isEnabled());
        open(selected);
        for (String key : new ArrayList<>(fragment.getVaccineDisplays().keySet())) {
            if (!vaccine(key).isChecked()) vaccine(key).performClick();
        }
        DatePicker picker = fragment.requireView().findViewById(R.id.earlier_date_picker);
        picker.updateDate(date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth());
        fragment.requireView().findViewById(R.id.save_btn).performClick();
        idle();
        assertNotNull(selected.getJsonPayload());
        assertFalse(fragment.isAdded());
    }

    public static class TestChildActivity extends ChildHomeVisitActivity {
        @Override
        protected void registerPresenter() { presenter = mock(BaseAncHomeVisitContract.Presenter.class); }

        @Override
        public void redrawVisitUI() { /* Whole-visit submission is outside this dialog test. */ }
    }

    private static class VaccineOnlyInteractor extends ChildHomeVisitInteractorFlv {
        LinkedHashMap<String, BaseAncHomeVisitAction> load(BaseAncHomeVisitContract.View host, MemberObject child) throws Exception {
            context = host.getContext();
            view = host;
            memberObject = child;
            dob = new LocalDate(child.getDob()).toDate();
            actionList = new LinkedHashMap<>();
            evaluateImmunization();
            return actionList;
        }

        @Override
        public VaccineRepository getVaccineRepo() {
            VaccineRepository repository = mock(VaccineRepository.class);
            when(repository.findByEntityId(anyString())).thenReturn(Collections.emptyList());
            return repository;
        }
    }
}
