package act.db.morphia.util;

import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import org.osgl.util.ValueObject;

/**
 * The {@link org.osgl.util.ValueObject} converter
 */
public class ValueObjectConverter extends TypeConverter implements SimpleValueConverter {
    public ValueObjectConverter() {
        setSupportedTypes(new Class[]{ValueObject.class});
    }

    @Override
    public Object decode(Class<?> aClass, Object o, MappedField mappedField) {
        return ValueObject.of(o);
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
        return ((ValueObject) value).value();
    }
}
