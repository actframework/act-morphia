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

import act.inject.param.NoBind;
import act.util.General;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;

/**
 * The morphia dao base implementation use {@link org.bson.types.ObjectId} as the ID type
 */
@General
@NoBind
public class MorphiaDao<MODEL_TYPE>
        extends MorphiaDaoBase<ObjectId, MODEL_TYPE> {

    public MorphiaDao(Class<MODEL_TYPE> modelType, Datastore ds) {
        super(ObjectId.class, modelType, ds);
    }

    public MorphiaDao(Class<MODEL_TYPE> modelType) {
        super(ObjectId.class, modelType);
    }

    public MorphiaDao() {
    }

    public MODEL_TYPE findById(String id) {
        return findById(new ObjectId(id));
    }

    public void deleteById(String id) {
        super.deleteById(new ObjectId(id));
    }

}
