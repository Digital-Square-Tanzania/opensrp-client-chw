package org.smartregister.chw.util;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import timber.log.Timber;


//ONLY USE for thoroughly tested cases, though this is convenient, it is still unstable and may break
/** @noinspection unused*/
public class JsonQ {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Object root;

    private static final Pattern INTEGER = Pattern.compile("^\\d+$");
    private static final Pattern REGULAR_PATH = Pattern.compile("\\w+(?:\\.\\w+)*");
    private static final Pattern ARRAY = Pattern.compile("\\[(?:(\\??\\(.+\\))|(-?\\d+:?-?\\d*(?:,-?\\d+:?-?\\d*)*)|(\\*))]");
    private static final Pattern GLOBED_PATH = Pattern.compile("(?=.*\\*)(?=.*\\w)[^.\\[\\]()?'\"]*");
    private static final Pattern WILDCARD = Pattern.compile("\\.{2,3}(?:" + REGULAR_PATH + ")?");
    private static final Pattern PATH_EXPRESSION = Pattern.compile(".*\\[?\\??\\(.*\\)");
    private static final Pattern PATH_POSSIBILITIES = Pattern.compile("(" +GLOBED_PATH+")?(" + REGULAR_PATH + ")?("+ WILDCARD + ")?(\\[[^]]+])?");
    private static final Pattern JSON_VARIABLE = Pattern.compile("@\\.(\\w+)");
    private static final Pattern VALUED_TRUE = Pattern.compile("(?i)yes|ndio|ndiyo|true");
    private static final Pattern VALUED_FALSE = Pattern.compile("(?i)hapana|no|false");

    private static final List<SimpleDateFormat> DATE_FORMATS=Arrays. asList(
            new SimpleDateFormat("yyyy-MM-dd",Locale.ENGLISH ),
            new SimpleDateFormat("dd-MM-yyyy" ,Locale.ENGLISH)
    );

    private final BoolEvaluator BOOL = new BoolEvaluator();

    private JsonQ(Object input) {
        root = input;
    }

    public static JsonQ fromJson(String json) {
        return new JsonQ(val(json));
    }

    public static JsonQ fromIO(InputStream jsonStream) {
        return new JsonQ(val(stringFromIO(jsonStream)));
    }

    public static JsonQ fromAsset(Context ctx, String assetName) {
        try (InputStream input = ctx.getAssets().open(assetName)) {
            return fromIO(input);
        } catch (IOException e) {
            log(e);
            return new JsonQ("");
        }
    }

    public static JsonQ fromIO(File jsonFile) {
        return new JsonQ(val(stringFromIO(jsonFile)));
    }

    public static JsonQ fromPOJO(Object object) {
        return new JsonQ(jsonPrimitive(object) ? object : getObjectRoot(object));
    }

    private static void log(Exception e) {
        Timber.tag(JsonQ.class.getSimpleName()).e(e);
    }

    private static String stringFromIO(Object input) {
        try {
            BufferedReader reader;
            if (input instanceof File)
                reader = new BufferedReader(new FileReader((File) input));
            else if (input instanceof InputStream)
                reader = new BufferedReader(new InputStreamReader((InputStream) input));
            else return "";

            StringBuilder sb = new StringBuilder();
            for (String s = reader.readLine(); s != null; s = reader.readLine()) sb.append(s);
            return sb.toString();
        } catch (IOException e) {
            Timber.e(e);
        }
        return "";
    }

