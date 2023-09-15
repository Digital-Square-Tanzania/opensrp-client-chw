package org.smartregister.chw.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import org.smartregister.chw.R;
import org.smartregister.chw.configs.AllClientsRegisterRowOptions;
import org.smartregister.chw.core.fragment.CoreAllClientsRegisterFragment;
import org.smartregister.chw.provider.ChwAllMaleClientsQueryProvider;
import org.smartregister.chw.provider.ChwMaleClientRegisterProvider;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.opd.configuration.OpdConfiguration;
import org.smartregister.opd.utils.ConfigurationInstancesHelper;




public class AllMaleClientsRegisterFragment extends CoreAllClientsRegisterFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        OpdConfiguration opdConfiguration = new OpdConfiguration.Builder(ChwAllMaleClientsQueryProvider.class)
                .setBottomNavigationEnabled(true)
                .setOpdRegisterRowOptions(AllClientsRegisterRowOptions.class)
                .build();

        setOpdRegisterQueryProvider(ConfigurationInstancesHelper.newInstance(opdConfiguration.getOpdRegisterQueryProvider()));
        return rootView;
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        View dueOnlyLayout = view.findViewById(R.id.due_only_layout);
        Toolbar toolbar = view.findViewById(org.smartregister.R.id.register_toolbar);
        toolbar.setVisibility(View.INVISIBLE);
        ViewGroup.LayoutParams layoutParams = toolbar.getLayoutParams();
        layoutParams.height = 20;
        toolbar.setLayoutParams(layoutParams);

        dueOnlyLayout.setVisibility(View.GONE);
    }


    @Override
    public void initializeAdapter() {
        ChwMaleClientRegisterProvider maleClientRegisterProvider = new ChwMaleClientRegisterProvider(getActivity(), registerActionHandler, paginationViewHandler);
        CommonRepository commonRepository = context().commonrepository(this.tablename);
        clientAdapter = new RecyclerViewPaginatedAdapter(null, maleClientRegisterProvider, commonRepository);
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }
}
