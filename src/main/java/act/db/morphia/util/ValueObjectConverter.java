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
import org.osgl.util.ValueObject;

import javax.inject.Inject;

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
            String valueString = dbObject.getString(VALUE);
            Class<?> cls = $.classForName(valueType, app.classLoader());
            o = ValueObject.decode(valueString, cls);
        }
        return ValueObject.of(o);
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
        ValueObject vo = (ValueObject) value;
        if (vo.isUDF()) {
            DBObject dbObject = new BasicDBObject();
            dbObject.put(VALUE, vo.toString());
            dbObject.put(UDF_TYPE, vo.value().getClass().getName());
            return dbObject;
        }
        return vo.value();
    }
}
