package org.smartregister.chw.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class MotherMentorHouseholdPurposeHistoryTest {
    @Test
    public void removedAndRenamedPurposesRemainLocalizedWithoutChangingRawAnswers() {
        for (String language : new String[] {"en", "sw"}) {
            Context base = RuntimeEnvironment.getApplication();
            Configuration config = new Configuration(base.getResources().getConfiguration());
            config.setLocale(new Locale(language));
            Context context = base.createConfigurationContext(config);
            for (String raw : new String[] {"routine_follow_up", "ufuatiliaji", "new_registration", "usajili"}) {
                Visit visit = new Visit();
                visit.setVisitType(Constants.EVENT_TYPE.MOTHER_MENTOR_SERVICES);
                VisitDetail detail = new VisitDetail();
                detail.setDetails(raw);
                detail.setHumanReadable(raw);
                Map<String, List<VisitDetail>> details = new HashMap<>();
                details.put(DBConstants.KEY.PURPOSE_OF_VISIT, Collections.singletonList(detail));
                visit.setVisitDetails(details);
                List<MotherMentorHistoryMapper.Row> rows = new MotherMentorHistoryMapper(context).map(visit);
                MotherMentorHistoryMapper.Row purpose = null;
                for (MotherMentorHistoryMapper.Row row : rows) {
                    if (DBConstants.KEY.PURPOSE_OF_VISIT.equals(row.getKey())) purpose = row;
                }
                assertNotNull(purpose);
                boolean legacy = "new_registration".equals(raw) || "usajili".equals(raw);
                String expected = legacy
                        ? ("sw".equals(language) ? "Usajili mpya (zamani)" : "New registration (legacy)")
                        : ("sw".equals(language) ? "Kufuatilia maendeleo ya kaya" : "Follow up household progress");
                assertEquals(Collections.singletonList(expected), purpose.getValues());
                assertEquals(raw, detail.getDetails());
            }
        }
    }
}
