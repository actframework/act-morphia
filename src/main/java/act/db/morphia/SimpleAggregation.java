package act.db.morphia;

/*-
 * #%L
 * ACT Morphia
 * %%
 * Copyright (C) 2015 - 2019 ActFramework
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

import act.db.morphia.util.AggregationResult;
import com.mongodb.*;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.Keyword;
import org.osgl.util.N;

import java.util.*;

import static act.db.morphia.SimpleAggregation.Operator.*;
import static act.db.morphia.MorphiaPlugin.dbo;
import static org.osgl.util.N.Comparator.*;

/**
 * A simple mongodb aggregation pipeline
 */
public class SimpleAggregation<T extends Number> {

    public enum Operator {
        AVG,
        COUNT() {
            @Override
            DBObject groupExpression(String field) {
                return dbo("$sum", 1);
            }
        },
        MAX,
        MIN,
        STD_DEV("$stdDevPop"),
        SUM,
        ;
        private String op;

        private Operator() {
            this.op = "$" + Keyword.of(name()).javaVariable();
        }

        private Operator(String op) {
            this.op = op;
        }

        DBObject groupExpression(String field) {
            return dbo(op, field);
        }
    }

    private MorphiaQuery<?> query;
    private DBCollection collection;
    private Class<?> modelType;
    private BasicDBObject where;
    private DBObject sample;
    private DBObject group;
    private DBObject having;
    private DBObject sort;
    private Class<T> valType;
    // key - mongo col name, val - java field name
    private Map<String, String> groupKeyMap = new LinkedHashMap<>();

    public SimpleAggregation(MorphiaQuery<?> query, Class<T> valType) {
        this.collection = query.getCollection();
        this.modelType = query.modelType();
        this.where = (BasicDBObject) query.getQueryObject();
        this.query = query;
        this.valType = valType;
    }

    private SimpleAggregation() {}

    /**
     * Having aggregation value must be applied to true when comparing with target value using
     * comparator specified.
     *
     * @param comparator the number comparator
     * @param target the target data to be evaluated using the comparator against aggregation value
     * @return this aggregation with having clause set
     */
    public  SimpleAggregation<T> having(N.Comparator comparator, Number target) {
        this.having = dbo("$match", dbo("val", dbo(compExpression(comparator), target)));
        return this;
    }

    /**
     * Set a list of having conditions
     * @param conditions a list of having conditions
     * @return this aggregation with having clause set
     */
    public SimpleAggregation<T> having(List<$.T2<N.Comparator, Number>> conditions) {
        if (conditions.isEmpty()) {
            return this;
        }
        if (conditions.size() == 1) {
            $.T2<N.Comparator, Number> pair = conditions.get(0);
            return having(pair._1, pair._2);
        }
        BasicDBList list = new BasicDBList();
        for ($.Pair<N.Comparator, Number> pair: conditions) {
            list.add(dbo("val", dbo(compExpression(pair._1), pair._2)));
        }
        this.having = dbo("$match", dbo("$and", list));
        return this;
    }

    /**
     * Set raw having clause
     * @param having the raw having clause
     * @return this aggregtion with having clause set
     */
    public SimpleAggregation<T> having(DBObject having) {
        this.having = having;
        return this;
    }

    /**
     * Having it must be greater than or equals to target
     * @param target the upper bound inclusive
     * @return this aggregation with having clause set
     */
    public SimpleAggregation<T> atLeast(Number target) {
        return having(GTE, target);
    }

    /**
     * Having it must be less than or equals to target
     * @param target the lower bound inclusive
     * @return this aggregation with having clause set
     */
    public SimpleAggregation<T> atMost(Number target) {
        return having(LTE, target);
    }

    /**
     * Having it must be greater than target
     * @param target the lower bound exclusive
     * @return this aggregation with having clause set
     */
    public SimpleAggregation<T> greaterThan(Number target) {
        return having(N.Comparator.GT, target);
    }

    /**
     * Having it must be less than target
     * @param target the upper bound exclusive
     * @return this aggregation with having clause set
     */
    public SimpleAggregation<T> lessThan(Number target) {
        return having(LT, target);
    }

    /**
     * Having it must be greater than or equals to `minInclusive` and less than `maxExclusive`
     * @param minInclusive the lower bound inclusive
     * @param maxExclusive the upper bound exclusive
     * @return this aggregation with having clause set
     */
    public SimpleAggregation<T> between(Number minInclusive, Number maxExclusive) {
        List<$.T2<N.Comparator, Number>> conditions = C.list($.Pair(GTE, minInclusive),$.Pair(LTE, maxExclusive));
        return having(conditions);
    }

    public SimpleAggregation<T> sorted() {
        return sorted(false);
    }

    public SimpleAggregation<T> sorted(boolean ascending) {
        this.sort = dbo("$sort", dbo("val", ascending ? 1 : -1));
        return this;
    }

    public AggregationResult<T> get() {
        E.illegalStateIf(null == group);
        List<DBObject> pipeline = C.newList();
        if (null != where && !where.isEmpty()) {
            pipeline.add(dbo("$match", where));
        }
        if (null != sample) {
            pipeline.add(sample);
        }
        pipeline.add(group);
        if (null != having) {
            pipeline.add(having);
        }
        if (null != sort) {
            pipeline.add(sort);
        }
        List<BasicDBObject> list = (List)C.list(collection.aggregate(pipeline, AggregationOptions.builder().build()));
        return new AggregationResult<T>(list, "val", groupKeyMap, modelType, valType);
    }

    public AggregationResult<Long> getAsLong() {
        return get().asLongResult();
    }