    private static Object val(String jsonRoot) {
        Gson gson = new Gson();
        JsonElement element = JsonParser.parseString(jsonRoot);
        Type objectType = new TypeToken<Map<String, Object>>() {
        }.getType();
        Type arrayType = new TypeToken<List<Object>>() {
        }.getType();

        return element.isJsonObject() ? gson.fromJson(element, objectType)
                : element.isJsonArray() ? gson.fromJson(element, arrayType)
                : element.isJsonPrimitive() ? valPrimitive(element.getAsJsonPrimitive())
                : null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T valPrimitive(JsonPrimitive primitive) {
        return primitive.isBoolean() ? (T) Boolean.valueOf(primitive.getAsBoolean())
                : primitive.isNumber() ? (T) primitive.getAsNumber()
                : primitive.isString() ? (T) primitive.getAsString()
                : null;
    }

    private <T> List<T> getListImpl(String jsonPath, JFunction<Object, T> changer) {
        List<T> list = new ArrayList<>();
        flatForEach(find(jsonPath), (key, obj) -> list.add(changer.apply(obj)));
        return list;
    }

    public <T> List<T> getList(String jsonPath, JFunction<Map<?, ?>, T> changer) {
        List<T> list = new ArrayList<>();
        flatForEach(find(jsonPath), (key, obj) -> {
            if (obj instanceof Map<?, ?>)
                list.add(changer.apply((Map<?, ?>) obj));
        });
        return list;
    }

    public JsonQ select(String ...columns) {
        List<Object> results=new ArrayList<>();
        collectionForEach(root,(k,v)->{
            if(v instanceof Map<?,?>){
                //noinspection unchecked
                Map<String,Object>data=(Map<String,Object>)v;
                Map<String,Object> selection=new HashMap<>();
                for(String key:columns){
                       data.get(key);
                       selection.put(key,data.get("key"));
                }
                results.add(selection);
            }
        });
        return fromResults(results);
    }
    public List<Integer> intColumn(String columnName) {
        String path= String.format("[(@.%s~'\\d+')]",columnName);
        return fromResults(getListImpl(path, a -> a instanceof Integer ? a : null)).val();
    }
    public List<Date> dateColumn(String columnName) {
        String path= String.format("[(@.%s~'\\d{2,4}-\\d{2}-\\d{2,4}')].%s",columnName,columnName);
        return getDates(path);
    }
    public List<Date> getDates(String path) {
        List<Date> dates = new ArrayList<>();
        for(String d:getStrings(path)){
            Date date=getDate(d);
            if(date!=null)dates.add(date);
        }
        return dates;
    }

    private Date getDate(String date){
        try {
            if(date.matches("\\d{4}-\\d{2}-\\d{2}"))
                return DATE_FORMATS.get(0).parse(date);
            else if (date.matches("\\d{2}-\\d{2}-\\d{4}"))
                return DATE_FORMATS.get(1).parse(date);
        }
        catch (ParseException e) {Timber.e(e);}
        return null;
    }
    public List<String> stringColumn(String columnName) {
        return getStrings("[*]."+columnName);
    }
    public List<String> getStrings() {return getStrings("[*]");}
    public List<String> getStrings(String jsonPath) {
        return getListImpl(jsonPath, a -> (a instanceof String) ? (String) a : gson.toJson(a));
    }

    public List<String> getStrings(String jsonPath, Object... args) {
        return getStrings(String.format(jsonPath, args));
    }

    public String str() {return str("");}

    public String str(String jsonPath, Object... args) {
        return str(String.format(jsonPath, args));
    }

    public String str(String jsonPath) {
        Object obj = first(jsonPath, o -> o);
        return obj instanceof String ? (String) obj : obj == null ? "" : gson.toJson(obj);
    }

    public JsonQ get(String path, Object... args) {
        return get(String.format(path, args));
    }

    public JsonQ where(String nCondition,Object ... values){
        String condition = nCondition.replaceAll("\\band\\b","&&")
                .replaceAll("\\bor\\b","||")
                .replaceAll("\\$?(\\w+)","@.$1")
                .replaceAll("=+","==");

        for(Object v :values){
            if(!jsonPrimitive(v)) continue;
            String value = v instanceof String? String.format("'%s'",v): String.valueOf(v);
            condition=condition.replaceFirst("\\?",escapeRGX(value));
        }

        condition=String.format("(%s)",condition);
        List<Object> results=filter(condition,root,new ArrayList<>());
        return fromResults(results);
    }
    private String escapeRGX(String input){
       return input.replace("\\","\\\\");
    }

    private JsonQ fromResults(List<Object> results){
        Iterator<Object> it=results.listIterator();
        while(it.hasNext()){
            Object o=it.next();
            if(o==null)it.remove();
        }
        if (results.size() == 1) {
            Object obj = results.get(0);
            return obj instanceof Map<?, ?> ? new JsonQ(results.get(0))
                    : fromPOJO(obj);
        } else if (!results.isEmpty()) return new JsonQ(results);
        return fromPOJO(results);
    }

    public JsonQ get(String path) {
        return fromResults(find(path));
    }

    /**
     * @noinspection unchecked
     */
    public <T> T val() {return (T) root;}

    public boolean isEmpty() {
        return root == null || (root instanceof Map && ((Map<?, ?>) root).isEmpty()) ||
                (root instanceof Collection && ((Collection<?>) root).isEmpty()) ||
                (root instanceof String && ((String) root).isEmpty());
    }

    public boolean hasStuff(){ return !isEmpty(); }
    public boolean notEmpty(){ return !isEmpty(); }

    @NonNull
    @Override
    public String toString() {
        return root instanceof String ? (String) root : gson.toJson(root);
    }

    public <T> T first(String jsonPath, JFunction<Object, T> changer) {
        List<?> res = find(jsonPath);
        if (!res.isEmpty()) {
            return changer.apply(res.get(0));
        }
        return null;
    }

    public void put(String jsonPath, Object value) {
        int x = jsonPath.lastIndexOf(".");
        String prop = jsonPath.substring(x < 0 ? 0 : x + 1);
        put(jsonPath, true, prop, value);
    }
    public void add( Object value) {
        put("", false, "", value);
    }

    public void put(String jsonPath, boolean override, Object... values) {
        int x = jsonPath.lastIndexOf(".");
        Object res = find(jsonPath.substring(0, Math.max(x, 0)));
        flatForEach(res, (k, v) -> put(override, v, values));
    }

    /**
     * @noinspection unchecked
     */
    private void put(boolean override, Object container, Object... values) {
        if (container instanceof Map<?, ?>) {
            putInMap(override, (Map<String, Object>) container, values);
        } else if (container instanceof List<?>) {
            putInList(override, (List<Object>) container, values);
        }
    }

    private void putInMap(boolean override, Map<String, Object> map, Object... values) {
        for (int i = 0; i < values.length; i += 2) {
            Object key = values[i];
            Object val = values[i + 1];
            val = val instanceof JsonQ ? ((JsonQ) val).val() : val;

            if (!(key instanceof String)) {
                throw new IllegalArgumentException("attempting to set values to aJSON object without a key");
            }

            if (!map.containsKey((String) key) || override) {
                map.put((String) key, val);
            }
        }
    }

    private void putInList(boolean override, List<Object> list, Object... values) {
        for (int i = 0; i < values.length; i += 2) {
            Object key = values[i];
            Object val = values[i + 1];
            val = val instanceof JsonQ ? ((JsonQ) val).val() : val;

            if (!(key instanceof Integer || key.toString().matches("\\$|"))) {
                throw new IllegalArgumentException("attempting to set values to a JSON array without a valid integer index");
            }

            if (key instanceof Integer && (int) key >= 0 && (int) key < list.size() && override) {
                list.add((int) key, val);
            } else {
                list.add(val);
            }
        }
    }

    @NonNull
    private List<Object> find(String jsonPath) {
        List<Object> results = new ArrayList<>();
        if (jsonPath.matches("\\.|")) return Collections.singletonList(root);
        if (jsonPrimitive(root)) return results;

        List<String> paths = evaluatePath(jsonPath);
        results.add(root);

        List<Object> temp = new ArrayList<>();
        for (String path : paths) {
            if(results.isEmpty()){return results;}
            temp.clear();
            Taker<Object> taker;
            if (REGULAR_PATH.matcher(path).matches())
                taker = (key, obj) -> temp.add(handleNormalPath(path, obj));
            else if(GLOBED_PATH.matcher(path).matches())
                taker = (key, obj) -> globedPath(path, obj,temp);
            else if(PATH_EXPRESSION.matcher(path).matches())
                taker = (key, obj) -> filter(path, obj,temp);
            else if(WILDCARD.matcher(path).matches())
                taker = (key, obj) -> findMatchingPath(obj);
            else if(ARRAY.matcher(path).matches())
                taker = (key, obj) -> handleArrayMatch(path, obj,temp);
            else {
                Timber.e("Path not found %s. When processing this part %s", jsonPath, path);
                return Collections.emptyList();
            }
            collectionForEach(results, taker);
            results.clear();
            results.addAll(temp);
        }
        results.clear();
        for (Object o : temp) {
            if (o == null) continue;
            results.add(o);
        }
        return results;
    }

    private void collectionForEach(Object input, Taker<Object> consumer) {
        if (input instanceof Collection<?>) {
            flatForEach(input, consumer);
        } else consumer.take("", input);
    }

    private void handleArrayMatch(String path, Object object, List<Object> results) {
        Matcher parts = ARRAY.matcher(path);
        if (!parts.find()) return;
        if (parts.group(3) != null) {
            collectionForEach(object, (k, v) -> results.add(v));
        } else if (parts.group(1) != null) {
            filter(parts.group(1), object, results);
        } else if (parts.group(2) != null) {
            collectionForEach(object, (k, v) -> results.add(v));
            sliceList(results, parts.group(2));
        }
    }

    private static <T> void sliceList(List<T> list, @Nullable String sliceNotation) {
        if (sliceNotation == null || list.isEmpty()) return;

        List<T> result = new ArrayList<>();
        String[] slices = sliceNotation.split(",");
        for (String slice : slices) {
            result.addAll(extractSlice(list, slice));
        }

        list.clear();
        list.addAll(result);
    }

    private static <T> List<T> extractSlice(List<T> list, String sliceNotation) {
        List<T> sliceResult = new ArrayList<>();
        String[] parts = sliceNotation.split(":");
        boolean singleIndex = parts.length == 1 && !parts[0].isEmpty();
        int len = list.size();

        int start = parts.length > 0&& !parts[0].isEmpty() ? Integer.parseInt(parts[0]) : 0;
        int end = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) : list.size();

        start = Math.max(start < 0 ? len + start : start, 0);
        end = singleIndex ? start + 1 : Math.min(end < 0 ? len + end : end, len);

        for (int i = start; i < end; i++) {
            sliceResult.add(list.get(i));
        }

        return sliceResult;
    }

