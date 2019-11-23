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

import act.Act;
import act.app.App;
import act.app.DbServiceManager;
import act.app.event.SysEventId;
import act.db.DB;
import act.db.*;
import act.db.morphia.util.FastJsonObjectIdCodec;
import act.util.FastJsonIterableSerializer;
import act.util.Stateless;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.mongodb.*;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.mapping.*;
import org.mongodb.morphia.query.MorphiaIterator;
import org.osgl.$;
import org.osgl.inject.NamedProvider;
import org.osgl.util.*;
import osgl.version.Version;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Provider;

public class MorphiaService extends DbService {

    public static final Version VERSION = MorphiaPlugin.VERSION;

    public static final String QUERY_SEP = "[,;:]+";
    public static final String GROUP_SEP = S.COMMON_SEP;

    @Stateless
    public static class MorphiaServiceProvider implements NamedProvider<MorphiaService>, Provider<MorphiaService> {

        @Inject
        private DbServiceManager dbServiceManager;

        @Override
        public MorphiaService get() {
            return get(DB.DEFAULT);
        }

        @Override
        public MorphiaService get(String name) {
            return getService(name, dbServiceManager);
        }
    }


    // the morphia instance - keep track of class mapping
    private Morphia morphia;

    private Datastore ds;

    private boolean initialized;

    /**
     * Map from Java object field name to mongodb property name
     */
    private Map<Class, Map<String, String>> fieldNameLookup;

