package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderPosition(
        Long id,
        Long variant,
        String variantName,
        String variantSource,
        String variantSourceId,
        String name,
        String code,
        BigDecimal price,
        BigDecimal netto,
        BigDecimal brutto,
        BigDecimal totalNetto,
        BigDecimal totalBrutto,
        BigDecimal quantity,
        @JsonProperty("tax") Long taxId,
        String taxName,
        @JsonProperty("unit") Long unitId,
        String unitShortName,
        @JsonProperty("warehouse") Long warehouseId,
        String warehouseName,
        String comment
) {
}
