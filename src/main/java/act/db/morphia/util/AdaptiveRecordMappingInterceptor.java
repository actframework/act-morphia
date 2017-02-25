package act.db.morphia.util;

import act.db.AdaptiveRecord;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.DBObject;
import org.mongodb.morphia.AbstractEntityInterceptor;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.utils.IterHelper;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.Map;
import java.util.Set;

public class AdaptiveRecordMappingInterceptor extends AbstractEntityInterceptor {

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
        }
    }

    private static final Set<String> BUILT_IN_PROPS = C.setOf("_id,className,_created,_modified,v".split(","));

    @Override
    public void postLoad(Object ent, DBObject dbObj, Mapper mapper) {
        Class<?> c = ent.getClass();
        if (AdaptiveRecord.class.isAssignableFrom(c)) {
            AdaptiveRecord ar = $.cast(ent);
            final Map<String, Object> kv = ar.internalMap();
            final AdaptiveRecord.MetaInfo metaInfo = ar.metaInfo();
            new IterHelper<>().loopMap(dbObj, new IterHelper.MapIterCallback<Object, Object>() {
                @Override
                public void eval(final Object k, final Object val) {
                    final String key = S.string(k);
                    if (BUILT_IN_PROPS.contains(key) || metaInfo.setterFieldSpecs.containsKey(key)) {
                        return;
                    }
                    kv.put(key, JSONObject.toJSON(val));
                }
            });
        }
    }
}