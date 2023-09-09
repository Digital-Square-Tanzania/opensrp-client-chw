package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.presenter.BaseAncHomeVisitPresenter;
import org.smartregister.chw.core.task.RunnableTask;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.chw.fp.activity.BaseFpFollowUpVisitActivity;
import org.smartregister.chw.fp.domain.FpMemberObject;
import org.smartregister.chw.fp.util.FamilyPlanningConstants;
import org.smartregister.chw.interactor.FpFollowUpVisitInteractor;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.JsonFormUtils;

import java.util.Date;

public class FpFollowUpVisitActivity extends BaseFpFollowUpVisitActivity {

    public static void startMe(Activity activity, FpMemberObject memberObject, Boolean isEditMode) {
        Intent intent = new Intent(activity, FpFollowUpVisitActivity.class);
        intent.putExtra(org.smartregister.chw.anc.util.Constants.ANC_MEMBER_OBJECTS.MEMBER_PROFILE_OBJECT, toMember(memberObject));
        intent.putExtra(org.smartregister.chw.anc.util.Constants.ANC_MEMBER_OBJECTS.EDIT_MODE, isEditMode);
        activity.startActivityForResult(intent, org.smartregister.chw.anc.util.Constants.REQUEST_CODE_HOME_VISIT);
    }

    private static MemberObject toMember(FpMemberObject memberObject) {
        MemberObject res = new MemberObject();
        res.setBaseEntityId(memberObject.getBaseEntityId());
        res.setFirstName(memberObject.getFirstName());
        res.setLastName(memberObject.getLastName());
        res.setMiddleName(memberObject.getMiddleName());
        res.setDob(memberObject.getAge());
        return res;
    }

    @Override
    protected void registerPresenter() {
        presenter = new BaseAncHomeVisitPresenter(memberObject, this, new FpFollowUpVisitInteractor());
    }

    @Override
    public void onBackPressed() {
        displayExitDialog(() -> FpFollowUpVisitActivity.this.finish());

    }

    @Override
    public void close() {
        finish();
    }

    @Override
    public void submittedAndClose() {
        // recompute schedule
        Runnable runnable = () -> ChwScheduleTaskExecutor.getInstance().execute(memberObject.getBaseEntityId(), FamilyPlanningConstants.EventType.FP_FOLLOW_UP_VISIT, new Date());
        org.smartregister.chw.util.Utils.startAsyncTask(new RunnableTask(runnable), null);
        super.submittedAndClose();
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Form form = new Form();
        form.setActionBarBackground(R.color.family_actionbar);
        form.setWizard(false);

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(Constants.WizardFormActivity.EnableOnCloseDialog, false);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

}
