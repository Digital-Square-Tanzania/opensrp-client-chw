package org.smartregister.chw.util;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.app.Application;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.LocalDate;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.dao.MotherMentorChildEidDao;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.util.DBConstants.KEY;
import org.smartregister.chw.mothermentor.util.MotherMentorDeceasedVisitUtils;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorJsonFormUtils;
import org.smartregister.chw.mothermentor.util.MotherMentorPartnerFollowUpVisitUtils;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.domain.Event;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.JsonFormUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorDeathPropagationTest {
    @Test
    public void finalizedPartnerDeathUsesTheStandardAdultRemovalForTheLinkedMember() throws Exception {
        Application context = RuntimeEnvironment.getApplication();
        CoreChwApplication application = mock(CoreChwApplication.class, RETURNS_DEEP_STUBS);
        SQLiteDatabase database = mock(SQLiteDatabase.class);
        when(application.getRepository().getWritableDatabase()).thenReturn(database);
        Map<String, String> partner = new HashMap<>();
        partner.put("first_name", "Synthetic");
        partner.put("gender", "Male");
        partner.put("dob", "1990-01-01T00:00:00.000+03:00");
        AtomicBoolean recorded = new AtomicBoolean();

        try (MockedStatic<CoreChwApplication> app = mockStatic(CoreChwApplication.class);
             MockedStatic<MotherMentorLibrary> library = mockStatic(MotherMentorLibrary.class);
             MockedStatic<MotherMentorDao> death = mockStatic(MotherMentorDao.class);
             MockedStatic<MotherMentorChildEidDao> member = mockStatic(MotherMentorChildEidDao.class);
             MockedStatic<MarkClientAsDeceasedHelper> removal = mockStatic(MarkClientAsDeceasedHelper.class)) {
            app.when(CoreChwApplication::getInstance).thenReturn(application);
            MotherMentorLibrary mentor = mock(MotherMentorLibrary.class);
            library.when(MotherMentorLibrary::getInstance).thenReturn(mentor);
            member.when(() -> MotherMentorChildEidDao.person(database, "partner")).thenReturn(partner);
            death.when(() -> MotherMentorDao.hasRecordedDeath("partner")).thenAnswer(call -> recorded.get());
            removal.when(() -> MarkClientAsDeceasedHelper.markAsDeceased(eq(context), eq("partner"),
                    eq("Synthetic"), isNull(), isNull(), eq("Male"), eq("2025-06-02"),
                    eq(false), anyString())).thenAnswer(call -> {
                recorded.set(true);
                return null;
            });

            Event other = partnerVisit("partner", "mother", "other", "2025-06-01");
            Event deceased = partnerVisit("partner", "mother", "deceased", "2025-06-02");
            assertFalse(MotherMentorDeceasedVisitUtils.isDeceased(JsonFormUtils.gson.toJson(other)));
            assertTrue(MotherMentorDeceasedVisitUtils.isDeceased(JsonFormUtils.gson.toJson(deceased)));
            MotherMentorDeathPropagation.process(context, other);
            MotherMentorDeathPropagation.process(context, deceased);
            MotherMentorDeathPropagation.process(context, deceased);

            removal.verify(() -> MarkClientAsDeceasedHelper.markAsDeceased(eq(context), eq("partner"),
                    eq("Synthetic"), isNull(), isNull(), eq("Male"), eq("2025-06-02"),
                    eq(false), anyString()));
            removal.verifyNoMoreInteractions();
            member.verify(() -> MotherMentorChildEidDao.person(database, "partner"), times(2));
            member.verifyNoMoreInteractions();
        }
    }

    static Event partnerVisit(String partnerId, String motherId, String reason, String day) throws Exception {
        JSONObject form;
        try (Scanner input = new Scanner(RuntimeEnvironment.getApplication().getAssets()
                .open("json.form/mothermentor_partner_follow_up_status.json"), "UTF-8")) {
            form = new JSONObject(input.useDelimiter("\\A").next());
        }
        com.vijay.jsonwizard.utils.FormUtils.getFieldFromForm(form, KEY.FOLLOW_UP_STATUS)
                .put("value", "not_found");
        com.vijay.jsonwizard.utils.FormUtils.getFieldFromForm(form, KEY.PARTNER_NOT_FOUND_REASON)
                .put("value", reason);
        com.vijay.jsonwizard.utils.FormUtils.getFieldFromForm(form, KEY.PARTNER_NOT_FOUND_REASON_OTHER)
                .put("value", "other".equals(reason) ? "Synthetic prior explanation" : "");
        MotherMentorFollowUpType type = MotherMentorFollowUpType.PARTNER;
        org.smartregister.clientandeventmodel.Event event = MotherMentorJsonFormUtils.processVisitJsonForm(
                mock(AllSharedPreferences.class), partnerId, type.getEventType(),
                Collections.singletonMap("Follow-up status", form.toString()), MotherMentorFollowUpType.FOLLOW_UP_TABLE);
        event.setFormSubmissionId("synthetic-visit-" + reason);
        event.setEventDate(new LocalDate(day).toDate());
        addObservation(event, KEY.FOLLOW_UP_CATEGORY, type.getValue());
        addObservation(event, KEY.PARTNER_FOLLOW_UP_SCHEMA, MotherMentorPartnerFollowUpVisitUtils.REFERRAL_SCHEMA);
        addObservation(event, KEY.PARTNER_FOLLOW_UP_PHASE, "tracing");
        addObservation(event, KEY.MOTHER_BASE_ENTITY_ID, motherId);
        addObservation(event, KEY.MOTHERMENTOR_VISIT_DATE, day);
        addObservation(event, KEY.PARTNER_FOLLOW_UP_END, "deceased".equals(reason) ? reason : "");
        return JsonFormUtils.gson.fromJson(JsonFormUtils.gson.toJson(event), Event.class);
    }

    private static void addObservation(org.smartregister.clientandeventmodel.Event event, String key, String value) {
        event.addObs(new Obs("concept", "text", key, "", Collections.singletonList(value),
                Collections.emptyList(), null, key));
    }

    @Test
    public void finalizedChildEidDeathTargetsTheChildOnceAndKeepsTheReportedDate() throws Exception {
        Application context = RuntimeEnvironment.getApplication();
        CoreChwApplication application = mock(CoreChwApplication.class, RETURNS_DEEP_STUBS);
        SQLiteDatabase database = mock(SQLiteDatabase.class);
        when(application.getRepository().getWritableDatabase()).thenReturn(database);
        Map<String, String> person = new HashMap<>();
        person.put("first_name", "Synthetic");
        person.put("gender", "Female");
        person.put("dob", "2025-03-20");
        AtomicBoolean recorded = new AtomicBoolean();

        try (MockedStatic<CoreChwApplication> app = mockStatic(CoreChwApplication.class);
             MockedStatic<MotherMentorDao> death = mockStatic(MotherMentorDao.class);
             MockedStatic<MotherMentorChildEidDao> child = mockStatic(MotherMentorChildEidDao.class);
             MockedStatic<MarkClientAsDeceasedHelper> removal = mockStatic(MarkClientAsDeceasedHelper.class)) {
            app.when(CoreChwApplication::getInstance).thenReturn(application);
            child.when(() -> MotherMentorChildEidDao.person(database, "child")).thenReturn(person);
            death.when(() -> MotherMentorDao.hasRecordedDeath("child")).thenAnswer(call -> recorded.get());
            removal.when(() -> MarkClientAsDeceasedHelper.markAsDeceased(eq(context), eq("child"),
                    eq("Synthetic"), isNull(), isNull(), eq("Female"), eq("2025-06-02"),
                    eq(true), anyString())).thenAnswer(call -> {
                recorded.set(true);
                return null;
            });

            Event event = visit("found", "deceased");
            MotherMentorDeathPropagation.process(context, event);
            MotherMentorDeathPropagation.process(context, event);
            MotherMentorDeathPropagation.process(context, visit("not_found", "deceased"));
            MotherMentorDeathPropagation.process(context, visit("found", "alive"));

            removal.verify(() -> MarkClientAsDeceasedHelper.markAsDeceased(eq(context), eq("child"),
                    eq("Synthetic"), isNull(), isNull(), eq("Female"), eq("2025-06-02"),
                    eq(true), anyString()));
            removal.verifyNoMoreInteractions();
            verify(database, times(2)).setTransactionSuccessful();
            verify(database, times(2)).endTransaction();
        }
    }

    private static Event visit(String status, String childStatus) {
        String json = "{\"eventType\":\"" + MotherMentorFollowUpType.CHILD_EID.getEventType()
                + "\",\"baseEntityId\":\"child\",\"formSubmissionId\":\"synthetic-visit\","
                + "\"obs\":[{\"fieldCode\":\"follow_up_status\",\"values\":[\"" + status + "\"]},"
                + "{\"fieldCode\":\"mama_child_status\",\"values\":[\"" + childStatus + "\"]},"
                + "{\"fieldCode\":\"" + KEY.MAMA_TAREHE_MTOTO_ALIFARIKI
                + "\",\"values\":[\"02-06-2025\"]}]}";
        return JsonFormUtils.gson.fromJson(json, Event.class);
    }
}
