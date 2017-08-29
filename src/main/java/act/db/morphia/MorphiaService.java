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

import static act.app.App.LOGGER;

import act.Act;
import act.app.App;
import act.app.DbServiceManager;
import act.db.DB;
import act.db.Dao;
import act.db.DbService;
import act.db.morphia.util.FastJsonObjectIdCodec;
import act.util.FastJsonIterableSerializer;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoInterruptedException;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.mapping.DefaultCreator;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MapperOptions;
import org.mongodb.morphia.query.MorphiaIterator;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MorphiaService extends DbService {

    public static final String QUERY_SEP = "[,;:]+";
    public static final String GROUP_SEP = S.COMMON_SEP;

    // the morphia instance - keep track of class mapping
    private Morphia morphia;

    private Datastore ds;

    private boolean initialized;

    /**
     * Map from Java object field name to mongodb property name
     */
    private Map<Class, Map<String, String>> fieldNameLookup;

    public MorphiaService(String id, final App app, Map<String, String> conf) {
        super(id, app);
        morphia = new Morphia();
        MapperOptions options = morphia.getMapper().getOptions();
        options.setObjectFactory(new DefaultCreator(){
            @Override
            protected ClassLoader getClassLoaderForClass() {
                return app.classLoader();
            }
        });
        // the TypeConverterFinder will register it
        // morphia.getMapper().getConverters().addConverter(new DateTimeConverter());
        fieldNameLookup = new HashMap<>();
        $.T2<MongoClientURI, MongoClient> t2 = ClientManager.register(this, conf);
        initDataStore(t2, conf);
        delayedEnsureIndexesAndCaps(app);
        registerFastJsonConfig();
        app.registerSingleton(MorphiaService.class, this);
        app.resolverManager().register(ObjectId.class, new StringValueResolver<ObjectId>() {
            @Override
            public ObjectId resolve(String s) {
                return S.blank(s) ? null : new ObjectId(s);
            }
        });
        initialized = true;
    }

    @Override
    public boolean initialized() {
        return initialized;
    }

    @Override
    protected void releaseResources() {
        ClientManager.release(this);
        fieldNameLookup.clear();
        morphia = null;
        if (logger.isDebugEnabled()) {
            logger.debug("Morphia shutdown: %s", id());
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
                    LOGGER.warn("No \"db\" (database name) configured. Will use \"test\" as database name for the default service");
                } else {
                    LOGGER.warn("No \"db\" (database name) configured. Will use service id \"%s\" as database name", db);
                }
            }
        }
        this.ds = morphia.createDatastore(client, db);
    }

    private void delayedEnsureIndexesAndCaps(App app) {
        app.jobManager().beforeAppStart(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureIndexesAndCaps();
                } catch (MongoInterruptedException e) {
                    if (Act.isDev()) {
                        // ignore the case caused by hot reload
                    } else {
                        LOGGER.warn(e, "Error calling ensure indexes and caps operation");
                    }
                }
            }
        });
    }

    private void ensureIndexesAndCaps() {
        ds.ensureIndexes();
        ds.ensureCaps();
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
