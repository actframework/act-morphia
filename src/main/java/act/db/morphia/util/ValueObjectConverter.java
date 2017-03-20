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

import act.app.App;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.KVStore;
import org.osgl.util.ValueObject;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static act.db.morphia.util.KVStoreConverter.UDF_TYPE;
import static act.db.morphia.util.KVStoreConverter.VALUE;

/**
 * The {@link org.osgl.util.ValueObject} converter
 */
@Singleton
public class ValueObjectConverter extends TypeConverter implements SimpleValueConverter {

    public static final ValueObjectConverter INSTANCE = new ValueObjectConverter();

    public ValueObjectConverter() {
        setSupportedTypes(new Class[]{ValueObject.class});
    }

    @Override
    public Object decode(Class<?> aClass, Object o, MappedField mappedField) {
        if (o instanceof DBObject) {
            if (o instanceof BasicDBObject) {
                BasicDBObject dbObject = (BasicDBObject) o;
                String valueType = dbObject.getString(UDF_TYPE);
                Class cls = $.classForName(valueType, App.instance().classLoader());
                if (Map.class.isAssignableFrom(cls)) {
                    return new KVStoreConverter().decode(cls, dbObject.get(VALUE));
                } else if (List.class.isAssignableFrom(cls)) {
                    BasicDBList dbList = $.cast(dbObject.get(VALUE));
                    List list = C.newSizedList(dbList.size());
                    for (Object item : dbList) {
                        list.add(ValueObject.of(decode(ValueObject.class, item)));
                    }
                    return list;
                } else {
                    String valueString = dbObject.getString(VALUE);
                    o = ValueObject.decode(valueString, cls);
                }
            } else if (o instanceof BasicDBList) {
                BasicDBList dbList = (BasicDBList) o;
                List<ValueObject> list = new ArrayList<ValueObject>();
                for (Object element : dbList) {
                    list.add(ValueObject.of(element));
                }
                return list;
            }
        }
        return ValueObject.of(o);
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
        if (!(value instanceof ValueObject)) {
            return value;
        }
        ValueObject vo = (ValueObject) value;
        if (vo.isUDF()) {
            Object v = vo.value();
            Class<?> type = v.getClass();
            if (Map.class.isAssignableFrom(type)) {
                v = new KVStoreConverter().encode(v, optionalExtraInfo);
            } else if (List.class.isAssignableFrom(type)) {
                BasicDBList dbList = new BasicDBList();
                List<Object> list = (List)v;
                for (Object item : list) {
                    dbList.add(encode(ValueObject.of(item), optionalExtraInfo));
                }
                v = dbList;
            } else {
                v = ValueObject.encode(v);
            }
            DBObject dbObject = new BasicDBObject();
            dbObject.put(VALUE, v);
            String typeName = type.getName();
            if (JSONObject.class.getName().equals(typeName)) {
                typeName = KVStore.class.getName();
            }
            dbObject.put(UDF_TYPE, typeName);
            return dbObject;
        } else {
            return vo.value();
        }
    }

}
