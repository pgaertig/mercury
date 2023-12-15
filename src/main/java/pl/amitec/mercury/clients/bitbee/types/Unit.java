package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Unit(
        Long id,
        String name,
        @JsonProperty("short") String shortName,
        @JsonProperty("shop") Long shopId
) {}