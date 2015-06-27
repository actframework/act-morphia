package act.db.morphia;

import act.app.App;
import act.app.DbServiceManager;
import act.db.*;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.UpdateOperations;
import org.osgl._;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.inject.Inject;
import java.util.Map;

public class MorphiaDao<ID_TYPE, MODEL_TYPE, DAO_TYPE extends MorphiaDao<ID_TYPE, MODEL_TYPE, DAO_TYPE>> extends DaoBase<ID_TYPE, MODEL_TYPE, MorphiaQuery<MODEL_TYPE>, DAO_TYPE> {

    private Class<MODEL_TYPE> modelType;
    private volatile Datastore ds;
    @Inject
    private App app;

    MorphiaDao(Class<MODEL_TYPE> modelType, Datastore ds) {
        E.NPE(modelType, ds);
        this.modelType = modelType;
        this.ds = ds;
    }

    protected MorphiaDao(Class<MODEL_TYPE> modelType) {
        this.modelType = modelType;
    }

    private MorphiaService getService(String dbId, DbServiceManager mgr) {
        DbService svc = mgr.dbService(dbId);
        E.invalidConfigurationIf(null == svc, "Cannot find db service by id: %s", dbId);
        E.invalidConfigurationIf(!(svc instanceof MorphiaService), "The db service[%s|%s] is not morphia service", dbId, svc.getClass());
        return _.cast(svc);
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
    public Iterable<MODEL_TYPE> findAll() {
        return q().fetch();
    }

    @Override
    public MODEL_TYPE reload(MODEL_TYPE model) {
        return ds().get(model);
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
    public void save(MODEL_TYPE entity) {
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
    public MorphiaQuery<MODEL_TYPE> q() {
        return new MorphiaQuery<MODEL_TYPE>(ds(), modelType);
    }

    @Override
    public DAO_TYPE on(String dbId) {
        return getService(dbId, app.dbServiceManager()).dao(modelType);
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

    private <T> Object getId(T entity) {
        if (entity instanceof Model) {
            return ((Model) entity)._id();
        } else {
            return MorphiaService.mapper().getId(entity);
        }
    }
}
