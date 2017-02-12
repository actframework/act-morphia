package act.db.morphia;

import act.inject.param.NoBind;
import act.util.General;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;

/**
 * The morphia dao base implementation use {@link ObjectId} as the ID type
 */
@General
@NoBind
public class MorphiaDaoWithLongId<MODEL_TYPE>
        extends MorphiaDaoBase<Long, MODEL_TYPE> {

    public MorphiaDaoWithLongId(Class<MODEL_TYPE> modelType, Datastore ds) {
        super(Long.class, modelType, ds);
    }

    public MorphiaDaoWithLongId(Class<MODEL_TYPE> modelType) {
        super(Long.class, modelType);
    }

    public MorphiaDaoWithLongId() {
    }

    public MODEL_TYPE findById(String id) {
        return findById(Long.valueOf(id));
    }

    public void deleteById(String id) {
        super.deleteById(Long.valueOf(id));
    }

}
