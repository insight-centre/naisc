package eu.monnetproject.lang;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

/**
 *
 * @author John McCrae
 */
public class LanguageSerializer extends StdSerializer<Language> {

    public LanguageSerializer() {
        super(Language.class);
    }
    
    public LanguageSerializer(Class<Language> t) {
        super(t);
    }

    @Override
    public void serialize(Language value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.toString());
    }

}
