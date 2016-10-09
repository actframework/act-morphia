package act.db.morphia;

import act.Act;
import act.app.App;
import act.db.*;
import act.db.morphia.util.AggregationResult;
import act.util.General;
import com.mongodb.DBCollection;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.query.UpdateOperations;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.KVStore;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static act.db.morphia.MorphiaService.getService;

@General
public class
MorphiaDaoBase<ID_TYPE, MODEL_TYPE>
        extends DaoBase<ID_TYPE, MODEL_TYPE, MorphiaQuery<MODEL_TYPE>> {

    private volatile Datastore ds;
    private App app;
    private MorphiaQuery<MODEL_TYPE> defQuery;

    protected MorphiaDaoBase() {
        this.app = App.instance();
    }

    MorphiaDaoBase(Datastore ds) {
        this.ds = ds;
        this.app = App.instance();
        this.defQuery = new MorphiaQuery<MODEL_TYPE>(this);
    }

    @Deprecated
    MorphiaDaoBase(Class<ID_TYPE> idType, Class<MODEL_TYPE> modelType, Datastore ds) {
        //TODO infer the ID_TYPE form model type by checking @Id annotation
        super(idType, modelType);
        E.NPE(modelType, ds);
        this.ds = ds;
        this.app = App.instance();
        this.defQuery = new MorphiaQuery<MODEL_TYPE>(this);
    }

    protected MorphiaDaoBase(Class<ID_TYPE> idType, Class<MODEL_TYPE> modelType) {
        super(idType, modelType);
        this.app = App.instance();
    }

    protected App app() {
        return app;
    }

    public void setDatastore(Datastore ds) {
        this.ds = ds;
    }

    void ds(Datastore ds) {
        this.ds = $.notNull(ds);
    }

    public void modelType(Class<MODEL_TYPE> modelType) {
        this.modelType = $.notNull(modelType);
    }

    public Datastore ds() {
        if (null != ds) {
            return ds;
        }
        synchronized (this) {
            if (null == ds) {
                ds = getService(modelType()).ds();
            }
        }
        return ds;
    }

    public AggregationPipeline aggregationPipeline() {
        return ds().createAggregation(modelType());
    }

    @Override
    public MODEL_TYPE findById(ID_TYPE id) {
        return ds().get(modelType, id);
    }

    @Override
    public Iterable<MODEL_TYPE> findBy(String fields, Object... values) throws IllegalArgumentException {
        MorphiaQuery<MODEL_TYPE> q = q(fields, values);
        return q.fetch();
    }

    @Override
    public Iterable<MODEL_TYPE> findByIdList(Collection<ID_TYPE> idList) {
        MorphiaQuery<MODEL_TYPE> q = q("_id in", idList);
        return q.fetch();
    }

    @Override
    public MODEL_TYPE findOneBy(String fields, Object... values) throws IllegalArgumentException {
        MorphiaQuery<MODEL_TYPE> q = q(fields, values);
        return q.first();
    }

    @Override
    public Iterable<MODEL_TYPE> findAll() {
        return q().fetch();
    }

    @Override
    public List<MODEL_TYPE> findAllAsList() {
        return q().fetchAsList();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> distinct(MorphiaQuery q, String field) {
        return collection().distinct(field, q.morphiaQuery().getQueryObject());
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> distinct(String field) {
        return collection().distinct(field);
    }

    public List<KVStore> distinct(MorphiaQuery q, String field, String... fields) {
        List<Group> id = C.listOf(fields).prepend(field).map(new $.Transformer<String, Group>() {
            @Override
            public Group transform(String s) {
                return Group.grouping(s);
            }
        });
        AggregationPipeline pipeline = ds().createAggregation(modelType());
        if (null != q) {
            pipeline.match(q.morphiaQuery());
        }
        Iterator<DistinctResult> result = pipeline.group(id).out(DistinctResult.class);
        List<KVStore> retList = C.newList();
        while (result.hasNext()) {
            DistinctResult dr = result.next();
            retList.add(new KVStore(dr._id));
        }
        return retList;
    }

    public List<KVStore> distinct(String field, String... fields) {
        return distinct(null, field, fields);
    }

    private static class DistinctResult {
        private Map<String, Object> _id;
    }

    @Override
    public ID_TYPE getId(MODEL_TYPE entity) {
        if (entity instanceof MorphiaModel) {
            return (ID_TYPE) ((MorphiaModel) entity).getId();
        } else if (entity instanceof Model) {
            return (ID_TYPE) ((Model) entity)._id();
        } else {
            return (ID_TYPE) MorphiaService.mapper().getId(entity);
        }
    }

    @Override
    public MODEL_TYPE reload(MODEL_TYPE entity) {
        return ds().get(entity);
    }

    @Override
    public long count() {
        return ds().getCount(modelType);
    }

    @Override
    public long countBy(String fields, Object... values) throws IllegalArgumentException {
        MorphiaQuery<MODEL_TYPE> q = q(fields, values);
        return q.count();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void save(MODEL_TYPE entity) {
        if (entity instanceof TimeTrackingModel && entity instanceof Model) {
            TimeTrackingModel ttm = $.cast(entity);
            TimestampGenerator tsg = Act.dbManager().timestampGenerator(ttm._timestampType());
            if (null != tsg) {
                Object now = tsg.now();
                if (((Model) entity)._isNew()) {
                    ttm._created(now);
                }
                ttm._lastModified(now);
            }
        }
        ds().save(entity);
    }

    @Override
    public void save(MODEL_TYPE entity, String fields, Object... values) throws IllegalArgumentException {
        Object id = getId(entity);
        E.illegalArgumentIf(null == id, "Cannot get ID of the entity specified: %s", entity);
        Map<String, Object> kvList = kvList(fields, values);
        org.mongodb.morphia.query.Query<MODEL_TYPE> q = ds().createQuery(modelType);
        q.filter("_id", id);
        UpdateOperations<MODEL_TYPE> upOps = ds().createUpdateOperations(modelType);
        for (String key : kvList.keySet()) {
            upOps.set(key, kvList.get(key));
        }
        if (entity instanceof TimeTrackingModel && entity instanceof Model) {
            TimeTrackingModel ttm = $.cast(entity);
            TimestampGenerator tsg = Act.dbManager().timestampGenerator(ttm._timestampType());
            if (null != tsg) {
                Object now = tsg.now();
                upOps.set("_modified", now);
            }
        }
        ds().update(q, upOps);
    }

    @Override
    public void save(Iterable<MODEL_TYPE> entities) {
        C.List<MODEL_TYPE> list = C.list(entities);
        if (list.isEmpty()) {
            return;
        }
        MODEL_TYPE e0 = list.get(0);
        if (e0 instanceof TimeTrackingModel && e0 instanceof Model) {
            TimeTrackingModel ttm = $.cast(e0);
            TimestampGenerator tsg = Act.dbManager().timestampGenerator(ttm._timestampType());
            if (null != tsg) {
                Object now = tsg.now();
                for (MODEL_TYPE entity : entities) {
                    if (((Model) entity)._isNew()) {
                        ttm._created(now);
                    }
                    ttm._lastModified(now);
                }
            }
        }
        ds().save(entities);
    }

    @Override
    public void delete(MODEL_TYPE entity) {
        ds().delete(entity);
    }

    @Override
    public void delete(MorphiaQuery<MODEL_TYPE> query) {
        ds().delete(query.morphiaQuery());
    }

    @Override
    public void deleteById(ID_TYPE id) {
        ds().delete(modelType(), id);
    }

    @Override
    public void deleteBy(String fields, Object... values) throws IllegalArgumentException {
        ds().delete(q(fields, values).morphiaQuery());
    }

    @Override
    public void drop() {
        ds().delete(ds().createQuery(modelType()));
    }

    @Override
    public MorphiaQuery<MODEL_TYPE> q() {
        return new MorphiaQuery<MODEL_TYPE>(this);
    }

    public Class<MODEL_TYPE> modelType() {
        return modelType;
    }

    public DBCollection collection() {
        return ds().getCollection(modelType());
    }

    @Override
    public MorphiaQuery<MODEL_TYPE> q(String keys, Object... values) {
        int len = values.length;
        E.illegalArgumentIf(len == 0, "no values supplied");
        String[] sa = MorphiaService.splitQueryKeys(keys);
        E.illegalArgumentIf(sa.length != len, "The number of values does not match the number of fields");
        MorphiaQuery<MODEL_TYPE> q = q();
        for (int i = 0; i < len; ++i) {
            q.filter(sa[i], values[i]);
        }
        return q;
    }

    public MorphiaQuery.GroupBy groupBy(String... groupKeys) {
        return defQuery.groupBy(groupKeys);
    }

    public AggregationResult groupMax(String field, String... groupKeys) {
        return defQuery.groupMax(field, groupKeys);
    }

    public Long max(String maxField) {
        return groupMax(maxField).getDefault();
    }

    public AggregationResult groupMin(String field, String... groupKeys) {
        return defQuery.groupMin(field, groupKeys);
    }

    public Long min(String minField) {
        return groupMin(minField).getDefault();
    }

    public AggregationResult groupAverage(String field, String... groupKeys) {
        return defQuery.groupAverage(field, groupKeys);
    }

    public Long average(String field) {
        return groupAverage(field).getDefault();
    }

    public AggregationResult groupSum(String field, String... groupKeys) {
        return defQuery.groupSum(field, groupKeys);
    }

    public Long sum(String field) {
        return groupSum(field).getDefault();
    }

    public AggregationResult groupCount(String... groupKeys) {
        return defQuery.groupCount(groupKeys);
    }

    private Map<String, Object> kvList(String keys, Object... values) {
        int len = values.length;
        E.illegalArgumentIf(len == 0, "no values supplied");
        String[] sa = keys.split("[,;:]+");
        E.illegalArgumentIf(sa.length != len, "The number of values does not match the number of fields");
        Map<String, Object> kvList = C.newMap();
        for (int i = 0; i < len; ++i) {
            kvList.put(sa[i], values[i]);
        }
        return kvList;
    }



}
