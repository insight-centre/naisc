package eu.monnetproject.lang;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

/**
 *
 * @author John McCrae
 */
public class LanguageDeserializer extends StdDeserializer<Language> {
    public LanguageDeserializer() {
        super(Language.class);
    }
    
    public LanguageDeserializer(Class<?> vc) {
        super(vc);
    }

    public LanguageDeserializer(JavaType valueType) {
        super(valueType);
    }

    public LanguageDeserializer(StdDeserializer<?> src) {
        super(src);
    }

    @Override
    public Language deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return Language.get(p.getText());
    }

}
