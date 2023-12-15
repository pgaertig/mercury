package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JournalItem(
        String id,
        @JsonProperty("shop") String shopId,
        String type,
        String objectId,
        String active,
        String added, //TODO LocalDateTime, requires serializer/deserializer setup
        String modified, //TODO LocalDateTime, requires serializer/deserializer setup
        String confirm
) { }