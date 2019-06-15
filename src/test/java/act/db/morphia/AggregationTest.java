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
        result = dao.groupBy("region, dep").on("price").sum().asIntResult();
        eq(1200, result.get("QLD", "Marketing"));

        result = dao.groupSum("price", "department").asIntResult();

        eq(2200, result.getByGroupKeys("department", "Marketing"));
        eq(2200, result.get("Marketing"));
        eq(3500, result.getByGroupKeys("department", "Logistics"));
        eq(3500, result.get("Logistics"));

        result = dao.groupBy("department, region").on("price").sum().asIntResult();
        eq(1200, result.getByGroupKeys("region, department", "QLD", "Marketing"));
        eq(1500, result.get("Logistics", "NSW"));

        result = dao.groupSum("price", "department", "region").as(Integer.class);
        eq(1200, result.getByGroupKeys("region, department", "QLD", "Marketing"));
        eq(1500, result.get("Logistics", "NSW"));
    }

    @Test
    public void testGroupMax() {
        result = dao.groupMax("price", "department").asIntResult();
        eq(1200, result.getByGroupKeys("dep", "Marketing"));
        eq(1500, result.getByGroupKeys("department", "Logistics"));
        eq(1200, result.get("Marketing"));
    }

    @Test
    public void testGroupMin() {
        result = dao.groupMin("price", "department").asIntResult();
        eq(1000, result.getByGroupKeys("dep", "Marketing"));

        eq(800, result.getByGroupKeys("department", "Logistics"));
        eq(800, result.get("Logistics"));
    }

    @Test
    public void testGroupCount() {
        result = dao.groupCount("dep").asIntResult();
        eq(2, result.get("Marketing"));
        eq(3, result.getByGroupKeys("department", "Logistics"));

        result = result.atLeast(3);
        isNull(result.get("Marketing"));
        notNull(result.get("Logistics"));

        result = dao.groupCount("dep", "region");
        eq(1L, result.get("Marketing", "NSW"));

        assertNull(result.get("Marketing", "SA"));

        result = dao.q("region", "NSW").groupCount("dep");
        eq(1L, result.get("Marketing"));
        eq(1L, result.getByGroupKeys("dep", "Logistics"));
    }

    @Test
    public void testGroupAverage() {
        result = dao.groupAverage("price", "dep").asIntResult();
        eq(1100, result.get("Marketing"));

        result = dao.groupBy("region").on("price").average().asIntResult();
        eq(1250, result.get("NSW"));
        eq(1000, result.get("QLD"));
    }

    @Test
    public void testGroupAverageWithCriteria() {
        result = dao.q("price >", 900).groupBy("region").on("price").average().asIntResult();
        eq(1250, result.get("NSW"));
        eq(1200, result.get("QLD"));
    }

}
