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
