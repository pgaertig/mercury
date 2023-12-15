package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderStatus(
        Long id,
        String type,
        String name,
        String visiblename,
        String description
) {
}
