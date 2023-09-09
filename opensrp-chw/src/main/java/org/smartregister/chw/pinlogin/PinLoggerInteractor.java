package org.smartregister.chw.pinlogin;

import androidx.annotation.NonNull;

import org.joda.time.DateTime;
import org.smartregister.AllConstants;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.contract.LoginJobScheduler;
import org.smartregister.chw.contract.PinLoginContract;
import org.smartregister.chw.interactor.LoginJobSchedulerProvider;
import org.smartregister.domain.TimeStatus;
import org.smartregister.service.UserService;

import timber.log.Timber;

public class PinLoggerInteractor implements PinLoginContract.Interactor {

    @Override
    public void authenticateUser(String userName, String password, @NonNull PinLogger.EventListener eventListener) {
        eventListener.onEvent("Attempting to authenticate");
        boolean isAuthenticated = getUserService().isUserInValidGroup(userName, password);
        if (!isAuthenticated) {
            eventListener.onEvent("User authentication failed");
            eventListener.onError(new Exception("Authentication failed"));
        } else if (isAuthenticated && (!AllConstants.TIME_CHECK || TimeStatus.OK.equals(getUserService().validateStoredServerTimeZone()))) {
            eventListener.onEvent("User authenticated");
            PinLoginUtil.getPinLogger().updateLastLogin();
            cleanUpLogin(userName, password, eventListener);
        }
    }

    private void cleanUpLogin(String userName, String password, @NonNull PinLogger.EventListener eventListener) {
        getUserService().localLogin(userName, password);
        eventListener.onSuccess();
        CoreLibrary.getInstance().initP2pLibrary(userName);

        new Thread(() -> {
            try {
                LoginJobScheduler scheduler = new LoginJobSchedulerProvider();

                eventListener.onEvent("Starting DrishtiSyncScheduler " + DateTime.now());

                scheduler.scheduleJobsImmediately();
                scheduler.scheduleJobsPeriodically();

                eventListener.onEvent("Started DrishtiSyncScheduler " + DateTime.now());

                CoreLibrary.getInstance().context().getUniqueIdRepository().releaseReservedIds();
            } catch (Exception e) {
                Timber.e(e);
            }
        }).start();
    }

    private UserService getUserService() {
        return CoreLibrary.getInstance().context().userService();
    }
}
