package act.db.morphia;

import act.Act;
import act.app.App;
import act.db.ActiveRecord;
import act.db.morphia.util.KVStoreConverter;
import act.db.morphia.util.ValueObjectConverter;
import act.inject.param.NoBind;
import act.plugin.AppServicePlugin;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.mongodb.morphia.AbstractEntityInterceptor;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.utils.IterHelper;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.*;
import relocated.morphia.org.apache.commons.collections.DefaultMapEntry;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implement {@link ActiveRecord} in Morphia
 */
public abstract class MorphiaActiveRecord<MODEL_TYPE extends MorphiaActiveRecord> extends MorphiaModel<MODEL_TYPE> implements ActiveRecord<ObjectId, MODEL_TYPE> {

    @Transient
    @NoBind
    private KVStore kv = new KVStore();

    @Transient
    private transient volatile MetaInfo metaInfo;

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
        return kv.containsKey(key) || metaInfo().fieldNames.contains(key);
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
        Set<String> set = new HashSet<String>(fieldNames());
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
                return kv.containsKey(key) || metaInfo().fieldNames.contains(key);
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
        return fieldNames().size();
    }

    private boolean hasFields() {
        return !fieldNames().isEmpty();
    }

    protected MetaInfo metaInfo() {
        if (null == metaInfo) {
            synchronized (this) {
                if (null == metaInfo) {
                    MetaInfo.Repository r = Act.appServicePluginManager().get(MetaInfo.Repository.class);
                    metaInfo = r.get(getClass());
                }
            }
        }
        return metaInfo;
    }

    protected Set<String> fieldNames() {
        return metaInfo().fieldNames;
    }

    protected Set<Field> fields() {
        return metaInfo().fields;
    }

    public static class MetaInfo {
        private Class<? extends MorphiaActiveRecord> arClass;
        private String className;
        private Set<Field> fields;
        private Set<String> fieldNames;
        private Map<String, $.Function> fieldGetters;
        private Map<String, $.Func2> fieldSetters;

        private MetaInfo(Class<? extends MorphiaActiveRecord> clazz) {
            this.className = clazz.getName();
            this.arClass = clazz;
            this.discoverFields(clazz);
        }

        public String className() {
            return className;
        }

        private void discoverFields(Class<? extends MorphiaActiveRecord> clazz) {
            List<Field> list = $.fieldsOf(arClass, MorphiaModelBase.class, $.F.NON_STATIC_FIELD.and($.F.fieldWithAnnotation(Transient.class)).negate());
            fields = new HashSet<Field>();
            fieldNames = new HashSet<String>();
            fieldGetters = new HashMap<String, Osgl.Function>();
            fieldSetters = new HashMap<String, Osgl.Func2>();
            for (Field f : list) {
                if (!f.isAnnotationPresent(Transient.class)) {
                    fields.add(f);
                    fieldNames.add(f.getName());
                    fieldGetters.put(f.getName(), fieldGetter(f, clazz));
                    fieldSetters.put(f.getName(), fieldSetter(f, clazz));
                }
            }
        }

        private $.Func2 fieldSetter(final Field f, final Class<?> clz) {
            final String setterName = setterName(f);
            try {
                final Method m = clz.getMethod(setterName, f.getDeclaringClass());
                return new $.Func2() {
                    @Override
                    public Object apply(Object host, Object value) throws NotAppliedException, Osgl.Break {
                        try {
                            m.invoke(host, value);
                            return null;
                        } catch (IllegalAccessException e) {
                            throw E.unexpected("Class.getMethod(String) return a method[%s] that is not accessible?", m);
                        } catch (InvocationTargetException e) {
                            throw E.unexpected(e.getTargetException(), "Error invoke setter method on %s::%s", clz.getName(), setterName);
                        }
                    }
                };
            } catch (NoSuchMethodException e) {
                f.setAccessible(true);
                return new $.Func2() {
                    @Override
                    public Object apply(Object host, Object value) throws NotAppliedException, Osgl.Break {
                        try {
                            f.set(host, value);
                            return null;
                        } catch (IllegalAccessException e1) {
                            throw E.unexpected("Field[%s] is not accessible?", f);
                        }
                    }
                };
            }
        }


        private $.Function fieldGetter(final Field f, final Class<?> clz) {
            final String getterName = getterName(f);
            try {
                final Method m = clz.getMethod(getterName);
                return new $.Function() {
                    @Override
                    public Object apply(Object o) throws NotAppliedException, Osgl.Break {
                        try {
                            return m.invoke(o);
                        } catch (IllegalAccessException e) {
                            throw E.unexpected("Class.getMethod(String) return a method[%s] that is not accessible?", m);
                        } catch (InvocationTargetException e) {
                            throw E.unexpected(e.getTargetException(), "Error invoke getter method on %s::%s", clz.getName(), getterName);
                        }
                    }
                };
            } catch (NoSuchMethodException e) {
                f.setAccessible(true);
                return new $.Function() {
                    @Override
                    public Object apply(Object o) throws NotAppliedException, Osgl.Break {
                        try {
                            return f.get(o);
                        } catch (IllegalAccessException e1) {
                            throw E.unexpected("Field[%s] is not accessible?", f);
                        }
                    }
                };
            }
        }

        private String getterName(Field field) {
            boolean isBoolean = field.getType() == Boolean.class || field.getType() == boolean.class;
            return (isBoolean ? "is" : "get") + S.capFirst(field.getName());
        }

        private String setterName(Field field) {
            return "set" + S.capFirst(field.getName());
        }

        public static class Repository extends AppServicePlugin {
            @Override
            protected void applyTo(App app) {
            }

            private ConcurrentMap<Class<?>, MetaInfo> map = new ConcurrentHashMap<Class<?>, MetaInfo>();

            public MetaInfo get(Class<? extends MorphiaActiveRecord> clazz) {
                MetaInfo info = map.get(clazz);
                if (null == info) {
                    info = new MetaInfo(clazz);
                    map.putIfAbsent(clazz, info);
                }
                return info;
            }
        }
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
                final MetaInfo metaInfo = ar.metaInfo();
                new IterHelper<Object, Object>().loopMap(dbObj, new IterHelper.MapIterCallback<Object, Object>() {
                    @Override
                    public void eval(final Object k, final Object val) {
                        final String key = S.string(k);
                        if (BUILT_IN_PROPS.contains(key) || metaInfo.fieldNames.contains(key)) {
                            return;
                        }
                        if (!metaInfo.fieldNames.contains(key)) {
                            kv.putValue(key, valueObjectConverter.decode(ValueObject.class, val));
                        }
                    }
                });
            }
        }
    }
}