    private List<String> evaluatePath(String jsonPath) {
        Matcher parts = PATH_POSSIBILITIES.matcher(jsonPath);
        List<String> paths = new ArrayList<>();
        while (parts.find()) {
            for(int i=1,len=parts.groupCount();i<=len;i++) {
                String x=parts.group(i);
                if(x==null||x.isEmpty())continue;
                paths.add(x);
            }
        }
        return paths;
    }

    private static boolean jsonPrimitive(Object o) {
        return o instanceof Number || o instanceof String || o instanceof Boolean;
    }

    private List<Object> filter(String expression, Object object,List<Object> results) {
        collectionForEach(object, (key, obj) -> {
            obj = getObjectRoot(obj);
            boolean evaluation = false;
            if (obj instanceof Map<?, ?>) {
                Map<?, ?> json = (Map<?, ?>) obj;
                Matcher m = JSON_VARIABLE.matcher(expression);
                String exp = expression;
                while (m.find()) {
                    String variable = m.group(1);
                    String val = prepVariableForExpression(json.get(variable));
                    if (val == null) return;
                    exp = exp.replace("@." + variable, val);
                }
                evaluation = BOOL.evaluate(exp.replaceAll("[]\\[]", ""));
            } else if (jsonPrimitive(obj) && !key.isEmpty()) {
                String val = prepVariableForExpression(obj);
                if (val == null) return;
                evaluation = BOOL.evaluate(expression.replace("@." + key, val));
            }
            if (evaluation) results.add(obj);
        });
        return results;
    }

