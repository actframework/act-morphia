package act.db.morphia;

import act.ActComponent;
import act.app.App;
import act.app.DbServiceManager;
import act.db.Dao;
import act.db.DbService;
import act.db.morphia.util.DateTimeConverter;
import act.db.morphia.util.FastJsonObjectIdCodec;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.DefaultCreator;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MapperOptions;
import org.osgl.$;
import org.osgl.util.S;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ActComponent
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
            morphia.getMapper().getConverters().addConverter(new DateTimeConverter());
        }
        daoMap = new ConcurrentHashMap<Class<?>, Dao>();
        MongoClient client = ClientManager.register(this, conf);
        initDataStore(client, conf);
        onAppStart(app);
    }

    @Override
    protected void releaseResources() {
        ClientManager.release(this);
        morphia = null;
    }

    @Override
    protected <DAO extends Dao> DAO defaultDao(Class<?> modelType) {
        return $.cast(new MorphiaDao(modelType, ds));
    }

    public Datastore ds() {
        return ds;
    }

    static Morphia morphia() {
        return morphia;
    }

    private void initDataStore(MongoClient client, Map<String, Object> conf) {
        String db = S.string(conf.get("db"));
        if (S.empty(db)) {
            db = id();
            if (DbServiceManager.DEFAULT.equals(db)) {
                db = "test";
                logger.warn("No \"db\" (database name) configured. Will use \"test\" as database name for the default service");
            } else {
                logger.warn("No \"db\" (database name) configured. Will use service id \"%s\" as database name", db);
            }
        }
        this.ds = morphia.createDatastore(client, db);
    }

    private void onAppStart(App app) {
        app.jobManager().beforeAppStart(new Runnable() {
            @Override
            public void run() {
                ensureIndexesAndCaps();
                registerFastJsonConfig();
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

        ParserConfig parserConfig = ParserConfig.getGlobalInstance();
        parserConfig.putDeserializer(ObjectId.class, objectIdCodec);
    }

    public static Mapper mapper() {
        return morphia.getMapper();
    }
}
