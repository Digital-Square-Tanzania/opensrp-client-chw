package org.smartregister.chw.util;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.mothermentor.repository.MotherMentorChildEidRepository;
import org.smartregister.domain.Client;
import org.smartregister.domain.Event;
import org.smartregister.domain.db.EventClient;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.helper.ECSyncHelper;

import java.util.Collections;

/** Persists the library's enrollment event and processes only that child's event. */
public final class MotherMentorChildEidAutoEnrollment {
    private MotherMentorChildEidAutoEnrollment() { }

    public static void process(SQLiteDatabase db, String motherId, ECSyncHelper sync,
            ClientProcessorForJava processor) throws Exception {
        MotherMentorChildEidRepository.EnrollmentPlan plan =
                MotherMentorChildEidRepository.getPendingEnrollmentPlan(db, motherId);
        if (plan == null) return;
        JSONObject clientJson = sync.getClient(plan.getChildId());
        if (clientJson == null) return;
        Client client = CoreJsonFormUtils.gson.fromJson(clientJson.toString(), Client.class);
        if (client == null || !plan.getChildId().equals(client.getBaseEntityId())) return;
        // Resume the actual queued event after interruption, including its immutable source snapshot.
        JSONObject eventJson = plan.getStoredEventJson(db);
        if (eventJson == null) {
            eventJson = new JSONObject(CoreJsonFormUtils.gson.toJson(plan.toEvent()));
            sync.addEvent(plan.getChildId(), eventJson);
            eventJson = plan.getStoredEventJson(db);
            if (eventJson == null) {
                throw new IllegalStateException("Child EID enrollment event was not persisted");
            }
        }
        Event event = CoreJsonFormUtils.gson.fromJson(eventJson.toString(), Event.class);
        processor.processClient(Collections.singletonList(new EventClient(event, client)));
    }
}
