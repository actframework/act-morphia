package act.db.morphia.util;

import act.app.App;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.KVStore;
import org.osgl.util.ValueObject;

import javax.inject.Inject;

import java.util.List;
import java.util.Map;

import static act.db.morphia.util.KVStoreConverter.UDF_TYPE;
import static act.db.morphia.util.KVStoreConverter.VALUE;

/**
 * The {@link org.osgl.util.ValueObject} converter
 */
public class ValueObjectConverter extends TypeConverter implements SimpleValueConverter {

    private App app;

    @Inject
    public ValueObjectConverter(App app) {
        this.app = app;
    }

    public ValueObjectConverter() {
        setSupportedTypes(new Class[]{ValueObject.class});
    }

    @Override
    public Object decode(Class<?> aClass, Object o, MappedField mappedField) {
        if (o instanceof DBObject) {
            BasicDBObject dbObject = (BasicDBObject) o;
            String valueType = dbObject.getString(UDF_TYPE);
            Class cls = $.classForName(valueType, app.classLoader());
            if (Map.class.isAssignableFrom(cls)) {
                return new KVStoreConverter(app).decode(KVStore.class, dbObject.get(VALUE));
            } else if (List.class.isAssignableFrom(cls)) {
                BasicDBList dbList = $.cast(dbObject.get(VALUE));
                List list = C.newSizedList(dbList.size());
                for (Object item : dbList) {
                    list.add(ValueObject.of(decode(ValueObject.class, item)));
                }
                return list;
            } else {
                String valueString = dbObject.getString(VALUE);
                o = ValueObject.decode(valueString, cls);
            }
        }
        return ValueObject.of(o);
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
        ValueObject vo = (ValueObject) value;
        if (vo.isUDF()) {
            Object v = vo.value();
            Class<?> type = v.getClass();
            if (Map.class.isAssignableFrom(type)) {
                v = new KVStoreConverter(app).encode(new KVStore((Map) v));
            } else if (List.class.isAssignableFrom(type)) {
                BasicDBList dbList = new BasicDBList();
                List<Object> list = (List)v;
                for (Object item : list) {
                    dbList.add(encode(ValueObject.of(item)));
                }
                v = dbList;
            } else {
                v = v.toString();
            }
            DBObject dbObject = new BasicDBObject();
            dbObject.put(VALUE, v);
            dbObject.put(UDF_TYPE, type.getName());
            return dbObject;
        } else {
            return vo.value();
        }
    }
}
