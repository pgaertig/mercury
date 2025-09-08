package pl.amitec.mercury.integrators.polsoft.model;


import lombok.Builder;

import java.util.Map;

@Builder
public record PsStocks(
        Map<String, PsStock> map,
        boolean hasShortestExpirationDate
) {
}
