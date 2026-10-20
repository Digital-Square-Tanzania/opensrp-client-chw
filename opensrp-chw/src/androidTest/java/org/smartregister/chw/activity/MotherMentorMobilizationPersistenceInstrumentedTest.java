package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.sync.CoreClientProcessor;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.domain.Event;
import org.smartregister.util.JsonFormUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

/** Verifies the actual packaged core processor, including repeated processing of a session. */
@RunWith(AndroidJUnit4.class)
public class MotherMentorMobilizationPersistenceInstrumentedTest {
    @Test
    public void reprocessingRetainsReferralCountsAndServices() throws Exception {
        verifyProcessing("1", "2", "3", "hiv_testing");
    }

    @Test
    public void zeroReferralsRemainKnownZeros() throws Exception {
        verifyProcessing("0", "0", "0", null);
    }

    private void verifyProcessing(String male, String female, String total, String service)
            throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Constructor<CoreClientProcessor> constructor =
                CoreClientProcessor.class.getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);
        CoreClientProcessor processor = constructor.newInstance(context);
        Method process =
                CoreClientProcessor.class.getDeclaredMethod(
                        "processMotherMentorMobilizationEvent", Event.class);
        process.setAccessible(true);

        String id = UUID.randomUUID().toString();
        JSONArray observations = new JSONArray();
        addObservation(observations, DBConstants.KEY.SH_TAREHE, "18-10-2026");
        addObservation(observations, DBConstants.KEY.SH_AINA, "baraza");
        addObservation(observations, DBConstants.KEY.SH_MADA, "pmtct");
        addObservation(observations, DBConstants.KEY.REFERRED_MALE, male);
        addObservation(observations, DBConstants.KEY.REFERRED_FEMALE, female);
        addObservation(observations, DBConstants.KEY.REFERRED_TOTAL, total);
        if (service != null) {
            addObservation(observations, DBConstants.KEY.FACILITY_REFERRAL_SERVICES, service);
        }
        JSONObject source = new JSONObject();
        source.put(DBConstants.KEY.EVENT_BASE_ENTITY_ID, id);
        source.put(DBConstants.KEY.EVENT_TYPE, Constants.EVENT_TYPE.MOTHER_MENTOR_MOBILIZATION);
        source.put("obs", observations);
        Event event = JsonFormUtils.gson.fromJson(source.toString(), Event.class);

        SQLiteDatabase db = ChwApplication.getInstance().getRepository().getWritableDatabase();
        db.beginTransaction();
        try {
            for (int attempt = 0; attempt < 2; attempt++) {
                process.invoke(processor, event);
                try (Cursor cursor =
                        db.query(
                                Constants.TABLES.MOTHERMENTOR_MOBILIZATION,
                                new String[] {
                                    DBConstants.KEY.REFERRED_MALE,
                                    DBConstants.KEY.REFERRED_FEMALE,
                                    DBConstants.KEY.REFERRED_TOTAL,
                                    DBConstants.KEY.FACILITY_REFERRAL_SERVICES
                                },
                                DBConstants.KEY.BASE_ENTITY_ID + " = ?",
                                new String[] {id},
                                null,
                                null,
                                null)) {
                    assertTrue(cursor.moveToFirst());
                    assertEquals(male, cursor.getString(0));
                    assertEquals(female, cursor.getString(1));
                    assertEquals(total, cursor.getString(2));
                    if (service == null) assertTrue(cursor.isNull(3));
                    else assertEquals("[" + service + "]", cursor.getString(3));
                }
            }
        } finally {
            // Leave the user's database unchanged, including if an assertion fails.
            db.endTransaction();
        }
    }

    private static void addObservation(JSONArray observations, String key, String value)
            throws Exception {
        observations.put(
                new JSONObject()
                        .put("fieldCode", key)
                        .put("formSubmissionField", key)
                        .put("values", new JSONArray().put(value)));
    }
}
