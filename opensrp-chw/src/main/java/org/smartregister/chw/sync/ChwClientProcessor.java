package org.smartregister.chw.sync;


import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.sync.CoreClientProcessor;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.chw.service.ChildAlertService;
import org.smartregister.chw.util.Constants;
import org.smartregister.domain.Event;
import org.smartregister.domain.Obs;
import org.smartregister.domain.db.EventClient;
import org.smartregister.domain.jsonmapping.ClientClassification;
import org.smartregister.domain.jsonmapping.Table;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.sync.ClientProcessorForJava;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ChwClientProcessor extends CoreClientProcessor {

    private ChwClientProcessor(Context context) {
        super(context);
    }

    public static ClientProcessorForJava getInstance(Context context) {
        if (instance == null) {
            instance = new ChwClientProcessor(context);
        }
        return instance;
    }

    @Override
    public void processEvents(ClientClassification clientClassification, Table vaccineTable, Table serviceTable, EventClient eventClient, Event event, String eventType) throws Exception {
        if (eventClient != null && eventClient.getEvent() != null) {
            String baseEntityID = eventClient.getEvent().getBaseEntityId();

            switch (eventType) {
                case CoreConstants.EventType.REMOVE_FAMILY:
                    ChwApplication.getInstance().getScheduleRepository().deleteSchedulesByFamilyEntityID(baseEntityID);
                case CoreConstants.EventType.REMOVE_MEMBER:
                    ChwApplication.getInstance().getScheduleRepository().deleteSchedulesByEntityID(baseEntityID);
                case CoreConstants.EventType.REMOVE_CHILD:
                    if (!CoreLibrary.getInstance().isPeerToPeerProcessing() && !SyncStatusBroadcastReceiver.getInstance().isSyncing()) {
                        ChwApplication.getInstance().getScheduleRepository().deleteSchedulesByEntityID(baseEntityID);
                    }
                    break;
                default:
                    break;
            }
        }

        super.processEvents(clientClassification, vaccineTable, serviceTable, eventClient, event, eventType);
        if (eventClient != null && eventClient.getEvent() != null) {
            String baseEntityID = eventClient.getEvent().getBaseEntityId();
            switch (eventType) {
                case CoreConstants.EventType.CHILD_HOME_VISIT:
                case CoreConstants.EventType.CHILD_VISIT_NOT_DONE:
                case CoreConstants.EventType.CHILD_REGISTRATION:
                case CoreConstants.EventType.UPDATE_CHILD_REGISTRATION:
                    if (!CoreLibrary.getInstance().isPeerToPeerProcessing() && !SyncStatusBroadcastReceiver.getInstance().isSyncing()) {
                        ChildAlertService.updateAlerts(baseEntityID);
                    }
                    break;
                case Constants.Events.CBHS_FOLLOWUP:
                case Constants.Events.MOTHER_CHAMPION_FOLLOWUP:
                case Constants.Events.ANC_FIRST_FACILITY_VISIT:
                case Constants.Events.ANC_RECURRING_FACILITY_VISIT:
                case Constants.Events.AGYW_STRUCTURAL_SERVICES:
                case Constants.Events.AGYW_BEHAVIORAL_SERVICES:
                case Constants.Events.AGYW_BIO_MEDICAL_SERVICES:
                case Constants.Events.KVP_PREP_FOLLOWUP_VISIT:
                case org.smartregister.chw.malaria.util.Constants.EVENT_TYPE.ICCM_SERVICES_VISIT:
                    if (eventClient.getEvent() == null) {
                        return;
                    }
                    processVisitEvent(eventClient);
                    processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                    break;
                default:
                    break;
            }
        }

        if (!CoreLibrary.getInstance().isPeerToPeerProcessing() && !SyncStatusBroadcastReceiver.getInstance().isSyncing()) {
            ChwScheduleTaskExecutor.getInstance().execute(event.getBaseEntityId(), event.getEventType(), event.getEventDate().toDate());
        }
    }

    private void processVisitEvent(EventClient eventClient) {
        try {
            NCUtils.processHomeVisit(eventClient);
        } catch (Exception e) {
            String formID = (eventClient != null && eventClient.getEvent() != null) ? eventClient.getEvent().getFormSubmissionId() : "no form id";
            Timber.e("Form id " + formID + ". " + e.toString());
        }
    }


    @Override
    protected String getHumanReadableConceptResponse(String value, Object object) {
        try {
            if (StringUtils.isBlank(value) || (object != null && !(object instanceof Obs))) {
                return value;
            }
            // Skip human readable values and just get values which would aid in translations
            final String VALUES = "values";
            List values = new ArrayList();

            Object valueObject = getValue(object, VALUES);
            if (valueObject instanceof List) {
                values = (List) valueObject;
            }
            if (object == null || values.isEmpty()) {
                return value;
            }

            return values.size() == 1 ? values.get(0).toString() : values.toString();

        } catch (Exception e) {
            Timber.e(e);
        }
        return value;
    }
}
