package org.smartregister.chw.presenter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONObject;
import org.smartregister.chw.core.presenter.CoreAllClientsRegisterPresenter;
import org.smartregister.chw.interactor.ChwAllClientsRegisterInteractor;
import org.smartregister.domain.FetchStatus;
import org.smartregister.opd.contract.OpdRegisterActivityContract;
import org.smartregister.opd.pojo.OpdEventClient;
import org.smartregister.opd.pojo.RegisterParams;

import java.util.List;

import timber.log.Timber;

public class ChwAllClientRegisterPresenter extends CoreAllClientsRegisterPresenter {

    public ChwAllClientRegisterPresenter(OpdRegisterActivityContract.View view, OpdRegisterActivityContract.Model model) {
        super(view, model);
    }

    @Override
    public void saveForm(String jsonString, @NonNull RegisterParams registerParams) {
        try {
            List<OpdEventClient> opdEventClientList = model.processRegistration(jsonString, registerParams.getFormTag());
            if (opdEventClientList == null || opdEventClientList.isEmpty()) {
                return;
            }
            interactor.saveRegistration(opdEventClientList, jsonString, registerParams, this);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @NonNull
    @Override
    public OpdRegisterActivityContract.Interactor createInteractor() {
        return new ChwAllClientsRegisterInteractor();
    }

    @Override
    public void onRegistrationSaved(boolean inEditMode) {
        if (getView() != null) {
            getView().refreshList(FetchStatus.fetched);
            getView().hideProgressDialog();
        }
    }

    @Nullable
    private OpdRegisterActivityContract.View getView() {
        if (viewReference != null) {
            return viewReference.get();
        } else {
            return null;
        }
    }

    @Override
    public void onNoUniqueId() {
        if (getView() != null)
            getView().displayShortToast(org.smartregister.family.R.string.no_unique_id);
    }

    @Override
    public void onUniqueIdFetched(Triple<String, String, String> triple, String entityId) {
        if (getView() != null) {
            try {
                startForm(triple.getLeft(), entityId, triple.getMiddle(), triple.getRight());
            } catch (Exception e) {
                Timber.e(e);
                getView().displayToast(org.smartregister.family.R.string.error_unable_to_start_form);
            }
        }
    }


    public void startForm(String formName, String entityId, String metadata, String currentLocationId) throws Exception {

        if (StringUtils.isBlank(entityId)) {
            Triple<String, String, String> triple = Triple.of(formName, metadata, currentLocationId);
            interactor.getNextUniqueId(triple, this);
            return;
        }

        JSONObject form = model.getFormAsJson(formName, entityId, currentLocationId);
        if (getView() != null)
            getView().startFormActivity(form);

    }
}
