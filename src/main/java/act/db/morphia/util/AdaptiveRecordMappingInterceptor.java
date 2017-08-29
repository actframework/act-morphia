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

import act.db.AdaptiveRecord;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.DBObject;
import org.mongodb.morphia.AbstractEntityInterceptor;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.converters.Converters;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.utils.IterHelper;
import org.osgl.$;
import org.osgl.cache.CacheService;
import org.osgl.util.C;
import org.osgl.util.S;
import org.osgl.util.ValueObject;

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

    private final Set<String> BUILT_IN_PROPS = C.setOf("_id,className,_created,_modified".split(","));
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
                Object value = entry.getValue();
                Converters converters = mapper.getConverters();
                value = value instanceof ValueObject ? ValueObjectConverter.INSTANCE.encode(value) : converters.hasSimpleValueConverter(value) ? converters.encode(value) : value;
                dbObj.put(entry.getKey(), value);
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
