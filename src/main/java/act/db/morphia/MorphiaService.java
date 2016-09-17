package act.db.morphia;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static act.app.App.logger;

public class MorphiaService extends DbService {

    // the morphia instance - keep track of class mapping
    private static Morphia morphia;

    private Datastore ds;

    /**
     * Map from Java object field name to mongodb property name
     */
    private Map<Class, Map<String, String>> fieldNameLookup;

    public MorphiaService(String id, final App app, Map<String, Object> conf) {
        super(id, app);
        if (null == morphia) {
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
        }
        fieldNameLookup = new HashMap<Class, Map<String, String>>();
        $.T2<MongoClientURI, MongoClient> t2 = ClientManager.register(this, conf);
        initDataStore(t2, conf);
        delayedEnsureIndexesAndCaps(app);
        registerFastJsonConfig();
        app.registerSingleton(MorphiaService.class, this);
        app.resolverManager().register(ObjectId.class, new StringValueResolver<ObjectId>() {
            @Override
            public ObjectId resolve(String s) {
                return new ObjectId(s);
            }
        });
    }

    @Override
    protected void releaseResources() {
        ClientManager.release(this);
        fieldNameLookup.clear();
        morphia = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <DAO extends Dao> DAO defaultDao(Class<?> modelType) {
        if (MorphiaModel.class.isAssignableFrom(modelType)) {
            return $.cast(new MorphiaDao(modelType, ds));
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

    private void initDataStore($.T2<MongoClientURI, MongoClient> t2, Map<String, Object> conf) {
        MongoClientURI uri = t2._1;
        MongoClient client = t2._2;
        String db = uri.getDatabase();
        if (S.blank(db)) {
            db = S.string(conf.get("db"));
            if (S.empty(db)) {
                db = id();
                if (DbServiceManager.DEFAULT.equals(db)) {
                    db = "test";
                    logger.warn("No \"db\" (database name) configured. Will use \"test\" as database name for the default service");
                } else {
                    logger.warn("No \"db\" (database name) configured. Will use service id \"%s\" as database name", db);
                }
            }
        }
        this.ds = morphia.createDatastore(client, db);
    }

    private void delayedEnsureIndexesAndCaps(App app) {
        app.jobManager().beforeAppStart(new Runnable() {
            @Override
            public void run() {
                ensureIndexesAndCaps();
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

    public static Morphia morphia() {
        return morphia;
    }

    public static Mapper mapper() {
        return morphia.getMapper();
    }

    static MorphiaService getService(Class<?> modelType) {
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
