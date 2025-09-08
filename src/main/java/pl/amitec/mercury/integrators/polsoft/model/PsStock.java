package pl.amitec.mercury.integrators.polsoft.model;

import lombok.Builder;

@Builder
public record PsStock(
        String productId,
        String amount,
        String shortestExpirationDate
) {
}
