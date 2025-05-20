package org.smartregister.chw.util;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.core.utils.BAJsonFormUtils;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import timber.log.Timber;

public class JsonFormUtilsFlv extends BAJsonFormUtils implements JsonFormUtils.Flavor {

    public static final JSONObject NONE= new JSONObject();

    public JsonFormUtilsFlv() {
        super(ChwApplication.getInstance());
    }

    @Override
    public JSONObject getAutoJsonEditMemberFormString(String title, String formName, Context context, CommonPersonObjectClient client, String eventType, String familyName, boolean isPrimaryCaregiver) {
        return super.getAutoJsonEditMemberFormString(title, formName, context, client, eventType, familyName, isPrimaryCaregiver);
    }

    @Override
    public void processFieldsForMemberEdit(CommonPersonObjectClient client, JSONObject jsonObject, JSONArray jsonArray, String familyName, boolean isPrimaryCaregiver, Event ecEvent, Client ecClient) throws JSONException {
        super.processFieldsForMemberEdit(client, jsonObject, jsonArray, familyName, isPrimaryCaregiver, ecEvent, ecClient);
    }

    public static JSONObject getQuestion(String key, JSONObject jsonForm,JSONObject defaultValue) {
        JSONObject obj=getQuestion(key,jsonForm);
        return obj!=NONE?obj:defaultValue;
    }
    public static JSONObject getQuestion(String key, JSONObject jsonForm) {
        Deque<Object> stack = new ArrayDeque<>();
        stack.add(jsonForm);
        while (!stack.isEmpty()) {
            Object obj = stack.removeLast();
            if (obj instanceof JSONObject) {
                JSONObject jsonObj = (JSONObject) obj;
                if (key.equals(jsonObj.optString("key"))) {
                    return jsonObj;
                }
                Iterator<String> keys=jsonObj.keys();
                while (keys.hasNext()) {
                    Object o=jsonObj.opt(keys.next());
                    if(o instanceof JSONObject || o instanceof JSONArray)
                        stack.addLast(o);
                }
            } else if (obj instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) obj;
                for (int i = 0; i < jsonArray.length(); i++) {
                    Object o=jsonArray.opt(i);
                    if(o instanceof JSONObject || o instanceof JSONArray)
                        stack.addLast(o);
                }
            }
        }
        return NONE;
    }

    public static <T> List<T> fromJsonArray(@Nullable JSONArray array, Mapper<Object,T> mapper){
        if(array==null)return new ArrayList<>();
        List<T> items=new ArrayList<>();
       for(int i=0,len=array.length();i<len;i++){
           Object obj=array.opt(i);
           if(obj!=null) items.add(mapper.map(array.opt(i)));
       }
       return items;
    }
    public static <T> List<T> fromJsonObjectArray(@Nullable JSONArray array, Mapper<JSONObject,T> mapper){
        return fromJsonArray(array,obj->mapper.map((JSONObject)obj));
    }
    public static  List<String> column(@Nullable JSONArray array, String columnName){
        return fromJsonObjectArray(array,obj->obj.optString(columnName));
    }

    public static void overwriteQuestionOptions( String questionKey, Map<String,String> keyValueOptions,JSONObject form){
        try {
            JSONObject question=getQuestion(questionKey,form);
            if(question==null){
                Timber.e("Question with key=%s was not found, make sure you have added a question in the form already",questionKey);
                return;
            }

            JSONArray optionsJSON=new JSONArray();
            for(Map.Entry<String,String> s : keyValueOptions.entrySet()){
                JSONObject option=new JSONObject();
                option.put("key",s.getKey());
                option.put("openmrs_entity","");
                option.put("openmrs_entity_id",s.getKey());
                option.put("openmrs_entity_parent","");
                option.put("text",s.getValue());
                optionsJSON.put(option);
            }
            question.put("options", optionsJSON);
        } catch (JSONException e) {Timber.e(e);}
    }

    public interface Mapper<S,T>{ public  T map(S s);}
}
