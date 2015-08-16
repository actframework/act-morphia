package act.db.morphia.util;

import org.joda.time.DateTime;
import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;

/**
 * The joda date time converter.
 * Disclaim: Source code copied from
 * http://grepcode.com/file/repo1.maven.org/maven2/io.rtr.alchemy/alchemy-db-mongo/0.1.10/io/rtr/alchemy/db/mongo/util/DateTimeConverter.java
 */
public class DateTimeConverter extends TypeConverter implements SimpleValueConverter {
    public DateTimeConverter() {
        setSupportedTypes(new Class[]{DateTime.class});
    }

    @Override
    public Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo) {
        if (null == fromDBObject) {
            return null;
        }
        final Long instant = (Long) fromDBObject;
        return new DateTime(instant);
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
        if (null == value) {
            return null;
        }
        final DateTime dateTime = (DateTime) value;
        return dateTime.getMillis();
    }

}
