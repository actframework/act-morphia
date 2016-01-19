package act.db.morphia.util;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
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
        setSupportedTypes(new Class[]{
                DateTime.class,
                LocalDate.class,
                LocalDateTime.class,
                LocalTime.class
        });
    }

    @Override
    public Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo) {
        if (null == fromDBObject) {
            return null;
        }
        final Long instant = (Long) fromDBObject;
        if (targetClass == DateTime.class) {
            return new DateTime(instant);
        } else if (targetClass == LocalDateTime.class) {
            return new LocalDateTime(instant);
        } else if (targetClass == LocalDate.class) {
            return new LocalDate(instant);
        } else if (targetClass == LocalTime.class) {
            return new LocalTime(instant);
        } else {
            assert false;
            return null;
        }
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
        if (null == value) {
            return null;
        }
        if (value instanceof DateTime) {
            DateTime dateTime = (DateTime) value;
            return dateTime.getMillis();
        } else if (value instanceof LocalDateTime) {
            LocalDateTime localDateTime = (LocalDateTime) value;
            return localDateTime.toDateTime().getMillis();
        } else if (value instanceof LocalDate) {
            LocalDate localDate = (LocalDate) value;
            return localDate.toDateTimeAtStartOfDay().getMillis();
        } else if (value instanceof LocalTime) {
            LocalTime localTime = (LocalTime) value;
            return localTime.toDateTime(new DateTime(0)).getMillis();
        } else {
            assert false;
            return null;
        }
    }


}