    public MorphiaService(String id, final App app, final Map<String, String> conf) {
        super(id, app);
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                init(app, conf);
            }
        };
        app.jobManager().post(SysEventId.DEPENDENCY_INJECTOR_LOADED, jobId("init"), runnable);
    }

    @Override
    public boolean initAsynchronously() {
        return true;
    }

    void init(final App app, Map<String, String> conf) {
        morphia = new Morphia();
        MapperOptions options = morphia.getMapper().getOptions();
        options.setObjectFactory(new DefaultCreator(){
            @Override
            public <T> T createInstance(Class<T> clazz) {
                return app.getInstance(clazz);
            }

            @Override
            public List createList(MappedField mf) {
                List list = super.createList(mf);
                if (null != mf) {
                    Class type = mf.getType();
                    if (!type.isInstance(list)) {
                        list = (List) app.getInstance(type);
                    }
                }
                return list;
            }

            @Override
            public Map createMap(MappedField mf) {
                Map map = super.createMap(mf);
                if (null != mf) {
                    Class type = mf.getType();
                    if (!type.isInstance(map)) {
                        map = (Map) app.getInstance(type);
                    }
                }
                return map;
            }

            @Override
            public Set createSet(MappedField mf) {
                Set set = super.createSet(mf);
                if (null != mf) {
                    Class type = mf.getType();
                    if (!type.isInstance(set)) {
                        set = (Set) app.getInstance(type);
                    }
                }
                return set;
            }

            @Override
            protected ClassLoader getClassLoaderForClass() {
                return app.classLoader();
            }
        });
        fieldNameLookup = new HashMap<>();
        $.T2<MongoClientURI, MongoClient> t2 = ClientManager.register(this, conf);
        initDataStore(t2, conf);
        delayedEnsureIndexesAndCaps(app);
        delayedMapExternalModels(app);
        registerFastJsonConfig();
        app.resolverManager().register(ObjectId.class, new StringValueResolver<ObjectId>() {
            @Override
            public ObjectId resolve(String s) {
                return S.blank(s) ? null : new ObjectId(s);
            }
        });
        initialized = true;
        final MorphiaService me = this;
        app.jobManager().post(SysEventId.SINGLETON_PROVISIONED, jobId("announceInitialized"),  new Runnable() {
            @Override
            public void run() {
                app.eventBus().emit(new DbServiceInitialized(me));
            }
        }, true);
    }

    @Override
    public boolean initialized() {
        return initialized;
    }

    @Override
    protected void releaseResources() {
        ClientManager.release(this);
        if (null != fieldNameLookup) {
            fieldNameLookup.clear();
            fieldNameLookup = null;
        }
        morphia = null;
        if (isDebugEnabled()) {
            debug("Morphia shutdown: %s", id());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <DAO extends Dao> DAO defaultDao(Class<?> modelType) {
        if (MorphiaModel.class.isAssignableFrom(modelType)) {
            return $.cast(new MorphiaDao(modelType, ds));
        }
        if (MorphiaModelWithLongId.class.isAssignableFrom(modelType)) {
            return $.cast(new MorphiaDaoWithLongId(modelType, ds()));
        }
        if (MorphiaModelBase.class.isAssignableFrom(modelType)) {
            Type type = modelType.getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                return $.cast(new MorphiaDaoBase((Class)((ParameterizedType) type).getActualTypeArguments()[0], modelType, ds));
            }
        }
        Class idType = findModelIdTypeByAnnotation(modelType, Id.class);
        E.illegalArgumentIf(null == idType, "Cannot find out Dao for model type[%s]: unable to identify the ID type", modelType);
        return $.cast(new MorphiaDaoBase(idType, modelType, ds));
    }

    @Override
    public <DAO extends Dao> DAO newDaoInstance(Class<DAO> daoType) {
        E.illegalArgumentIf(!MorphiaDaoBase.class.isAssignableFrom(daoType), "expected MorphiaDaoBase, found: %s", daoType);
        MorphiaDaoBase dao = $.cast(app().getInstance(daoType));
        dao.ds(ds);
        return (DAO) dao;
    }

    @Override
    public Class<? extends Annotation> entityAnnotationType() {
        return Entity.class;
    }

    public Datastore ds() {
        return ds;
    }

    public void registerFieldNameMapping(Class<?> entityClass, Map<String, String> nameMapping) {
        fieldNameLookup.put(entityClass, nameMapping);
    }

    private void initDataStore($.T2<MongoClientURI, MongoClient> t2, Map<String, String> conf) {
        MongoClientURI uri = t2._1;
        MongoClient client = t2._2;
        String db = uri.getDatabase();
        if (S.blank(db)) {
            db = S.string(conf.get("db"));
            if (S.empty(db)) {
                db = id();
                if (DbServiceManager.DEFAULT.equals(db)) {
                    db = "test";
                    warn("No \"db\" (database name) configured. Will use \"test\" as database name for the default service");
                } else {
                    warn("No \"db\" (database name) configured. Will use service id \"%s\" as database name", db);
                }
            }
        }
        this.ds = morphia.createDatastore(client, db);
    }

    private void delayedEnsureIndexesAndCaps(App app) {
        app.jobManager().on(SysEventId.START, jobId("ensureIndexesAndCaps"), new Runnable() {
            @Override
            public void run() {
                try {
                    ensureIndexesAndCaps();
                } catch (MongoInterruptedException e) {
                    if (Act.isDev()) {
                        // ignore the case caused by hot reload
                    } else {
                        warn(e, "Error calling ensure indexes and caps operation");
                    }
                }
            }
        });
    }

    private String jobId(String task) {
        return S.buffer("MorphiaService:").a(task).a("[").a(id()).a("]").toString();
    }

    private void delayedMapExternalModels(App app) {
        app.jobManager().on(SysEventId.START, jobId("mapExternalModels"), new Runnable() {
            @Override
            public void run() {
                mapExternalModels();
            }
        });
    }

    private void ensureIndexesAndCaps() {
        ds.ensureIndexes();
        ds.ensureCaps();
    }

    private void mapExternalModels() {
        ExternalModelAdaptor.applyAdaptorFor(this);
    }

    private void registerFastJsonConfig() {
        FastJsonObjectIdCodec objectIdCodec = new FastJsonObjectIdCodec();

        SerializeConfig serializeConfig = SerializeConfig.getGlobalInstance();
        serializeConfig.put(ObjectId.class, objectIdCodec);
        serializeConfig.put(MorphiaIterator.class, FastJsonIterableSerializer.instance);

        ParserConfig parserConfig = ParserConfig.getGlobalInstance();
        parserConfig.putDeserializer(ObjectId.class, objectIdCodec);
    }

    public Morphia morphia() {
        return morphia;
    }

    public Mapper mapper() {
        return morphia.getMapper();
    }

    public static MorphiaService findByModelClass(Class<?> modelClass) {
        DB db = modelClass.getAnnotation(DB.class);
        String serviceId = null == db ? "default" : db.value();
        DbService service = Act.app().dbServiceManager().dbService(serviceId);
        return service instanceof MorphiaService ? (MorphiaService) service : null;
    }

    public static List<MorphiaService> allMorphiaServices() {
        return Act.app().dbServiceManager().dbServicesByClass(MorphiaService.class);
    }

    public static String[] splitQueryKeys(String keys) {
        return keys.split(QUERY_SEP);
    }

    public static String[] splitGroupKeys(String keys) {
        return keys.split(GROUP_SEP);
    }

    public static MorphiaService getService(Class<?> modelType) {
        DB db = modelType.getAnnotation(DB.class);
        String dbId = null == db ? DbServiceManager.DEFAULT : db.value();
        return getService(dbId, App.instance().dbServiceManager());
    }

    public static String mappedName(String fieldName, Class<?> modelType) {
        MorphiaService service = getService(modelType);
        Map<String, String> mapping = service.fieldNameLookup.get(modelType);
        String mappedName = null == mapping ? fieldName : mapping.get(fieldName);
        return mappedName == null ? fieldName : mappedName;
    }

    private static MorphiaService getService(String dbId, DbServiceManager mgr) {
        DbService svc = mgr.dbService(dbId);
        E.invalidConfigurationIf(null == svc, "Cannot find db service by id: %s", dbId);
        E.invalidConfigurationIf(!(svc instanceof MorphiaService), "The db service[%s|%s] is not morphia service", dbId, svc.getClass());
        return $.cast(svc);
    }
}
