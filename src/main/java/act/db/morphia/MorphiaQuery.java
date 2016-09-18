package act.db.morphia;

import act.db.Dao;
import act.db.morphia.util.AggregationResult;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;
import org.bson.types.CodeWScope;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.*;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MorphiaQuery<MODEL_TYPE> implements Dao.Query<MODEL_TYPE, MorphiaQuery<MODEL_TYPE>>, Query<MODEL_TYPE> {

    private Query<MODEL_TYPE> mq;
    private Class<MODEL_TYPE> modelType;
    private Datastore ds;
    private MorphiaDaoBase<?, MODEL_TYPE> dao;

    public MorphiaQuery(MorphiaDaoBase<?, MODEL_TYPE> dao) {
        this.ds = dao.ds();
        this.modelType = dao.modelType();
        this.mq = ds.createQuery(modelType);
        this.dao = dao;
    }

    public MorphiaQuery<MODEL_TYPE> filter(String key, Object val) {
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
        StringBuilder sb = S.builder();
        for (String s: spec) {
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
        return mq.getCollection();
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


    public AggregationResult groupMax(String field, String... groupKeys) {
        return groupBy(groupKeys).on(field).max();
    }

    public Long max(String maxField) {
        return groupMax(maxField).getDefault();
    }

    public AggregationResult groupMin(String field, String... groupKeys) {
        return groupBy(groupKeys).on(field).min();
    }

    public Long min(String minField) {
        return groupMin(minField).getDefault();
    }

    public AggregationResult groupAverage(String field, String... groupKeys) {
        return groupBy(groupKeys).on(field).average();
    }

    public Long average(String field) {
        return groupAverage(field).getDefault();
    }

    public AggregationResult groupSum(String field, String... groupKeys) {
        return groupBy(groupKeys).on(field).sum();
    }

    public Long sum(String field) {
        return groupSum(field).getDefault();
    }

    public AggregationResult groupCount(String... groupKeys) {
        return groupBy(groupKeys).count();
    }

    private AggregationResult aggregate_(String mappedField, DBObject initial,
                                         Long initVal, String reduce, String finalize,
                                         String... groupKeys) {
        if (null == initial) {
            initial = new BasicDBObject();
        }
        initial.put(mappedField, initVal);
        return new AggregationResult(group(S.join(",", groupKeys), initial, reduce, finalize), mappedField, modelType);
    }

    private List<BasicDBObject> group(String groupKeys, DBObject initial,
                                     String reduce, String finalize) {
        DBObject key = new BasicDBObject();
        if (!S.empty(groupKeys)) {
            String[] sa = MorphiaService.splitGroupKeys(groupKeys);
            for (String s : sa) {
                key.put(s, true);
            }
        }
        return (List<BasicDBObject>) ds.getCollection(modelType).group(key, mq.getQueryObject(), initial, reduce, finalize);
    }

    public GroupBy groupBy(String ... groupByKeys) {
        return new GroupBy(groupByKeys);
    }

    private String mappedName(String fieldName) {
        return MorphiaService.mappedName(fieldName, modelType);
    }

    private enum Aggregation {
        COUNT() {

            @Override
            String _reduce(String mappedField) {
                return String.format("function(obj, prev){prev.%s++;}", mappedField);
            }
        },
        SUM() {
            @Override
            String _reduce(String mappedField) {
                return String.format("function(obj, prev){prev.%s+=obj.%s;}", mappedField, mappedField);
            }
        },
        MAX() {
            @Override
            String _reduce(String mappedField) {
                return String.format("function(obj, prev){if (obj.%s > prev.%s) prev.%s = obj.%s}",
                        mappedField, mappedField, mappedField, mappedField);
            }

            @Override
            Long _initVal() {
                return Long.MIN_VALUE + 1;
            }
        },
        MIN() {
            @Override
            String _reduce(String mappedField) {
                return String.format("function(obj, prev){if (obj.%s < prev.%s) prev.%s = obj.%s}",
                        mappedField, mappedField, mappedField, mappedField);
            }

            @Override
            Long _initVal() {
                return Long.MAX_VALUE - 1;
            }
        },
        AVERAGE() {
            @Override
            DBObject _initial() {
                DBObject initial = new BasicDBObject();
                initial.put("__count", 0);
                initial.put("__sum", 0);
                return initial;
            }

            @Override
            String _reduce(String mappedField) {
                return String.format("function(obj, prev){prev.__count++; prev.__sum+=obj.%s;}", mappedField);
            }


            @Override
            String _finalize(String mappedField) {
                return String.format("function(prev) {prev.%s = prev.__sum / prev.__count;}", mappedField);
            }
        }
        ;
        DBObject _initial() {
            return null;
        }
        abstract String _reduce(String mappedField);
        String _finalize (String mappedField) {
            return null;
        }
        Long _initVal() {
            return 0L;
        }
    }

    public class GroupBy {
        private String[] groupKeys;
        private String field;
        private GroupBy(String... groupKeys) {
            this.groupKeys = canonicalGroupKeys(groupKeys);
        }

        public GroupBy on(String aggregationField) {
            this.field = mappedName(aggregationField);
            return this;
        }

        public AggregationResult count() {
            this.field = "_id";
            return aggregate(Aggregation.COUNT);
        }

        public AggregationResult sum() {
            return aggregate(Aggregation.SUM);
        }

        public AggregationResult average() {
            return aggregate(Aggregation.AVERAGE);
        }

        public AggregationResult max() {
            return aggregate(Aggregation.MAX);
        }

        public AggregationResult min() {
            return aggregate(Aggregation.MIN);
        }

        private AggregationResult aggregate(Aggregation type) {
            E.illegalArgumentIf(null == field, "It must specify the field on which aggregation will be calculated");
            return aggregate_(field,
                    type._initial(),
                    type._initVal(),
                    type._reduce(field),
                    type._finalize(field),
                    groupKeys);
        }

        private String[] canonicalGroupKeys(String... keys) {
            List<String> list = new ArrayList<String>();
            for (String key : keys) {
                String[] sa = MorphiaService.splitGroupKeys(key);
                for (String s: sa) {
                    list.add(mappedName(s));
                }
            }
            return list.toArray(new String[list.size()]);
        }

    }
}
