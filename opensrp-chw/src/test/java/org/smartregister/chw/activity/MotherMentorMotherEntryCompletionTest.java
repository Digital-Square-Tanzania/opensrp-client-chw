package org.smartregister.chw.activity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.ART_INITIATED_BEFORE_VISIT;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.ATTENDANCE_TYPE;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.EDUCATION_PROVIDED;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.FOLLOW_UP_STATUS;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.HAS_BEEN_LINKED_TO_IGA_GROUP;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.HAS_BEEN_LINKED_TO_PSYCHOSOCIAL_SUPPORT_GROUP;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.MAMA_KITUO_KIPYA;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.MAMA_NOT_FOUND_REASON;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.MAMA_SABABU_NYINGINE_TEXT;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.NEW_PMTCT_ART_INITIATION;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.NEXT_APPOINTMENT_DATE;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.REFERRAL_GIVEN;
import static org.smartregister.chw.mothermentor.util.DBConstants.KEY.VISIT_ART_START_DATE;

import android.app.Application;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.mothermentor.domain.Visit;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorMotherEntryCompletionTest {
    @Test
    public void notFoundNeedsItsSpecificReasonAndRequiredDetails() throws Exception {
        assertFalse(complete(ATTENDANCE_TYPE, "repeat", FOLLOW_UP_STATUS, "not_found"));
        assertFalse(complete(ATTENDANCE_TYPE, "repeat", FOLLOW_UP_STATUS, "not_found",
                MAMA_NOT_FOUND_REASON, "other"));
        assertTrue(complete(ATTENDANCE_TYPE, "repeat", FOLLOW_UP_STATUS, "not_found",
                MAMA_NOT_FOUND_REASON, "other", MAMA_SABABU_NYINGINE_TEXT, "Return tomorrow"));
        assertFalse(complete(ATTENDANCE_TYPE, "repeat", FOLLOW_UP_STATUS, "not_found",
                MAMA_NOT_FOUND_REASON, "moved_facility"));
        assertTrue(complete(ATTENDANCE_TYPE, "repeat", FOLLOW_UP_STATUS, "not_found",
                MAMA_NOT_FOUND_REASON, "moved_facility", MAMA_KITUO_KIPYA, "Hospital A"));
        assertTrue(complete(ATTENDANCE_TYPE, "repeat", FOLLOW_UP_STATUS, "not_found",
                MAMA_NOT_FOUND_REASON, "moved_area"));
    }

    @Test
    public void foundDoesNotBypassTheApplicableServiceSections() throws Exception {
        assertFalse(complete(ATTENDANCE_TYPE, "repeat", FOLLOW_UP_STATUS, "found"));
        assertTrue(complete(ATTENDANCE_TYPE, "repeat", FOLLOW_UP_STATUS, "found",
                HAS_BEEN_LINKED_TO_PSYCHOSOCIAL_SUPPORT_GROUP, "no", HAS_BEEN_LINKED_TO_IGA_GROUP, "no",
                EDUCATION_PROVIDED, "no", REFERRAL_GIVEN, "no", NEXT_APPOINTMENT_DATE, "04-12-2026"));
    }

    @Test
    public void newVisitRequiresArtAnswerOnlyWhenNotPreviouslyStarted() throws Exception {
        String[] services = {ATTENDANCE_TYPE, "new", HAS_BEEN_LINKED_TO_PSYCHOSOCIAL_SUPPORT_GROUP, "no",
                HAS_BEEN_LINKED_TO_IGA_GROUP, "no", EDUCATION_PROVIDED, "no", REFERRAL_GIVEN, "no",
                NEXT_APPOINTMENT_DATE, "04-12-2026"};
        assertFalse(complete(services));
        assertTrue(complete(join(services, ART_INITIATED_BEFORE_VISIT, "true")));
        assertTrue(complete(join(services, NEW_PMTCT_ART_INITIATION, "no")));
        assertFalse(complete(join(services, NEW_PMTCT_ART_INITIATION, "yes")));
        assertTrue(complete(join(services, NEW_PMTCT_ART_INITIATION, "yes", VISIT_ART_START_DATE, "04-11-2026")));
    }

    private boolean complete(String... answers) throws Exception {
        MotherMentorProfileActivity activity = Robolectric.buildActivity(MotherMentorProfileActivity.class).get();
        JSONArray observations = new JSONArray();
        for (int i = 0; i < answers.length; i += 2) observations.put(new JSONObject()
                .put("fieldCode", answers[i]).put("values", new JSONArray().put(answers[i + 1])));
        Visit visit = new Visit();
        visit.setJson(new JSONObject().put("obs", observations).toString());
        return ReflectionHelpers.callInstanceMethod(activity, "hasCompletedMotherMentorServiceSections",
                ReflectionHelpers.ClassParameter.from(Visit.class, visit));
    }

    private String[] join(String[] first, String... second) {
        String[] result = new String[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
