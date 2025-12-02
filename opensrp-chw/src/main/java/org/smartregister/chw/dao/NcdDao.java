package org.smartregister.chw.dao;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.model.NcdConfirmedRegisterFragmentModel;
import org.smartregister.chw.model.NcdRegisterAtRiskFragmentModel;
import org.smartregister.chw.ncd.contract.NcdRegisterFragmentContract;
import org.smartregister.chw.ncd.domain.MemberObject;
import org.smartregister.chw.presenter.NcdConfirmedRegisterFragmentPresenter;
import org.smartregister.chw.presenter.NcdRegisterFragmentPresenter;
import org.smartregister.dao.AbstractDao;
import org.smartregister.family.util.DBConstants;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class NcdDao extends AbstractDao {

    private static final String DIABETES_SCREENING_EVENT_TYPE = "Diabetes and Hypertension Screening";

    private NcdDao() {
        // no-op
    }

    public static Date getLastDiabetesScreeningDate(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            return null;
        }

        String sql = String.format(Locale.US,
                "SELECT eventDate FROM event WHERE eventType = '%s' AND baseEntityId = '%s' ORDER BY eventDate DESC LIMIT 1",
                DIABETES_SCREENING_EVENT_TYPE,
                baseEntityId
        );

        DataMap<Date> dataMap = cursor -> getCursorValueAsDate(cursor, "eventDate", getDobDateFormat());
        List<Date> results = readData(sql, dataMap);
        return (results != null && !results.isEmpty()) ? results.get(0) : null;
    }

    public static List<MemberObject> getAtRiskClients() {
        NcdRegisterFragmentContract.Model model = new NcdRegisterAtRiskFragmentModel();
        NcdRegisterFragmentContract.Presenter presenter = new NcdRegisterFragmentPresenter(null, model, null);
        return getMembersFromRegister(model, presenter);
    }

    public static List<MemberObject> getConfirmedClients() {
        NcdRegisterFragmentContract.Model model = new NcdConfirmedRegisterFragmentModel();
        NcdRegisterFragmentContract.Presenter presenter = new NcdConfirmedRegisterFragmentPresenter(null, model, null);
        return getMembersFromRegister(model, presenter);
    }

    public static MemberObject getClientById(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            return null;
        }
        return org.smartregister.chw.ncd.dao.NcdDao.getMember(baseEntityId);
    }

    private static List<MemberObject> getMembersFromRegister(NcdRegisterFragmentContract.Model model,
                                                            NcdRegisterFragmentContract.Presenter presenter) {
        List<String> baseEntityIds = readBaseEntityIds(buildRegisterQuery(model,
                presenter.getMainCondition(), presenter.getDefaultSortQuery()));
        return toMemberObjects(baseEntityIds);
    }

    private static String buildRegisterQuery(NcdRegisterFragmentContract.Model model,
                                             String mainCondition, String sortQuery) {
        String mainSelect = model.mainSelect(CoreConstants.TABLE_NAME.NCD_REGISTER, mainCondition);
        return String.format(Locale.US, "%s ORDER BY %s", mainSelect, sortQuery);
    }

    private static List<String> readBaseEntityIds(String query) {
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, DBConstants.KEY.BASE_ENTITY_ID);
        List<String> results = readData(query, dataMap);
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        LinkedHashSet<String> uniqueIds = new LinkedHashSet<>();
        for (String baseEntityId : results) {
            if (StringUtils.isNotBlank(baseEntityId)) {
                uniqueIds.add(baseEntityId);
            }
        }
        return new ArrayList<>(uniqueIds);
    }

    private static List<MemberObject> toMemberObjects(List<String> baseEntityIds) {
        List<MemberObject> members = new ArrayList<>();
        if (baseEntityIds == null || baseEntityIds.isEmpty()) {
            return members;
        }

        for (String baseEntityId : baseEntityIds) {
            MemberObject memberObject = org.smartregister.chw.ncd.dao.NcdDao.getMember(baseEntityId);
            if (memberObject != null) {
                members.add(memberObject);
            }
        }
        return members;
    }
}
