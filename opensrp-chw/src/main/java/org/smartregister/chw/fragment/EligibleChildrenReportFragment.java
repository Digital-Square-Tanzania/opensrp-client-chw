package org.smartregister.chw.fragment;

import static org.smartregister.chw.core.utils.ChildDBConstants.KEY.FAMILY_LAST_NAME;
import static org.smartregister.chw.core.utils.CoreReferralUtils.getCommonRepository;

import android.app.Activity;

import androidx.annotation.NonNull;

import org.smartregister.chw.R;
import org.smartregister.chw.activity.ChildProfileActivity;
import org.smartregister.chw.activity.FragmentBaseActivity;
import org.smartregister.chw.adapter.EligibleChildrenAdapter;
import org.smartregister.chw.adapter.ListableAdapter;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.EligibleChild;
import org.smartregister.chw.viewholder.ListableViewHolder;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.Utils;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class EligibleChildrenReportFragment extends ReportResultFragment<EligibleChild> {
    public static final String TAG = "EligibleChildrenReportFragment";

    @Override
    protected void executeFetch() {
        presenter.fetchList(() -> ReportDao.fetchLiveEligibleChildrenReport(communityIds, reportDate));
    }

    @NonNull
    @Override
    public ListableAdapter<EligibleChild, ListableViewHolder<EligibleChild>> adapter() {
        return new EligibleChildrenAdapter(list, this, this.getContext());
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
    public void refreshView() {
        super.refreshView();
        if (getActivity() instanceof FragmentBaseActivity) {
            ((FragmentBaseActivity) getActivity()).setTitle(list.size() + " " + getString(R.string.eligible_children));
        }
    }
}
