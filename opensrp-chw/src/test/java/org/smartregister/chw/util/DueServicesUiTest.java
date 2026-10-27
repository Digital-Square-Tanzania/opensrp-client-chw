package org.smartregister.chw.util;

import android.app.Application;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.chw.R;
import org.smartregister.chw.core.model.NavigationOption;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.due.DueQuery;
import org.smartregister.chw.due.DueServiceItem;
import org.smartregister.chw.due.DueServicesSnapshot;
import org.smartregister.chw.due.LandingDueServiceRepository;
import org.smartregister.chw.due.ModuleDueResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class DueServicesUiTest {
    private static final String MODULE = LandingDueServiceRepository.MOTHER_MENTOR;
    private static final String SERVICE = "mother_mentor.followup";
    private View view;
    private DueQuery query;
    private final LocalDate today = new LocalDate(2026, 10, 27);

    @Before public void setUp() {
        Application context = RuntimeEnvironment.getApplication();
        context.setTheme(R.style.Theme_AppCompat_NoActionBar);
        view = LayoutInflater.from(context).inflate(R.layout.all_clients_due_summary, null);
        Map<String, String> names = new LinkedHashMap<>();
        names.put("one", "Asha Test");
        names.put("two", "Juma Test");
        query = new DueQuery(today, names);
    }

    @Test public void cardsUseDistinctClientBucketsAndPassTheSelectedFilter() {
        DueServicesSnapshot snapshot = snapshot(ModuleDueResult.complete(MODULE,
                Arrays.asList(item("one", "a", DueServiceItem.Status.DUE),
                        item("one", "b", DueServiceItem.Status.OVERDUE),
                        item("two", "c", DueServiceItem.Status.DUE)), Collections.singleton(SERVICE)));
        AtomicReference<DueServiceItem.Status> selected = new AtomicReference<>();
        DueServicesUi.bindSummary(view, snapshot, selected::set, () -> { });

        assertEquals("1", text(R.id.dashboard_due_count));
        assertEquals("1", text(R.id.dashboard_overdue_count));
        view.findViewById(R.id.dashboard_overdue_card).performClick();
        assertEquals(DueServiceItem.Status.OVERDUE, selected.get());
        view.findViewById(R.id.dashboard_due_card).performClick();
        assertEquals(DueServiceItem.Status.DUE, selected.get());
        assertTrue(text(R.id.dashboard_due_coverage).contains("Mother Mentor"));
        assertTrue(text(R.id.dashboard_due_coverage).contains("Next appointments (primary clients)"));
    }

    @Test public void successfulEmptyProviderShowsZeroAndAllowsOpeningEmptyRegister() {
        DueServicesUi.bindSummary(view, snapshot(ModuleDueResult.complete(MODULE,
                Collections.emptyList(), Collections.singleton(SERVICE))), status -> { }, () -> { });
        assertEquals("0", text(R.id.dashboard_due_count));
        assertTrue(view.findViewById(R.id.dashboard_due_card).isEnabled());
    }

    @Test public void unsupportedModuleNeverAppearsAsZeroOrClickableRegister() {
        DueServicesUi.bindSummary(view, snapshot(ModuleDueResult.unsupported(MODULE)),
                status -> { throw new AssertionError("Unavailable register opened"); }, () -> { });
        assertEquals("—", text(R.id.dashboard_due_count));
        assertEquals("—", text(R.id.dashboard_overdue_count));
        assertFalse(view.findViewById(R.id.dashboard_due_card).isEnabled());
        assertTrue(text(R.id.dashboard_due_coverage).contains("not available"));
    }

    @Test public void failedProviderClearsOldCountsAndOffersRetry() {
        DueServicesUi.bindSummary(view, snapshot(ModuleDueResult.complete(MODULE,
                Collections.singletonList(item("one", "a", DueServiceItem.Status.OVERDUE)),
                Collections.singleton(SERVICE))), status -> { }, () -> { });
        AtomicInteger retries = new AtomicInteger();
        DueServicesUi.bindSummary(view, snapshot(ModuleDueResult.error(MODULE)), status -> { }, retries::incrementAndGet);
        assertEquals("—", text(R.id.dashboard_overdue_count));
        assertFalse(view.findViewById(R.id.dashboard_overdue_card).isEnabled());
        view.findViewById(R.id.dashboard_due_coverage).performClick();
        assertEquals(1, retries.get());
    }

    @Test public void partialCoverageMarksKnownCountsAndDoesNotClaimEmptyBucketIsZero() {
        DueServicesUi.bindSummary(view, snapshot(new ModuleDueResult(MODULE, ModuleDueResult.Coverage.PARTIAL,
                Collections.singletonList(item("one", "a", DueServiceItem.Status.DUE)), Collections.singleton(SERVICE))),
                status -> { }, () -> { });
        assertEquals("1+", text(R.id.dashboard_due_count));
        assertEquals("—", text(R.id.dashboard_overdue_count));
        assertTrue(text(R.id.dashboard_due_coverage).contains("incomplete"));
    }

    @Test public void noEnabledModuleWithClientsHidesTheWholeSection() {
        DueServicesUi.bindSummary(view, DueServicesSnapshot.aggregate(query, Collections.emptyList()),
                status -> { }, () -> { });
        assertEquals(View.GONE, view.getVisibility());
    }

    @Test public void profileRouteRequiresTheSameRoleAllowedMotherMentorMenu() {
        DueServiceItem item = item("one", "a", DueServiceItem.Status.DUE);
        NavigationOption other = mock(NavigationOption.class);
        when(other.getMenuTitle()).thenReturn(CoreConstants.DrawerMenu.HPS);
        assertFalse(DueServiceNavigation.canOpen(item, Collections.singletonList(other)));
        NavigationOption mother = mock(NavigationOption.class);
        when(mother.getMenuTitle()).thenReturn(CoreConstants.DrawerMenu.MOTHER_MENTOR);
        assertTrue(DueServiceNavigation.canOpen(item, Collections.singletonList(mother)));
    }

    @Test public void futureMotherMentorSubtypesCannotOpenThePrimaryClientProfile() {
        NavigationOption mother = mock(NavigationOption.class);
        when(mother.getMenuTitle()).thenReturn(CoreConstants.DrawerMenu.MOTHER_MENTOR);
        for (String code : Arrays.asList("mother_mentor.iit", "mother_mentor.partner", "mother_mentor.eid")) {
            DueServiceItem unsupported = new DueServiceItem(MODULE, "one", "one", code,
                    "future-subtype", today, today.plusDays(1), DueServiceItem.Status.DUE);
            assertFalse(DueServiceNavigation.canOpen(unsupported, Collections.singletonList(mother)));
        }
    }

    private DueServicesSnapshot snapshot(ModuleDueResult result) {
        return DueServicesSnapshot.aggregate(query, Collections.singletonList(result));
    }

    private DueServiceItem item(String client, String key, DueServiceItem.Status status) {
        return new DueServiceItem(MODULE, client, client, SERVICE, key, today.minusDays(1), today, status);
    }

    private String text(int id) { return ((TextView) view.findViewById(id)).getText().toString(); }
}
