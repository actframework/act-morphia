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

import static act.db.morphia.MorphiaService.getService;

import act.Act;
import act.app.App;
import act.db.*;
import act.db.morphia.util.AggregationResult;
import act.event.EventBus;
import act.util.General;
import act.util.Stateless;
import com.mongodb.DBCollection;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.query.UpdateOperations;
import org.osgl.$;
import org.osgl.util.*;

import java.util.*;

@General
@Stateless
public class
MorphiaDaoBase<ID_TYPE, MODEL_TYPE>
        extends DaoBase<ID_TYPE, MODEL_TYPE, MorphiaQuery<MODEL_TYPE>> {

    private volatile Datastore ds;
    private App app;
    private MorphiaQuery<MODEL_TYPE> defQuery;
    private boolean isAdaptive;

    protected MorphiaDaoBase() {
        this.app = App.instance();
        probeAdaptive();
    }

    MorphiaDaoBase(Datastore ds) {
        this.app = App.instance();
        this.ds(ds);
        probeAdaptive();
    }

    MorphiaDaoBase(Class<ID_TYPE> idType, Class<MODEL_TYPE> modelType, Datastore ds) {
        //TODO infer the ID_TYPE form model type by checking @Id annotation
        super(idType, modelType);
        E.NPE(modelType, ds);
        this.ds(ds);
        this.app = App.instance();
        probeAdaptive();
    }

    protected MorphiaDaoBase(Class<ID_TYPE> idType, Class<MODEL_TYPE> modelType) {
        super(idType, modelType);
        this.app = App.instance();
        probeAdaptive();
    }

    private void probeAdaptive() {
        this.isAdaptive = AdaptiveRecord.class.isAssignableFrom(this.modelType());
    }

    protected App app() {
        return app;
    }

    public void setDatastore(Datastore ds) {
        this.ds(ds);
    }

    void ds(Datastore ds) {
        this.ds = $.requireNotNull(ds);
        this.defQuery = new MorphiaQuery<>(this);
    }

    public void modelType(Class<MODEL_TYPE> modelType) {
        this.modelType = $.requireNotNull(modelType);
    }

    public Datastore ds() {
        if (null != ds) {
            return ds;
        }
        synchronized (this) {
            if (null == ds) {
                ds(getService(modelType()).ds());
            }
        }
        return ds;
    }

    public boolean isAdaptive() {
        return isAdaptive;
    }

    public AggregationPipeline aggregationPipeline() {
        return ds().createAggregation(modelType());
    }

    @Override
    public MODEL_TYPE findById(ID_TYPE id) {
        return ds().get(modelType(), id);
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

    public MODEL_TYPE findLatest() {
        return q().orderBy("-_created").get();
    }

    public MODEL_TYPE findLastModified() {
        return q().orderBy("-_modified").get();
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
            MorphiaService service = MorphiaService.findByModelClass(entity.getClass());
            return null == service ? null : (ID_TYPE) service.mapper().getId(entity);
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
    public MODEL_TYPE save(MODEL_TYPE entity) {
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
        return entity;
    }

    @Override
    public void save(MODEL_TYPE entity, String fields, Object... values) throws IllegalArgumentException {
        Object id = getId(entity);
        E.illegalArgumentIf(null == id, "Cannot get ID of the entity specified: %s", entity);
        Map<String, Object> kvList = kvList(fields, values);
        org.mongodb.morphia.query.Query<MODEL_TYPE> q = ds().createQuery(modelType());
        q.filter("_id", id);
        UpdateOperations<MODEL_TYPE> upOps = ds().createUpdateOperations(modelType());
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
    public List<MODEL_TYPE> save(Iterable<MODEL_TYPE> entities) {
        C.List<MODEL_TYPE> list = C.list(entities);
        if (list.isEmpty()) {
            return list;
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
        return list;
    }

    @Override
    public void delete(MODEL_TYPE entity) {
        ds().delete(entity);
        EventBus eventBus = app.eventBus();
        eventBus.trigger(new DeleteEvent<>(entity));
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
    public void deleteAll() {
        ds().delete(q());
    }

    @Override
    public void drop() {
        ds().delete(ds().createQuery(modelType()));
    }

    @Override
    public MorphiaQuery<MODEL_TYPE> q() {
        return new MorphiaQuery<MODEL_TYPE>(this);
    }

    @Override
    public MorphiaQuery<MODEL_TYPE> createQuery() {
        return q();
    }

    public DBCollection collection() {
        return ds().getCollection(modelType());
    }

    @Override
    public MorphiaQuery<MODEL_TYPE> q(String keys, Object... values) {
        int len = values.length;
        String[] sa = MorphiaService.splitQueryKeys(keys);
        if (0 == len) {
            // check if it is `dao.q("_id in", ids);` case
            // see https://github.com/actframework/act-morphia/issues/16
            boolean xInArray = 1 == sa.length && keys.trim().endsWith(" in");
            E.illegalArgumentIf(!xInArray, "no values supplied");
            return q().filter(keys, values);
        }
        E.illegalArgumentIf(sa.length != len, "The number of values does not match the number of fields");
        MorphiaQuery<MODEL_TYPE> q = q();
        for (int i = 0; i < len; ++i) {
            q.filter(sa[i], values[i]);
        }
        return q;
    }

    @Override
    public MorphiaQuery<MODEL_TYPE> createQuery(String s, Object... objects) {
        return q(s, objects);
    }

    public UpdateOperations<MODEL_TYPE> updates() {
        return ds().createUpdateOperations(modelType());
    }

    public UpdateOperations<MODEL_TYPE> createUpdateOperations() {
        return updates();
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
