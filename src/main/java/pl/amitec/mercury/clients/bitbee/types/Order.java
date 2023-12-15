package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Order(
        Long id,
        String uniqueNumber,
        Payment payment,
        Delivery delivery,
        OrderStatus status,
        ZonedDateTime added,
        ZonedDateTime modified,
        String source,
        String sourceid,
        BigDecimal netto,
        BigDecimal tax,
        BigDecimal brutto,
        String comment,
        OrderParty contact,
        OrderParty receiver,
        OrderParty invoice,
        List<OrderPosition> positions
) {
}