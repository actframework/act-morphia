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
