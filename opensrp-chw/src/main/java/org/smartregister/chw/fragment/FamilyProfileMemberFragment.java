package org.smartregister.chw.fragment;

import android.database.Cursor;
import android.os.Bundle;

import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.smartregister.chw.core.fragment.CoreFamilyProfileMemberFragment;
import org.smartregister.chw.model.FamilyProfileMemberModel;
import org.smartregister.chw.presenter.FamilyProfileMemberPresenter;
import org.smartregister.chw.provider.ChwMemberRegisterProvider;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.family.fragment.BaseFamilyProfileMemberFragment;
import org.smartregister.family.util.Constants;

import java.util.Set;

import timber.log.Timber;

public class FamilyProfileMemberFragment extends CoreFamilyProfileMemberFragment {


    public static BaseFamilyProfileMemberFragment newInstance(Bundle bundle) {
        Bundle args = bundle;
        BaseFamilyProfileMemberFragment fragment = new FamilyProfileMemberFragment();
        if (args == null) {
            args = new Bundle();
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void initializeAdapter(Set<org.smartregister.configurableviews.model.View> visibleColumns, String familyHead, String primaryCaregiver) {
        ChwMemberRegisterProvider chwMemberRegisterProvider = new ChwMemberRegisterProvider(this.getActivity(), this.commonRepository(), visibleColumns, this.registerActionHandler, this.paginationViewHandler, familyHead, primaryCaregiver);
        this.clientAdapter = new RecyclerViewPaginatedAdapter(null, chwMemberRegisterProvider, this.context().commonrepository(this.tablename));
        this.clientAdapter.setCurrentlimit(20);
        this.clientsView.setAdapter(this.clientAdapter);
    }

    @Override
    protected void initializePresenter() {
        Bundle bundle = getArguments();
        if (bundle == null)
            return;

        String familyBaseEntityId = bundle.getString(Constants.INTENT_KEY.FAMILY_BASE_ENTITY_ID);
        String familyHead = bundle.getString(Constants.INTENT_KEY.FAMILY_HEAD);
        String primaryCareGiver = bundle.getString(Constants.INTENT_KEY.PRIMARY_CAREGIVER);
        presenter = new FamilyProfileMemberPresenter(this, new FamilyProfileMemberModel(), null, familyBaseEntityId, familyHead, primaryCareGiver);
    }

    public void countExecute() {
        Cursor c = null;

        try {
            SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder(countSelect);
            sqb.addCondition(filters);
            String query = sqb.orderbyCondition(Sortqueries);
            query = sqb.Endquery(query);

            Timber.i(query);
            c = commonRepository().rawCustomQueryForAdapter(query);
            c.moveToFirst();
            clientAdapter.setTotalcount(c.getInt(0));
            Timber.tag("total count here").v("%s", clientAdapter.getTotalcount());

            clientAdapter.setCurrentlimit(20);
            clientAdapter.setCurrentoffset(0);


        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, final Bundle args) {
        switch (id) {
            case LOADER_ID:
                // Returns a new CursorLoader
                return new CursorLoader(getActivity()) {
                    @Override
                    public Cursor loadInBackground() {
                        // Count query
                        if (args != null && args.getBoolean("count_execute")) {
                            countExecute();
                        }
                        SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder(mainSelect);
                        sqb.addCondition(filters);
                        sqb.addCondition(((FamilyProfileMemberPresenter) presenter()).getChildFilter());
                        String query = sqb.orderbyCondition(Sortqueries);
                        query = sqb.Endquery(sqb.addlimitandOffset(query, clientAdapter.getCurrentlimit(), clientAdapter.getCurrentoffset()));
                        return commonRepository().rawCustomQueryForAdapter(query);
                    }
                };
            default:
                // An invalid id was passed in
                return null;
        }
    }
}
