package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
        BigDecimal quantity,
        Long tax,
        String taxName,
        Long warehouse,
        String comment
) {
}
