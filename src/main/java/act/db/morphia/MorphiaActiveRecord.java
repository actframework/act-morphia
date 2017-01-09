package act.db.morphia;

import act.Act;
import act.db.ActiveRecord;
import act.db.morphia.util.KVStoreConverter;
import act.db.morphia.util.ValueObjectConverter;
import act.inject.param.NoBind;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.mongodb.morphia.AbstractEntityInterceptor;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.utils.IterHelper;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.KVStore;
import org.osgl.util.S;
import org.osgl.util.ValueObject;
import relocated.morphia.org.apache.commons.collections.DefaultMapEntry;

import java.util.*;

/**
 * Implement {@link ActiveRecord} in Morphia
 */
public abstract class MorphiaActiveRecord<MODEL_TYPE extends MorphiaActiveRecord> extends MorphiaModel<MODEL_TYPE> implements ActiveRecord<ObjectId, MODEL_TYPE> {

    @Transient
    @NoBind
    private KVStore kv = new KVStore();

    @Transient
    private transient volatile ActiveRecord.MetaInfo metaInfo;

    // --- implement KV
    @Override
    public MODEL_TYPE putValue(String key, Object val) {
        $.Func2 setter = metaInfo().fieldSetters.get(key);
        if (null != setter) {
            setter.apply(this, val);
        } else {
            kv.putValue(key, val);
        }
        return _me();
    }

    @Override
    public <T> T getValue(String key) {
        $.Function getter = metaInfo().fieldGetters.get(key);
        if (null != getter) {
            return (T) getter.apply(this);
        }
        return kv.getValue(key);
    }

    @Override
    public MODEL_TYPE putValues(Map<String, Object> kvMap) {
        for (Map.Entry<String, Object> entry: kvMap.entrySet()) {
            putValue(entry.getKey(), entry.getValue());
        }
        return _me();
    }

    @Override
    public boolean containsKey(String key) {
        return kv.containsKey(key) || metaInfo().fieldTypes.containsKey(key);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = kv.toMap();
        for (Map.Entry<String, $.Function> entry : metaInfo().fieldGetters.entrySet()) {
            map.put(entry.getKey(), entry.getValue().apply(this));
        }
        return map;
    }

    @Override
    public int size() {
        return kv.size() + fieldsSize();
    }

    @Override
    public Set<String> keySet() {
        if (!hasFields()) {
            return kv.keySet();
        }
        Set<String> set = new HashSet<String>(metaInfo().fieldTypes.keySet());
        set.addAll(kv.keySet());
        return set;
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        if (!hasFields()) {
            return kv.toMap().entrySet();
        }
        Set<Map.Entry<String, Object>> set = new HashSet<Map.Entry<String, Object>>(kv.toMap().entrySet());
        for (Map.Entry<String, $.Function> entry: metaInfo().fieldGetters.entrySet()) {
            String fieldName = entry.getKey();
            $.Function getter = entry.getValue();
            set.add(new DefaultMapEntry(fieldName, getter.apply(this)));
        }
        return set;
    }

