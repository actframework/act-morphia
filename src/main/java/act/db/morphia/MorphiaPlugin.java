package act.db.morphia;

import act.app.App;
import act.db.DbPlugin;
import act.db.DbService;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.osgl.logging.L;
import org.osgl.logging.Logger;

import java.util.Map;

public class MorphiaPlugin extends DbPlugin {

    public MorphiaPlugin() {
        MorphiaLoggerFactory.reset();
        MorphiaLoggerFactory.registerLogger(ActMorphiaLogger.Factory.class);
    }

    public DbService initDbService(String id, App app, Map<String, Object> conf) {
        return new MorphiaService(id, app, conf);
    }

}
