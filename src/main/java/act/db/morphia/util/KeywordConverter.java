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

import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import org.osgl.util.Keyword;

/**
 * The {@link org.osgl.util.Keyword} converter
 */
public class KeywordConverter extends TypeConverter implements SimpleValueConverter {
    public KeywordConverter() {
        setSupportedTypes(new Class[]{ Keyword.class });
    }

    @Override
    public Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo) {
        if (null == fromDBObject) {
            return null;
        }
        return Keyword.of((String) fromDBObject);
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
        if (null == value) {
            return null;
        }
        return ((Keyword) value).dashed();
    }


}
