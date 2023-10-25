package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

@JsonPropertyOrder({"source_id", "source", "warehouse_id", "quantity", "price"})
@Builder
public record Stock(
        @JsonProperty("source_id") String sourceId,
        String source,
        @JsonProperty("warehouse_id") String warehouseId,
        String quantity,
        String price) {}
