package act.db.morphia;

import act.db.morphia.util.DateTimeConverter;
import com.github.fakemongo.Fongo;
import org.junit.Before;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

public abstract class MongoTestBase extends TestBase {

    private static final String DB_NAME = "con";

    private Morphia morphia;
    private Datastore ds;

    @Before
    public final void setup() {
        prepareMongoDB();
    }

    public final void prepareMongoDB() {
        morphia = new Morphia();
        mapClasses();
        ds = morphia.createDatastore(new Fongo(DB_NAME).getMongo(), DB_NAME);
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
