package act.db.morphia;

import act.db.morphia.util.AggregationResult;
import model.Order;
import org.junit.Test;
import org.osgl.util.C;

import java.util.Map;

/**
 * Test Aggregation features
 */
public class AggregationTest extends MorphiaDaoTestBase<Order> {

    @Override
    protected Class<Order> entityClass() {
        return Order.class;
    }

    @Override
    protected void postPrepareData() {
        MorphiaService service = MorphiaService.getService(entityClass());
        Map<String, String> mapping = C.map("department", "dep", "region", "reg");
        service.registerFieldNameMapping(entityClass(), mapping);
    }

    @Test
    public void testCount() {
        eq(4, dao.count());
    }

    @Test
    public void testSum() {
        eq(4500, dao.q().sum("price"));
    }

    @Test
    public void testSumWithCriteria() {
        eq(2200, dao.q("department", "Marketing").sum("price"));
    }

    @Test
    public void testGroupSum() {
        AggregationResult result = dao.groupSum("price", "department");

        eq(2200, result.getResultByGroupKeys("department", "Marketing"));
        eq(2200, result.getResult("Marketing"));
        eq(2300, result.getResultByGroupKeys("department", "Logistics"));
        eq(2300, result.getResult("Logistics"));

        result = dao.groupSum("price", "department", "region");
        eq(1200, result.getResultByGroupKeys("region,department", "QLD", "Marketing"));
        eq(1500, result.getResult("Logistics", "NSW"));

        result = dao.groupBy("region, dep").on("price").sum();
        eq(1200, result.getResult("QLD", "Marketing"));
    }

    @Test
    public void testGroupMax() {
        AggregationResult result = dao.groupMax("price", "department");
        eq(1200, result.getResultByGroupKeys("dep", "Marketing"));
        eq(1500, result.getResultByGroupKeys("department", "Logistics"));
        eq(1200, result.getResult("Marketing"));
    }

    @Test
    public void testGroupMin() {
        AggregationResult result = dao.groupMin("price", "department");
        eq(1000, result.getResultByGroupKeys("dep", "Marketing"));
        eq(800, result.getResultByGroupKeys("department", "Logistics"));
        eq(800, result.getResult("Logistics"));
    }

    @Test
    public void testMin() {
        eq(800, dao.min("price"));
    }

    @Test
    public void testMax() {
        eq(1500, dao.max("price"));
    }

    @Test
    public void testGroupCount() {
        AggregationResult result = dao.groupCount("dep");
        eq(2, result.getResult("Marketing"));
        eq(2, result.getResultByGroupKeys("department", "Logistics"));

        result = dao.groupCount("dep", "region");
        eq(1, result.getResult("Marketing", "NSW"));

        assertNull(result.getResult("Marketing", "SA"));

        result = dao.q("region", "NSW").groupCount("dep");
        eq(1, result.getResult("Marketing"));
        eq(1, result.getResultByGroupKeys("dep", "Logistics"));
    }

    @Test
    public void testGroupAverage() {
        AggregationResult result = dao.groupAverage("price", "dep");
        eq(1100, result.getResult("Marketing"));
    }

}
