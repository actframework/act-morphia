package act.db.morphia;

import act.app.DbServiceManager;
import act.db.morphia.util.DateTimeConverter;
import act.db.morphia.util.DateTimeConverterTest;
import com.github.fakemongo.Fongo;
import org.junit.Before;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class MongoTestBase extends TestBase {

    private static final String DB_NAME = "con";

    protected Morphia morphia;
    protected Datastore ds;
    protected DbServiceManager dbServiceManager;

    @Before
    public final void setup() {
        prepareMongoDB();
    }

    public final void prepareMongoDB() {
        morphia = new Morphia();
        mapClasses();
        ds = morphia.createDatastore(new Fongo(DB_NAME).getMongo(), DB_NAME);
        dbServiceManager = mock(DbServiceManager.class);
        when(app.dbServiceManager()).thenReturn(dbServiceManager);
        prepareData();
    }

    protected final Morphia morphia() {
        return morphia;
    }
    protected final Datastore ds() {
        return ds;
    }

    protected void prepareData() {}

    protected void mapClasses() {
        morphia.getMapper().getConverters().addConverter(new DateTimeConverter());
        morphia.mapPackage("playground", true);
    }

}
