package act.db.morphia;

import act.db.TimeTrackingModelBase;
import org.joda.time.DateTime;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Version;

public abstract class MorphiaModelBase<ID_TYPE, MODEL_TYPE extends MorphiaModelBase> extends TimeTrackingModelBase<DateTime, ID_TYPE, MODEL_TYPE> {

    @Indexed
    private DateTime _created;

    @Indexed
    private DateTime _modified;

    @Version
    private Long v;

    @Override
    public DateTime _created() {
        return _created;
    }

    @Override
    public void _created(DateTime timestamp) {
        _created = timestamp;
    }

    @Override
    public void _lastModified(DateTime timestamp) {
        _modified = timestamp;
    }

    @Override
    public Class<DateTime> _timestampType() {
        return DateTime.class;
    }

    @Override
    public DateTime _lastModified() {
        return _modified;
    }

}
