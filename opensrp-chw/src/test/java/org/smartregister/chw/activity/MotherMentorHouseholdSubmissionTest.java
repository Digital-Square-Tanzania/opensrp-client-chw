package org.smartregister.chw.activity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.widget.TextView;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.smartregister.chw.BaseUnitTest;
import org.smartregister.chw.mothermentor.contract.BaseMotherMentorVisitContract;

public class MotherMentorHouseholdSubmissionTest extends BaseUnitTest {
    @Test
    public void repeatedTapsSaveOnceAndCancellationRearmsTheSameActivity() {
        SubmissionActivity activity = Robolectric.buildActivity(SubmissionActivity.class).get();
        BaseMotherMentorVisitContract.Presenter presenter =
                mock(BaseMotherMentorVisitContract.Presenter.class);
        activity.bind(presenter);
        activity.submitVisit();
        activity.submitVisit();
        verify(presenter).submitVisit();
        assertFalse(activity.submitEnabled());
        activity.cancelSubmission();
        assertTrue(activity.submitEnabled());
        activity.submitVisit();
        verify(presenter, times(2)).submitVisit();
    }

    public static class SubmissionActivity extends MotherMentorHouseholdVisitActivity {
        void bind(BaseMotherMentorVisitContract.Presenter presenter) {
            this.presenter = presenter;
            tvSubmit = new TextView(this);
        }

        boolean submitEnabled() { return tvSubmit.isEnabled(); }
        void cancelSubmission() { onSubmissionCancelled(); }
    }
}
