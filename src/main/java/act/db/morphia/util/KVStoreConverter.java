package act.db.morphia.util;

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
import act.db.morphia.annotation.PersistAsList;
import act.db.morphia.annotation.PersistAsMap;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.utils.IterHelper;
import org.osgl.util.KV;
import org.osgl.util.KVStore;
import org.osgl.util.S;
import org.osgl.util.ValueObject;

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
        setSupportedTypes(new Class[] {KVStore.class, KV.class});
        this.valueObjectConverter = new ValueObjectConverter();
        Object o = App.instance().config().get("morphia.kvstore.persist.structure");
        if (null != o) {
            persistAsList = S.eq(S.string(o), "list", S.IGNORECASE);
        }
    }

    @Override
    public Object decode(Class<?> aClass, Object fromDB, MappedField mappedField) {
        final KV store = new KVStore();
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
        Map<String, Object> map = (Map) value;
        boolean persistAsList = this.persistAsList || (null != optionalExtraInfo && optionalExtraInfo.hasAnnotation(PersistAsList.class) && !optionalExtraInfo.hasAnnotation(PersistAsMap.class));
        if (persistAsList) {
            BasicDBList list = new BasicDBList();
            for (String key : map.keySet()) {
                ValueObject vo = ValueObject.of(map.get(key));
                BasicDBObject dbObject = new BasicDBObject();
                dbObject.put(KEY, key);
                dbObject.put(VALUE, valueObjectConverter.encode(vo, optionalExtraInfo));
                list.add(dbObject);
            }
            return list;
        } else {
            return encodeAsMap(map);
        }
    }

    public Map<String, Object> encodeAsMap(Map<String, ?> map) {
        final Map<String, Object> mapForDb = new HashMap();
        for (final Map.Entry<String, ?> entry : map.entrySet()) {
            mapForDb.put(entry.getKey(), valueObjectConverter.encode(entry.getValue(), null));
        }
        return mapForDb;
    }

}
