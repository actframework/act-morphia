package act.db.morphia;

/*-
 * #%L
 * ACT Morphia
 * %%
 * Copyright (C) 2015 - 2019 ActFramework
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

import act.app.App;
import act.app.DbServiceManager;
import act.app.event.SysEventId;
import act.db.DbService;
import act.db.meta.EntityClassMetaInfo;
import act.db.meta.EntityMetaInfoRepo;
import act.db.meta.MasterEntityMetaInfoRepo;
import act.job.OnSysEvent;
import org.mongodb.morphia.annotations.Id;
import org.osgl.$;

import java.lang.reflect.Field;
import java.util.List;

public class EntityMetaInfoUpdater {

    @OnSysEvent(SysEventId.PRE_START)
    public void doJob(DbServiceManager dbServiceManager, App app) {
        MasterEntityMetaInfoRepo masterRepo = app.entityMetaInfoRepo();
        for (DbService db : dbServiceManager.registeredServices()) {
            if (db instanceof MorphiaService) {
                EntityMetaInfoRepo repo = masterRepo.forDb(db.id());
                if (null != repo) {
                    for (Class entityClass : repo.entityClasses()) {
                        EntityClassMetaInfo classMetaInfo = repo.classMetaInfo(entityClass);
                        if (null != classMetaInfo) {
                            updateClassMetaInfo(classMetaInfo, entityClass, repo);
                        }
                    }
                }
            }
        }
    }

    private void updateClassMetaInfo(EntityClassMetaInfo classMetaInfo, Class entityClass, EntityMetaInfoRepo repo) {
        String entityClassName = entityClass.getName();
        List<Field> fields = $.fieldsOf(entityClass);
        if (null == classMetaInfo.idField()) {
            String idFieldName = idFieldNameOf(fields);
            if (null != idFieldName) {
                repo.registerIdField(entityClassName, idFieldName);
            }
        }
        if (null == classMetaInfo.createdAtField()) {
            if (null != $.fieldOf(entityClass, "_created")) {
                repo.registerCreatedAtField(entityClassName, "_created");
            }
        }
        if (null == classMetaInfo.lastModifiedAtField()) {
            if (null != $.fieldOf(entityClass, "_modified")) {
                repo.registerLastModifiedAtField(entityClassName, "_modified");
            }
        }
    }

    private String idFieldNameOf(List<Field> fields) {
        for (Field field : fields) {
            if (null != field.getAnnotation(Id.class)) {
                return field.getName();
            }
        }
        return null;
    }

}
