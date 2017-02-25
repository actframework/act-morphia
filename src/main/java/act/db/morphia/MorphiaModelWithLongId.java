package act.db.morphia;

import act.Act;
import com.alibaba.fastjson.annotation.JSONField;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PrePersist;
import org.osgl.util.E;
import org.osgl.util.S;

/**
 * The default morphia model base implementation using {@link ObjectId} as the id type
 * @param <MODEL_TYPE>
 */
public abstract class MorphiaModelWithLongId<MODEL_TYPE extends MorphiaModelWithLongId> extends MorphiaModelBase<Long, MODEL_TYPE> {

    @Id
    private Long id;

    public MorphiaModelWithLongId() {
    }

    public MorphiaModelWithLongId(Long id) {
        this.id = id;
    }

    @JSONField(serialize = false)
    public String getIdAsStr() {
        return null != id ? id.toString() : null;
    }

    public void setId(String id) {
        E.illegalArgumentIf(!ObjectId.isValid(id), "Invalid Object Id: %s", id);
        this.id = Long.valueOf(id);
    }

    public Long _id() {
        return id;
    }

    @Override
    public MODEL_TYPE _id(Long id) {
        E.illegalStateIf(null != this.id);
        this.id = id;
        return _me();
    }

    @PrePersist
    private void populateId() {
        if (null == id) {
            id = Act.appConfig().sequenceNumberGenerator().next(getClass().getName());
        }
    }
}
