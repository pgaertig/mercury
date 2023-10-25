package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.*;

@JsonPropertyOrder({"source_id", "source", "name", "email", "phone", "street", "postcode",
        "city", "province", "nip", "country", "properties", "stock_discounts"})
@Builder
public record ImportClient(
        @JsonProperty("source_id") String sourceId,
        String source,
        String name,
        String email,
        String phone,
        String street,
        String postcode,
        String city,
        String province,
        String nip,
        String country,
        Map<String, String> properties,
        @JsonProperty("stock_discounts") @JsonInclude(Include.NON_EMPTY) List<StockDiscount> stockDiscounts) {
}
