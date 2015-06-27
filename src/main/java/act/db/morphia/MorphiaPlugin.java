package act.db.morphia;

import act.app.App;
import act.db.DbPlugin;
import act.db.DbService;

import java.util.Map;

public class MorphiaPlugin extends DbPlugin {
    @Override
    public DbService initDbService(String id, App app, Map<String, Object> conf) {
        return new MorphiaService(id, app, conf);
    }
}
