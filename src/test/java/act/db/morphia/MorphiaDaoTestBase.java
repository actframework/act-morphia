package act.db.morphia;

import act.app.App;
import act.db.Dao;
import act.test.util.Fixture;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgl.$;

import java.lang.reflect.Field;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public abstract class MorphiaDaoTestBase<T> extends MongoTestBase {

    MorphiaDao<T> dao;
    Class<T> entityClass;

    Fixture fixture;
    Map<String, Object> initData;

    @Override
    protected void prepareData() {
        prepareDao();
        fixture = new Fixture(app);
        initData = fixture.loadYamlFile("/test-data.yaml");
        postPrepareData();
    }

    protected void prepareDao() {
        entityClass = entityClass();
        dao = createDao();
        when(dbServiceManager.dao(any(Class.class))).thenAnswer(new Answer<Dao>() {
            @Override
            public Dao answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                Class<?> modelType = $.cast(args[0]);
                if (entityClass.isAssignableFrom(modelType)) {
                    return dao;
                }
                return null;
            }
        });
    }

    protected void postPrepareData() {
    }

    protected abstract Class<T> entityClass();

    protected MorphiaDao<T> createDao() {
        return new MorphiaDao<T>(entityClass(), ds());
    }

}
