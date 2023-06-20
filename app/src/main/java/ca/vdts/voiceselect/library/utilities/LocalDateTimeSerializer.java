package ca.vdts.voiceselect.library.utilities;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeSerializer implements JsonDeserializer<LocalDateTime>, JsonSerializer<LocalDateTime> {
    private static final Logger LOG = LoggerFactory.getLogger(LocalDateTimeSerializer.class);
    private final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
    private final DateTimeFormatter dateTimeWithoutSeconds = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    public LocalDateTimeSerializer() {
    }

    public LocalDateTime deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
        String date = element.getAsString().replace('T', ' ').replaceAll("-", "/");
        synchronized(this.dateTimeFormat) {
            LocalDateTime result;
            if (date.length() == 16) {
                result = LocalDateTime.parse(date, this.dateTimeWithoutSeconds);
            } else if (date.length() > 16) {
                result = LocalDateTime.parse(date, this.dateTimeFormat);
            } else {
                result = null;
            }

            return result;
        }
    }

    public JsonElement serialize(LocalDateTime date, Type type, JsonSerializationContext context) {
        String result;
        synchronized(this.dateTimeFormat) {
            result = this.dateTimeFormat.format(date);
        }

        return new JsonPrimitive(result);
    }
}