    public Map<String, Object> asMap() {
        final ActiveRecord ar = this;
        // TODO: should we check the field value on size, remove, containsXxx etc methods?
        return new Map<String, Object>() {
            @Override
            public int size() {
                return ar.size();
            }

            @Override
            public boolean isEmpty() {
                return ar.size() == 0;
            }

            @Override
            public boolean containsKey(Object key) {
                return kv.containsKey(key) || metaInfo().fieldTypes.containsKey(key);
            }

            @Override
            public boolean containsValue(Object value) {
                return kv.containsValue(value);
            }

            @Override
            public Object get(Object key) {
                $.Function getter = metaInfo().fieldGetters.get(key);
                return null != getter ? getter.apply(this) : kv.getValue((String)key);
            }

            @Override
            public Object put(String key, Object value) {
                $.Func2 setter = metaInfo().fieldSetters.get(key);
                if (null != setter) {
                    Object o = get(key);
                    setter.apply(this, value);
                    return o;
                }
                return kv.putValue(key, value);
            }

            @Override
            public Object remove(Object key) {
                $.Function getter = metaInfo().fieldGetters.get(key);
                if (null != getter) {
                    return null;
                } else {
                    return kv.remove(key).value();
                }
            }

            @Override
            public void putAll(Map<? extends String, ?> m) {
                putValues((Map)m);
            }

            @Override
            public void clear() {
                kv.clear();
                // TODO: should we clear field values?
            }

            @Override
            public Set<String> keySet() {
                return ar.keySet();
            }

            @Override
            public Collection<Object> values() {
                List<Object> list = new ArrayList<Object>();
                for (ValueObject vo : kv.values()) {
                    list.add(vo.value());
                }
                for ($.Function getter : metaInfo().fieldGetters.values()) {
                    list.add(getter.apply(this));
                }
                return list;
            }

            @Override
            public Set<Entry<String, Object>> entrySet() {
                return ar.entrySet();
            }
        };
    }

    private int fieldsSize() {
        return metaInfo().fields.size();
    }

    private boolean hasFields() {
        return !metaInfo().fields.isEmpty();
    }

    @Override
    public ActiveRecord.MetaInfo metaInfo() {
        if (null == metaInfo) {
            synchronized (this) {
                if (null == metaInfo) {
                    ActiveRecord.MetaInfo.Repository r = Act.appServicePluginManager().get(ActiveRecord.MetaInfo.Repository.class);
                    metaInfo = r.get(getClass(), new $.Transformer<Class<? extends ActiveRecord>, ActiveRecord.MetaInfo>() {
                        @Override
                        public ActiveRecord.MetaInfo transform(Class<? extends ActiveRecord> aClass) {
                            return new ActiveRecord.MetaInfo(aClass, Transient.class);
                        }
                    });
                }
            }
        }
        return metaInfo;
    }

    public static class ActiveRecordMappingInterceptor extends AbstractEntityInterceptor {

        private KVStoreConverter kvStoreConverter = new KVStoreConverter();
        private ValueObjectConverter valueObjectConverter = new ValueObjectConverter();

        @Override
        public void prePersist(Object ent, DBObject dbObj, Mapper mapper) {
            if (null == ent) {
                return;
            }
            Class<?> c = ent.getClass();
            if (MorphiaActiveRecord.class.isAssignableFrom(c)) {
                MorphiaActiveRecord ar = $.cast(ent);
                KVStore kv = ar.kv;
                Map<String, Object> converted = kvStoreConverter.encodeAsMap(kv);
                for (Map.Entry<String, Object> entry : converted.entrySet()) {
                    dbObj.put(entry.getKey(), entry.getValue());
                }
            }
        }

        private static final Set<String> BUILT_IN_PROPS = C.setOf("_id,className,_created,_modified,v".split(","));

        @Override
        public void postLoad(Object ent, DBObject dbObj, Mapper mapper) {
            Class<?> c = ent.getClass();
            if (MorphiaActiveRecord.class.isAssignableFrom(c)) {
                MorphiaActiveRecord ar = $.cast(ent);
                final KVStore kv = ar.kv;
                final ActiveRecord.MetaInfo metaInfo = ar.metaInfo();
                new IterHelper<Object, Object>().loopMap(dbObj, new IterHelper.MapIterCallback<Object, Object>() {
                    @Override
                    public void eval(final Object k, final Object val) {
                        final String key = S.string(k);
                        if (BUILT_IN_PROPS.contains(key) || metaInfo.fieldTypes.containsKey(key)) {
                            return;
                        }
                        if (!metaInfo.fieldTypes.containsKey(key)) {
                            kv.putValue(key, valueObjectConverter.decode(ValueObject.class, val));
                        }
                    }
                });
            }
        }
    }
}
