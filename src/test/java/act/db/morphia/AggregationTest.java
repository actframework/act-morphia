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

    AggregationResult result;

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
        result = dao.groupBy("region, dep").on("price").sum();
        eq(1200, result.get("QLD", "Marketing"));

        result = dao.groupSum("price", "department");

        eq(2200, result.getByGroupKeys("department", "Marketing"));
        eq(2200, result.get("Marketing"));
        eq(2300, result.getByGroupKeys("department", "Logistics"));
        eq(2300, result.get("Logistics"));

        result = dao.groupSum("price", "department", "region");
        eq(1200, result.getByGroupKeys("region,department", "QLD", "Marketing"));
        eq(1500, result.get("Logistics", "NSW"));
    }

    @Test
    public void testGroupMax() {
        result = dao.groupMax("price", "department");
        eq(1200, result.getByGroupKeys("dep", "Marketing"));
        eq(1500, result.getByGroupKeys("department", "Logistics"));
        eq(1200, result.get("Marketing"));
    }

    @Test
    public void testGroupMin() {
        result = dao.groupMin("price", "department");
        eq(1000, result.getByGroupKeys("dep", "Marketing"));
        eq(800, result.getByGroupKeys("department", "Logistics"));
        eq(800, result.get("Logistics"));
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
        result = dao.groupCount("dep");
        eq(2, result.get("Marketing"));
        eq(2, result.getByGroupKeys("department", "Logistics"));

        result = dao.groupCount("dep", "region");
        eq(1, result.get("Marketing", "NSW"));

        assertNull(result.get("Marketing", "SA"));

        result = dao.q("region", "NSW").groupCount("dep");
        eq(1, result.get("Marketing"));
        eq(1, result.getByGroupKeys("dep", "Logistics"));
    }

    @Test
    public void testGroupAverage() {
        result = dao.groupAverage("price", "dep");
        eq(1100, result.get("Marketing"));
    }

}
