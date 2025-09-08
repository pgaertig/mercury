package pl.amitec.mercury.clients.bitbee.types;

import java.math.BigDecimal;
import java.util.List;

public record Delivery(
        Long id,
        String name,
        BigDecimal tax,
        List<Payment> payments,
        BigDecimal cost,
        String type
) {
}
