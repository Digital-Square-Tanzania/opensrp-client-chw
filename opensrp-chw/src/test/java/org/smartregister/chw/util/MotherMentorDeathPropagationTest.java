package org.smartregister.chw.util;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.app.Application;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.mothermentor.dao.MotherMentorChildEidDao;
import org.smartregister.chw.mothermentor.dao.MotherMentorDao;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.domain.Event;
import org.smartregister.util.JsonFormUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorDeathPropagationTest {
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
                + "{\"fieldCode\":\"mtoto_tarehe_alifariki\",\"values\":[\"02-06-2025\"]}]}";
        return JsonFormUtils.gson.fromJson(json, Event.class);
    }
}
