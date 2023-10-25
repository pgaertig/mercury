package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

@JsonPropertyOrder({"source_id", "name"})
@Builder
public record Category(
        @JsonProperty("source_id") String sourceId,
        TranslatedName name){}
