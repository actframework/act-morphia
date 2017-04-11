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

import act.Act;
import act.app.DbServiceManager;
import act.db.DbManager;
import act.db.morphia.util.JodaDateTimeConverter;
import act.db.util.JodaDateTimeTsGenerator;
import com.mongodb.MongoClient;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class MongoTestBase extends TestBase {

    private static final String DB_NAME = "act_test";

    protected Morphia morphia;
    protected Datastore ds;
    protected DbServiceManager dbServiceManager;
    protected MorphiaService morphiaService;
    protected DbManager dbManager;

    @Before
    public final void setup() throws Exception {
        prepareMongoDB();
    }

    @After
    public final void cleanUp() throws Exception {
        ds().getDB().dropDatabase();
    }

    public final void prepareMongoDB() throws Exception {
        morphia = new Morphia();
        mapClasses();
        //ds = morphia.createDatastore(new Fongo(DB_NAME).getMongo(), DB_NAME);
        ds = morphia.createDatastore(new MongoClient(), DB_NAME);
        dbServiceManager = mock(DbServiceManager.class);
        morphiaService = new MorphiaService("default", app, new HashMap<String, String>());
        when(app.dbServiceManager()).thenReturn(dbServiceManager);
        when(dbServiceManager.dbService("default")).thenReturn(morphiaService);
        dbManager = mock(DbManager.class);
        when(dbManager.timestampGenerator(DateTime.class)).thenReturn(new JodaDateTimeTsGenerator());
        Field f = Act.class.getDeclaredField("dbManager");
        f.setAccessible(true);
        f.set(null, dbManager);
        prepareData();
    }

    protected final Morphia morphia() {
        return morphia;
    }

    protected final Datastore ds() {
        return ds;
    }

    protected void prepareData() {
    }

    protected void mapClasses() {
        morphia.getMapper().getConverters().addConverter(new JodaDateTimeConverter());
        morphia.mapPackage("playground", true);
    }

}
