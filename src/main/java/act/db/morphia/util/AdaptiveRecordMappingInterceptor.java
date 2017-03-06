package act.db.morphia.util;

import act.db.AdaptiveRecord;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.DBObject;
import org.mongodb.morphia.AbstractEntityInterceptor;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.utils.IterHelper;
import org.osgl.$;
import org.osgl.cache.CacheService;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
public class AdaptiveRecordMappingInterceptor extends AbstractEntityInterceptor {

    private final Set<String> BUILT_IN_PROPS = C.setOf("_id,className,_created,_modified,v".split(","));
    private final ConcurrentMap<Class<?>, Set<String>> mappedPropertiesLookup = new ConcurrentHashMap<>();

    @Inject
    private CacheService cacheService;

    @Override
    public void prePersist(Object ent, DBObject dbObj, Mapper mapper) {
        if (null == ent) {
            return;
        }
        Class<?> c = ent.getClass();
        if (AdaptiveRecord.class.isAssignableFrom(c)) {
            AdaptiveRecord ar = $.cast(ent);
            Map<String, Object> kv = ar.internalMap();
            for (Map.Entry<String, Object> entry : kv.entrySet()) {
                dbObj.put(entry.getKey(), ValueObjectConverter.INSTANCE.encode(entry.getValue()));
            }
            Object o = kv.get("v");
            Long v = null;
            if (null != o && o instanceof Number) {
                v = ((Number) o).longValue();
            }
            if (null != v) {
                dbObj.put("v", v);
                $.setProperty(cacheService, ent, v, "v");
            }
        }
    }

    @Override
    public void postLoad(Object ent, DBObject dbObj, Mapper mapper) {
        final Class<?> c = ent.getClass();
        if (AdaptiveRecord.class.isAssignableFrom(c)) {
            AdaptiveRecord ar = $.cast(ent);
            final Map<String, Object> kv = ar.internalMap();
            final AdaptiveRecord.MetaInfo metaInfo = ar.metaInfo();
            new IterHelper<>().loopMap(dbObj, new IterHelper.MapIterCallback<Object, Object>() {
                @Override
                public void eval(final Object k, final Object val) {
                    final String key = S.string(k);
                    if (BUILT_IN_PROPS.contains(key) || metaInfo.setterFieldSpecs.containsKey(key) || mappedProperties(c).contains(key)) {
                        return;
                    }
                    kv.put(key, JSONObject.toJSON(val));
                }
            });
            kv.put("v", dbObj.get("v"));
        }
    }

    private Set<String> mappedProperties(Class c) {
        Set<String> set = mappedPropertiesLookup.get(c);
        if (null == set) {
            set = findMappedProperties(c);
            mappedPropertiesLookup.putIfAbsent(c, set);
        }
        return set;
    }

    private Set<String> findMappedProperties(Class c) {
        Set<String> set = new HashSet<>();
        List<Field> fieldList = $.fieldsOf(c, true);
        for (Field field : fieldList) {
            Property property = field.getAnnotation(Property.class);
            if (null != property) {
                set.add(property.value());
            }
        }
        return set;
    }
}