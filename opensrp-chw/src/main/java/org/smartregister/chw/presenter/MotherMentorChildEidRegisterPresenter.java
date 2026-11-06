package org.smartregister.chw.presenter;

import androidx.annotation.NonNull;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.util.MotherMentorChildEidFormUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.opd.contract.OpdRegisterActivityContract;
import org.smartregister.opd.pojo.OpdEventClient;
import org.smartregister.opd.pojo.RegisterParams;
import java.lang.ref.WeakReference;
import java.util.List;
import timber.log.Timber;

/** Same registration callback pattern as TB contacts, before opening child enrollment. */
public class MotherMentorChildEidRegisterPresenter extends ChwAllClientRegisterPresenter {
    public interface Callback { void onChildRegistered(String childId); }
    private final WeakReference<Callback> callback;
    private String pendingChildId;

    public MotherMentorChildEidRegisterPresenter(OpdRegisterActivityContract.View view,
                                               OpdRegisterActivityContract.Model model, Callback callback) {
        super(view, model);
        this.callback = new WeakReference<>(callback);
    }

    @Override
    public void saveForm(String json, @NonNull RegisterParams params) {
        try {
            List<OpdEventClient> clients = model.processRegistration(json, params.getFormTag());
            pendingChildId = registeredMemberId(clients);
            if (pendingChildId == null) throw new IllegalArgumentException("No registered child identity");
            if (!eligibleRegisteredMember(clients)) throw new IllegalArgumentException("Child must be younger than 18 months with a known date of birth");
            interactor.saveRegistration(clients, json, params, this);
        } catch (Exception e) {
            pendingChildId = null;
            Timber.e(e, "Unable to register child client");
            if (callback.get() != null) callback.get().onChildRegistered(null);
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
                return !Boolean.TRUE.equals(item.getClient().getBirthdateApprox())
                        && MotherMentorChildEidFormUtils.isEligibleChild(dob);
            }
        }
        return false;
    }

    @Override
    public void onRegistrationSaved(boolean edit) {
        super.onRegistrationSaved(edit);
        String id = pendingChildId;
        pendingChildId = null;
        if (callback.get() != null) callback.get().onChildRegistered(id);
    }
}
