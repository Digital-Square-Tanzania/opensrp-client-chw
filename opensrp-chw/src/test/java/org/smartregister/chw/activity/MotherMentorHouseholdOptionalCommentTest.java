package org.smartregister.chw.activity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.domain.Visit;
import org.smartregister.chw.mothermentor.util.DBConstants;

import java.util.LinkedHashMap;
import java.util.Map;

public class MotherMentorHouseholdOptionalCommentTest extends BaseUnitTest {
    @Test
    public void householdVisitCanBeProcessedWithoutComment() throws Exception {
        Map<String, String> values = completeHouseholdService();
        assertTrue(isComplete(values));
        values.put(DBConstants.KEY.COMMENTS, "");
        assertTrue(isComplete(values));
        values.put(DBConstants.KEY.COMMENTS, "Household follow-up information");
        assertTrue(isComplete(values));
    }

    @Test
    public void otherHouseholdSectionsRemainRequiredForProcessing() throws Exception {
        for (String required : completeHouseholdService().keySet()) {
            Map<String, String> values = completeHouseholdService();
            values.put(DBConstants.KEY.COMMENTS, "Household follow-up information");
            values.remove(required);
            assertFalse(required, isComplete(values));
        }
        assertFalse(isComplete(new LinkedHashMap<>()));
    }

    private Map<String, String> completeHouseholdService() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(DBConstants.KEY.PURPOSE_OF_VISIT, "counselling");
        values.put(DBConstants.KEY.PARTICIPANTS, "mother");
        values.put(DBConstants.KEY.TOPICS_TAUGHT, "pmtct");
        return values;
    }

    private boolean isComplete(Map<String, String> values) throws Exception {
        MotherMentorHouseholdProfileActivity activity =
                Robolectric.buildActivity(MotherMentorHouseholdProfileActivity.class).get();
        JSONArray observations = new JSONArray();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            observations.put(
                    new JSONObject()
                            .put("fieldCode", entry.getKey())
                            .put("values", new JSONArray().put(entry.getValue())));
        }
        Visit visit = new Visit();
        visit.setJson(new JSONObject().put("obs", observations).toString());
        return ReflectionHelpers.callInstanceMethod(
                activity,
                "hasCompletedHouseholdServiceSections",
                ReflectionHelpers.ClassParameter.from(Visit.class, visit));
    }
}
