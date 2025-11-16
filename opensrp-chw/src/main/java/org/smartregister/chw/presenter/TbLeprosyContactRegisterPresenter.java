package org.smartregister.chw.presenter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.clientandeventmodel.Client;
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

    public interface ContactRegistrationCallback {
        void onContactBaseEntityIdGenerated(@Nullable String contactBaseEntityId);
    }

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
            Client client = opdEventClient != null ? opdEventClient.getClient() : null;
            if (client != null && StringUtils.isNotBlank(client.getBaseEntityId())) {
                return client.getBaseEntityId();
            }
        }

        return null;
    }
}
