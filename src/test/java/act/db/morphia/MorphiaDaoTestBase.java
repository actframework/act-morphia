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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import act.db.Dao;
import act.test.util.Fixture;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgl.$;

import java.util.Map;

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
