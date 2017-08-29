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

import act.app.event.AppEventId;
import act.db.DB;
import act.db.morphia.event.EntityMapped;
import act.job.OnAppEvent;
import act.util.AnnotatedClassFinder;
import org.mongodb.morphia.EntityInterceptor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.mapping.Mapper;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.S;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static act.Act.app;
import static act.db.morphia.MorphiaService.getService;

@SuppressWarnings("unused")
public class MorphiaModule {

    private static final Logger LOGGER = LogManager.get(MorphiaModule.class);

    @Inject
    private List<EntityInterceptor> interceptors;

    @AnnotatedClassFinder(Entity.class)
    @SuppressWarnings("unused")
    public void autoMapEntity(Class<?> clz) {
        MorphiaService service = MorphiaService.findByModelClass(clz);
        if (null == service) {
            LOGGER.warn("Cannot find MorphiaService for entity class: %s", clz);
            return;
        }
        Mapper mapper = service.mapper();
        mapper.addMappedClass(clz);

        registerFieldNameMapping(clz);
    }

    @OnAppEvent(AppEventId.PRE_START)
    @SuppressWarnings("unused")
    public void raiseMappedEvent() {
        for (MorphiaService service : MorphiaService.allMorphiaServices()) {
            app().eventBus().emit(new EntityMapped(service.mapper()));
            for (EntityInterceptor interceptor : interceptors) {
                DB db = interceptor.getClass().getAnnotation(DB.class);
                if (null == db || S.eq(db.value(), service.id())) {
                    service.mapper().addInterceptor(interceptor);
                }
            }
        }
    }

    private void registerFieldNameMapping(Class<?> clz) {
        Map<String, String> mapping = findFieldNameMapping(clz);
        if (!mapping.isEmpty()) {
            MorphiaService dbService = getService(clz);
            dbService.registerFieldNameMapping(clz, mapping);
        }
    }

    private Map<String, String> findFieldNameMapping(Class<?> clz) {
        Map<String, String> mapping = new HashMap<String, String>();
        List<Field> fields = $.fieldsOf(clz, true);
        for (Field f : fields) {
            Property property = f.getAnnotation(Property.class);
            if (null != property) {
                mapping.put(f.getName(), property.value());
            }
        }
        return mapping;
    }

}
