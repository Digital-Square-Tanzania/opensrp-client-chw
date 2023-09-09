package org.smartregister.chw.provider;

import static org.smartregister.chw.core.utils.Utils.getDuration;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.RelativeLayout;

import org.apache.commons.lang3.StringUtils;
import org.jeasy.rules.api.Rules;
import org.smartregister.chw.R;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.dao.PNCDao;
import org.smartregister.chw.core.model.ChildVisit;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.FamilyDao;
import org.smartregister.chw.util.ChildUtils;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.Utils;
import org.smartregister.commonregistry.CommonFtsObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.family.fragment.BaseFamilyProfileMemberFragment;
import org.smartregister.family.provider.FamilyMemberRegisterProvider;
import org.smartregister.family.util.DBConstants;
import org.smartregister.view.contract.SmartRegisterClient;
import org.smartregister.view.customcontrols.FontVariant;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public class ChwMemberRegisterProvider extends FamilyMemberRegisterProvider {
    private Context context;
    private View.OnClickListener onClickListener;

    public ChwMemberRegisterProvider(Context context, CommonRepository commonRepository, Set visibleColumns, View.OnClickListener onClickListener, View.OnClickListener paginationClickListener, String familyHead, String primaryCaregiver) {
        super(context, commonRepository, visibleColumns, onClickListener, paginationClickListener, familyHead, primaryCaregiver);
        this.onClickListener = onClickListener;
        this.context = context;
    }


    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, RegisterViewHolder viewHolder) {
        super.getView(cursor, client, viewHolder);

        // Update UI cutoffs
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewHolder.profile.getLayoutParams();
        layoutParams.width = context.getResources().getDimensionPixelSize(R.dimen.member_profile_pic_width);
        layoutParams.height = context.getResources().getDimensionPixelSize(R.dimen.member_profile_pic_width);
        layoutParams.setMarginStart(context.getResources().getDimensionPixelSize(R.dimen.change_layout_to_start));
        viewHolder.profile.setLayoutParams(layoutParams);
        viewHolder.patientNameAge.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimensionPixelSize(R.dimen.member_profile_list_title_size));

        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;

        viewHolder.statusLayout.setVisibility(View.GONE);
        viewHolder.status.setVisibility(View.GONE);
        viewHolder.status.getLayoutParams().height = context.getResources().getDimensionPixelSize(R.dimen.member_profile_pic_width);

        String entityType = Utils.getValue(pc.getColumnmaps(), ChildDBConstants.KEY.ENTITY_TYPE, false);
        if (Constants.TABLE_NAME.CHILD.equals(entityType)) {
            Utils.startAsyncTask(new UpdateAsyncTask(viewHolder, pc), null);
        }
        populatePatientColumn(pc, client, viewHolder);
    }


    private void populatePatientColumn(CommonPersonObjectClient pc, SmartRegisterClient client, final RegisterViewHolder viewHolder) {
        String firstName = org.smartregister.family.util.Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true);
        String middleName = org.smartregister.family.util.Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.MIDDLE_NAME, true);
        String lastName = org.smartregister.family.util.Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.LAST_NAME, true);
        String baseEntityId = org.smartregister.family.util.Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.BASE_ENTITY_ID, false);
        String patientName = Utils.getClientName(firstName, middleName, lastName);
        String dob = org.smartregister.family.util.Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.DOB, false);
        String dobString = getDuration(dob);
        dobString = dobString.contains("y") ? dobString.substring(0, dobString.indexOf("y")) : dobString;

        String dod = org.smartregister.family.util.Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.DOD, false);
        if (StringUtils.isNotBlank(dod)) {

            dobString = getDuration(dod, dob);
            dobString = dobString.contains("y") ? dobString.substring(0, dobString.indexOf("y")) : dobString;

            patientName = patientName + ", " + org.smartregister.family.util.Utils.getTranslatedDate(dobString, context) + " " + context.getString(R.string.deceased_brackets);
            viewHolder.patientNameAge.setFontVariant(FontVariant.REGULAR);
            viewHolder.patientNameAge.setTextColor(Color.GRAY);
            viewHolder.patientNameAge.setTypeface(viewHolder.patientNameAge.getTypeface(), Typeface.ITALIC);
            // Replace person avatar
            new ReplaceAvatarTask(viewHolder, pc).execute(baseEntityId);
            viewHolder.nextArrow.setVisibility(View.GONE);
        } else {
            patientName = patientName + ", " + org.smartregister.family.util.Utils.getTranslatedDate(dobString, context);
            viewHolder.patientNameAge.setFontVariant(FontVariant.REGULAR);
            viewHolder.patientNameAge.setTextColor(Color.BLACK);
            viewHolder.patientNameAge.setTypeface(viewHolder.patientNameAge.getTypeface(), Typeface.NORMAL);
            // Replace person avatar
            new ReplaceAvatarTask(viewHolder, pc).execute(baseEntityId);
            viewHolder.nextArrow.setVisibility(View.VISIBLE);
        }

        fillValue(viewHolder.patientNameAge, patientName);

        String gender_key = org.smartregister.family.util.Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.GENDER, true);
        String gender = "";
        if (gender_key.equalsIgnoreCase("Male")) {
            gender = context.getString(R.string.male);
        } else if (gender_key.equalsIgnoreCase("Female")) {
            gender = context.getString(R.string.female);
        }
        fillValue(viewHolder.gender, gender);

        viewHolder.nextArrowColumn.setOnClickListener(v -> viewHolder.nextArrow.performClick());

        viewHolder.profile.setOnClickListener(v -> viewHolder.patientColumn.performClick());

        viewHolder.registerColumns.setOnClickListener(v -> viewHolder.patientColumn.performClick());
        if (StringUtils.isBlank(dod)) {
            View patient = viewHolder.patientColumn;
            attachPatientOnclickListener(patient, client);

            View nextArrow = viewHolder.nextArrow;
            attachNextArrowOnclickListener(nextArrow, client);
        }

        updateDueColumn(viewHolder, baseEntityId);

    }

    private void attachPatientOnclickListener(View view, SmartRegisterClient client) {
        view.setOnClickListener(onClickListener);
        view.setTag(client);
        view.setTag(R.id.VIEW_ID, BaseFamilyProfileMemberFragment.CLICK_VIEW_NORMAL);
    }

    private void attachNextArrowOnclickListener(View view, SmartRegisterClient client) {
        view.setOnClickListener(onClickListener);
        view.setTag(client);
        view.setTag(R.id.VIEW_ID, BaseFamilyProfileMemberFragment.CLICK_VIEW_NEXT_ARROW);
    }

    private String getDueState(String memberBaseEntityId) {
        if (ChwApplication.getApplicationFlavor().showChildrenAboveTwoDueStatus()) {
            return FamilyDao.getMemberDueStatus(memberBaseEntityId);
        } else {
            return FamilyDao.getMemberDueStatusForUnderTwoChildren(memberBaseEntityId);
        }
    }

    private void updateDueColumn(RegisterViewHolder viewHolder, String memberBaseEntityId) {
        viewHolder.statusLayout.setVisibility(View.VISIBLE);
        String dueState = getDueState(memberBaseEntityId);

        try {
            if (dueState != null) {
                if (dueState.equalsIgnoreCase(CoreConstants.VISIT_STATE.OVERDUE)) {
                    viewHolder.status.setVisibility(View.VISIBLE);
                    viewHolder.status.setImageResource(Utils.getOverDueProfileImageResourceIDentifier());
                } else if (dueState.equalsIgnoreCase(CoreConstants.VISIT_STATE.DUE)) {
                    viewHolder.status.setVisibility(View.VISIBLE);
                    viewHolder.status.setImageResource(Utils.getDueProfileImageResourceIDentifier());
                } else {
                    viewHolder.status.setVisibility(View.INVISIBLE);
                }
            } else {
                viewHolder.status.setVisibility(View.INVISIBLE);
            }

        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void setMemberProfileAvatar(int imageResourceIdentifier, CommonPersonObjectClient commonPersonObject, RegisterViewHolder registerViewHolder) {
        String dod = org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOD, false);
        if (StringUtils.isNotBlank(dod)) {
            registerViewHolder.profile.setImageResource(imageResourceIdentifier);
        } else {
//            imageRenderHelper.refreshProfileImage(commonPersonObject.getCaseId(), registerViewHolder.profile, imageResourceIdentifier);
            registerViewHolder.profile.setImageResource(imageResourceIdentifier);
        }
    }

    private void setMemberProfileImageResourceIdentifier(Constants.FamilyMemberType memberType, CommonPersonObjectClient commonPersonObject, RegisterViewHolder viewHolder) {
        if (Constants.FamilyMemberType.Other.equals(memberType)) { // Non ANC/PNC family member
            String entityType = Utils.getValue(commonPersonObject.getColumnmaps(), ChildDBConstants.KEY.ENTITY_TYPE, false);
            if (CoreConstants.TABLE_NAME.CHILD.equals(entityType)) {
                setMemberProfileAvatar(org.smartregister.family.util.Utils.getMemberProfileImageResourceIDentifier(entityType), commonPersonObject, viewHolder);
            } else {
                setMemberProfileAvatar(Utils.getMemberImageResourceIdentifier(), commonPersonObject, viewHolder);
            }
        } else {
            if (Constants.FamilyMemberType.ANC.equals(memberType) && ChwApplication.getApplicationFlavor().hasANC()) {
                setMemberProfileAvatar(Utils.getAnCWomanImageResourceIdentifier(), commonPersonObject, viewHolder);
            } else if (Constants.FamilyMemberType.PNC.equals(memberType) && ChwApplication.getApplicationFlavor().hasPNC()) {
                setMemberProfileAvatar(Utils.getPnCWomanImageResourceIdentifier(), commonPersonObject, viewHolder);
            } else {
                setMemberProfileAvatar(Utils.getMemberImageResourceIdentifier(), commonPersonObject, viewHolder);
            }
        }
    }

    private Map<String, String> getChildDetails(String baseEntityId) {
        SmartRegisterQueryBuilder queryBUilder = new SmartRegisterQueryBuilder();
        queryBUilder.selectInitiateMainTable(CommonFtsObject.searchTableName(Constants.TABLE_NAME.CHILD), new String[]{CommonFtsObject.idColumn, ChildDBConstants.KEY.LAST_HOME_VISIT, ChildDBConstants.KEY.VISIT_NOT_DONE, ChildDBConstants.KEY.DATE_CREATED});
        String query = queryBUilder.mainCondition(String.format(" %s is null AND %s = '%s' AND %s ",
                DBConstants.KEY.DATE_REMOVED,
                CommonFtsObject.idColumn,
                baseEntityId,
                ChildDBConstants.childAgeLimitFilter().replace("ec_child.", "ec_child_search.")));

        query = query.replace(CommonFtsObject.searchTableName(Constants.TABLE_NAME.CHILD) + ".id as _id ,", "");

        CommonRepository commonRepository = Utils.context().commonrepository(Constants.TABLE_NAME.CHILD);
        List<Map<String, String>> res = new ArrayList<>();

        Cursor cursor = null;
        try {
            cursor = commonRepository.queryTable(query);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                int columncount = cursor.getColumnCount();
                Map<String, String> columns = new HashMap<>();
                for (int i = 0; i < columncount; i++) {
                    columns.put(cursor.getColumnName(i), cursor.getType(i) == Cursor.FIELD_TYPE_NULL ? null : String.valueOf(cursor.getString(i)));
                }
                res.add(columns);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            Timber.e(e, e.toString());
        } finally {
            if (cursor != null)
                cursor.close();
        }

        if (res.isEmpty()) {
            return null;
        }
        return res.get(0);
    }

    private ChildVisit retrieveChildVisitList(Rules rules, CommonPersonObjectClient pc, Map<String, String> map) {
        String dob = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.DOB, false);
        String dobString = getDuration(dob);
        String lastVisitDate = map.get(ChildDBConstants.KEY.LAST_HOME_VISIT);
        String visitNotDone = map.get(ChildDBConstants.KEY.VISIT_NOT_DONE);
        String strDateCreated = map.get(ChildDBConstants.KEY.DATE_CREATED);
        long lastVisit = 0;
        long visitNot = 0;
        long dateCreated = 0;
        if (!TextUtils.isEmpty(lastVisitDate)) {
            lastVisit = Long.valueOf(lastVisitDate);
        }
        if (!TextUtils.isEmpty(visitNotDone)) {
            visitNot = Long.valueOf(visitNotDone);
        }
        if (!TextUtils.isEmpty(strDateCreated)) {
            dateCreated = org.smartregister.family.util.Utils.dobStringToDateTime(strDateCreated).getMillis();
        }
        return ChildUtils.getChildVisitStatus(context, rules, dobString, lastVisit, visitNot, dateCreated);
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////

    private class ReplaceAvatarTask extends AsyncTask<String, Void, Constants.FamilyMemberType> {

        private WeakReference<RegisterViewHolder> viewHolderWeakReference;
        private CommonPersonObjectClient commonPersonObject;

        private ReplaceAvatarTask(RegisterViewHolder registerViewHolder, CommonPersonObjectClient commonPersonObjectClient) {
            viewHolderWeakReference = new WeakReference<>(registerViewHolder);
            commonPersonObject = commonPersonObjectClient;
        }

        @Override
        protected Constants.FamilyMemberType doInBackground(String... strings) {
            String baseEntityId = strings[0];
            if (PNCDao.isPNCMember(baseEntityId)) {
                return Constants.FamilyMemberType.PNC;
            } else if (AncDao.isANCMember(baseEntityId)) {
                return Constants.FamilyMemberType.ANC;
            } else {
                return Constants.FamilyMemberType.Other;
            }
        }

        @Override
        protected void onPostExecute(Constants.FamilyMemberType memberType) {
            setMemberProfileImageResourceIdentifier(memberType, commonPersonObject, viewHolderWeakReference.get());
        }
    }

    private class UpdateAsyncTask extends AsyncTask<Void, Void, Void> {
        private final CommonPersonObjectClient pc;

        private final Rules rules;

        private ChildVisit childVisit;

        private UpdateAsyncTask(RegisterViewHolder viewHolder, CommonPersonObjectClient pc) {
            this.pc = pc;
            this.rules = ChwApplication.getInstance().getRulesEngineHelper().rules(Constants.RULE_FILE.HOME_VISIT);
        }

        @Override
        protected Void doInBackground(Void... params) {
            Map<String, String> map = getChildDetails(pc.getCaseId());
            if (map != null) {
                childVisit = retrieveChildVisitList(rules, pc, map);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            // Update status column
            if (childVisit != null) {
            }
        }
    }
}