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
import com.mongodb.BasicDBObject;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.Generics;
import org.osgl.util.N;
import org.osgl.util.S;

import java.lang.reflect.Type;
import java.util.*;

import static org.osgl.util.N.Comparator.*;

public abstract class AggregationResult<T extends Number> {
    protected List<BasicDBObject> result;
    protected Class<?> modelType;
    protected String field;
    protected Class<T> valueType;

    private static final class ConvertResult<T extends Number> extends AggregationResult<T> {}

    private AggregationResult() {}

    public AggregationResult(List<BasicDBObject> r, String aggregationField, Class<?> modelClass) {
        if (null == r || null == aggregationField) throw new NullPointerException();
        result = r;
        modelType = modelClass;
        field = aggregationField;
        exploreValueType();
    }

    public <NT extends Number> AggregationResult<NT> as(Class<NT> newValueType) {
        ConvertResult<NT> result = new ConvertResult<>();
        result.result = this.result;
        result.modelType = this.modelType;
        result.field = this.field;
        result.valueType = newValueType;
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

    private T getValue(BasicDBObject dbObject) {
        return $.convert(dbObject.get(field)).to(valueType);
    }

    public T getDefault() {
        return result.size() > 0 ? getValue(result.get(0)) : null;
    }

    public T get(Object ... groupValues) {
        if (groupValues.length == 0) {
            return getDefault();
        }
        int len = groupValues.length;
        for (BasicDBObject r : result) {
            int i = 0;
            boolean found = true;
            for (Map.Entry<String, Object> entry : r.entrySet()) {
                if (i < len) {
                    if ($.ne(entry.getValue(), groupValues[i++])) {
                        found = false;
                        break;
                    }
                }
            }
            if (found) {
                return getValue(r);
            }
        }
        return null;
    }

    public T getByGroupKeys(String groupKeys, Object... groupValues) {
        if (S.empty(groupKeys)) {
            if (groupValues.length == 0) return getDefault();
            throw new IllegalArgumentException("the number of group keys does not match the number of group values");
        }
        String[] sa = groupKeys.split("[\\s,;:]+");
        if (sa.length != groupValues.length) throw new IllegalArgumentException("the number of group keys does not match the number of group values");
        for (BasicDBObject r: result) {
            boolean found = true;
            for (int i = 0; i < sa.length; ++i) {
                String groupKey = sa[i];
                String mappedGroupKey = mappedName(groupKey);
                Object groupValue = groupValues[i];
                if ($.ne(r.get(groupKey), groupValue) && $.ne(r.get(mappedGroupKey), groupValue)) {
                    found = false;
                    break;
                }
            }
            if (found) return getValue(r);
        }
        return null;
    }

    public Map<String, T> asMap() {
        return asNumberMap();
    }

    /**
     * This method is deprecated. Use {@link #asMap()} instead
     */
    @Deprecated
    public Map<String, T> asNumberMap() {
        Map<String, T> m = new HashMap<>(result.size());
        for (BasicDBObject r : result) {
            Collection<?> c = r.values();
            Iterator itr = c.iterator();
            String k = S.string(itr.next());
            Number n = (Number) itr.next();
            m.put(k, $.convert(n).to(valueType));
        }
        return m;
    }
    
    public List<BasicDBObject> raw() {
        return result;
    }
    
    public <NT extends Number> AggregationResult<NT> gt(NT targetValue) {
        return greaterThan(targetValue);
    }

    public <NT extends Number> AggregationResult<NT> greaterThan(NT targetValue) {
        return filter(GT, targetValue);
    }
    
    public <NT extends Number> AggregationResult<NT> gte(NT targetValue) {
        return atLeast(targetValue);
    }

    public <NT extends Number> AggregationResult<NT> greaterThanOrEqualTo(NT targetValue) {
        return atLeast(targetValue);
    }

    public <NT extends Number> AggregationResult<NT> atLeast(NT targetValue) {
        return filter(GTE, targetValue);
    }


    public <NT extends Number> AggregationResult<NT> lt(NT targetValue) {
        return lessThan(targetValue);
    }

    public <NT extends Number> AggregationResult<NT> lessThan(NT targetValue) {
        return filter(LT, targetValue);
    }

    public <NT extends Number> AggregationResult<NT> lte(NT targetValue) {
        return atMost(targetValue);
    }

    public <NT extends Number> AggregationResult<NT> lessThanOrEqualTo(NT targetValue) {
        return atMost(targetValue);
    }

    public <NT extends Number> AggregationResult<NT> atMost(NT targetValue) {
        return filter(LTE, targetValue);
    }

    public <NT extends Number> AggregationResult<NT> eq(NT targetValue) {
        return equalTo(targetValue);
    }

    public <NT extends Number> AggregationResult<NT> equalTo(NT targetValue) {
        return filter(EQ, targetValue);
    }

    public <T extends Number> AggregationResult<T> filter(N.Comparator comp, T targetValue) {
        List<BasicDBObject> newResult = new ArrayList<>();
        Class<T> targetType = $.cast(targetValue.getClass());
        for (BasicDBObject obj : result) {
            Collection<?> c = obj.values();
            Iterator itr = c.iterator();
            String k = S.string(itr.next());
            T n = $.convert(itr.next()).to(targetType);
            if (comp.compare(n, targetValue)) {
                newResult.add(obj);
            }
        }
        AggregationResult<T> retVal = new ConvertResult<>();
        retVal.result = newResult;
        retVal.field = field;
        retVal.modelType = modelType;
        retVal.valueType = targetType;
        return retVal;
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