    private static Object getObjectRoot(Object object) {
        return object == null ? null
                : object instanceof Map<?, ?> || object instanceof List<?> ? object
                : object instanceof JsonQ ? ((JsonQ) object).root
                : val(gson.toJson(object));
    }

    private String prepVariableForExpression(Object val) {
        if (!(val instanceof String)) {
            return jsonPrimitive(val) ? String.valueOf(val) : null;
        }
        String v = (String) val;
        return VALUED_TRUE.matcher(v).matches() ? "true"
                : VALUED_FALSE.matcher(v).matches() ? "false"
                : String.format("'%s'", v);
    }

    private Object handleNormalPath(String path, Object object) {
        if (path == null || path.isEmpty()) return object;
        Object current = object;
        for (String p : path.split("\\."))
            current = valueAtKey(p, current);
        return current.equals(object) ? null : current;
    }

    private List<Object> globedPath(String path, Object jsonThing,List<Object> results){
        flatForEach(jsonThing,(k,v)->{
            if(k.matches(path.replace("*","\\w*"))){
                results.add(v);
            }
        });
        return results;
    }

    private void findMatchingPath(Object root) {
        Deque<Object> stack = new ArrayDeque<>();
        Set<Object> seen = new HashSet<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Object current = stack.pop();
            flatForEach(current, (key, obj) -> {
                if (obj == null || seen.contains(obj)) return;
                stack.push(obj);
                seen.add(obj);
            });
        }
    }

    private Object valueAtKey(String key, Object jsonThing) {
        return jsonThing instanceof Map<?, ?> ? ((Map<?, ?>) jsonThing).get(key)
                : jsonThing instanceof List && INTEGER.matcher(key).matches() ? ((List<?>) jsonThing).get(Integer.parseInt(key))
                : jsonThing;
    }

    public void forEach(Taker<JsonQ> taker) {
        flatForEach(root, (k, v) -> {
            if (v != root) {
                taker.take(k, fromPOJO(v));
            }
        });
    }

    private void flatForEach(Object input, Taker<Object> consumer) {
        if (input instanceof Map<?, ?>) {
            Map<?, ?> data = (Map<?, ?>) input;
            for (Map.Entry<?, ?> entry : data.entrySet()) {
                Object prop = entry.getValue();
                if (prop != null) consumer.take(entry.getKey().toString(), prop);
            }
        } else if (input instanceof List<?>) {
            List<?> data = (List<?>) input;
            for (int i = 0, len = data.size(); i < len; i++) {
                Object obj = data.get(i);
                if (obj != null) consumer.take(String.valueOf(i), obj);
            }
        } else if (input != null) consumer.take("", input);
    }

    public interface Taker<T> { void take(String key, T t);}
    public interface JFunction<S, T> { T apply(S s);}

}
