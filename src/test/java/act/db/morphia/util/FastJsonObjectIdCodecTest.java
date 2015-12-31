package act.db.morphia.util;

import act.db.morphia.TestBase;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.osgl.$;
import org.osgl.mvc.annotation.Before;

public class FastJsonObjectIdCodecTest extends TestBase {
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