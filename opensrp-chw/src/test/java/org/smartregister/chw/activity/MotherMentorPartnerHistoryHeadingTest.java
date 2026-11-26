package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.interactor.MotherMentorMedicalHistoryInteractor;
import org.smartregister.chw.mothermentor.dao.MotherMentorPartnerFollowUpDao;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherClosureRepository;
import org.smartregister.chw.mothermentor.repository.MotherMentorMotherResultEvent;
import org.smartregister.chw.mothermentor.repository.MotherMentorPartnerClosureRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorPartnerClosure;
import org.smartregister.util.LangUtils;
import org.smartregister.util.Session;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

@Config(sdk = 28)
public class MotherMentorPartnerHistoryHeadingTest extends BaseUnitTest {
    private static final String LEGACY_EVENT = Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES;
    private static final String CLIENT_ID = "synthetic-history-client";
    private TimeZone previousTimeZone;

    @Before
    public void setUpHistory() {
        previousTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Session session = ReflectionHelpers.getField(
                CoreLibrary.getInstance().context().userService(), "session");
        session.start(session.lengthInMilliseconds()).setPassword(UUID.randomUUID().toString());
    }

    @After
    public void tearDownHistory() {
        TimeZone.setDefault(previousTimeZone);
        CoreLibrary.getInstance().context().userService().logoutSession();
    }

    @Test
    @Config(qualifiers = "en")
    public void historyHeadingsAndSavedRowsInEnglish() throws Exception {
        LangUtils.saveLanguage(RuntimeEnvironment.getApplication(), "en");
        assertHistoryHeadings("Partner Visit", "Mother Mentor Visit");
    }

    @Test
    @Config(qualifiers = "sw")
    public void historyHeadingsAndSavedRowsInKiswahili() throws Exception {
        LangUtils.saveLanguage(RuntimeEnvironment.getApplication(), "sw");
        assertHistoryHeadings("Tembeleo la Mwenzi", "Tembeleo la Mama Kinara");
    }

    private void assertHistoryHeadings(String partnerHeading, String maternalHeading) throws Exception {
        String partnerEvent = MotherMentorFollowUpType.PARTNER.getEventType();
        Visit older = visit(partnerEvent, "2026-11-08T09:15:10Z", "Synthetic first partner answer");
        Visit legacy = visit(LEGACY_EVENT, "2026-11-09T10:20:30Z", "Synthetic legacy partner answer");
        legacy.setJson(new JSONObject().put("obs", new JSONArray().put(new JSONObject()
                .put("fieldCode", DBConstants.KEY.FOLLOW_UP_STATUS)
                .put("values", new JSONArray().put("found")))).toString());
        Visit newer = visit(partnerEvent, "2026-11-10T11:25:40Z", "Synthetic latest partner answer");

        try (MockedStatic<MotherMentorPartnerClosureRepository> closure =
                     mockStatic(MotherMentorPartnerClosureRepository.class);
             MockedStatic<MotherMentorPartnerFollowUpDao> partnerContext =
                     mockStatic(MotherMentorPartnerFollowUpDao.class);
             ActivityController<MotherMentorMedicalHistoryActivity> history =
                     openHistory(MotherMentorFollowUpType.PARTNER)) {
            closure.when(() -> MotherMentorPartnerClosureRepository.getState(any(), eq(CLIENT_ID), isNull()))
                    .thenReturn(MotherMentorPartnerClosure.active());
            LinearLayout cards = renderHistory(history.get(), partnerHeading, Arrays.asList(older, legacy, newer));
            assertEquals(3, cards.getChildCount());
            assertRow(history.get(), cards.getChildAt(0), partnerHeading,
                    "10-11-2026 11:25:40", "Synthetic latest partner answer", "3");
            assertRow(history.get(), cards.getChildAt(1), partnerHeading,
                    "09-11-2026 10:20:30", "Synthetic legacy partner answer", "2");
            assertRow(history.get(), cards.getChildAt(2), partnerHeading,
                    "08-11-2026 09:15:10", "Synthetic first partner answer", "1");
        }

        Visit mother = visit(LEGACY_EVENT, "2026-11-07T08:10:00Z", "Synthetic maternal answer");
        try (MockedStatic<MotherMentorMotherResultEvent> results =
                     mockStatic(MotherMentorMotherResultEvent.class);
             MockedStatic<MotherMentorMotherClosureRepository> closure =
                     mockStatic(MotherMentorMotherClosureRepository.class);
             ActivityController<MotherMentorMedicalHistoryActivity> history = openHistory(null)) {
            LinearLayout cards = renderHistory(history.get(), maternalHeading, Collections.singletonList(mother));
            assertEquals(1, cards.getChildCount());
            assertRow(history.get(), cards.getChildAt(0), maternalHeading,
                    "07-11-2026 08:10:00", "Synthetic maternal answer", "1");
        }
    }

    private ActivityController<MotherMentorMedicalHistoryActivity> openHistory(MotherMentorFollowUpType type) {
        Activity source = Robolectric.buildActivity(Activity.class).get();
        MemberObject member = new MemberObject();
        member.setBaseEntityId(CLIENT_ID);
        member.setFirstName("Synthetic");
        member.setLastName("History Client");
        MotherMentorMedicalHistoryActivity.startMe(source, member, type);
        Intent intent = Shadows.shadowOf(source).getNextStartedActivity();

        try (MockedConstruction<MotherMentorMedicalHistoryInteractor> ignored =
                     mockConstruction(MotherMentorMedicalHistoryInteractor.class)) {
            return Robolectric.buildActivity(MotherMentorMedicalHistoryActivity.class, intent).create();
        }
    }

    private LinearLayout renderHistory(MotherMentorMedicalHistoryActivity activity,
            String heading, List<Visit> visits) {
        View history = activity.renderView(visits);
        TextView section = history.findViewById(
                org.smartregister.chw.core.R.id.customFontTextViewHealthFacilityVisitTitle);
        assertEquals(heading, section.getText().toString());
        return history.findViewById(org.smartregister.chw.core.R.id.linearLayoutHealthFacilityVisitDetails);
    }

    private void assertRow(Activity activity, View card, String heading, String date,
            String answer, String visitNumber) {
        TextView title = card.findViewById(R.id.type_of_service);
        TextView comment = card.findViewWithTag(DBConstants.KEY.COMMENTS);
        TextView number = card.findViewWithTag(DBConstants.KEY.VISIT_NUMBER);
        assertEquals(heading + " - " + date, title.getText().toString());
        assertEquals(answer, comment.getText().toString().trim().split("\n", 2)[1]);
        assertEquals(activity.getString(R.string.mothermentor_history_visit_number) + "\n" + visitNumber,
                number.getText().toString().trim());
    }

    private Visit visit(String event, String date, String answer) {
        Visit visit = new Visit();
        visit.setBaseEntityId(CLIENT_ID);
        visit.setVisitType(event);
        visit.setDate(Date.from(Instant.parse(date)));
        visit.setProcessed(true);
        VisitDetail comment = new VisitDetail();
        comment.setVisitKey(DBConstants.KEY.COMMENTS);
        comment.setDetails(answer);
        comment.setHumanReadable(answer);
        visit.setVisitDetails(new LinkedHashMap<>());
        visit.getVisitDetails().put(DBConstants.KEY.COMMENTS, Collections.singletonList(comment));
        return visit;
    }
}
