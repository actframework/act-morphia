package act.db.morphia;

import act.ActComponent;
import act.app.App;
import act.db.DbPlugin;
import act.db.DbService;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;

import java.util.Map;

@ActComponent
public class MorphiaPlugin extends DbPlugin {

    /**
     * The key to fetch the {@link act.db.morphia.util.SequenceNumberGenerator}
     * db service ID configuration
     */
    public static final String CONF_KEY_SEQ_SVC_ID = "act_morphia_seqgen_svc_id";

    public MorphiaPlugin() {
        MorphiaLoggerFactory.reset();
        MorphiaLoggerFactory.registerLogger(ActMorphiaLogger.Factory.class);
    }

    public DbService initDbService(String id, App app, Map<String, Object> conf) {
        return new MorphiaService(id, app, conf);
    }

}
