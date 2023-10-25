package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

@JsonPropertyOrder({"source_id", "price"})
@Builder
public record StockDiscount(
        @JsonProperty("source_id") String sourceId,
        String price) {}
