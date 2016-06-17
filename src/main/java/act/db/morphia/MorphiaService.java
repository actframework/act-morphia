package act.db.morphia;

import act.ActComponent;
import act.app.App;
import act.app.DbServiceManager;
import act.app.event.AppEventId;
import act.db.Dao;
import act.db.DbService;
import act.db.morphia.util.FastJsonObjectIdCodec;
import act.di.DependencyInjectionBinder;
import act.util.FastJsonIterableSerializer;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.mapping.DefaultCreator;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MapperOptions;
import org.mongodb.morphia.query.MorphiaIterator;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static act.app.App.logger;

@ActComponent
@Singleton
public class MorphiaService extends DbService {

    // the morphia instance - keep track of class mapping
    private static Morphia morphia;

    private Datastore ds;
    private ConcurrentMap<Class<?>, Dao> daoMap;

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
        daoMap = new ConcurrentHashMap<Class<?>, Dao>();
        $.T2<MongoClientURI, MongoClient> t2 = ClientManager.register(this, conf);
        initDataStore(t2, conf);
        delayedEnsureIndexesAndCaps(app);
        registerFastJsonConfig();
        app.registerSingleton(MorphiaService.class, this);
        app.jobManager().on(AppEventId.DEPENDENCY_INJECTOR_LOADED, new Runnable() {
            @Override
            public void run() {
                app().eventBus().emit(new DependencyInjectionBinder<Morphia>(this, Morphia.class) {
                    @Override
                    public Morphia resolve(App app) {
                        return MorphiaService.morphia();
                    }
                });
                app().eventBus().emit(new DependencyInjectionBinder<Mapper>(this, Mapper.class) {
                    @Override
                    public Mapper resolve(App app) {
                        return MorphiaService.mapper();
                    }
                });
            }
        });
    }

    @Override
    protected void releaseResources() {
        ClientManager.release(this);
        morphia = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <DAO extends Dao> DAO defaultDao(Class<?> modelType) {
        if (MorphiaModel.class.isAssignableFrom(modelType)) {
            return $.cast(new MorphiaDao(modelType, ds));
        }
        return $.cast(new MorphiaDaoBase(modelType, ds));
    }

    @Override
    public <DAO extends Dao> DAO newDaoInstance(Class<DAO> daoType) {
        E.illegalArgumentIf(!MorphiaDaoBase.class.isAssignableFrom(daoType), "expected MorphiaDaoBase, found: %s", daoType);
        MorphiaDaoBase dao = $.cast(app().newInstance(daoType));
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

    static Morphia morphia() {
        return morphia;
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

    public static Mapper mapper() {
        return morphia.getMapper();
    }
}
