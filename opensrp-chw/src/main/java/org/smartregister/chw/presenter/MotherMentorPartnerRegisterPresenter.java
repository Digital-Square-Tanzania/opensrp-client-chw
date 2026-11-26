package org.smartregister.chw.presenter;

import androidx.annotation.NonNull;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.util.MotherMentorPartnerFormUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.opd.contract.OpdRegisterActivityContract;
import org.smartregister.opd.pojo.OpdEventClient;
import org.smartregister.opd.pojo.RegisterParams;
import java.lang.ref.WeakReference;
import java.util.List;
import timber.log.Timber;

/** Same registration callback pattern as TB contacts, before opening partner enrollment. */
public class MotherMentorPartnerRegisterPresenter extends ChwAllClientRegisterPresenter {
    public interface Callback { void onPartnerRegistered(String partnerId); }
    private final WeakReference<Callback> callback;
    private String pendingPartnerId;

    public MotherMentorPartnerRegisterPresenter(OpdRegisterActivityContract.View view,
                                               OpdRegisterActivityContract.Model model, Callback callback) {
        super(view, model);
        this.callback = new WeakReference<>(callback);
    }

    @Override
    public void saveForm(String json, @NonNull RegisterParams params) {
        try {
            List<OpdEventClient> clients = model.processRegistration(json, params.getFormTag());
            pendingPartnerId = registeredMemberId(clients);
            if (pendingPartnerId == null) throw new IllegalArgumentException("No registered partner identity");
            if (!eligibleRegisteredMember(clients)) throw new IllegalArgumentException("Partner must be male and aged 15 or older");
            interactor.saveRegistration(clients, json, params, this);
        } catch (Exception e) {
            pendingPartnerId = null;
            Timber.e(e, "Unable to register partner client");
            if (callback.get() != null) callback.get().onPartnerRegistered(null);
        }
    }

    public static String registeredMemberId(List<OpdEventClient> clients) {
        if (clients != null) for (OpdEventClient client : clients) {
            Event event = client == null ? null : client.getEvent();
            if (event != null && CoreConstants.EventType.FAMILY_MEMBER_REGISTRATION.equals(event.getEventType())) {
                return event.getBaseEntityId();
            }
        }
        return null;
    }

    public static boolean eligibleRegisteredMember(List<OpdEventClient> clients) {
        String memberId = registeredMemberId(clients);
        if (memberId == null) return false;
        for (OpdEventClient item : clients) {
            if (item != null && item.getEvent() != null && memberId.equals(item.getEvent().getBaseEntityId())
                    && item.getClient() != null && item.getClient().getBirthdate() != null) {
                String dob = new org.joda.time.DateTime(item.getClient().getBirthdate()).toLocalDate().toString();
                return MotherMentorPartnerFormUtils.isEligiblePartner(item.getClient().getGender(), dob);
            }
        }
        return false;
    }

    @Override
    public void onRegistrationSaved(boolean edit) {
        super.onRegistrationSaved(edit);
        String id = pendingPartnerId;
        pendingPartnerId = null;
        if (callback.get() != null) callback.get().onPartnerRegistered(id);
    }
}
