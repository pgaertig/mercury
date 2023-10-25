package pl.amitec.mercury.clients.bitbee.types;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"source_id", "name", "source"})
public record Producer(
        @JsonProperty("source_id") String sourceId,
        String name,
        String source) {}
