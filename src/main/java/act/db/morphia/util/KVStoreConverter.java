package act.db.morphia.util;

import act.Act;
import act.app.App;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.utils.IterHelper;
import org.osgl.util.KVStore;
import org.osgl.util.S;
import org.osgl.util.ValueObject;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * When `persistAsList` is enabled, {@link org.osgl.util.KVStore} will be persisted in a special form
 * so that application can create a indexable {@code KVStore}. The mongodb JSON structure of the
 * KVStore will be:
 *
 * ```
 *     [
 *     {"k": the-key, "v": the-value, "t": udf-type},
 *     ..
 *     ]
 * ```
 *
 * When `persistAsList` is not enabled, then persist KVStore as normal map:
 *
 * ```
 * {
 *     "the-key": "the-value",
 *     "another-key" "another-value"
 * }
 * ```
 *
 * see <a href="https://groups.google.com/forum/#!topic/morphia/TiaP6EOD-Mo">this</a>
 * thread
 */

public class KVStoreConverter extends TypeConverter implements SimpleValueConverter {

    public static final String KEY = "k";
    public static final String VALUE = "v";
    public static final String UDF_TYPE = "t";

    private ValueObjectConverter valueObjectConverter;
    private boolean persistAsList;

    public KVStoreConverter() {
        setSupportedTypes(new Class[] {KVStore.class});
        this.valueObjectConverter = new ValueObjectConverter();
        Object o = App.instance().config().get("morphia.kvstore.persist.structure");
        if (null != o) {
            persistAsList = S.eq(S.string(o), "list", S.IGNORECASE);
        }
    }

    @Override
    public Object decode(Class<?> aClass, Object fromDB, MappedField mappedField) {
        final KVStore store = new KVStore();
        boolean isStore = aClass.isInstance(store);
        if (null == fromDB) {
            return isStore ? store : Act.app().getInstance(aClass);
        }
        if (fromDB instanceof BasicDBList) {
            BasicDBList dbList = (BasicDBList) fromDB;
            int sz = dbList.size();
            for (int i = 0; i < sz; ++i) {
                BasicDBObject dbObj = (BasicDBObject) dbList.get(i);
                String key = dbObj.getString(KEY);
                Object val = dbObj.get(VALUE);
                store.putValue(key, valueObjectConverter.decode(ValueObject.class, val));
            }
        } else if (fromDB instanceof BasicDBObject) {
            new IterHelper<Object, Object>().loopMap(fromDB, new IterHelper.MapIterCallback<Object, Object>() {
                @Override
                public void eval(final Object k, final Object val) {
                    final String key = S.string(k);
                    store.putValue(key, valueObjectConverter.decode(ValueObject.class, val));
                }
            });
        }
        if (isStore) {
            return store;
        }
        Map retVal = (Map) Act.app().getInstance(aClass);
        retVal.putAll(store.toMap());
        return retVal;
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
        if (null == value) {
            return null;
        }
        Map<String, ?> store = (Map) value;
        boolean persistAsList = (this.persistAsList || optionalExtraInfo.hasAnnotation(PersistAsList.class)) && !optionalExtraInfo.hasAnnotation(PersistAsMap.class);
        if (persistAsList) {
            BasicDBList list = new BasicDBList();
            for (String key : store.keySet()) {
                ValueObject vo = ValueObject.of(store.get(key));
                BasicDBObject dbObject = new BasicDBObject();
                dbObject.put(KEY, key);
                dbObject.put(VALUE, valueObjectConverter.encode(vo, optionalExtraInfo));
                list.add(dbObject);
            }
            return list;
        } else {
            final Map mapForDb = new HashMap();
            for (final Map.Entry<String, ?> entry : store.entrySet()) {
                mapForDb.put(entry.getKey(), valueObjectConverter.encode(ValueObject.of(entry.getValue()), optionalExtraInfo));
            }
            return mapForDb;
        }
    }

}