    public AggregationResult<Integer> getAsInt() {
        return get().asIntResult();
    }

    public AggregationResult<Double> getAsDouble() {
        return get().asDoubleResult();
    }

    public Map<Object, T> getAsMap() {
        return get().asMap();
    }

    public Map<Object, Double> getAsDoubleMap() {
        return getAsDouble().asMap();
    }

    public Map<Object, Integer> getAsIntMap() {
        return getAsInt().asMap();
    }

    public Map<Object, Long> getAsLongMap() {
        return getAsLong().asMap();
    }

    public SimpleAggregation<T> groupCount(String groupBy, String... otherGroupBys) {
        return group(COUNT, null, groupBy, otherGroupBys);
    }

    public SimpleAggregation<T> groupSum(String sumField, String groupBy, String... otherGroupBys) {
        return group(SUM, sumField, groupBy, otherGroupBys);
    }

    public SimpleAggregation<T> groupAverage(String avgField, String groupBy, String... otherGroupBys) {
        return group(AVG, avgField, groupBy, otherGroupBys);
    }

    public SimpleAggregation<T> groupStdDev(String stdDevField, String groupBy, String... otherGroupBys) {
        return group(STD_DEV, stdDevField, groupBy, otherGroupBys);
    }

    public SimpleAggregation<T> groupSampleStdDev(String stdDevField, int samples, String groupBy, String... otherGroupBys) {
        this.sample = dbo("$sample", dbo("size", N.require(samples).greaterThan(1)));
        return group(STD_DEV, stdDevField, groupBy, otherGroupBys);
    }

    public SimpleAggregation<T> group(Operator op, String opField, String groupBy, String... otherGroupBys) {
        opField = null == opField ? null : "$" + MorphiaService.mappedName(opField, modelType);
        DBObject groupExp;
        if (null != sample && op == STD_DEV) {
            groupExp = dbo("$stdDevSamp", opField);
        } else {
            groupExp = op.groupExpression(opField);
        }
        group = dbo("$group", dbo("_id", idObject(groupBy, otherGroupBys), "val", groupExp));
        return this;
    }

    public <NT extends Number> SimpleAggregation<NT> as(Class<NT> newValType) {
        if (newValType == this.valType) {
            return $.cast(this);
        }
        SimpleAggregation<NT> na = new SimpleAggregation<>();
        na.collection = this.collection;
        na.modelType = this.modelType;
        na.where = this.where;
        na.query = this.query;
        na.valType = newValType;
        return na;
    }

    public SimpleAggregation<Long> asLong() {
        return as(Long.class);
    }

    public SimpleAggregation<Double> asDouble() {
        return as(Double.class);
    }

    public SimpleAggregation<Integer> asInt() {
        return as(Integer.class);
    }

    private DBObject idObject(DBObject idObject) {
        if (!idObject.containsField("_id")) {
            return dbo("_id", idObject);
        }
        return idObject;
    }

    private Object idObject(String groupBy, String... otherGroupBys) {
        List<String> groupKeys = canonicalGroupKeys(groupBy, otherGroupBys);
        int len = groupKeys.size();
        if (0 == len) {
            return null;
        }
        if (1 == len) {
            String key = groupKeys.get(0);
            return null == key ? null : "$" + key;
        }
        DBObject id = new BasicDBObject();
        for (String key : groupKeys) {
            id.put(key, "$" + key);
        }
        return id;
    }

    private List<String> canonicalGroupKeys(String key, String... otherKeys) {
        if (null == key) {
            return C.list();
        }
        List<String> list = new ArrayList<String>();
        String[] sa = MorphiaService.splitGroupKeys(key);
        for (String s : sa) {
            String mappedName = query.mappedName(s);
            groupKeyMap.put(mappedName, s);
            list.add(mappedName);
        }
        for (String key0 : otherKeys) {
            sa = MorphiaService.splitGroupKeys(key0);
            for (String s : sa) {
                String mappedName = query.mappedName(s);
                groupKeyMap.put(mappedName, s);
                list.add(mappedName);
            }
        }
        return list;
    }

    /**
     * Returns DBObject help to group time field by date - exclude the time portion
     *
     * @param dateTimeField the name of the date time field
     * @return DBObject for group on the field by date
     */
    public static DBObject byDate(String dateTimeField) {
        String field = "$" + dateTimeField;
        return dbo(dateTimeField, dbo(
                "month", dbo("$month", field),
                "day", dbo("$dayOfMonth", field),
                "year", dbo("$year", field)
        ));
    }

    /**
     * Returns DBObject help to group time field by month - exclude the day and time portion
     *
     * @param dateTimeField the name of the date time field
     * @return DBObject for group on the field by month
     */
    public static DBObject byMonth(String dateTimeField) {
        String field = "$" + dateTimeField;
        return dbo(dateTimeField, dbo(
                "month", dbo("$month", field),
                "year", dbo("$year", field)
        ));
    }

    /**
     * Returns DBObject help to group time field by year - exclude the month, day and time portion
     *
     * @param dateTimeField the name of the date time field
     * @return DBObject for group on the field by month
     */
    public static DBObject byYear(String dateTimeField) {
        String field = "$" + dateTimeField;
        return dbo(dateTimeField, dbo(
                "year", dbo("$year", field)
        ));
    }

    private static String compExpression(N.Comparator comparator) {
        switch (comparator) {
            case GT:
                return "$gt";
            case GTE:
                return "$gte";
            case LT:
                return "$lt";
            case LTE:
                return "$lte";
            case EQ:
                return "$eq";
            case NE:
                return "$ne";
        }
        throw E.unexpected("oops");
    }

}
