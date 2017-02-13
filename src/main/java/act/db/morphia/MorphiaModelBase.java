package act.db.morphia;

import act.data.Versioned;
import act.data.util.JodaDateTimeResolver;
import act.db.TimeTrackingModelBase;
import act.inject.param.NoBind;
import org.joda.time.DateTime;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Version;
import org.osgl.util.S;

@NoBind
public abstract class MorphiaModelBase<ID_TYPE, MODEL_TYPE extends MorphiaModelBase>
        extends TimeTrackingModelBase<ID_TYPE, MODEL_TYPE, DateTime, JodaDateTimeResolver> implements Versioned {

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

    @Override
    public JodaDateTimeResolver _timestampTypeResolver() {
        return JodaDateTimeResolver.INSTANCE;
    }

    /**
     * Returns version of the entity. This function should return
     * the value of field {@link #v}. However if the  field `v`
     * is `null`, then it will try to return the {@link DateTime#getMillis() millis}
     * of {@link #_modified} field, if that field is also `null` then
     * it shall return `-1`
     * @return the version of the entity
     */
    public String _version() {
        return S.string(_v());
    }

    private Long _v() {
        if (null != v) {
            return v;
        }
        return null == _modified ? -1L : _modified.getMillis();
    }

}
