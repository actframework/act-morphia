package act.db.morphia;

/*-
 * #%L
 * ACT Morphia
 * %%
 * Copyright (C) 2017 ActFramework
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

import act.app.DbServiceManager;
import act.app.event.SysEventId;
import act.db.DB;
import act.db.DbService;
import act.job.OnSysEvent;
import act.util.SimpleBean;
import act.util.SubClassFinder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.Generics;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * `ExternalModelAdaptor` allows application to use third party model classes
 * with morphia library.
 *
 * For example, suppose there is a third part model class `Snapshot`:
 *
 * ```java
 * public class Snapshot {
 *     private String snapshotId;
 *     private String volumeId;
 *     ...
 * }
 * ```
 *
 * Because it comes from third party library thus it is not possible to add
 * morphia annotations to that class. In order to use the class with morphia,
 * developer can create an adaptor class for it:
 *
 * ```java
 * public class SnapshotModel extends ExternalModelAdaptor<Snapshot> {
 *     .@Id
 *     private String snapshotId;
 *
 *     .@Indexed
 *     private String volumeId;
 * }
 * ```
 */
public abstract class ExternalModelAdaptor<MODEL_TYPE> implements SimpleBean {

    private static final Logger logger = LogManager.get(ExternalModelAdaptor.class);

    private static Map<String, List<$.T2<Class<ExternalModelAdaptor>, Class>>> adaptorRegistry = new HashMap<>();

    @SubClassFinder
    public static void processExternalModel(Class<ExternalModelAdaptor> adaptorClass) {
        List<Type> paramTypes = Generics.typeParamImplementations(adaptorClass, ExternalModelAdaptor.class);
        if (paramTypes.isEmpty()) {
            logger.warn("Found untyped ExternalModelAdaptor class: %s", adaptorClass);
            return;
        }
        Type type = paramTypes.get(0);
        if (type instanceof Class) {
            Class<ExternalModelAdaptor> modelClass = $.cast(type);
            registerAdaptor(adaptorClass, modelClass);
        } else {
            logger.warn("Unknown external model class type parameter: %s", type);
        }
    }

    private static void registerAdaptor(Class<ExternalModelAdaptor> adaptorClass, Class modelClass) {
        DB dbAnno = adaptorClass.getAnnotation(DB.class);
        String serviceId = null == dbAnno ? DbServiceManager.DEFAULT : dbAnno.value();
        List<$.T2<Class<ExternalModelAdaptor>, Class>> list = adaptorRegistry.get(serviceId);
        if (null == list) {
            list = new ArrayList<>();
            adaptorRegistry.put(serviceId, list);
        }
        list.add($.T2(adaptorClass, modelClass));
    }

    static void applyAdaptorFor(MorphiaService service) {
        applyAdaptorFor(service.id(), service);
    }

    private static void applyAdaptorFor(String serviceId, MorphiaService service) {
        List<$.T2<Class<ExternalModelAdaptor>, Class>> list = adaptorRegistry.remove(serviceId);
        if (null == list) {
            return;
        }
        for ($.T2<Class<ExternalModelAdaptor>, Class> pair : list) {
            Mapper mapper = service.mapper();
            map(pair._1, pair._2, mapper);
        }
    }

    @OnSysEvent(SysEventId.POST_START)
    static void applyAdaptorForDefaultService(DbServiceManager dbServiceManager) {
        if (adaptorRegistry.isEmpty()) {
            return;
        }
        DbService db = dbServiceManager.dbService(DbServiceManager.DEFAULT);
        if (!(db instanceof MorphiaService)) {
            return;
        }
        applyAdaptorFor(DbServiceManager.DEFAULT, (MorphiaService) db);
    }

    private static void map(Class<ExternalModelAdaptor> adaptorClass, Class modelClass, Mapper mapper) {
        final MappedClass mappedModel = mapper.getMappedClass(modelClass);
        final MappedClass mappedAdaptor = mapper.getMappedClass(adaptorClass);
        //copy the class level annotations
        for (final Map.Entry<Class<? extends Annotation>, List<Annotation>> e : mappedAdaptor.getRelevantAnnotations().entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) {
                for (final Annotation ann : e.getValue()) {
                    mappedModel.addAnnotation(e.getKey(), ann);
                }
            }
        }
        Entity entity = mappedAdaptor.getEntityAnnotation();
        if (null != entity) {
            // set the entity annotation
            mappedModel.update();
        }

        //copy the fields.
        for (final MappedField mf : mappedAdaptor.getPersistenceFields()) {
            final Map<Class<? extends Annotation>, Annotation> annMap = mf.getAnnotations();
            final MappedField destMF = mappedModel.getMappedFieldByJavaField(mf.getJavaFieldName());
            if (destMF != null && annMap != null && !annMap.isEmpty()) {
                for (final Map.Entry<Class<? extends Annotation>, Annotation> e : annMap.entrySet()) {
                    destMF.addAnnotation(e.getKey(), e.getValue());
                }
            }
        }

    }

}
