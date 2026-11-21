package org.smartregister.chw.sync;

import static org.mockito.Mockito.*;

import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.chw.mothermentor.util.MotherMentorFollowUpType;
import org.smartregister.domain.Event;
import org.smartregister.domain.db.EventClient;
import java.lang.reflect.Method;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class MotherMentorChildEidVisitProcessingTest {
    @Test
    public void childEidUsesItsOwnCanonicalProcessorWhileOtherModulesRetainTheExistingProcessor() throws Exception {
        for (boolean child : new boolean[] {true, false}) {
            Event event = new Event();
            event.setEventType(child ? MotherMentorFollowUpType.CHILD_EID.getEventType() : "ANC Home Visit");
            EventClient eventClient = mock(EventClient.class);
            when(eventClient.getEvent()).thenReturn(event);
            ChwClientProcessor processor = mock(ChwClientProcessor.class, CALLS_REAL_METHODS);
            try (MockedStatic<org.smartregister.chw.mothermentor.util.NCUtils> mother =
                        mockStatic(org.smartregister.chw.mothermentor.util.NCUtils.class);
                    MockedStatic<org.smartregister.chw.anc.util.NCUtils> anc =
                        mockStatic(org.smartregister.chw.anc.util.NCUtils.class)) {
                Method method = ChwClientProcessor.class.getDeclaredMethod("processVisitEvent", EventClient.class);
                method.setAccessible(true); method.invoke(processor, eventClient);
                if (child) {
                    mother.verify(() -> org.smartregister.chw.mothermentor.util.NCUtils.processHomeVisit(eventClient));
                    anc.verifyNoInteractions();
                } else {
                    anc.verify(() -> org.smartregister.chw.anc.util.NCUtils.processHomeVisit(eventClient));
                    mother.verifyNoInteractions();
                }
            }
        }
    }
}
