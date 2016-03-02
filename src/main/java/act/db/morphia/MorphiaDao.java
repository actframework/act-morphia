package act.db.morphia;

import act.util.General;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.osgl.Osgl;
import org.osgl.util.C;

import java.util.Collection;

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

    public MorphiaDao() {}

    public MODEL_TYPE findById(String id) {
        return findById(new ObjectId(id));
    }

    public void deleteById(String id) {
        super.deleteById(new ObjectId(id));
    }

    public Iterable<MODEL_TYPE> findByIdList(Collection<String> idList) {
        return super.findByIdList(C.list(idList).map(new Osgl.Transformer<String, ObjectId>() {
            @Override
            public ObjectId transform(String s) {
                return new ObjectId(s);
            }
        }));
    }
}
