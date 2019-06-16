package act.db.morphia;

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

import act.db.morphia.util.AggregationResult;
import model.Order;
import org.junit.Test;
import org.osgl.util.C;

import java.util.Map;

/**
 * Test Aggregation features
 */
public class AggregationTest extends MorphiaDaoTestBase<Order> {

    AggregationResult result;

    @Override
    protected Class<Order> entityClass() {
        return Order.class;
    }

    @Override
    protected void postPrepareData() {
        MorphiaService service = MorphiaService.getService(entityClass());
        service.init(app, C.<String, String>Map());
        Map<String, String> mapping = C.Map("department", "dep", "region", "reg");
        service.registerFieldNameMapping(entityClass(), mapping);
    }

    @Test
    public void testCount() {
        eq(5, dao.count());
    }

    @Test
    public void testSum() {
        eq(5700, dao.q().intSum("price"));
    }

    @Test
    public void testMin() {
        eq(800, dao.intMin("price"));
    }

    @Test
    public void testMax() {
        eq(1500, dao.intMax("price"));
    }

    @Test
    public void testSumWithCriteria() {
        eq(2200, dao.q("department", "Marketing").intSum("price"));
    }

    @Test
    public void testGroupSum() {
        result = dao.groupBy("region, dep").sum("price").getAsInt();
        eq(1200, result.val(C.Map("region", "QLD", "dep", "Marketing")));

        result = dao.groupBy("department").sum("price").getAsInt();

        eq(2200, result.val("Marketing"));
        eq(3500, result.val("Logistics"));

        result = dao.groupBy("department, region").sum("price").getAsInt();
        eq(1200, result.val("Marketing", "QLD"));
        eq(1500, result.val("Logistics", "NSW"));

        result = dao.groupBy("department", "region").sum("price").getAsInt();
        eq(1000, result.val("Marketing", "NSW"));
        eq(1000, result.val(C.Map("region", "NSW", "department", "Marketing")));
        eq(1500, result.val("Logistics", "NSW"));
        eq(1500, result.val(C.Map("reg", "NSW", "dep", "Logistics")));
    }

    @Test
    public void testGroupMax() {
        result = dao.groupBy("department").max("price").getAsInt();
        eq(1200, result.val( "Marketing"));
        eq(1500, result.val("Logistics"));
        eq(1200, result.val("Marketing"));
    }

    @Test
    public void testGroupMin() {
        result = dao.groupBy("department").min("price").getAsInt();
        eq(1000, result.val("Marketing"));

        eq(800, result.val("Logistics"));
        eq(800, result.val("Logistics"));
    }

    @Test
    public void testGroupCount() {
        result = dao.groupBy("dep").count().getAsInt();
        eq(2, result.val("Marketing"));
        eq(3, result.val("Logistics"));

        result = dao.groupBy("dep", "region").count().get();
        eq(1L, result.val("Marketing", "NSW"));

        assertNull(result.val("Marketing", "SA"));

        result = dao.q("region", "NSW").groupBy("dep").count().get();
        eq(1L, result.val("Marketing"));
        eq(1L, result.val("Logistics"));
    }

    @Test
    public void testGroupAverage() {
        result = dao.groupBy("dep").average("price").getAsInt();
        eq(1100, result.val("Marketing"));

        result = dao.groupBy("region").average("price").getAsInt();
        eq(1250, result.val("NSW"));
        eq(1000, result.val("QLD"));
    }

    @Test
    public void testGroupAverageWithCriteria() {
        result = dao.q("price >", 900).groupBy("region").average("price").getAsInt();
        eq(1250, result.val("NSW"));
        eq(1200, result.val("QLD"));
    }

}
