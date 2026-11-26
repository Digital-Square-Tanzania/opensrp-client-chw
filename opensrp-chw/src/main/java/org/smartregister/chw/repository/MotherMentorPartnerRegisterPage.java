package org.smartregister.chw.repository;

import android.database.Cursor;
import android.database.MatrixCursor;

import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.mothermentor.repository.MotherMentorPartnerClosureRepository;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorPartnerClosure;
import org.smartregister.commonregistry.CommonRepository;

/** Resolves the partner list against the same finalized closure evidence as its profile. */
public final class MotherMentorPartnerRegisterPage {
    public final Cursor rows;
    public final int count;

    private MotherMentorPartnerRegisterPage(Cursor rows, int count) {
        this.rows = rows;
        this.count = count;
    }

    public static MotherMentorPartnerRegisterPage read(CommonRepository repository, String query,
            boolean closed, int offset, int limit) {
        try (Cursor candidates = repository.rawCustomQueryForAdapter(query)) {
            MatrixCursor page = new MatrixCursor(candidates.getColumnNames());
            int indexedColumn = candidates.getColumnIndexOrThrow("partner_indexed_match");
            int replayColumn = candidates.getColumnIndexOrThrow("partner_replay_closure");
            int idColumn = candidates.getColumnIndexOrThrow(DBConstants.KEY.BASE_ENTITY_ID);
            int statusColumn = candidates.getColumnIndexOrThrow(DBConstants.KEY.MWENZI_STATUS);
            int dateColumn = candidates.getColumnIndexOrThrow(DBConstants.KEY.MWENZI_TAREHE_YA_KUFUNGWA);
            int reasonColumn = candidates.getColumnIndexOrThrow(DBConstants.KEY.MWENZI_SABABU_YA_KUFUNGWA);
            int total = 0;
            while (candidates.moveToNext()) {
                MotherMentorPartnerClosure closure = candidates.getInt(replayColumn) == 0
                        ? null : MotherMentorPartnerClosureRepository.getState(ChwApplication.getInstance()
                                .getRepository().getReadableDatabase(), candidates.getString(idColumn), null);
                boolean resolvedClosed = closure != null && closure.isClosed();
                boolean matches = resolvedClosed ? closed : candidates.getInt(indexedColumn) == 1;
                if (!matches) continue;
                int matchIndex = total++;
                if (matchIndex < offset || page.getCount() >= limit) continue;
                String[] row = new String[candidates.getColumnCount()];
                for (int i = 0; i < row.length; i++) row[i] = candidates.getString(i);
                if (resolvedClosed) {
                    row[statusColumn] = closure.getStatus();
                    row[dateColumn] = closure.getClosedDate();
                    row[reasonColumn] = closure.getClosureReason();
                }
                page.addRow(row);
            }
            return new MotherMentorPartnerRegisterPage(page, total);
        }
    }
}
