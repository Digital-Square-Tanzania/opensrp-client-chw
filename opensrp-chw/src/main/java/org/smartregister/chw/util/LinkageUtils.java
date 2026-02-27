package org.smartregister.chw.util;

import android.content.SharedPreferences;

import com.google.gson.Gson;

import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.referral.ReferralLibrary;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class LinkageUtils {

    public static void processEvent(ReferralLibrary referralLibrary, Event event){
        try {
            if (event != null){
                tagEvent(referralLibrary, event);
                JSONObject eventJson = new JSONObject(org.smartregister.util.JsonFormUtils.gson.toJson(event));
                referralLibrary.getSyncHelper().addEvent(event.getFormSubmissionId(), eventJson);
                Date lastSyncDate = new Date(referralLibrary.getContext().allSharedPreferences().fetchLastUpdatedAtDate(0));
                Timber.i(
                        "EventClient = %s",
                        new Gson().toJson(
                                referralLibrary.getSyncHelper().getEvents(lastSyncDate, BaseRepository.TYPE_Unsynced)
                        )
                );
                List<String> ids = new ArrayList<>();
                ids.add(event.getFormSubmissionId());
                referralLibrary.getClientProcessorForJava().processClient(referralLibrary.getSyncHelper().getEvents(ids));
                Utils.getAllSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
            }
        }catch (Exception e){
            Timber.e(e);
        }
    }

    public static void addLinkageDetails(Event baseEvent, String referralType, String referralProblems){
        if(baseEvent==null) return;

        long referralDate = System.currentTimeMillis();
        Map<String, Object> obsMap = new HashMap<>();
        obsMap.put("referral_status", "PENDING");
        obsMap.put("chw_referral_service", referralType);
        obsMap.put("referral_type", "community_to_addo_referral");
        obsMap.put("referral_date", referralDate);
        obsMap.put("referral_time",new SimpleDateFormat("HH:mm:ss.SSS", Locale.ENGLISH).format(referralDate));
        obsMap.put("problem", referralProblems);

        for (String key:obsMap.keySet()) {
            List<Object> value = Collections.singletonList(obsMap.get(key));
            baseEvent.addObs(new Obs("concept", "text",key, "", value, value, "",key));
        }

    }

    public static FormTag getFormTag(ReferralLibrary referralLibrary) {
        FormTag formTag = new FormTag();
        formTag.providerId = referralLibrary.getContext().allSharedPreferences().fetchRegisteredANM();
        formTag.appVersion = referralLibrary.getAppVersion();
        formTag.databaseVersion = referralLibrary.getDatabaseVersion();
        return formTag;
    }

    private static void tagEvent(ReferralLibrary referralLibrary, Event event) {
        AllSharedPreferences preferences = referralLibrary.getContext().allSharedPreferences();
        String providerId = preferences.fetchRegisteredANM();
        event.setProviderId(providerId);
        event.setLocationId(preferences.fetchUserLocalityId(providerId) != null ?
                preferences.fetchUserLocalityId(providerId) : preferences.fetchDefaultLocalityId(providerId));
        event.setChildLocationId(preferences.fetchCurrentLocality());
        event.setTeam(preferences.fetchDefaultTeam(providerId));
        event.setTeamId(preferences.fetchDefaultTeamId(providerId));
        event.setClientApplicationVersion(referralLibrary.getAppVersion());
        event.setClientDatabaseVersion(referralLibrary.getDatabaseVersion());
    }

}
