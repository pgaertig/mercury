package pl.amitec.mercury.clients.bitbee.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetTimeDeserializer extends JsonDeserializer<OffsetTime> {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    @Override
    public OffsetTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        if(node.isEmpty()) {
            return null;
        }
        String dateAsString = node.get("date").asText();
        String timezone = node.get("timezone").asText();
        ZoneId zoneId = ZoneId.of(timezone);
        LocalDateTime localDateTime = LocalDateTime.parse(dateAsString, DATE_TIME_FORMATTER);
        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
        return zonedDateTime.toOffsetDateTime().toOffsetTime();
    }
}
