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

import act.db.morphia.MorphiaTestBase;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.osgl.$;
import org.osgl.mvc.annotation.Before;

public class FastJsonObjectIdCodecTest extends MorphiaTestBase {
    Foo foo;

    @Before
    public void prepare() {
        foo = new Foo();
        SerializeConfig serializeConfig = SerializeConfig.getGlobalInstance();
        serializeConfig.put(ObjectId.class, new FastJsonObjectIdCodec());
        ParserConfig parserConfig = ParserConfig.getGlobalInstance();
        parserConfig.putDeserializer(ObjectId.class, new FastJsonObjectIdCodec());
    }

    @Test
    public void test() {
        String s = JSON.toJSONString(foo);
        System.out.println(s);
        Foo foo1 = JSON.parseObject(s, Foo.class);
        eq(foo, foo1);
    }
}

class Foo {
    ObjectId id;
    Foo() {
        id = new ObjectId();
    }
    public ObjectId getId() {
        return id;
    }
    public void setId(ObjectId id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Foo) {
            Foo that = $.cast(obj);
            return that.id.equals(this.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return $.hc(id);
    }
}
