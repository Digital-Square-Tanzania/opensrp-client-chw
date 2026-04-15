package org.smartregister.chw.interactor;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.actionhelper.DewormingAction;
import org.smartregister.chw.actionhelper.ECDAction;
import org.smartregister.chw.actionhelper.ExclusiveBreastFeedingAction;
import org.smartregister.chw.actionhelper.ImmunizationActionHelper;
import org.smartregister.chw.actionhelper.ImmunizationValidator;
import org.smartregister.chw.actionhelper.MNPAction;
import org.smartregister.chw.actionhelper.ObservationAction;
import org.smartregister.chw.actionhelper.VitaminaAction;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.contract.BaseAncHomeVisitContract;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.VaccineDisplay;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.fragment.BaseAncHomeVisitFragment;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.repository.VisitDetailsRepository;
import org.smartregister.chw.anc.repository.VisitRepository;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.chw.anc.util.VisitUtils;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.dao.VisitDao;
import org.smartregister.chw.core.interactor.CoreChildHomeVisitInteractor;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.core.utils.RecurringServiceUtil;
import org.smartregister.chw.core.utils.VaccineScheduleUtil;
import org.smartregister.chw.core.utils.VisitVaccineUtil;
import org.smartregister.chw.fragment.BaseHomeVisitImmunizationFragmentFlv;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.Utils;
import org.smartregister.domain.Alert;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.ServiceWrapper;
import org.smartregister.immunization.domain.VaccineWrapper;
import org.smartregister.immunization.domain.jsonmapping.Vaccine;
import org.smartregister.immunization.domain.jsonmapping.VaccineGroup;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.VaccinatorUtils;
import org.smartregister.util.FormUtils;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public abstract class DefaultChildHomeVisitInteractorFlv implements CoreChildHomeVisitInteractor.Flavor {
    protected LinkedHashMap<String, BaseAncHomeVisitAction> actionList;
    protected Context context;
    protected Map<String, List<VisitDetail>> details = null;
    protected MemberObject memberObject;
    protected BaseAncHomeVisitContract.View view;
    protected Date dob;
    protected Boolean vaccineCardReceived = false;
    protected Boolean hasBirthCert = false;
    protected Boolean editMode = false;
    protected Boolean vaccinesDefaultChecked = true;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    private static final int FIVE_YEARS = 5;

    @Override
    public LinkedHashMap<String, BaseAncHomeVisitAction> calculateActions(BaseAncHomeVisitContract.View view, MemberObject memberObject, BaseAncHomeVisitContract.InteractorCallBack callBack) throws BaseAncHomeVisitAction.ValidationException {
        actionList = new LinkedHashMap<>();
        context = view.getContext();
        this.memberObject = memberObject;
        editMode = view.getEditMode();
        try {
            this.dob = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(memberObject.getDob());
        } catch (ParseException e) {
            Timber.e(e);
        }
        this.view = view;
        // get the preloaded data
        if (view.getEditMode()) {
            Visit lastVisit = getVisitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EventType.CHILD_HOME_VISIT);
            if (lastVisit != null) {
                details = VisitUtils.getVisitGroups(getVisitDetailsRepository().getVisits(lastVisit.getVisitId()));
            }
        }

        hasBirthCert = VisitDao.memberHasBirthCert(memberObject.getBaseEntityId());
        vaccineCardReceived = VisitDao.memberHasVaccineCard(memberObject.getBaseEntityId());

        Map<String, ServiceWrapper> serviceWrapperMap = getServices();

        try {
            Constants.JSON_FORM.setLocaleAndAssetManager(ChwApplication.getCurrentLocale(), ChwApplication.getInstance().getApplicationContext().getAssets());
        } catch (Exception e) {
            Timber.e(e);
        }
        bindEvents(serviceWrapperMap);
        return actionList;
    }

    @VisibleForTesting
    public Map<String, ServiceWrapper> getServices() {
        return RecurringServiceUtil.getRecurringServices(
                memberObject.getBaseEntityId(),
                new DateTime(dob),
                Constants.SERVICE_GROUPS.CHILD
        );
    }

    protected VisitRepository getVisitRepository() {
        return AncLibrary.getInstance().visitRepository();
    }

    protected VisitDetailsRepository getVisitDetailsRepository() {
        return AncLibrary.getInstance().visitDetailsRepository();
    }

    protected void bindEvents(Map<String, ServiceWrapper> serviceWrapperMap) throws BaseAncHomeVisitAction.ValidationException {
        try {

            evaluateChildVaccineCard();
            evaluateImmunization();
            evaluateExclusiveBreastFeeding(serviceWrapperMap);
            evaluateVitaminA(serviceWrapperMap);
            evaluateDeworming(serviceWrapperMap);
            evaluateMNP(serviceWrapperMap);
            evaluateBirthCertForm();
            evaluateMUAC();
            evaluateDietary();
            evaluateECD();
            evaluateLLITN();
            evaluateObsAndIllness();
        } catch (BaseAncHomeVisitAction.ValidationException e) {
            throw (e);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    protected Map<String, List<VisitDetail>> getDetails(String eventName) {
        if (!editMode)
            return null;

        Map<String, List<VisitDetail>> visitDetails = null;
        Visit lastVisit = getVisitRepository().getLatestVisit(memberObject.getBaseEntityId(), eventName);
        if (lastVisit != null) {
            visitDetails = VisitUtils.getVisitGroups(getVisitDetailsRepository().getVisits(lastVisit.getVisitId()));
        }

        return visitDetails;
    }

    @VisibleForTesting
    public BaseAncHomeVisitAction.Builder getBuilder(String title) {
        return new BaseAncHomeVisitAction.Builder(context, title);
    }

    @VisibleForTesting
    public JSONObject getFormJson(String name) throws Exception {
        return FormUtils.getInstance(context).getFormJson(name);
    }

    @VisibleForTesting
    public JSONObject getFormJson(String name, String baseEntityID) throws Exception {
        return org.smartregister.chw.util.JsonFormUtils.getJson(view.getContext(), name, baseEntityID);
    }

    @VisibleForTesting
    public VaccineRepository getVaccineRepo() {
        return CoreChwApplication.getInstance().vaccineRepository();
    }

    @VisibleForTesting
    public List<VaccineGroup> getVaccineGroups() {
        return VaccineScheduleUtil.getVaccineGroups(ChwApplication.getInstance().getApplicationContext(), CoreConstants.SERVICE_GROUPS.CHILD);
    }

    @VisibleForTesting
    public List<Vaccine> getSpecialVaccines() {
        return VaccinatorUtils.getSpecialVaccines(context);
    }

    protected int immunizationCeiling() {
        return 24;
    }

    protected void evaluateChildVaccineCard() throws Exception {
        // expires after 24 months. verify that vaccine card is not received
        if (!new LocalDate().isAfter(new LocalDate(dob).plusMonths(24)) && !vaccineCardReceived) {
            Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.CHILD_VACCINE_CARD_RECEIVED);

            BaseAncHomeVisitAction vaccine_card = getBuilder(context.getString(R.string.vaccine_card_title))
                    .withOptional(false)
                    .withDetails(details)
                    .withBaseEntityID(memberObject.getBaseEntityId())
                    .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.SEPARATE)
                    .withHelper(new ChildVaccineCardHelper(dob))
                    .withDestinationFragment(BaseAncHomeVisitFragment.getInstance(view, Constants.JSON_FORM.CHILD_HOME_VISIT.getVaccineCard(), null, details, null))
                    .build();

            actionList.put(context.getString(R.string.vaccine_card_title), vaccine_card);
        }
    }

    protected void evaluateImmunization() throws Exception {
        //int age = getAgeInMonths();
        if (getAgeInMonths() >= immunizationCeiling()) return;

        List<VaccineGroup> childVaccineGroups = getVaccineGroups();
        List<Vaccine> specialVaccines = getSpecialVaccines();
        List<org.smartregister.immunization.domain.Vaccine> vaccines = getVaccineRepo().findByEntityId(memberObject.getBaseEntityId());

        String vaccineCategory = memberObject.getAge() > FIVE_YEARS ? Constants.CHILD_OVER_5 : CoreConstants.SERVICE_GROUPS.CHILD;
        List<VaccineRepo.Vaccine> allVacs = VaccineRepo.getVaccines(vaccineCategory);

        Map<String, VaccineRepo.Vaccine> vaccinesRepo = new HashMap<>();
        for (VaccineRepo.Vaccine vaccine : allVacs) {
            vaccinesRepo.put(vaccine.display().toLowerCase().replace(" ", ""), vaccine);
        }

        Map<VaccineGroup, List<Pair<VaccineRepo.Vaccine, Alert>>> pendingVaccines = VisitVaccineUtil.generateVisitVaccines(
                memberObject.getBaseEntityId(),
                vaccinesRepo,
                new DateTime(dob),
                childVaccineGroups,
                specialVaccines,
                vaccines,
                details
        );

        ImmunizationValidator validator = new ImmunizationValidator(childVaccineGroups, specialVaccines, CoreConstants.SERVICE_GROUPS.CHILD, vaccines);

        Map<String,BaseAncHomeVisitAction> actions = new HashMap<>();
        Map<String,Integer> vaccineOrder = new HashMap<>();

        int position = 0;
        for (Map.Entry<VaccineGroup, List<Pair<VaccineRepo.Vaccine, Alert>>> entry : pendingVaccines.entrySet()) {
            // add the objects to be displayed here

            List<VaccineWrapper> wrappers = VisitVaccineUtil.wrapVaccines(entry.getValue());
            List<VaccineDisplay> displays = VisitVaccineUtil.toDisplays(wrappers);

            String vaccineTitle= VisitVaccineUtil.getVaccineTitle(entry.getKey().name, context);
            String title = MessageFormat.format(context.getString(R.string.immunizations_count), vaccineTitle);
            BaseHomeVisitImmunizationFragmentFlv fragment = BaseHomeVisitImmunizationFragmentFlv
                    .getInstance(view, memberObject.getBaseEntityId(), details, displays, vaccinesDefaultChecked,vaccineTitle);
//                    BaseHomeVisitImmunizationFragmentFlv.getInstance(view, memberObject.getBaseEntityId(), details, displays, vaccinesDefaultChecked);
            if (ChwApplication.getApplicationFlavor().relaxVisitDateRestrictions()) {
                fragment.setRelaxedDates(ChwApplication.getApplicationFlavor().relaxVisitDateRestrictions());
                fragment.setMinimumDate(dob);
            }

            validator.addFragment(title, fragment, entry.getKey(), new DateTime(dob));

            BaseAncHomeVisitAction action = getBuilder(title)
                    .withOptional(false)
                    .withDetails(details)
                    .withDestinationFragment(fragment)
                    .withHelper(new ImmunizationActionHelper(context, wrappers))
                    .withDisabledMessage(context.getString(R.string.fill_earler_immunization))
                    .withValidator(validator)
                    .build();

            actions.put(title,action);
            vaccineOrder.put(title,position);
            actionList.put(title, action);
            position++;
        }

        validator.setActions(actions);
        validator.setVaccineOrder(vaccineOrder);

    }

    protected void evaluateExclusiveBreastFeeding(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {
        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Exclusive breastfeeding");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceIteration = serviceWrapper.getName().substring(serviceWrapper.getName().length() - 1);

        String title = context.getString(R.string.exclusive_breastfeeding_months, serviceIteration);

        // alert if overdue after 14 days
        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        ExclusiveBreastFeedingAction helper = new ExclusiveBreastFeedingAction(context, alert);
        JSONObject jsonObject = getFormJson(Constants.JSON_FORM.PNC_HOME_VISIT.getExclusiveBreastFeeding(), memberObject.getBaseEntityId());

        Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.EXCLUSIVE_BREASTFEEDING);

        if (details != null && details.size() > 0) {
            JsonFormUtils.populateForm(jsonObject, details);
        }

        BaseAncHomeVisitAction action = getBuilder(title)
                .withHelper(helper)
                .withDetails(details)
                .withOptional(false)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.SEPARATE)
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withPayloadDetails(MessageFormat.format("Exclusive_breastfeeding{0}", serviceIteration))
                .withDestinationFragment(BaseAncHomeVisitFragment.getInstance(view, null, jsonObject, details, serviceIteration))
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withSubtitle(MessageFormat.format("{0}{1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .build();

        // don't show if its after now
        if (!serviceWrapper.getVaccineDate().isAfterNow()) actionList.put(title, action);
    }

    protected void evaluateVitaminA(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {
        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Vitamin A");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;


        final String serviceIteration = serviceWrapper.getName().substring(serviceWrapper.getName().length() - 1);

        String title = context.getString(R.string.vitamin_a_number_dose, Utils.getDayOfMonthWithSuffix(Integer.valueOf(serviceIteration), context));

        // alert if overdue after 14 days
        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        VitaminaAction helper = new VitaminaAction(context, serviceIteration, alert);
        JSONObject jsonObject = getFormJson(Constants.JSON_FORM.CHILD_HOME_VISIT.getVitaminA(), memberObject.getBaseEntityId());

        Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.VITAMIN_A);

        // Before pre-processing
        setMinDate(jsonObject, "vitamin_a{0}_date", memberObject.getDob());

        JSONObject preProcessObject = helper.preProcess(jsonObject, serviceIteration);
        if (details != null && details.size() > 0) JsonFormUtils.populateForm(jsonObject, details);

        BaseAncHomeVisitAction action = getBuilder(title)
                .withHelper(helper)
                .withDetails(details)
                .withOptional(false)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.SEPARATE)
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withPayloadDetails(MessageFormat.format("Vitamin_A{0}", serviceIteration))
                .withDestinationFragment(BaseAncHomeVisitFragment.getInstance(view, null, preProcessObject, details, serviceIteration))
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withSubtitle(MessageFormat.format("{0} {1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .build();

        // don't show if its after now
        if (!serviceWrapper.getVaccineDate().isAfterNow()) actionList.put(title, action);
    }

    protected void evaluateDeworming(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {
        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Deworming");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceIteration = serviceWrapper.getName().substring(serviceWrapper.getName().length() - 1);

        String title = context.getString(R.string.deworming_number_dose, Utils.getDayOfMonthWithSuffix(Integer.valueOf(serviceIteration), context));

        // alert if overdue after 14 days
        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        DewormingAction helper = new DewormingAction(context, serviceIteration, alert);
        JSONObject jsonObject = getFormJson(Constants.JSON_FORM.CHILD_HOME_VISIT.getDEWORMING(), memberObject.getBaseEntityId());

        // Before pre-processing
        setMinDate(jsonObject, "deworming{0}_date", memberObject.getDob());

        JSONObject preProcessObject = helper.preProcess(jsonObject, serviceIteration);

        Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.DEWORMING);

        if (details != null && details.size() > 0) {
            JsonFormUtils.populateForm(jsonObject, details);
        }

        BaseAncHomeVisitAction action = getBuilder(title)
                .withHelper(helper)
                .withDetails(details)
                .withOptional(false)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.SEPARATE)
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withPayloadDetails(MessageFormat.format("Deworming_{0}", serviceIteration))
                .withDestinationFragment(BaseAncHomeVisitFragment.getInstance(view, null, preProcessObject, details, serviceIteration))
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withSubtitle(MessageFormat.format("{0} {1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .build();

        // don't show if its after now
        if (!serviceWrapper.getVaccineDate().isAfterNow()) actionList.put(title, action);
    }

    protected void evaluateMNP(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {
        ServiceWrapper serviceWrapper = serviceWrapperMap.get("MNP");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceIteration = serviceWrapper.getName().substring(serviceWrapper.getName().length() - 1);

        String title = context.getString(R.string.mnp_number_pack, Utils.getDayOfMonthWithSuffix(Integer.valueOf(serviceIteration), context));

        // alert if overdue after 14 days
        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        MNPAction helper = new MNPAction(context, serviceIteration, alert);
        JSONObject jsonObject = getFormJson(Constants.JSON_FORM.CHILD_HOME_VISIT.getMNP(), memberObject.getBaseEntityId());
        JSONObject preProcessObject = helper.preProcess(jsonObject, serviceIteration);

        Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.MNP);

        if (details != null && details.size() > 0) JsonFormUtils.populateForm(jsonObject, details);

        BaseAncHomeVisitAction action = getBuilder(title)
                .withHelper(helper)
                .withDetails(details)
                .withOptional(false)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.SEPARATE)
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withPayloadDetails(MessageFormat.format("MNP{0}", serviceIteration))
                .withDestinationFragment(BaseAncHomeVisitFragment.getInstance(view, null, preProcessObject, details, serviceIteration))
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withSubtitle(MessageFormat.format("{0} {1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .build();

        // don't show if its after now
        if (!serviceWrapper.getVaccineDate().isAfterNow()) actionList.put(title, action);
    }

    protected void evaluateBirthCertForm() throws Exception {
        if (!hasBirthCert) {

            Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.BIRTH_CERTIFICATION);

            BaseAncHomeVisitAction action = getBuilder(context.getString(R.string.birth_certification))
                    .withOptional(false)
                    .withDetails(details)
                    .withBaseEntityID(memberObject.getBaseEntityId())
                    .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.SEPARATE)
                    .withHelper(new BirthCertHelper(dob))
                    .withFormName(Constants.JSON_FORM.getBirthCertification())
                    .build();

            actionList.put(context.getString(R.string.birth_certification), action);
        }
    }

    protected void evaluateMUAC() throws Exception {
        int age = getAgeInMonths();
        if (age > 60 || age < 6) return;

        Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.MUAC);

        BaseAncHomeVisitAction action = getBuilder(context.getString(R.string.muac_title))
                .withOptional(false)
                .withDetails(details)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.SEPARATE)
                .withHelper(new MUACHelper())
                .withDestinationFragment(BaseAncHomeVisitFragment.getInstance(view, Constants.JSON_FORM.CHILD_HOME_VISIT.getMUAC(), null, details, null))
                .build();

        actionList.put(context.getString(R.string.muac_title), action);
    }

    protected void evaluateDietary() throws Exception {
        int age = getAgeInMonths();
        if (age > 60 || age < 6) {
            return;
        }

        Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.MINIMUM_DIETARY_DIVERSITY);

        BaseAncHomeVisitAction action = getBuilder(context.getString(R.string.minimum_dietary_title))
                .withOptional(false)
                .withDetails(details)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.SEPARATE)
                .withHelper(new DietaryHelper())
                .withDestinationFragment(BaseAncHomeVisitFragment.getInstance(view, Constants.JSON_FORM.CHILD_HOME_VISIT.getDIETARY(), null, details, null))
                .build();

        actionList.put(context.getString(R.string.minimum_dietary_title), action);
    }

    protected void evaluateECD() throws Exception {
        if (getAgeInMonths() > 60) {
            return;
        }

        JSONObject jsonObject = getFormJson(CoreConstants.JSON_FORM.ANC_HOME_VISIT.getEarlyChildhoodDevelopment());
        Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.ECD);
        try {
            jsonObject = CoreJsonFormUtils.getEcdWithDatePass(jsonObject, memberObject.getDob());
            JsonFormUtils.populateForm(jsonObject, details);
        } catch (Exception e) {
            Timber.e(e);
        }

        BaseAncHomeVisitAction action = getBuilder(context.getString(R.string.ecd_title))
                .withOptional(false)
                .withDetails(details)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.SEPARATE)
                .withHelper(new ECDAction())
                .withFormName(Constants.JSON_FORM.ANC_HOME_VISIT.getEarlyChildhoodDevelopment())
                .withJsonPayload(jsonObject.toString())
                .build();

        actionList.put(context.getString(R.string.ecd_title), action);
    }

    protected void evaluateLLITN() throws Exception {
        if (getAgeInMonths() > 60) {
            return;
        }

        Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.LLITN);

        BaseAncHomeVisitAction sleeping = getBuilder(context.getString(R.string.anc_home_visit_sleeping_under_llitn_net))
                .withOptional(false)
                .withDetails(details)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.SEPARATE)
                .withHelper(new LLITNHelper())
                .withDestinationFragment(BaseAncHomeVisitFragment.getInstance(view, Constants.JSON_FORM.CHILD_HOME_VISIT.getSleepingUnderLlitn(), null, details, null))
                .build();

        actionList.put(context.getString(R.string.anc_home_visit_sleeping_under_llitn_net), sleeping);

    }

    protected void evaluateObsAndIllness() throws Exception {
        Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.OBS_ILLNESS);

        BaseAncHomeVisitAction observation = getBuilder(context.getString(R.string.anc_home_visit_observations_n_illnes))
                .withOptional(true)
                .withDetails(details)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.SEPARATE)
                .withHelper(new ObservationAction())
                .withFormName(Constants.JSON_FORM.ANC_HOME_VISIT.getObservationAndIllness())
                .build();

        actionList.put(context.getString(R.string.anc_home_visit_observations_n_illnes), observation);
    }

    protected int getAgeInMonths() {
        return Months.monthsBetween(new LocalDate(dob), new LocalDate()).getMonths();
    }

    protected void setVaccinesDefaultChecked(boolean isChecked) {
        this.vaccinesDefaultChecked = isChecked;
    }

    private void setMinDate(JSONObject jsonObject, String dateFieldKey, String minDateString) {
        try {
            Date minDate = dateFormat.parse(minDateString);
            String parsedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(minDate);
            JSONObject fieldJSONObject = org.smartregister.chw.util.JsonFormUtils.getFieldJSONObject(org.smartregister.chw.util.JsonFormUtils.fields(jsonObject), dateFieldKey);
            fieldJSONObject.put(JsonFormConstants.MIN_DATE, parsedDate);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private class LLITNHelper extends HomeVisitActionHelper {
        private String llitn;

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                llitn = JsonFormUtils.getValue(jsonObject, "llitn");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public String evaluateSubTitle() {
            if (StringUtils.isBlank(llitn))
                return null;

            return llitn.equalsIgnoreCase("Yes") ? context.getString(R.string.yes) : context.getString(R.string.no);
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(llitn))
                return BaseAncHomeVisitAction.Status.PENDING;

            if (llitn.equalsIgnoreCase("Yes")) {
                return BaseAncHomeVisitAction.Status.COMPLETED;
            } else if (llitn.equalsIgnoreCase("No")) {
                return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
            } else {
                return BaseAncHomeVisitAction.Status.PENDING;
            }
        }
    }

    private class MUACHelper extends HomeVisitActionHelper {
        private String muac;

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                muac = JsonFormUtils.getValue(jsonObject, "muac");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public String evaluateSubTitle() {
            if (StringUtils.isBlank(muac)) {
                return null;
            }

            String value = "";
            if ("chk_green".equalsIgnoreCase(muac)) {
                value = context.getString(R.string.muac_choice_1);
            } else if ("chk_yellow".equalsIgnoreCase(muac)) {
                value = context.getString(R.string.muac_choice_2);
            } else if ("chk_red".equalsIgnoreCase(muac)) {
                value = context.getString(R.string.muac_choice_3);
            }
            return value;
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(muac)) {
                return BaseAncHomeVisitAction.Status.PENDING;
            }

            if ("chk_green".equalsIgnoreCase(muac)) {
                return BaseAncHomeVisitAction.Status.COMPLETED;
            }

            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        }
    }

    private class DietaryHelper extends HomeVisitActionHelper {
        private String diet_diversity;

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                diet_diversity = JsonFormUtils.getValue(jsonObject, "diet_diversity");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public String evaluateSubTitle() {
            if (StringUtils.isBlank(diet_diversity)) {
                return null;
            }

            String value = "";
            if ("chk_no_animal_products".equalsIgnoreCase(diet_diversity)) {
                value = context.getString(R.string.minimum_dietary_choice_1);
            } else if ("chw_one_animal_product_or_fruit".equalsIgnoreCase(diet_diversity)) {
                value = context.getString(R.string.minimum_dietary_choice_2);
            } else if ("chw_one_animal_product_and_fruit".equalsIgnoreCase(diet_diversity)) {
                value = context.getString(R.string.minimum_dietary_choice_3);
            }
            return value;
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(diet_diversity)) {
                return BaseAncHomeVisitAction.Status.PENDING;
            }

            if ("chw_one_animal_product_and_fruit".equalsIgnoreCase(diet_diversity)) {
                return BaseAncHomeVisitAction.Status.COMPLETED;
            }

            return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
        }
    }

    private class ChildVaccineCardHelper extends HomeVisitActionHelper {
        private String child_vaccine_card;
        private LocalDate birthDate;

        public ChildVaccineCardHelper(Date birthDate) {
            this.birthDate = new LocalDate(birthDate);
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                child_vaccine_card = JsonFormUtils.getValue(jsonObject, "child_vaccine_card");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public String evaluateSubTitle() {
            if (StringUtils.isBlank(child_vaccine_card)) {
                return null;
            }

            return child_vaccine_card.equalsIgnoreCase("Yes") ? context.getString(R.string.yes) : context.getString(R.string.no);
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(child_vaccine_card)) {
                return BaseAncHomeVisitAction.Status.PENDING;
            }

            if (child_vaccine_card.equalsIgnoreCase("Yes")) {
                return BaseAncHomeVisitAction.Status.COMPLETED;
            } else if (child_vaccine_card.equalsIgnoreCase("No")) {
                return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
            } else {
                return BaseAncHomeVisitAction.Status.PENDING;
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return isOverDue() ?
                    BaseAncHomeVisitAction.ScheduleStatus.OVERDUE : BaseAncHomeVisitAction.ScheduleStatus.DUE;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return MessageFormat.format("{0} {1}",
                    context.getString(isOverDue() ? R.string.overdue : R.string.due),
                    org.smartregister.chw.core.utils.Utils.dd_MMM_yyyy.format(birthDate.toDate())
            );
        }

        private boolean isOverDue() {
            return new LocalDate().isAfter(birthDate.plusMonths(12));
        }
    }

    public static class BirthCertHelper extends HomeVisitActionHelper {
        private String birth_cert;
        private String birth_cert_issue_date;
        private String birth_cert_num;
        private LocalDate birthDate;

        public BirthCertHelper(Date birthDate) {
            this.birthDate = new LocalDate(birthDate);
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                birth_cert = JsonFormUtils.getValue(jsonObject, "birth_cert");
                birth_cert_issue_date = JsonFormUtils.getValue(jsonObject, "birth_cert_issue_date");
                birth_cert_num = JsonFormUtils.getValue(jsonObject, "birth_cert_num");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public String evaluateSubTitle() {
            if (StringUtils.isBlank(birth_cert)) {
                return null;
            }

            String certDate;
            try {
                Date date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(birth_cert_issue_date);
                certDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date);
            } catch (Exception e) {
                certDate = birth_cert_issue_date;
            }

            return birth_cert.equalsIgnoreCase("Yes") ?
                    MessageFormat.format("{0} {1} (#{2}) ", context.getString(R.string.issued), certDate, birth_cert_num) :
                    context.getString(R.string.not_done);
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(birth_cert)) {
                return BaseAncHomeVisitAction.Status.PENDING;
            }

            if ("Yes".equalsIgnoreCase(birth_cert)) {
                return BaseAncHomeVisitAction.Status.COMPLETED;
            } else if (birth_cert.equalsIgnoreCase("No")) {
                return BaseAncHomeVisitAction.Status.PARTIALLY_COMPLETED;
            } else {
                return BaseAncHomeVisitAction.Status.PENDING;
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return isOverDue() ?
                    BaseAncHomeVisitAction.ScheduleStatus.OVERDUE : BaseAncHomeVisitAction.ScheduleStatus.DUE;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return MessageFormat.format("{0} {1}",
                    context.getString(isOverDue() ? R.string.overdue : R.string.due),
                    org.smartregister.chw.core.utils.Utils.dd_MMM_yyyy.format(birthDate.toDate())
            );
        }

        private boolean isOverDue() {
            return new LocalDate().isAfter(birthDate.plusMonths(12));
        }
    }
}