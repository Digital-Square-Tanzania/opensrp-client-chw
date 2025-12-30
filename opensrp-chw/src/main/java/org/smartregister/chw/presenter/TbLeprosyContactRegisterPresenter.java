package org.smartregister.chw.presenter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.opd.contract.OpdRegisterActivityContract;
import org.smartregister.opd.pojo.OpdEventClient;
import org.smartregister.opd.pojo.RegisterParams;

import java.lang.ref.WeakReference;
import java.util.List;

import timber.log.Timber;

/**
 * Presenter that exposes the generated contact base entity ID once registration is saved.
 */
public class TbLeprosyContactRegisterPresenter extends ChwAllClientRegisterPresenter {

    private final WeakReference<ContactRegistrationCallback> callbackReference;
    @Nullable
    private String pendingContactBaseEntityId;
    public TbLeprosyContactRegisterPresenter(OpdRegisterActivityContract.View view,
                                             OpdRegisterActivityContract.Model model,
                                             ContactRegistrationCallback callback) {
        super(view, model);
        this.callbackReference = new WeakReference<>(callback);
    }

    @Override
    public void saveForm(String jsonString, @NonNull RegisterParams registerParams) {
        try {
            List<OpdEventClient> opdEventClientList = model.processRegistration(jsonString, registerParams.getFormTag());
            if (opdEventClientList == null || opdEventClientList.isEmpty()) {
                pendingContactBaseEntityId = null;
                return;
            }

            pendingContactBaseEntityId = extractContactBaseEntityId(opdEventClientList);
            interactor.saveRegistration(opdEventClientList, jsonString, registerParams, this);
        } catch (Exception e) {
            pendingContactBaseEntityId = null;
            Timber.e(e);
        }
    }

    @Override
    public void onRegistrationSaved(boolean inEditMode) {
        super.onRegistrationSaved(inEditMode);
        ContactRegistrationCallback callback = callbackReference.get();
        if (callback != null) {
            callback.onContactBaseEntityIdGenerated(pendingContactBaseEntityId);
        }
        pendingContactBaseEntityId = null;
    }

    @Nullable
    private String extractContactBaseEntityId(@NonNull List<OpdEventClient> opdEventClientList) {
        for (OpdEventClient opdEventClient : opdEventClientList) {
            Event event = opdEventClient != null ? opdEventClient.getEvent() : null;
            if (event != null && CoreConstants.EventType.FAMILY_MEMBER_REGISTRATION.equals(event.getEventType())) {
                String baseEntityId = event.getBaseEntityId();
                if (StringUtils.isNotBlank(baseEntityId)) {
                    return baseEntityId;
                }
            }
        }

        return null;
    }

    public interface ContactRegistrationCallback {
        void onContactBaseEntityIdGenerated(@Nullable String contactBaseEntityId);
    }
}
