package act.db.morphia.util;

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

import act.db.DbBind;
import act.db.morphia.MorphiaAdaptiveRecord;
import act.db.morphia.MorphiaDao;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Action;
import org.osgl.mvc.annotation.DeleteAction;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

import java.util.Map;

import static act.controller.Controller.Util.notFoundIfNull;

public abstract class MorphiaAdaptiveRecordRestfulServiceBase<MODEL_TYPE extends MorphiaAdaptiveRecord<MODEL_TYPE>> extends MorphiaDao<MODEL_TYPE> {
    @GetAction
    public Iterable<MODEL_TYPE> list() {
        return findAll();
    }

    @GetAction("{id}")
    public MODEL_TYPE get(@DbBind("id") MODEL_TYPE model) {
        return model;
    }

    @PostAction
    public MODEL_TYPE create(MODEL_TYPE model) {
        return save(model);
    }

    @Action(value = "{id}", methods = {H.Method.PATCH, H.Method.PUT})
    public MODEL_TYPE update(@DbBind("id") MODEL_TYPE model, Map<String, Object> data) {
        notFoundIfNull(model);
        return save(model.mergeValues(data));
    }

    @DeleteAction("{id}")
    public void delete(String id) {
        deleteById(id);
    }

}
