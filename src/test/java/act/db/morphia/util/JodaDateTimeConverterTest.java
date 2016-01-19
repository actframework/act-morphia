package act.db.morphia.util;

import act.db.morphia.TestBase;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.junit.Test;
import org.osgl.$;

public class JodaDateTimeConverterTest extends TestBase {
    @Test
    public void testDateTime() {
        long ts = $.ms();
        DateTime now = new DateTime(ts);
        long l = now.getMillis();
        eq(l, ts);
    }

    @Test
    public void testLocalDate() {
        LocalDate today = LocalDate.now();
        long l = today.toDateTimeAtStartOfDay().getMillis();
        LocalDate localDate = new LocalDate(l);
        eq(today, localDate);
    }

    @Test
    public void testLocalTime() {
        LocalTime now = LocalTime.now();
        long l = now.toDateTime(new DateTime(0)).getMillis();
        LocalTime localTime = new LocalTime(l);
        eq(now, localTime);
    }
}
