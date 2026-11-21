package org.smartregister.chw.provider;

import android.content.Context;
import android.database.Cursor;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.joda.time.LocalDate;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.mothermentor.dao.MotherMentorIitFollowUpDao;
import org.smartregister.chw.mothermentor.fragment.BaseMotherMentorRegisterFragment;
import org.smartregister.chw.mothermentor.repository.MotherMentorIitRepository;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.chw.mothermentor.util.MotherMentorAgeUtils;
import org.smartregister.chw.mothermentor.util.MotherMentorIitState;
import org.smartregister.chw.mothermentor.util.MotherMentorIitText;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;

import timber.log.Timber;

public class MotherMentorIitRegisterProvider extends MotherMentorFollowUpRegisterProvider {
    private final Context context;
    private final View.OnClickListener clickListener;

    public MotherMentorIitRegisterProvider(Context context, View.OnClickListener paginationListener,
            View.OnClickListener clickListener) {
        super(context, paginationListener, clickListener);
        this.context = context;
        this.clickListener = clickListener;
    }

    @Override
    public Holder createViewHolder(ViewGroup parent) {
        return new IitHolder(inflater().inflate(
                org.smartregister.chw.mothermentor.R.layout.mothermentor_iit_follow_up_list_row,
                parent, false));
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, Holder baseHolder) {
        IitHolder holder = (IitHolder) baseHolder;
        CommonPersonObjectClient person = (CommonPersonObjectClient) client;
        String fullName = (value(person, DBConstants.KEY.FIRST_NAME) + " "
                + value(person, DBConstants.KEY.MIDDLE_NAME) + " "
                + value(person, DBConstants.KEY.LAST_NAME)).trim().replaceAll("\\s+", " ");
        String age = MotherMentorAgeUtils.formatAge(context, value(person, DBConstants.KEY.DOB));
        holder.name.setText(age.isEmpty() ? fullName : fullName + ", " + age);

        MotherMentorIitState state = MotherMentorIitState.active();
        String ctc = "";
        try {
            SQLiteDatabase db = ChwApplication.getInstance().getRepository().getReadableDatabase();
            state = MotherMentorIitRepository.getState(db, person.entityId(), null);
            ctc = MotherMentorIitFollowUpDao.ctcNumber(db, person.entityId());
        } catch (Exception e) {
            Timber.e(e, "Unable to load the IIT register summary");
        }
        if (state.isClosed()) {
            String tag = context.getString(org.smartregister.chw.mothermentor.R.string.mothermentor_iit_closed);
            String reason = MotherMentorIitText.reason(context, state.getStatus());
            SpannableString metadata = new SpannableString(tag.equals(reason) ? tag : tag + " · " + reason);
            int color = MotherMentorIitState.RETURNED_TO_CARE.equals(state.getStatus())
                    ? 0xff21663e : 0xff9d1b29;
            metadata.setSpan(new ForegroundColorSpan(color), 0, tag.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.metadata.setText(metadata);
        } else {
            String status = MotherMentorIitText.returnState(context, state);
            String counters = MotherMentorIitText.counters(context, state);
            holder.metadata.setText(counters.isEmpty() ? status : status + " · " + counters);
        }
        holder.pregnancy.setText(MotherMentorIitText.missedAppointment(context,
                state.getMissedAppointmentDate(), LocalDate.now()));
        String summary = ctc.isEmpty() ? ""
                : context.getString(org.smartregister.chw.mothermentor.R.string.mothermentor_iit_ctc, ctc) + " · ";
        if (state.isMamaKinaraLinked()) {
            summary += context.getString(org.smartregister.chw.mothermentor.R.string.mothermentor_iit_linked_tag) + " · ";
        }
        holder.summary.setText(summary + context.getString(
                org.smartregister.chw.mothermentor.R.string.mothermentor_iit_visit_count, state.getVisitCount()));
        holder.row.setTag(person);
        holder.row.setTag(R.id.VIEW_ID, BaseMotherMentorRegisterFragment.CLICK_VIEW_NORMAL);
        holder.row.setOnClickListener(clickListener);
    }

    private String value(CommonPersonObjectClient person, String key) {
        return Utils.getValue(person.getColumnmaps(), key, false);
    }

    private static class IitHolder extends Holder {
        final TextView summary;

        IitHolder(View view) {
            super(view);
            summary = view.findViewById(
                    org.smartregister.chw.mothermentor.R.id.mothermentor_iit_register_summary);
        }
    }
}
