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

import act.db.morphia.TestBase;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
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
