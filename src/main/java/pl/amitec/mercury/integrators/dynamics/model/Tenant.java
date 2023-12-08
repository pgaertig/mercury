package pl.amitec.mercury.integrators.dynamics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Tenant(
        @JsonProperty("Id") String id,
        @JsonProperty("Name") String name,
        @JsonProperty("Display_Name") String displayName,
        @JsonProperty("SystemModifiedAt") ZonedDateTime systemModifiedAt
) {
}
