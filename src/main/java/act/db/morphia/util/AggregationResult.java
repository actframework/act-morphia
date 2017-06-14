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


import act.db.morphia.MorphiaService;
import com.mongodb.BasicDBObject;
import org.osgl.$;
import org.osgl.util.S;

import java.util.*;

public class AggregationResult {
    private List<BasicDBObject> result = null;
    private Class<?> modelType = null;
    private String field = null;

    public AggregationResult(List<BasicDBObject> r, String aggregationField, Class<?> modelClass) {
        if (null == r || null == aggregationField) throw new NullPointerException();
        result = r;
        modelType = modelClass;
        field = aggregationField;
    }

    public Long getDefault() {
        return result.size() > 0 ? result.get(0).getLong(field) : null;
    }

    public Long get(Object ... groupValues) {
        if (groupValues.length == 0) {
            return getDefault();
        }
        int len = groupValues.length;
        for (BasicDBObject r : result) {
            int i = 0;
            boolean found = true;
            for (Map.Entry<String, Object> entry : r.entrySet()) {
                if (i < len) {
                    if ($.ne(entry.getValue(), groupValues[i++])) {
                        found = false;
                        break;
                    }
                }
            }
            if (found) {
                return r.getLong(field);
            }
        }
        return null;
    }

    public Long getByGroupKeys(String groupKeys, Object... groupValues) {
        if (S.empty(groupKeys)) {
            if (groupValues.length == 0) return getDefault();
            throw new IllegalArgumentException("the number of group keys does not match the number of group values");
        }
        String[] sa = groupKeys.split("[\\s,;:]+");
        if (sa.length != groupValues.length) throw new IllegalArgumentException("the number of group keys does not match the number of group values");
        Set<String> mappedKeys = new HashSet<String>();
        for (String key : sa) {
            mappedKeys.add(mappedName(key));
        }
        for (BasicDBObject r: result) {
            boolean found = true;
            for (int i = 0; i < sa.length; ++i) {
                String groupKey = sa[i];
                String mappedGroupKey = mappedName(groupKey);
                Object groupValue = groupValues[i];
                if ($.ne(r.get(groupKey), groupValue) && $.ne(r.get(mappedGroupKey), groupValue)) {
                    found = false;
                    break;
                }
            }
            if (found) return r.getLong(this.field);
        }
        return null;
    }

    public Map<String, Long> asNumberMap() {
        Map<String, Long> m = new HashMap(result.size());
        for (BasicDBObject r : result) {
            Collection<?> c = r.values();
            Iterator itr = c.iterator();
            String k = itr.next().toString();
            String s = itr.next().toString();
            float f = Float.parseFloat(s);
            long l = (long)f;
            m.put(k, l);
        }
        return m;
    }
    
    public List<BasicDBObject> raw() {
        return result;
    }

    private String mappedName(String field) {
        return MorphiaService.mappedName(field, modelType);
    }
}
