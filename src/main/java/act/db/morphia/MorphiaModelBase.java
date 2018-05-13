package act.db.morphia;

/*-
 * #%L
 * ACT Morphia
 * %%
 * Copyright (C) 2015 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.data.Versioned;
import act.data.util.JodaDateTimeResolver;
import act.db.TimeTrackingModelBase;
import act.inject.param.NoBind;
import org.joda.time.DateTime;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Version;
import org.osgl.$;
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
        // we must add _id() to _v().
        // see https://github.com/actframework/act-morphia/issues/17
        return S.concat(_v(), _id());
    }

    // For JSON serialization
    public Long getV() {
        return v;
    }

    // For JSON deserialization
    public void setV(Number v) {
        this.v = null == v ? null : v.longValue();
    }

    private Long _v() {
        if (null != v) {
            return v;
        }
        return null == _modified ? -1L : _modified.getMillis();
    }

    /**
     * Returns a copy stage with `rootClass` specified as `MorphiaModelBase`.
     *
     * @param model
     *      the model to be copied
     * @return
     *      a mapping stage with copy semantic
     */
    public static $._MappingStage copy(MorphiaModelBase model) {
        return $.copy(model).rootClass(MorphiaModelBase.class);
    }

    /**
     * Returns a copy stage with `rootClass` specified as `MorphiaModelBase`.
     *
     * @param model
     *      the model to be copied
     * @return
     *      a mapping stage with deep copy semantic
     */
    public static $._MappingStage deepCopy(MorphiaModelBase model) {
        return $.deepCopy(model).rootClass(MorphiaModelBase.class);
    }

    /**
     * Returns a merge stage with `rootClass` specified as `MorphiaModelBase`.
     *
     * @param model
     *      the model to be copied
     * @return
     *      a mapping stage with merge semantic
     */
    public static $._MappingStage merge(MorphiaModelBase model) {
        return $.merge(model).rootClass(MorphiaModelBase.class);
    }


    /**
     * Returns a mapping stage with `rootClass` specified as `MorphiaModelBase`.
     *
     * @param model
     *      the model to be copied
     * @return
     *      a mapping stage with mapping semantic
     */
    public static $._MappingStage map(MorphiaModelBase model) {
        return $.map(model).rootClass(MorphiaModelBase.class);
    }
}
