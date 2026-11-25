package org.smartregister.chw.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.database.Cursor;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.vijay.jsonwizard.utils.FormUtils;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.R;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.mothermentor.MotherMentorLibrary;
import org.smartregister.chw.mothermentor.actionhelper.MotherMentorIitStatusActionHelper;
import org.smartregister.chw.mothermentor.contract.BaseMotherMentorVisitContract;
import org.smartregister.chw.mothermentor.model.BaseMotherMentorVisitAction;
import org.smartregister.chw.mothermentor.repository.MotherMentorIitRepository;
import org.smartregister.chw.mothermentor.repository.VisitRepository;
import org.smartregister.chw.mothermentor.util.Constants;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.chw.mothermentor.util.MotherMentorMotherVisitUtils;
import org.smartregister.repository.Repository;
import org.smartregister.util.LangUtils;

import java.io.InputStream;
import java.util.Scanner;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class MotherMentorIitStagedClosureTest {
    private static final String CLIENT = "synthetic-iit";
    private static final String IIT_TABLE = Constants.TABLES.MOTHERMENTOR_ENROLL_IIT;
    private static final String STATUS_SECTION = "mothermentor_iit_follow_up_status";
    private static final String EVENT_TYPE = MotherMentorFollowUpType.MOTHER_IIT.getEventType();

    private android.database.sqlite.SQLiteDatabase data;
    private SQLiteDatabase db;
    private MockedStatic<CoreChwApplication> application;
    private MockedStatic<MotherMentorLibrary> library;

    @Before
    public void connectRealPreviewToSyntheticRows() {
        data = android.database.sqlite.SQLiteDatabase.create(null);
        data.execSQL("CREATE TABLE " + IIT_TABLE + " (base_entity_id TEXT, is_closed INTEGER)");
        data.execSQL("CREATE TABLE " + Constants.TABLES.MOTHERMENTOR_ENROLLMENT + " (base_entity_id TEXT)");
        data.execSQL("CREATE TABLE event (baseEntityId TEXT, eventType TEXT, formSubmissionId TEXT, json TEXT)");
        data.execSQL("CREATE TABLE visits (visit_id TEXT, base_entity_id TEXT, visit_type TEXT, "
                + "form_submission_id TEXT, processed INTEGER, visit_json TEXT)");
        data.execSQL("INSERT INTO " + IIT_TABLE + " VALUES (?, 0)", new String[]{CLIENT});

        db = mock(SQLiteDatabase.class);
        when(db.rawQuery(anyString(), any(String[].class)))
                .thenAnswer(query -> data.rawQuery(query.getArgument(0), query.getArgument(1)));
        Repository repository = mock(Repository.class);
        when(repository.getReadableDatabase()).thenReturn(db);
        CoreChwApplication app = mock(CoreChwApplication.class);
        when(app.getRepository()).thenReturn(repository);
        application = mockStatic(CoreChwApplication.class);
        application.when(CoreChwApplication::getInstance).thenReturn(app);

        MotherMentorLibrary mentor = mock(MotherMentorLibrary.class);
        when(mentor.visitRepository()).thenReturn(mock(VisitRepository.class));
        library = mockStatic(MotherMentorLibrary.class);
        library.when(MotherMentorLibrary::getInstance).thenReturn(mentor);
    }

    @After
    public void closeFixtures() {
        DateTimeUtils.setCurrentMillisSystem();
        if (library != null) library.close();
        if (application != null) application.close();
        if (data != null) data.close();
    }

    @Test
    public void englishDeathPromptsWhenReplayDoesNotProjectAnExit() throws Exception {
        checkStagedDeath(false);
    }

    @Test
    public void kiswahiliLinkedDeathPromptsWhenReplayDoesNotProjectAnExit() throws Exception {
        checkStagedDeath(true);
    }

    private void checkStagedDeath(boolean swahili) throws Exception {
        if (swahili) {
            data.execSQL("INSERT INTO " + Constants.TABLES.MOTHERMENTOR_ENROLLMENT
                    + " VALUES (?)", new String[]{CLIENT});
        }
        // The enrollment is on the clinical day with a timestamp ahead of the wall clock.
        DateTime clinicalTime = new DateTime(System.currentTimeMillis()).plusDays(1);
        DateTimeUtils.setCurrentMillisFixed(clinicalTime.getMillis());
        JSONObject enrollment = new JSONObject()
                .put(DBConstants.KEY.EVENT_FORM_SUBMISSION_ID, "episode")
                .put(DBConstants.KEY.EVENT_DATE_TIMESTAMP, clinicalTime.toString());
        data.execSQL("INSERT INTO event VALUES (?, ?, ?, ?)", new String[]{CLIENT,
                Constants.EVENT_TYPE.MOTHER_MENTOR_ENROLL_IIT, "episode", enrollment.toString()});

        VisitActivity visit = activity(swahili);
        BaseMotherMentorVisitAction action = stageStatus(visit, swahili, "deceased");
        String staged = action.getJsonPayload();
        assertEquals("episode", MotherMentorIitRepository.getState(db, CLIENT, null).getEnrollmentId());
        assertFalse(MotherMentorIitRepository.preview(db, CLIENT, null,
                MotherMentorMotherVisitUtils.formValues(new JSONObject(staged)), LocalDate.now()).isClosed());

        AlertDialog prompt = promptBeforeSubmitting(visit,
                swahili ? "Amefariki\n\nKadi yake ya Mama Kinara nayo itafungwa." : "Deceased",
                swahili ? "Ndiyo, Funga" : "Yes, close", swahili ? "Ghairi" : "Cancel");
        assertEquals(swahili ? "Thibitisha kufunga rekodi" : "Confirm closing the record",
                ((TextView) prompt.findViewById(androidx.appcompat.R.id.alertTitle)).getText().toString());
        cancelAndConfirmOnce(visit, action, staged, prompt);
    }

    @Test
    public void englishThirdTravelUsesReasonOnlySavePrompt() throws Exception {
        checkThirdTravel(false);
    }

    @Test
    public void kiswahiliThirdTravelUsesReasonOnlySavePrompt() throws Exception {
        checkThirdTravel(true);
    }

    private void checkThirdTravel(boolean swahili) throws Exception {
        addTravel("first", LocalDate.now().minusDays(2));
        addTravel("second", LocalDate.now().minusDays(1));
        VisitActivity visit = activity(swahili);
        BaseMotherMentorVisitAction action = stageStatus(visit, swahili, "travelled");
        String staged = action.getJsonPayload();
        assertEquals(2, MotherMentorIitRepository.getState(db, CLIENT, null).getTravelledCount());

        AlertDialog prompt = promptBeforeSubmitting(visit,
                swahili ? "Rekodi hii itafungwa baada ya kuhifadhi. Sababu: Amesafiri"
                        : "This record will close after saving. Reason: Travelled",
                swahili ? "Hifadhi na funga" : "Save and close", swahili ? "Ghairi" : "Cancel");
        cancelAndConfirmOnce(visit, action, staged, prompt);
        try (Cursor visits = data.rawQuery("SELECT COUNT(*) FROM visits", null)) {
            assertTrue(visits.moveToFirst());
            assertEquals(2, visits.getInt(0));
        }
    }

    private VisitActivity activity(boolean swahili) {
        LangUtils.saveLanguage(RuntimeEnvironment.getApplication(), swahili ? "sw" : "en");
        Intent intent = new Intent().putExtra(MotherMentorVisitActivity.EXTRA_FOLLOW_UP_VISIT, true)
                .putExtra(MotherMentorFollowUpType.EXTRA_FOLLOW_UP_TYPE,
                        MotherMentorFollowUpType.MOTHER_IIT.getValue());
        VisitActivity visit = Robolectric.buildActivity(VisitActivity.class, intent).get();
        visit.setTheme(R.style.ChwTheme);
        visit.prepare();
        return visit;
    }

    private BaseMotherMentorVisitAction stageStatus(VisitActivity visit, boolean swahili,
            String reason) throws Exception {
        JSONObject form;
        String locale = swahili ? "json.form-sw" : "json.form";
        try (InputStream input = visit.getAssets().open(locale + "/" + STATUS_SECTION + ".json");
                Scanner scanner = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
            form = new JSONObject(scanner.next());
        }
        assertFalse(FormUtils.getFieldFromForm(form, "iit_note_deceased").has("key"));
        BaseMotherMentorVisitAction action = new BaseMotherMentorVisitAction.Builder(visit, STATUS_SECTION)
                .withFormName(STATUS_SECTION)
                .withJsonPayload(form.toString())
                .withHelper(new MotherMentorIitStatusActionHelper(() -> {}))
                .build();
        FormUtils.getFieldFromForm(form, DBConstants.KEY.FOLLOW_UP_STATUS).put("value", "not_found");
        FormUtils.getFieldFromForm(form, DBConstants.KEY.IIT_NOT_FOUND_REASON).put("value", reason);
        visit.saveSection(action, form);
        assertEquals(BaseMotherMentorVisitAction.Status.COMPLETED, action.getActionStatus());
        return action;
    }

    private void addTravel(String id, LocalDate day) throws Exception {
        JSONArray observations = new JSONArray()
                .put(observation(DBConstants.KEY.MOTHERMENTOR_VISIT_DATE, day.toString()))
                .put(observation(DBConstants.KEY.IIT_FOLLOW_UP_SCHEMA, "2"))
                .put(observation(DBConstants.KEY.FOLLOW_UP_STATUS, "not_found"))
                .put(observation(DBConstants.KEY.IIT_NOT_FOUND_REASON, "travelled"));
        JSONObject event = new JSONObject()
                .put(DBConstants.KEY.EVENT_DATE_TIMESTAMP, day.toDateTimeAtStartOfDay().toString())
                .put("obs", observations);
        data.execSQL("INSERT INTO visits VALUES (?, ?, ?, ?, 1, ?)",
                new String[] {id, CLIENT, EVENT_TYPE, id, event.toString()});
    }

    private JSONObject observation(String key, String value) throws Exception {
        return new JSONObject().put("fieldCode", key).put("values", new JSONArray().put(value));
    }

    private AlertDialog promptBeforeSubmitting(VisitActivity visit, String message, String confirm,
            String cancel) {
        AlertDialog previous = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        visit.submitVisit();
        visit.submitVisit();
        AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertNotSame(previous, dialog);
        assertTrue(dialog.isShowing());
        assertFalse(visit.submitEnabled());
        assertEquals(message, ((TextView) dialog.findViewById(android.R.id.message)).getText());
        assertEquals(confirm, dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        assertEquals(cancel, dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString());
        verifyNoInteractions(visit.getPresenter());
        return dialog;
    }

    private void cancelAndConfirmOnce(VisitActivity visit, BaseMotherMentorVisitAction action,
            String staged, AlertDialog dialog) {
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(dialog.isShowing());
        assertTrue(visit.submitEnabled());
        assertEquals(staged, action.getJsonPayload());
        assertFalse(MotherMentorIitRepository.getState(db, CLIENT, null).isClosed());
        verifyNoInteractions(visit.getPresenter());

        visit.submitVisit();
        AlertDialog retry = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertNotSame(dialog, retry);
        assertTrue(retry.isShowing());
        retry.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        retry.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        visit.submitVisit();
        verify(visit.getPresenter()).submitVisit();
        verifyNoMoreInteractions(visit.getPresenter());
    }

    public static class VisitActivity extends MotherMentorVisitActivity {
        void prepare() {
            baseEntityID = CLIENT;
            isEditMode = false;
            tvSubmit = new TextView(this);
            ReflectionHelpers.setField(this, "presenter", mock(BaseMotherMentorVisitContract.Presenter.class));
        }

        void saveSection(BaseMotherMentorVisitAction action, JSONObject form) {
            current_action = STATUS_SECTION;
            getMotherMentorVisitActions().put(STATUS_SECTION, action);
            onActivityResult(Constants.REQUEST_CODE_GET_JSON, Activity.RESULT_OK, new Intent()
                    .putExtra(Constants.JSON_FORM_EXTRA.JSON, form.toString()));
        }

        boolean submitEnabled() {
            return tvSubmit.isEnabled();
        }
    }
}
