package act.db.morphia;

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

import act.db.Dao;
import act.db.morphia.SimpleAggregation.Operator;
import act.db.morphia.annotation.NoQueryValidation;
import com.mongodb.*;
import org.bson.types.CodeWScope;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.*;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class MorphiaQuery<MODEL_TYPE> implements Dao.Query<MODEL_TYPE, MorphiaQuery<MODEL_TYPE>>, Query<MODEL_TYPE> {

    private Query<MODEL_TYPE> mq;
    private Class<MODEL_TYPE> modelType;
    private Datastore ds;
    private MorphiaDaoBase<?, MODEL_TYPE> dao;

    public MorphiaQuery(MorphiaDaoBase<?, MODEL_TYPE> dao) {
        this.ds = dao.ds();
        this.modelType = dao.modelType();
        this.dao = dao;
        this.mq = ds.createQuery(modelType);
        if (dao.isAdaptive() || modelType.isAnnotationPresent(NoQueryValidation.class)) {
            this.mq = this.mq.disableValidation();
        }
    }

    DBCollection collection() {
        return ds.getCollection(modelType);
    }

    Class<?> modelType() {
        return modelType;
    }

    public MorphiaQuery<MODEL_TYPE> filter(String key, Object val) {
        if (key.endsWith(" like")) {
            key = S.cut(key).before(" like");
            if (!(val instanceof Pattern)) {
                val = Pattern.compile(S.string(val));
            }
        }
        mq.filter(key, val);
        return this;
    }

    @Override
    public MorphiaQuery<MODEL_TYPE> offset(int pos) {
        mq.offset(pos);
        return this;
    }

    @Override
    public MorphiaQuery<MODEL_TYPE> limit(int limit) {
        mq.limit(limit);
        return this;
    }

    public MorphiaQuery batchSize(int value) {
        mq.batchSize(value);
        return this;
    }


    @Override
    public MorphiaQuery<MODEL_TYPE> orderBy(String... fieldList) {
        C.List<String> spec = C.listOf(fieldList).flatMap(S.F.SPLIT);
        S.Buffer sb = S.newBuffer();
        for (String s : spec) {
            if (s.startsWith("+")) {
                s = s.substring(1);
            }
            sb.append(s).append(",");
        }
        sb.delete(sb.length() - 1, sb.length());
        mq.order(sb.toString());
        return this;
    }

    public MorphiaQuery enableValidation() {
        mq.enableValidation();
        return this;
    }

    public MorphiaQuery disableValidation() {
        mq.disableValidation();
        return this;
    }

    public MorphiaQuery hintIndex(String idxName) {
        mq.hintIndex(idxName);
        return this;
    }

    public MorphiaQuery retrievedFields(boolean include, String... fields) {
        mq.retrievedFields(include, fields);
        return this;
    }

    @Override
    public Map<String, Object> explain(FindOptions findOptions) {
        return mq.explain(findOptions);
    }

    @Override
    public Query<MODEL_TYPE> order(Meta meta) {
        return mq.order(meta);
    }

    @Override
    public Query<MODEL_TYPE> order(Sort... sorts) {
        return mq.order(sorts);
    }

    @Override
    public Query<MODEL_TYPE> project(String s, boolean b) {
        return mq.project(s, b);
    }

    @Override
    public Query<MODEL_TYPE> project(String s, ArraySlice arraySlice) {
        return mq.project(s, arraySlice);
    }

    @Override
    public Query<MODEL_TYPE> project(Meta meta) {
        return mq.project(meta);
    }

    @Override
    public List<Key<MODEL_TYPE>> asKeyList(FindOptions findOptions) {
        return mq.asKeyList(findOptions);
    }

    @Override
    public List<MODEL_TYPE> asList(FindOptions findOptions) {
        return mq.asList(findOptions);
    }

    @Override
    public long count(CountOptions countOptions) {
        return mq.count(countOptions);
    }

    @Override
    public MorphiaIterator<MODEL_TYPE, MODEL_TYPE> fetch(FindOptions findOptions) {
        return mq.fetch(findOptions);
    }

    @Override
    public MorphiaIterator<MODEL_TYPE, MODEL_TYPE> fetchEmptyEntities(FindOptions findOptions) {
        return mq.fetchEmptyEntities(findOptions);
    }

    @Override
    public MorphiaKeyIterator<MODEL_TYPE> fetchKeys(FindOptions findOptions) {
        return mq.fetchKeys(findOptions);
    }

    @Override
    public MODEL_TYPE get(FindOptions findOptions) {
        return mq.get(findOptions);
    }

    @Override
    public Key<MODEL_TYPE> getKey(FindOptions findOptions) {
        return mq.getKey(findOptions);
    }

    public MorphiaQuery enableSnapshotMode() {
        mq.enableSnapshotMode();
        return this;
    }

    public MorphiaQuery disableSnapshotMode() {
        mq.disableSnapshotMode();
        return this;
    }

    public MorphiaQuery disableCursorTimeout() {
        mq.disableCursorTimeout();
        return this;
    }

    public MorphiaQuery enableCursorTimeout() {
        mq.enableCursorTimeout();
        return this;
    }

    public Class<MODEL_TYPE> getEntityClass() {
        return mq.getEntityClass();
    }

    @Override
    public int getBatchSize() {
        return mq.getBatchSize();
    }

    @Override
    public Query<MODEL_TYPE> cloneQuery() {
        MorphiaQuery<MODEL_TYPE> newQuery = new MorphiaQuery<MODEL_TYPE>(dao);
        newQuery.mq = mq.cloneQuery();
        return newQuery;
    }

    @Override
    public Query<MODEL_TYPE> comment(String comment) {
        mq.comment(comment);
        return this;
    }

    @Override
    public Map<String, Object> explain() {
        return mq.explain();
    }

    @Override
    public DBCollection getCollection() {
        return collection();
    }

    @Override
    public DBObject getFieldsObject() {
        return mq.getFieldsObject();
    }

    @Override
    public int getLimit() {
        return mq.getLimit();
    }

    @Override
    public int getOffset() {
        return mq.getOffset();
    }

    @Override
    public DBObject getQueryObject() {
        return mq.getQueryObject();
    }

    @Override
    public DBObject getSortObject() {
        return mq.getSortObject();
    }

    @Override
    public Query<MODEL_TYPE> lowerIndexBound(DBObject lowerBound) {
        mq.lowerIndexBound(lowerBound);
        return this;
    }

    @Override
    public Query<MODEL_TYPE> maxScan(int value) {
        mq.maxScan(value);
        return this;
    }

    @Override
    public Query<MODEL_TYPE> maxTime(long maxTime, TimeUnit maxTimeUnit) {
        mq.maxTime(maxTime, maxTimeUnit);
        return this;
    }

    @Override
    public Query<MODEL_TYPE> order(String sort) {
        mq.order(sort);
        return this;
    }

    @Override
    public Query<MODEL_TYPE> queryNonPrimary() {
        mq.queryNonPrimary();
        return this;
    }

    @Override
    public Query<MODEL_TYPE> queryPrimaryOnly() {
        mq.queryPrimaryOnly();
        return this;
    }

    @Override
    public Query<MODEL_TYPE> retrieveKnownFields() {
        mq.retrieveKnownFields();
        return this;
    }

    @Override
    public Query<MODEL_TYPE> returnKey() {
        mq.returnKey();
        return this;
    }

    @Override
    public Query<MODEL_TYPE> search(String text) {
        mq.search(text);
        return this;
    }

    @Override
    public Query<MODEL_TYPE> search(String text, String language) {
        mq.search(text, language);
        return this;
    }

    @Override
    public Query<MODEL_TYPE> upperIndexBound(DBObject upperBound) {
        mq.upperIndexBound(upperBound);
        return this;
    }

    @Override
    public Query<MODEL_TYPE> useReadPreference(ReadPreference readPref) {
        mq.useReadPreference(readPref);
        return this;
    }

    @Override
    public List<Key<MODEL_TYPE>> asKeyList() {
        return mq.asKeyList();
    }

    @Override
    public List<MODEL_TYPE> asList() {
        return mq.asList();
    }

    @Override
    public long countAll() {
        return mq.countAll();
    }

    @Override
    public MorphiaKeyIterator<MODEL_TYPE> fetchKeys() {
        return mq.fetchKeys();
    }

    @Override
    public MODEL_TYPE get() {
        return mq.get();
    }

    @Override
    public Key<MODEL_TYPE> getKey() {
        return mq.getKey();
    }

    @Override
    public MorphiaIterator<MODEL_TYPE, MODEL_TYPE> tail() {
        return mq.tail();
    }

    @Override
    public MorphiaIterator<MODEL_TYPE, MODEL_TYPE> tail(boolean awaitData) {
        return mq.tail(awaitData);
    }

    @Override
    public Iterator<MODEL_TYPE> iterator() {
        return mq.iterator();
    }

    @Override
    public MODEL_TYPE first() {
        return mq.get();
    }

    @Override
    public long count() {
        return mq.countAll();
    }

    @Override
    public MorphiaIterator<MODEL_TYPE, MODEL_TYPE> fetch() {
        return mq.fetch();
    }

    public MorphiaIterator<MODEL_TYPE, MODEL_TYPE> fetchEmptyEntities() {
        return mq.fetchEmptyEntities();
    }

    public List<MODEL_TYPE> fetchAsList() {
        return mq.asList();
    }

    public Query<MODEL_TYPE> morphiaQuery() {
        return mq;
    }

    public FieldEnd<? extends Query<MODEL_TYPE>> field(String field) {
        return $.cast(mq.field(field));
    }

    public FieldEnd<? extends CriteriaContainerImpl> criteria(
            String field) {
        return mq.criteria(field);
    }

    public CriteriaContainer and(Criteria... criteria) {
        return mq.and(criteria);
    }


    public CriteriaContainer or(Criteria... criteria) {
        return mq.or(criteria);
    }

    public MorphiaQuery where(String js) {
        mq.where(js);
        return this;
    }

    public MorphiaQuery where(CodeWScope js) {
        mq.where(js);
        return this;
    }

    public SimpleAggregation<Double> aggregation() {
        return new SimpleAggregation<>(this, Double.class);
    }

    public Double max(String maxField) {
        return new GroupByStage(this).max(maxField).get().val();
    }

    public Long longMax(String maxField) {
        return new GroupByStage(this).max(maxField).get().asLongResult().val();
    }

    public Integer intMax(String maxField) {
        return new GroupByStage(this).max(maxField).get().asIntResult().val();
    }

    public Double min(String minField) {
        return new GroupByStage(this).min(minField).get().val();
    }

    public Long longMin(String minField) {
        return new GroupByStage(this).min(minField).get().asLongResult().val();
    }

    public Integer intMin(String minField) {
        return new GroupByStage(this).min(minField).get().asIntResult().val();
    }

    public Double average(String averageField) {
        return new GroupByStage(this).average(averageField).get().val();
    }

    public Long longAverage(String averageField) {
        return new GroupByStage(this).average(averageField).get().asLongResult().val();
    }

    public Integer intAverage(String averageField) {
        return new GroupByStage(this).average(averageField).get().asIntResult().val();
    }

    public Double sum(String sumField) {
        return new GroupByStage(this).sum(sumField).get().val();
    }

    public Long longSum(String sumField) {
        return new GroupByStage(this).sum(sumField).get().asLongResult().val();
    }

    public Integer intSum(String sumField) {
        return new GroupByStage(this).sum(sumField).get().asIntResult().val();
    }
    
    public GroupByStage groupBy(String groupKey, String... otherGroupKeys) {
        return new GroupByStage(this, groupKey, otherGroupKeys);
    }

    String mappedName(String fieldName) {
        return MorphiaService.mappedName(fieldName, modelType);
    }

    public class GroupByStage {
        private String groupKey;
        private String[] otherGroupKeys;
        private MorphiaQuery<?> q;

        private GroupByStage(MorphiaQuery<?> q) {
            this.q = $.requireNotNull(q);
        }

        private GroupByStage(MorphiaQuery<?> q, String groupKey, String... otherGroupKeys) {
            this.groupKey = S.requireNotBlank(groupKey);
            this.q = $.requireNotNull(q);
            this.otherGroupKeys = canonicalGroupKeys(otherGroupKeys);
        }

        private <T extends Number> SimpleAggregation<T> agg(Class<T> valType) {
            return new SimpleAggregation<>(q, valType);
        }

        public SimpleAggregation<Long> count() {
            return agg(Long.class).groupCount(groupKey, otherGroupKeys);
        }

        public SimpleAggregation<Double> sum(String sumField) {
            return agg(Double.class).groupSum(S.requireNotBlank(sumField), groupKey, otherGroupKeys);
        }

        public SimpleAggregation<Long> longSum(String sumField) {
            return agg(Long.class).groupSum(S.requireNotBlank(sumField), groupKey, otherGroupKeys);
        }

        public SimpleAggregation<Double> average(String avgField) {
            return agg(Double.class).groupAverage(S.requireNotBlank(avgField), groupKey, otherGroupKeys);
        }

        public SimpleAggregation<Integer> intAverage(String avgField) {
            return agg(Integer.class).groupAverage(S.requireNotBlank(avgField), groupKey, otherGroupKeys);
        }

        public SimpleAggregation<Double> max(String maxField) {
            return agg(Double.class).group(Operator.MAX, S.requireNotBlank(maxField), groupKey, otherGroupKeys);
        }

        public SimpleAggregation<Long> longMax(String maxField) {
            return agg(Long.class).group(Operator.MAX, S.requireNotBlank(maxField), groupKey, otherGroupKeys);
        }

        public SimpleAggregation<Integer> intMax(String maxField) {
            return agg(Integer.class).group(Operator.MAX, S.requireNotBlank(maxField), groupKey, otherGroupKeys);
        }

        public SimpleAggregation<Double> min(String minField) {
            return agg(Double.class).group(Operator.MIN, S.requireNotBlank(minField), groupKey, otherGroupKeys);
        }

        public SimpleAggregation<Long> longMin(String minField) {
            return agg(Long.class).group(Operator.MIN, S.requireNotBlank(minField), groupKey, otherGroupKeys);
        }

        public SimpleAggregation<Integer> intMin(String minField) {
            return agg(Integer.class).group(Operator.MIN, S.requireNotBlank(minField), groupKey, otherGroupKeys);
        }

        public SimpleAggregation<Double> stdDev(String stdDevField) {
            return agg(Double.class).groupStdDev(S.requireNotBlank(stdDevField), groupKey, otherGroupKeys);
        }

        public SimpleAggregation<Double> sampleStdDev(String stdDevField, int samples) {
            return agg(Double.class).groupSampleStdDev(S.requireNotBlank(stdDevField), samples, groupKey, otherGroupKeys);
        }

    }

    private String[] canonicalGroupKeys(String... keys) {
        List<String> list = new ArrayList<String>();
        for (String key : keys) {
            String[] sa = MorphiaService.splitGroupKeys(key);
            for (String s : sa) {
                list.add(s);
            }
        }
        return list.toArray(new String[list.size()]);
    }
}
