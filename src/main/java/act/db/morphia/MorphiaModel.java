package act.db.morphia;

import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.mongodb.morphia.annotations.Id;
import org.osgl._;
import org.osgl.util.S;

public abstract class MorphiaModel {

    @Id
    private ObjectId id;

    private DateTime _created;

    private DateTime _modified;

    public MorphiaModel() {
    }

    public MorphiaModel(ObjectId id) {
        this.id = id;
    }

    public ObjectId getId() {
        return id;
    }

    public String getIdAsStr() {
        return null != id ? id.toString() : null;
    }

    public ObjectId _id() {
        return getId();
    }

    public boolean _isNew() {
        return null == _created;
    }

    public DateTime _created() {
        return _created;
    }

    public DateTime _modified() {
        return _modified;
    }

    void _preSave() {
        DateTime now = DateTime.now();
        if (null == _created) {
            _created = now;
        }
        _modified = now;
    }

    @Override
    public String toString() {
        return S.builder().append(getClass().getName()).append("[").append(id).append("]").toString();
    }

    @Override
    public int hashCode() {
        return _.hc(getClass(), id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (_.eq(obj.getClass(), getClass())) {
            MorphiaModel that = (MorphiaModel) obj;
            return _.eq(that.id, id);
        }
        return false;
    }
}
