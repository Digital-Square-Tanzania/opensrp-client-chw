package org.smartregister.chw.interactor;

import org.smartregister.chw.tbleprosy.dao.TbLeprosyDao;
import org.smartregister.chw.tbleprosy.domain.MemberObject;
import org.smartregister.chw.tbleprosy.interactor.BaseTbLeprosyServiceVisitInteractor;

public class TbLeprosyVisitInteractor extends BaseTbLeprosyServiceVisitInteractor {

    public TbLeprosyVisitInteractor(String visitType) {
        super(visitType);
    }

    @Override
    public MemberObject getMemberClient(String memberID, String profileType) {
        return TbLeprosyDao.getContact(memberID);
    }
}
