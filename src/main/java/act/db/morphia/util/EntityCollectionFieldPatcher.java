package act.db.morphia.util;

/*-
 * #%L
 * ACT Morphia
 * %%
 * Copyright (C) 2015 - 2020 ActFramework
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
import com.mongodb.DBObject;
import org.mongodb.morphia.AbstractEntityInterceptor;
import org.mongodb.morphia.mapping.Mapper;
import org.osgl.$;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Make sure non-initialized collection or map typed fields of the entity get
 * initialized
 */
public class EntityCollectionFieldPatcher extends AbstractEntityInterceptor {

    private ConcurrentMap<Class, List<Field>> collectionFieldLookup = new ConcurrentHashMap<>();

    @Override
    public void postLoad(Object ent, DBObject dbObj, Mapper mapper) {
        if (null == ent) {
            return;
        }
        List<Field> collectionFields = collectionFieldsOf(ent.getClass());
        ensureCollectionFieldsInitialized(collectionFields, ent);
    }

    private List<Field> collectionFieldsOf(Class entityType) {
        List<Field> fields = collectionFieldLookup.get(entityType);
        if (null == fields) {
            fields = searchForCollectionFields(entityType);
            List<Field> list = collectionFieldLookup.putIfAbsent(entityType, fields);
            if (null == list) {
                list = fields;
            }
            return list;
        }
        return fields;
    }

    private List<Field> searchForCollectionFields(Class entityType) {
        List<Field> fields = $.fieldsOf(entityType);
        List<Field> retVal = new ArrayList<>(fields.size());
        for (Field f : fields) {
            Class<?> type = f.getType();
            if (Collection.class.isAssignableFrom(type)) {
                retVal.add(f);
            } else if (Map.class.isAssignableFrom(type)) {
                retVal.add(f);
            }
        }
        return retVal;
    }

    private void ensureCollectionFieldsInitialized(List<Field> fields, Object entity) {
        for (Field field : fields) {
            ensureCollectionFieldInitialized(field, entity);
        }
    }

    private void ensureCollectionFieldInitialized(Field field, Object entity) {
        if (null == $.getFieldValue(entity, field)) {
            $.setFieldValue(entity, field, Act.getInstance(field.getType()));
        }
    }
}
