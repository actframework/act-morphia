package act.db.morphia.util;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Serializer and Deserializer of ObjectID for FastJson
 */
public class FastJsonObjectIdCodec implements ObjectSerializer, ObjectDeserializer {

    @Override
    public int getFastMatchToken() {
        return JSONToken.LITERAL_STRING;
    }

    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        JSONLexer lexer = parser.getLexer();
        if (lexer.token() == JSONToken.LITERAL_STRING) {
            String text = lexer.stringVal();
            lexer.nextToken(JSONToken.COMMA);
            return (T) new ObjectId(text);
        }
        return null;
    }

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.getWriter();
        if (object == null) {
            out.writeNull();
            return;
        }
        out.write("\"" + object.toString() + "\"");
    }
}
