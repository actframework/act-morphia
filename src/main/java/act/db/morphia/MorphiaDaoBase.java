package act.db.morphia;

import act.Act;
import act.ActComponent;
import act.app.App;
import act.app.DbServiceManager;
import act.db.*;
import act.util.General;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.UpdateOperations;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@ActComponent
@General
public class
MorphiaDaoBase<ID_TYPE, MODEL_TYPE>
        extends DaoBase<ID_TYPE, MODEL_TYPE, MorphiaQuery<MODEL_TYPE>> {

    private Class<MODEL_TYPE> modelType;
    private volatile Datastore ds;
    private App app;

    MorphiaDaoBase(Class<MODEL_TYPE> modelType, Datastore ds) {
        E.NPE(modelType, ds);
        this.modelType = modelType;
        this.ds = ds;
    }

    protected MorphiaDaoBase(Class<MODEL_TYPE> modelType) {
        this.modelType = modelType;
    }

    protected App app() {
        return app;
    }

    @Inject
    public void setApp(App app) {
        this.app = app;
    }

    public void setDatastore(Datastore ds) {
        this.ds = ds;
    }

    void ds(Datastore ds) {
        this.ds = $.notNull(ds);
    }

    private MorphiaService getService(String dbId, DbServiceManager mgr) {
        DbService svc = mgr.dbService(dbId);
        E.invalidConfigurationIf(null == svc, "Cannot find db service by id: %s", dbId);
        E.invalidConfigurationIf(!(svc instanceof MorphiaService), "The db service[%s|%s] is not morphia service", dbId, svc.getClass());
        return $.cast(svc);
    }

    private Datastore ds() {
        if (null != ds) {
            return ds;
        }
        synchronized (this) {
            if (null == ds) {
                DB db = modelType.getAnnotation(DB.class);
                String dbId = null == db ? DbServiceManager.DEFAULT : db.value();
                MorphiaService dbService = getService(dbId, app.dbServiceManager());
                E.NPE(dbService);
                ds = dbService.ds();
            }
        }
        return ds;
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
        ds().update(q, upOps);
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
    public void drop() {
        ds().delete(ds().createQuery(modelType()));
    }

    @Override
    public MorphiaQuery<MODEL_TYPE> q() {
        return new MorphiaQuery<MODEL_TYPE>(ds(), modelType);
    }

    public Class modelType() {
        return modelType;
    }

    private MorphiaQuery<MODEL_TYPE> q(String keys, Object... values) {
        int len = values.length;
        E.illegalArgumentIf(len == 0, "no values supplied");
        String[] sa = keys.split("[,;:]+");
        E.illegalArgumentIf(sa.length != len, "The number of values does not match the number of fields");
        MorphiaQuery<MODEL_TYPE> q = q();
        for (int i = 0; i < len; ++i) {
            q.filter(sa[i], values[i]);
        }
        return q;
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
