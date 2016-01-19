package act.db.morphia.util;

import act.app.App;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import org.osgl.$;
import org.osgl.util.KVStore;
import org.osgl.util.S;
import org.osgl.util.ValueObject;

import javax.inject.Inject;

/**
 * Persistent {@link org.osgl.util.KVStore} in a special form so that application can create
 * a indexable {@code KVStore}. The mongodb JSON structure of the KVStore will be:
 * <pre>
 *     [
 *     {"k": the-key, "v": the-value, "t": udf-type},
 *     ..
 *     ]
 * </pre>
 * see <a href="https://groups.google.com/forum/#!topic/morphia/TiaP6EOD-Mo">this</a>
 * thread
 */

public class KVStoreConverter extends TypeConverter implements SimpleValueConverter {

    public static final String KEY = "k";
    public static final String VALUE = "v";
    public static final String UDF_TYPE = "t";

    private App app;

    @Inject
    public KVStoreConverter(App app) {
        setSupportedTypes(new Class[] {KVStore.class});
        this.app = app;
    }

    @Override
    public Object decode(Class<?> aClass, Object fromDB, MappedField mappedField) {
        KVStore store = new KVStore();
        if (null == fromDB) {
            return store;
        }
        BasicDBList dbList = (BasicDBList) fromDB;
        int sz = dbList.size();
        for (int i = 0; i < sz; ++i) {
            BasicDBObject dbObj = (BasicDBObject) dbList.get(i);
            String key = dbObj.getString(KEY);
            Object val = dbObj.get(VALUE);
            boolean udf = dbObj.containsField(UDF_TYPE);
            if (udf) {
                String type = dbObj.getString(UDF_TYPE);
                Class cls = $.classForName(type, app.classLoader());
                val = ValueObject.decode(S.string(val), cls);
            }
            store.putValue(key, val);
        }
        return store;
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
        if (null == value) {
            return null;
        }
        KVStore store = (KVStore) value;
        BasicDBList list = new BasicDBList();
        for (String key : store.keySet()) {
            ValueObject vo = store.get(key);
            BasicDBObject dbObject = new BasicDBObject();
            dbObject.put(KEY, key);
            if (vo.isUDF()) {
                dbObject.put(VALUE, vo.toString());
                dbObject.put(UDF_TYPE, vo.value().getClass().getName());
            } else {
                dbObject.put(VALUE, vo.value());
            }
            list.add(dbObject);
        }
        return list;
    }
}
