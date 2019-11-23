package act.db.morphia.util;

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

import com.mongodb.DBObject;
import org.mongodb.morphia.converters.Converters;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.EphemeralMappedField;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.utils.ReflectionUtils;
import org.osgl.util.C;

import java.util.Collection;
import java.util.List;

public class CListConverter extends TypeConverter {

    public CListConverter() {
        super(C.List.class);
    }

    @Override
    public Object decode(Class<?> targetClass, Object fromDBObject, MappedField mf) {
        if (mf == null || fromDBObject == null) {
            return fromDBObject;
        }

        final Class subtypeDest = mf.getSubClass();
        final Collection values = C.newList();

        final Converters converters = getMapper().getConverters();
        if (fromDBObject.getClass().isArray()) {
            //This should never happen. The driver always returns list/arrays as a List
            for (final Object o : (Object[]) fromDBObject) {
                values.add(converters.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, mf));
            }
        } else if (fromDBObject instanceof Iterable) {
            // map back to the java data type
            // (List/Set/Array[])
            for (final Object o : (Iterable) fromDBObject) {
                if (o instanceof DBObject) {
                    final List<MappedField> typeParameters = mf.getTypeParameters();
                    if (!typeParameters.isEmpty()) {
                        final MappedField mappedField = typeParameters.get(0);
                        if (mappedField instanceof EphemeralMappedField) {
                            values.add(converters.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, mappedField));
                        } else {
                            throw new UnsupportedOperationException("mappedField isn't an EphemeralMappedField");
                        }
                    } else {
                        values.add(converters.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, mf));
                    }
                } else {
                    values.add(converters.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, mf));
                }
            }
        } else {
            //Single value case.
            values.add(converters.decode((subtypeDest != null) ? subtypeDest : fromDBObject.getClass(), fromDBObject, mf));
        }

        //convert to and array if that is the destination type (not a list/set)
        if (mf.getType().isArray()) {
            return ReflectionUtils.convertToArray(subtypeDest, (List) values);
        } else {
            return values;
        }
    }
}
