package org.insightcentre.uld.naisc.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdKeyDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

/**
 * A pair of strings
 * 
 * @author John McCrae
 */
@JsonDeserialize(using = StringPair.StringPairDeserializer.class)
@JsonSerialize(using = StringPair.StringPairSerializer.class)
public class StringPair extends Pair<String, String> {

    public StringPair(String _1, String _2) {
        super(_1, _2);
    }
    
    public static class StringPairDeserializer extends StdDeserializer<StringPair> {

        public StringPairDeserializer(Class<?> vc) {
            super(vc);
        }

        public StringPairDeserializer(JavaType valueType) {
            super(valueType);
        }

        public StringPairDeserializer(StdDeserializer<?> src) {
            super(src);
        }

        public StringPairDeserializer() {
            super((Class<?>)null);
        }
        
        

        @Override
        public StringPair deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String s = jp.getText();
            String[] ss = s.split("--");
            if(ss.length != 2) {
                throw new JsonMappingException(jp, "String pair not in correct format: " + s);
            }
            return new StringPair(ss[0].replaceAll("\\\\-", "-"), ss[1].replaceAll("\\\\-", "--"));
        }
        
    }

    public static class StringPairSerializer extends StdSerializer<StringPair> {

        public StringPairSerializer() {
            super((Class<StringPair>)null);
        }
        
        public StringPairSerializer(Class<StringPair> t) {
            super(t);
        }

        @Override
        public void serialize(StringPair t, JsonGenerator jg, SerializerProvider sp) throws IOException {
            jg.writeString(t._1.replaceAll("-", "\\\\-") + "--" + t._2.replaceAll("-", "\\\\-"));
        }
        
    }
    
    public static class StringPairKeyDeserializer extends StdKeyDeserializer {

        public StringPairKeyDeserializer(int kind, Class<?> cls) {
            super(kind, cls);
        }

        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
            String[] ss = key.split("--");
            if(ss.length != 2) {
                throw new RuntimeException("String pair not in correct format: " + key);
            }
            return new StringPair(ss[0].replaceAll("\\\\-", "-"), ss[1].replaceAll("\\\\-", "--"));
        }
     
    }

    @Override
    public String toString() {
        return this._1.replaceAll("-", "\\\\-") + "--" + this._2.replaceAll("-", "\\\\-");
    }
    
    
}
