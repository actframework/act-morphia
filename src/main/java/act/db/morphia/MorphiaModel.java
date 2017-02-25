package act.db.morphia;

import com.alibaba.fastjson.annotation.JSONField;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.osgl.util.E;
import org.osgl.util.S;

/**
 * The default morphia model base implementation using {@link ObjectId} as the id type
 * @param <MODEL_TYPE>
 */
public abstract class MorphiaModel<MODEL_TYPE extends MorphiaModel> extends MorphiaModelBase<ObjectId, MODEL_TYPE> {

    @Id
    private ObjectId id;

    public MorphiaModel() {
    }

    public MorphiaModel(ObjectId id) {
        this.id = id;
    }

    @JSONField(serialize = false)
    public String getIdAsStr() {
        return null != id ? id.toString() : null;
    }

    public void setId(String id) {
        E.illegalArgumentIf(!ObjectId.isValid(id), "Invalid Object Id: %s", id);
        this.id = new ObjectId(id);
    }

    public ObjectId _id() {
        return id;
    }

    @Override
    public MODEL_TYPE _id(ObjectId id) {
        E.illegalStateIf(null != this.id);
        this.id = id;
        return _me();
    }

}
