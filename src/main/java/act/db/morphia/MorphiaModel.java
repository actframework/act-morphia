package act.db.morphia;

import act.db.morphia.util.DateTimeConverter;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Id;

@Converters(DateTimeConverter.class)
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

    public String getId() {
        return null != id ? id.toString() : null;
    }

    public String _id() {
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

}
