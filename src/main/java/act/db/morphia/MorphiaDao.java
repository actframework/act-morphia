package act.db.morphia;

import act.util.General;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;

/**
 * The morphia dao base implementation use {@link org.bson.types.ObjectId} as the ID type
 */
@General
public class MorphiaDao<MODEL_TYPE>
        extends MorphiaDaoBase<ObjectId, MODEL_TYPE> {

    public MorphiaDao(Class<MODEL_TYPE> modelType, Datastore ds) {
        super(modelType, ds);
    }

    public MorphiaDao(Class<MODEL_TYPE> modelType) {
        super(modelType);
    }
}
