package act.db.morphia.util;

/*-
 * #%L
 * ACT Morphia
 * %%
 * Copyright (C) 2015 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import act.db.morphia.MorphiaService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.Generics;
import org.osgl.util.N;
import org.osgl.util.S;

import java.lang.reflect.Type;
import java.util.*;

import static org.osgl.util.N.Comparator.*;

public class AggregationResult<T extends Number> {
    protected List<BasicDBObject> result;
    protected Class<?> modelType;
    protected String field;
    protected Class<T> valueType;
    protected Map<Object, T> mapCache;
    protected Map<String, String> groupKeyMap;

    private static final class ConvertResult<T extends Number> extends AggregationResult<T> {}

    private AggregationResult() {}

    public AggregationResult(List<BasicDBObject> r, String aggregationField, Map<String, String> groupKeyMap, Class<?> modelClass, Class<T> valueType) {
        if (null == r || null == aggregationField) throw new NullPointerException();
        result = r;
        modelType = modelClass;
        field = aggregationField;
        this.groupKeyMap = $.requireNotNull(groupKeyMap);
        this.valueType = $.requireNotNull(valueType);
    }

    public <NT extends Number> AggregationResult<NT> as(Class<NT> newValueType) {
        if (newValueType == valueType) {
            return $.cast(this);
        }
        ConvertResult<NT> result = new ConvertResult<>();
        result.result = this.result;
        result.modelType = this.modelType;
        result.field = this.field;
        result.valueType = newValueType;
        result.groupKeyMap = groupKeyMap;
        if (null != mapCache) {
            Map<Object, T> myMapCache = mapCache();
            Map<Object, NT> newMapCache = new LinkedHashMap<>();
            for (Map.Entry<Object, T> entry : myMapCache.entrySet()) {
                newMapCache.put(entry.getKey(), $.convert(entry.getValue()).to(newValueType));
            }
            result.mapCache = newMapCache;
        }
        return result;
    }

    public AggregationResult<Integer> asIntResult() {
        return as(Integer.class);
    }

    public AggregationResult<Long> asLongResult() {
        return as(Long.class);
    }

    public AggregationResult<Double> asDoubleResult() {
        return as(Double.class);
    }

    public AggregationResult<Float> asFloatResult() {
        return as(Float.class);
    }

    @Override
    public String toString() {
        return JSON.toJSONString(asMap(), true);
    }

    private Map<Object, T> mapCache() {
        if (null == mapCache) {
            synchronized (this) {
                if (null == mapCache) {
                    mapCache = new LinkedHashMap<>();
                    for (BasicDBObject obj : result) {
                        Object key = obj.get("_id");
                        T val = $.convert(obj.get(field)).to(valueType);
                        if (null == key) {
                            mapCache.put("", val);
                        } else if (key instanceof DBObject) {
                            BasicDBObject dbKey = $.cast(key);
                            // the mongodb column name
                            mapCache.put(new HashMap<>(dbKey), val);
                            // the java field column name
                            Map<String, Object> javaFields = new HashMap<>();
                            // the group value sequence
                            List seq = new ArrayList();
                            for (Map.Entry<String, Object> keyEntry : dbKey.entrySet()) {
                                javaFields.put(groupKeyMap.get(keyEntry.getKey()), keyEntry.getValue());
                                seq.add(keyEntry.getValue());
                            }
                            mapCache.put(javaFields, val);
                            mapCache.put(seq, val);
                        } else {
                            mapCache.put(key, val);
                        }
                    }
                }
            }
        }
        return mapCache;
    }

    /**
     * Returns the first val in the aggregation result set.
     *
     * Normally this method shall be used to return single aggregation result,
     * e.g. sum, max, min without grouping
     * @return the single val aggregation result
     */
    public T val() {
        return mapCache().values().iterator().next();
    }

    /**
     * Returns aggregation value by group values.
     *
     * When multiple group value specified, the sequence must corresponding to
     * the sequence of aggregation group key sequence. For example if aggregation
     * group key sequence is "region,department", the value sequence must
     * be "NSW,Sales", not "Sales,NSW".
     *
     * @param groupValue the first group field value
     * @param groupValues optional group field values
     * @return the corresponding aggregation result.
     */
    public T val(Object groupValue, Object... groupValues) {
        Map<Object, T> cache = mapCache();
        int len = groupValues.length;
        if (0 == len) {
            return cache.get(groupValue);
        }
        List list = new ArrayList();
        list.add(groupValue);
        for (Object v : groupValues) {
            list.add(v);
        }
        return cache.get(list);
    }

    /**
     * Returns aggregation result by multiple group values.
     *
     * This method is used when aggregation is grouped by multiple groups.
     *
     * Sample code:
     *
     * ```
     * Map<String, Object> groupValues = new HashMap<>();
     * groupValues.put("departmentId", 5);
     * groupValues.put("regionId", 10);
     * return result.get(groupValues);
     * ```
     *
     * The above code returns the aggregation result of (departmentId=5, regionId=10)
     *
     * @param groupValues specifies group
     * @return the corresponding aggregation value
     */
    public T val(Map<String, Object> groupValues) {
        Map<String, Object> key = new HashMap<>(groupValues);
        return mapCache().get(key);
    }

    public Map<Object, T> asMap() {
        return asNumberMap();
    }

    /**
     * This method is deprecated. Use {@link #asMap()} instead
     */
    @Deprecated
    public Map<Object, T> asNumberMap() {
        Map<Object, T> map = new LinkedHashMap<>();
        for (Map.Entry<Object, T> entry : mapCache().entrySet()) {
            if (entry.getKey() instanceof Map) {
                continue;
            }
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
    
    public List<BasicDBObject> raw() {
        return result;
    }
    
    private String mappedName(String field) {
        return MorphiaService.mappedName(field, modelType);
    }

    private void exploreValueType() {
        List<Type> types = Generics.typeParamImplementations(getClass(), AggregationResult.class);
        int sz = types.size();
        if (sz != 1) {
            throw E.unexpected("Cannot determine value type");
        }
        Type type = types.get(0);
        valueType = Generics.classOf(type);
    }

}
