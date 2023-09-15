package org.smartregister.chw.presenter;

import static org.smartregister.chw.core.utils.Utils.getDuration;
import static org.smartregister.util.Utils.getName;
import static org.smartregister.util.Utils.getValue;

import android.app.Activity;
import android.util.Pair;

import com.vijay.jsonwizard.utils.FormUtils;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.joda.time.Period;
import org.json.JSONObject;
import org.smartregister.chw.BuildConfig;
import org.smartregister.chw.R;
import org.smartregister.chw.activity.ChildProfileActivity;
import org.smartregister.chw.activity.ReferralRegistrationActivity;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.contract.CoreChildProfileContract;
import org.smartregister.chw.core.model.ChildVisit;
import org.smartregister.chw.core.presenter.CoreChildProfilePresenter;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.CoreChildService;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.ChwChildDao;
import org.smartregister.chw.interactor.ChildProfileInteractor;
import org.smartregister.chw.interactor.FamilyProfileInteractor;
import org.smartregister.chw.model.ChildRegisterModel;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.chw.util.Utils;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class ChildProfilePresenter extends CoreChildProfilePresenter {

    private List<ReferralTypeModel> referralTypeModels;

    public ChildProfilePresenter(CoreChildProfileContract.View childView, CoreChildProfileContract.Model model, String childBaseEntityId) {
        super(childView, model, childBaseEntityId);
        setView(new WeakReference<>(childView));
        setInteractor(new ChildProfileInteractor());
        getInteractor().setChildBaseEntityId(childBaseEntityId);
        setModel(model);
    }

    @Override
    public void verifyHasPhone() {
        new FamilyProfileInteractor().verifyHasPhone(familyID, this);
    }

    @Override
    public void notifyHasPhone(boolean hasPhone) {
        if (getView() != null) {
            getView().updateHasPhone(hasPhone);
        }
    }

    @Override
    public void updateChildProfile(String jsonString) {
        getView().showProgressDialog(R.string.updating);
        Pair<Client, Event> pair = new ChildRegisterModel().processRegistration(jsonString);
        if (pair == null) {
            return;
        }

        getInteractor().saveRegistration(pair, jsonString, true, this);
    }

    @Override
    public void startSickChildReferralForm() {
        if (BuildConfig.USE_UNIFIED_REFERRAL_APPROACH) {
            try {
                JSONObject formJson = (new FormUtils()).getFormJsonFromRepositoryOrAssets(getView().getContext(), Constants.JSON_FORM.getChildUnifiedReferralForm());
                formJson.put(Constants.REFERRAL_TASK_FOCUS, referralTypeModels.get(0).getReferralType());
                ReferralRegistrationActivity.startGeneralReferralFormActivityForResults((Activity) getView().getContext(),
                        getChildBaseEntityId(), formJson, false);
            } catch (Exception e) {
                Timber.e(e);
            }
        } else {
            super.startSickChildReferralForm();
        }
    }

    @Override
    public void startSickChildForm(CommonPersonObjectClient client) {
        try {
            getView().setProgressBarState(true);
            JSONObject jsonObject = this.getFormUtils().getFormJson(CoreConstants.JSON_FORM.getChildSickForm());
            jsonObject.put(CoreConstants.ENTITY_ID, getValue(client.getColumnmaps(), DBConstants.KEY.BASE_ENTITY_ID, false));

            String dobStr = getValue(client.getColumnmaps(), DBConstants.KEY.DOB, false);
            Date dobDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(dobStr);

            LocalDate date1 = LocalDate.fromDateFields(dobDate);
            LocalDate date2 = LocalDate.now();
            int months = Months.monthsBetween(date1, date2).getMonths();

            Map<String, String> valueMap = new HashMap<>();
            valueMap.put("age_in_months", String.valueOf(months));
            valueMap.put("child_first_name", getValue(client.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true));
            valueMap.put("gender", getValue(client.getColumnmaps(), DBConstants.KEY.GENDER, true).equals("Male") ? "1" : "2");

            JsonFormUtils.populatedJsonForm(jsonObject, valueMap);

            this.getView().startFormActivity(jsonObject);
        } catch (Exception var3) {
            Timber.e(var3);
        } finally {
            if (getView() != null)
                getView().setProgressBarState(false);
        }
    }

    @Override
    public void refreshProfileTopSection(CommonPersonObjectClient client) {
        super.refreshProfileTopSection(client);

        if (ChwApplication.getApplicationFlavor().showLastNameOnChildProfile()) {
            String relationalId = getValue(client.getColumnmaps(), ChildDBConstants.KEY.RELATIONAL_ID, true).toLowerCase();
            // String parentLastName = getValue(client.getColumnmaps(), ChildDBConstants.KEY.FAMILY_FIRST_NAME, true);
            String familyName = ChwChildDao.getChildFamilyName(relationalId);

            String firstName = getValue(client.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true);
            String lastName = getValue(client.getColumnmaps(), DBConstants.KEY.LAST_NAME, true);
            String middleName = getValue(client.getColumnmaps(), DBConstants.KEY.MIDDLE_NAME, true);
            String childName = getName(firstName, middleName + " " + lastName);
            getView().setProfileName(getName(childName, familyName));
            getView().setAge(org.smartregister.family.util.Utils.getTranslatedDate(getDuration(getValue(client.getColumnmaps(), DBConstants.KEY.DOB, false)), getView().getContext()));
        }
    }

    public void referToFacility() {
        referralTypeModels = ((ChildProfileActivity) getView()).getReferralTypeModels();
        if (referralTypeModels.size() == 1) {
            startSickChildReferralForm();
        } else {
            Utils.launchClientReferralActivity((Activity) getView(), referralTypeModels, childBaseEntityId);
        }
    }

    @Override
    public void updateChildService(CoreChildService childService) {
        if (getView() != null) {
            if (!(ChwApplication.getApplicationFlavor().splitUpcomingServicesView())) {
                if (childService != null) {
                    if (childService.getServiceStatus().equalsIgnoreCase(CoreConstants.ServiceType.UPCOMING.name())) {
                        getView().setServiceNameUpcoming(childService.getServiceName().trim(), childService.getServiceDate());
                    } else if (childService.getServiceStatus().equalsIgnoreCase(CoreConstants.ServiceType.OVERDUE.name())) {
                        getView().setServiceNameOverDue(childService.getServiceName().trim(), childService.getServiceDate());
                    } else {
                        getView().setServiceNameDue(childService.getServiceName().trim(), childService.getServiceDate());
                    }
                } else {
                    getView().setServiceNameDue("", "");
                }
            } else {
                getView().setDueTodayServices();
            }
        }
    }

    private void setDueView() {
        if (ChwChildDao.hasDueTodayVaccines(childBaseEntityId) || ChwChildDao.hasDueAlerts(childBaseEntityId)) {
            getView().setVisitButtonDueStatus();
        } else {
            getView().setNoButtonView();
        }
    }

    private void setVisitDoneThisMonth() {
        if (ChwChildDao.hasDueTodayVaccines(childBaseEntityId) || ChwChildDao.hasDueAlerts(childBaseEntityId)) {
            getView().setVisitAboveTwentyFourView();
        } else {
            getView().setNoButtonView();
        }
    }

    @Override
    public void updateFamilyMemberServiceDue(String serviceDueStatus) {
        if (ChwApplication.getApplicationFlavor().includeCurrentChild()) {
            super.updateFamilyMemberServiceDue(serviceDueStatus);
        } else {
            if (getView() != null) {
                 if (serviceDueStatus.equalsIgnoreCase(CoreConstants.FamilyServiceType.DUE.name())) {
                    getView().setFamilyHasServiceDue();
                } else if (serviceDueStatus.equalsIgnoreCase(CoreConstants.FamilyServiceType.OVERDUE.name())) {
                    getView().setFamilyHasServiceOverdue();
                } else if (serviceDueStatus.equalsIgnoreCase(CoreConstants.FamilyServiceType.NOTHING.name()) && ChwChildDao.hasActiveSchedule(childBaseEntityId)) {
                    getView().setFamilyHasNothingElseDue();
                } else {
                    getView().setFamilyHasNothingDue();
                }
            }
        }

    }

    @Override
    public void updateChildVisit(ChildVisit childVisit) {
        if (!ChwApplication.getApplicationFlavor().showNoDueVaccineView()) {
            super.updateChildVisit(childVisit);
        } else {
            if (childVisit != null) {
                if (childVisit.getVisitStatus().equalsIgnoreCase(CoreConstants.VisitType.DUE.name())) {
                    setDueView();
                }
                if (childVisit.getVisitStatus().equalsIgnoreCase(CoreConstants.VisitType.OVERDUE.name())) {
                    getView().setVisitButtonOverdueStatus();
                }
                if (childVisit.getVisitStatus().equalsIgnoreCase(CoreConstants.VisitType.LESS_TWENTY_FOUR.name())) {
                    getView().setVisitLessTwentyFourView(childVisit.getLastVisitMonthName());
                }
                if (childVisit.getVisitStatus().equalsIgnoreCase(CoreConstants.VisitType.VISIT_THIS_MONTH.name())) {
                    setVisitDoneThisMonth();
                }
                if (childVisit.getVisitStatus().equalsIgnoreCase(CoreConstants.VisitType.NOT_VISIT_THIS_MONTH.name())) {
                    boolean withinEditPeriod = isWithinEditPeriod(childVisit.getLastNotVisitDate());
                    getView().setVisitNotDoneThisMonth(withinEditPeriod);
                }
                if (childVisit.getLastVisitTime() != 0) {
                    getView().setLastVisitRowView(childVisit.getLastVisitDays());
                }
                if (!childVisit.getVisitStatus().equalsIgnoreCase(CoreConstants.VisitType.NOT_VISIT_THIS_MONTH.name()) && childVisit.getLastVisitTime() != 0) {
                    getView().enableEdit(new Period(new DateTime(childVisit.getLastVisitTime()), DateTime.now()).getHours() <= 24);
                }
            }
        }

    }

}
