package org.smartregister.chw.actionhelper;

import static org.smartregister.chw.util.JsonFormUtils.getCheckBoxValue;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.core.domain.Person;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class BabyMinorAilmentActionHelper implements BaseAncHomeVisitAction.AncHomeVisitActionHelper {

    private String minor_ailment;
    private final Context context;
    private final Person baby;

    public BabyMinorAilmentActionHelper(Context context, Person baby){
        this.context = context;
        this.baby = baby;
    }

    @Override
    public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {

    }

    @Override
    public String getPreProcessed() {
        return "";
    }

    @Override
    public void onPayloadReceived(String dangerSignForm) {
        try {
            JSONObject form = new JSONObject(dangerSignForm);
            minor_ailment = getCheckBoxValue(form, "child_minor_ailment");
        } catch (JSONException e) {
            Timber.e(e);}
    }

    @Override
    public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
        return null;
    }

    @Override
    public String getPreProcessedSubTitle() {
        return "";
    }

    @Override
    public String evaluateSubTitle() {
        return MessageFormat.format("{0}: {1}", (MessageFormat.format(context.getString(R.string.child_minor_illness), baby.getFullName())), minor_ailment);
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (minor_ailment == null) {
            return BaseAncHomeVisitAction.Status.PENDING;
        }else{
            return BaseAncHomeVisitAction.Status.COMPLETED;
        }
    }

    @Override
    public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {

    }

    @Override
    public String postProcess(String jsonPayload) {
        //TODO: Send the referral Linkage
        return "";
    }

}
