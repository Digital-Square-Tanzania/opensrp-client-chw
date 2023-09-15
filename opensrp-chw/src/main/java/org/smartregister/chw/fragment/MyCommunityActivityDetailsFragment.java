package org.smartregister.chw.fragment;

import static org.smartregister.chw.core.utils.ChildDBConstants.KEY.FAMILY_LAST_NAME;
import static org.smartregister.chw.core.utils.CoreReferralUtils.getCommonRepository;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.ChildProfileActivity;
import org.smartregister.chw.activity.FragmentBaseActivity;
import org.smartregister.chw.adapter.ListableAdapter;
import org.smartregister.chw.adapter.MyCommunityActivityDetailsAdapter;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.EligibleChild;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.viewholder.ListableViewHolder;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class MyCommunityActivityDetailsFragment extends ReportResultFragment<EligibleChild> {
    public static final String TAG = "MyCommunityActivityDetailsFragment";

    @Override
    protected void executeFetch() {
        presenter.fetchList(() -> ReportDao.myCommunityActivityReportDetails(indicatorCode));
    }

    @NonNull
    @Override
    public ListableAdapter<EligibleChild, ListableViewHolder<EligibleChild>> adapter() {
        return new MyCommunityActivityDetailsAdapter(list, this);
    }

    @Override
    public void onListItemClicked(EligibleChild eligibleChild, int layoutID) {
        Observable<CommonPersonObjectClient> observable = Observable.create(e -> {
            CommonPersonObject personObject = getCommonRepository(CoreConstants.TABLE_NAME.CHILD).findByBaseEntityId(eligibleChild.getID());
            CommonPersonObjectClient client = new CommonPersonObjectClient(personObject.getCaseId(),
                    personObject.getDetails(), "");
            client.setColumnmaps(personObject.getColumnmaps());

            e.onNext(client);
            e.onComplete();
        });

        final Disposable[] disposable = new Disposable[1];
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<CommonPersonObjectClient>() {
            @Override
            public void onSubscribe(Disposable d) {
                disposable[0] = d;
                setLoadingState(true);
            }

            @Override
            public void onNext(CommonPersonObjectClient client) {
                setLoadingState(false);
                Activity activity = getActivity();
                if (activity != null) {
                    MemberObject memberObject = new MemberObject(client);
                    memberObject.setFamilyName(Utils.getValue(client.getColumnmaps(), FAMILY_LAST_NAME, false));
                    ChildProfileActivity.startMe(activity, memberObject, ChildProfileActivity.class);
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }

            @Override
            public void onComplete() {
                setLoadingState(false);
                disposable[0].dispose();
                disposable[0] = null;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.my_community_activity_details_fragment, container, false);

        // TextView tvDate = view.findViewById(R.id.tvDate);
        //  TextView tvCommunity = view.findViewById(R.id.tvCommunity);
        Bundle bundle = getArguments();
        if (bundle != null) {
            //  communityID = bundle.getString(Constants.ReportParameters.COMMUNITY_ID);
            //   communityName = bundle.getString(Constants.ReportParameters.COMMUNITY);
            indicatorCode = bundle.getString(Constants.ReportParameters.INDICATOR_CODE);


            String date = bundle.getString(Constants.ReportParameters.REPORT_DATE);

            if (date != null) {
                try {
                    reportDate = new SimpleDateFormat("dd MMM yyyy", Locale.US).parse(date);
                } catch (ParseException e) {
                    Timber.e(e);
                }
            }
            // LinearLayout lLReportDetails = view.findViewById(R.id.report_details);
            // lLReportDetails.setVisibility(View.VISIBLE);
            // tvDate.setText(date);
            //  tvCommunity.setText(communityName);
        }
        bindLayout();
        loadPresenter();
        executeFetch();
        return view;
    }

    @Override
    public void refreshView() {
        super.refreshView();
        if (getActivity() instanceof FragmentBaseActivity) {
            ((FragmentBaseActivity) getActivity()).setTitle(list.size() + " " + getString(R.string.children));
        }
    }
}
